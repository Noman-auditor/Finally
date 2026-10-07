package com.nora.tunnel.ui.screens.logs

import androidx.compose.runtime.Composable
import com.nora.tunnel.ui.components.InfoScreen

@Composable
fun LogCenter() {
    InfoScreen("Log Center", listOf("No logs yet. Secrets are always redacted."))
}
