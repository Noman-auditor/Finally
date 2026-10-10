package com.nora.tunnel.profile

import android.content.Context
import com.nora.tunnel.ControlPlane
import com.nora.tunnel.Prefs

/** Bridge used by the Java service: turns the selected local profile into the same Envelope the control plane produced. */
object LocalSession {
    @JvmStatic
    fun hasActiveProfile(context: Context): Boolean = ProfileRepository(context).active() != null

    /** True when there is anything to connect to: a local profile or a control-plane selection. */
    @JvmStatic
    fun canConnect(context: Context): Boolean = hasActiveProfile(context) || Prefs.profileId(context) != 0

    @JvmStatic
    fun buildEnvelope(context: Context): ControlPlane.Envelope {
        val profile = ProfileRepository(context).active() ?: throw ProfileException("No profile selected")
        val ipv6 = Prefs.flag(context, Prefs.KEY_IPV6, true)
        val options = BuildOptions(
            strict = Prefs.flag(context, Prefs.KEY_STRICT, true),
            ipv6 = ipv6,
            bypassLan = Prefs.flag(context, Prefs.KEY_BYPASS_LAN, true),
            dnsServer = Prefs.text(context, Prefs.KEY_DNS_SERVER, ""),
            dnsOverHttps = Prefs.flag(context, Prefs.KEY_DNS_DOH, true)
        )
        val built = SingBoxConfig.build(profile, options)
        return ControlPlane.Envelope().apply {
            configJson = built.json
            tunAddress = SingBoxConfig.TUN_V4
            tunAddress6 = if (ipv6) SingBoxConfig.TUN_V6 else ""
            dns = SingBoxConfig.TUN_DNS
            mtu = options.mtu
            endpoint = built.endpoint
            profileName = profile.name
            warnings = built.warnings.joinToString("; ")
        }
    }
}
