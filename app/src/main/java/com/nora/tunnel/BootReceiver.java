package com.nora.tunnel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Starts the tunnel after boot only when the operator explicitly enabled it. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Prefs.bootStart(context)) {
            TunnelState.log("[boot] boot start disabled — nothing started");
            return;
        }
        if (!LocalSession.canConnect(context)) {
            TunnelState.log("[boot] no profile selected — nothing started");
            return;
        }
        if (NoraVpnService.permissionIntent(context) != null) {
            TunnelState.log("[boot] VPN permission not granted yet — open the app once and tap Connect");
            return;
        }
        TunnelState.log("[boot] starting tunnel");
        NoraVpnService.start(context);
    }
}
