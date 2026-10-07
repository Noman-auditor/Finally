package com.nora.tunnel.domain.usecase

import com.nora.tunnel.core.model.CapabilityRegistry
import com.nora.tunnel.data.database.TunnelProfile

sealed class ValidationResult {
    object Ok : ValidationResult()
    data class Fail(val field: String, val reason: String) : ValidationResult()
}

class ValidateProfileUseCase {
    operator fun invoke(p: TunnelProfile): ValidationResult {
        if(p.name.isBlank()) return ValidationResult.Fail("name", "Name is required")
        if(p.serverAddress.isBlank()) return ValidationResult.Fail("serverAddress", "Server address is required")
        if(p.port !in 1..65535) return ValidationResult.Fail("port", "Port must be 1-65535")
        if(!CapabilityRegistry.isValid(p.core, p.protocol, p.transport)) {
            return ValidationResult.Fail("transport", "${p.core} does not support ${p.protocol} over ${p.transport}")
        }
        return ValidationResult.Ok
    }
}
