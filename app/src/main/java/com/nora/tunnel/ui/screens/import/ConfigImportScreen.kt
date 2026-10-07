package com.nora.tunnel.ui.screens.import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nora.tunnel.core.parser.ConfigParser
import com.nora.tunnel.core.parser.ParseResult

@Composable
fun ConfigImportScreen(onConfirm: (com.nora.tunnel.data.database.TunnelProfile)->Unit) {
    var preview by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<ParseResult.Failure?>(null) }
    var success by remember { mutableStateOf<ParseResult.Success?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // Real: read via ContentResolver, no silent modification
            // val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText() ?: ""
            val text = "[Interface]\nAddress=10.0.0.2/32\nEndpoint=example.com:51820"
            when(val r = ConfigParser.parse(text, it.toString())) {
                is ParseResult.Success -> { success = r; preview = r.preview; error = null }
                is ParseResult.Failure -> { error = r; success = null }
            }
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // Real: ZXing QR scan -> text -> ConfigParser.parse(text)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Config Center", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.padding(top=12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { filePicker.launch(arrayOf("*/*")) }) { Text("Import File") }
            OutlinedButton(onClick = { /* launch QR scanner */ }) { Text("Scan QR") }
            OutlinedButton(onClick = { /* paste URI from clipboard */ }) { Text("Paste URI") }
        }
        Text("Supports: .ovpn, WireGuard, Xray JSON, sing-box JSON, URI, QR", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=8.dp))

        success?.let {
            Card(Modifier.fillMaxWidth().padding(top=16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Preview", style = MaterialTheme.typography.titleMedium)
                    Text(it.preview, style = MaterialTheme.typography.bodySmall)
                    Text("Protocol: ${it.profile.protocol} • Core: ${it.profile.core} • Transport: ${it.profile.transport}")
                    Button(onClick = { onConfirm(it.profile) }, modifier = Modifier.fillMaxWidth().padding(top=12.dp)) { Text("Confirm & Save") }
                }
            }
        }
        error?.let {
            Card(Modifier.fillMaxWidth().padding(top=16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Configuration Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text("Line/field: ${it.field}")
                    Text("Reason: ${it.reason}")
                    Text("Suggested action: ${it.action}")
                }
            }
        }
    }
}
