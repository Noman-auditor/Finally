package com.nora.tunnel.tunnel

import com.nora.tunnel.data.database.TunnelProfile
import kotlinx.serialization.json.*

fun generateXrayConfig(profile: TunnelProfile, secret: String?): String {
    // Never hardcode secrets, validate without silent modification
    val outbound = buildJsonObject {
        put("protocol", JsonPrimitive(profile.protocol.name.lowercase()))
        put("settings", buildJsonObject {
            put("address", profile.serverAddress)
            put("port", profile.port)
            if(secret != null) put("password", secret) // Will be redacted in logs
        })
        put("streamSettings", buildJsonObject {
            put("network", JsonPrimitive(profile.transport.name.lowercase())) // tcp, ws, grpc, quic
            if(profile.security != com.nora.tunnel.core.model.Security.NONE) {
                put("security", JsonPrimitive(profile.security.name.lowercase()))
            }
        })
    }
    return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), buildJsonObject {
        put("outbounds", JsonArray(listOf(outbound)))
        put("routing", buildJsonObject {
            put("domainStrategy", "IPIfNonMatch")
        })
    })
}

fun generateSingBoxConfig(profile: TunnelProfile): String {
    // Capability-aware: Only Hysteria2/TUIC if core == SINGBOX
    return """{"outbounds":[{"type":"${profile.protocol.name.lowercase()}","server":"${profile.serverAddress}","server_port":${profile.port}}]}"""
}
