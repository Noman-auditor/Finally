package com.nora.tunnel.tunnel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class TunnelService : VpnService() {
    private var pfd: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            "CONNECT" -> {
                val profileId = intent?.getStringExtra("profileId") ?: return START_NOT_STICKY
                startForeground(1, createNotification("Connecting..."))
                scope.launch { establishTunnel(profileId) }
            }
            "DISCONNECT" -> {
                scope.launch { teardown() }
            }
        }
        return START_STICKY
    }

    private suspend fun establishTunnel(profileId: String) {
        // 1. Validate profile from Room
        // 2. Delegate to TunnelManager -> Adapter.validate().prepare().connect()
        // 3. Only AFTER adapter succeeds, establish VPN interface:
        try {
            val builder = Builder()
                .addAddress("10.8.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .setSession("Nora Tunnel")
                .setBlocking(false)
                .allowBypass()

            // Per-app routing
            // builder.addAllowedApplication("com.example.app")

            pfd = builder.establish() ?: throw IllegalStateException("VpnService not prepared - permission denied")
            TunnelManager.notifyState(TunnelState.Connected)
            updateNotification("Connected")
            // Monitor network via ConnectivityManager.NetworkCallback
        } catch (e: Exception) {
            TunnelManager.notifyState(TunnelState.Error("Connection failed", e.message))
            teardown()
        }
    }

    private suspend fun teardown() {
        TunnelManager.notifyState(TunnelState.Disconnecting)
        try { pfd?.close() } catch(e: Exception){}
        pfd = null
        TunnelManager.currentAdapter?.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        TunnelManager.notifyState(TunnelState.Disconnected)
    }

    private fun createNotification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("tunnel", "Nora Tunnel", NotificationManager.IMPORTANCE_LOW))
        return NotificationCompat.Builder(this, "tunnel")
            .setContentTitle("Nora Tunnel")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .addAction(NotificationCompat.Action.Builder(null, "Disconnect", null).build())
            .build()
    }
    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(1, createNotification(text))
    }
    override fun onRevoke() { scope.launch { teardown() }; super.onRevoke() }
}

// Inside establishTunnel(profile: TunnelProfile) val builder = Builder() .addAddress("10.8.0.2", 32) .addRoute("0.0.0.0", 0) .addDnsServer(customDns ?: "1.1.1.1") .addDnsServer("8.8.8.8") .setSession("Nora Tunnel - ${profile.name}") .setMtu(1500) .setBlocking(false) // IPv6 handling - real if(ipv6Enabled) { builder.addAddress("fd00::2", 64) builder.addRoute("::", 0) } // else: no IPv6 route = IPv6 blocked // Per-app routing - real VpnService API when(appRoutingMode) { "SELECTED" -> allowedApps.forEach { pkg -> try { builder.addAllowedApplication(pkg) } catch(e: Exception){} } "EXCLUDED" -> disallowedApps.forEach { pkg -> try { builder.addDisallowedApplication(pkg) } catch(_: Exception){} } else -> {} // All apps = default } pfd = builder.establish() ?: throw IllegalStateException("VpnService not prepared - user denied permission")




// in TunnelService.onStartCommand val nm = getSystemService(NotificationManager::class.java) TunnelNotification.createChannel(nm) startForeground(TunnelNotification.NOTIF_ID, TunnelNotification.build(this, "Connecting...", "0 B/s", "0 B/s"))
