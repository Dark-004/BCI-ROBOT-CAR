package com.neurodrive.smartcar.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val backgroundColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Default.Psychology,
            title = "Brain-Computer Interface",
            description = "Control your smart car using your brain signals. Our advanced EEG sensors detect your thoughts and translate them into commands.",
            backgroundColor = Purple80
        ),
        OnboardingPage(
            icon = Icons.Default.Bluetooth,
            title = "Seamless Connectivity",
            description = "Connect wirelessly to your bio-signal headband and ESP32-powered car. Real-time communication ensures instant response.",
            backgroundColor = BlueAccent
        ),
        OnboardingPage(
            icon = Icons.Default.Security,
            title = "Safety First",
            description = "Built-in obstacle detection, emergency braking, and signal quality monitoring keep you safe at all times.",
            backgroundColor = GreenPrimary
        ),
        OnboardingPage(
            icon = Icons.Default.Accessibility,
            title = "Accessible Design",
            description = "Designed for everyone. Adjustable controls, high contrast modes, and large touch targets for easy navigation.",
            backgroundColor = AmberWarning
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(page = pages[page])
        }

        // Page Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == pagerState.currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage)
                                Color.White
                            else
                                Color.White.copy(alpha = 0.5f)
                        )
                )
            }
        }

        // Navigation Buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Skip Button
            TextButton(
                onClick = {
                    viewModel.completeOnboarding()
                    onComplete()
                }
            ) {
                Text("Skip", color = Color.White)
            }

            // Next / Get Started Button
            Button(
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        viewModel.completeOnboarding()
                        onComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = pages[pagerState.currentPage].backgroundColor
                )
            ) {
                Text(
                    if (pagerState.currentPage == pages.lastIndex) "Get Started" else "Next",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    if (pagerState.currentPage == pages.lastIndex)
                        Icons.Default.Check
                    else
                        Icons.Default.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(page.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}