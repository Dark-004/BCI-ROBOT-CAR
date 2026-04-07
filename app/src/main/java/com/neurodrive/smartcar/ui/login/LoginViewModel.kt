package com.neurodrive.smartcar.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.local.LoginResult
import com.neurodrive.smartcar.data.local.SecureStorage
import com.neurodrive.smartcar.data.local.UserPreferences
import com.neurodrive.smartcar.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val userPreferences: UserPreferences,
    private val secureStorage: SecureStorage   // <-- added
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (firebaseAuthRepository.isUserLoggedIn) {
            _uiState.update { it.copy(isLoggedIn = true) }
        }
    }

    fun loginWithBiometric() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = secureStorage.authenticateWithBiometric()   // <-- fixed

            when (result) {
                is LoginResult.Success -> {
                    userPreferences.setLoggedIn(true)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
                    }
                }
                is LoginResult.Failed -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
                is LoginResult.LockedOut -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLockedOut = true,
                            lockoutSecondsRemaining = (result.remainingMs / 1000).toInt(),
                            error = "Biometric authentication locked"
                        )
                    }
                }
            }
        }
    }

    fun updateUsername(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameError = null,
                error = null
            )
        }
    }

    fun updatePassword(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                error = null
            )
        }
    }

    fun updateRememberMe(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun login() {
        val currentState = _uiState.value

        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Email is required") }
            return
        }

        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password is required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = firebaseAuthRepository.login(
                email = currentState.username,
                password = currentState.password
            )

            result.fold(
                onSuccess = { user ->
                    userPreferences.setLoggedIn(true)
                    userPreferences.setUsername(user.displayName ?: user.email ?: "")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true
                        )
                    }
                },
                onFailure = { exception ->
                    val errorMessage = when {
                        exception.message?.contains("password") == true ->
                            "Invalid email or password"
                        exception.message?.contains("network") == true ->
                            "Network error. Please check your connection."
                        else ->
                            "Login failed: ${exception.localizedMessage}"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                }
            )
        }
    }

    fun loginWithGoogle() {
        // TODO: Implement Google Sign-In
    }
}

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Int = 0,
    val attemptsRemaining: Int = 5,
    val biometricAvailable: Boolean = false
)