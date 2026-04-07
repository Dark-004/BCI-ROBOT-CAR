package com.neurodrive.smartcar.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurodrive.smartcar.data.repository.AlertEvent
import com.neurodrive.smartcar.data.repository.AlertSeverity
import com.neurodrive.smartcar.data.repository.AlertType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AlertViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val alertHistory by viewModel.alertHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts & Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (alertHistory.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearAlerts() }) {
                            Text("Clear All")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (alertHistory.isEmpty()) {
            // ══════════════════════════════════
            //  EMPTY STATE
            // ══════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No alerts",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Everything is running smoothly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            // ══════════════════════════════════
            //  ALERT LIST
            // ══════════════════════════════════
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = alertHistory.reversed(),
                    key = { alert: AlertEvent -> alert.id }
                ) { alert: AlertEvent ->
                    AlertCard(
                        alert = alert,
                        onDismiss = { viewModel.dismissAlert() }
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════
//  ALERT CARD
// ════════════════════════════════════════

@Composable
fun AlertCard(
    alert: AlertEvent,
    onDismiss: () -> Unit = {}
) {
    val (icon, tint) = alertVisuals(alert.type)
    val severityColor = severityColor(alert.severity)
    val timeFormat = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Icon ──
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // ── Text content ──
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = tint
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Message
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp + Severity badge + Type badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = timeFormat.format(Date(alert.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )

                    SeverityBadge(severity = alert.severity)

                    AlertTypeBadge(type = alert.type)
                }
            }
        }
    }
}

// ════════════════════════════════════════
//  SEVERITY BADGE
// ════════════════════════════════════════

@Composable
fun SeverityBadge(severity: AlertSeverity) {
    val (text, color) = when (severity) {
        AlertSeverity.INFO     -> "Info" to Color(0xFF2196F3)
        AlertSeverity.WARNING  -> "Warning" to Color(0xFFFF9800)
        AlertSeverity.CRITICAL -> "Critical" to Color(0xFFF44336)
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// ════════════════════════════════════════
//  ALERT TYPE BADGE
// ════════════════════════════════════════

@Composable
fun AlertTypeBadge(type: AlertType) {
    val (text, color) = getAlertTypeInfo(type)

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

private fun getAlertTypeInfo(type: AlertType): Pair<String, Color> {
    return when (type) {
        // EEG Alerts
        AlertType.DROWSINESS -> "EEG" to Color(0xFFF44336)
        AlertType.HIGH_STRESS -> "EEG" to Color(0xFFFF9800)
        AlertType.MENTAL_FATIGUE -> "EEG" to Color(0xFFFF9800)
        AlertType.FOCUS_LOST -> "EEG" to Color(0xFFFF9800)
        AlertType.DEEP_SLEEP -> "EEG" to Color(0xFFF44336)

        // EOG Alerts
        AlertType.EYES_CLOSED -> "EOG" to Color(0xFFF44336)
        AlertType.EXCESSIVE_BLINKING -> "EOG" to Color(0xFFFF9800)
        AlertType.SLOW_BLINK_RATE -> "EOG" to Color(0xFF2196F3)
        AlertType.HIGH_PERCLOS -> "EOG" to Color(0xFFF44336)
        AlertType.PROLONGED_FIXATION -> "EOG" to Color(0xFFFF9800)

        // Signal Quality
        AlertType.SIGNAL_LOST -> "Signal" to Color(0xFFFF9800)
        AlertType.LOW_SNR -> "Signal" to Color(0xFFFF9800)
        AlertType.ARTIFACT_DETECTED -> "Signal" to Color(0xFFFF9800)
        AlertType.EEG_UNSTABLE -> "EEG" to Color(0xFFFF9800)
        AlertType.EOG_UNSTABLE -> "EOG" to Color(0xFFFF9800)

        // Connection
        AlertType.CONNECTION_LOST -> "Connection" to Color(0xFFF44336)
        AlertType.BLE_DISCONNECTED -> "BLE" to Color(0xFFF44336)
        AlertType.ESP32_DISCONNECTED -> "ESP32" to Color(0xFFF44336)

        // System
        AlertType.ANOMALY -> "System" to Color(0xFFF44336)
        AlertType.CALIBRATION_NEEDED -> "System" to Color(0xFF2196F3)
    }
}

// ════════════════════════════════════════
//  ALERT VISUALS — icon + color by type
// ════════════════════════════════════════

private fun alertVisuals(type: AlertType): Pair<ImageVector, Color> {
    return when (type) {
        // ══════════════════════════════════
        //  EEG ALERTS
        // ══════════════════════════════════
        AlertType.DROWSINESS -> Icons.Filled.Bedtime to Color(0xFFF44336)
        AlertType.HIGH_STRESS -> Icons.Filled.Psychology to Color(0xFFFF9800)
        AlertType.MENTAL_FATIGUE -> Icons.Filled.BatteryAlert to Color(0xFFFF9800)
        AlertType.FOCUS_LOST -> Icons.Filled.RemoveRedEye to Color(0xFFFF9800)
        AlertType.DEEP_SLEEP -> Icons.Filled.NightsStay to Color(0xFFF44336)

        // ══════════════════════════════════
        //  EOG ALERTS
        // ══════════════════════════════════
        AlertType.EYES_CLOSED -> Icons.Filled.VisibilityOff to Color(0xFFF44336)
        AlertType.EXCESSIVE_BLINKING -> Icons.Filled.RemoveRedEye to Color(0xFFFF9800)
        AlertType.SLOW_BLINK_RATE -> Icons.Filled.Visibility to Color(0xFF2196F3)
        AlertType.HIGH_PERCLOS -> Icons.Filled.VisibilityOff to Color(0xFFF44336)
        AlertType.PROLONGED_FIXATION -> Icons.Filled.CenterFocusStrong to Color(0xFFFF9800)

        // ══════════════════════════════════
        //  SIGNAL QUALITY ALERTS
        // ══════════════════════════════════
        AlertType.SIGNAL_LOST -> Icons.Filled.WifiOff to Color(0xFFFF9800)
        AlertType.LOW_SNR -> Icons.Filled.SignalCellularAlt to Color(0xFFFF9800)
        AlertType.ARTIFACT_DETECTED -> Icons.Filled.BrokenImage to Color(0xFFFF9800)
        AlertType.EEG_UNSTABLE -> Icons.Filled.Waves to Color(0xFFFF9800)
        AlertType.EOG_UNSTABLE -> Icons.Filled.RemoveRedEye to Color(0xFFFF9800)

        // ══════════════════════════════════
        //  CONNECTION ALERTS
        // ══════════════════════════════════
        AlertType.CONNECTION_LOST -> Icons.Filled.BluetoothDisabled to Color(0xFFF44336)
        AlertType.BLE_DISCONNECTED -> Icons.Filled.BluetoothDisabled to Color(0xFFF44336)
        AlertType.ESP32_DISCONNECTED -> Icons.Filled.LinkOff to Color(0xFFF44336)

        // ══════════════════════════════════
        //  SYSTEM ALERTS
        // ══════════════════════════════════
        AlertType.ANOMALY -> Icons.Filled.Warning to Color(0xFFF44336)
        AlertType.CALIBRATION_NEEDED -> Icons.Filled.Tune to Color(0xFF2196F3)
    }
}

// ════════════════════════════════════════
//  SEVERITY COLOR
// ════════════════════════════════════════

private fun severityColor(severity: AlertSeverity): Color {
    return when (severity) {
        AlertSeverity.INFO     -> Color(0xFF2196F3)
        AlertSeverity.WARNING  -> Color(0xFFFF9800)
        AlertSeverity.CRITICAL -> Color(0xFFF44336)
    }
}

// ════════════════════════════════════════
//  ALERT SUMMARY CARD (Optional - for Dashboard)
// ════════════════════════════════════════

@Composable
fun AlertSummaryCard(
    criticalCount: Int,
    warningCount: Int,
    infoCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (criticalCount > 0)
                Color(0xFFF44336).copy(alpha = 0.1f)
            else if (warningCount > 0)
                Color(0xFFFF9800).copy(alpha = 0.1f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (criticalCount > 0) Icons.Filled.Warning else Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = if (criticalCount > 0) Color(0xFFF44336) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${criticalCount + warningCount + infoCount} total alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (criticalCount > 0) {
                    AlertCountBadge(count = criticalCount, color = Color(0xFFF44336))
                }
                if (warningCount > 0) {
                    AlertCountBadge(count = warningCount, color = Color(0xFFFF9800))
                }
                if (infoCount > 0) {
                    AlertCountBadge(count = infoCount, color = Color(0xFF2196F3))
                }
            }
        }
    }
}

@Composable
fun AlertCountBadge(count: Int, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Text(
            text = "$count",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}