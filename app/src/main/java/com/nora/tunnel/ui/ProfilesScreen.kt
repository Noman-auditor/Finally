package com.nora.tunnel.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nora.tunnel.profile.TunnelProfile

@Composable
fun ProfilesScreen(
    profiles: List<TunnelProfile>,
    activeId: String?,
    notice: String?,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit,
    onSelect: (String?) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onExport: () -> Unit
) {
    var renaming by remember { mutableStateOf<TunnelProfile?>(null) }
    var deleting by remember { mutableStateOf<TunnelProfile?>(null) }
    var confirmExport by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("Profiles", "Stored encrypted on this device") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onImportFile, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.FileOpen, null); Spacer(Modifier.width(6.dp)); Text("FILE")
                }
                Button(onClick = onImportClipboard, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentPaste, null); Spacer(Modifier.width(6.dp)); Text("PASTE")
                }
            }
        }
        if (notice != null) item { InfoCard("IMPORT", notice) }
        if (profiles.isEmpty()) {
            item {
                InfoCard(
                    "NO PROFILES YET",
                    "Copy a vless://, vmess://, trojan://, ss://, hysteria2://, tuic:// link (or a WireGuard .conf / sing-box JSON) and tap PASTE, or pick a file."
                )
            }
        }
        items(profiles, key = { it.id }) { profile ->
            val active = profile.id == activeId
            Card(
                Modifier.fillMaxWidth().clickable { onSelect(if (active) null else profile.id) },
                colors = CardDefaults.cardColors(Panel),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (active) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (active) Good else Dim)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profile.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(profile.protocol + if (active) " · active" else "", color = Dim, fontSize = 12.sp)
                    }
                    IconButton({ renaming = profile }) { Icon(Icons.Default.Edit, "Rename", tint = Dim) }
                    IconButton({ deleting = profile }) { Icon(Icons.Default.Delete, "Delete", tint = Bad) }
                }
            }
        }
        if (profiles.isNotEmpty()) {
            item {
                OutlinedButton(onClick = { confirmExport = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share, null); Spacer(Modifier.width(8.dp)); Text("EXPORT BACKUP")
                }
            }
        }
    }

    renaming?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename profile") },
            text = { OutlinedTextField(text, { text = it }, singleLine = true) },
            confirmButton = { TextButton({ onRename(target.id, text); renaming = null }) { Text("Save") } },
            dismissButton = { TextButton({ renaming = null }) { Text("Cancel") } }
        )
    }
    deleting?.let { target ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete profile?") },
            text = { Text("\"${target.name}\" will be removed from this device.") },
            confirmButton = { TextButton({ onDelete(target.id); deleting = null }) { Text("Delete", color = Bad) } },
            dismissButton = { TextButton({ deleting = null }) { Text("Cancel") } }
        )
    }
    if (confirmExport) {
        AlertDialog(
            onDismissRequest = { confirmExport = false },
            title = { Text("Export credentials?") },
            text = { Text("The backup file contains your server credentials in plain text. Store it somewhere safe and delete it when done.") },
            confirmButton = { TextButton({ confirmExport = false; onExport() }) { Text("Export") } },
            dismissButton = { TextButton({ confirmExport = false }) { Text("Cancel") } }
        )
    }
}
