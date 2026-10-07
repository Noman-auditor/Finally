package com.nora.tunnel.tunnel

import com.nora.tunnel.data.database.TunnelProfile
import com.nora.tunnel.domain.usecase.ValidateProfileUseCase
import com.nora.tunnel.domain.usecase.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Honest adapters: validation is real; the engine binaries are not bundled yet,
// so connect() reports that instead of pretending to connect.
abstract class BaseTunnelAdapter(private val engine: String) : TunnelAdapter {
    private val _state = MutableStateFlow<TunnelState>(TunnelState.Idle)
    private val _stats = MutableStateFlow<TunnelStats?>(null)

    override suspend fun validate(profile: TunnelProfile): Result<Unit> =
        when(val r = ValidateProfileUseCase().invoke(profile)) {
            is ValidationResult.Ok -> Result.success(Unit)
            is ValidationResult.Fail -> Result.failure(IllegalArgumentException("${r.field}: ${r.reason}"))
        }

    override suspend fun prepare(profile: TunnelProfile): Result<Unit> = Result.success(Unit)

    override suspend fun connect(profile: TunnelProfile): Result<Unit> =
        Result.failure(IllegalStateException("$engine engine is not bundled in this build"))

    override suspend fun disconnect() { _state.value = TunnelState.Disconnected }

    override fun state(): StateFlow<TunnelState> = _state
    override fun statistics(): StateFlow<TunnelStats?> = _stats

    override suspend fun diagnostics(): Diagnostics =
        Diagnostics(mapOf("engine" to engine, "bundled" to "no"))
}

class WireGuardAdapter : BaseTunnelAdapter("WireGuard")
class OpenVpnAdapter : BaseTunnelAdapter("OpenVPN")
class XrayAdapter : BaseTunnelAdapter("Xray")
class SingBoxAdapter : BaseTunnelAdapter("sing-box")
class SshAdapter : BaseTunnelAdapter("SSH")
