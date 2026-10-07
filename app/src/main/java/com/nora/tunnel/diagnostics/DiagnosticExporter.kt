package com.nora.tunnel.diagnostics

import android.os.Build
import com.nora.tunnel.tunnel.TunnelState

object DiagnosticExporter {
    fun export(state: TunnelState, protocol: String, core: String): String {
        // SAFE ONLY - Never include secrets
        return """
            Nora Tunnel Diagnostics
            App version: 1.0.0 (1)
            Android: ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}
            ABI: ${Build.SUPPORTED_ABIS.joinToString()}
            VPN state: $state
            Selected protocol: $protocol
            Core: $core
            Connection state: $state
            DNS state: System DNS
            Network type: ${if(state is TunnelState.Connected) "VPN" else "WIFI/MOBILE"}
            Recent errors: ${if(state is TunnelState.Error) state.reason else "None"}
            -- Redacted Log Sample --
            password=******
            privateKey=******
            token=******
        """.trimIndent()
    }
}
