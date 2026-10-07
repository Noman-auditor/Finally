package com.nora.tunnel.data

import android.content.Context
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts

class SafBackupManager(private val ctx: Context) {
    // Export: User picks file via SAF - no silent upload
    fun exportToUri(uri: Uri, json: String) {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }
    fun importFromUri(uri: Uri): String? {
        return ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
    }
}

// In Activity:
// val createDoc = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> backupManager.exportToUri(uri, json) }
// val openDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> val json = backupManager.importFromUri(uri) }
