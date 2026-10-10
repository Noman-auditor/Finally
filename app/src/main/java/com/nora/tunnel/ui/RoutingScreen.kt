package com.nora.tunnel.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nora.tunnel.NoraSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AppEntry(val label: String, val pkg: String)

@Composable
fun RoutingScreen() {
    val context = LocalContext.current
    val store = remember { NoraSettingsStore(context) }
    var bypassLan by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.BYPASS_LAN, true)) }
    var ipv6 by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.TUNNEL_IPV6, true)) }
    var perApp by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.PER_APP_ROUTING, false)) }
    var mode by remember { mutableStateOf(store.getString(NoraSettingsStore.PER_APP_MODE, "include")) }
    var chosen by remember { mutableStateOf(store.getStringSet(NoraSettingsStore.PER_APP_PACKAGES)) }
    var picking by remember { mutableStateOf(false) }

    if (picking) {
        AppPicker(chosen, onBack = { picking = false }) { chosen = it; store.setStringSet(NoraSettingsStore.PER_APP_PACKAGES, it) }
        return
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenHeader("Routing", "Applies the next time you connect") }
        item {
            SwitchRow("Bypass LAN", "Keep local network addresses (10.x, 172.16–31.x, 192.168.x) outside the tunnel", bypassLan) {
                bypassLan = it; store.setBoolean(NoraSettingsStore.BYPASS_LAN, it)
            }
        }
        item {
            SwitchRow("Tunnel IPv6", "When off, IPv6 is blocked instead of leaking around the VPN", ipv6) {
                ipv6 = it; store.setBoolean(NoraSettingsStore.TUNNEL_IPV6, it)
            }
        }
        item {
            SwitchRow("Per-app routing", "Choose which apps use the VPN", perApp) {
                perApp = it; store.setBoolean(NoraSettingsStore.PER_APP_ROUTING, it)
            }
        }
        if (perApp) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == "include", { mode = "include"; store.setString(NoraSettingsStore.PER_APP_MODE, "include") }, { Text("Only selected apps") })
                    FilterChip(mode == "exclude", { mode = "exclude"; store.setString(NoraSettingsStore.PER_APP_MODE, "exclude") }, { Text("All except selected") })
                }
            }
            item {
                Button({ picking = true }, Modifier.fillMaxWidth()) { Text("CHOOSE APPS (${chosen.size} selected)") }
            }
            if (chosen.isEmpty()) {
                item { InfoCard("NO APPS SELECTED", "Until you pick at least one app, all apps are routed through the VPN.", Warn) }
            }
        }
        item {
            InfoCard("ANDROID LIMIT", "Android supports either an allow-list or a deny-list of apps, never both. Nora Tunnel itself always stays outside the tunnel.")
        }
    }
}

@Composable
private fun AppPicker(selected: Set<String>, onBack: () -> Unit, onChange: (Set<String>) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(selected) }
    val apps by produceState<List<AppEntry>?>(null) {
        value = withContext(Dispatchers.Default) { loadApps(context.packageManager, context.packageName) }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Choose apps", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Search") })
        val list = apps
        if (list == null) {
            Text("Loading apps…", color = Dim, modifier = Modifier.padding(top = 16.dp))
        } else {
            val shown = list.filter { query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true) }
            LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.pkg }) { app ->
                    val on = app.pkg in current
                    val toggle = {
                        current = if (on) current - app.pkg else current + app.pkg
                        onChange(current)
                    }
                    Row(Modifier.fillMaxWidth().clickable { toggle() }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(on, { toggle() })
                        Column {
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                            Text(app.pkg, color = Dim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun loadApps(pm: PackageManager, self: String): List<AppEntry> {
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
        .map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.pkg != self }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}
