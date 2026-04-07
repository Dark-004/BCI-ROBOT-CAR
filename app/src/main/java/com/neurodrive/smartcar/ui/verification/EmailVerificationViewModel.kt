package com.neurodrive.smartcar.ui.verification

import androidx.lifecycle.SavedStateHandle
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
class EmailVerificationViewModel @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState: StateFlow<EmailVerificationUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var generatedOtp: String = ""

    init {
        // ✅ Get email from navigation argument
        val email = savedStateHandle.get<String>("email") ?: ""
        _uiState.update { it.copy(email = email) }

        // ✅ Auto-send OTP when screen opens
        if (email.isNotBlank()) {
            sendOtp()
        }
    }

    // ════════════════════════════════════════
    //  OTP Input Handling
    // ════════════════════════════════════════

    fun updateOtp(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.update {
                it.copy(
                    otp = otp,
                    otpError = null,
                    message = null,
                    isError = false
                )
            }

            // ✅ Auto-verify when 6 digits entered
            if (otp.length == 6) {
                verifyOtp()
            }
        }
    }

    fun clearOtp() {
        _uiState.update {
            it.copy(
                otp = "",
                otpError = null,
                message = null,
                isError = false
            )
        }
    }

    // ════════════════════════════════════════
    //  Send OTP
    // ════════════════════════════════════════

    fun sendOtp() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update {
                it.copy(
                    message = "Email address is missing",
                    isError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSending = true,
                    message = null,
                    isError = false,
                    otpError = null
                )
            }

            try {
                val result = firebaseAuthRepository.sendOtpToEmail(email)

                result.fold(
                    onSuccess = { otp ->
                        generatedOtp = otp
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                message = "✉️ Verification code sent to $email",
                                isError = false,
                                canResend = false,
                                resendTimerSeconds = 60
                            )
                        }
                        // ✅ Start cooldown timer
                        startResendTimer()
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                message = "Failed to send code: ${exception.localizedMessage}",
                                isError = true
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        message = "Something went wrong. Please try again.",
                        isError = true
                    )
                }
            }
        }
    }

    // ════════════════════════════════════════
    //  Verify OTP
    // ════════════════════════════════════════

    fun verifyOtp() {
        val enteredOtp = _uiState.value.otp

        if (enteredOtp.length != 6) {
            _uiState.update {
                it.copy(otpError = "Please enter the 6-digit code")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isVerifying = true,
                    otpError = null,
                    message = null,
                    isError = false
                )
            }

            try {
                val result = firebaseAuthRepository.verifyOtp(
                    email = _uiState.value.email,
                    otp = enteredOtp
                )

                result.fold(
                    onSuccess = { isValid ->
                        if (isValid) {
                            _uiState.update {
                                it.copy(
                                    isVerifying = false,
                                    isVerified = true,
                                    message = "✅ Email verified successfully!",
                                    isError = false,
                                    shouldNavigateBack = true
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isVerifying = false,
                                    otpError = "Invalid verification code",
                                    otp = "", // Clear invalid OTP
                                    isError = true
                                )
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                isVerifying = false,
                                otpError = "Verification failed: ${exception.localizedMessage}",
                                isError = true
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        otpError = "Something went wrong. Please try again.",
                        isError = true
                    )
                }
            }
        }
    }

    // ════════════════════════════════════════
    //  Resend OTP with Cooldown Timer
    // ════════════════════════════════════════

    fun resendOtp() {
        if (!_uiState.value.canResend) return
        clearOtp()
        sendOtp()
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var seconds = 60
            while (seconds > 0) {
                _uiState.update {
                    it.copy(
                        resendTimerSeconds = seconds,
                        canResend = false
                    )
                }
                delay(1000L)
                seconds--
            }
            _uiState.update {
                it.copy(
                    resendTimerSeconds = 0,
                    canResend = true
                )
            }
        }
    }

    // ════════════════════════════════════════
    //  Navigation State Reset
    // ════════════════════════════════════════

    fun onNavigatedBack() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }

    /**
     * Set email manually (fallback if not from SavedStateHandle)
     */
    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email) }
        if (email.isNotBlank() && generatedOtp.isEmpty()) {
            sendOtp()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}