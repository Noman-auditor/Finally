package com.nora.tunnel.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import com.nora.tunnel.core.model.*

@Serializable
@Entity
data class TunnelProfile(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val protocol: Protocol,
    val core: Core,
    val transport: Transport,
    val security: Security,
    val serverAddress: String,
    val port: Int,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    // Secrets stored via SecureStorage reference, NOT plain
    val credentialRef: String? = null,
    val dnsJson: String? = null,
    val routingJson: String? = null
)

@Entity
data class ConnectionSession(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val profileId: String,
    val protocol: String,
    val startTime: Long,
    val endTime: Long?,
    val durationMs: Long?,
    val rxBytes: Long,
    val txBytes: Long,
    val result: String,
    val disconnectReason: String?
)
