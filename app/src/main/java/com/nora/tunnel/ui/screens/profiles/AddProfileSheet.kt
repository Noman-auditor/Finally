package com.nora.tunnel.ui.screens.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nora.tunnel.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProfileSheet(onSave: (com.nora.tunnel.data.database.TunnelProfile)->Unit) {
    var name by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf(Protocol.VLESS) }
    var core by remember { mutableStateOf(Core.XRAY) }
    var transport by remember { mutableStateOf(Transport.TCP) }
    var server by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("443") }

    val availableCores = remember(protocol) {
        CapabilityRegistry.registry.filter { it.value.protocols.contains(protocol) }.keys.toList()
    }
    val availableTransports = remember(core, protocol) {
        // Only show transports valid for BOTH core and protocol
        CapabilityRegistry.registry[core]?.transports?.filter { CapabilityRegistry.isValid(core, protocol, it) } ?: emptyList()
    }

    Column(Modifier.padding(16.dp).fillMaxWidth()) {
        Text("New Profile", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = name, onValueChange = {name=it}, label = {Text("Name *")}, modifier = Modifier.fillMaxWidth().padding(top=8.dp))

        Text("Protocol", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top=12.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Protocol.values().take(5).forEachIndexed { i, p ->
                SegmentedButton(selected = protocol==p, onClick = {protocol=p}, shape = SegmentedButtonDefaults.itemShape(i,5)) { Text(p.name, maxLines=1) }
            }
        }

        Text("Core: ${availableCores.joinToString()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            availableCores.forEachIndexed { i, c ->
                SegmentedButton(selected = core==c, onClick = {
                    core=c
                    // Auto-correct transport if invalid
                    if(!CapabilityRegistry.isValid(c, protocol, transport)) transport = CapabilityRegistry.registry[c]?.transports?.first() ?: Transport.TCP
                }, shape = SegmentedButtonDefaults.itemShape(i, availableCores.size)) { Text(c.name) }
            }
        }

        Text("Transport - Only valid combos shown", style = MaterialTheme.typography.labelSmall)
        SingleChoiceSegmentedButtonRow {
            availableTransports.forEachIndexed { i, t ->
                SegmentedButton(selected = transport==t, onClick = {transport=t}, shape = SegmentedButtonDefaults.itemShape(i, availableTransports.size)) { Text(t.name) }
            }
        }
        if(availableTransports.isEmpty()) Text("✕ No valid transport for $core + $protocol", color = MaterialTheme.colorScheme.error)

        OutlinedTextField(value = server, onValueChange = {server=it}, label = {Text("Server Address *")}, modifier = Modifier.fillMaxWidth().padding(top=8.dp))
        OutlinedTextField(value = port, onValueChange = {port=it}, label = {Text("Port *")}, modifier = Modifier.fillMaxWidth())

        Button(
            enabled = name.isNotBlank() && server.isNotBlank() && port.toIntOrNull() in 1..65535 && availableTransports.isNotEmpty(),
            onClick = {
                onSave(com.nora.tunnel.data.database.TunnelProfile(
                    name=name, protocol=protocol, core=core, transport=transport,
                    security = Security.TLS, serverAddress=server, port=port.toInt()
                ))
            },
            modifier = Modifier.fillMaxWidth().padding(top=16.dp)
        ) { Text("Save Profile") }
        Text("Unsupported combos are never allowed.", style = MaterialTheme.typography.bodySmall)
    }
}
