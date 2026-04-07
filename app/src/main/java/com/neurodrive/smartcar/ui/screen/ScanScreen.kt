// ui/screen/ScanScreen.kt
package com.neurodrive.smartcar.ui.screen

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neurodrive.smartcar.domain.BlePermissionHelper
import com.neurodrive.smartcar.ui.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val devices by viewModel.discoveredDevices.collectAsState()
    val isConnected by viewModel.connectionState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    // ✅ NEW: Track connecting state
    var isConnecting by remember { mutableStateOf(false) }
    var connectingDeviceAddress by remember { mutableStateOf("") }

    var permissionsGranted by remember {
        mutableStateOf(BlePermissionHelper.hasAllPermissions(context))
    }

    // ✅ When connection succeeds, reset connecting state
    LaunchedEffect(isConnected) {
        if (isConnected) {
            isConnecting = false
            Toast.makeText(context, "✅ Connected!", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (permissionsGranted) {
            viewModel.startScan()
        } else {
            Toast.makeText(
                context,
                "BLE permissions required for scanning",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val btEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NeuroDrive - BLE Scanner") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // ── Connection status ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isConnected ->
                            MaterialTheme.colorScheme.primaryContainer
                        isConnecting ->
                            MaterialTheme.colorScheme.tertiaryContainer
                        else ->
                            MaterialTheme.colorScheme.errorContainer
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isConnected -> "✅ Connected"
                            isConnecting -> "⏳ Connecting to $connectingDeviceAddress..."
                            else -> "❌ Disconnected"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (isConnecting) {
                        Spacer(modifier = Modifier.width(12.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Scan toggle button ──
            Button(
                onClick = {
                    if (isScanning) {
                        viewModel.stopScan()
                    } else {
                        if (!BlePermissionHelper.hasAllPermissions(context)) {
                            permissionLauncher.launch(
                                BlePermissionHelper.getRequiredPermissions()
                            )
                            return@Button
                        }

                        val adapter = BluetoothAdapter.getDefaultAdapter()
                        if (adapter == null || !adapter.isEnabled) {
                            btEnableLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )
                            return@Button
                        }

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            val locationManager = context.getSystemService(
                                android.content.Context.LOCATION_SERVICE
                            ) as android.location.LocationManager
                            val isLocationOn = locationManager.isProviderEnabled(
                                android.location.LocationManager.GPS_PROVIDER
                            ) || locationManager.isProviderEnabled(
                                android.location.LocationManager.NETWORK_PROVIDER
                            )
                            if (!isLocationOn) {
                                Toast.makeText(
                                    context,
                                    "Enable Location Services for BLE",
                                    Toast.LENGTH_LONG
                                ).show()
                                context.startActivity(
                                    Intent(
                                        android.provider.Settings
                                            .ACTION_LOCATION_SOURCE_SETTINGS
                                    )
                                )
                                return@Button
                            }
                        }

                        viewModel.startScan()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isConnecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isScanning) "⏹ Stop Scan" else "🔍 Start Scan")
            }

            if (isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Found ${devices.size} device(s)",
                style = MaterialTheme.typography.titleSmall
            )

            // ✅ NEW: Hint text
            if (devices.isNotEmpty() && !isConnected && !isConnecting) {
                Text(
                    "👆 Tap a device to connect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ══════════════════════════════════════
            //  DEVICE LIST — FIXED CLICK HANDLER
            // ══════════════════════════════════════
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = devices,
                    key = { it.address }  // ✅ Stable key prevents recomposition issues
                ) { device ->

                    val deviceName = try {
                        device.name ?: "Unknown Device"
                    } catch (_: SecurityException) {
                        "Unknown Device"
                    }

                    val isThisDeviceConnecting =
                        isConnecting && connectingDeviceAddress == device.address

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {

                                // ✅ ADD THIS LOG to verify click works
                                android.util.Log.d(
                                    "ScanScreen",
                                    "👆 DEVICE TAPPED: ${device.address}"
                                )
                            // ✅ Guard: Don't connect if already connecting
                            if (isConnecting || isConnected) {
                                Toast.makeText(
                                    context,
                                    if (isConnected)
                                        "Already connected!"
                                    else
                                        "Connection in progress...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Card
                            }

                            // ✅ Check BLUETOOTH_CONNECT permission (Android 12+)
                            if (!BlePermissionHelper.hasAllPermissions(context)) {
                                Toast.makeText(
                                    context,
                                    "Missing Bluetooth permissions",
                                    Toast.LENGTH_SHORT
                                ).show()
                                permissionLauncher.launch(
                                    BlePermissionHelper.getRequiredPermissions()
                                )
                                return@Card
                            }

                            // ✅ Stop scan and connect
                            viewModel.stopScan()
                            isConnecting = true
                            connectingDeviceAddress = device.address

                            Toast.makeText(
                                context,
                                "Connecting to $deviceName...",
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.connectToDevice(device)
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isThisDeviceConnecting ->
                                    MaterialTheme.colorScheme.tertiaryContainer
                                else ->
                                    MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deviceName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isThisDeviceConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }

            // ── Disconnect button (shown when connected) ──
            if (isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.disconnect()
                        isConnecting = false
                        connectingDeviceAddress = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("🔌 Disconnect")
                }
            }
        }
    }
}