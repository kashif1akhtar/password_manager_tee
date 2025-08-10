package com.kashif.passwordmanager.manager

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.kashif.passwordmanager.model.BiometricStrength

class TEEBiometricManager(private val context: Context) {

    interface BiometricAuthenticationCallback {
        fun onSuccess(result: BiometricPrompt.AuthenticationResult)
        fun onError(errorMessage: String)
        fun onCancel()
    }

    fun authenticateWithTEE(
        fragmentActivity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject?,
        callback: BiometricAuthenticationCallback
    ) {
        val biometricPrompt = BiometricPrompt(
            fragmentActivity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    // Verify the operation was performed in TEE
                    if (cryptoObject != null && verifyTEEOperation(result.cryptoObject)) {
                        callback.onSuccess(result)
                    } else if (cryptoObject == null) {
                        callback.onSuccess(result)
                    } else {
                        callback.onError("TEE verification failed")
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        callback.onCancel()
                    } else {
                        callback.onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.onError("Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Authentication")
            .setSubtitle("Authenticate using hardware-backed biometrics")
            .setDescription("Use your biometric credential to access your secure credentials")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun verifyTEEOperation(cryptoObject: BiometricPrompt.CryptoObject?): Boolean {
        // Verify that the cryptographic operation was performed in TEE
        // This is a simplified verification - in practice, you'd check attestation
        return cryptoObject?.cipher != null || cryptoObject?.signature != null || cryptoObject?.mac != null
    }

    fun checkBiometricCapability(): BiometricStrength {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStrength.STRONG
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStrength.NONE
            else -> BiometricStrength.WEAK
        }
    }


}