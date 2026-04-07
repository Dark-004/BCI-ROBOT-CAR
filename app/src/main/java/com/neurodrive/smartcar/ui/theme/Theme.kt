package com.neurodrive.smartcar.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Purple80,
    secondary = GreenPrimary,
    tertiary = BlueAccent,
    background = BackgroundLight,
    error = RedAlert
)

@Composable
fun NeuroDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}