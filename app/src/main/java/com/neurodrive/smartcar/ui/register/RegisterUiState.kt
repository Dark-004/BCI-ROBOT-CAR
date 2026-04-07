package com.neurodrive.smartcar.ui.register

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.neurodrive.smartcar.ui.theme.*
import androidx.compose.ui.graphics.Color.Companion.Yellow

data class RegisterUiState(
    // Fields
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val selectedRole: UserRole? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val termsAccepted: Boolean = false,

    // Field errors
    val fullNameError: String? = null,
    val usernameError: String? = null,
    val emailError: String? = null,
    val phoneNumberError: String? = null,
    val roleError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val termsError: String? = null,
    val generalError: String? = null,

    // Username check
    val isCheckingUsername: Boolean = false,
    val usernameAvailable: Boolean? = null,

    // Email verification
    val isEmailVerified: Boolean = false,
    val isSendingVerification: Boolean = false,
    val emailVerificationMessage: String? = null,
    // ✅ Navigation flag - ONLY for verification screen
    val shouldNavigateToVerification: Boolean = false,

    // Password
    val passwordStrength: PasswordStrength = PasswordStrength.NONE,
    val passwordRequirements: List<PasswordRequirement> = emptyList(),

    // Loading & registration
    val isLoading: Boolean = false,
    // ✅ ONLY true after successful register() call
    val isRegistered: Boolean = false
) {
    /**
     * Form is valid ONLY when ALL conditions are met
     * including email verification
     */
    val isFormValid: Boolean
        get() = fullName.isNotBlank() &&
                fullNameError == null &&
                username.length >= 3 &&
                usernameError == null &&
                usernameAvailable == true &&
                email.isNotBlank() &&
                emailError == null &&
                isEmailVerified && // ✅ Must be verified
                selectedRole != null &&
                roleError == null &&
                password.length >= 8 &&
                passwordError == null &&
                confirmPassword == password &&
                confirmPasswordError == null &&
                termsAccepted &&
                !isLoading
}

// ════════════════════════════════════════
//  Supporting Data Classes & Enums
// ════════════════════════════════════════

enum class PasswordStrength(
    val label: String,
    val color: Color,
    val progress: Float
) {
    NONE("", Color.Transparent, 0f),
    WEAK("Weak", RedAlert, 0.25f),
    MEDIUM("Medium", Yellow, 0.5f),
    STRONG("Strong", BlueAccent, 0.75f),
    VERY_STRONG("Very Strong", GreenPrimary, 1f)
}

data class PasswordRequirement(
    val description: String,
    val isMet: Boolean
)

enum class UserRole(
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
) {
    DRIVER(
        "Driver",
        "Control your vehicle with brain signals",
        Icons.Default.DirectionsCar,
        GreenPrimary
    ),
    RESEARCHER(
        "Researcher",
        "Analyze BCI data and patterns",
        Icons.Default.Science,
        BlueAccent
    ),
    STUDENT(
        "STUDENT",
        "Develop new features and tools",
        Icons.Default.Person,
        Yellow

    ),
    /*ADMIN(
        "Administrator",
        "Manage system and users",
        Icons.Default.AdminPanelSettings,
        RedAlert
    ),
    TESTER(
        "Beta Tester",
        "Test new features and provide feedback",
        Icons.Default.BugReport,
        Color(0xFFAB47BC)
    )*/
}