package com.nora.tunnel

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.nora.tunnel.profile.LocalSession
import com.nora.tunnel.profile.ProfileException
import com.nora.tunnel.profile.ProfileRepository
import com.nora.tunnel.ui.Bg
import com.nora.tunnel.ui.HomeScreen
import com.nora.tunnel.ui.LogsScreen
import com.nora.tunnel.ui.NoraTheme
import com.nora.tunnel.ui.Panel
import com.nora.tunnel.ui.ProfilesScreen
import com.nora.tunnel.ui.RoutingScreen
import com.nora.tunnel.ui.SettingsScreen
import java.io.ByteArrayOutputStream

class MainActivity : ComponentActivity() {
    private val repo by lazy { ProfileRepository(applicationContext) }
    private var notice by mutableStateOf<String?>(null)
    private var profilesVersion by mutableIntStateOf(0)

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) NoraVpnService.start(this)
        else TunnelState.setState(TunnelState.ERROR, "VPN permission was denied")
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val importPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFromUri(uri)
    }
    private val exportPicker = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) exportTo(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TunnelState.setRedact(Prefs.flag(this, Prefs.KEY_REDACT, true))
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent { NoraTheme { NoraApp() } }

        // Auto-connect only on a fresh launch, and only when everything needed is already in place.
        if (savedInstanceState == null &&
            Prefs.flag(this, Prefs.KEY_AUTO_CONNECT, false) &&
            !NoraVpnService.isActive() &&
            LocalSession.canConnect(this) &&
            NoraVpnService.permissionIntent(this) == null
        ) NoraVpnService.start(this)
    }

    @Composable
    private fun NoraApp() {
        var tab by remember { mutableStateOf(Tab.HOME) }
        val profiles = remember(profilesVersion) { repo.all() }
        val activeId = remember(profilesVersion) { repo.activeId() }
        val activeName = profiles.firstOrNull { it.id == activeId }?.name

        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            profileLabel = activeName ?: if (Prefs.profileId(this@MainActivity) != 0) Prefs.profileName(this@MainActivity) else "No profile selected",
                            onToggle = ::toggleVpn
                        )
                        Tab.PROFILES -> ProfilesScreen(
                            profiles = profiles,
                            activeId = activeId,
                            notice = notice,
                            onImportFile = { importPicker.launch(arrayOf("*/*")) },
                            onImportClipboard = ::importFromClipboard,
                            onSelect = { id -> repo.select(id); profilesVersion++ },
                            onRename = { id, name -> repo.rename(id, name); profilesVersion++ },
                            onDelete = { id -> repo.delete(id); profilesVersion++ },
                            onExport = { exportPicker.launch("nora-profiles-backup.json") }
                        )
                        Tab.ROUTING -> RoutingScreen()
                        Tab.LOGS -> LogsScreen()
                        Tab.SETTINGS -> SettingsScreen(onOpenVpnSettings = ::openVpnSettings)
                    }
                }
                NavigationBar(containerColor = Panel) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, t.label) },
                            label = { Text(t.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }

    private fun toggleVpn() {
        if (NoraVpnService.isActive()) {
            NoraVpnService.stop(this)
            return
        }
        if (!LocalSession.canConnect(this)) {
            TunnelState.setState(TunnelState.ERROR, "No profile selected. Import a profile on the Profiles tab and tap it to select.")
            return
        }
        val intent = NoraVpnService.permissionIntent(this)
        if (intent != null) vpnPermission.launch(intent) else NoraVpnService.start(this)
    }

    private fun openVpnSettings() = startActivity(Intent(Settings.ACTION_VPN_SETTINGS))

    /* ------------------------------- import/export ------------------------------- */

    private fun importFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        if (text.isBlank()) { notice = "The clipboard is empty."; return }
        importText(text, "clipboard")
    }

    private fun importFromUri(uri: Uri) {
        try {
            val out = ByteArrayOutputStream()
            contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    if (out.size() > MAX_IMPORT_BYTES) throw ProfileException("File is too large (limit 1 MB)")
                }
            } ?: throw ProfileException("Could not open the file")
            importText(out.toString(Charsets.UTF_8.name()), "file")
        } catch (e: ProfileException) {
            notice = "Import failed: ${e.message}"
        } catch (e: Exception) {
            notice = "Import failed: could not read the file"
        }
    }

    private fun importText(text: String, origin: String) {
        try {
            val result = repo.importText(text, Prefs.flag(this, Prefs.KEY_STRICT, true))
            if (repo.activeId() == null) repo.select(result.imported.first().id)
            notice = "Imported ${result.imported.size} profile(s) from $origin." +
                if (result.skipped.isNotEmpty()) " Skipped ${result.skipped.size}: ${result.skipped.first()}" else ""
            TunnelState.log("[profile] imported ${result.imported.size} profile(s) from $origin")
            profilesVersion++
        } catch (e: ProfileException) {
            notice = "Import failed: ${e.message}"
            TunnelState.log("[profile] import failed: ${e.message}")
        } catch (e: Exception) {
            notice = "Import failed (${e.javaClass.simpleName})"
            TunnelState.log("[profile] import failed (${e.javaClass.simpleName})")
        }
    }

    private fun exportTo(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri, "wt")?.use { it.write(repo.exportJson().toByteArray(Charsets.UTF_8)) }
                ?: throw ProfileException("Could not open the destination")
            notice = "Backup written. It contains credentials in plain text — keep it private."
        } catch (e: Exception) {
            notice = "Export failed"
        }
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 1_000_000
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    PROFILES("Profiles", Icons.AutoMirrored.Filled.List),
    ROUTING("Routing", Icons.Default.Route),
    LOGS("Logs", Icons.Default.Description),
    SETTINGS("Settings", Icons.Default.Settings)
}
