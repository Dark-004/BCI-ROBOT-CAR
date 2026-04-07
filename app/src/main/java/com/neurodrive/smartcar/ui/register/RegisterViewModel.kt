package com.neurodrive.smartcar.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RegisterViewModel"
    }

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    // ════════════════════════════════════════
    //  Field Updates
    // ════════════════════════════════════════

    fun updateFullName(value: String) {
        _uiState.update {
            it.copy(
                fullName = value,
                fullNameError = validateFullName(value),
                generalError = null
            )
        }
    }

    fun updateUsername(value: String) {
        val cleaned = value.lowercase().trim()
        _uiState.update {
            it.copy(
                username = cleaned,
                usernameError = validateUsername(cleaned),
                usernameAvailable = null,
                generalError = null
            )
        }

        // Debounced username availability check
        if (cleaned.length >= 3 && validateUsername(cleaned) == null) {
            checkUsernameAvailability(cleaned)
        }
    }

    fun updateEmail(value: String) {
        _uiState.update {
            it.copy(
                email = value.trim(),
                emailError = validateEmail(value.trim()),
                // ✅ Reset verification when email changes
                isEmailVerified = false,
                emailVerificationMessage = null,
                generalError = null
            )
        }
    }

    fun updatePhoneNumber(value: String) {
        _uiState.update {
            it.copy(
                phoneNumber = value,
                phoneNumberError = validatePhoneNumber(value),
                generalError = null
            )
        }
    }

    fun updateRole(role: UserRole) {
        _uiState.update {
            it.copy(
                selectedRole = role,
                roleError = null,
                generalError = null
            )
        }
    }

    fun updatePassword(value: String) {
        val strength = calculatePasswordStrength(value)
        val requirements = checkPasswordRequirements(value)
        _uiState.update {
            it.copy(
                password = value,
                passwordError = validatePassword(value),
                passwordStrength = strength,
                passwordRequirements = requirements,
                confirmPasswordError = if (it.confirmPassword.isNotEmpty())
                    validateConfirmPassword(it.confirmPassword, value)
                else null,
                generalError = null
            )
        }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = validateConfirmPassword(value, it.password),
                generalError = null
            )
        }
    }

    fun updateTermsAccepted(accepted: Boolean) {
        _uiState.update {
            it.copy(
                termsAccepted = accepted,
                termsError = if (!accepted) "You must accept the terms" else null,
                generalError = null
            )
        }
    }

    // ════════════════════════════════════════
    //  Email Verification (OTP Flow)
    // ════════════════════════════════════════

    /**
     * Called when user presses "Verify Email" button
     * Sends OTP and navigates to verification screen
     */
    fun sendVerificationEmail() {
        val email = _uiState.value.email
        val emailError = validateEmail(email)

        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSendingVerification = true,
                    emailVerificationMessage = null,
                    generalError = null
                )
            }

            try {
                val result = firebaseAuthRepository.sendOtpToEmail(email)

                result.fold(
                    onSuccess = { otp ->
                        Log.d(TAG, "✅ OTP sent for: $email")
                        _uiState.update {
                            it.copy(
                                isSendingVerification = false,
                                emailVerificationMessage = "Verification code sent!",
                                // ✅ THIS triggers navigation to verification screen
                                shouldNavigateToVerification = true
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ Failed to send OTP", exception)
                        _uiState.update {
                            it.copy(
                                isSendingVerification = false,
                                emailVerificationMessage = "Failed to send: ${exception.localizedMessage}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ OTP send error", e)
                _uiState.update {
                    it.copy(
                        isSendingVerification = false,
                        emailVerificationMessage = "Something went wrong. Try again."
                    )
                }
            }
        }
    }

    /**
     * Called AFTER navigation to verification screen has occurred
     * Resets the navigation flag
     */
    fun onNavigatedToVerification() {
        Log.d(TAG, "📍 Navigated to verification screen")
        _uiState.update {
            it.copy(shouldNavigateToVerification = false)
        }
    }

    /**
     * Called when user returns from verification screen with verified email
     */
    fun onEmailVerified(email: String) {
        Log.d(TAG, "✅ Email verified callback: $email")
        _uiState.update {
            it.copy(
                email = email,
                isEmailVerified = true,
                emailError = null,
                emailVerificationMessage = "✅ Email verified successfully!"
            )
        }
    }

    /**
     * Reset email verification (when user wants to change email)
     */
    fun resetEmailVerification() {
        _uiState.update {
            it.copy(
                isEmailVerified = false,
                emailVerificationMessage = null,
                email = ""
            )
        }
    }

    // ════════════════════════════════════════
    //  Username Availability Check
    // ════════════════════════════════════════

    private fun checkUsernameAvailability(username: String) {
        usernameCheckJob?.cancel()
        usernameCheckJob = viewModelScope.launch {
            delay(500) // Debounce

            _uiState.update { it.copy(isCheckingUsername = true) }

            try {
                val result = firebaseAuthRepository.isUsernameAvailable(username)
                result.fold(
                    onSuccess = { isAvailable ->
                        _uiState.update {
                            it.copy(
                                isCheckingUsername = false,
                                usernameAvailable = isAvailable,
                                usernameError = if (!isAvailable)
                                    "Username is already taken"
                                else null
                            )
                        }
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(
                                isCheckingUsername = false,
                                usernameAvailable = null
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingUsername = false,
                        usernameAvailable = null
                    )
                }
            }
        }
    }

    // ════════════════════════════════════════
    //  Registration
    // ════════════════════════════════════════

    fun register() {
        // ✅ Don't register if already loading or already registered
        if (_uiState.value.isLoading || _uiState.value.isRegistered) {
            Log.d(TAG, "⚠️ Register called but already loading/registered")
            return
        }

        if (!validateAllFields()) {
            Log.d(TAG, "❌ Form validation failed")
            return
        }

        // ✅ Check email verification
        if (!_uiState.value.isEmailVerified) {
            _uiState.update {
                it.copy(
                    generalError = "Please verify your email address first",
                    emailError = "Email not verified"
                )
            }
            return
        }

        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    generalError = null
                )
            }

            try {
                Log.d(TAG, "🚀 Starting registration for: ${state.email}")

                val result = firebaseAuthRepository.register(
                    email = state.email,
                    password = state.password,
                    fullName = state.fullName,
                    username = state.username,
                    phoneNumber = state.phoneNumber,
                    role = state.selectedRole?.name ?: ""
                )

                result.fold(
                    onSuccess = { user ->
                        Log.d(TAG, "✅ Registration successful: ${user.uid}")

                        // Clean up OTP document
                        try {
                            firebaseAuthRepository.cleanupOtp(state.email)
                        } catch (e: Exception) {
                            Log.w(TAG, "OTP cleanup failed, non-critical", e)
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                // ✅ ONLY set isRegistered here
                                isRegistered = true,
                                generalError = null
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "❌ Registration failed", exception)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRegistered = false,
                                generalError = getReadableError(exception)
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Registration exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRegistered = false,
                        generalError = "Something went wrong. Please try again."
                    )
                }
            }
        }
    }

    // ════════════════════════════════════════
    //  Validation Functions
    // ════════════════════════════════════════

    private fun validateAllFields(): Boolean {
        val state = _uiState.value

        val fullNameError = validateFullName(state.fullName)
        val usernameError = validateUsername(state.username)
        val emailError = validateEmail(state.email)
        val phoneError = validatePhoneNumber(state.phoneNumber)
        val roleError = if (state.selectedRole == null) "Please select a role" else null
        val passwordError = validatePassword(state.password)
        val confirmError = validateConfirmPassword(state.confirmPassword, state.password)
        val termsError = if (!state.termsAccepted) "You must accept the terms" else null

        _uiState.update {
            it.copy(
                fullNameError = fullNameError,
                usernameError = usernameError,
                emailError = emailError,
                phoneNumberError = phoneError,
                roleError = roleError,
                passwordError = passwordError,
                confirmPasswordError = confirmError,
                termsError = termsError
            )
        }

        return listOf(
            fullNameError,
            usernameError,
            emailError,
            phoneError,
            roleError,
            passwordError,
            confirmError,
            termsError
        ).all { it == null }
    }

    private fun validateFullName(name: String): String? {
        return when {
            name.isBlank() -> "Full name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            name.length > 50 -> "Name must be less than 50 characters"
            !name.matches(Regex("^[a-zA-Z\\s]+$")) -> "Name can only contain letters and spaces"
            else -> null
        }
    }

    private fun validateUsername(username: String): String? {
        return when {
            username.isBlank() -> "Username is required"
            username.length < 3 -> "Username must be at least 3 characters"
            username.length > 20 -> "Username must be less than 20 characters"
            !username.matches(Regex("^[a-z0-9_]+$")) ->
                "Only lowercase letters, numbers, and underscores"
            username.startsWith("_") || username.endsWith("_") ->
                "Cannot start or end with underscore"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Please enter a valid email"
            else -> null
        }
    }

    private fun validatePhoneNumber(phone: String): String? {
        if (phone.isBlank()) return null // Phone is optional
        val cleaned = phone.replace(Regex("[\\s\\-()]"), "")
        return when {
            cleaned.length < 10 -> "Phone number must be at least 10 digits"
            cleaned.length > 15 -> "Phone number is too long"
            !cleaned.matches(Regex("^\\+?[0-9]+$")) -> "Invalid phone number format"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password is required"
            password.length < 8 -> "Password must be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Must contain an uppercase letter"
            !password.any { it.isLowerCase() } -> "Must contain a lowercase letter"
            !password.any { it.isDigit() } -> "Must contain a number"
            !password.any { !it.isLetterOrDigit() } -> "Must contain a special character"
            else -> null
        }
    }

    private fun validateConfirmPassword(confirm: String, password: String): String? {
        return when {
            confirm.isBlank() -> "Please confirm your password"
            confirm != password -> "Passwords don't match"
            else -> null
        }
    }

    // ════════════════════════════════════════
    //  Password Strength
    // ════════════════════════════════════════

    private fun calculatePasswordStrength(password: String): PasswordStrength {
        if (password.isEmpty()) return PasswordStrength.NONE

        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++

        return when {
            score <= 2 -> PasswordStrength.WEAK
            score <= 4 -> PasswordStrength.MEDIUM
            score <= 5 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }

    private fun checkPasswordRequirements(password: String): List<PasswordRequirement> {
        return listOf(
            PasswordRequirement(
                "At least 8 characters",
                password.length >= 8
            ),
            PasswordRequirement(
                "Contains uppercase letter",
                password.any { it.isUpperCase() }
            ),
            PasswordRequirement(
                "Contains lowercase letter",
                password.any { it.isLowerCase() }
            ),
            PasswordRequirement(
                "Contains a number",
                password.any { it.isDigit() }
            ),
            PasswordRequirement(
                "Contains special character",
                password.any { !it.isLetterOrDigit() }
            )
        )
    }

    // ════════════════════════════════════════
    //  Error Messages
    // ════════════════════════════════════════

    private fun getReadableError(exception: Throwable): String {
        return when {
            exception.message?.contains("email-already-in-use") == true ->
                "This email is already registered. Try signing in."
            exception.message?.contains("weak-password") == true ->
                "Password is too weak. Please choose a stronger one."
            exception.message?.contains("invalid-email") == true ->
                "Please enter a valid email address."
            exception.message?.contains("network") == true ->
                "Network error. Please check your connection."
            exception.message?.contains("too-many-requests") == true ->
                "Too many attempts. Please try again later."
            else -> exception.localizedMessage ?: "Registration failed. Please try again."
        }
    }
}