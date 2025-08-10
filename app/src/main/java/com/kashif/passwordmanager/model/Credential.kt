package com.kashif.passwordmanager.model

import java.util.UUID

data class Credential(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val username: String,
    val password: String,
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

// Security Models
data class TEECapabilities(
    val hasStrongBox: Boolean,
    val hasTEE: Boolean,
    val hasSecureDisplay: Boolean,
    val biometricStrength: BiometricStrength,
    val attestationLevel: AttestationLevel
)

enum class BiometricStrength {
    NONE, WEAK, STRONG
}

enum class AttestationLevel {
    NONE, SOFTWARE, TEE, STRONGBOX
}

data class AttestationResult(
    val isStrongBoxBacked: Boolean,
    val isHardwareBacked: Boolean,
    val verifiedBootState: Boolean,
    val deviceLocked: Boolean,
    val attestationSecurityLevel: Int
)

data class IntegrityStatus(
    val codeIntegrity: Boolean,
    val runtimeIntegrity: Boolean,
    val teeIntegrity: Boolean,
    val environmentSecurity: Boolean
) {
    val isValid: Boolean
        get() = codeIntegrity && runtimeIntegrity && teeIntegrity && environmentSecurity
}

sealed class SecurityLevel {
    object StrongBox : SecurityLevel()
    object TEE : SecurityLevel()
    object SoftwareHSM : SecurityLevel()
    object BasicEncryption : SecurityLevel()
}

sealed class TEEError : Exception() {
    object TEENotAvailable : TEEError()
    object KeyGenerationFailed : TEEError()
    object AttestationFailed : TEEError()
    object AuthenticationFailed : TEEError()
    object IntegrityViolation : TEEError()
}

enum class ErrorRecoveryAction {
    FallbackToSoftware,
    RetryWithBackoff,
    SecurityAlert,
    RequestReauth,
    LockApplication
}

enum class Screen {
    Security,
    Credentials
}