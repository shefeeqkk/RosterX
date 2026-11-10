package com.rosterx.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rosterx.data.auth.GooglePasswordManagerHelper
import com.rosterx.data.local.UserSessionManager
import com.rosterx.domain.model.Engineer
import com.rosterx.domain.repository.RosterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val otpSent: Boolean = false,
    val otpMessage: String? = null,
    val errorMessage: String? = null,
    val loggedInEngineer: Engineer? = null,
    val lastSuccessCredentials: Pair<String, String>? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: RosterRepository,
    private val sessionManager: UserSessionManager,
    val googlePasswordManager: GooglePasswordManagerHelper
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
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginSuccess = true,
                    loggedInEngineer = engineer,
                    lastSuccessCredentials = Pair(identifier, password)
                )
            }.onFailure { error ->
                val rawMsg = error.message ?: "Authentication failed"
                val cleanMsg = when {
                    rawMsg.contains("http", ignoreCase = true) || rawMsg.contains("/api/", ignoreCase = true) -> "Invalid username or password"
                    rawMsg.contains("Failed to connect", ignoreCase = true) || rawMsg.contains("Unable to reach", ignoreCase = true) || rawMsg.contains("timeout", ignoreCase = true) -> "Unable to reach server. Please check your internet connection."
                    else -> rawMsg
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = cleanMsg
                )
            }
        }
    }

    fun completeLogin(engineer: Engineer) {
        _state.value = _state.value.copy(
            loginSuccess = false,
            lastSuccessCredentials = null,
            loggedInEngineer = null
        )
        sessionManager.setCurrentUser(engineer)
        // Immediately register FCM token for active user to enforce exclusive device binding
        val fcmToken = sessionManager.getFcmToken()
        if (!fcmToken.isNullOrBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.registerFcmToken(fcmToken)
                } catch (_: Exception) {}
            }
        }
    }

    fun resetLoginState() {
        _state.value = LoginState()
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
}
