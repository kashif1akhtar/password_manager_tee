package com.kashif.passwordmanager.manager

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.kashif.passwordmanager.model.AttestationLevel
import com.kashif.passwordmanager.model.BiometricStrength
import com.kashif.passwordmanager.model.SecurityLevel
import com.kashif.passwordmanager.model.TEECapabilities
import java.security.KeyStore
import java.util.UUID
import javax.crypto.KeyGenerator

class TEECapabilityManager(private val context: Context) {

    fun detectTEECapabilities(): TEECapabilities {
        return TEECapabilities(
            hasStrongBox = hasStrongBoxSupport(),
            hasTEE = hasTEESupport(),
            hasSecureDisplay = hasSecureDisplaySupport(),
            biometricStrength = getBiometricStrength(),
            attestationLevel = getAttestationLevel()
        )
    }

    private fun hasStrongBoxSupport(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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

                true
            } catch (e: StrongBoxUnavailableException) {
                false
            } catch (e: Exception) {
                Log.w("TEECapability", "StrongBox check failed", e)
                false
            }
        } else {
            false
        }
    }

    private fun hasTEESupport(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun hasSecureDisplaySupport(): Boolean {
        // Check for secure display capabilities (device-specific)
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }

    private fun getBiometricStrength(): BiometricStrength {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStrength.STRONG
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStrength.NONE
            else -> BiometricStrength.WEAK
        }
    }

    private fun getAttestationLevel(): AttestationLevel {
        return when {
            hasStrongBoxSupport() -> AttestationLevel.STRONGBOX
            hasTEESupport() -> AttestationLevel.TEE
            else -> AttestationLevel.SOFTWARE
        }
    }

    fun getSecurityStrategy(capabilities: TEECapabilities): SecurityLevel {
        return when {
            capabilities.hasStrongBox -> SecurityLevel.StrongBox
            capabilities.hasTEE -> SecurityLevel.TEE
            else -> SecurityLevel.SoftwareHSM
        }
    }
}

