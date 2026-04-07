package com.neurodrive.smartcar.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.AppConfig
import com.neurodrive.smartcar.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToCalibration: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ══════════════════════════════════════════════════
            //  CONNECTED DEVICES SECTION
            // ══════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Connected Devices",
                    icon = Icons.Default.Bluetooth
                )
            }

            item {
                ConnectedDeviceStatusItem(
                    label = "ESP32 Car Controller",
                    deviceName = uiState.esp32ConnectedDeviceName,
                    isConnected = uiState.isEsp32Connected,
                    icon = Icons.Default.DirectionsCar
                )
            }

            item {
                ConnectedDeviceStatusItem(
                    label = "EEG Headband (BLE)",
                    deviceName = uiState.bleConnectedDeviceName,
                    isConnected = uiState.isBleConnected,
                    icon = Icons.Default.Headphones
                )
            }

            item {
                if (!uiState.isEsp32Connected && !uiState.isBleConnected) {
                    NoDeviceHintCard()
                }
            }

            // ══════════════════════════════════════════════════
            //  BATTERY STATUS SECTION - NEW!
            // ══════════════════════════════════════════════════
            item {
                SettingsSectionHeader(
                    title = "Battery Status (EEG & EMG Headband)",
                    icon = Icons.Default.BatteryStd
                )
            }

            item {
                BatteryStatusCard(
                    batteryPercent = uiState.batteryPercent,
                    batteryLevel = uiState.batteryLevel,
                    isConnected = uiState.isBleConnected,
                    deviceName = uiState.bleConnectedDeviceName
                )
            }

            // ── Calibration Section ─────────────────
            item {
                SettingsSectionHeader(title = "Calibration", icon = Icons.Default.Tune)
            }
            item {
                CalibrationStatusCard(
                    lastCalibration = uiState.lastCalibrationTime,
                    isCalibrationFresh = uiState.isCalibrationFresh,
                    eegAlpha = uiState.eegBaselineAlpha,
                    eegBeta = uiState.eegBaselineBeta,
                    emgBaseline = uiState.emgBaseline,
                    onRecalibrate = { viewModel.showCalibrationDialog() }
                )
            }
            item {
                SettingsClickableItem(
                    title = "Advanced Calibration",
                    subtitle = "Run detailed calibration wizard",
                    icon = Icons.Default.Tune,
                    onClick = onNavigateToCalibration
                )
            }

            // ── Data & Privacy Section ──────────────
            item {
                SettingsSectionHeader(title = "Data & Privacy", icon = Icons.Default.PrivacyTip)
            }
            item {
                SettingsClickableItem(
                    title = "Export Data",
                    subtitle = "Download your signal history and settings",
                    icon = Icons.Default.Download,
                    onClick = { viewModel.exportData() }
                )
            }
            item {
                SettingsClickableItem(
                    title = "Clear Signal History",
                    subtitle = "Delete all recorded bio-signal data",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { viewModel.showClearHistoryDialog() },
                    tint = AmberWarning
                )
            }
            item {
                SettingsClickableItem(
                    title = "Privacy Policy",
                    subtitle = "View our privacy policy",
                    icon = Icons.Default.Policy,
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://neurodrive.app/privacy")
                        )
                        context.startActivity(intent)
                    }
                )
            }

            // ── About Section ────────────────────────
            item {
                SettingsSectionHeader(title = "About", icon = Icons.Default.Info)
            }
            item {
                SettingsInfoItem(
                    title = "App Version",
                    value = "${AppConfig.VERSION_NAME} (${AppConfig.VERSION_CODE})"
                )
            }
            item {
                SettingsInfoItem(
                    title = "Device",
                    value = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
            }
            item {
                SettingsInfoItem(
                    title = "Android Version",
                    value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
                )
            }
            /*item {
                SettingsClickableItem(
                    title = "Open Source Licenses",
                    subtitle = "View third-party licenses",
                    icon = Icons.Default.Code,
                    onClick = { viewModel.showLicensesDialog() }
                )
            }*/
            item {
                SettingsClickableItem(
                    title = "Send Feedback",
                    subtitle = "Report bugs or suggest features",
                    icon = Icons.Default.Feedback,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@neurodrive.app")
                            putExtra(Intent.EXTRA_SUBJECT, "NeuroDrive Feedback")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            // ── Account Actions ──────────────────────
            item {
                SettingsSectionHeader(title = "Account", icon = Icons.Default.ManageAccounts)
            }
            item {
                SettingsClickableItem(
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = { viewModel.showLogoutDialog() },
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            item {
                SettingsClickableItem(
                    title = "Delete Account",
                    subtitle = "Permanently delete your account and data",
                    icon = Icons.Default.DeleteForever,
                    onClick = { viewModel.showDeleteAccountDialog() },
                    tint = RedAlert
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────
    if (uiState.showLogoutDialog) {
        ConfirmationDialog(
            title = "Logout",
            message = "Are you sure you want to logout?",
            confirmLabel = "Logout",
            onConfirm = { viewModel.logout() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
    if (uiState.showDeleteAccountDialog) {
        DangerousActionDialog(
            title = "Delete Account",
            message = "This will permanently delete your account and all associated data. This action cannot be undone.",
            confirmLabel = "Delete Forever",
            confirmationText = "DELETE",
            onConfirm = { viewModel.deleteAccount() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
    if (uiState.showResetDrivingDialog) {
        ConfirmationDialog(
            title = "Reset Driving Settings",
            message = "This will reset speed limit, signal threshold, obstacle distance, and other driving settings to their default values.",
            confirmLabel = "Reset",
            onConfirm = { viewModel.resetDrivingSettings() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
    if (uiState.showResetAccessibilityDialog) {
        ConfirmationDialog(
            title = "Reset Accessibility Settings",
            message = "This will reset all accessibility settings including font scale, high contrast, and haptic feedback to their default values.",
            confirmLabel = "Reset",
            onConfirm = { viewModel.resetAccessibilitySettings() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
    if (uiState.showResetAllDialog) {
        DangerousActionDialog(
            title = "Reset All Settings",
            message = "This will reset ALL settings to their default values. Your account and login will not be affected.",
            confirmLabel = "Reset All",
            confirmationText = "RESET",
            onConfirm = { viewModel.resetAllSettings() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
    if (uiState.showClearHistoryDialog) {
        DangerousActionDialog(
            title = "Clear Signal History",
            message = "This will permanently delete all recorded bio-signal data and command logs. This action cannot be undone.",
            confirmLabel = "Clear All",
            confirmationText = "CLEAR",
            onConfirm = { viewModel.clearSignalHistory() },
            onDismiss = { viewModel.dismissDialog() }
        )
    }
    if (uiState.showCalibrationDialog) {
        CalibrationDialog(
            onStartCalibration = { viewModel.startCalibration() },
            onDismiss = { viewModel.dismissDialog() },
            isCalibrating = uiState.isCalibrating,
            calibrationProgress = uiState.calibrationProgress,
            calibrationStep = uiState.calibrationStep
        )
    }
    if (uiState.showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = uiState.selectedTheme,
            onSelectTheme = { viewModel.updateTheme(it) },
            onDismiss = { viewModel.dismissDialog() }
        )
    }

    uiState.snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            delay(3000)
            viewModel.clearSnackbar()
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  BATTERY STATUS CARD - NEW COMPONENT!
// ════════════════════════════════════════════════════════════════

@Composable
fun BatteryStatusCard(
    batteryPercent: Int,
    batteryLevel: BatteryLevel,
    isConnected: Boolean,
    deviceName: String?
) {
    val batteryColor = Color(batteryLevel.color)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (batteryLevel == BatteryLevel.CRITICAL)
                Color(0xFFF44336).copy(alpha = 0.08f)
            else if (batteryLevel == BatteryLevel.LOW)
                Color(0xFFFF9800).copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Battery Icon
                    val batteryIcon = when {
                        !isConnected -> Icons.Default.BatteryUnknown
                        batteryPercent <= 10 -> Icons.Default.BatteryAlert
                        batteryPercent <= 20 -> Icons.Default.Battery2Bar
                        batteryPercent <= 40 -> Icons.Default.Battery3Bar
                        batteryPercent <= 60 -> Icons.Default.Battery4Bar
                        batteryPercent <= 80 -> Icons.Default.Battery5Bar
                        else -> Icons.Default.BatteryFull
                    }

                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = "Battery",
                        tint = if (isConnected) batteryColor else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Headband Battery",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = deviceName ?: "Not connected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Battery Percentage Badge
                if (isConnected && batteryPercent >= 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = batteryColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "$batteryPercent%",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = batteryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isConnected && batteryPercent >= 0) {
                // Battery Progress Bar
                BatteryProgressBar(
                    percent = batteryPercent,
                    color = batteryColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Battery Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BatteryInfoChip(
                        label = "Status",
                        value = batteryLevel.displayName,
                        color = batteryColor
                    )
                    BatteryInfoChip(
                        label = "Connection",
                        value = "Active",
                        color = Color(0xFF4CAF50)
                    )
                    BatteryInfoChip(
                        label = "Estimated",
                        value = estimateBatteryTime(batteryPercent),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Low Battery Warning
                if (batteryPercent <= 20) {
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryWarningBanner(
                        isCritical = batteryPercent <= 10,
                        percent = batteryPercent
                    )
                }

            } else {
                // Not Connected State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect headband to view battery",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryProgressBar(
    percent: Int,
    color: Color
) {
    Column {
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percent / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Min/Max labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                text = "100%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun BatteryInfoChip(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun BatteryWarningBanner(
    isCritical: Boolean,
    percent: Int
) {
    val backgroundColor = if (isCritical)
        Color(0xFFF44336).copy(alpha = 0.12f)
    else
        Color(0xFFFF9800).copy(alpha = 0.12f)

    val iconColor = if (isCritical)
        Color(0xFFF44336)
    else
        Color(0xFFFF9800)

    val message = if (isCritical)
        "Critical! Charge headband immediately ($percent%)"
    else
        "Low battery. Consider charging soon ($percent%)"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = iconColor
        )
    }
}

private fun estimateBatteryTime(percent: Int): String {
    // Rough estimation: ~8 hours for full battery
    val totalMinutes = (percent * 8 * 60) / 100
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

// ════════════════════════════════════════════════════════════════
//  EXISTING COMPONENTS (unchanged)
// ════════════════════════════════════════════════════════════════

@Composable
fun ConnectedDeviceStatusItem(
    label: String,
    deviceName: String?,
    isConnected: Boolean,
    icon: ImageVector
) {
    val statusColor = if (isConnected) GreenPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val displayName = if (isConnected && deviceName != null) deviceName else "Not connected"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) GreenPrimary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
    }
}

@Composable
fun NoDeviceHintCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "No devices connected. Go to Dashboard to scan and connect.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
/*
@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 0.6f else 0.3f
                ),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "$title toggle" }
        )
    }
}

@Composable
fun SettingsSliderItem(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    icon: ImageVector,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp)
                .semantics {
                    contentDescription = "$title slider, current value $valueLabel"
                }
        )
    }
}
*/
@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (tint == RedAlert || tint == AmberWarning) tint
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) trailing()
        else Icon(
            Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun SettingsInfoItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CalibrationStatusCard(
    lastCalibration: String,
    isCalibrationFresh: Boolean,
    eegAlpha: Float,
    eegBeta: Float,
    emgBaseline: Float,
    onRecalibrate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCalibrationFresh) GreenPrimary.copy(alpha = 0.1f)
            else AmberWarning.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Last Calibration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        lastCalibration,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!isCalibrationFresh) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Outdated") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = AmberWarning.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CalibrationValue("EEG α", "%.2f".format(eegAlpha))
                CalibrationValue("EEG β", "%.2f".format(eegBeta))
                CalibrationValue("EMG", "%.2f".format(emgBaseline))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onRecalibrate, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Recalibrate")
            }
        }
    }
}

@Composable
private fun CalibrationValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WarningCard(message: String, actionLabel: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning)
            Spacer(modifier = Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String, message: String, confirmLabel: String,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DangerousActionDialog(
    title: String, message: String, confirmLabel: String,
    confirmationText: String, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = RedAlert)
                Spacer(modifier = Modifier.width(8.dp))
                Text(title)
            }
        },
        text = {
            Column {
                Text(message)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Type \"$confirmationText\" to confirm:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(); onDismiss() },
                enabled = inputText == confirmationText,
                colors = ButtonDefaults.textButtonColors(contentColor = RedAlert)
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun CalibrationDialog(
    onStartCalibration: () -> Unit, onDismiss: () -> Unit,
    isCalibrating: Boolean, calibrationProgress: Float, calibrationStep: String
) {
    AlertDialog(
        onDismissRequest = { if (!isCalibrating) onDismiss() },
        title = { Text("Signal Calibration") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCalibrating) {
                    CircularProgressIndicator(
                        progress = { calibrationProgress },
                        modifier = Modifier.size(80.dp), strokeWidth = 8.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(calibrationStep, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${(calibrationProgress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Calibration will measure your baseline brain signals. Please sit still and relax.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Duration: ~30 seconds", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        },
        confirmButton = { if (!isCalibrating) Button(onClick = onStartCalibration) { Text("Start Calibration") } },
        dismissButton = { if (!isCalibrating) TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: String, onSelectTheme: (String) -> Unit, onDismiss: () -> Unit
) {
    val themes = listOf("Purple (Default)", "Blue", "Green", "Orange", "Red", "Teal")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            Column {
                themes.forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme(theme); onDismiss() }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = theme == currentTheme, onClick = { onSelectTheme(theme); onDismiss() })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(theme)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}