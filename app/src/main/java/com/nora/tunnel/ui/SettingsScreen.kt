package com.nora.tunnel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nora.tunnel.NoraSettingsStore
import com.nora.tunnel.TunnelState
import com.nora.tunnel.profile.ProfileException
import com.nora.tunnel.profile.SingBoxConfig

@Composable
fun SettingsScreen(onOpenVpnSettings: () -> Unit) {
    val context = LocalContext.current
    val store = remember { NoraSettingsStore(context) }
    var autoConnect by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.AUTO_CONNECT)) }
    var onBoot by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.CONNECT_ON_BOOT)) }
    var killSwitch by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.KILL_SWITCH)) }
    var doh by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.DNS_HTTPS, true)) }
    var strict by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.STRICT_VALIDATION, true)) }
    var redact by remember { mutableStateOf(store.getBoolean(NoraSettingsStore.REDACT_SECRETS, true)) }
    var dns by remember { mutableStateOf(store.getString(NoraSettingsStore.DNS_SERVER)) }
    var dnsError by remember { mutableStateOf<String?>(null) }
    var dnsSaved by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { ScreenHeader("Settings", "Changes apply the next time you connect") }

        item { SectionLabel("CONNECTION") }
        item {
            SwitchRow("Auto-connect on launch", "Connect when the app opens and a profile is selected", autoConnect) {
                autoConnect = it; store.setBoolean(NoraSettingsStore.AUTO_CONNECT, it)
            }
        }
        item {
            SwitchRow("Connect on boot", "Needs VPN permission already granted and a selected profile", onBoot) {
                onBoot = it; store.setBoolean(NoraSettingsStore.CONNECT_ON_BOOT, it)
            }
        }

        item { SectionLabel("KILL SWITCH") }
        item {
            SwitchRow("Block traffic if the tunnel fails", "If the core fails to start or stops, all traffic is blocked until you disconnect", killSwitch) {
                killSwitch = it; store.setBoolean(NoraSettingsStore.KILL_SWITCH, it)
            }
        }
        item {
            InfoCard(
                "SYSTEM-LEVEL PROTECTION",
                "An app cannot stop leaks while it is not running. For full protection also enable \"Always-on VPN\" and \"Block connections without VPN\" for Nora Tunnel in Android's VPN settings."
            )
        }
        item { OutlinedButton(onOpenVpnSettings, Modifier.fillMaxWidth()) { Text("OPEN ANDROID VPN SETTINGS") } }

        item { SectionLabel("DNS (LOCAL PROFILES)") }
        item {
            SwitchRow("Encrypted DNS (DoH)", "Resolve through the tunnel using DNS over HTTPS", doh) {
                doh = it; store.setBoolean(NoraSettingsStore.DNS_HTTPS, it)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    dns, { dns = it; dnsSaved = false; dnsError = null },
                    modifier = Modifier.weight(1f), singleLine = true,
                    label = { Text("DNS server (blank = 1.1.1.1)") },
                    isError = dnsError != null,
                    supportingText = { Text(dnsError ?: if (dnsSaved) "Saved" else "IP, host name or https:// URL") }
                )
                Button({
                    try {
                        SingBoxConfig.parseDns(dns, doh)
                        store.setString(NoraSettingsStore.DNS_SERVER, dns.trim()); dnsSaved = true; dnsError = null
                    } catch (e: ProfileException) { dnsError = e.message }
                }) { Text("SAVE") }
            }
        }
        item { InfoCard("DNS HIJACKING", "Local profiles always capture DNS inside the tunnel, so apps cannot resolve names outside it.") }

        item { SectionLabel("SECURITY & PRIVACY") }
        item {
            SwitchRow("Strict profile validation", "Reject profiles that turn off TLS certificate verification", strict) {
                strict = it; store.setBoolean(NoraSettingsStore.STRICT_VALIDATION, it)
            }
        }
        item {
            SwitchRow("Redact secrets in logs", "Hide links, keys, passwords and IDs from the log view and reports", redact) {
                redact = it; store.setBoolean(NoraSettingsStore.REDACT_SECRETS, it); TunnelState.setRedact(it)
            }
        }

        item { SectionLabel("ABOUT") }
        item {
            InfoCard("NORA TUNNEL", "Native Android VPN client · com.nora.tunnel\nCore: ${TunnelState.coreVersion().ifBlank { "sing-box (version shown after the first connection)" }}")
        }
    }
}
