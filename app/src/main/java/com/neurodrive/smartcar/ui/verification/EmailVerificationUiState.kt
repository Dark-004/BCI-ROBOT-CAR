package com.neurodrive.smartcar.ui.verification

data class EmailVerificationUiState(
    // Email
    val email: String = "",

    // OTP
    val otp: String = "",
    val otpError: String? = null,

    // Verification status
    val isVerified: Boolean = false,
    val isVerifying: Boolean = false,

    // Sending status
    val isSending: Boolean = false,

    // Resend
    val canResend: Boolean = false,
    val resendTimerSeconds: Int = 0,

    // Messages
    val message: String? = null,
    val isError: Boolean = false,

    // Navigation
    val shouldNavigateBack: Boolean = false
) {
    /**
     * Formatted timer display (e.g., "0:45")
     */
    val resendTimerDisplay: String
        get() {
            val minutes = resendTimerSeconds / 60
            val seconds = resendTimerSeconds % 60
            return "$minutes:${seconds.toString().padStart(2, '0')}"
        }

    /**
     * Whether the verify button should be enabled
     */
    val canVerify: Boolean
        get() = otp.length == 6 && !isVerifying && !isVerified
}