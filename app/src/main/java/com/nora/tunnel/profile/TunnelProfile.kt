package com.nora.tunnel.profile

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Base64
import java.util.UUID

/** Raised for any import/validation problem. Messages never contain profile contents. */
class ProfileException(message: String) : IllegalArgumentException(message)

/** A stored profile. [rawConfig] holds credentials; [toString] deliberately hides it. */
data class TunnelProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val protocol: String,
    /** uri | wireguard-conf | json-outbound | json-config */
    val sourceType: String,
    val rawConfig: String,
    val importedAt: Long = System.currentTimeMillis(),
    val enabled: Boolean = true
) {
    override fun toString(): String = "TunnelProfile(id=$id, name=$name, protocol=$protocol)"
}

data class ParseResult(val profiles: List<TunnelProfile>, val errors: List<String>)

object TunnelProfileParser {
    private val linkSchemes = mapOf(
        "vless" to "VLESS", "vmess" to "VMess", "trojan" to "Trojan",
        "ss" to "Shadowsocks", "shadowsocks" to "Shadowsocks",
        "hysteria2" to "Hysteria2", "hy2" to "Hysteria2", "tuic" to "TUIC",
        "socks" to "SOCKS", "socks5" to "SOCKS", "http" to "HTTP"
    )
    private val jsonTypes = mapOf(
        "vless" to "VLESS", "vmess" to "VMess", "trojan" to "Trojan", "shadowsocks" to "Shadowsocks",
        "hysteria2" to "Hysteria2", "tuic" to "TUIC", "socks" to "SOCKS", "http" to "HTTP"
    )

    fun parse(input: String): ParseResult {
        val text = input.removePrefix("\uFEFF").trim()
        if (text.isEmpty()) throw ProfileException("Configuration is empty")

        if (text.contains("[Interface]", ignoreCase = true) && text.contains("[Peer]", ignoreCase = true)) {
            val host = Regex("(?im)^\\s*Endpoint\\s*=\\s*(.+)$").find(text)?.groupValues?.get(1)?.trim()
                ?.removeSurrounding("[", "]")?.substringBeforeLast(':')?.trim('[', ']').orEmpty()
            val name = if (host.isBlank()) "WireGuard profile" else "WireGuard · $host"
            return ParseResult(listOf(TunnelProfile(name = name, protocol = "WireGuard", sourceType = "wireguard-conf", rawConfig = text)), emptyList())
        }
        if (text.startsWith("{")) return ParseResult(listOf(fromJsonObject(parseObject(text))), emptyList())
        if (text.startsWith("[")) return fromJsonArray(text)
        return fromLinks(text)
    }

    private fun parseObject(text: String): JSONObject =
        try { JSONObject(text) } catch (e: JSONException) { throw ProfileException("Invalid JSON") }

    private fun fromJsonObject(o: JSONObject): TunnelProfile {
        if (o.has("outbounds")) {
            val name = o.optString("name").ifBlank { "Custom sing-box config" }
            return TunnelProfile(name = name, protocol = "sing-box", sourceType = "json-config", rawConfig = o.toString())
        }
        val type = o.optString("type").lowercase()
        val label = jsonTypes[type] ?: throw ProfileException("Unsupported JSON profile type")
        val name = o.optString("name").ifBlank { o.optString("tag") }.ifBlank { "$label profile" }
        return TunnelProfile(name = name, protocol = label, sourceType = "json-outbound", rawConfig = o.toString())
    }

    private fun fromJsonArray(text: String): ParseResult {
        val array = try { JSONArray(text) } catch (e: JSONException) { throw ProfileException("Invalid JSON") }
        if (array.length() == 0) throw ProfileException("JSON profile list is empty")
        val profiles = ArrayList<TunnelProfile>()
        val errors = ArrayList<String>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            if (item == null) { errors += "Entry ${i + 1}: not an object"; continue }
            try { profiles += fromJsonObject(item) } catch (e: ProfileException) { errors += "Entry ${i + 1}: ${e.message}" }
        }
        return ParseResult(profiles, errors)
    }

    private fun fromLinks(text: String): ParseResult {
        var lines = lineList(text)
        // Subscriptions are often one base64 blob containing the link list.
        if (lines.none { schemeOf(it) in linkSchemes }) {
            val decoded = runCatching { String(decodeBase64(text.filterNot { it.isWhitespace() }), Charsets.UTF_8) }.getOrNull()
            if (decoded != null) lines = lineList(decoded)
        }
        val links = lines.filter { schemeOf(it) in linkSchemes }
        if (links.isEmpty()) throw ProfileException("Unsupported format. Import a supported protocol link, sing-box JSON, or a WireGuard .conf file.")
        val profiles = links.map { line ->
            val scheme = schemeOf(line)
            val protocol = linkSchemes.getValue(scheme)
            val fragment = line.substringAfter('#', "")
            val label = runCatching { java.net.URLDecoder.decode(fragment, "UTF-8") }.getOrDefault(fragment).trim()
            TunnelProfile(name = label.ifBlank { "$protocol profile" }, protocol = protocol, sourceType = "uri", rawConfig = line)
        }
        return ParseResult(profiles, emptyList())
    }

    private fun lineList(text: String) = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
    private fun schemeOf(line: String) = line.substringBefore("://", "").lowercase()

    internal fun decodeBase64(raw: String): ByteArray {
        val normalized = raw.trim().replace('-', '+').replace('_', '/').trimEnd('=')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        return Base64.getDecoder().decode(padded)
    }
}
