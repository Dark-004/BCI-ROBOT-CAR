package com.neurodrive.smartcar.ui.register

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.neurodrive.smartcar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit = {},
    onRegistrationSuccess: () -> Unit = {},
    onNavigateToVerification: (String) -> Unit = {},
    // ✅ Pass savedStateHandle from NavBackStackEntry in your NavHost
    savedStateHandle: SavedStateHandle? = null,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // ════════════════════════════════════════
    //  NAVIGATION & STATE HANDLERS
    // ════════════════════════════════════════

    // ✅ Handle navigation to verification screen
    LaunchedEffect(uiState.shouldNavigateToVerification) {
        if (uiState.shouldNavigateToVerification) {
            viewModel.onNavigatedToVerification()
            onNavigateToVerification(uiState.email)
        }
    }

    // ✅ Receive verified email from verification screen via SavedStateHandle
    val verifiedEmail = savedStateHandle?.get<String>("verified_email")

    LaunchedEffect(verifiedEmail) {
        verifiedEmail?.let { email ->
            viewModel.onEmailVerified(email)
            savedStateHandle.remove<String>("verified_email")
        }
    }

    // ✅ Prevent back press during registration
    BackHandler(enabled = uiState.isLoading) { }

    // ✅ Navigate only AFTER registration is complete
    LaunchedEffect(uiState.isRegistered) {
        if (uiState.isRegistered) {
            onRegistrationSuccess()
        }
    }

    // ════════════════════════════════════════
    //  MAIN UI
    // ════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkSurface,
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            // ════════════════════════════════════
            //  Top Bar
            // ════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateToLogin,
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join NeuroDrive and start your journey",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ════════════════════════════════════
            //  Registration Form Card
            // ════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface.copy(alpha = 0.8f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // ─────────────────────────────
                    //  Full Name
                    // ─────────────────────────────
                    OutlinedTextField(
                        value = uiState.fullName,
                        onValueChange = { viewModel.updateFullName(it) },
                        label = { Text("Full Name") },
                        placeholder = { Text("Enter your full name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = uiState.fullNameError != null,
                        supportingText = uiState.fullNameError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = GreenPrimary,
                            focusedLeadingIconColor = GreenPrimary,
                            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─────────────────────────────
                    //  Username
                    // ─────────────────────────────
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        label = { Text("Username") },
                        placeholder = { Text("Choose a username") },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null)
                        },
                        trailingIcon = {
                            when {
                                uiState.isCheckingUsername -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = GreenPrimary
                                    )
                                }
                                uiState.usernameAvailable == true -> {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Available",
                                        tint = GreenPrimary
                                    )
                                }
                                uiState.usernameAvailable == false -> {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = "Taken",
                                        tint = RedAlert
                                    )
                                }
                            }
                        },
                        isError = uiState.usernameError != null,
                        supportingText = uiState.usernameError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = GreenPrimary,
                            focusedLeadingIconColor = GreenPrimary,
                            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─────────────────────────────
                    //  Email with Verify Button
                    // ─────────────────────────────
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = { Text("Email Address") },
                        placeholder = { Text("Enter your email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        trailingIcon = {
                            when {
                                uiState.isEmailVerified -> {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = GreenPrimary
                                    )
                                }
                                uiState.isSendingVerification -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = GreenPrimary
                                    )
                                }
                            }
                        },
                        isError = uiState.emailError != null,
                        supportingText = uiState.emailError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        enabled = !uiState.isEmailVerified,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.White.copy(alpha = 0.8f),
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = if (uiState.isEmailVerified)
                                GreenPrimary.copy(alpha = 0.5f)
                            else
                                Color.White.copy(alpha = 0.3f),
                            disabledBorderColor = GreenPrimary.copy(alpha = 0.5f),
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            disabledLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = GreenPrimary,
                            focusedLeadingIconColor = GreenPrimary,
                            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f),
                            disabledLeadingIconColor = GreenPrimary.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Verify Email Button / Status
                    AnimatedContent(
                        targetState = uiState.isEmailVerified,
                        label = "email_verify_state"
                    ) { isVerified ->
                        if (isVerified) {
                            // ✅ Email Verified Badge
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = GreenPrimary.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GreenPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Email verified",
                                            color = GreenPrimary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.resetEmailVerification() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = Color.White.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Text(
                                            "Change",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else {
                            // Verify Email Button
                            Column {
                                OutlinedButton(
                                    onClick = { viewModel.sendVerificationEmail() },
                                    enabled = uiState.email.isNotBlank() &&
                                            uiState.emailError == null &&
                                            !uiState.isSendingVerification,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = BlueAccent,
                                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (uiState.email.isNotBlank() && uiState.emailError == null)
                                            BlueAccent
                                        else
                                            Color.White.copy(alpha = 0.2f)
                                    )
                                ) {
                                    if (uiState.isSendingVerification) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = BlueAccent
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sending...")
                                    } else {
                                        Icon(
                                            Icons.Default.MarkEmailRead,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Verify Email")
                                    }
                                }

                                if (!uiState.isSendingVerification && uiState.email.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You must verify your email before registering",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }

                                uiState.emailVerificationMessage?.let { message ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (message.contains("✅")) GreenPrimary
                                        else BlueAccent,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─────────────────────────────
                    //  Phone Number
                    // ─────────────────────────────
                    OutlinedTextField(
                        value = uiState.phoneNumber,
                        onValueChange = { viewModel.updatePhoneNumber(it) },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 XXXXX XXXXX") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        isError = uiState.phoneNumberError != null,
                        supportingText = uiState.phoneNumberError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = GreenPrimary,
                            focusedLeadingIconColor = GreenPrimary,
                            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─────────────────────────────
                    //  Role Dropdown
                    // ─────────────────────────────
                    RoleDropdown(
                        selectedRole = uiState.selectedRole,
                        onRoleSelected = { viewModel.updateRole(it) },
                        isError = uiState.roleError != null,
                        errorMessage = uiState.roleError
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─────────────────────────────
                    //  Password
                    // ─────────────────────────────
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        placeholder = { Text("Create a password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide"
                                    else "Show"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = GreenPrimary,
                            focusedLeadingIconColor = GreenPrimary,
                            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f)
                        )
                    )

                    // Password Strength
                    if (uiState.password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordStrengthIndicator(strength = uiState.passwordStrength)

                        Spacer(modifier = Modifier.height(8.dp))
                        PasswordRequirementsCard(requirements = uiState.passwordRequirements)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─────────────────────────────
                    //  Confirm Password
                    // ─────────────────────────────
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = { viewModel.updateConfirmPassword(it) },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Re-enter your password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                confirmPasswordVisible = !confirmPasswordVisible
                            }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide"
                                    else "Show"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        isError = uiState.confirmPasswordError != null,
                        supportingText = uiState.confirmPasswordError?.let {
                            { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (uiState.isFormValid) {
                                    viewModel.register()
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = GreenPrimary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                            cursorColor = GreenPrimary,
                            focusedLeadingIconColor = GreenPrimary,
                            unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─────────────────────────────
                    //  Terms & Conditions
                    // ─────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = uiState.termsAccepted,
                            onCheckedChange = { viewModel.updateTermsAccepted(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = GreenPrimary,
                                uncheckedColor = Color.White.copy(alpha = 0.6f),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I agree to the Terms of Service and Privacy Policy",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    if (uiState.termsError != null) {
                        Text(
                            text = uiState.termsError ?: "",
                            color = RedAlert,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─────────────────────────────
                    //  Error Message
                    // ─────────────────────────────
                    AnimatedVisibility(
                        visible = uiState.generalError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = RedAlert.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = RedAlert,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = uiState.generalError ?: "",
                                    color = RedAlert,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // ─────────────────────────────
                    //  Register Button
                    // ─────────────────────────────
                    Button(
                        onClick = { viewModel.register() },
                        enabled = uiState.isFormValid && !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            disabledContainerColor = GreenPrimary.copy(alpha = 0.3f)
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Creating Account...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Create Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ─────────────────────────────
                    //  Login Link
                    // ─────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Already have an account? ",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = onNavigateToLogin,
                            enabled = !uiState.isLoading
                        ) {
                            Text(
                                "Sign In",
                                color = GreenPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Role Dropdown Component
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleDropdown(
    selectedRole: UserRole?,
    onRoleSelected: (UserRole) -> Unit,
    isError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val roles = UserRole.entries

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedRole?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Role / User Type") },
                placeholder = { Text("Select your role") },
                leadingIcon = {
                    Icon(
                        imageVector = selectedRole?.icon ?: Icons.Default.Badge,
                        contentDescription = null,
                        tint = if (selectedRole != null)
                            selectedRole.color
                        else
                            Color.White.copy(alpha = 0.6f)
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                isError = isError,
                supportingText = errorMessage?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = GreenPrimary,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                    cursorColor = GreenPrimary,
                    focusedLeadingIconColor = GreenPrimary,
                    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.6f)
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = role.icon,
                                    contentDescription = null,
                                    tint = role.color,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = role.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = role.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                            .copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onRoleSelected(role)
                            expanded = false
                        },
                        trailingIcon = {
                            if (selectedRole == role) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = GreenPrimary
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Password Strength Indicator
// ════════════════════════════════════════════════════════════════

@Composable
fun PasswordStrengthIndicator(
    strength: PasswordStrength,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Password Strength:",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                strength.label,
                style = MaterialTheme.typography.labelSmall,
                color = strength.color,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { strength.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = strength.color,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

// ════════════════════════════════════════════════════════════════
//  Password Requirements Card
// ════════════════════════════════════════════════════════════════

@Composable
fun PasswordRequirementsCard(
    requirements: List<PasswordRequirement>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "Password Requirements",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            requirements.forEach { requirement ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (requirement.isMet) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (requirement.isMet) GreenPrimary
                        else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        requirement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (requirement.isMet) Color.White
                        else Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}