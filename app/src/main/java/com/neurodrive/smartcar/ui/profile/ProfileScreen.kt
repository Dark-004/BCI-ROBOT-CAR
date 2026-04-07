package com.neurodrive.smartcar.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.profileState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1117),
                        Color(0xFF161B22),
                        Color(0xFF0D1117)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ─────────────────────────────
            //  Top Bar
            // ─────────────────────────────
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },

                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            when {
                // ─────────────────────────────
                //  Loading State
                // ─────────────────────────────
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = GreenPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading profile...",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // ─────────────────────────────
                //  Error State
                // ─────────────────────────────
                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = RedAlert,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.error ?: "Unknown error",
                                color = RedAlert,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadProfile() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenPrimary
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }

                // ─────────────────────────────
                //  Profile Content
                // ─────────────────────────────
                else -> {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Avatar ──
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(GreenPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.fullName
                                    .split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString(""),
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Name ──
                        Text(
                            text = state.fullName.ifEmpty { "No Name" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // ── Username ──
                        Text(
                            text = "@${state.username}",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        // ── Role Badge ──
                        if (state.role.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = GreenPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = state.role,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 6.dp
                                    ),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GreenPrimary
                                )
                            }
                        }

                        // ── Verification Badge ──
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (state.isEmailVerified)
                                    Icons.Default.VerifiedUser
                                else
                                    Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (state.isEmailVerified)
                                    GreenPrimary
                                else
                                    AmberWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (state.isEmailVerified)
                                    "Email Verified"
                                else
                                    "Email Not Verified",
                                fontSize = 12.sp,
                                color = if (state.isEmailVerified)
                                    GreenPrimary
                                else
                                    AmberWarning
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Profile Details Card ──
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF161B22)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    "Personal Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ProfileDetailItem(
                                    icon = Icons.Default.Person,
                                    label = "Full Name",
                                    value = state.fullName
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                ProfileDetailItem(
                                    icon = Icons.Default.AlternateEmail,
                                    label = "Username",
                                    value = state.username
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                ProfileDetailItem(
                                    icon = Icons.Default.Email,
                                    label = "Email",
                                    value = state.email
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                ProfileDetailItem(
                                    icon = Icons.Default.Phone,
                                    label = "Phone Number",
                                    value = state.phoneNumber
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                ProfileDetailItem(
                                    icon = Icons.Default.Badge,
                                    label = "Role",
                                    value = state.role
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Account Info Card ──
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF161B22)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    "Account Information",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                ProfileDetailItem(
                                    icon = Icons.Default.Fingerprint,
                                    label = "User ID",
                                    value = state.userId
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Color.White.copy(alpha = 0.1f)
                                )

                                ProfileDetailItem(
                                    icon = if (state.isPremium)
                                        Icons.Default.Star
                                    else
                                        Icons.Default.StarBorder,
                                    label = "Account Type",
                                    value = if (state.isPremium) "Premium" else "Free"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════
//  Profile Detail Item Component
// ════════════════════════════════════════════════════

@Composable
fun ProfileDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = GreenPrimary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value.ifEmpty { "Not set" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (value.isEmpty())
                    Color.White.copy(alpha = 0.3f)
                else
                    Color.White
            )
        }
    }
}