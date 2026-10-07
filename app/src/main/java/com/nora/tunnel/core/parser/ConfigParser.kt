package com.nora.tunnel.core.parser

import com.nora.tunnel.core.model.*
import com.nora.tunnel.data.database.TunnelProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class ParseResult {
    data class Success(val profile: TunnelProfile, val preview: String) : ParseResult()
    data class Failure(val field: String, val reason: String, val action: String) : ParseResult()
}

// Parse -> Validate -> Preview. Secrets (keys/UUIDs/passwords) are never put in the preview.
object ConfigParser {
    fun parse(text: String, source: String = ""): ParseResult {
        val t = text.trim()
        if(t.isEmpty()) return ParseResult.Failure("input", "Empty configuration", "Provide a configuration file or URI")
        return try {
            when {
                t.startsWith("[Interface]", ignoreCase = true) -> parseWireGuard(t)
                t.startsWith("vless://") -> parseUri(t, Protocol.VLESS)
                t.startsWith("trojan://") -> parseUri(t, Protocol.TROJAN)
                t.startsWith("vmess://") -> parseVmess(t)
                else -> ParseResult.Failure("format", "Unsupported or unrecognised configuration format", "Use a WireGuard .conf or a vless:// trojan:// vmess:// URI")
            }
        } catch(e: Exception) {
            ParseResult.Failure("parse", e.message ?: "Invalid configuration", "Check the configuration for typos")
        }
    }

    private fun success(profile: TunnelProfile): ParseResult {
        val preview = "Name: ${profile.name}\nServer: ${profile.serverAddress}:${profile.port}"
        return ParseResult.Success(profile, preview)
    }

    private fun parseWireGuard(t: String): ParseResult {
        val endpoint = t.lines()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Endpoint", ignoreCase = true) && it.contains("=") }
            ?.substringAfter("=")?.trim()
            ?: return ParseResult.Failure("Endpoint", "Missing Endpoint", "Add Endpoint = host:port")
        val host = endpoint.substringBeforeLast(":").trim('[', ']')
        val port = endpoint.substringAfterLast(":", "").toIntOrNull()
            ?: return ParseResult.Failure("Endpoint", "Invalid port in Endpoint", "Use host:port")
        return success(TunnelProfile(
            name = "WireGuard $host", protocol = Protocol.WIREGUARD, core = Core.WIREGUARD,
            transport = Transport.UDP, security = Security.NONE, serverAddress = host, port = port
        ))
    }

    private fun parseUri(t: String, protocol: Protocol): ParseResult {
        val uri = java.net.URI(t)
        val host = uri.host ?: return ParseResult.Failure("host", "Missing host", "Check the URI")
        val port = if(uri.port == -1) 443 else uri.port
        val q = (uri.rawQuery ?: "").split("&").filter { it.contains("=") }
            .associate { it.substringBefore("=") to java.net.URLDecoder.decode(it.substringAfter("="), "UTF-8") }
        val transport = when(q["type"]?.lowercase()) {
            null, "tcp" -> Transport.TCP
            "ws" -> Transport.WS
            "grpc" -> Transport.GRPC
            "h2", "http" -> Transport.H2
            "quic" -> Transport.QUIC
            else -> return ParseResult.Failure("type", "Unsupported transport '${q["type"]}'", "Use tcp, ws, grpc, h2 or quic")
        }
        val security = when(q["security"]?.lowercase()) {
            null, "none" -> Security.NONE
            "tls" -> Security.TLS
            "reality" -> Security.REALITY
            "utls" -> Security.UTLS
            else -> return ParseResult.Failure("security", "Unsupported security '${q["security"]}'", "Use none, tls or reality")
        }
        if(!CapabilityRegistry.isValid(Core.XRAY, protocol, transport)) {
            return ParseResult.Failure("type", "XRAY does not support $protocol over $transport", "Choose a supported transport")
        }
        val name = uri.fragment?.takeIf { it.isNotBlank() } ?: "$protocol $host"
        return success(TunnelProfile(
            name = name, protocol = protocol, core = Core.XRAY, transport = transport,
            security = security, serverAddress = host, port = port
        ))
    }

    private fun parseVmess(t: String): ParseResult {
        val decoded = String(java.util.Base64.getMimeDecoder().decode(t.removePrefix("vmess://")))
        val o = Json.parseToJsonElement(decoded).jsonObject
        val host = o["add"]?.jsonPrimitive?.content
            ?: return ParseResult.Failure("add", "Missing server address", "Check the vmess link")
        val port = o["port"]?.jsonPrimitive?.content?.toIntOrNull()
            ?: return ParseResult.Failure("port", "Missing or invalid port", "Check the vmess link")
        val transport = when(o["net"]?.jsonPrimitive?.content?.lowercase()) {
            null, "", "tcp" -> Transport.TCP
            "ws" -> Transport.WS
            "grpc" -> Transport.GRPC
            "h2" -> Transport.H2
            "quic" -> Transport.QUIC
            else -> return ParseResult.Failure("net", "Unsupported transport", "Use tcp, ws, grpc, h2 or quic")
        }
        if(!CapabilityRegistry.isValid(Core.XRAY, Protocol.VMESS, transport)) {
            return ParseResult.Failure("net", "XRAY does not support VMESS over $transport", "Choose a supported transport")
        }
        val security = if(o["tls"]?.jsonPrimitive?.content == "tls") Security.TLS else Security.NONE
        val name = o["ps"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "VMESS $host"
        return success(TunnelProfile(
            name = name, protocol = Protocol.VMESS, core = Core.XRAY, transport = transport,
            security = security, serverAddress = host, port = port
        ))
    }
}
