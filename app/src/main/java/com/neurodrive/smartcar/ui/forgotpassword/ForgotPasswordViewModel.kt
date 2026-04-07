package com.neurodrive.smartcar.ui.forgotpassword
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val isEmailSent: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ForgotPasswordVM"
    }

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        Log.d(TAG, "📝 Email updated: $email")
        _uiState.update {
            it.copy(email = email, errorMessage = null)
        }
    }

    fun sendResetEmail() {
        val email = _uiState.value.email.trim()

        Log.d(TAG, "════════════════════════════════════")
        Log.d(TAG, "🔑 SEND RESET EMAIL CALLED")
        Log.d(TAG, "📧 Email: '$email'")
        Log.d(TAG, "════════════════════════════════════")

        // Validation
        if (email.isBlank()) {
            Log.e(TAG, "❌ Email is blank")
            _uiState.update {
                it.copy(errorMessage = "Please enter your email address")
            }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.e(TAG, "❌ Email format invalid: $email")
            _uiState.update {
                it.copy(errorMessage = "Please enter a valid email address")
            }
            return
        }

        Log.d(TAG, "✅ Validation passed, calling Firebase...")

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {
                Log.d(TAG, "📡 Calling firebaseAuthRepository.sendPasswordResetEmail($email)")

                val result = firebaseAuthRepository.sendPasswordResetEmail(email)

                Log.d(TAG, "📡 Firebase returned: isSuccess=${result.isSuccess}, isFailure=${result.isFailure}")

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "✅ PASSWORD RESET EMAIL SENT SUCCESSFULLY to $email")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isEmailSent = true,
                                toastMessage = "Password reset email sent to $email"
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ FIREBASE ERROR: ${exception.message}", exception)
                        Log.e(TAG, "❌ Exception class: ${exception::class.java.simpleName}")

                        val errorMsg = parseFirebaseError(exception)
                        Log.e(TAG, "❌ Parsed error: $errorMsg")

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = errorMsg
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ UNEXPECTED EXCEPTION: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unexpected error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun parseFirebaseError(exception: Throwable): String {
        val message = exception.message ?: "Unknown error"
        Log.d(TAG, "🔍 Parsing error message: $message")

        return when {
            message.contains("no user record", ignoreCase = true) ||
                    message.contains("INVALID_EMAIL", ignoreCase = true) ||
                    message.contains("USER_NOT_FOUND", ignoreCase = true) ->
                "No account found with this email address"

            message.contains("badly formatted", ignoreCase = true) ->
                "Please enter a valid email address"

            message.contains("network", ignoreCase = true) ||
                    message.contains("NETWORK_ERROR", ignoreCase = true) ->
                "Network error. Check your internet connection."

            message.contains("too many requests", ignoreCase = true) ||
                    message.contains("TOO_MANY_ATTEMPTS", ignoreCase = true) ->
                "Too many attempts. Please try again in a few minutes."

            message.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "Password reset is not enabled. Contact support."

            message.contains("API key", ignoreCase = true) ->
                "App configuration error. Please reinstall the app."

            else ->
                "Failed to send reset email: $message"
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}