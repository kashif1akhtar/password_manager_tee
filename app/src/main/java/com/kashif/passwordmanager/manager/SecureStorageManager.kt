package com.kashif.passwordmanager.manager

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kashif.passwordmanager.model.BiometricStrength
import com.kashif.passwordmanager.model.Credential
import com.kashif.passwordmanager.model.CredentialDatabase
import com.kashif.passwordmanager.model.CredentialEntity
import com.kashif.passwordmanager.model.TEEError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class SecureStorageManager(
    private val context: Context,
    private val keyManager: TEEKeyManager,
    private val database: CredentialDatabase
) {
    private val credentialDao = database.credentialDao()

    suspend fun storeCredential(credential: Credential): Result<String> = withContext(Dispatchers.IO) {
        try {
            val keyAlias = "cred_key_${credential.id}"

            // Generate TEE-backed key
            if (!keyManager.generateKey(keyAlias)) {
                return@withContext Result.failure(TEEError.KeyGenerationFailed)
            }

            // Encrypt password with TEE key
            val encryptionResult = encryptWithTEEKey(keyAlias, credential.password.toByteArray())

            val credentialEntity = CredentialEntity(
                id = credential.id,
                title = credential.title,
                username = credential.username,
                encryptedPassword = encryptionResult.first,
                iv = encryptionResult.second,
                keyAlias = keyAlias,
                category = credential.category,
                createdAt = credential.createdAt,
                lastModified = credential.lastModified
            )

            credentialDao.insertCredential(credentialEntity)
            Result.success(credential.id)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to store credential", e)
            Result.failure(e)
        }
    }

    suspend fun retrieveCredential(credentialId: String): Result<Credential?> = withContext(Dispatchers.IO) {
        try {
            val entity = credentialDao.getCredentialById(credentialId)
                ?: return@withContext Result.success(null)

            // Decrypt password with TEE key
            val decryptedPassword = decryptWithTEEKey(
                entity.keyAlias,
                entity.encryptedPassword,
                entity.iv
            )

            credentialDao.incrementAccessCount(credentialId)

            val credential = Credential(
                id = entity.id,
                title = entity.title,
                username = entity.username,
                password = String(decryptedPassword, StandardCharsets.UTF_8),
                category = entity.category,
                createdAt = entity.createdAt,
                lastModified = entity.lastModified
            )

            Result.success(credential)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to retrieve credential", e)
            Result.failure(e)
        }
    }

    suspend fun getAllCredentials(): Result<List<Credential>> = withContext(Dispatchers.IO) {
        try {
            val entities = credentialDao.getAllCredentials()
            val credentials = entities.map { entity ->
                val decryptedPassword = decryptWithTEEKey(
                    entity.keyAlias,
                    entity.encryptedPassword,
                    entity.iv
                )

                Credential(
                    id = entity.id,
                    title = entity.title,
                    username = entity.username,
                    password = String(decryptedPassword, StandardCharsets.UTF_8),
                    category = entity.category,
                    createdAt = entity.createdAt,
                    lastModified = entity.lastModified
                )
            }
            Result.success(credentials)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to retrieve all credentials", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCredential(credentialId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val entity = credentialDao.getCredentialById(credentialId)
                ?: return@withContext Result.success(false)

            // Delete the TEE key
            keyManager.deleteKey(entity.keyAlias)

            // Delete from database
            credentialDao.deleteCredential(entity)
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SecureStorage", "Failed to delete credential", e)
            Result.failure(e)
        }
    }

    private fun encryptWithTEEKey(keyAlias: String, data: ByteArray): Pair<ByteArray, ByteArray> {
        val key = keyManager.getKey(keyAlias)
            ?: throw IllegalStateException("Key not found: $keyAlias")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encryptedData = cipher.doFinal(data)
        val iv = cipher.iv

        return Pair(encryptedData, iv)
    }

    private fun decryptWithTEEKey(keyAlias: String, encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val key = keyManager.getKey(keyAlias)
            ?: throw IllegalStateException("Key not found: $keyAlias")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)

        return cipher.doFinal(encryptedData)
    }


}

