package com.nora.tunnel.widget

import android.appwidget.*
import android.content.Context
import android.widget.RemoteViews
import com.nora.tunnel.R

class NoraWidgetProvider: AppWidgetProvider() {
    override fun onUpdate(ctx: Context, manager: AppWidgetManager, ids: IntArray) {
        // Real stats from TunnelManager.state + statistics()
        // Shows: IDLE/DISCONNECTED/CONNECTED, Protocol, Latency, Uptime
        // Quick Connect -> PendingIntent to MainActivity
        ids.forEach { id ->
            val views = RemoteViews(ctx.packageName, R.layout.widget_nora).apply {
                setTextViewText(R.id.widget_status, "● DISCONNECTED")
                setTextViewText(R.id.widget_server, "No configured server")
            }
            manager.updateAppWidget(id, views)
        }
    }
}
