package com.nora.tunnel

import android.content.Context

/**
 * Thin Kotlin view over the same SharedPreferences file the Java service reads ([Prefs]).
 * Every key below is read by the service or the config builder; nothing here is decorative.
 */
class NoraSettingsStore(context: Context) {
    private val prefs = Prefs.prefs(context)

    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)
    fun setBoolean(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun getString(key: String, default: String = ""): String = prefs.getString(key, default) ?: default
    fun setString(key: String, value: String) { prefs.edit().putString(key, value).apply() }
    fun getStringSet(key: String): Set<String> = prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    fun setStringSet(key: String, value: Set<String>) { prefs.edit().putStringSet(key, HashSet(value)).apply() }

    companion object {
        const val AUTO_CONNECT = Prefs.KEY_AUTO_CONNECT
        const val CONNECT_ON_BOOT = Prefs.KEY_BOOT_START
        const val KILL_SWITCH = Prefs.KEY_KILL_SWITCH
        const val TUNNEL_IPV6 = Prefs.KEY_IPV6
        const val BYPASS_LAN = Prefs.KEY_BYPASS_LAN
        const val PER_APP_ROUTING = Prefs.KEY_PER_APP
        const val PER_APP_PACKAGES = Prefs.KEY_PER_APP_PACKAGES
        const val PER_APP_MODE = Prefs.KEY_PER_APP_MODE
        const val REDACT_SECRETS = Prefs.KEY_REDACT
        const val STRICT_VALIDATION = Prefs.KEY_STRICT
        const val DNS_SERVER = Prefs.KEY_DNS_SERVER
        const val DNS_HTTPS = Prefs.KEY_DNS_DOH
    }
}
