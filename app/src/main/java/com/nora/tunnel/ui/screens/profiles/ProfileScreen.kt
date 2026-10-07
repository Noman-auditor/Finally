package com.nora.tunnel.ui.screens.profiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nora.tunnel.data.database.TunnelProfile
import com.nora.tunnel.data.repository.ProfileRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(onConnectRequest: (TunnelProfile) -> Unit, repo: ProfileRepository = koinInject()) {
    val profiles by repo.observe().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Profiles", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { adding = !adding }) { Text(if(adding) "Close" else "Add") }
            }
        }
        if(adding) {
            item {
                AddProfileSheet(onSave = { p ->
                    scope.launch { repo.save(p) }
                    adding = false
                })
            }
        }
        if(profiles.isEmpty()) {
            item { Text("No profiles yet.", modifier = Modifier.padding(top = 12.dp)) }
        }
        items(profiles, key = { it.id }) { p ->
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                    Text("${p.protocol} • ${p.core} • ${p.serverAddress}:${p.port}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onConnectRequest(p) }) { Text("Connect") }
                        TextButton(onClick = { scope.launch { repo.delete(p) } }) { Text("Delete") }
                    }
                }
            }
        }
    }
}
