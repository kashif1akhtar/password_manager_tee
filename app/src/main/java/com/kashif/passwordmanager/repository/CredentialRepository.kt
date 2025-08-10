package com.kashif.passwordmanager.repository

import com.kashif.passwordmanager.manager.SecureStorageManager
import com.kashif.passwordmanager.model.Credential
import com.kashif.passwordmanager.model.TEEError
import com.kashif.passwordmanager.util.IntegrityMonitor
import com.kashif.passwordmanager.manager.SecureSessionManager
import com.kashif.passwordmanager.util.TEEErrorHandler

class CredentialRepository(
    private val storageManager: SecureStorageManager,
    private val sessionManager: SecureSessionManager,
    private val integrityMonitor: IntegrityMonitor,
    private val errorHandler: TEEErrorHandler
) {

    suspend fun storeCredential(credential: Credential): Result<String> {
        // Verify session and integrity
        if (!sessionManager.validateSession()) {
            return Result.failure(TEEError.AuthenticationFailed)
        }

        val integrityStatus = integrityMonitor.performIntegrityCheck()
        if (!integrityStatus.isValid) {
            return Result.failure(TEEError.IntegrityViolation)
        }

        sessionManager.updateActivity()
        return storageManager.storeCredential(credential)
    }

    suspend fun getCredential(credentialId: String): Result<Credential?> {
        if (!sessionManager.validateSession()) {
            return Result.failure(TEEError.AuthenticationFailed)
        }

        sessionManager.updateActivity()
        return storageManager.retrieveCredential(credentialId)
    }

    suspend fun getAllCredentials(): Result<List<Credential>> {
        if (!sessionManager.validateSession()) {
            return Result.failure(TEEError.AuthenticationFailed)
        }

        sessionManager.updateActivity()
        return storageManager.getAllCredentials()
    }

    suspend fun deleteCredential(credentialId: String): Result<Boolean> {
        if (!sessionManager.validateSession()) {
            return Result.failure(TEEError.AuthenticationFailed)
        }

        sessionManager.updateActivity()
        return storageManager.deleteCredential(credentialId)
    }
}