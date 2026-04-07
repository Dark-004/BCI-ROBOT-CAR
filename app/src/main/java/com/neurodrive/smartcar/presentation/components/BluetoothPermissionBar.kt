package com.neurodrive.smartcar.presentation.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BluetoothPermissionBar(
    state: BluetoothPermissionState,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !state.isReady,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onRequestPermissions() },
            colors = CardDefaults.cardColors(
                containerColor = when {
                    !state.allPermissionsGranted -> Color(0xFFFF5252)
                    !state.bluetoothEnabled -> Color(0xFFFF9800)
                    !state.locationEnabled -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon
                Icon(
                    imageVector = when {
                        !state.allPermissionsGranted -> Icons.Default.Warning
                        !state.bluetoothEnabled -> Icons.Default.BluetoothDisabled
                        !state.locationEnabled -> Icons.Default.LocationOff
                        else -> Icons.Default.Bluetooth
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )

                // Message
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !state.allPermissionsGranted -> "Permissions Required"
                            !state.bluetoothEnabled -> "Bluetooth is Off"
                            !state.locationEnabled -> "Location is Off"
                            else -> "Ready"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = state.message ?: "Tap to fix",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                // Action button
                Text(
                    text = "FIX",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}