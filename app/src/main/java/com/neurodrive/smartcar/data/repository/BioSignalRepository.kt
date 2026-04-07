package com.neurodrive.smartcar.data.repository

import android.util.Log
import com.neurodrive.smartcar.data.model.*
import com.neurodrive.smartcar.domain.SignalProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ════════════════════════════════════════════════════════════════
//  ALERT EVENT
// ════════════════════════════════════════════════════════════════

data class AlertEvent(
    val id: Long = System.currentTimeMillis(),
    val type: AlertType,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long = System.currentTimeMillis(),
    val isDismissed: Boolean = false
)

enum class AlertType {
    // EEG Alerts
    DROWSINESS,
    HIGH_STRESS,
    MENTAL_FATIGUE,
    FOCUS_LOST,
    DEEP_SLEEP,

    // EOG Alerts
    EYES_CLOSED,
    EXCESSIVE_BLINKING,
    SLOW_BLINK_RATE,
    HIGH_PERCLOS,
    PROLONGED_FIXATION,

    // Signal Quality
    SIGNAL_LOST,
    LOW_SNR,
    ARTIFACT_DETECTED,
    EEG_UNSTABLE,
    EOG_UNSTABLE,

    // Connection
    CONNECTION_LOST,
    BLE_DISCONNECTED,
    ESP32_DISCONNECTED,

    // System
    ANOMALY,
    CALIBRATION_NEEDED
}

enum class AlertSeverity(val color: Long) {
    INFO(0xFF2196F3),      // Blue
    WARNING(0xFFFF9800),   // Orange
    CRITICAL(0xFFF44336)   // Red
}

// ════════════════════════════════════════════════════════════════
//  REPOSITORY
// ════════════════════════════════════════════════════════════════

@Singleton
class BioSignalRepository @Inject constructor(
    private val signalProcessor: SignalProcessor
) {

    companion object {
        private const val TAG = "BioSignalRepository"
        private const val MAX_HISTORY_SIZE = 1000
        private const val MAX_ALERTS = 200
        private const val MAX_COMMANDS = 100
        private const val ALERT_COOLDOWN_MS = 5000L // 5 seconds between same alert type
    }

    // ══════════════════════════════════════════════════════════════
    //  CONNECTION STATES
    // ══════════════════════════════════════════════════════════════

    private val _bleConnectionState = MutableStateFlow(false)
    val bleConnectionState: StateFlow<Boolean> = _bleConnectionState.asStateFlow()

    private val _esp32ConnectionState = MutableStateFlow(false)
    val esp32ConnectionState: StateFlow<Boolean> = _esp32ConnectionState.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  CURRENT BIO SIGNAL
    // ══════════════════════════════════════════════════════════════

    private val _currentSignal = MutableStateFlow<BioSignal?>(null)
    val currentSignal: StateFlow<BioSignal?> = _currentSignal.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  SIGNAL HISTORY
    // ══════════════════════════════════════════════════════════════

    private val _signalHistory = MutableStateFlow<List<BioSignal>>(emptyList())
    val signalHistory: StateFlow<List<BioSignal>> = _signalHistory.asStateFlow()

    // EEG-specific history
    private val _eegHistory = MutableStateFlow<List<EEGSignal>>(emptyList())
    val eegHistory: StateFlow<List<EEGSignal>> = _eegHistory.asStateFlow()

    // EOG-specific history
    private val _eogHistory = MutableStateFlow<List<EOGSignal>>(emptyList())
    val eogHistory: StateFlow<List<EOGSignal>> = _eogHistory.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  COMMANDS
    // ══════════════════════════════════════════════════════════════

    private val _currentCommand = MutableStateFlow<Command?>(null)
    val currentCommand: StateFlow<Command?> = _currentCommand.asStateFlow()

    private val _lastCommand = MutableStateFlow<Command?>(null)
    val lastCommand: StateFlow<Command?> = _lastCommand.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<Command>>(emptyList())
    val commandHistory: StateFlow<List<Command>> = _commandHistory.asStateFlow()

    private val _pendingCommand = MutableStateFlow<Command?>(null)
    val pendingCommand: StateFlow<Command?> = _pendingCommand.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  SENSOR DATA (from ESP32)
    // ══════════════════════════════════════════════════════════════

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  ALERTS
    // ══════════════════════════════════════════════════════════════

    private val _currentAlert = MutableStateFlow<AlertEvent?>(null)
    val currentAlert: StateFlow<AlertEvent?> = _currentAlert.asStateFlow()

    private val _alertEvents = MutableStateFlow<List<AlertEvent>>(emptyList())
    val alertEvents: StateFlow<List<AlertEvent>> = _alertEvents.asStateFlow()

    private val lastAlertTimes = mutableMapOf<AlertType, Long>()

    // ══════════════════════════════════════════════════════════════
    //  SIGNAL QUALITY
    // ══════════════════════════════════════════════════════════════

    private val _signalQuality = MutableStateFlow(SignalQualityLevel.NO_SIGNAL)
    val signalQuality: StateFlow<SignalQualityLevel> = _signalQuality.asStateFlow()

    private val _eegQuality = MutableStateFlow(SignalQualityLevel.NO_SIGNAL)
    val eegQuality: StateFlow<SignalQualityLevel> = _eegQuality.asStateFlow()

    private val _eogQuality = MutableStateFlow(SignalQualityLevel.NO_SIGNAL)
    val eogQuality: StateFlow<SignalQualityLevel> = _eogQuality.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  EVENTS
    // ══════════════════════════════════════════════════════════════

    private val _eegEvents = MutableStateFlow<List<EEGEvent>>(emptyList())
    val eegEvents: StateFlow<List<EEGEvent>> = _eegEvents.asStateFlow()

    private val _eogEvents = MutableStateFlow<List<EOGEvent>>(emptyList())
    val eogEvents: StateFlow<List<EOGEvent>> = _eogEvents.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  SESSION STATISTICS
    // ══════════════════════════════════════════════════════════════

    private var sessionStats: SessionStatistics? = null
    private val _currentSessionStats = MutableStateFlow<SessionStatistics?>(null)
    val currentSessionStats: StateFlow<SessionStatistics?> = _currentSessionStats.asStateFlow()

    // Running averages
    private var totalAlpha = 0f
    private var totalBeta = 0f
    private var totalTheta = 0f
    private var totalDelta = 0f
    private var totalFocusIndex = 0f
    private var totalDrowsinessIndex = 0f
    private var totalDrowsinessLevel = 0f
    private var totalAttentionLevel = 0f
    private var totalBlinks = 0
    private var totalSaccades = 0
    private var totalPerclos = 0f
    private var sampleCount = 0L

    // State distribution
    private val stateOccurrences = mutableMapOf<MentalState, Int>()

    // ══════════════════════════════════════════════════════════════
    //  DRIVER STATE
    // ══════════════════════════════════════════════════════════════

    private val _driverState = MutableStateFlow<DriverState?>(null)
    val driverState: StateFlow<DriverState?> = _driverState.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  PROCESS RAW SIGNALS - MAIN ENTRY POINT FOR RAW DATA
    // ══════════════════════════════════════════════════════════════

    /**
     * Process raw EEG and EOG samples
     * This is called from BLE when receiving raw sensor data
     */
    /**
     * Process raw EEG and EOG signals (single-channel)
     */
    fun processRawSignals(eegRaw: Float, eogRaw: Float): BioSignal {
        // Process through SignalProcessor
        signalProcessor.processBothSamples(eegRaw, eogRaw)

        // Get filtered results
        val eegFiltered = signalProcessor.eegState.value
        val eogFiltered = signalProcessor.eogState.value

        // Convert to BioSignal
        val bioSignal = BioSignal.fromFilteredSignals(
            eegFiltered = eegFiltered,
            eogFiltered = eogFiltered,
            samplesProcessed = sampleCount++
        )

        // Process the BioSignal
        processSignal(bioSignal)

        return bioSignal
    }

    // ══════════════════════════════════════════════════════════════
    //  PROCESS BIOSIGNAL - FOR PRE-PROCESSED SIGNALS
    // ══════════════════════════════════════════════════════════════

    /**
     * Process BioSignal (when signal is already processed)
     * This is used when receiving pre-processed signals from ESP32 or testing
     */
    fun processSignal(signal: BioSignal): Command? {
        try {
            // 1. Update SignalProcessor state if signal already has processed data
            signalProcessor.updateFromBioSignal(signal)

            // 2. Store current signal
            _currentSignal.value = signal

            // 3. Append to history
            _signalHistory.value = (_signalHistory.value + signal).takeLast(MAX_HISTORY_SIZE)
            _eegHistory.value = (_eegHistory.value + signal.eeg).takeLast(MAX_HISTORY_SIZE)
            _eogHistory.value = (_eogHistory.value + signal.eog).takeLast(MAX_HISTORY_SIZE)

            // 4. Evaluate signal quality
            _signalQuality.value = evaluateOverallQuality(signal)
            _eegQuality.value = evaluateEEGQuality(signal.eeg)
            _eogQuality.value = evaluateEOGQuality(signal.eog)

            // 5. Update session statistics
            updateSessionStatistics(signal)

            // 6. Update driver state
            updateDriverState(signal)

            // 7. Check for alert conditions
            checkAlertConditions(signal)

            // 8. Classify intent → Command
            val command: Command? = signalProcessor.classifyIntent(signal)
            if (command != null) {
                _currentCommand.value = command
                _lastCommand.value = command
                _commandHistory.value = (_commandHistory.value + command).takeLast(MAX_COMMANDS)

                Log.d(TAG, "Command classified: ${command.type} (confidence: ${command.confidence})")
            }

            return command

        } catch (e: Exception) {
            Log.e(TAG, "Error processing signal", e)
            return null
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROCESS EEG EVENT
    // ══════════════════════════════════════════════════════════════

    fun processEEGEvent(event: EEGEvent) {
        _eegEvents.value = (_eegEvents.value + event).takeLast(100)

        Log.d(TAG, "EEG Event: ${event.type} - ${event.state}")

        // Generate alerts for critical EEG events
        when (event.type) {
            EEGEventType.DROWSINESS_WARNING -> {
                pushAlert(AlertEvent(
                    type = AlertType.DROWSINESS,
                    title = "Drowsiness Warning",
                    message = "Drowsiness index: ${event.value.format(2)}",
                    severity = AlertSeverity.CRITICAL
                ))
            }
            EEGEventType.STATE_CHANGE -> {
                if (event.state == MentalState.DEEP_SLEEP || event.state == MentalState.LIGHT_SLEEP) {
                    pushAlert(AlertEvent(
                        type = AlertType.DEEP_SLEEP,
                        title = "Sleep State Detected",
                        message = "Mental state: ${event.state.displayName}",
                        severity = AlertSeverity.CRITICAL
                    ))
                }
            }
            EEGEventType.ARTIFACT_DETECTED -> {
                pushAlert(AlertEvent(
                    type = AlertType.ARTIFACT_DETECTED,
                    title = "Signal Artifact",
                    message = "EEG artifact detected, signal may be unreliable",
                    severity = AlertSeverity.WARNING
                ))
            }
            else -> {} // Other events don't generate alerts
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PROCESS EOG EVENT
    // ══════════════════════════════════════════════════════════════

    fun processEOGEvent(event: EOGEvent) {
        _eogEvents.value = (_eogEvents.value + event).takeLast(100)

        Log.d(TAG, "EOG Event: ${event.type} - ${event.direction}")

        // Generate alerts for critical EOG events
        when (event.type) {
            EOGEventType.DROWSINESS_WARNING -> {
                pushAlert(AlertEvent(
                    type = AlertType.HIGH_PERCLOS,
                    title = "Drowsiness Warning",
                    message = "Prolonged eye closure detected",
                    severity = AlertSeverity.CRITICAL
                ))
            }
            EOGEventType.LONG_BLINK -> {
                pushAlert(AlertEvent(
                    type = AlertType.EYES_CLOSED,
                    title = "Long Blink Detected",
                    message = "Duration: ${event.duration}ms",
                    severity = AlertSeverity.WARNING
                ))
            }
            EOGEventType.EYES_CLOSED -> {
                pushAlert(AlertEvent(
                    type = AlertType.EYES_CLOSED,
                    title = "Eyes Closed",
                    message = "Eyes have been closed",
                    severity = AlertSeverity.CRITICAL
                ))
            }
            else -> {} // Other events don't generate alerts
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MANUAL COMMAND
    // ══════════════════════════════════════════════════════════════

    fun sendManualCommand(command: Command) {
        _currentCommand.value = command
        _lastCommand.value = command
        _commandHistory.value = (_commandHistory.value + command).takeLast(MAX_COMMANDS)
        _pendingCommand.value = command

        Log.d(TAG, "Manual command: ${command.type}")
    }

    fun clearPendingCommand() {
        _pendingCommand.value = null
    }

    // ══════════════════════════════════════════════════════════════
    //  CONNECTION STATE UPDATES
    // ══════════════════════════════════════════════════════════════

    fun updateBleConnectionState(connected: Boolean) {
        val wasConnected = _bleConnectionState.value
        _bleConnectionState.value = connected

        if (wasConnected && !connected) {
            pushAlert(AlertEvent(
                type = AlertType.BLE_DISCONNECTED,
                title = "BLE Headband Disconnected",
                message = "Lost connection to EEG/EOG headband. Reconnect to resume.",
                severity = AlertSeverity.CRITICAL
            ))
        } else if (!wasConnected && connected) {
            Log.d(TAG, "BLE headband connected")
        }
    }

    fun updateEsp32ConnectionState(connected: Boolean) {
        val wasConnected = _esp32ConnectionState.value
        _esp32ConnectionState.value = connected

        if (wasConnected && !connected) {
            pushAlert(AlertEvent(
                type = AlertType.ESP32_DISCONNECTED,
                title = "ESP32 Disconnected",
                message = "Lost connection to car controller.",
                severity = AlertSeverity.CRITICAL
            ))
        } else if (!wasConnected && connected) {
            Log.d(TAG, "ESP32 connected")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SENSOR DATA UPDATE
    // ══════════════════════════════════════════════════════════════

    fun updateSensorData(data: SensorData) {
        _sensorData.value = data
    }

    // ══════════════════════════════════════════════════════════════
    //  SESSION MANAGEMENT
    // ══════════════════════════════════════════════════════════════

    fun startSession() {
        val sessionId = "session_${System.currentTimeMillis()}"
        sessionStats = SessionStatistics.create(sessionId)
        _currentSessionStats.value = sessionStats

        // Reset running totals
        totalAlpha = 0f
        totalBeta = 0f
        totalTheta = 0f
        totalDelta = 0f
        totalFocusIndex = 0f
        totalDrowsinessIndex = 0f
        totalDrowsinessLevel = 0f
        totalAttentionLevel = 0f
        totalBlinks = 0
        totalSaccades = 0
        totalPerclos = 0f
        sampleCount = 0L
        stateOccurrences.clear()

        Log.d(TAG, "Session started: $sessionId")
    }

    fun endSession() {
        sessionStats?.let { stats ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - stats.startTime

            // Calculate averages
            val avgAlpha = if (sampleCount > 0) totalAlpha / sampleCount else 0f
            val avgBeta = if (sampleCount > 0) totalBeta / sampleCount else 0f
            val avgTheta = if (sampleCount > 0) totalTheta / sampleCount else 0f
            val avgFocusIndex = if (sampleCount > 0) totalFocusIndex / sampleCount else 0f
            val avgDrowsinessIndex = if (sampleCount > 0) totalDrowsinessIndex / sampleCount else 0f
            val avgDrowsinessLevel = if (sampleCount > 0) totalDrowsinessLevel / sampleCount else 0f
            val avgAttentionLevel = if (sampleCount > 0) totalAttentionLevel / sampleCount else 0f
            val avgPerclos = if (sampleCount > 0) totalPerclos / sampleCount else 0f
            val avgBlinkRate = if (duration > 0) totalBlinks * 60000f / duration else 0f

            // Calculate state distribution
            val totalStateOccurrences = stateOccurrences.values.sum()
            val stateDistribution = stateOccurrences.mapValues {
                if (totalStateOccurrences > 0) it.value.toFloat() / totalStateOccurrences else 0f
            }

            // Find dominant state
            val dominantState = stateOccurrences.maxByOrNull { it.value }?.key ?: MentalState.UNKNOWN

            // Count alerts
            val drowsinessWarnings = _alertEvents.value.count {
                it.type == AlertType.DROWSINESS || it.type == AlertType.HIGH_PERCLOS
            }
            val criticalAlerts = _alertEvents.value.count { it.severity == AlertSeverity.CRITICAL }

            // Calculate max drowsiness
            val maxDrowsinessLevel = _signalHistory.value.maxOfOrNull { it.drowsinessLevel } ?: 0f

            // Calculate signal quality
            val avgSignalQuality = _signalHistory.value
                .map { it.overallQualityPercentage / 100f }
                .average()
                .toFloat()

            val signalLossCount = _signalHistory.value.count { !it.isStable }
            val signalLossPercentage = if (sampleCount > 0)
                signalLossCount.toFloat() / sampleCount else 0f

            val updatedStats = stats.copy(
                endTime = endTime,
                durationMs = duration,
                avgAlpha = avgAlpha,
                avgBeta = avgBeta,
                avgTheta = avgTheta,
                avgFocusIndex = avgFocusIndex,
                avgDrowsinessIndex = avgDrowsinessIndex,
                dominantState = dominantState,
                stateDistribution = stateDistribution,
                totalBlinks = totalBlinks,
                avgBlinkRate = avgBlinkRate,
                totalSaccades = totalSaccades,
                avgPerclos = avgPerclos,
                drowsinessWarnings = drowsinessWarnings,
                criticalAlerts = criticalAlerts,
                avgDrowsinessLevel = avgDrowsinessLevel,
                avgAttentionLevel = avgAttentionLevel,
                maxDrowsinessLevel = maxDrowsinessLevel,
                avgSignalQuality = avgSignalQuality,
                signalLossPercentage = signalLossPercentage
            )

            _currentSessionStats.value = updatedStats
            sessionStats = null

            Log.d(TAG, "Session ended: ${updatedStats.sessionId}, Duration: ${updatedStats.formattedDuration}")
        }
    }

    private fun updateSessionStatistics(signal: BioSignal) {
        if (sessionStats == null) return

        sampleCount++

        totalAlpha += signal.eegAlpha
        totalBeta += signal.eegBeta
        totalTheta += signal.eegTheta
        totalDelta += signal.eegDelta
        totalFocusIndex += signal.focusIndex
        totalDrowsinessIndex += signal.eeg.drowsinessIndex
        totalDrowsinessLevel += signal.drowsinessLevel
        totalAttentionLevel += signal.attentionLevel
        totalPerclos += signal.perclos

        if (signal.isBlinkDetected) totalBlinks++
        if (signal.isSaccadeDetected) totalSaccades++

        // Track state occurrences
        val state = signal.mentalState
        stateOccurrences[state] = (stateOccurrences[state] ?: 0) + 1

        // Update running session stats
        val currentTime = System.currentTimeMillis()
        sessionStats?.let { stats ->
            _currentSessionStats.value = stats.copy(
                durationMs = currentTime - stats.startTime
            )
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DRIVER STATE UPDATE
    // ══════════════════════════════════════════════════════════════

    private fun updateDriverState(signal: BioSignal) {
        val recommendations = signal.getRecommendations()

        val state = DriverState(
            drowsinessLevel = signal.drowsinessLevel,
            attentionLevel = signal.attentionLevel,
            stressLevel = signal.stressLevel,
            fatigueLevel = signal.fatigueLevel,
            alertLevel = signal.alertLevel,
            recommendations = recommendations
        )

        _driverState.value = state
    }

    // ══════════════════════════════════════════════════════════════
    //  ALERT DETECTION
    // ══════════════════════════════════════════════════════════════

    private fun checkAlertConditions(signal: BioSignal) {
        val now = System.currentTimeMillis()

        // ──────────────────────────────────
        //  EEG-based Alerts
        // ──────────────────────────────────

        // Critical drowsiness (high theta + low beta)
        if (signal.eegTheta > 12f && signal.eegBeta < 4f) {
            pushAlertWithCooldown(
                AlertType.DROWSINESS,
                AlertEvent(
                    type = AlertType.DROWSINESS,
                    title = "Critical Drowsiness",
                    message = "High theta (${signal.eegTheta.format(1)}) with low beta (${signal.eegBeta.format(1)}). PULL OVER!",
                    severity = AlertSeverity.CRITICAL
                ),
                now
            )
        }

        // High stress (high beta + high gamma)
        if (signal.eegBeta > 18f && signal.eegGamma > 10f) {
            pushAlertWithCooldown(
                AlertType.HIGH_STRESS,
                AlertEvent(
                    type = AlertType.HIGH_STRESS,
                    title = "High Stress Level",
                    message = "Beta: ${signal.eegBeta.format(1)}, Gamma: ${signal.eegGamma.format(1)} — Take deep breaths.",
                    severity = AlertSeverity.WARNING
                ),
                now
            )
        }

        // Mental fatigue
        if (signal.fatigueLevel > 0.7f) {
            pushAlertWithCooldown(
                AlertType.MENTAL_FATIGUE,
                AlertEvent(
                    type = AlertType.MENTAL_FATIGUE,
                    title = "Mental Fatigue Detected",
                    message = "Fatigue level: ${(signal.fatigueLevel * 100).toInt()}%",
                    severity = AlertSeverity.WARNING
                ),
                now
            )
        }

        // Focus lost
        if (signal.attentionLevel < 0.3f && signal.eeg.isStable) {
            pushAlertWithCooldown(
                AlertType.FOCUS_LOST,
                AlertEvent(
                    type = AlertType.FOCUS_LOST,
                    title = "Low Attention",
                    message = "Attention level: ${(signal.attentionLevel * 100).toInt()}% — Stay focused!",
                    severity = AlertSeverity.WARNING
                ),
                now
            )
        }

        // ──────────────────────────────────
        //  EOG-based Alerts
        // ──────────────────────────────────

        // High PERCLOS
        if (signal.perclos > 0.15f) {
            pushAlertWithCooldown(
                AlertType.HIGH_PERCLOS,
                AlertEvent(
                    type = AlertType.HIGH_PERCLOS,
                    title = "Eyes Closing Frequently",
                    message = "PERCLOS: ${(signal.perclos * 100).toInt()}% — Take a break!",
                    severity = AlertSeverity.CRITICAL
                ),
                now
            )
        }

        // Excessive blinking
        if (signal.blinkRate > 30f) {
            pushAlertWithCooldown(
                AlertType.EXCESSIVE_BLINKING,
                AlertEvent(
                    type = AlertType.EXCESSIVE_BLINKING,
                    title = "Excessive Blinking",
                    message = "Blink rate: ${signal.blinkRate.format(1)}/min — Check eye fatigue.",
                    severity = AlertSeverity.WARNING
                ),
                now
            )
        }

        // Slow blink rate (eye strain or drowsiness)
        if (signal.blinkRate < 8f && signal.blinkRate > 0f) {
            pushAlertWithCooldown(
                AlertType.SLOW_BLINK_RATE,
                AlertEvent(
                    type = AlertType.SLOW_BLINK_RATE,
                    title = "Low Blink Rate",
                    message = "Blink rate: ${signal.blinkRate.format(1)}/min — Blink more to avoid eye strain.",
                    severity = AlertSeverity.INFO
                ),
                now
            )
        }

        // ──────────────────────────────────
        //  Signal Quality Alerts
        // ──────────────────────────────────

        // EEG low SNR
        if (signal.eeg.snr < 3f && signal.eeg.snr > 0f) {
            pushAlertWithCooldown(
                AlertType.LOW_SNR,
                AlertEvent(
                    type = AlertType.LOW_SNR,
                    title = "Low EEG Signal Quality",
                    message = "SNR: ${signal.eeg.snr.format(1)} dB — Adjust headband position.",
                    severity = AlertSeverity.WARNING
                ),
                now
            )
        }

        // EEG unstable
        if (!signal.eeg.isStable) {
            pushAlertWithCooldown(
                AlertType.EEG_UNSTABLE,
                AlertEvent(
                    type = AlertType.EEG_UNSTABLE,
                    title = "Unstable EEG Signal",
                    message = "Check electrode contact on forehead.",
                    severity = AlertSeverity.INFO
                ),
                now
            )
        }

        // EOG unstable
        if (!signal.eog.isStable) {
            pushAlertWithCooldown(
                AlertType.EOG_UNSTABLE,
                AlertEvent(
                    type = AlertType.EOG_UNSTABLE,
                    title = "Unstable EOG Signal",
                    message = "Check electrode contact near eyes.",
                    severity = AlertSeverity.INFO
                ),
                now
            )
        }

        // Overall signal lost
        if (!signal.isStable) {
            pushAlertWithCooldown(
                AlertType.SIGNAL_LOST,
                AlertEvent(
                    type = AlertType.SIGNAL_LOST,
                    title = "Signal Lost",
                    message = "Both EEG and EOG signals are unstable.",
                    severity = AlertSeverity.WARNING
                ),
                now
            )
        }

        // ──────────────────────────────────
        //  Combined Alerts
        // ──────────────────────────────────

        // Critical combined drowsiness
        if (signal.drowsinessLevel > 0.8f) {
            pushAlertWithCooldown(
                AlertType.DROWSINESS,
                AlertEvent(
                    type = AlertType.DROWSINESS,
                    title = "CRITICAL DROWSINESS",
                    message = "${signal.getDrowsinessDescription()} — STOP DRIVING NOW!",
                    severity = AlertSeverity.CRITICAL
                ),
                now
            )
        }
    }

    private fun pushAlertWithCooldown(type: AlertType, alert: AlertEvent, currentTime: Long) {
        val lastTime = lastAlertTimes[type] ?: 0L

        if (currentTime - lastTime >= ALERT_COOLDOWN_MS) {
            pushAlert(alert)
            lastAlertTimes[type] = currentTime
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  ALERT MANAGEMENT
    // ══════════════════════════════════════════════════════════════

    private fun pushAlert(alert: AlertEvent) {
        _currentAlert.value = alert
        _alertEvents.value = (_alertEvents.value + alert).takeLast(MAX_ALERTS)

        Log.d(TAG, "Alert: [${alert.severity}] ${alert.title} - ${alert.message}")
    }

    fun pushConnectionAlert(title: String, message: String) {
        pushAlert(
            AlertEvent(
                type = AlertType.CONNECTION_LOST,
                title = title,
                message = message,
                severity = AlertSeverity.CRITICAL
            )
        )
    }

    fun dismissCurrentAlert() {
        _currentAlert.value = null
    }

    fun dismissAlert(alertId: Long) {
        _alertEvents.value = _alertEvents.value.map {
            if (it.id == alertId) it.copy(isDismissed = true) else it
        }

        if (_currentAlert.value?.id == alertId) {
            _currentAlert.value = null
        }
    }

    fun clearAlerts() {
        _alertEvents.value = emptyList()
        _currentAlert.value = null
        lastAlertTimes.clear()
    }

    // ══════════════════════════════════════════════════════════════
    //  SIGNAL QUALITY EVALUATION
    // ══════════════════════════════════════════════════════════════

    private fun evaluateOverallQuality(signal: BioSignal): SignalQualityLevel {
        return signal.signalStatus.toQualityLevel()
    }

    private fun evaluateEEGQuality(eeg: EEGSignal): SignalQualityLevel {
        if (!eeg.isStable) return SignalQualityLevel.POOR

        return when {
            eeg.qualityPercentage >= 80 -> SignalQualityLevel.EXCELLENT
            eeg.qualityPercentage >= 60 -> SignalQualityLevel.GOOD
            eeg.qualityPercentage >= 40 -> SignalQualityLevel.FAIR
            eeg.qualityPercentage > 0 -> SignalQualityLevel.POOR
            else -> SignalQualityLevel.NO_SIGNAL
        }
    }

    private fun evaluateEOGQuality(eog: EOGSignal): SignalQualityLevel {
        if (!eog.isStable) return SignalQualityLevel.POOR

        return when {
            eog.qualityPercentage >= 80 -> SignalQualityLevel.EXCELLENT
            eog.qualityPercentage >= 60 -> SignalQualityLevel.GOOD
            eog.qualityPercentage >= 40 -> SignalQualityLevel.FAIR
            eog.qualityPercentage > 0 -> SignalQualityLevel.POOR
            else -> SignalQualityLevel.NO_SIGNAL
        }
    }

    private fun SignalStatus.toQualityLevel(): SignalQualityLevel {
        return when (this) {
            SignalStatus.NO_SIGNAL -> SignalQualityLevel.NO_SIGNAL
            SignalStatus.POOR -> SignalQualityLevel.POOR
            SignalStatus.FAIR -> SignalQualityLevel.FAIR
            SignalStatus.GOOD -> SignalQualityLevel.GOOD
            SignalStatus.EXCELLENT -> SignalQualityLevel.EXCELLENT
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DATA CLEARING
    // ══════════════════════════════════════════════════════════════

    fun clearHistory() {
        _signalHistory.value = emptyList()
        _eegHistory.value = emptyList()
        _eogHistory.value = emptyList()
        _commandHistory.value = emptyList()
        _eegEvents.value = emptyList()
        _eogEvents.value = emptyList()
        _currentSignal.value = null
        _currentCommand.value = null
        _lastCommand.value = null

        Log.d(TAG, "History cleared")
    }

    fun resetAll() {
        clearHistory()
        clearAlerts()
        endSession()

        _bleConnectionState.value = false
        _esp32ConnectionState.value = false
        _sensorData.value = null
        _driverState.value = null
        _signalQuality.value = SignalQualityLevel.NO_SIGNAL
        _eegQuality.value = SignalQualityLevel.NO_SIGNAL
        _eogQuality.value = SignalQualityLevel.NO_SIGNAL

        Log.d(TAG, "Repository reset")
    }

    // ══════════════════════════════════════════════════════════════
    //  UTILITIES
    // ══════════════════════════════════════════════════════════════

    fun getStatisticsSummary(): String {
        val stats = _currentSessionStats.value ?: return "No active session"

        return buildString {
            appendLine("Session: ${stats.sessionId}")
            appendLine("Duration: ${stats.formattedDuration}")
            appendLine("Samples: $sampleCount")
            appendLine("Avg Alpha: ${stats.avgAlpha.format(2)} µV²")
            appendLine("Avg Beta: ${stats.avgBeta.format(2)} µV²")
            appendLine("Avg Focus: ${stats.avgFocusIndex.format(2)}")
            appendLine("Avg Drowsiness: ${stats.avgDrowsinessLevel.format(2)}")
            appendLine("Total Blinks: ${stats.totalBlinks}")
            appendLine("Blink Rate: ${stats.avgBlinkRate.format(1)}/min")
            appendLine("Dominant State: ${stats.dominantState.displayName}")
            appendLine("Warnings: ${stats.drowsinessWarnings}")
            appendLine("Critical Alerts: ${stats.criticalAlerts}")
        }
    }

    private fun Float.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }

    // ══════════════════════════════════════════════════════════════
    //  ENUMS
    // ══════════════════════════════════════════════════════════════

    enum class SignalQualityLevel(val displayName: String, val color: Long) {
        NO_SIGNAL("No Signal", 0xFF9E9E9E),
        POOR("Poor", 0xFFF44336),
        FAIR("Fair", 0xFFFF9800),
        GOOD("Good", 0xFFFFEB3B),
        EXCELLENT("Excellent", 0xFF4CAF50)
    }
}

// ══════════════════════════════════════════════════════════════
//  DRIVER STATE (moved from SignalProcessor)
// ══════════════════════════════════════════════════════════════

data class DriverState(
    val drowsinessLevel: Float,      // 0-1
    val attentionLevel: Float,       // 0-1
    val stressLevel: Float,          // 0-1
    val fatigueLevel: Float,         // 0-1
    val alertLevel: AlertLevel,
    val recommendations: List<String>
)