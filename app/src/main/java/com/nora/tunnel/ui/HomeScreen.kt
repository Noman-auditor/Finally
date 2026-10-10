package com.nora.tunnel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nora.tunnel.NoraVpnService
import com.nora.tunnel.TunnelState

private class HomeSnapshot(
    val state: String, val message: String, val active: Boolean,
    val rx: Long, val tx: Long, val down: Long, val up: Long
)

@Composable
fun HomeScreen(profileLabel: String, onToggle: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val listener = Runnable { tick++ }
        TunnelState.addListener(listener)
        onDispose { TunnelState.removeListener(listener) }
    }
    val snap = remember(tick) {
        HomeSnapshot(
            TunnelState.state(), TunnelState.message(), NoraVpnService.isActive(),
            TunnelState.rxBytes(), TunnelState.txBytes(), TunnelState.downlinkBps(), TunnelState.uplinkBps()
        )
    }
    val connected = snap.state == TunnelState.CONNECTED
    val stateColor = when {
        connected -> Good
        snap.state == TunnelState.ERROR -> Bad
        snap.active -> Warn
        else -> Dim
    }
    val buttonText = when {
        connected -> "DISCONNECT"
        snap.active && snap.state == TunnelState.ERROR -> "DISCONNECT"
        snap.active -> "CANCEL"
        else -> "CONNECT"
    }
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("NORA TUNNEL", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Secure. Private. Connected.", color = Dim)
        }
        item {
            Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(snap.state.uppercase(), color = stateColor, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.size(170.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (snap.active) Bad else Accent)
                    ) { Text(buttonText, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(16.dp))
                    Text(profileLabel, color = Dim)
                    Text(snap.message, color = Dim, fontSize = 12.sp)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("DOWNLOAD", bytes(snap.rx), Modifier.weight(1f))
                StatCard("UPLOAD", bytes(snap.tx), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("DOWN RATE", rate(snap.down), Modifier.weight(1f))
                StatCard("UP RATE", rate(snap.up), Modifier.weight(1f))
            }
        }
        item {
            InfoCard(
                "STATUS",
                if (connected) "Connected through the sing-box core. Counters come from the core, not from the UI."
                else "Not connected. Nothing is being routed by Nora Tunnel."
            )
        }
    }
}

private fun bytes(v: Long): String = when {
    v >= 1_000_000_000 -> "%.2f GB".format(v / 1e9)
    v >= 1_000_000 -> "%.2f MB".format(v / 1e6)
    v >= 1_000 -> "%.1f KB".format(v / 1e3)
    else -> "$v B"
}

private fun rate(v: Long): String = if (v <= 0) "0 B/s" else "${bytes(v)}/s"
