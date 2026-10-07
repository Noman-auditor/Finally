package com.nora.tunnel.ui.screens.dns

import androidx.compose.runtime.Composable
import com.nora.tunnel.ui.components.InfoScreen

@Composable
fun DnsCenter() {
    InfoScreen("DNS Center", listOf("DNS state: System DNS", "Custom DNS per profile is not configured yet."))
}
