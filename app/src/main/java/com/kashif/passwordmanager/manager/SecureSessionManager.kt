package com.kashif.passwordmanager.manager

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.UUID

class SecureSessionManager(private val context: Context) {
    private val sessionPrefs = EncryptedSharedPreferences.create(
        "secure_session_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val sessionTimeout = 5 * 60 * 1000L // 5 minutes
    private var lastActivityTime = 0L
    private var isSessionActive = false

    fun createSession(userId: String): String {
        val sessionToken = UUID.randomUUID().toString()
        lastActivityTime = System.currentTimeMillis()
        isSessionActive = true

        sessionPrefs.edit()
            .putString("session_token", sessionToken)
            .putString("user_id", userId)
            .putLong("session_start", lastActivityTime)
            .apply()

        return sessionToken
    }

    fun validateSession(): Boolean {
         if (!isSessionActive) return false

        val currentTime = System.currentTimeMillis()
        return if (currentTime - lastActivityTime < sessionTimeout) {
            lastActivityTime = currentTime
            true
        } else {
            invalidateSession()
            false
        }
    }

    fun updateActivity() {
        if (isSessionActive) {
            lastActivityTime = System.currentTimeMillis()
        }
    }

    fun invalidateSession() {
        isSessionActive = false
        sessionPrefs.edit().clear().apply()
    }

    fun getCurrentUserId(): String? {
        return if (validateSession()) {
            sessionPrefs.getString("user_id", null)
        } else null
    }
}