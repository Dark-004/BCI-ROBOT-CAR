package com.neurodrive.smartcar.ui.verification

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    email: String = "",
    onVerificationSuccess: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // ✅ Set email if passed from navigation
    LaunchedEffect(email) {
        if (email.isNotBlank()) {
            viewModel.setEmail(email)
        }
    }

    // ✅ Handle successful verification → navigate back
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.onNavigatedBack()
            onVerificationSuccess(uiState.email)
        }
    }

    // ✅ Prevent accidental back during verification
    BackHandler(enabled = uiState.isVerifying) { }

    // Auto-focus OTP field
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    enabled = !uiState.isVerifying
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "Verify Email",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Email Icon ──
            Icon(
                Icons.Default.MarkEmailRead,
                contentDescription = null,
                tint = BlueAccent,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Title ──
            Text(
                text = "Check your email",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Text(
                text = uiState.email,
                style = MaterialTheme.typography.bodyLarge,
                color = BlueAccent,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── OTP Input ──
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
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter verification code",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── OTP Boxes ──
                    OtpInputField(
                        otpValue = uiState.otp,
                        onOtpChange = { viewModel.updateOtp(it) },
                        isError = uiState.otpError != null,
                        modifier = Modifier.focusRequester(focusRequester)
                    )

                    // ── Error Message ──
                    AnimatedVisibility(
                        visible = uiState.otpError != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = uiState.otpError ?: "",
                            color = RedAlert,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Verify Button ──
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.verifyOtp()
                        },
                        enabled = uiState.canVerify,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            disabledContainerColor = GreenPrimary.copy(alpha = 0.3f)
                        )
                    ) {
                        if (uiState.isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verifying...")
                        } else {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Verify",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Resend Section ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Didn't receive the code? ",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (uiState.canResend) {
                            TextButton(
                                onClick = { viewModel.resendOtp() },
                                enabled = !uiState.isSending
                            ) {
                                Text(
                                    "Resend",
                                    color = BlueAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = uiState.resendTimerDisplay,
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // ── Status Message ──
                    uiState.message?.let { message ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (uiState.isError)
                                    RedAlert.copy(alpha = 0.15f)
                                else
                                    GreenPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (uiState.isError) Icons.Default.Error
                                    else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (uiState.isError) RedAlert
                                    else GreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    color = if (uiState.isError) RedAlert
                                    else GreenPrimary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Dev hint (remove in production) ──
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = BlueAccent.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = BlueAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEV: Check Logcat or Firebase Console > " +
                                "Firestore > email_otps for the OTP code",
                        color = BlueAccent.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ── Verified Overlay ──
        AnimatedVisibility(
            visible = uiState.isVerified,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = GreenPrimary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Email Verified!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = GreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Returning to registration...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  OTP Input Field Component
// ════════════════════════════════════════════════════════════════

@Composable
fun OtpInputField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    otpLength: Int = 6
) {
    BasicTextField(
        value = otpValue,
        onValueChange = { value ->
            if (value.length <= otpLength && value.all { it.isDigit() }) {
                onOtpChange(value)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        decorationBox = { _ ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(otpLength) { index ->
                    val char = otpValue.getOrNull(index)?.toString() ?: ""
                    val isFocused = otpValue.length == index

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .border(
                                width = 2.dp,
                                color = when {
                                    isError -> RedAlert
                                    isFocused -> GreenPrimary
                                    char.isNotEmpty() -> GreenPrimary.copy(alpha = 0.5f)
                                    else -> Color.White.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (char.isNotEmpty())
                                    GreenPrimary.copy(alpha = 0.1f)
                                else
                                    Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )

                        // Cursor indicator
                        if (isFocused) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(24.dp)
                                    .background(GreenPrimary)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}