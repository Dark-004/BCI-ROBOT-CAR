package com.neurodrive.smartcar.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ════════════════════════════════════════════════════════════════
//  MENTAL STATE ENUM
// ════════════════════════════════════════════════════════════════

enum class MentalState(val code: Int, val displayName: String) {
    FOCUSED(1, "Focused"),
    RELAXED(2, "Relaxed"),
    NEUTRAL(3, "Neutral"),
    ALERT(4, "Alert"),
    LIGHT_SLEEP(5, "Drowsy"),
    STRESSED(6, "Stressed"),
    CONCENTRATING(7, "Concentrating"),
    DAYDREAMING(8, "Daydreaming"),
    MEDITATIVE(9, "Meditating"),
    EYES_OPEN(10, "Eyes Open"),
    EYES_CLOSED(11, "Eyes Closed"),
    BLINKING(12, "Blinking"),
    CALIBRATING(20, "Calibrating"),
    DEEP_SLEEP(code=21, displayName="Deep Sleep"),
    UNKNOWN(0, "Unknown");

    companion object {
        fun fromCode(code: Int): MentalState {
            return entries.find { it.code == code } ?: UNKNOWN
        }

        fun fromAlphaBetaRatio(ratio: Float): MentalState {
            return when {
                ratio > 2.0f -> RELAXED
                ratio > 1.5f -> EYES_CLOSED
                ratio > 1.0f -> NEUTRAL
                ratio > 0.5f -> FOCUSED
                else -> CONCENTRATING
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  SACCADE DIRECTION ENUM
// ════════════════════════════════════════════════════════════════

enum class SaccadeDirection(val code: Int, val displayName: String) {
    NONE(0, "None"),
    LEFT(1, "Left"),
    RIGHT(2, "Right"),
    UP(3, "Up"),
    DOWN(4, "Down");

    companion object {
        fun fromCode(code: Int): SaccadeDirection {
            return values().find { it.code == code } ?: NONE
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  ALERT LEVEL ENUM
// ════════════════════════════════════════════════════════════════

enum class AlertLevel(val code: Int, val displayName: String, val color: Long) {
    NORMAL(0, "Normal", 0xFF4CAF50),      // Green
    CAUTION(1, "Caution", 0xFFFFEB3B),    // Yellow
    WARNING(2, "Warning", 0xFFFF9800),    // Orange
    CRITICAL(3, "Critical", 0xFFF44336);  // Red

    companion object {
        fun fromCode(code: Int): AlertLevel {
            return values().find { it.code == code } ?: NORMAL
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  EEG SIGNAL DATA
// ════════════════════════════════════════════════════════════════

@Parcelize
data class EEGSignal(
    // Raw filtered value - FOR GRAPHING WAVEFORM
    val filteredValue: Float = 0f,

    // Band Powers (in µV²) - FOR GRAPHING BAND POWERS
    val delta: Float = 0f,       // Delta waves (0.5-4 Hz) - Deep sleep
    val theta: Float = 0f,       // Theta waves (4-8 Hz) - Drowsiness, meditation
    val alpha: Float = 0f,       // Alpha waves (8-13 Hz) - Relaxation, closed eyes
    val beta: Float = 0f,        // Beta waves (13-30 Hz) - Focus, concentration
    val gamma: Float = 0f,       // Gamma waves (30+ Hz) - High cognition
    val totalPower: Float = 0f,  // Total power across all bands

    // Dominant frequency band
    val dominantBand: String = "",

    // Ratios
    val alphaBetaRatio: Float = 0f,    // Alpha/Beta - Relaxation vs Focus
    val thetaAlphaRatio: Float = 0f,   // Theta/Alpha - Drowsiness indicator
    val thetaBetaRatio: Float = 0f,    // Theta/Beta - Attention indicator
    val deltaAlphaRatio: Float = 0f,   // Delta/Alpha - Sleep depth

    // Cognitive Indices
    val focusIndex: Float = 0f,        // Beta / (Alpha + Theta)
    val relaxationIndex: Float = 0f,   // Alpha / Beta
    val drowsinessIndex: Float = 0f,   // (Theta + Alpha) / Beta
    val engagementIndex: Float = 0f,   // Beta / (Alpha + Theta)

    // Mental State Classification
    val mentalState: MentalState = MentalState.UNKNOWN,

    // Signal Quality
    val snr: Float = 0f,               // Signal-to-noise ratio (dB)
    val isStable: Boolean = false,
    val hasArtifact: Boolean = false,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    // Computed properties
    val isRelaxed: Boolean
        get() = mentalState == MentalState.RELAXED || mentalState == MentalState.MEDITATIVE

    val isFocused: Boolean
        get() = mentalState == MentalState.FOCUSED || mentalState == MentalState.ALERT

    val isDrowsy: Boolean
        get() = drowsinessIndex > 1.5f || mentalState == MentalState.LIGHT_SLEEP

    val isAsleep: Boolean
        get() = mentalState == MentalState.DEEP_SLEEP || mentalState == MentalState.LIGHT_SLEEP

    val qualityPercentage: Int
        get() = when {
            !isStable -> 0
            snr > 20f -> 100
            snr > 15f -> 80
            snr > 10f -> 60
            snr > 5f -> 40
            snr > 0f -> 20
            else -> 0
        }

    companion object {
        val EMPTY = EEGSignal()

        /**
         * Create EEGSignal from SignalFilter.FilteredEEG
         */
        fun fromFilteredEEG(filtered: com.neurodrive.smartcar.domain.SignalFilter.FilteredEEG): EEGSignal {
            return EEGSignal(
                filteredValue = filtered.filteredValue,
                delta = filtered.delta,
                theta = filtered.theta,
                alpha = filtered.alpha,
                beta = filtered.beta,
                gamma = filtered.gamma,
                totalPower = filtered.totalPower,
                dominantBand = filtered.dominantBand,
                alphaBetaRatio = filtered.alphaBetaRatio,
                thetaAlphaRatio = filtered.thetaAlphaRatio,
                thetaBetaRatio = 0f, // Not in FilteredEEG, calculate if needed
                deltaAlphaRatio = 0f, // Not in FilteredEEG, calculate if needed
                focusIndex = filtered.focusIndex,
                relaxationIndex = filtered.relaxationIndex,
                drowsinessIndex = filtered.drowsinessIndex,
                engagementIndex = 0f, // Same as focusIndex
                mentalState = filtered.mentalState,
                snr = filtered.snr,
                isStable = filtered.isStable,
                hasArtifact = filtered.hasArtifact,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  EOG SIGNAL DATA
// ════════════════════════════════════════════════════════════════

@Parcelize
data class EOGSignal(
    val horizontalEOG: Float = 0f,        // ✅ ADD THIS
    val verticalEOG: Float = 0f,          // ✅ ADD THIS
    // Raw filtered value - FOR GRAPHING WAVEFORM
    val filteredValue: Float = 0f,

    // Amplitude
    val amplitude: Float = 0f,         // Peak-to-peak amplitude

    // Blink Detection
    val isBlinkDetected: Boolean = false,
    val blinkStrength: Float = 0f,     // Blink amplitude
    val blinkCount: Int = 0,           // Total blinks in session
    val blinkRate: Float = 0f,         // Blinks per minute

    // Saccade Detection (Rapid Eye Movement)
    val isSaccadeDetected: Boolean = false,
    val saccadeDirection: SaccadeDirection = SaccadeDirection.NONE,
    val saccadeVelocity: Float = 0f,   // Degrees per second
    val saccadeCount: Int = 0,         // Total saccades in session

    // Eye State
    val eyePosition: Float = 0f,       // Estimated horizontal position (-1 to 1)
    val eyeOpenness: Float = 1f,       // 0 = closed, 1 = fully open
    val isEyesClosed: Boolean = false,

    // Fixation
    val isFixating: Boolean = false,
    val fixationDuration: Long = 0L,   // ms

    // Drowsiness Indicators
    val perclos: Float = 0f,           // Percentage of eye closure (0-1)
    val avgBlinkDuration: Float = 0f,  // Average blink duration (ms)

    // Signal Quality
    val snr: Float = 0f,
    val isStable: Boolean = false,
    val hasArtifact: Boolean = false,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    // Computed properties
    val isDrowsyFromEOG: Boolean
        get() = perclos > 0.15f || blinkRate < 10f || blinkRate > 25f || avgBlinkDuration > 300f

    val isLookingLeft: Boolean
        get() = saccadeDirection == SaccadeDirection.LEFT || eyePosition < -0.3f

    val isLookingRight: Boolean
        get() = saccadeDirection == SaccadeDirection.RIGHT || eyePosition > 0.3f

    val isLookingStraight: Boolean
        get() = eyePosition in -0.3f..0.3f

    val eyeOpennessPercentage: Int
        get() = (eyeOpenness * 100).toInt().coerceIn(0, 100)

    val qualityPercentage: Int
        get() = when {
            !isStable -> 0
            snr > 15f -> 100
            snr > 10f -> 80
            snr > 5f -> 60
            snr > 0f -> 40
            else -> 20
        }

    companion object {
        val EMPTY = EOGSignal()

        /**
         * Create EOGSignal from SignalFilter.FilteredEOG
         */
        fun fromFilteredEOG(filtered: com.neurodrive.smartcar.domain.SignalFilter.FilteredEOG): EOGSignal {
            return EOGSignal(
                filteredValue = filtered.filteredValue,
                amplitude = filtered.amplitude,
                isBlinkDetected = filtered.isBlinkDetected,
                blinkStrength = filtered.blinkStrength,
                blinkCount = 0, // Track separately in repository
                blinkRate = 0f, // Track separately in repository
                isSaccadeDetected = filtered.isSaccadeDetected,
                saccadeDirection = filtered.saccadeDirection,
                saccadeVelocity = filtered.saccadeVelocity,
                saccadeCount = 0, // Track separately in repository
                eyePosition = filtered.eyePosition,
                eyeOpenness = filtered.eyeOpenness,
                isEyesClosed = filtered.eyeOpenness < 0.3f,
                isFixating = false, // Not in FilteredEOG
                fixationDuration = 0L,
                perclos = 0f, // Calculate from history
                avgBlinkDuration = 0f, // Calculate from history
                snr = filtered.snr,
                isStable = filtered.isStable,
                hasArtifact = filtered.hasArtifact,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  COMBINED BIO SIGNAL
// ════════════════════════════════════════════════════════════════

@Parcelize
data class BioSignal(
    // EEG Data
    val eeg: EEGSignal = EEGSignal(),

    // EOG Data
    val eog: EOGSignal = EOGSignal(),

    // Combined Metrics
    val drowsinessLevel: Float = 0f,   // 0-1 (combined EEG + EOG)
    val attentionLevel: Float = 0f,    // 0-1
    val stressLevel: Float = 0f,       // 0-1
    val fatigueLevel: Float = 0f,      // 0-1
    val alertLevel: AlertLevel = AlertLevel.NORMAL,

    // Overall Signal Quality
    val isStable: Boolean = false,
    val overallSnr: Float = 0f,

    // Session Info
    val sessionDuration: Long = 0L,    // ms since session start
    val samplesProcessed: Long = 0L,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    // ════════════════════════════════════════
    //  CONVENIENCE ACCESSORS (EEG)
    // ════════════════════════════════════════

    val eegAlpha: Float get() = eeg.alpha
    val eegBeta: Float get() = eeg.beta
    val eegTheta: Float get() = eeg.theta
    val eegDelta: Float get() = eeg.delta
    val eegGamma: Float get() = eeg.gamma
    val mentalState: MentalState get() = eeg.mentalState
    val focusIndex: Float get() = eeg.focusIndex
    val relaxationIndex: Float get() = eeg.relaxationIndex

    // ════════════════════════════════════════
    //  CONVENIENCE ACCESSORS (EOG)
    // ════════════════════════════════════════

    val isBlinkDetected: Boolean get() = eog.isBlinkDetected
    val isSaccadeDetected: Boolean get() = eog.isSaccadeDetected
    val saccadeDirection: SaccadeDirection get() = eog.saccadeDirection
    val eyeOpenness: Float get() = eog.eyeOpenness
    val eyePosition: Float get() = eog.eyePosition
    val blinkRate: Float get() = eog.blinkRate
    val perclos: Float get() = eog.perclos

    // ════════════════════════════════════════
    //  COMPUTED PROPERTIES
    // ════════════════════════════════════════

    val isDrowsy: Boolean
        get() = drowsinessLevel > 0.6f || eeg.isDrowsy || eog.isDrowsyFromEOG

    val isAlert: Boolean
        get() = attentionLevel > 0.6f && drowsinessLevel < 0.3f

    val isFatigued: Boolean
        get() = fatigueLevel > 0.6f

    val isStressed: Boolean
        get() = stressLevel > 0.7f || eeg.mentalState == MentalState.STRESSED

    val needsWarning: Boolean
        get() = alertLevel == AlertLevel.WARNING || alertLevel == AlertLevel.CRITICAL

    val overallQualityPercentage: Int
        get() = ((eeg.qualityPercentage + eog.qualityPercentage) / 2)

    val signalStatus: SignalStatus
        get() = when {
            !isStable -> SignalStatus.NO_SIGNAL
            overallQualityPercentage < 30 -> SignalStatus.POOR
            overallQualityPercentage < 60 -> SignalStatus.FAIR
            overallQualityPercentage < 80 -> SignalStatus.GOOD
            else -> SignalStatus.EXCELLENT
        }

    // ════════════════════════════════════════
    //  HELPER METHODS
    // ════════════════════════════════════════

    fun getRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        if (drowsinessLevel > 0.7f) {
            recommendations.add("Please stop and take a rest")
        } else if (drowsinessLevel > 0.5f) {
            recommendations.add("Consider taking a break soon")
        }

        if (attentionLevel < 0.3f) {
            recommendations.add("Stay focused on the road")
        }

        if (stressLevel > 0.7f) {
            recommendations.add("Try to relax - take deep breaths")
        }

        if (eog.perclos > 0.2f) {
            recommendations.add("Your eyes are closing frequently")
        }

        if (eog.blinkRate < 8f) {
            recommendations.add("Blink more often to avoid eye strain")
        }

        return recommendations
    }

    fun getDrowsinessDescription(): String {
        return when {
            drowsinessLevel > 0.8f -> "Very Drowsy - Stop Driving!"
            drowsinessLevel > 0.6f -> "Drowsy - Take a Break"
            drowsinessLevel > 0.4f -> "Slightly Drowsy"
            drowsinessLevel > 0.2f -> "Mildly Tired"
            else -> "Alert"
        }
    }

    fun getAttentionDescription(): String {
        return when {
            attentionLevel > 0.8f -> "Highly Focused"
            attentionLevel > 0.6f -> "Focused"
            attentionLevel > 0.4f -> "Moderate Attention"
            attentionLevel > 0.2f -> "Low Attention"
            else -> "Distracted"
        }
    }

    companion object {
        val EMPTY = BioSignal()

        /**
         * Create BioSignal from filtered signals (for real-time processing)
         */
        fun fromFilteredSignals(
            eegFiltered: com.neurodrive.smartcar.domain.SignalFilter.FilteredEEG,
            eogFiltered: com.neurodrive.smartcar.domain.SignalFilter.FilteredEOG,
            samplesProcessed: Long = 0L
        ): BioSignal {
            val eeg = EEGSignal.fromFilteredEEG(eegFiltered)
            val eog = EOGSignal.fromFilteredEOG(eogFiltered)
            return fromEEGAndEOG(eeg, eog, samplesProcessed)
        }

        /**
         * Original method - kept for compatibility
         */
        fun fromEEGAndEOG(
            eeg: EEGSignal,
            eog: EOGSignal,
            samplesProcessed: Long = 0L
        ): BioSignal {
            // Calculate combined metrics
            val eegDrowsiness = (eeg.drowsinessIndex / 2f).coerceIn(0f, 1f)
            val eogDrowsiness = when {
                eog.perclos > 0.15f -> 0.8f
                eog.eyeOpenness < 0.3f -> 0.7f
                eog.blinkRate < 10f || eog.blinkRate > 25f -> 0.5f
                else -> 0f
            }
            val drowsinessLevel = (eegDrowsiness * 0.6f + eogDrowsiness * 0.4f).coerceIn(0f, 1f)

            val attentionLevel = eeg.focusIndex.coerceIn(0f, 1f)

            val stressLevel = when (eeg.mentalState) {
                MentalState.STRESSED -> 0.9f
                MentalState.FOCUSED -> 0.4f
                MentalState.ALERT -> 0.3f
                else -> 0.2f
            }

            val fatigueLevel = (drowsinessLevel * 0.6f + (1f - attentionLevel) * 0.4f).coerceIn(0f, 1f)

            val alertLevel = when {
                drowsinessLevel > 0.8f || fatigueLevel > 0.8f -> AlertLevel.CRITICAL
                drowsinessLevel > 0.6f || fatigueLevel > 0.6f -> AlertLevel.WARNING
                drowsinessLevel > 0.4f || fatigueLevel > 0.4f -> AlertLevel.CAUTION
                else -> AlertLevel.NORMAL
            }

            val isStable = eeg.isStable && eog.isStable
            val overallSnr = (eeg.snr + eog.snr) / 2f

            return BioSignal(
                eeg = eeg,
                eog = eog,
                drowsinessLevel = drowsinessLevel,
                attentionLevel = attentionLevel,
                stressLevel = stressLevel,
                fatigueLevel = fatigueLevel,
                alertLevel = alertLevel,
                isStable = isStable,
                overallSnr = overallSnr,
                samplesProcessed = samplesProcessed,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
// ════════════════════════════════════════════════════════════════
//  SIGNAL STATUS ENUM
// ════════════════════════════════════════════════════════════════

enum class SignalStatus(val displayName: String, val color: Long) {
    NO_SIGNAL("No Signal", 0xFF9E9E9E),    // Gray
    POOR("Poor", 0xFFF44336),               // Red
    FAIR("Fair", 0xFFFF9800),               // Orange
    GOOD("Good", 0xFFFFEB3B),               // Yellow
    EXCELLENT("Excellent", 0xFF4CAF50)      // Green
}

// ════════════════════════════════════════════════════════════════
//  EOG EVENT DATA
// ════════════════════════════════════════════════════════════════

@Parcelize
data class EOGEvent(
    val type: EOGEventType,
    val timestamp: Long,
    val direction: SaccadeDirection = SaccadeDirection.NONE,
    val amplitude: Float = 0f,
    val duration: Long = 0L
) : Parcelable

enum class EOGEventType {
    BLINK,
    DOUBLE_BLINK,
    TRIPLE_BLINK,
    LONG_BLINK,
    SACCADE,
    FIXATION_START,
    FIXATION_END,
    EYES_CLOSED,
    EYES_OPENED,
    DROWSINESS_WARNING
}

// ════════════════════════════════════════════════════════════════
//  EEG EVENT DATA
// ════════════════════════════════════════════════════════════════

@Parcelize
data class EEGEvent(
    val type: EEGEventType,
    val timestamp: Long,
    val state: MentalState = MentalState.UNKNOWN,
    val value: Float = 0f
) : Parcelable

enum class EEGEventType {
    STATE_CHANGE,
    DROWSINESS_WARNING,
    FOCUS_PEAK,
    RELAXATION_PEAK,
    ALPHA_BURST,
    THETA_BURST,
    ARTIFACT_DETECTED,
    SIGNAL_LOST,
    SIGNAL_RECOVERED
}

// ════════════════════════════════════════════════════════════════
//  BLE PACKET DATA (matching ESP32)
// ════════════════════════════════════════════════════════════════

data class EEGPacket(
    val header: Byte = 0xEE.toByte(),
    val timestamp: Long,
    val filteredValue: Float,
    val alpha: Float,
    val beta: Float,
    val mentalState: Int,
    val checksum: Byte
) {
    companion object {
        const val SIZE = 19

        fun fromBytes(bytes: ByteArray): EEGPacket? {
            if (bytes.size < SIZE || bytes[0] != 0xEE.toByte()) return null

            // Parse packet (little-endian)
            val timestamp = bytes.sliceArray(1..4).toLong()
            val filteredValue = bytes.sliceArray(5..8).toFloat()
            val alpha = bytes.sliceArray(9..12).toFloat()
            val beta = bytes.sliceArray(13..16).toFloat()
            val mentalState = bytes[17].toInt() and 0xFF
            val checksum = bytes[18]

            // Verify checksum
            var calcChecksum: Byte = 0
            for (i in 0 until SIZE - 1) {
                calcChecksum = (calcChecksum.toInt() xor bytes[i].toInt()).toByte()
            }

            if (calcChecksum != checksum) return null

            return EEGPacket(
                timestamp = timestamp,
                filteredValue = filteredValue,
                alpha = alpha,
                beta = beta,
                mentalState = mentalState,
                checksum = checksum
            )
        }

        private fun ByteArray.toLong(): Long {
            var result = 0L
            for (i in indices) {
                result = result or ((this[i].toLong() and 0xFF) shl (i * 8))
            }
            return result
        }

        private fun ByteArray.toFloat(): Float {
            val bits = ((this[3].toInt() and 0xFF) shl 24) or
                    ((this[2].toInt() and 0xFF) shl 16) or
                    ((this[1].toInt() and 0xFF) shl 8) or
                    (this[0].toInt() and 0xFF)
            return java.lang.Float.intBitsToFloat(bits)
        }
    }
}

data class EOGPacket(
    val header: Byte = 0xE0.toByte(),
    val timestamp: Long,
    val filteredValue: Float,
    val amplitude: Float,
    val blinkDetected: Boolean,
    val saccadeDirection: SaccadeDirection,
    val checksum: Byte
) {
    companion object {
        const val SIZE = 16

        fun fromBytes(bytes: ByteArray): EOGPacket? {
            if (bytes.size < SIZE || bytes[0] != 0xE0.toByte()) return null

            val timestamp = bytes.sliceArray(1..4).toLong()
            val filteredValue = bytes.sliceArray(5..8).toFloat()
            val amplitude = bytes.sliceArray(9..12).toFloat()
            val blinkDetected = bytes[13] != 0.toByte()
            val saccadeDirection = SaccadeDirection.fromCode(bytes[14].toInt() and 0xFF)
            val checksum = bytes[15]

            // Verify checksum
            var calcChecksum: Byte = 0
            for (i in 0 until SIZE - 1) {
                calcChecksum = (calcChecksum.toInt() xor bytes[i].toInt()).toByte()
            }

            if (calcChecksum != checksum) return null

            return EOGPacket(
                timestamp = timestamp,
                filteredValue = filteredValue,
                amplitude = amplitude,
                blinkDetected = blinkDetected,
                saccadeDirection = saccadeDirection,
                checksum = checksum
            )
        }

        private fun ByteArray.toLong(): Long {
            var result = 0L
            for (i in indices) {
                result = result or ((this[i].toLong() and 0xFF) shl (i * 8))
            }
            return result
        }

        private fun ByteArray.toFloat(): Float {
            val bits = ((this[3].toInt() and 0xFF) shl 24) or
                    ((this[2].toInt() and 0xFF) shl 16) or
                    ((this[1].toInt() and 0xFF) shl 8) or
                    (this[0].toInt() and 0xFF)
            return java.lang.Float.intBitsToFloat(bits)
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  SESSION STATISTICS
// ════════════════════════════════════════════════════════════════

@Parcelize
data class SessionStatistics(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val durationMs: Long = 0L,

    // EEG Statistics
    val avgAlpha: Float = 0f,
    val avgBeta: Float = 0f,
    val avgTheta: Float = 0f,
    val avgFocusIndex: Float = 0f,
    val avgDrowsinessIndex: Float = 0f,
    val dominantState: MentalState = MentalState.UNKNOWN,
    val stateDistribution: Map<MentalState, Float> = emptyMap(),

    // EOG Statistics
    val totalBlinks: Int = 0,
    val avgBlinkRate: Float = 0f,
    val totalSaccades: Int = 0,
    val avgPerclos: Float = 0f,

    // Alert Statistics
    val drowsinessWarnings: Int = 0,
    val criticalAlerts: Int = 0,
    val avgDrowsinessLevel: Float = 0f,
    val avgAttentionLevel: Float = 0f,
    val maxDrowsinessLevel: Float = 0f,

    // Signal Quality
    val avgSignalQuality: Float = 0f,
    val signalLossPercentage: Float = 0f
) : Parcelable {

    val isOngoing: Boolean
        get() = endTime == 0L

    val formattedDuration: String
        get() {
            val seconds = durationMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> "${hours}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        }

    companion object {
        fun create(sessionId: String): SessionStatistics {
            return SessionStatistics(
                sessionId = sessionId,
                startTime = System.currentTimeMillis()
            )
        }
    }
}

// ... rest of your code (SignalStatus, EOGEvent, EEGEvent, etc.) stays the same ...

 /*
package com.neurodrive.smartcar.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ════════════════════════════════════════════════════════════════
//  ACTIVITY TYPE ENUM
// ════════════════════════════════════════════════════════════════

enum class ActivityType(val code: Int, val displayName: String) {
    NONE(0, "No Activity"),
    SUBTLE(1, "Subtle Movement"),
    MODERATE(2, "Moderate Movement"),
    INTENSE(3, "Intense Movement");

    companion object {
        fun fromCode(code: Int): ActivityType {
            return entries.find { it.code == code } ?: NONE
        }

        fun fromIntensity(intensity: Float): ActivityType {
            return when {
                intensity > 0.8f -> INTENSE
                intensity > 0.5f -> MODERATE
                intensity > 0.2f -> SUBTLE
                else -> NONE
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  SIGNAL QUALITY ENUM
// ════════════════════════════════════════════════════════════════

enum class SignalQuality(val code: Int, val displayName: String, val color: Long) {
    NO_SIGNAL(0, "No Signal", 0xFF9E9E9E),      // Gray
    POOR(1, "Poor", 0xFFF44336),                 // Red
    FAIR(2, "Fair", 0xFFFF9800),                 // Orange
    GOOD(3, "Good", 0xFFFFEB3B),                 // Yellow
    EXCELLENT(4, "Excellent", 0xFF4CAF50);       // Green

    companion object {
        fun fromCode(code: Int): SignalQuality {
            return entries.find { it.code == code } ?: NO_SIGNAL
        }

        fun fromSNR(snrDb: Float): SignalQuality {
            return when {
                snrDb < 0f -> NO_SIGNAL
                snrDb < 5f -> POOR
                snrDb < 10f -> FAIR
                snrDb < 15f -> GOOD
                else -> EXCELLENT
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  RAW SIGNAL SAMPLE
// ════════════════════════════════════════════════════════════════

@Parcelize
data class RawSignal(
    val eegValue: Float = 0f,        // EEG raw ADC or filtered (µV)
    val eogValue: Float = 0f,        // EOG raw ADC or filtered (µV)
    val timestamp: Long = System.currentTimeMillis(),
    val sequenceNumber: Long = 0L    // For detecting dropped packets
) : Parcelable

// ════════════════════════════════════════════════════════════════
//  EEG SIGNAL (simplified)
// ════════════════════════════════════════════════════════════════

@Parcelize
data class EEGSignal(
    // Raw & Filtered Values
    val filteredValue: Float = 0f,    // Filtered waveform (µV)
    val rms: Float = 0f,              // RMS amplitude (µV)

    // Band Powers (optional, if you want spectral info)
    val alpha: Float = 0f,            // Alpha band power (µV²)
    val beta: Float = 0f,             // Beta band power (µV²)
    val theta: Float = 0f,            // Theta band power (µV²)
    val delta: Float = 0f,            // Delta band power (µV²)
    val gamma: Float = 0f,            // Gamma band power (µV²)

    // Indices (simplified)
    val focusIndex: Float = 0f,       // 0-1, based on Beta/(Alpha+Theta)
    val relaxationIndex: Float = 0f,  // 0-1, based on Alpha/Beta
    val drowsinessIndex: Float = 0f,  // 0-1

    // Signal Quality
    val snr: Float = 0f,              // Signal-to-noise ratio (dB)
    val quality: SignalQuality = SignalQuality.NO_SIGNAL,
    val isStable: Boolean = false,
    val hasArtifact: Boolean = false,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val qualityPercentage: Int
        get() = when (quality) {
            SignalQuality.NO_SIGNAL -> 0
            SignalQuality.POOR -> 20
            SignalQuality.FAIR -> 40
            SignalQuality.GOOD -> 70
            SignalQuality.EXCELLENT -> 100
        }

    companion object {
        val EMPTY = EEGSignal()
    }
}

// ════════════════════════════════════════════════════════════════
//  EOG SIGNAL (simplified)
// ════════════════════════════════════════════════════════════════

@Parcelize
data class EOGSignal(
    // Raw & Filtered Values
    val filteredValue: Float = 0f,    // Filtered waveform (µV)
    val amplitude: Float = 0f,        // Peak-to-peak amplitude (µV)
    val rms: Float = 0f,              // RMS amplitude (µV)

    // Blink Detection
    val isBlinkDetected: Boolean = false,
    val blinkStrength: Float = 0f,    // Blink amplitude (µV)

    // Saccade Detection
    val isSaccadeDetected: Boolean = false,
    val saccadeDirection: SaccadeDirection = SaccadeDirection.NONE,
    val saccadeVelocity: Float = 0f,  // degrees/second

    // Eye State
    val eyeOpenness: Float = 1f,      // 0=closed, 1=fully open
    val eyePosition: Float = 0f,      // -1=left, 0=center, 1=right

    // Signal Quality
    val snr: Float = 0f,              // Signal-to-noise ratio (dB)
    val quality: SignalQuality = SignalQuality.NO_SIGNAL,
    val isStable: Boolean = false,
    val hasArtifact: Boolean = false,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable {

    val qualityPercentage: Int
        get() = when (quality) {
            SignalQuality.NO_SIGNAL -> 0
            SignalQuality.POOR -> 20
            SignalQuality.FAIR -> 40
            SignalQuality.GOOD -> 70
            SignalQuality.EXCELLENT -> 100
        }

    val isLookingLeft: Boolean
        get() = saccadeDirection == SaccadeDirection.LEFT || eyePosition < -0.3f

    val isLookingRight: Boolean
        get() = saccadeDirection == SaccadeDirection.RIGHT || eyePosition > 0.3f

    val isLookingStraight: Boolean
        get() = eyePosition in -0.3f..0.3f

    companion object {
        val EMPTY = EOGSignal()
    }
}

// ════════════════════════════════════════════════════════════════
//  THIGH ACTIVITY SAMPLE
// ════════════════════════════════════════════════════════════════

@Parcelize
data class ThighActivitySample(
    val timestamp: Long = System.currentTimeMillis(),
    val eeg: EEGSignal = EEGSignal.EMPTY,
    val eog: EOGSignal = EOGSignal.EMPTY,
    val activityType: ActivityType = ActivityType.NONE,
    val intensity: Float = 0f,           // 0.0–1.0
    val isMoving: Boolean = false,
    val duration: Long = 0L              // ms since activity started
) : Parcelable {

    val overallQuality: Int
        get() = ((eeg.qualityPercentage + eog.qualityPercentage) / 2)

    val isGoodQuality: Boolean
        get() = overallQuality >= 70

    companion object {
        val EMPTY = ThighActivitySample()
    }
}

// ════════════════════════════════════════════════════════════════
//  SACCADE DIRECTION ENUM
// ════════════════════════════════════════════════════════════════

enum class SaccadeDirection(val code: Int, val displayName: String) {
    NONE(0, "None"),
    LEFT(1, "Left"),
    RIGHT(2, "Right"),
    UP(3, "Up"),
    DOWN(4, "Down");

    companion object {
        fun fromCode(code: Int): SaccadeDirection {
            return entries.find { it.code == code } ?: NONE
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  BLE DEVICE INFO
// ════════════════════════════════════════════════════════════════

@Parcelize
data class BleDeviceInfo(
    val name: String = "Unknown",
    val address: String = "",
    val rssi: Int = 0,
    val batteryPercent: Int = -1,
    val isConnected: Boolean = false,
    val lastSeenTime: Long = System.currentTimeMillis()
) : Parcelable

// ════════════════════════════════════════════════════════════════
//  SESSION STATISTICS (simplified)
// ════════════════════════════════════════════════════════════════

@Parcelize
data class SessionStatistics(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long = 0L,

    // Activity Statistics
    val totalActivityEvents: Int = 0,
    val totalActivityDuration: Long = 0L,        // milliseconds
    val avgIntensity: Float = 0f,
    val peakIntensity: Float = 0f,
    val activityDistribution: Map<ActivityType, Int> = emptyMap(),

    // Signal Quality
    val avgEegQuality: Float = 0f,
    val avgEogQuality: Float = 0f,
    val totalSamples: Long = 0L,

    // Eye Movement
    val totalBlinks: Int = 0,
    val totalSaccades: Int = 0,
    val avgBlinkRate: Float = 0f,                // blinks per minute

    // Device Info
    val deviceName: String = "",
    val avgBattery: Int = 50
) : Parcelable {

    val isOngoing: Boolean
        get() = endTime == 0L

    val durationMs: Long
        get() = if (endTime > 0) endTime - startTime else System.currentTimeMillis() - startTime

    val formattedDuration: String
        get() {
            val seconds = durationMs / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            return when {
                hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        }

    companion object {
        fun create(sessionId: String, deviceName: String = ""): SessionStatistics {
            return SessionStatistics(
                sessionId = sessionId,
                startTime = System.currentTimeMillis(),
                deviceName = deviceName
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  EVENT DATA CLASSES
// ════════════════════════════════════════════════════════════════

@Parcelize
data class ActivityEvent(
    val timestamp: Long,
    val type: ActivityType,
    val intensity: Float,
    val duration: Long = 0L
) : Parcelable

@Parcelize
data class EyeEvent(
    val timestamp: Long,
    val type: EyeEventType,
    val direction: SaccadeDirection = SaccadeDirection.NONE,
    val amplitude: Float = 0f
) : Parcelable

enum class EyeEventType {
    BLINK, SACCADE_START, SACCADE_END, FIXATION
}

@Parcelize
data class SignalQualityEvent(
    val timestamp: Long,
    val eegQuality: SignalQuality,
    val eogQuality: SignalQuality,
    val message: String = ""
) : Parcelable

// ════════════════════════════════════════════════════════════════
//  BLUETOOTH CONFIGURATION
// ════════════════════════════════════════════════════════════════

object BluetoothConfig {
    const val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    const val EEG_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    const val EOG_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a9"
    const val STATUS_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26ab"
    const val CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    // Sampling rates
    const val EEG_SAMPLE_RATE = 256    // Hz
    const val EOG_SAMPLE_RATE = 75     // Hz

    // ADC specs
    const val EOG_ADC_MAX = 4095f
    const val EOG_ADC_CENTER = 2048f
    const val EOG_ADC_BITS = 12
}*/