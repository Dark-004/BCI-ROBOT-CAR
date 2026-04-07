package com.neurodrive.smartcar.presentation.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

data class BluetoothPermissionState(
    val allPermissionsGranted: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val locationEnabled: Boolean = false,
    val isReady: Boolean = false,
    val message: String? = null
)

@Composable
fun rememberBluetoothPermissionState(
    onReady: () -> Unit = {},
    onDenied: () -> Unit = {}
): Pair<BluetoothPermissionState, () -> Unit> {

    val context = LocalContext.current
    var permissionState by remember { mutableStateOf(BluetoothPermissionState()) }

    // ═══ REQUIRED PERMISSIONS ═══
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // ═══ PERMISSION LAUNCHER ═══
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }

        if (allGranted) {
            val btEnabled = isBluetoothEnabled(context)
            val locEnabled = isLocationEnabled(context)

            permissionState = BluetoothPermissionState(
                allPermissionsGranted = true,
                bluetoothEnabled = btEnabled,
                locationEnabled = locEnabled,
                isReady = btEnabled && locEnabled,
                message = when {
                    !btEnabled -> "Please turn on Bluetooth"
                    !locEnabled -> "Please enable Location services"
                    else -> null
                }
            )

            if (btEnabled && locEnabled) {
                onReady()
            }
        } else {
            permissionState = BluetoothPermissionState(
                allPermissionsGranted = false,
                message = "Bluetooth permissions are required for scanning"
            )
            onDenied()
        }
    }

    // ═══ BLUETOOTH ENABLE LAUNCHER ═══
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val btEnabled = isBluetoothEnabled(context)
        val locEnabled = isLocationEnabled(context)

        permissionState = permissionState.copy(
            bluetoothEnabled = btEnabled,
            locationEnabled = locEnabled,
            isReady = permissionState.allPermissionsGranted && btEnabled && locEnabled,
            message = when {
                !btEnabled -> "Bluetooth is still off"
                !locEnabled -> "Please enable Location services"
                else -> null
            }
        )

        if (permissionState.isReady) {
            onReady()
        }
    }

    // ═══ CHECK ON FIRST COMPOSITION ═══
    LaunchedEffect(Unit) {
        val allGranted = hasAllPermissions(context, requiredPermissions)
        val btEnabled = isBluetoothEnabled(context)
        val locEnabled = isLocationEnabled(context)

        permissionState = BluetoothPermissionState(
            allPermissionsGranted = allGranted,
            bluetoothEnabled = btEnabled,
            locationEnabled = locEnabled,
            isReady = allGranted && btEnabled && locEnabled,
            message = when {
                !allGranted -> "Bluetooth permissions needed"
                !btEnabled -> "Please turn on Bluetooth"
                !locEnabled -> "Please enable Location services"
                else -> null
            }
        )

        if (permissionState.isReady) {
            onReady()
        }
    }

    // ═══ REQUEST FUNCTION ═══
    val requestPermissions: () -> Unit = {
        val allGranted = hasAllPermissions(context, requiredPermissions)

        if (!allGranted) {
            // Step 1: Request permissions
            permissionLauncher.launch(requiredPermissions)
        } else if (!isBluetoothEnabled(context)) {
            // Step 2: Enable Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        } else if (!isLocationEnabled(context)) {
            // Step 3: Enable Location
            val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            context.startActivity(locationIntent)
        } else {
            // All ready
            permissionState = permissionState.copy(isReady = true, message = null)
            onReady()
        }
    }

    return Pair(permissionState, requestPermissions)
}

// ═══ UTILITY FUNCTIONS ═══

private fun hasAllPermissions(context: Context, permissions: Array<String>): Boolean {
    return permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

private fun isBluetoothEnabled(context: Context): Boolean {
    return try {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter?.isEnabled == true
    } catch (_: Exception) {
        false
    }
}

private fun isLocationEnabled(context: Context): Boolean {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (_: Exception) {
        false
    }
}