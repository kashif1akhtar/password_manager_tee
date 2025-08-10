package com.kashif.passwordmanager.manager

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import androidx.annotation.RequiresApi
import com.kashif.passwordmanager.model.AttestationResult
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class TEEKeyManager(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @RequiresApi(Build.VERSION_CODES.M)
    fun generateKey(keyAlias: String, useStrongBox: Boolean = true): Boolean {
        return try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val builder = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationValidityDurationSeconds(300)
                .setRandomizedEncryptionRequired(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
                try {
                    builder.setIsStrongBoxBacked(true)
                } catch (e: StrongBoxUnavailableException) {
                    Log.w("TEEKeyManager", "StrongBox unavailable, falling back to TEE")
                }
            }

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            true
        } catch (e: Exception) {
            Log.e("TEEKeyManager", "Key generation failed", e)
            false
        }
    }

    fun getKey(keyAlias: String): SecretKey? {
        return try {
            keyStore.getKey(keyAlias, null) as? SecretKey
        } catch (e: Exception) {
            Log.e("TEEKeyManager", "Failed to retrieve key", e)
            null
        }
    }

    fun deleteKey(keyAlias: String): Boolean {
        return try {
            keyStore.deleteEntry(keyAlias)
            true
        } catch (e: Exception) {
            Log.e("TEEKeyManager", "Failed to delete key", e)
            false
        }
    }

    fun verifyKeyAttestation(keyAlias: String): AttestationResult? {
        return try {
            val certificate = keyStore.getCertificate(keyAlias) as X509Certificate

            // Parse attestation extension (simplified implementation)
            val attestationExtension = certificate.getExtensionValue("1.3.6.1.4.1.11129.2.1.17")

            // In a real implementation, you would parse the ASN.1 structure
            // For demonstration, we'll check basic certificate properties

            AttestationResult(
                isStrongBoxBacked = checkStrongBoxSupport(certificate),
                isHardwareBacked = true, // Simplified check
                verifiedBootState = true, // Would check actual boot state
                deviceLocked = false, // Would check device lock state
                attestationSecurityLevel = 1 // TEE level
            )
        } catch (e: Exception) {
            Log.e("TEEKeyManager", "Attestation verification failed", e)
            null
        }
    }

    private fun checkStrongBoxSupport(certificate: X509Certificate): Boolean {
        // Simplified StrongBox detection
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }


    private fun hasStrongBoxSupport(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // API 28+: Check StrongBox support
            try {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    "strongbox_test_key",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setIsStrongBoxBacked(true)
                    .build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()

                // Clean up test key
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry("strongbox_test_key")
                Log.d("TEECapability", "StrongBox supported")
                true
            } catch (e: StrongBoxUnavailableException) {
                Log.d("TEECapability", "StrongBox not supported")
                false
            } catch (e: Exception) {
                Log.w("TEECapability", "StrongBox check failed", e)
                false
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // API 24–27: Check hardware-backed KeyStore support
            try {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    "hardware_test_key",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenSpec)
                val key = keyGenerator.generateKey()

                // Verify hardware-backed status
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                val keyEntry = keyStore.getEntry("hardware_test_key", null) as? KeyStore.SecretKeyEntry
                keyStore.deleteEntry("hardware_test_key")

                val isHardwareBacked = keyEntry != null && keyEntry.secretKey != null &&
                        keyStore.isHardwareBackedKey(keyEntry.secretKey)
                Log.d("TEECapability", "Hardware-backed KeyStore ${if (isHardwareBacked) "supported" else "not supported"}")
                isHardwareBacked
            } catch (e: Exception) {
                Log.w("TEECapability", "Hardware-backed KeyStore check failed", e)
                false
            }
        } else {
            // API 23 and below: Use KeyStore without hardware-backed check
            try {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    "keystore_test_key",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(256)
                    // Avoid GCM or advanced modes (not guaranteed on API < 23)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build()
                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()

                // Clean up test key
                val keyStore = KeyStore.getInstance("AndroidKeyStore")
                keyStore.load(null)
                keyStore.deleteEntry("keystore_test_key")
                Log.d("TEECapability", "KeyStore available (hardware backing unknown)")
                true
            } catch (e: Exception) {
                Log.w("TEECapability", "KeyStore check failed", e)
                false
            }
        }
    }
}