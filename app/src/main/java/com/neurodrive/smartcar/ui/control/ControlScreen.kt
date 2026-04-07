package com.neurodrive.smartcar.ui.control

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurodrive.smartcar.data.model.Command
import com.neurodrive.smartcar.data.model.CommandType
import com.neurodrive.smartcar.domain.LockStatus
import com.neurodrive.smartcar.ui.components.AccessibleButton
import com.neurodrive.smartcar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun ControlScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ControlViewModel = hiltViewModel()
) {
    val bioMode by viewModel.bioControlEnabled.collectAsStateWithLifecycle()
    val lastCommand by viewModel.lastCommand.collectAsStateWithLifecycle()
    val confidence by viewModel.confidence.collectAsStateWithLifecycle()
    val speed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val bleConnected by viewModel.bleConnected.collectAsStateWithLifecycle()
    val esp32Connected by viewModel.esp32Connected.collectAsStateWithLifecycle()
    val sensorData by viewModel.sensorData.collectAsStateWithLifecycle()
    val lockStatus by viewModel.lockStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Control") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show lock indicator in top bar when locked
                    if (lockStatus.isLocked) {
                        LockBadge(lockStatus.remainingSeconds)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    ConnectionDot("BLE", bleConnected)
                    Spacer(modifier = Modifier.width(4.dp))
                    ConnectionDot("ESP", esp32Connected)
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ══════════════════════════════════
            //  MODE SWITCH CARD
            // ══════════════════════════════════

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (bioMode)
                        GreenPrimary.copy(alpha = 0.1f)
                    else
                        BlueAccent.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (bioMode) "Bio-Signal Mode"
                            else "Manual Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (bioMode) "EEG/EOG controlling the car"
                            else "Touch buttons controlling the car",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = bioMode,
                        onCheckedChange = { viewModel.toggleBioControl(it) },
                        modifier = Modifier.semantics {
                            contentDescription = "Toggle bio-signal control"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════
            //  INFO CHIPS ROW
            // ══════════════════════════════════

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoChip("Speed", "$speed%")
                InfoChip("Confidence", "%.0f%%".format(confidence * 100))
                InfoChip(
                    label = "Command",
                    value = lastCommand?.type?.displayName ?: "NONE",
                    isLocked = lockStatus.isLocked
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ══════════════════════════════════
            //  SENSOR DATA
            // ══════════════════════════════════

            sensorData?.let { data ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DistanceIndicator("Front", data.distanceFront)
                        DistanceIndicator("Left", data.distanceLeft)
                        DistanceIndicator("Right", data.distanceRight)
                        DistanceIndicator("Rear", data.distanceRear)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════
            //  CONTROL AREA
            // ══════════════════════════════════

            if (!bioMode) {
                // Manual D-Pad
                ManualDPad(
                    onForward = {
                        haptic(context)
                        viewModel.sendManualCommand(CommandType.FORWARD)
                    },
                    onLeft = {
                        haptic(context)
                        viewModel.sendManualCommand(CommandType.TURN_LEFT)
                    },
                    onRight = {
                        haptic(context)
                        viewModel.sendManualCommand(CommandType.TURN_RIGHT)
                    },
                    onStop = {
                        haptic(context)
                        viewModel.sendManualCommand(CommandType.STOP)
                    },
                    onBackward = {
                        haptic(context)
                        viewModel.sendManualCommand(CommandType.BACKWARD)
                    }
                )
            } else {
                // ══════════════════════════════════
                //  BIO-SIGNAL MODE CARD WITH LOCK STATUS
                // ══════════════════════════════════

                BioSignalModeCard(
                    lastCommand = lastCommand,
                    lockStatus = lockStatus,
                    onForceUnlock = { viewModel.forceUnlock() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ══════════════════════════════════
            //  SPEED SLIDER
            // ══════════════════════════════════

            Text(
                "Max Speed: $speed%",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = speed.toFloat(),
                onValueChange = { viewModel.setSpeed(it.toInt()) },
                valueRange = 0f..100f,
                steps = 9,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = "Speed slider, current $speed percent"
                    }
            )

            Spacer(modifier = Modifier.weight(1f))

            // ══════════════════════════════════
            //  EMERGENCY STOP
            // ══════════════════════════════════

            AccessibleButton(
                text = "EMERGENCY STOP",
                onClick = {
                    haptic(context)
                    viewModel.emergencyStop()
                },
                containerColor = RedAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentDescription = "Emergency stop button"
            )
        }
    }
}

// ════════════════════════════════════════
//  BIO-SIGNAL MODE CARD
// ════════════════════════════════════════

@Composable
fun BioSignalModeCard(
    lastCommand: Command?,
    lockStatus: LockStatus,
    onForceUnlock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (lockStatus.isLocked)
                Color(0xFFFFE082).copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Icon & Title ──
            Icon(
                imageVector = if (lockStatus.isLocked) Icons.Default.Lock else Icons.Default.Memory,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (lockStatus.isLocked) Color(0xFFFF6F00) else GreenPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (lockStatus.isLocked) "Command Locked" else "Bio-signal Active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Current Command Display ──
            CommandDisplay(
                commandType = lastCommand?.type,
                isLocked = lockStatus.isLocked
            )

            // ══════════════════════════════════
            //  LOCK STATUS SECTION
            // ══════════════════════════════════

            AnimatedVisibility(
                visible = lockStatus.isLocked,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timer with color coding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = getTimerColor(lockStatus.remainingSeconds),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${lockStatus.remainingSeconds}s remaining",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = getTimerColor(lockStatus.remainingSeconds)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    LinearProgressIndicator(
                        progress = { lockStatus.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFF6F00),
                        trackColor = Color(0xFFFFE0B2),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Emergency Unlock Button
                    OutlinedButton(
                        onClick = onForceUnlock,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = SolidColor(Color(0xFFD32F2F))
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Force Unlock",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Hint when not locked ──
            if (!lockStatus.isLocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Commands will lock for 5 seconds",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  COMMAND DISPLAY
// ════════════════════════════════════════

@Composable
fun CommandDisplay(
    commandType: CommandType?,
    isLocked: Boolean
) {
    val displayText = commandType?.displayName ?: "---"
    val icon = when (commandType) {
        CommandType.FORWARD -> Icons.Default.KeyboardArrowUp
        CommandType.BACKWARD -> Icons.Default.KeyboardArrowDown
        CommandType.TURN_LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        CommandType.TURN_RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        CommandType.STOP -> Icons.Default.Stop
        CommandType.ACCELERATE -> Icons.Default.Speed
        CommandType.DECELERATE -> Icons.Default.Speed
        CommandType.EMERGENCY_STOP -> Icons.Default.Warning
        else -> Icons.Default.HelpOutline
    }

    val bgColor = when (commandType) {
        CommandType.FORWARD -> Color(0xFF4CAF50)
        CommandType.BACKWARD -> Color(0xFFFF9800)
        CommandType.TURN_LEFT -> Color(0xFF2196F3)
        CommandType.TURN_RIGHT -> Color(0xFF2196F3)
        CommandType.STOP -> Color(0xFFF44336)
        CommandType.EMERGENCY_STOP -> Color(0xFFD32F2F)
        CommandType.ACCELERATE -> Color(0xFF4CAF50)
        CommandType.DECELERATE -> Color(0xFFFF9800)
        else -> Color.Gray
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = if (isLocked) 0.3f else 0.2f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = bgColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = bgColor
            )

            if (isLocked) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFFF6F00),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  LOCK BADGE (for top bar)
// ════════════════════════════════════════

@Composable
fun LockBadge(remainingSeconds: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFE082)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = Color(0xFFFF6F00),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${remainingSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )
        }
    }
}

// ════════════════════════════════════════
//  HELPER FUNCTIONS
// ════════════════════════════════════════

@Composable
private fun getTimerColor(seconds: Int): Color {
    return when {
        seconds > 20 -> Color(0xFF4CAF50)  // Green
        seconds > 10 -> Color(0xFFFF9800)  // Orange
        else -> Color(0xFFF44336)          // Red
    }
}

// ════════════════════════════════════════
//  MANUAL D-PAD
// ════════════════════════════════════════

@Composable
fun ManualDPad(
    onForward: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onStop: () -> Unit,
    onBackward: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        DPadButton(Icons.Default.KeyboardArrowUp, "Forward", onForward)

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            DPadButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Left", onLeft)
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onStop,
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White  // ← White text on red
                )
            ) {
                Text("STOP", fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            DPadButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Right", onRight)
        }

        DPadButton(Icons.Default.KeyboardArrowDown, "Backward", onBackward)
    }
}

@Composable
fun DPadButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .size(72.dp)
            .semantics { contentDescription = "$desc button" },
        shape = CircleShape
    ) {
        Icon(icon, contentDescription = desc, modifier = Modifier.size(36.dp))
    }
}

@Composable
fun ConnectionDot(label: String, connected: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(
                    if (connected) Color.Green else Color.Red,
                    CircleShape
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun InfoChip(
    label: String,
    value: String,
    isLocked: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            if (isLocked && label == "Command") {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFFF6F00),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
fun DistanceIndicator(label: String, cm: Float) {
    val color = when {
        cm < 15f -> RedAlert
        cm < 30f -> AmberWarning
        else -> GreenPrimary
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            "%.0f".format(cm),
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text("cm", style = MaterialTheme.typography.labelSmall)
    }
}

@Suppress("DEPRECATION")
private fun haptic(context: android.content.Context) {
    val vibrator = context.getSystemService(
        android.content.Context.VIBRATOR_SERVICE
    ) as? Vibrator
    vibrator?.vibrate(VibrationEffect.createOneShot(30, 100))
}