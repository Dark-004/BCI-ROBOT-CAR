package com.neurodrive.smartcar.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neurodrive.smartcar.data.repository.AlertSeverity
import com.neurodrive.smartcar.data.repository.BioSignalRepository.SignalQualityLevel
import com.neurodrive.smartcar.domain.LockStatus
import com.neurodrive.smartcar.ui.alerts.AlertViewModel
import com.neurodrive.smartcar.presentation.components.BluetoothPermissionBar
import com.neurodrive.smartcar.ui.components.SignalGraph
import com.neurodrive.smartcar.ui.components.StatusGauge
import com.neurodrive.smartcar.presentation.components.rememberBluetoothPermissionState
import com.neurodrive.smartcar.ui.components.AllSignalGraphs




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToControl: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCalibration: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {}
) {

    val alertViewModel: AlertViewModel = hiltViewModel()

    // ══════════════════════════════════
    //  BLUETOOTH PERMISSION
    // ══════════════════════════════════

    val (btPermissionState, requestPermissions) = rememberBluetoothPermissionState(
        onReady = {
            // Do NOT auto-scan — user must tap button
        },
        onDenied = {
            viewModel.setScanMessage("Bluetooth permissions are required. Please grant all permissions.")
        }
    )

    // ══════════════════════════════════
    //  COLLECT STATES
    // ══════════════════════════════════

    val bleConnected by viewModel.bleConnected.collectAsStateWithLifecycle()
    val btConnected by viewModel.btConnected.collectAsStateWithLifecycle()
    val currentSignal by viewModel.currentSignal.collectAsStateWithLifecycle()
    val signalHistory by viewModel.signalHistory.collectAsStateWithLifecycle()
    val currentCommand by viewModel.currentCommand.collectAsStateWithLifecycle()
    val signalQuality by viewModel.signalQuality.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val classicDevices by viewModel.discoveredClassicDevices.collectAsStateWithLifecycle()
    val connectionMessage by viewModel.btConnectionMessage.collectAsStateWithLifecycle()
    val bleDevices by viewModel.discoveredBleDevices.collectAsStateWithLifecycle()
    val eegGraphData by viewModel.eegGraphData.collectAsState()
    val eogGraphData by viewModel.eogGraphData.collectAsState()
    val alphaData by viewModel.alphaGraphData.collectAsState()
    val betaData by viewModel.betaGraphData.collectAsState()
    val thetaData by viewModel.thetaGraphData.collectAsState()
    val gammaData by viewModel.gammaGraphData.collectAsState()

    // ══════════════════════════════════
    //  LOCK STATUS — NEW!
    // ══════════════════════════════════

    val lockStatus by viewModel.lockStatus.collectAsStateWithLifecycle()

    val alertList by alertViewModel.alerts.collectAsStateWithLifecycle()
    val currentAlert by alertViewModel.currentAlert.collectAsStateWithLifecycle()

    // ══════════════════════════════════
    //  SCAFFOLD
    // ══════════════════════════════════

    Scaffold(
        topBar = {
            DashboardTopBar(
                alertCount = alertList.size,
                onNavigateToAlerts = onNavigateToAlerts,

                onNavigateToProfile = onNavigateToProfile,
                onNavigateToCalibration = onNavigateToCalibration,
                onNavigateToHelp = onNavigateToHelp
            )
        },
        bottomBar = {
            DashboardBottomBar(
                onNavigateToControl = onNavigateToControl,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Permission bar ──
            BluetoothPermissionBar(
                state = btPermissionState,
                onRequestPermissions = requestPermissions
            )

            // ══════════════════════════════════
            //  COMMAND LOCK INDICATOR — NEW!
            // ══════════════════════════════════

            AnimatedVisibility(
                visible = lockStatus.isLocked,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                LockIndicatorCard(
                    lockStatus = lockStatus,
                    onUnlockClick = { viewModel.forceUnlock() }
                )
            }

            // ── Connection message ──
            ConnectionMessageBar(
                message = connectionMessage,
                isConnected = btConnected
            )

            // ── Alert banner ──
            AnimatedVisibility(
                visible = currentAlert != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                currentAlert?.let { alert ->
                    val bannerColor = when (alert.severity) {
                        AlertSeverity.CRITICAL -> MaterialTheme.colorScheme.error
                        AlertSeverity.WARNING -> Color(0xFFFF9800)
                        AlertSeverity.INFO -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = bannerColor.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (alert.severity) {
                                    AlertSeverity.CRITICAL -> Icons.Default.Warning
                                    else -> Icons.Default.Info
                                },
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = bannerColor
                                )
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { alertViewModel.dismissAlert() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = bannerColor
                                )
                            }
                        }
                    }
                }
            }

            // ── Error message ──
            ErrorBar(
                error = errorMessage,
                onDismiss = { viewModel.clearError() }
            )

            // ── Connection status chips ──
            ConnectionStatusRow(
                bleConnected = bleConnected,
                btConnected = btConnected
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Signal quality ──
            SignalQualityBadge(quality = signalQuality)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Gauges ──
            GaugeRow(signal = currentSignal)

            Spacer(modifier = Modifier.height(8.dp))

            // ── SNR display ──
            SnrDisplay(signal = currentSignal)

            Spacer(modifier = Modifier.height(8.dp))

            // ── Current command ──
            CommandCard(
                command = currentCommand,
                isLocked = lockStatus.isLocked,
                remainingSeconds = lockStatus.remainingSeconds
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Live EEG graph ──
           /* Text("Live EEG Stream", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            SignalGraph(signals = signalHistory)*/
            // GRAPHS SECTION
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Live Brain Activity",
                        style = MaterialTheme.typography.titleLarge
                    )

                    // All graphs
                    AllSignalGraphs(
                        eegData = eegGraphData,
                        eogData = eogGraphData,
                        alphaData = alphaData,
                        betaData = betaData,
                        thetaData = thetaData,
                        gammaData = gammaData
                    )
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            // ── Scan status ──
            ScanStatusSection(
                scanMessage = scanMessage,
                isScanning = isScanning
            )

            // ── Scan & Connect buttons ──
            ScanButtonsRow(
                isReady = btPermissionState.isReady,
                isScanning = isScanning,
                btConnected = btConnected,
                onScanBle = { viewModel.startBLEScan() },
                onScanClassic = { viewModel.startClassicDiscovery() },
                onRequestPermissions = requestPermissions
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ════════════════════════════════
            //  DISCOVERED BLE DEVICES
            // ════════════════════════════════

            if (bleDevices.isNotEmpty()) {
                Text(
                    "Found BLE Devices:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                bleDevices.forEach { device ->
                    key(device.address) {
                        BleDeviceCard(
                            device = device,
                            isReady = btPermissionState.isReady,
                            onConnect = { viewModel.connectBleDevice(device) },
                            onRequestPermissions = requestPermissions
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ════════════════════════════════
            //  DISCOVERED CLASSIC BT DEVICES
            // ════════════════════════════════

            if (classicDevices.isNotEmpty()) {
                Text(
                    "Found Classic Bluetooth Devices:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                classicDevices.forEach { device ->
                    key(device.address) {
                        ClassicDeviceCard(
                            device = device,
                            isReady = btPermissionState.isReady,
                            onConnect = { address -> viewModel.connectESP32(address) },
                            onRequestPermissions = requestPermissions
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Disconnect buttons ──
            DisconnectButtons(
                bleConnected = bleConnected,
                btConnected = btConnected,
                onDisconnectBle = { viewModel.disconnectBle() },
                onDisconnectEsp = { viewModel.disconnectESP32() }
            )
        }
    }
}

// ════════════════════════════════════════
//  LOCK INDICATOR CARD — NEW!
// ════════════════════════════════════════

@Composable
fun LockIndicatorCard(
    lockStatus: LockStatus,
    onUnlockClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE082) // Amber
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFFF6F00),
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Command Locked",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = if (lockStatus.lockedCommand.isNotEmpty()) {
                            "Active: ${lockStatus.lockedCommand}"
                        } else {
                            "Command active"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF795548)
                    )
                }

                // Countdown timer badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        lockStatus.remainingSeconds > 20 -> Color(0xFF4CAF50)
                        lockStatus.remainingSeconds > 10 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                ) {
                    Text(
                        text = "${lockStatus.remainingSeconds}s",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            // Emergency unlock button
            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD32F2F)
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = "Unlock",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Emergency Unlock",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  BLE DEVICE CARD
// ════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
private fun BleDeviceCard(
    device: android.bluetooth.BluetoothDevice,
    isReady: Boolean,
    onConnect: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    val deviceName = remember(device.address) {
        try {
            device.name ?: "Unknown BLE Device"
        } catch (_: SecurityException) {
            "Unknown BLE Device"
        }
    }

    val isEsp32 = deviceName.contains("ESP32", ignoreCase = true) ||
            deviceName.contains("NeuroDrive", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = {
            if (isReady) onConnect() else onRequestPermissions()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isEsp32)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isEsp32)
                    Icons.Default.DirectionsCar
                else
                    Icons.Default.BluetoothSearching,
                contentDescription = null,
                tint = if (isEsp32)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            if (isEsp32) {
                Text(
                    text = "TAP TO CONNECT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  CLASSIC BT DEVICE CARD
// ════════════════════════════════════════

@SuppressLint("MissingPermission")
@Composable
private fun ClassicDeviceCard(
    device: android.bluetooth.BluetoothDevice,
    isReady: Boolean,
    onConnect: (String) -> Unit,
    onRequestPermissions: () -> Unit
) {
    val deviceName = remember(device.address) {
        try {
            device.name ?: "Unknown Device"
        } catch (_: SecurityException) {
            "Unknown Device"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = {
            if (isReady) onConnect(device.address) else onRequestPermissions()
        }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = deviceName,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  COMMAND CARD — UPDATED WITH LOCK STATUS
// ════════════════════════════════════════

@Composable
private fun CommandCard(
    command: com.neurodrive.smartcar.data.model.Command?,
    isLocked: Boolean = false,
    remainingSeconds: Int = 0
) {
    command?.let { cmd ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isLocked)
                    Color(0xFFFFE082).copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = if (isLocked) Color(0xFFFF6F00) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Command: ${cmd.type.displayName}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Source: ${cmd.source.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (isLocked) {
                        Text(
                            text = "Locked for ${remainingSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6F00),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${(cmd.value * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Confidence: ${(cmd.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════
//  TOP BAR
// ════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    alertCount: Int,
    onNavigateToAlerts: () -> Unit,
    //onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("NeuroDrive Dashboard") },
        actions = {
            BadgedBox(
                badge = {
                    if (alertCount > 0) {
                        Badge { Text("$alertCount") }
                    }
                }
            ) {
                IconButton(onClick = onNavigateToAlerts) {
                    Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                }
            }
           /* IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }*/
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Profile") },
                        onClick = {
                            menuExpanded = false
                            android.util.Log.d("NAVIGATION", "Profile clicked - navigating to Profile screen")
                            onNavigateToProfile()  // This should trigger navigation
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        }
                    )
                   /* DropdownMenuItem(
                        text = { Text("Calibration") },
                        onClick = { menuExpanded = false; onNavigateToCalibration() },
                        leadingIcon = { Icon(Icons.Default.Tune, null) }
                    )*/
                    DropdownMenuItem(
                        text = { Text("Help") },
                        onClick = { menuExpanded = false; onNavigateToHelp() },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, null) }
                    )
                }
            }
        }
    )
}

// ════════════════════════════════════════
//  BOTTOM BAR
// ════════════════════════════════════════

@Composable
private fun DashboardBottomBar(
    onNavigateToControl: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
            label = { Text("Dashboard") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToControl,
            icon = { Icon(Icons.Default.SportsEsports, contentDescription = "Control") },
            label = { Text("Control") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onNavigateToSettings,
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") }
        )
    }
}

// ════════════════════════════════════════
//  HELPER COMPOSABLES
// ════════════════════════════════════════

@Composable
private fun ConnectionMessageBar(
    message: String?,
    isConnected: Boolean
) {
    message?.let {
        Text(
            text = it,
            modifier = Modifier.padding(vertical = 4.dp),
            fontSize = 12.sp,
            color = if (isConnected) Color(0xFF4CAF50) else Color.Gray
        )
    }
}

@Composable
private fun ErrorBar(
    error: String?,
    onDismiss: () -> Unit
) {
    error?.let {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusRow(
    bleConnected: Boolean,
    btConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusChip("BLE Headband", bleConnected)
        StatusChip("ESP32 Car", btConnected)
    }
}

@Composable
private fun GaugeRow(
    signal: com.neurodrive.smartcar.data.model.BioSignal?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatusGauge(
            "EEG α",
            signal?.eegAlpha ?: 0f,
            maxValue = 20f
        )
        StatusGauge(
            "EEG β",
            signal?.eegBeta ?: 0f,
            maxValue = 20f
        )
        StatusGauge(
            "EMG",
            signal?.eog?.amplitude ?: 0f,
            maxValue = 200f
        )
    }
}

@Composable
private fun SnrDisplay(
    signal: com.neurodrive.smartcar.data.model.BioSignal?
) {
    signal?.let { sig ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SNR: ", style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.1f dB".format(sig.overallSnr),
                fontWeight = FontWeight.Bold,
                color = when {
                    sig.overallSnr > 15f -> Color(0xFF4CAF50)  // Good
                    sig.overallSnr > 10f -> Color(0xFFFF9800)  // Fair
                    sig.overallSnr > 5f -> Color(0xFFF44336)   // Poor
                    else -> Color.Gray                          // Very poor
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (sig.isStable) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (sig.isStable) "Stable" else "Unstable",
                tint = if (sig.isStable) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ScanStatusSection(
    scanMessage: String?,
    isScanning: Boolean
) {
    scanMessage?.let { msg ->
        Text(
            text = msg,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }

    if (isScanning) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun ScanButtonsRow(
    isReady: Boolean,
    isScanning: Boolean,
    btConnected: Boolean,
    onScanBle: () -> Unit,
    onScanClassic: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                if (isReady) onScanBle() else onRequestPermissions()
            },
            modifier = Modifier.weight(1f),
            enabled = !isScanning
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                when {
                    !isReady -> "Grant Permissions"
                    isScanning -> "Scanning..."
                    else -> "Scan BLE"
                }
            )
        }

        Button(
            onClick = {
                if (isReady) onScanClassic() else onRequestPermissions()
            },
            modifier = Modifier.weight(1f),
            enabled = !isScanning && !btConnected
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                if (!isReady) "Grant Permissions" else "Scan ESP32"
            )
        }
    }
}

@Composable
private fun DisconnectButtons(
    bleConnected: Boolean,
    btConnected: Boolean,
    onDisconnectBle: () -> Unit,
    onDisconnectEsp: () -> Unit
) {
    if (!bleConnected && !btConnected) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (bleConnected) {
            OutlinedButton(
                onClick = onDisconnectBle,
                modifier = Modifier.weight(1f)
            ) {
                Text("Disconnect BLE")
            }
        }
        if (btConnected) {
            OutlinedButton(
                onClick = onDisconnectEsp,
                modifier = Modifier.weight(1f)
            ) {
                Text("Disconnect Car")
            }
        }
    }
}

@Composable
fun SignalQualityBadge(quality: SignalQualityLevel) {
    val (label, color) = remember(quality) {
        when (quality) {
            SignalQualityLevel.NO_SIGNAL -> "No Signal" to Color.Gray
            SignalQualityLevel.POOR -> "Poor" to Color(0xFFF44336)
            SignalQualityLevel.FAIR -> "Fair" to Color(0xFFFF9800)
            SignalQualityLevel.GOOD -> "Good" to Color(0xFF4CAF50)
            SignalQualityLevel.EXCELLENT -> "Excellent" to Color(0xFF2196F3)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Signal Quality: ", style = MaterialTheme.typography.bodyMedium)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = color,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun StatusChip(label: String, isConnected: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(if (isConnected) "$label ✓" else "$label ✗") },
        leadingIcon = {
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isConnected)
                    MaterialTheme.colorScheme.secondary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.secondary.copy(alpha = .15f)
            else
                MaterialTheme.colorScheme.error.copy(alpha = .15f)
        )
    )
}