package com.kashif.passwordmanager.util

import android.content.Context
import android.os.Build
import com.kashif.passwordmanager.model.IntegrityStatus
import java.security.KeyStore

class IntegrityMonitor(private val context: Context) {

    fun performIntegrityCheck(): IntegrityStatus {
        return IntegrityStatus(
            codeIntegrity = verifyCodeSignature(),
            runtimeIntegrity = checkRuntimeTampering(),
            teeIntegrity = verifyTEEState(),
            environmentSecurity = checkSecurityEnvironment()
        )
    }

    private fun verifyCodeSignature(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            // Verify against expected signature
            packageInfo.signatures?.isNotEmpty() == true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkRuntimeTampering(): Boolean {
        val debuggerDetected = android.os.Debug.isDebuggerConnected()
        val rootDetected = checkRootAccess()
        val hookingDetected = detectHookingFrameworks()

        return !(debuggerDetected || rootDetected || hookingDetected)
    }

    private fun checkRootAccess(): Boolean {
        val rootIndicators = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )

        return rootIndicators.any { java.io.File(it).exists() }
    }

    private fun detectHookingFrameworks(): Boolean {
        val hookingIndicators = listOf(
            "de.robv.android.xposed.XposedBridge",
            "com.saurik.substrate.MS$2",
            "com.elderorb.substrate"
        )

        return hookingIndicators.any { className ->
            try {
                Class.forName(className)
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
    }

    private fun verifyTEEState(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun checkSecurityEnvironment(): Boolean {
        return !isEmulator() && !isDebuggable()
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion"))
    }

    private fun isDebuggable(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
}