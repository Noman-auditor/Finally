package com.nora.tunnel.ui.screens.security

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.nora.tunnel.security.KillSwitchManager
import com.nora.tunnel.ui.components.InfoScreen

@Composable
fun SecurityCenter() {
    val ctx = LocalContext.current
    InfoScreen("Security Center", listOf("Kill switch: ${KillSwitchManager.isAlwaysOnSupported()}")) {
        Button(onClick = { ctx.startActivity(KillSwitchManager.getAlwaysOnIntent()) }) { Text("Open VPN settings") }
    }
}
