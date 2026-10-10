package com.nora.tunnel.profile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ImportResult(val imported: List<TunnelProfile>, val skipped: List<String>)

/**
 * Profile store. The whole list is one AES-GCM blob (see [SecretBox]); metadata and credentials
 * are never written to disk in clear and never to logs.
 */
class ProfileRepository(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("nora_profiles_v2", Context.MODE_PRIVATE)

    init { migrateLegacy() }

    fun all(): List<TunnelProfile> = synchronized(LOCK) { load() }

    fun activeId(): String? = prefs.getString(ACTIVE, null)

    fun active(): TunnelProfile? = synchronized(LOCK) {
        val id = activeId() ?: return null
        load().firstOrNull { it.id == id && it.enabled }
    }

    fun importText(text: String, strict: Boolean): ImportResult = synchronized(LOCK) {
        val parsed = TunnelProfileParser.parse(text)
        val skipped = parsed.errors.toMutableList()
        val valid = ArrayList<TunnelProfile>()
        for (profile in parsed.profiles) {
            try {
                SingBoxConfig.validate(profile, strict)
                valid += profile
            } catch (e: ProfileException) {
                skipped += "${profile.name}: ${e.message}"
            }
        }
        if (valid.isEmpty()) throw ProfileException(skipped.firstOrNull() ?: "Nothing to import")

        val stored = load().toMutableList()
        val result = ArrayList<TunnelProfile>()
        for (profile in valid) {
            val existing = stored.firstOrNull { it.rawConfig == profile.rawConfig }
            if (existing != null) {
                result += existing
            } else {
                stored += profile
                result += profile
            }
        }
        save(stored)
        ImportResult(result, skipped)
    }

    fun select(id: String?) = synchronized(LOCK) {
        val editor = prefs.edit()
        if (id == null) editor.remove(ACTIVE) else editor.putString(ACTIVE, id)
        check(editor.commit()) { "Could not save selection" }
    }

    fun delete(id: String) = synchronized(LOCK) {
        save(load().filterNot { it.id == id })
        if (activeId() == id) select(null)
    }

    fun rename(id: String, name: String) = synchronized(LOCK) {
        val clean = name.trim()
        if (clean.isNotEmpty()) save(load().map { if (it.id == id) it.copy(name = clean.take(80)) else it })
    }

    /** Plain-text export. It contains credentials: the UI warns before calling this. */
    fun exportJson(): String = synchronized(LOCK) { toArray(load()).toString(2) }

    /* ------------------------------ persistence ------------------------------ */

    private fun load(): List<TunnelProfile> {
        val sealed = prefs.getString(BLOB, null) ?: return emptyList()
        return try { fromArray(JSONArray(SecretBox.open(sealed))) } catch (e: Exception) { emptyList() }
    }

    private fun save(profiles: List<TunnelProfile>) {
        check(prefs.edit().putString(BLOB, SecretBox.seal(toArray(profiles).toString())).commit()) { "Could not save profiles" }
    }

    private fun toArray(profiles: List<TunnelProfile>) = JSONArray().apply {
        profiles.forEach {
            put(JSONObject().put("id", it.id).put("name", it.name).put("protocol", it.protocol)
                .put("sourceType", it.sourceType).put("rawConfig", it.rawConfig)
                .put("importedAt", it.importedAt).put("enabled", it.enabled))
        }
    }

    private fun fromArray(array: JSONArray): List<TunnelProfile> = (0 until array.length()).mapNotNull { i ->
        val o = array.optJSONObject(i) ?: return@mapNotNull null
        runCatching {
            TunnelProfile(
                id = o.getString("id"), name = o.getString("name"), protocol = o.getString("protocol"),
                sourceType = o.optString("sourceType", "uri"), rawConfig = o.getString("rawConfig"),
                importedAt = o.optLong("importedAt", System.currentTimeMillis()), enabled = o.optBoolean("enabled", true)
            )
        }.getOrNull()
    }

    /** Moves the old plaintext stores into the encrypted one, then wipes them. */
    private fun migrateLegacy() = synchronized(LOCK) {
        val legacy = app.getSharedPreferences("nora_profiles_v1", Context.MODE_PRIVATE)
        val old = legacy.getString("profiles", null)
        if (old != null) {
            try {
                val stored = load().toMutableList()
                val oldArray = JSONArray(old)
                for (i in 0 until oldArray.length()) {
                    val o = oldArray.optJSONObject(i) ?: continue
                    val parsed = runCatching { TunnelProfileParser.parse(o.getString("rawConfig")) }.getOrNull() ?: continue
                    parsed.profiles.forEachIndexed { index, p ->
                        if (stored.none { it.rawConfig == p.rawConfig }) {
                            stored += if (index == 0) p.copy(id = o.optString("id", p.id), name = o.optString("name", p.name)) else p
                        }
                    }
                }
                save(stored)
                legacy.getString("active_profile_id", null)?.let { select(it) }
                legacy.edit().clear().commit()
            } catch (e: Exception) {
                // Keep the legacy data untouched if migration fails; nothing is lost.
            }
        }
        // An earlier build copied the last imported file in clear text. Remove it.
        app.getSharedPreferences("nora_imports", Context.MODE_PRIVATE).edit().clear().apply()
    }

    private companion object {
        val LOCK = Any()
        const val BLOB = "blob"
        const val ACTIVE = "active_profile_id"
    }
}
