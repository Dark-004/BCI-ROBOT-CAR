package com.neurodrive.smartcar.data.model

data class ESP32Alert(
    val severity: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)