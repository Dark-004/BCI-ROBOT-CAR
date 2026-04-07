package com.neurodrive.smartcar.ui.login

import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import com.neurodrive.smartcar.R
import com.neurodrive.smartcar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    onNavigateToForgotPassword: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var passwordVisible by remember { mutableStateOf(false) }

    // Handle successful login
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Purple80, BlueAccent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo / Header ──────────────────────
            // Use the legacy PNG icon from mipmap (automatically generated when adding a launcher icon)
            // Replace the Image composable with:
            // ══════════════════════════════
            //  APP LOGO — Round with Shadow
            // ══════════════════════════════


            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "NeuroDrive Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.height(20.dp))


            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "NeuroDrive",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Bio-Signal Controlled Smart Car",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Login Card ─────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome Back",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Username Field ─────────────
                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.updateUsername(it) },
                        label = { Text("Enter Gmail Address") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        isError = uiState.usernameError != null,
                        supportingText = uiState.usernameError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Username input" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password Field ─────────────
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible)
                                        "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        isError = uiState.passwordError != null,
                        supportingText = uiState.passwordError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                viewModel.login()
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Password input" }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Remember Me & Forgot Password ─
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = uiState.rememberMe,
                                onCheckedChange = { viewModel.updateRememberMe(it) }
                            )
                            Text("Remember me", style = MaterialTheme.typography.bodySmall)
                        }

                        // Inside the login card, update the "Forgot Password?" button:
                        TextButton(onClick = onNavigateToForgotPassword) {   // <-- use the callback
                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)

                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Error Message ──────────────
                    AnimatedVisibility(
                        visible = uiState.error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = RedAlert.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = RedAlert
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    uiState.error ?: "",
                                    color = RedAlert,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // ── Lockout Warning ────────────
                    AnimatedVisibility(
                        visible = uiState.isLockedOut,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AmberWarning.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = AmberWarning
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Account locked. Try again in ${uiState.lockoutSecondsRemaining}s",
                                    color = AmberWarning,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Login Button ───────────────
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.login()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !uiState.isLoading && !uiState.isLockedOut
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGN IN", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Biometric Login ────────────
                    if (uiState.biometricAvailable) {
                        OutlinedButton(
                            onClick = {
                                showBiometricPrompt(
                                    activity = context as FragmentActivity,
                                    onSuccess = { viewModel.loginWithBiometric() }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isLockedOut
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Purple80
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Biometric")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Register Link ──────────────────────
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have an account?",
                    color = Color.White.copy(alpha = 0.8f)
                )
                TextButton(onClick = onNavigateToRegister) {
                    Text(
                        "Sign Up",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit
) {
    val executor = activity.mainExecutor
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Login")
        .setSubtitle("Authenticate to access NeuroDrive")
        .setNegativeButtonText("Cancel")
        .build()

    biometricPrompt.authenticate(promptInfo)
}