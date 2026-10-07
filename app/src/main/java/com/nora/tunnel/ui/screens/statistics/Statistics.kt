package com.nora.tunnel.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nora.tunnel.data.database.ConnectionSession
import com.nora.tunnel.tunnel.TunnelManager
import com.nora.tunnel.ui.screens.home.DashboardGrid

@Composable
fun LiveDashboard() {
    val state by TunnelManager.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Live Dashboard", style = MaterialTheme.typography.headlineSmall)
        DashboardGrid(state, null)
    }
}

@Composable
fun HistoryScreen(sessions: List<ConnectionSession>, onClear: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)
        if(sessions.isEmpty()) Text("No connection history yet.", modifier = Modifier.padding(top = 8.dp))
        LazyColumn {
            items(sessions, key = { it.id }) { s ->
                Text("${s.protocol} • ${s.result} • ↓ ${s.rxBytes} B ↑ ${s.txBytes} B", modifier = Modifier.padding(top = 8.dp))
            }
        }
        if(sessions.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear") }
    }
}
