package com.nora.tunnel.tunnel

import com.nora.tunnel.data.database.TunnelProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TunnelManager {
    private val _state = MutableStateFlow<TunnelState>(TunnelState.Idle)
    val state: StateFlow<TunnelState> = _state
    var currentAdapter: TunnelAdapter? = null
        private set

    fun notifyState(s: TunnelState) { _state.value = s }

    suspend fun connect(profile: TunnelProfile) {
        _state.value = TunnelState.Validating
        val adapter = resolveAdapter(profile) ?: run {
            _state.value = TunnelState.Error("Not available in this build", "${profile.core} / ${profile.protocol} not bundled")
            return
        }
        currentAdapter = adapter
        val v = adapter.validate(profile)
        if(v.isFailure) { _state.value = TunnelState.Error("Configuration Error", v.exceptionOrNull()?.message); return }
        _state.value = TunnelState.Preparing
        adapter.prepare(profile)
        _state.value = TunnelState.Connecting
        // VpnService will actually establish interface
    }

    private fun resolveAdapter(profile: TunnelProfile): TunnelAdapter? = when(profile.core) {
        com.nora.tunnel.core.model.Core.WIREGUARD -> WireGuardAdapter()
        com.nora.tunnel.core.model.Core.OPENVPN -> OpenVpnAdapter()
        com.nora.tunnel.core.model.Core.XRAY -> XrayAdapter()
        com.nora.tunnel.core.model.Core.SINGBOX -> SingBoxAdapter()
        else -> SshAdapter() // Handles SSH family
    }
}
