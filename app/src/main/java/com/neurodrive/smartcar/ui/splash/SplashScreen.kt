package com.neurodrive.smartcar.ui.splash


import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.R
import com.neurodrive.smartcar.ui.theme.BlueAccent
import com.neurodrive.smartcar.ui.theme.Purple80
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation
    LaunchedEffect(uiState) {
        when (uiState) {
            is SplashUiState.NavigateToLogin -> onNavigateToLogin()
            is SplashUiState.NavigateToDashboard -> onNavigateToDashboard()
            is SplashUiState.NavigateToOnboarding -> onNavigateToOnboarding()
            else -> {}
        }
    }

    // Animation
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            // App Icon/Logo
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NeuroDrive",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bio-Signal Controlled Smart Car",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp
            )
        }
    }
}