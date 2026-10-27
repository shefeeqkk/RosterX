package com.rosterx.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosterx.data.local.UserSessionManager
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.repository.RosterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val otpSent: Boolean = false,
    val otpMessage: String? = null,
    val errorMessage: String? = null,
    val loggedInEngineer: Engineer? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: RosterRepository,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _engineers = MutableStateFlow<List<Engineer>>(emptyList())
    val engineers: StateFlow<List<Engineer>> = _engineers.asStateFlow()

    init {
        loadEngineers()
    }

    fun loadEngineers() {
        viewModelScope.launch {
            repository.getEngineers().onSuccess {
                _engineers.value = it
            }
        }
    }

    fun checkIsAdmin(identifier: String): Boolean {
        val cleanId = identifier.trim().lowercase()
        if (cleanId.isBlank()) return false
        if (cleanId == "admin" || cleanId == "admin@rosterx.dev") return true
        val matched = _engineers.value.find { 
            it.username.trim().equals(cleanId, ignoreCase = true) ||
            it.email.trim().equals(cleanId, ignoreCase = true) ||
            (it.email.contains("@") && it.email.split("@")[0].trim().equals(cleanId, ignoreCase = true)) ||
            it.name.trim().equals(cleanId, ignoreCase = true) ||
            it.id.trim().equals(cleanId, ignoreCase = true)
        }
        return matched?.role == com.rosterx.domain.model.EngineerRole.ADMIN
    }

    fun getBaseUrl(): String = sessionManager.getBaseUrl()
    fun setBaseUrl(url: String) = sessionManager.setBaseUrl(url)

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(identifier, password)
            
            result.onSuccess { engineer ->
                sessionManager.setCurrentUser(engineer)
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    loggedInEngineer = engineer
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Authentication failed"
                )
            }
        }
    }

    fun sendOtp(identifier: String) {
        if (identifier.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "Please enter your username or email")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null, otpMessage = null)
            val result = repository.sendOtp(identifier)
            
            result.onSuccess { msg ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    otpSent = true,
                    otpMessage = msg
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to send verification code"
                )
            }
        }
    }

    fun verifyOtp(identifier: String, otp: String, newPin: String) {
        if (otp.isBlank() || newPin.isBlank()) {
            _state.value = _state.value.copy(errorMessage = "OTP code and new password are required")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val result = repository.verifyOtp(identifier, otp, newPin)
            
            result.onSuccess { msg ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    otpSent = false,
                    otpMessage = null,
                    errorMessage = null
                )
                // Log in automatically with new password
                login(identifier, newPin)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Invalid verification code"
                )
            }
        }
    }

    fun resetOtpState() {
        _state.value = _state.value.copy(otpSent = false, otpMessage = null, errorMessage = null)
    }

    fun loginWithBiometrics(identifier: String) {
        val cleanId = identifier.trim().lowercase()
        if (cleanId.isBlank()) {
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = "Please enter your username or email for biometric login."
            )
            return
        }

        if (cleanId == "admin" || cleanId == "admin@rosterx.dev") {
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = "Accounts with Admin privileges require password authentication."
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            
            // 1. Resolve engineer directly from database directory
            val engineersResult = repository.getEngineers()
            val engineers = engineersResult.getOrElse { emptyList() }
            _engineers.value = engineers
            
            val matched = engineers.find { 
                it.username.trim().equals(cleanId, ignoreCase = true) ||
                it.email.trim().equals(cleanId, ignoreCase = true) ||
                (it.email.contains("@") && it.email.split("@")[0].trim().equals(cleanId, ignoreCase = true)) ||
                it.name.trim().equals(cleanId, ignoreCase = true) ||
                it.id.trim().equals(cleanId, ignoreCase = true)
            }

            if (matched != null) {
                // Strictly block biometrics for accounts with Admin privilege
                if (matched.role == com.rosterx.domain.model.EngineerRole.ADMIN) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Accounts with Admin privileges require password authentication."
                    )
                    return@launch
                }
                sessionManager.setCurrentUser(matched)
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    loggedInEngineer = matched
                )
            } else {
                val savedUser = sessionManager.currentUser.value
                if (savedUser != null && (
                    savedUser.username.trim().equals(cleanId, ignoreCase = true) ||
                    savedUser.email.trim().equals(cleanId, ignoreCase = true) ||
                    savedUser.id.trim().equals(cleanId, ignoreCase = true)
                )) {
                    if (savedUser.role == com.rosterx.domain.model.EngineerRole.ADMIN) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            errorMessage = "Accounts with Admin privileges require password authentication."
                        )
                        return@launch
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        loginSuccess = true,
                        loggedInEngineer = savedUser
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "No account found for '$identifier'. Please verify username or use password."
                    )
                }
            }
        }
    }
}
