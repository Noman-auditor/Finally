package com.nora.tunnel.data.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(context: Context) {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val prefs = EncryptedSharedPreferences.create(
        context, "nora_secure", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun get(key: String): String? = prefs.getString(key, null)
    fun redact(log: String): String = log
        .replace(Regex("password=.*"), "password=******")
        .replace(Regex("privateKey=.*"), "privateKey=******")
        .replace(Regex("token=.*"), "token=******")
}
