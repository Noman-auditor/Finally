package com.nora.tunnel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Real Android VpnService host for the bundled sing-box core.
 *
 * Lifecycle: prepare() -> establish TUN with the parameters the control plane
 * computed -> hand the validated config to the core -> poll the core's own
 * traffic totals -> report the real numbers back to the control plane.
 *
 * If any step fails the service stops and publishes an explicit error; it never
 * reports "connected" without a live core and a live TUN.
 */
public class NoraVpnService extends VpnService {
    public static final String ACTION_START = "com.nora.tunnel.START";
    public static final String ACTION_STOP = "com.nora.tunnel.STOP";
    public static final String ACTION_RELOAD = "com.nora.tunnel.RELOAD";

    private static final String CHANNEL_ID = "nora_tunnel_session";
    private static final int NOTIFICATION_ID = 4711;
    private static final long REPORT_INTERVAL_MS = 15000L;

    private static volatile boolean running;
    /** True while the kill switch keeps a blackhole TUN open after a failure. */
    private static volatile boolean holding;
    private static volatile NoraVpnService instance;
    private volatile boolean localSession;

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private ParcelFileDescriptor tunDescriptor;
    private volatile boolean reportTickRunning;
    private Runnable trafficTick;

    public static boolean isRunning() {
        return running;
    }

    /** Running, or blocking traffic under the kill switch. Either way a Disconnect must be offered. */
    public static boolean isActive() {
        return running || holding;
    }

    /** Called by the core handler when sing-box stops on its own. */
    static void onCoreStoppedUnexpectedly() {
        NoraVpnService service = instance;
        if (service != null && running) {
            service.io.execute(() -> {
                if (running) {
                    service.fail("The sing-box core stopped unexpectedly.", true);
                }
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        TunnelState.setRedact(Prefs.flag(this, Prefs.KEY_REDACT, true));
    }

    /** True when Android still needs to show its "allow VPN connection" dialog (null intent = already granted). */
    public static Intent permissionIntent(Context context) {
        return VpnService.prepare(context);
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, NoraVpnService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, NoraVpnService.class).setAction(ACTION_STOP);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null || intent.getAction() == null ? ACTION_START : intent.getAction();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        switch (action) {
            case ACTION_STOP:
                io.execute(this::shutdown);
                break;
            case ACTION_RELOAD:
                io.execute(() -> {
                    shutdownCore();
                    connect();
                });
                break;
            default:
                io.execute(this::connect);
        }
        return START_STICKY;
    }

    /* ------------------------------- connect ------------------------------- */

    private void connect() {
        if (running) {
            TunnelState.log("[vpn] already running — ignoring duplicate start");
            return;
        }
        holding = false;
        TunnelState.resetCounters();

        ControlPlane.Envelope envelope;
        if (LocalSession.hasActiveProfile(this)) {
            TunnelState.setState(TunnelState.PREPARING, "Preparing the selected local profile…");
            try {
                envelope = LocalSession.buildEnvelope(this);
            } catch (Exception error) {
                fail("The selected profile cannot be used: " + error.getMessage());
                return;
            }
            localSession = true;
            TunnelState.setProfile(0, envelope.profileName);
            TunnelState.log("[profile] using local profile \"" + envelope.profileName + "\" → " + envelope.endpoint);
            if (!envelope.warnings.isEmpty()) {
                TunnelState.log("[profile] note: " + envelope.warnings);
            }
        } else {
            localSession = false;
            envelope = fetchRemoteEnvelope();
            if (envelope == null) {
                return;
            }
        }

        applyRoutingSettings(envelope);

        TunnelState.setState(TunnelState.CONNECTING, "Establishing the TUN interface…");
        int fd = establishTun(envelope);
        if (fd < 0) {
            return;
        }

        TunnelState.setState(TunnelState.CONNECTING, "Starting the sing-box core…");
        boolean started = TunnelCore.start(this, envelope.configJson, fd);
        if (!started) {
            fail("The sing-box core refused to start. See the log for the real error.", true);
            return;
        }

        running = true;
        TunnelState.markStarted();
        TunnelState.setCoreVersion(TunnelCore.version());
        TunnelState.setState(TunnelState.CONNECTED,
                "Core running with " + envelope.profileName + " · traffic is routed by sing-box inside this VpnService.");
        if (!localSession) {
            startReportLoop();
        }
        sampleTraffic();
        updateNotification();
    }

    /** Legacy path: ask the control plane for a validated config. Returns null after calling fail(). */
    private ControlPlane.Envelope fetchRemoteEnvelope() {
        int profileId = Prefs.profileId(this);
        if (profileId == 0) {
            fail("No profile selected. Import a profile and select it, or pick one from the control plane.");
            return null;
        }
        TunnelState.setState(TunnelState.PREPARING, "Asking the control plane for a validated configuration…");
        TunnelState.setProfile(profileId, Prefs.profileName(this));
        try {
            ControlPlane.Envelope envelope = ControlPlane.envelope(this, profileId);
            TunnelState.log("[control-plane] received validated config for profile #" + profileId
                    + " (" + envelope.profileName + " → " + envelope.endpoint + ")");
            if (!envelope.warnings.isEmpty()) {
                TunnelState.log("[control-plane] warnings: " + envelope.warnings);
            }
            return envelope;
        } catch (Exception error) {
            TunnelState.log("[control-plane] config fetch failed: " + error.getMessage());
            String cached = ControlPlane.cachedConfig(this);
            if (cached == null || cached.isEmpty()) {
                fail("Could not fetch a configuration and no cached copy exists: " + error.getMessage());
                return null;
            }
            ControlPlane.Envelope envelope = new ControlPlane.Envelope();
            envelope.configJson = cached;
            envelope.profileName = Prefs.profileName(this);
            envelope.mtu = 9000;
            TunnelState.log("[control-plane] falling back to the cached configuration from the last successful fetch");
            return envelope;
        }
    }

    /** Applies the user's per-app routing choice. Android allows an allow-list or a deny-list, never both. */
    private void applyRoutingSettings(ControlPlane.Envelope envelope) {
        if (!Prefs.flag(this, Prefs.KEY_PER_APP, false)) {
            return;
        }
        java.util.Set<String> chosen = Prefs.stringSet(this, Prefs.KEY_PER_APP_PACKAGES);
        chosen.remove(getPackageName());
        if (chosen.isEmpty()) {
            TunnelState.log("[routing] per-app routing is on but no apps are selected — routing all apps");
            return;
        }
        envelope.includePackages.clear();
        envelope.excludePackages.clear();
        if ("exclude".equals(Prefs.text(this, Prefs.KEY_PER_APP_MODE, "include"))) {
            envelope.excludePackages.addAll(chosen);
        } else {
            envelope.includePackages.addAll(chosen);
        }
    }

    /** Builds the VpnService TUN using the exact parameters the control plane supplied. */
    private int establishTun(ControlPlane.Envelope envelope) {
        try {
            Builder builder = new Builder()
                    .setSession("Nora Tunnel")
                    .setMtu(envelope.mtu > 0 ? envelope.mtu : 9000)
                    .addAddress(envelope.tunAddress.isEmpty() ? "172.19.0.1" : envelope.tunAddress, 30)
                    .addRoute("0.0.0.0", 0);

            boolean tunnelIpv6 = Prefs.flag(this, Prefs.KEY_IPV6, true);
            if (!envelope.tunAddress6.isEmpty() && tunnelIpv6) {
                builder.addAddress(envelope.tunAddress6, 126);
                builder.addRoute("::", 0);
            } else {
                // Capture IPv6 and let it die in the TUN instead of leaking around the tunnel.
                builder.addAddress("fd00:6e6f:7261::1", 126);
                builder.addRoute("::", 0);
                TunnelState.log("[vpn] IPv6 is not tunnelled — IPv6 traffic is blocked rather than leaked");
            }
            if (Prefs.flag(this, Prefs.KEY_BYPASS_LAN, true)) {
                excludeLan(builder);
            }
            if (!envelope.dns.isEmpty()) {
                for (String server : envelope.dns.split(",")) {
                    // The control plane may send "udp://1.1.1.1" or "1.1.1.1:53"; VpnService only accepts a bare IP.
                    String trimmed = server.trim().replaceFirst("^[a-zA-Z0-9]+://", "").replaceFirst("/.*$", "");
                    if (trimmed.indexOf(':') == trimmed.lastIndexOf(':')) {
                        trimmed = trimmed.replaceFirst(":\\d+$", "");
                    }
                    if (trimmed.matches("[0-9a-fA-F:.]+")) {
                        builder.addDnsServer(trimmed);
                    } else if (!trimmed.isEmpty()) {
                        TunnelState.log("[vpn] DNS entry is not an IP address, skipped: " + trimmed);
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false);
                builder.setBlocking(true);
            }
            // Android allows either an allow-list or a deny-list, never both.
            if (!envelope.includePackages.isEmpty()) {
                for (String included : envelope.includePackages) {
                    try {
                        builder.addAllowedApplication(included);
                    } catch (android.content.pm.PackageManager.NameNotFoundException missing) {
                        TunnelState.log("[vpn] package not installed on this device, skipping: " + included);
                    }
                }
                if (!envelope.excludePackages.isEmpty()) {
                    TunnelState.log("[vpn] allow-list is active, so the exclude list is ignored (Android cannot combine them)");
                }
            } else {
                for (String excluded : envelope.excludePackages) {
                    try {
                        builder.addDisallowedApplication(excluded);
                    } catch (android.content.pm.PackageManager.NameNotFoundException missing) {
                        TunnelState.log("[vpn] package not installed on this device, skipping: " + excluded);
                    }
                }
                // The core and the control-plane client live in this process: keep them outside the TUN.
                try {
                    builder.addDisallowedApplication(getPackageName());
                } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                }
            }

            tunDescriptor = builder.establish();
            if (tunDescriptor == null) {
                fail("Android refused to establish the VPN interface (the permission request may have been denied).");
                return -1;
            }
            TunnelState.log("[vpn] TUN established · mtu " + envelope.mtu
                    + " · dns " + (envelope.dns.isEmpty() ? "core default" : envelope.dns)
                    + " · include " + envelope.includePackages.size()
                    + " · exclude " + envelope.excludePackages.size());
            return tunDescriptor.detachFd();
        } catch (Exception error) {
            fail("TUN setup failed: " + error.getMessage());
            return -1;
        }
    }

    /** Keeps private ranges outside the tunnel. VpnService.Builder.excludeRoute needs Android 13+. */
    private void excludeLan(Builder builder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            TunnelState.log("[vpn] LAN bypass is enforced by the core's routing rules on Android < 13");
            return;
        }
        String[][] ranges = {{"10.0.0.0", "8"}, {"172.16.0.0", "12"}, {"192.168.0.0", "16"}, {"169.254.0.0", "16"}};
        for (String[] range : ranges) {
            try {
                builder.excludeRoute(new android.net.IpPrefix(java.net.InetAddress.getByName(range[0]), Integer.parseInt(range[1])));
            } catch (Exception error) {
                TunnelState.log("[vpn] could not exclude " + range[0] + "/" + range[1] + ": " + error.getMessage());
            }
        }
    }

    /** Kill switch: replace the failed tunnel with an empty TUN so nothing leaves the device unprotected. */
    private boolean holdBlackhole() {
        try {
            Builder builder = new Builder()
                    .setSession("Nora Tunnel (kill switch)")
                    .setMtu(1500)
                    .addAddress("172.19.0.1", 30)
                    .addRoute("0.0.0.0", 0)
                    .addAddress("fd00:6e6f:7261::1", 126)
                    .addRoute("::", 0);
            try {
                builder.addDisallowedApplication(getPackageName());
            } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            }
            ParcelFileDescriptor hold = builder.establish();
            if (hold == null) {
                return false;
            }
            closeTun();
            tunDescriptor = hold;
            holding = true;
            return true;
        } catch (Exception error) {
            TunnelState.log("[kill-switch] could not hold traffic: " + error.getMessage());
            return false;
        }
    }

    /* -------------------------------- stats -------------------------------- */

    private void sampleTraffic() {
        if (trafficTick != null) {
            ui.removeCallbacks(trafficTick);
        }
        trafficTick = new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    return;
                }
                long[] totals = TunnelCore.trafficTotals();
                if (totals != null && totals.length >= 4) {
                    TunnelState.setTotals(totals[0], totals[1]);
                    TunnelState.setRates(totals[2], totals[3]);
                }
                String exit = TunnelCore.lastExitAddress();
                if (exit != null && !exit.isEmpty()) {
                    TunnelState.setExitAddress(exit);
                }
                updateNotification();
                ui.postDelayed(this, 1000L);
            }
        };
        ui.postDelayed(trafficTick, 1000L);
    }

    private void startReportLoop() {
        if (reportTickRunning) {
            return;
        }
        reportTickRunning = true;
        ui.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!running) {
                    reportTickRunning = false;
                    return;
                }
                io.execute(() -> pushReport(false));
                ui.postDelayed(this, REPORT_INTERVAL_MS);
            }
        }, REPORT_INTERVAL_MS);
    }

    /** Sends the numbers the core actually produced to the control plane. */
    private void pushReport(boolean finalReport) {
        if (localSession) {
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("profileId", Prefs.profileId(this));
            body.put("state", TunnelState.state());
            body.put("coreVersion", TunnelCore.version());
            body.put("rxBytes", TunnelState.rxBytes());
            body.put("txBytes", TunnelState.txBytes());
            body.put("latencyMs", TunnelCore.lastHandshakeMs());
            body.put("exitAddress", TunnelState.exitAddress());
            body.put("sessionStartedAt", TunnelState.connectedAt());
            body.put("final", finalReport);
            body.put("deviceModel", Build.MANUFACTURER + " " + Build.MODEL);
            body.put("androidVersion", Build.VERSION.RELEASE);
            JSONArray logLines = new JSONArray();
            List<String> logs = TunnelState.logsSnapshot();
            int from = Math.max(0, logs.size() - 12);
            for (int index = from; index < logs.size(); index += 1) {
                logLines.put(logs.get(index));
            }
            body.put("logTail", logLines);
            ControlPlane.report(this, body);
            TunnelState.reportResult(true, formatBytes(TunnelState.rxBytes()) + " down / " + formatBytes(TunnelState.txBytes()) + " up");
        } catch (Exception error) {
            TunnelState.reportResult(false, error.getMessage());
        }
    }

    /* ------------------------------- shutdown ------------------------------ */

    private void shutdownCore() {
        running = false;
        reportTickRunning = false;
        if (trafficTick != null) {
            ui.removeCallbacks(trafficTick);
        }
        TunnelState.setState(TunnelState.STOPPING, "Stopping the core…");
        try {
            TunnelCore.stop();
        } catch (RuntimeException error) {
            TunnelState.log("[core] stop raised: " + error.getMessage());
        }
        TunnelState.markStopped();
        closeTun();
    }

    private void shutdown() {
        if (running) {
            pushReport(true);
        }
        shutdownCore();
        TunnelState.setState(TunnelState.STOPPED, "Tunnel stopped. Traffic is no longer routed by Nora Tunnel.");
        Prefs.saveState(this, TunnelState.STOPPED);
        stopForegroundCompat();
        stopSelf();
    }

    private void closeTun() {
        holding = false;
        if (tunDescriptor != null) {
            try {
                tunDescriptor.close();
            } catch (Exception ignored) {
            }
            tunDescriptor = null;
        }
    }

    private void fail(String detail) {
        fail(detail, false);
    }

    /** @param afterTun true when a TUN/core was already in play, so the kill switch may hold traffic. */
    private void fail(String detail, boolean afterTun) {
        boolean wasRunning = running;
        running = false;
        reportTickRunning = false;
        if (trafficTick != null) {
            ui.removeCallbacks(trafficTick);
        }
        if (wasRunning) {
            try {
                TunnelCore.stop();
            } catch (RuntimeException error) {
                TunnelState.log("[core] stop raised: " + error.getMessage());
            }
            TunnelState.markStopped();
        }
        TunnelState.setState(TunnelState.ERROR, detail);
        TunnelState.log("[error] " + detail);
        if (afterTun && Prefs.flag(this, Prefs.KEY_KILL_SWITCH, false) && holdBlackhole()) {
            TunnelState.setState(TunnelState.ERROR, detail
                    + " — kill switch is blocking all traffic. Tap Disconnect to restore your connection.");
            updateNotification();
            return;
        }
        closeTun();
        stopForegroundCompat();
        stopSelf();
    }

    @Override
    public void onRevoke() {
        TunnelState.log("[vpn] Android revoked the VPN permission — tearing down the real session");
        shutdown();
    }

    @Override
    public void onDestroy() {
        instance = null;
        if (running) {
            shutdownCore();
        }
        super.onDestroy();
    }

    /* ----------------------------- notification ---------------------------- */

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null || manager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.channel_description));
        channel.setShowBadge(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, open,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Intent stopIntent = new Intent(this, NoraVpnService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        String title = "Nora Tunnel · " + TunnelState.state();
        String text = TunnelState.profileName().isEmpty()
                ? TunnelState.message()
                : String.format(Locale.US, "%s · ↓ %s ↑ %s", TunnelState.profileName(),
                formatBytes(TunnelState.rxBytes()), formatBytes(TunnelState.txBytes()));

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text + "\n" + TunnelState.message()))
                .setSmallIcon(R.drawable.ic_tile)
                .setContentIntent(contentIntent)
                .setOngoing(TunnelState.coreRunning())
                .setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_tile), "Disconnect", stopPending).build());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setVisibility(Notification.VISIBILITY_PRIVATE);
        }
        return builder.build();
    }

    private void updateNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int index = (int) (Math.log10(bytes) / Math.log10(1024));
        index = Math.max(0, Math.min(index, units.length - 1));
        return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024, index), units[index]);
    }

    @SuppressWarnings("unused")
    private List<String> packagesPlaceholder() {
        return new ArrayList<>();
    }
}
