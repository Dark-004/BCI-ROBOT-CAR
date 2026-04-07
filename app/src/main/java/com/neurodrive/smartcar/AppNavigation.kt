package com.neurodrive.smartcar

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neurodrive.smartcar.ui.alerts.AlertsScreen
import com.neurodrive.smartcar.ui.control.ControlScreen
import com.neurodrive.smartcar.ui.dashboard.DashboardScreen
import com.neurodrive.smartcar.ui.forgotpassword.ForgotPasswordScreen
import com.neurodrive.smartcar.ui.login.LoginScreen
import com.neurodrive.smartcar.ui.onboarding.OnboardingScreen
import com.neurodrive.smartcar.ui.register.RegisterScreen
import com.neurodrive.smartcar.ui.settings.SettingsScreen
import com.neurodrive.smartcar.ui.splash.SplashScreen
import com.neurodrive.smartcar.ui.verification.EmailVerificationScreen
import com.neurodrive.smartcar.util.Constants.Routes
import com.neurodrive.smartcar.ui.profile.ProfileScreen
import com.neurodrive.smartcar.ui.calibration.CalibrationScreen
import com.neurodrive.smartcar.ui.help.HelpScreen

/**
 * Main Navigation Controller for NeuroDrive App
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthNavigationViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val startDestination = Routes.SPLASH

    // ✅ Track current route to prevent unwanted redirects
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // ✅ Define which screens should NOT be redirected by auth state
    val authScreens = listOf(
        Routes.SPLASH,
        Routes.LOGIN,
        Routes.REGISTER,
        Routes.FORGOT_PASSWORD,
        Routes.ONBOARDING
    )

    // ✅ Define screens where auth redirect should be BLOCKED
    // (verification screen is part of registration flow)
    val registrationFlowScreens = listOf(
        Routes.REGISTER,
        "email_verification/{email}" // Route pattern
    )

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                // ✅ CRITICAL FIX: Don't redirect if user is in registration flow
                // (Register screen or Email Verification screen)
                val isInRegistrationFlow = currentRoute?.let { route ->
                    route == Routes.REGISTER ||
                            route.startsWith("email_verification")
                } ?: false

                if (isInRegistrationFlow) {
                    // ✅ User is registering - DO NOT redirect to dashboard
                    return@LaunchedEffect
                }

                // Only redirect from auth screens (login, splash, etc.)
                if (currentRoute in authScreens) {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                // Don't redirect from auth screens or registration flow
                if (currentRoute != null &&
                    currentRoute !in authScreens &&
                    !currentRoute.startsWith("email_verification")
                ) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> { }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { 1000 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -1000 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -1000 },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { 1000 },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {

        // ════════════════════════════════════════
        //  Splash
        // ════════════════════════════════════════
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ════════════════════════════════════════
        //  Onboarding
        // ════════════════════════════════════════
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ════════════════════════════════════════
        //  Login
        // ════════════════════════════════════════
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                }
            )
        }

        // ════════════════════════════════════════
        //  Register
        // ════════════════════════════════════════
        composable(Routes.REGISTER) { backStackEntry ->
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    // ✅ ONLY called after successful registration
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToVerification = { email ->
                    // ✅ Navigate to email verification with encoded email
                    val encodedEmail = java.net.URLEncoder.encode(email, "UTF-8")
                    navController.navigate("email_verification/$encodedEmail")
                },
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        // ════════════════════════════════════════
        //  Forgot Password
        // ════════════════════════════════════════
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        //  Email Verification
        // ════════════════════════════════════════
        composable(
            route = "email_verification/{email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            // ✅ Decode the email properly
            val email = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("email") ?: "",
                "UTF-8"
            )

            EmailVerificationScreen(
                email = email,
                onVerificationSuccess = { verifiedEmail ->
                    // ✅ Pass verified email BACK to RegisterScreen
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("verified_email", verifiedEmail)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ════════════════════════════════════════
        //  Dashboard
        // ════════════════════════════════════════
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToControl = {
                    navController.navigate(Routes.CONTROL)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToAlerts = {
                    navController.navigate(Routes.ALERTS)
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                },
                onNavigateToCalibration = {
                    navController.navigate(Routes.CALIBRATION)
                },
                onNavigateToHelp = {
                    navController.navigate(Routes.HELP)
                }
            )
        }

        // ════════════════════════════════════════
        //  Control
        // ════════════════════════════════════════
        composable(Routes.CONTROL) {
            ControlScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        //  Settings
        // ════════════════════════════════════════
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ════════════════════════════════════════
        //  Alerts
        // ════════════════════════════════════════
        composable(Routes.ALERTS) {
            AlertsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        //  Profile
        // ════════════════════════════════════════
        composable(Routes.PROFILE) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        // ════════════════════════════════════════
        //  Calibration
        // ════════════════════════════════════════
        composable(Routes.CALIBRATION) {
            CalibrationScreen(
                onNavigateBack = { navController.popBackStack() },
                onCalibrationComplete = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        //  Help
        // ════════════════════════════════════════
        composable(Routes.HELP) {
            HelpScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        //  Device Connection
        // ════════════════════════════════════════
        composable(Routes.DEVICE_CONNECTION) {
            DeviceConnectionScreen(
                onNavigateBack = { navController.popBackStack() },
                onDeviceConnected = { navController.popBackStack() }
            )
        }

        // ════════════════════════════════════════
        //  Signal History
        // ════════════════════════════════════════
        composable(
            route = "${Routes.SIGNAL_HISTORY}/{signalType}",
            arguments = listOf(
                navArgument("signalType") {
                    type = NavType.StringType
                    defaultValue = "all"
                }
            )
        ) { backStackEntry ->
            val signalType = backStackEntry.arguments?.getString("signalType") ?: "all"
            SignalHistoryScreen(
                signalType = signalType,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  Navigation Extensions
// ════════════════════════════════════════════════════════════════

fun NavHostController.navigateToSignalHistory(signalType: String) {
    navigate("${Routes.SIGNAL_HISTORY}/$signalType")
}

fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) { launchSingleTop = true }
}

fun NavHostController.navigateAndClearBackStack(route: String) {
    navigate(route) { popUpTo(0) { inclusive = true } }
}

// ════════════════════════════════════════════════════════════════
//  Placeholder Screens
// ════════════════════════════════════════════════════════════════

@Composable
fun DeviceConnectionScreen(
    onNavigateBack: () -> Unit,
    onDeviceConnected: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Device Connection Screen")
    }
}

@Composable
fun SignalHistoryScreen(
    signalType: String,
    onNavigateBack: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Signal History: $signalType")
    }
}