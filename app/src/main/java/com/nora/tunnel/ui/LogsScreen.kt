package com.nora.tunnel.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nora.tunnel.TunnelState

@Composable
fun LogsScreen() {
    var lines by remember { mutableStateOf(TunnelState.logsSnapshot()) }
    DisposableEffect(Unit) {
        val listener = Runnable { lines = TunnelState.logsSnapshot() }
        TunnelState.addListener(listener)
        onDispose { TunnelState.removeListener(listener) }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Logs", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton({ TunnelState.clearLogs(); lines = emptyList() }) { Text("CLEAR") }
        }
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxSize(), colors = CardDefaults.cardColors(Color(0xFF080B15))) {
            LazyColumn(Modifier.padding(12.dp)) {
                items(lines) {
                    Text(it, fontSize = 11.sp, color = Color(0xFFD8DDF5), fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}
