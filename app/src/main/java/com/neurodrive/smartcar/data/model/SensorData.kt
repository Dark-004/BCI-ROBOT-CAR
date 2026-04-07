package com.neurodrive.smartcar.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SensorData(
    val distanceFront: Float = 0f,
    val distanceLeft: Float = 0f,
    val distanceRight: Float = 0f,
    val distanceRear: Float = 0f,
    val batteryLevel: Float = 100f,
    val temperature: Float = 25f,
    val speed: Float = 0f,
    val heading: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val hasObstacleFront: Boolean
        get() = distanceFront < 30f

    val hasObstacleRear: Boolean
        get() = distanceRear < 30f

    val hasObstacleLeft: Boolean
        get() = distanceLeft < 20f

    val hasObstacleRight: Boolean
        get() = distanceRight < 20f

    val hasAnyObstacle: Boolean
        get() = hasObstacleFront || hasObstacleRear || hasObstacleLeft || hasObstacleRight

    val isCriticalFront: Boolean
        get() = distanceFront < 15f

    val isCriticalRear: Boolean
        get() = distanceRear < 15f

    companion object {
        val EMPTY = SensorData()
    }
}