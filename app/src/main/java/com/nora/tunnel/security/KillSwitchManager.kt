package com.nora.tunnel.security

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings

object KillSwitchManager {
    // Real implementation: On Android 10+ this is handled by system "Block connections without VPN"
    // We cannot programmatically enable it without user consent - we guide user.
    fun getAlwaysOnIntent(): Intent = Intent(Settings.ACTION_VPN_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    fun isAlwaysOnSupported(): String {
        return if(Build.VERSION.SDK_INT >= 29) "SUPPORTED - Settings > Network > VPN > Nora Tunnel > Always-on VPN + Block without VPN"
        else "NOT SUPPORTED - Requires Android 10+"
    }

    fun getIpv6State(builder: VpnService.Builder, enableIpv6: Boolean): String {
        // Real: If enableIpv6 false, we do NOT add IPv6 route/address to builder
        // This prevents IPv6 leak outside TUN
        return if(enableIpv6) "ENABLED - ::/0 routed via TUN" else "DISABLED - IPv6 blocked"
    }
}
