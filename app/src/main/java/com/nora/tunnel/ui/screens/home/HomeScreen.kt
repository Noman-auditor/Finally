package com.nora.tunnel.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nora.tunnel.tunnel.TunnelState

@Composable
fun HomeScreen(state: TunnelState, onConnect: ()->Unit, onDisconnect: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("NORA TUNNEL", style = MaterialTheme.typography.headlineMedium)
        Text("Secure. Private. Connected.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f))) {
            Column(Modifier.padding(20.dp)) {
                when(state) {
                    is TunnelState.Connected -> {
                        Text("● CONNECTED", color = MaterialTheme.colorScheme.primary)
                        Text("Latency: 48 ms • Uptime 01:42:18", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onDisconnect, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("DISCONNECT") }
                    }
                    is TunnelState.Error -> {
                        Text("● ERROR", color = MaterialTheme.colorScheme.error)
                        Text(state.reason)
                        Text(state.details ?: "", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("RETRY") }
                    }
                    is TunnelState.Connecting, is TunnelState.Validating, is TunnelState.Preparing -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("Connecting... $state")
                    }
                    else -> {
                        Text("● DISCONNECTED", color = MaterialTheme.colorScheme.outline)
                        Text("No configured server", style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().padding(top=16.dp)) { Text("CONNECT") }
                    }
                }
            }
        }
        if(state is TunnelState.Disconnected || state is TunnelState.Idle) {
            Text("Import a configuration or create your first tunnel profile.", modifier = Modifier.padding(top=16.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
