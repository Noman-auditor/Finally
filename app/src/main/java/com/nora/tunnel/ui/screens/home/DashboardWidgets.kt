package com.nora.tunnel.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DashboardGrid(state: com.nora.tunnel.tunnel.TunnelState, profile: com.nora.tunnel.data.database.TunnelProfile?) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) { Text("Active Profile"); Text(profile?.name ?: "None") }
            GlassCard(Modifier.weight(1f)) { Text("Protocol"); Text(profile?.protocol?.name ?: "--") }
        }
        Row(Modifier.fillMaxWidth().padding(top=8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassCard(Modifier.weight(1f)) { Text("Server"); Text(profile?.serverAddress ?: "No configured server") }
            GlassCard(Modifier.weight(1f)) { Text("Security"); Text(if(state is com.nora.tunnel.tunnel.TunnelState.Connected) "ACTIVE" else "UNKNOWN") }
        }
        // Quick Actions
        Row(Modifier.padding(top=12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick={}) { Text("Quick Connect") }
            OutlinedButton(onClick={}) { Text("Test") }
            OutlinedButton(onClick={}) { Text("Logs") }
            OutlinedButton(onClick={}) { Text("Diagnostics") }
        }
    }
}
