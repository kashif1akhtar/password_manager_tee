package com.kashif.passwordmanager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kashif.passwordmanager.manager.SecureStorageManager
import com.kashif.passwordmanager.manager.TEEKeyManager
import com.kashif.passwordmanager.model.CredentialDatabase
import com.kashif.passwordmanager.repository.CredentialRepository
import com.kashif.passwordmanager.util.IntegrityMonitor
import com.kashif.passwordmanager.manager.SecureSessionManager
import com.kashif.passwordmanager.manager.TEEBiometricManager
import com.kashif.passwordmanager.util.TEEErrorHandler

class CredentialManagerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CredentialManagerViewModel::class.java)) {

            // Initialize dependencies
            val database = CredentialDatabase.getDatabase(context)
            val keyManager = TEEKeyManager(context)
            val storageManager = SecureStorageManager(context, keyManager, database)
            val sessionManager = SecureSessionManager(context)
            val integrityMonitor = IntegrityMonitor(context)
            val errorHandler = TEEErrorHandler()
            val biometricManager = TEEBiometricManager(context)

            val repository = CredentialRepository(
                storageManager,
                sessionManager,
                integrityMonitor,
                errorHandler
            )

            return CredentialManagerViewModel(
                repository,
                biometricManager,
                sessionManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}