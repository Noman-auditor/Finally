package com.nora.tunnel.data

import android.content.Context
import com.nora.tunnel.core.parser.ConfigParser
import com.nora.tunnel.core.parser.ParseResult
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BackupManager(private val context: Context, private val db: com.nora.tunnel.data.database.NoraDatabase) {
    suspend fun export(): String {
        val profiles = db.profileDao().observeAll().first() // Collect first
        // NEVER export secrets - export credentialRef only, secrets stay in EncryptedSharedPreferences
        return Json.encodeToString(profiles)
    }

    suspend fun import(json: String): ParseResult {
        return try {
            ConfigParser.parse(json)
        } catch(e: Exception) {
            ParseResult.Failure("import", e.message ?: "Invalid backup", "Check file")
        }
    }
}
