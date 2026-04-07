package com.neurodrive.smartcar.ui.forgotpassword

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Show toast on success
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Purple80, BlueAccent)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ── Back Button ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Login"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ══════════════════════════════════════════
            //  MAIN CARD
            // ══════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                AnimatedContent(
                    targetState = uiState.isEmailSent,
                    transitionSpec = {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    },
                    label = "forgot_password_transition"
                ) { emailSent ->
                    if (emailSent) {
                        // ══════════════════════════════
                        //  SUCCESS STATE
                        // ══════════════════════════════
                        SuccessContent(
                            email = uiState.email,
                            onBackToLogin = onNavigateBack,
                            onResendEmail = { viewModel.sendResetEmail() }
                        )
                    } else {
                        // ══════════════════════════════
                        //  INPUT STATE
                        // ══════════════════════════════
                        InputContent(
                            email = uiState.email,
                            onEmailChange = { viewModel.updateEmail(it) },
                            isLoading = uiState.isLoading,
                            errorMessage = uiState.errorMessage,
                            onSubmit = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.sendResetEmail()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Back to login text ──
            if (!uiState.isEmailSent) {
                TextButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Back to Sign In",
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  INPUT CONTENT - Enter email to reset
// ══════════════════════════════════════════════════════════════

@Composable
private fun InputContent(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon
        Icon(
            Icons.Default.LockReset,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No worries! Enter the email address linked to your account and we'll send you a link to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Email Field ──
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email Address") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            trailingIcon = {
                if (email.isNotBlank()) {
                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Valid email",
                            tint = GreenPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Invalid email",
                            tint = RedAlert
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            isError = errorMessage != null,
            enabled = !isLoading
        )

        // ── Error Message ──
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            errorMessage?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = RedAlert.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = RedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = RedAlert
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Send Button ──
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && email.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending...", fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("SEND RESET LINK", fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ══════════════════════════════════════════════════════════════
//  SUCCESS CONTENT - Email sent
// ══════════════════════════════════════════════════════════════

@Composable
private fun SuccessContent(
    email: String,
    onBackToLogin: () -> Unit,
    onResendEmail: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon
        Icon(
            Icons.Default.MarkEmailRead,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = GreenPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Check Your Email!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We've sent a password reset link to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email chip
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Next Steps:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                StepItem(number = "1", text = "Open your email app")
                StepItem(number = "2", text = "Find the email from NeuroDrive")
                StepItem(number = "3", text = "Tap the password reset link")
                StepItem(number = "4", text = "Create your new password")
                StepItem(number = "5", text = "Come back and sign in")

                Spacer(modifier = Modifier.height(12.dp))

                // Tip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            AmberWarning.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AmberWarning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Check your spam/junk folder if you don't see the email. The link expires in 1 hour.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Back to Login button
        Button(
            onClick = onBackToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("BACK TO SIGN IN", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Resend email
        TextButton(onClick = onResendEmail) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Didn't receive it? Resend email")
        }
    }
}

@Composable
private fun StepItem(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}