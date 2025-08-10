package com.kashif.passwordmanager.viewmodel

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kashif.passwordmanager.model.Credential
import com.kashif.passwordmanager.repository.CredentialRepository
import com.kashif.passwordmanager.manager.SecureSessionManager
import com.kashif.passwordmanager.manager.TEEBiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CredentialManagerViewModel(
    private val repository: CredentialRepository,
    private val biometricManager: TEEBiometricManager,
    private val sessionManager: SecureSessionManager
) : ViewModel() {

    private val _credentials = MutableStateFlow<List<Credential>>(emptyList())
    val credentials: StateFlow<List<Credential>> = _credentials

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun authenticate(fragmentActivity: FragmentActivity) {
        biometricManager.authenticateWithTEE(
            fragmentActivity,
            null,
            object : TEEBiometricManager.BiometricAuthenticationCallback {
                override fun onSuccess(result: BiometricPrompt.AuthenticationResult) {
                    val sessionToken = sessionManager.createSession("user_default")
                    _isAuthenticated.value = true
                    loadCredentials()
                }

                override fun onError(errorMessage: String) {
                    _error.value = errorMessage
                    _isAuthenticated.value = false
                }

                override fun onCancel() {
                    _isAuthenticated.value = false
                }
            }
        )
    }

    fun loadCredentials() {
        viewModelScope.launch {
            _loading.value = true
            repository.getAllCredentials()
                .onSuccess { credentialList ->
                    _credentials.value = credentialList
                    _error.value = null
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            _loading.value = false
        }
    }

    fun addCredential(credential: Credential) {
        viewModelScope.launch {
            _loading.value = true
            repository.storeCredential(credential)
                .onSuccess {
                    loadCredentials()
                    _error.value = null
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            _loading.value = false
        }
    }

    fun deleteCredential(credentialId: String) {
        viewModelScope.launch {
            _loading.value = true
            repository.deleteCredential(credentialId)
                .onSuccess { deleted ->
                    if (deleted) {
                        loadCredentials()
                        _error.value = null
                    } else {
                        _error.value = "Failed to delete credential"
                    }
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            _loading.value = false
        }
    }

    fun logout() {
        sessionManager.invalidateSession()
        _isAuthenticated.value = false
        _credentials.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }
}