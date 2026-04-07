package com.neurodrive.smartcar.data.model

data class ESP32SensorData(
    val distance: Float = 999f,
    val obstacleDetected: Boolean = false,
    val emergencyStop: Boolean = false,
    val motorASpeed: Int = 0,
    val motorBSpeed: Int = 0,
    val motorADirection: String = "STOP",
    val motorBDirection: String = "STOP",
    val currentCommand: String = "STOP",
    val currentSpeed: Int = 0,
    val commandCount: Long = 0,
    val uptimeSeconds: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)