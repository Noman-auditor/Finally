package com.nora.tunnel.profile

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder

data class BuildOptions(
    val strict: Boolean = true,
    val ipv6: Boolean = true,
    val bypassLan: Boolean = true,
    val dnsServer: String = "",
    val dnsOverHttps: Boolean = true,
    val mtu: Int = 9000
)

data class BuiltConfig(val json: String, val endpoint: String, val warnings: List<String>)

data class DnsSpec(val type: String, val host: String, val port: Int?, val path: String?)

private enum class Kind { OUTBOUND, ENDPOINT, FULL }

private class Proxy(val json: JSONObject, val kind: Kind, val host: String, val port: Int)

private class Link(
    val scheme: String,
    val user: String,
    val host: String,
    val port: Int?,
    val query: Map<String, String>
) {
    fun q(key: String): String? = query[key]?.takeIf { it.isNotBlank() }
    fun needPort(): Int = port ?: throw ProfileException("Missing port")

    companion object {
        fun parse(raw: String): Link {
            val scheme = raw.substringBefore("://").lowercase()
            var rest = raw.substringAfter("://").substringBefore('#')
            val queryString = rest.substringAfter('?', "")
            rest = rest.substringBefore('?')
            val authority = rest.substringBefore('/')
            val user = if (authority.contains('@')) authority.substringBeforeLast('@') else ""
            val (host, port) = splitHostPort(authority.substringAfterLast('@'))
            val query = queryString.split('&')
                .filter { it.contains('=') }
                .associate { dec(it.substringBefore('=')) to dec(it.substringAfter('=')) }
            return Link(scheme, user, host, port, query)
        }
    }
}

private const val STRICT_MESSAGE = "This profile disables certificate verification. Turn off \"Strict validation\" in Settings to allow it."

private fun dec(value: String): String =
    try { URLDecoder.decode(value, "UTF-8") } catch (e: IllegalArgumentException) { value }

private fun truthy(value: String?) = value == "1" || value.equals("true", ignoreCase = true)

private fun isIp(host: String) = host.contains(':') || host.matches(Regex("[0-9.]+"))

private fun checkHost(host: String): String {
    if (host.isBlank() || host.length > 253 || !host.matches(Regex("[A-Za-z0-9._:\\-]+"))) {
        throw ProfileException("Invalid server address")
    }
    return host
}

private fun portOf(text: String): Int? {
    if (text.isEmpty()) return null
    val port = text.toIntOrNull()
    if (port == null || port !in 1..65535) throw ProfileException("Invalid port")
    return port
}

private fun splitHostPort(value: String): Pair<String, Int?> {
    val s = value.trim()
    if (s.startsWith("[")) {
        val end = s.indexOf(']')
        if (end < 0) throw ProfileException("Invalid address")
        return checkHost(s.substring(1, end)) to portOf(s.substring(end + 1).removePrefix(":"))
    }
    if (s.count { it == ':' } > 1) throw ProfileException("IPv6 addresses must be written in brackets")
    return checkHost(s.substringBefore(':')) to portOf(s.substringAfter(':', ""))
}

/**
 * Turns a stored profile into a sing-box (1.12+ schema) document and validates it.
 * Errors are [ProfileException]s whose text never includes credentials.
 */
object SingBoxConfig {
    const val TUN_V4 = "172.19.0.1"
    const val TUN_V6 = "fdfe:dcba:9876::1"
    const val TUN_DNS = "172.19.0.2"
    private const val TAG = "proxy"
    private val vmessCiphers = setOf("auto", "aes-128-gcm", "chacha20-poly1305", "none", "zero")
    private val outboundTypes = setOf("vless", "vmess", "trojan", "shadowsocks", "hysteria2", "tuic", "socks", "http")

    fun validate(profile: TunnelProfile, strict: Boolean) {
        resolve(profile, strict, mutableListOf())
    }

    fun build(profile: TunnelProfile, options: BuildOptions): BuiltConfig {
        val warnings = mutableListOf<String>()
        val proxy = resolve(profile, options.strict, warnings)
        val root = if (proxy.kind == Kind.FULL) withTun(proxy.json, options, warnings) else assemble(proxy, options)
        return BuiltConfig(root.toString(), "${proxy.host}:${proxy.port}", warnings)
    }

    fun parseDns(input: String, overHttps: Boolean): DnsSpec {
        val s = input.trim().ifEmpty { "1.1.1.1" }
        if (s.startsWith("https://")) {
            val rest = s.removePrefix("https://")
            val (host, port) = splitHostPort(rest.substringBefore('/'))
            val path = "/" + rest.substringAfter('/', "dns-query")
            return DnsSpec("https", host, port, path)
        }
        if (s.contains("://") || s.contains('/')) throw ProfileException("DNS must be an IP address, a host name or an https:// URL")
        val host = checkHost(s)
        return if (overHttps) DnsSpec("https", host, null, "/dns-query") else DnsSpec("udp", host, null, null)
    }

    /* ----------------------------- resolution ----------------------------- */

    private fun resolve(profile: TunnelProfile, strict: Boolean, warnings: MutableList<String>): Proxy = try {
        when (profile.sourceType) {
            "wireguard-conf" -> wireguard(profile.rawConfig)
            "json-config" -> fullConfig(JSONObject(profile.rawConfig), warnings)
            "json-outbound" -> jsonOutbound(JSONObject(profile.rawConfig), strict)
            else -> link(profile.rawConfig.trim(), strict)
        }
    } catch (e: JSONException) {
        throw ProfileException("Invalid JSON structure")
    }

    private fun link(raw: String, strict: Boolean): Proxy {
        return when (raw.substringBefore("://", "").lowercase()) {
            "vless" -> vless(Link.parse(raw), strict)
            "vmess" -> vmess(raw, strict)
            "trojan" -> trojan(Link.parse(raw), strict)
            "ss", "shadowsocks" -> shadowsocks(raw)
            "hysteria2", "hy2" -> hysteria2(Link.parse(raw), strict)
            "tuic" -> tuic(Link.parse(raw), strict)
            "socks", "socks5" -> simpleProxy(Link.parse(raw), "socks", 1080)
            "http" -> simpleProxy(Link.parse(raw), "http", 8080)
            else -> throw ProfileException("Unsupported protocol")
        }
    }

    private fun base(type: String, host: String, port: Int) =
        JSONObject().put("type", type).put("tag", TAG).put("server", host).put("server_port", port)

    private fun outboundProxy(o: JSONObject) = Proxy(o, Kind.OUTBOUND, o.optString("server"), o.optInt("server_port"))

    private fun vless(l: Link, strict: Boolean): Proxy {
        if (l.user.isBlank()) throw ProfileException("Missing user ID")
        val o = base("vless", l.host, l.needPort()).put("uuid", dec(l.user)).put("packet_encoding", "xudp")
        l.q("flow")?.let { o.put("flow", it) }
        tlsFor(l, false, strict)?.let { o.put("tls", it) }
        transportFor(l)?.let { o.put("transport", it) }
        return outboundProxy(o)
    }

    private fun vmess(raw: String, strict: Boolean): Proxy {
        val payload = raw.substringAfter("://").substringBefore('#')
        if (payload.contains('@')) {
            val l = Link.parse(raw)
            val o = base("vmess", l.host, l.needPort()).put("uuid", dec(l.user)).put("security", "auto").put("alter_id", 0)
            tlsFor(l, false, strict)?.let { o.put("tls", it) }
            transportFor(l)?.let { o.put("transport", it) }
            return outboundProxy(o)
        }
        val json = try {
            JSONObject(String(TunnelProfileParser.decodeBase64(payload), Charsets.UTF_8))
        } catch (e: Exception) {
            throw ProfileException("Invalid VMess link")
        }
        val port = portOf(json.optString("port")) ?: throw ProfileException("Missing port")
        val net = json.optString("net").lowercase().let { if (it == "h2") "http" else it }
        val query = HashMap<String, String>()
        if (net.isNotEmpty()) query["type"] = net
        query["security"] = if (json.optString("tls").equals("tls", true)) "tls" else "none"
        for (key in listOf("host", "path", "sni", "alpn", "fp")) json.optString(key).takeIf { it.isNotBlank() }?.let { query[key] = it }
        if (net == "grpc") query["serviceName"] = json.optString("path")
        val l = Link("vmess", json.optString("id"), checkHost(json.optString("add")), port, query)
        if (l.user.isBlank()) throw ProfileException("Missing user ID")
        val cipher = json.optString("scy").ifBlank { "auto" }
        if (cipher !in vmessCiphers) throw ProfileException("Unsupported VMess cipher")
        val o = base("vmess", l.host, port).put("uuid", l.user).put("security", cipher).put("alter_id", json.optInt("aid", 0))
        tlsFor(l, false, strict)?.let { o.put("tls", it) }
        transportFor(l)?.let { o.put("transport", it) }
        return outboundProxy(o)
    }

    private fun trojan(l: Link, strict: Boolean): Proxy {
        if (l.user.isBlank()) throw ProfileException("Missing password")
        val o = base("trojan", l.host, l.needPort()).put("password", dec(l.user))
        tlsFor(l, true, strict)?.let { o.put("tls", it) }
        transportFor(l)?.let { o.put("transport", it) }
        return outboundProxy(o)
    }

    private fun shadowsocks(raw: String): Proxy {
        var main = raw.substringAfter("://").substringBefore('#')
        val query = main.substringAfter('?', "")
        main = main.substringBefore('?').trimEnd('/')
        if (query.contains("plugin", ignoreCase = true)) throw ProfileException("Shadowsocks plugins are not supported")
        val (credentials, hostPort) = try {
            if (main.contains('@')) {
                val info = main.substringBeforeLast('@')
                val creds = if (info.contains(':')) dec(info) else String(TunnelProfileParser.decodeBase64(info), Charsets.UTF_8)
                creds to main.substringAfterLast('@')
            } else {
                val decoded = String(TunnelProfileParser.decodeBase64(main), Charsets.UTF_8)
                decoded.substringBeforeLast('@') to decoded.substringAfterLast('@', "")
            }
        } catch (e: IllegalArgumentException) {
            throw ProfileException("Invalid Shadowsocks link")
        }
        val method = credentials.substringBefore(':')
        val password = credentials.substringAfter(':', "")
        if (method.isBlank() || password.isEmpty()) throw ProfileException("Invalid Shadowsocks credentials")
        val (host, port) = splitHostPort(hostPort)
        val o = base("shadowsocks", host, port ?: throw ProfileException("Missing port")).put("method", method).put("password", password)
        return outboundProxy(o)
    }

    private fun hysteria2(l: Link, strict: Boolean): Proxy {
        if (l.user.isBlank()) throw ProfileException("Missing password")
        val o = base("hysteria2", l.host, l.needPort()).put("password", dec(l.user))
        tlsFor(l, true, strict)?.let { o.put("tls", it) }
        l.q("obfs")?.let { type ->
            if (type != "salamander") throw ProfileException("Unsupported obfuscation")
            val pw = l.q("obfs-password") ?: throw ProfileException("Obfuscation password missing")
            o.put("obfs", JSONObject().put("type", "salamander").put("password", pw))
        }
        return outboundProxy(o)
    }

    private fun tuic(l: Link, strict: Boolean): Proxy {
        val uuid = dec(l.user.substringBefore(':'))
        val password = dec(l.user.substringAfter(':', ""))
        if (uuid.isBlank() || password.isEmpty()) throw ProfileException("Missing UUID or password")
        val o = base("tuic", l.host, l.needPort()).put("uuid", uuid).put("password", password)
        l.q("congestion_control")?.let { o.put("congestion_control", it) }
        l.q("udp_relay_mode")?.let { o.put("udp_relay_mode", it) }
        tlsFor(l, true, strict)?.let { o.put("tls", it) }
        return outboundProxy(o)
    }

    private fun simpleProxy(l: Link, type: String, defaultPort: Int): Proxy {
        val o = base(type, l.host, l.port ?: defaultPort)
        if (type == "socks") o.put("version", "5")
        if (l.user.isNotBlank()) {
            o.put("username", dec(l.user.substringBefore(':')))
            l.user.substringAfter(':', "").takeIf { it.isNotEmpty() }?.let { o.put("password", dec(it)) }
        }
        return outboundProxy(o)
    }

    private fun tlsFor(l: Link, forceTls: Boolean, strict: Boolean): JSONObject? {
        val security = (l.q("security") ?: if (forceTls) "tls" else "none").lowercase()
        if (security == "none") return null
        if (security != "tls" && security != "reality") throw ProfileException("Unsupported security mode")
        if ((truthy(l.q("allowInsecure")) || truthy(l.q("insecure"))) && strict) throw ProfileException(STRICT_MESSAGE)
        val tls = JSONObject().put("enabled", true)
        (l.q("sni") ?: l.q("peer") ?: l.host.takeUnless { isIp(it) })?.let { tls.put("server_name", it) }
        if (truthy(l.q("allowInsecure")) || truthy(l.q("insecure"))) tls.put("insecure", true)
        l.q("alpn")?.let { tls.put("alpn", JSONArray(it.split(',').map { a -> a.trim() }.filter { a -> a.isNotEmpty() })) }
        val fingerprint = l.q("fp")
        if (security == "reality") {
            val publicKey = l.q("pbk") ?: throw ProfileException("Reality profile has no public key")
            tls.put("reality", JSONObject().put("enabled", true).put("public_key", publicKey).put("short_id", l.q("sid") ?: ""))
            tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", fingerprint ?: "chrome"))
        } else if (fingerprint != null) {
            tls.put("utls", JSONObject().put("enabled", true).put("fingerprint", fingerprint))
        }
        return tls
    }

    private fun transportFor(l: Link): JSONObject? {
        if (l.q("headerType").equals("http", ignoreCase = true)) throw ProfileException("TCP HTTP header obfuscation is not supported")
        return when (val type = (l.q("type") ?: "tcp").lowercase()) {
            "tcp", "none" -> null
            "ws" -> JSONObject().put("type", "ws").put("path", l.q("path") ?: "/")
                .also { t -> l.q("host")?.let { t.put("headers", JSONObject().put("Host", it)) } }
            "grpc" -> JSONObject().put("type", "grpc").put("service_name", l.q("serviceName") ?: "")
            "http", "h2" -> JSONObject().put("type", "http").put("path", l.q("path") ?: "/")
                .also { t -> l.q("host")?.let { t.put("host", JSONArray(it.split(',').map { h -> h.trim() })) } }
            "httpupgrade" -> JSONObject().put("type", "httpupgrade").put("path", l.q("path") ?: "/")
                .also { t -> l.q("host")?.let { t.put("host", it) } }
            else -> throw ProfileException("Unsupported transport \"${type.take(16)}\"")
        }
    }

    private fun jsonOutbound(source: JSONObject, strict: Boolean): Proxy {
        val type = source.optString("type").lowercase()
        if (type !in outboundTypes) throw ProfileException("Unsupported outbound type")
        val host = checkHost(source.optString("server"))
        val port = portOf(source.optString("server_port")) ?: throw ProfileException("Missing port")
        val tls = source.optJSONObject("tls")
        if (strict && tls != null && tls.optBoolean("insecure", false)) throw ProfileException(STRICT_MESSAGE)
        val copy = JSONObject(source.toString())
        copy.remove("name")
        copy.put("tag", TAG)
        return Proxy(copy, Kind.OUTBOUND, host, port)
    }

    private fun fullConfig(source: JSONObject, warnings: MutableList<String>): Proxy {
        val outbounds = source.optJSONArray("outbounds")
        if (outbounds == null || outbounds.length() == 0) throw ProfileException("Configuration has no outbounds")
        var host = "custom"
        var port = 0
        for (i in 0 until outbounds.length()) {
            val o = outbounds.optJSONObject(i) ?: continue
            if (o.has("server")) { host = o.optString("server"); port = o.optInt("server_port"); break }
        }
        warnings += "Custom sing-box configuration is used as-is; the DNS, LAN and IPv6 settings do not modify it."
        source.remove("name")
        return Proxy(source, Kind.FULL, host, port)
    }

    private fun wireguard(text: String): Proxy {
        var section = ""
        val iface = HashMap<String, String>()
        val peers = ArrayList<HashMap<String, String>>()
        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue
            if (line.startsWith("[")) {
                section = line.trim('[', ']').lowercase()
                if (section == "peer") peers.add(HashMap())
                continue
            }
            val key = line.substringBefore('=').trim().lowercase()
            val value = line.substringAfter('=', "").trim()
            when (section) {
                "interface" -> iface[key] = value
                "peer" -> peers.last()[key] = value
            }
        }
        checkWgKey(iface["privatekey"])
        val addresses = (iface["address"] ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }
            .map { if (it.contains('/')) it else if (it.contains(':')) "$it/128" else "$it/32" }
        if (addresses.isEmpty()) throw ProfileException("WireGuard config has no Address")
        if (peers.isEmpty()) throw ProfileException("WireGuard config has no Peer")

        val peerArray = JSONArray()
        var firstHost = ""
        var firstPort = 0
        for (peer in peers) {
            checkWgKey(peer["publickey"])
            val (host, port) = splitHostPort(peer["endpoint"] ?: throw ProfileException("WireGuard peer has no Endpoint"))
            if (port == null) throw ProfileException("WireGuard endpoint has no port")
            if (firstHost.isEmpty()) { firstHost = host; firstPort = port }
            val allowed = (peer["allowedips"] ?: "0.0.0.0/0, ::/0").split(',').map { it.trim() }.filter { it.isNotEmpty() }
            val p = JSONObject().put("address", host).put("port", port).put("public_key", peer["publickey"])
                .put("allowed_ips", JSONArray(allowed))
            peer["presharedkey"]?.takeIf { it.isNotBlank() }?.let { checkWgKey(it); p.put("pre_shared_key", it) }
            peer["persistentkeepalive"]?.toIntOrNull()?.takeIf { it > 0 }?.let { p.put("persistent_keepalive_interval", it) }
            peerArray.put(p)
        }
        val endpoint = JSONObject().put("type", "wireguard").put("tag", TAG)
            .put("address", JSONArray(addresses)).put("private_key", iface["privatekey"])
            .put("mtu", iface["mtu"]?.toIntOrNull()?.takeIf { it in 576..9000 } ?: 1408)
            .put("peers", peerArray)
        return Proxy(endpoint, Kind.ENDPOINT, firstHost, firstPort)
    }

    private fun checkWgKey(key: String?) {
        val ok = key != null && runCatching { TunnelProfileParser.decodeBase64(key).size == 32 }.getOrDefault(false)
        if (!ok) throw ProfileException("WireGuard key is missing or malformed")
    }

    /* ------------------------------- assembly ------------------------------ */

    private fun tunInbound(o: BuildOptions): JSONObject {
        val addresses = JSONArray().put("$TUN_V4/30")
        if (o.ipv6) addresses.put("$TUN_V6/126")
        return JSONObject().put("type", "tun").put("tag", "tun-in").put("address", addresses)
            .put("mtu", o.mtu).put("auto_route", true).put("stack", "gvisor")
    }

    private fun routeRules(o: BuildOptions): JSONArray {
        val rules = JSONArray()
        rules.put(JSONObject().put("action", "sniff"))
        rules.put(JSONObject().put("protocol", "dns").put("action", "hijack-dns"))
        if (o.bypassLan) rules.put(JSONObject().put("ip_is_private", true).put("action", "route").put("outbound", "direct"))
        return rules
    }

    private fun assemble(proxy: Proxy, o: BuildOptions): JSONObject {
        val spec = parseDns(o.dnsServer, o.dnsOverHttps)
        val remote = JSONObject().put("type", spec.type).put("tag", "remote").put("server", spec.host).put("detour", TAG)
        spec.port?.let { remote.put("server_port", it) }
        spec.path?.let { remote.put("path", it) }
        if (!isIp(spec.host)) remote.put("domain_resolver", "bootstrap")

        // "bootstrap" only resolves the proxy server's own host name, directly, before the tunnel is up.
        val bootstrap = JSONObject().put("type", "udp").put("tag", "bootstrap").put("server", "1.1.1.1")
        val dns = JSONObject().put("servers", JSONArray().put(bootstrap).put(remote)).put("final", "remote")
        if (!o.ipv6) dns.put("strategy", "ipv4_only")

        val outbounds = JSONArray().put(JSONObject().put("type", "direct").put("tag", "direct"))
        val root = JSONObject()
            .put("log", JSONObject().put("level", "warn").put("timestamp", false))
            .put("dns", dns)
            .put("inbounds", JSONArray().put(tunInbound(o)))
        if (proxy.kind == Kind.ENDPOINT) {
            root.put("endpoints", JSONArray().put(proxy.json))
        } else {
            outbounds.put(proxy.json)
        }
        root.put("outbounds", outbounds)
        root.put(
            "route", JSONObject()
                .put("rules", routeRules(o))
                .put("final", TAG)
                .put("auto_detect_interface", true)
                .put("default_domain_resolver", JSONObject().put("server", "bootstrap"))
        )
        return root
    }

    private fun withTun(root: JSONObject, o: BuildOptions, warnings: MutableList<String>): JSONObject {
        val inbounds = root.optJSONArray("inbounds") ?: JSONArray().also { root.put("inbounds", it) }
        var hasTun = false
        for (i in 0 until inbounds.length()) {
            if (inbounds.optJSONObject(i)?.optString("type") == "tun") hasTun = true
        }
        if (!hasTun) {
            inbounds.put(tunInbound(o))
            warnings += "No tun inbound found; one was added."
        }
        return root
    }
}
