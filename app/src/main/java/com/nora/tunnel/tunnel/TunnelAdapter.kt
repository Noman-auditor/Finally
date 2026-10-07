package com.nora.tunnel.tunnel

import com.nora.tunnel.data.database.TunnelProfile
import kotlinx.coroutines.flow.StateFlow

sealed class TunnelState {
    object Idle: TunnelState()
    object Validating: TunnelState()
    object Preparing: TunnelState()
    object Connecting: TunnelState()
    object Connected: TunnelState()
    object Reconnecting: TunnelState()
    object Disconnecting: TunnelState()
    object Disconnected: TunnelState()
    data class Error(val reason: String, val details: String? = null): TunnelState()
}

data class TunnelStats(val rxBytes: Long, val txBytes: Long, val latencyMs: Long?, val uptimeMs: Long)
data class Diagnostics(val checks: Map<String, String>)

interface TunnelAdapter {
    suspend fun validate(profile: TunnelProfile): Result<Unit>
    suspend fun prepare(profile: TunnelProfile): Result<Unit>
    suspend fun connect(profile: TunnelProfile): Result<Unit>
    suspend fun disconnect()
    fun state(): StateFlow<TunnelState>
    fun statistics(): StateFlow<TunnelStats?>
    suspend fun diagnostics(): Diagnostics
}
