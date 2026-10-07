package com.nora.tunnel.notification

import android.app.*
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nora.tunnel.MainActivity
import com.nora.tunnel.tunnel.TunnelService

object TunnelNotification {
    const val CHANNEL_ID = "nora_tunnel"
    const val NOTIF_ID = 1

    fun createChannel(manager: NotificationManager) {
        val channel = NotificationChannel(CHANNEL_ID, "Nora Tunnel", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows VPN connection state"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(ctx: android.content.Context, state: String, rx: String, tx: String): Notification {
        val openIntent = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val disconnectIntent = PendingIntent.getService(ctx, 1, Intent(ctx, TunnelService::class.java).apply { action = "DISCONNECT" }, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setContentTitle("Nora Tunnel")
            .setContentText(if(state == "CONNECTED") "Connected • ↓ $rx • ↑ $tx" else state)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(state == "CONNECTED")
            .setContentIntent(openIntent)
            .addAction(0, "Disconnect", disconnectIntent)
            .addAction(0, "Open App", openIntent)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}
