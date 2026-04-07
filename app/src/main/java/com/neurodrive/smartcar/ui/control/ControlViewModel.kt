package com.neurodrive.smartcar.ui.control

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.model.Command
import com.neurodrive.smartcar.data.model.CommandType
import com.neurodrive.smartcar.data.model.CommandSource
import com.neurodrive.smartcar.data.model.SensorData
import com.neurodrive.smartcar.data.repository.BioSignalRepository
import com.neurodrive.smartcar.domain.LockStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val repository: BioSignalRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ControlViewModel"
        private const val LOCK_DURATION_SECONDS = 5
    }

    // ════════════════════════════════════════════════════════════════
    //  UI STATE
    // ════════════════════════════════════════════════════════════════

    private val _bioControlEnabled = MutableStateFlow(false)
    val bioControlEnabled: StateFlow<Boolean> = _bioControlEnabled.asStateFlow()

    private val _lastCommand = MutableStateFlow<Command?>(null)
    val lastCommand: StateFlow<Command?> = _lastCommand.asStateFlow()

    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence.asStateFlow()

    private val _currentSpeed = MutableStateFlow(50)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    // ════════════════════════════════════════════════════════════════
    //  CONNECTION STATE
    // ════════════════════════════════════════════════════════════════

    val bleConnected: StateFlow<Boolean> = repository.bleConnectionState

    val esp32Connected: StateFlow<Boolean> = repository.esp32ConnectionState

    // ════════════════════════════════════════════════════════════════
    //  SENSOR DATA
    // ════════════════════════════════════════════════════════════════

    val sensorData: StateFlow<SensorData?> = repository.sensorData

    // ════════════════════════════════════════════════════════════════
    //  LOCK STATUS
    // ════════════════════════════════════════════════════════════════

    private val _lockStatus = MutableStateFlow(LockStatus.UNLOCKED)
    val lockStatus: StateFlow<LockStatus> = _lockStatus.asStateFlow()

    private var lockJob: Job? = null

    // ════════════════════════════════════════════════════════════════
    //  INITIALIZATION
    // ════════════════════════════════════════════════════════════════

    init {
        observeCommands()
    }

    private fun observeCommands() {
        viewModelScope.launch {
            repository.currentCommand.collect { command ->
                command?.let {
                    if (_bioControlEnabled.value && !_lockStatus.value.isLocked) {
                        executeCommand(it)
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  BIO CONTROL TOGGLE
    // ════════════════════════════════════════════════════════════════

    fun toggleBioControl(enabled: Boolean) {
        _bioControlEnabled.value = enabled
        Log.d(TAG, "Bio control ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
            // Unlock when switching to manual mode
            forceUnlock()
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  MANUAL COMMAND
    // ════════════════════════════════════════════════════════════════

    fun sendManualCommand(type: CommandType) {
        val command = Command(
            type = type,
            value = _currentSpeed.value / 100f,
            confidence = 1f,
            source = CommandSource.MANUAL,
            description = "Manual: ${type.displayName}"
        )

        _lastCommand.value = command
        _confidence.value = 1f

        repository.sendManualCommand(command)

        Log.d(TAG, "Manual command sent: ${type.displayName}")
    }

    // ════════════════════════════════════════════════════════════════
    //  COMMAND EXECUTION WITH LOCK
    // ════════════════════════════════════════════════════════════════

    private fun executeCommand(command: Command) {
        // Skip if already locked
        if (_lockStatus.value.isLocked) {
            Log.d(TAG, "Command ignored - locked: ${command.type}")
            return
        }

        // Update state
        _lastCommand.value = command
        _confidence.value = command.confidence

        // Lock for LOCK_DURATION_SECONDS
        startLock(command.type.displayName)

        // Send to repository for transmission
        repository.sendManualCommand(command)

        Log.d(TAG, "Command executed: ${command.type} (locked for ${LOCK_DURATION_SECONDS}s)")
    }

    private fun startLock(commandName: String) {
        // Cancel any existing lock job
        lockJob?.cancel()

        // Set initial lock status
        _lockStatus.value = LockStatus.locked(
            durationSeconds = LOCK_DURATION_SECONDS,
            command = commandName
        )

        // Start countdown
        lockJob = viewModelScope.launch {
            for (remaining in LOCK_DURATION_SECONDS downTo 1) {
                _lockStatus.value = _lockStatus.value.copy(
                    remainingSeconds = remaining
                )
                delay(1000L)
            }

            // Unlock
            _lockStatus.value = LockStatus.UNLOCKED
            Log.d(TAG, "Lock released")
        }
    }

    fun forceUnlock() {
        lockJob?.cancel()
        _lockStatus.value = LockStatus.UNLOCKED
        Log.d(TAG, "Force unlocked")
    }

    // ════════════════════════════════════════════════════════════════
    //  SPEED CONTROL
    // ════════════════════════════════════════════════════════════════

    fun setSpeed(speed: Int) {
        _currentSpeed.value = speed.coerceIn(0, 100)
    }

    // ════════════════════════════════════════════════════════════════
    //  EMERGENCY STOP
    // ════════════════════════════════════════════════════════════════

    fun emergencyStop() {
        // Force unlock
        forceUnlock()

        // Send emergency stop command
        val command = Command.emergencyStop(
            source = CommandSource.MANUAL,
            reason = "User triggered emergency stop"
        )

        _lastCommand.value = command
        _confidence.value = 1f

        repository.sendManualCommand(command)

        Log.d(TAG, "EMERGENCY STOP executed")
    }

    // ════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        lockJob?.cancel()
    }
}/*
package com.neurodrive.smartcar.ui.control

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.model.*
import com.neurodrive.smartcar.data.repository.BioSignalRepository
import com.neurodrive.smartcar.data.repository.BioSignalRepository.*
import com.neurodrive.smartcar.domain.LockStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val repository: BioSignalRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ControlViewModel"
        private const val LOCK_DURATION_SECONDS = 5
    }

    // ══════════════════════════════════════════════════════════════
    //  COMMAND CONTROL (KEPT)
    // ══════════════════════════════════════════════════════════════

    private val _bioControlEnabled = MutableStateFlow(false)
    val bioControlEnabled: StateFlow<Boolean> = _bioControlEnabled.asStateFlow()

    private val _lastCommand = MutableStateFlow<Command?>(null)
    val lastCommand: StateFlow<Command?> = _lastCommand.asStateFlow()

    private val _confidence = MutableStateFlow(0f)
    val confidence: StateFlow<Float> = _confidence.asStateFlow()

    private val _currentSpeed = MutableStateFlow(50)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  LOCK STATUS (KEPT)
    // ══════════════════════════════════════════════════════════════

    private val _lockStatus = MutableStateFlow(LockStatus.UNLOCKED)
    val lockStatus: StateFlow<LockStatus> = _lockStatus.asStateFlow()

    private var lockJob: Job? = null

    // ══════════════════════════════════════════════════════════════
    //  CONNECTION STATE
    // ══════════════════════════════════════════════════════════════

    val bleConnected: StateFlow<Boolean> = repository.bleConnectionState
    val deviceConnected: StateFlow<Boolean> = repository.deviceConnectionState

    // ══════════════════════════════════════════════════════════════
    //  DEVICE INFO
    // ══════════════════════════════════════════════════════════════

    val deviceInfo: StateFlow<BleDeviceInfo?> = repository.deviceInfo
    val batteryPercent: StateFlow<Int> = repository.batteryPercent

    // ══════════════════════════════════════════════════════════════
    //  CURRENT SIGNALS
    // ══════════════════════════════════════════════════════════════

    val rawEegValue: StateFlow<Float> = repository.currentRawSignal
        .mapNotNull { it?.eegValue }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val rawEogValue: StateFlow<Float> = repository.currentRawSignal
        .mapNotNull { it?.eogValue }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0f)

    val currentEegSignal: StateFlow<EEGSignal?> = repository.currentEegSignal
    val currentEogSignal: StateFlow<EOGSignal?> = repository.currentEogSignal
    val currentActivity: StateFlow<ThighActivitySample?> = repository.currentActivity

    // ══════════════════════════════════════════════════════════════
    //  SIGNAL QUALITY
    // ══════════════════════════════════════════════════════════════

    val eegQuality: StateFlow<SignalQuality> = repository.eegQuality
    val eogQuality: StateFlow<SignalQuality> = repository.eogQuality
    val overallQuality: StateFlow<SignalQuality> = repository.overallQuality

    // ══════════════════════════════════════════════════════════════
    //  ACTIVITY TRACKING (NEW)
    // ══════════════════════════════════════════════════════════════

    val activityHistory: StateFlow<List<ThighActivitySample>> = repository.activityHistory
    val activityEvents: StateFlow<List<ActivityEvent>> = repository.activityEvents
    val eyeEvents: StateFlow<List<EyeEvent>> = repository.eyeEvents

    // ══════════════════════════════════════════════════════════════
    //  ALERTS
    // ══════════════════════════════════════════════════════════════

   // val currentAlert: StateFlow<AlertEvent?> = repository.currentAlert
  //  val alertHistory: StateFlow<List<AlertEvent>> = repository.alertEvents

    // ══════════════════════════════════════════════════════════════
    //  SESSION STATISTICS
    // ══════════════════════════════════════════════════════════════

    val sessionStats: StateFlow<SessionStatistics?> = repository.currentSessionStats

    // ══════════════════════════════════════════════════════════════
    //  UI STATE (NEW)
    // ══════════════════════════════════════════════════════════════

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingStatus = MutableStateFlow("Ready")
    val recordingStatus: StateFlow<String> = _recordingStatus.asStateFlow()

    private val _activityIntensity = MutableStateFlow(0f)
    val activityIntensity: StateFlow<Float> = _activityIntensity.asStateFlow()

    private val _blinkRate = MutableStateFlow(0f)
    val blinkRate: StateFlow<Float> = _blinkRate.asStateFlow()

    private val _saccadeCount = MutableStateFlow(0)
    val saccadeCount: StateFlow<Int> = _saccadeCount.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  INITIALIZATION
    // ══════════════════════════════════════════════════════════════

    init {
        observeCommands()
        observeActivity()
        observeEyeMovement()
        observeAlerts()
    }

    // ══════════════════════════════════════════════════════════════
    //  COMMAND OBSERVATION (KEPT)
    // ══════════════════════════════════════════════════════════════

    private fun observeCommands() {
        viewModelScope.launch {
            repository.currentCommand.collect { command ->
                command?.let {
                    if (_bioControlEnabled.value && !_lockStatus.value.isLocked) {
                        executeCommand(it)
                    }
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  BIO CONTROL TOGGLE (KEPT)
    // ══════════════════════════════════════════════════════════════

    fun toggleBioControl(enabled: Boolean) {
        _bioControlEnabled.value = enabled
        Log.d(TAG, "Bio control ${if (enabled) "enabled" else "disabled"}")

        if (!enabled) {
            forceUnlock()
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MANUAL COMMAND (KEPT)
    // ══════════════════════════════════════════════════════════════

    fun sendManualCommand(type: CommandType) {
        val command = Command(
            type = type,
            value = _currentSpeed.value / 100f,
            confidence = 1f,
            source = CommandSource.MANUAL,
            description = "Manual: ${type.displayName}"
        )

        _lastCommand.value = command
        _confidence.value = 1f

        repository.sendManualCommand(command)

        Log.d(TAG, "Manual command sent: ${type.displayName}")
    }

    // ══════════════════════════════════════════════════════════════
    //  COMMAND EXECUTION WITH LOCK (KEPT)
    // ══════════════════════════════════════════════════════════════

    private fun executeCommand(command: Command) {
        // Skip if already locked
        if (_lockStatus.value.isLocked) {
            Log.d(TAG, "Command ignored - locked: ${command.type}")
            return
        }

        // Update state
        _lastCommand.value = command
        _confidence.value = command.confidence

        // Lock for LOCK_DURATION_SECONDS
        startLock(command.type.displayName)

        // Send to repository for transmission
        repository.sendManualCommand(command)

        Log.d(TAG, "Command executed: ${command.type} (locked for ${LOCK_DURATION_SECONDS}s)")
    }

    private fun startLock(commandName: String) {
        // Cancel any existing lock job
        lockJob?.cancel()

        // Set initial lock status
        _lockStatus.value = LockStatus.locked(
            durationSeconds = LOCK_DURATION_SECONDS,
            command = commandName
        )

        // Start countdown
        lockJob = viewModelScope.launch {
            for (remaining in LOCK_DURATION_SECONDS downTo 1) {
                _lockStatus.value = _lockStatus.value.copy(
                    remainingSeconds = remaining
                )
                delay(1000L)
            }

            // Unlock
            _lockStatus.value = LockStatus.UNLOCKED
            Log.d(TAG, "Lock released")
        }
    }

    fun forceUnlock() {
        lockJob?.cancel()
        _lockStatus.value = LockStatus.UNLOCKED
        Log.d(TAG, "Force unlocked")
    }

    // ══════════════════════════════════════════════════════════════
    //  SPEED CONTROL (KEPT)
    // ══════════════════════════════════════════════════════════════

    fun setSpeed(speed: Int) {
        _currentSpeed.value = speed.coerceIn(0, 100)
    }

    // ══════════════════════════════════════════════════════════════
    //  EMERGENCY STOP (KEPT)
    // ══════════════════════════════════════════════════════════════

    fun emergencyStop() {
        // Force unlock
        forceUnlock()

        // Send emergency stop command
        val command = Command.emergencyStop(
            source = CommandSource.MANUAL,
            reason = "User triggered emergency stop"
        )

        _lastCommand.value = command
        _confidence.value = 1f

        repository.sendManualCommand(command)

        Log.d(TAG, "EMERGENCY STOP executed")
    }

    // ══════════════════════════════════════════════════════════════
    //  ACTIVITY OBSERVATION (NEW)
    // ══════════════════════════════════════════════════════════════

    private fun observeActivity() {
        viewModelScope.launch {
            currentActivity.collect { activity ->
                activity?.let {
                    _activityIntensity.value = it.intensity
                    Log.v(TAG, "📊 Activity: ${it.activityType.displayName} (${(it.intensity * 100).toInt()}%)")
                }
            }
        }
    }

    private fun observeEyeMovement() {
        viewModelScope.launch {
            eyeEvents.collect { events ->
                val recentEvents = events.takeLast(50)

                // Count blinks in recent events
                val blinkCount = recentEvents.count { it.type == EyeEventType.BLINK }
                _blinkRate.value = (blinkCount / 5f) * 60f  // Approximate per minute

                // Count saccades
                val saccadeCount = recentEvents.count { it.type == EyeEventType.SACCADE_START }
                _saccadeCount.value = saccadeCount

                Log.v(TAG, "👁️ Blinks: $blinkCount, Saccades: $saccadeCount")
            }
        }
    }

    private fun observeAlerts() {
        viewModelScope.launch {
            currentAlert.collect { alert ->
                alert?.let {
                    _recordingStatus.value = "${it.title}: ${it.message}"
                    Log.w(TAG, "🚨 Alert: ${it.title}")
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SESSION MANAGEMENT (NEW)
    // ══════════════════════════════════════════════════════════════

    fun startSession() {
        if (_isRecording.value) {
            Log.w(TAG, "⚠️ Session already recording")
            return
        }

        _isRecording.value = true
        _recordingStatus.value = "Recording..."

        repository.startSession(deviceInfo.value?.name ?: "Unknown Device")

        Log.d(TAG, "🟢 Session started")
    }

    fun stopSession() {
        if (!_isRecording.value) {
            Log.w(TAG, "⚠️ No session active")
            return
        }

        _isRecording.value = false
        _recordingStatus.value = "Session ended"

        repository.endSession()

        Log.d(TAG, "🔴 Session stopped")
    }

    fun pauseSession() {
        _recordingStatus.value = "Paused"
        Log.d(TAG, "⏸️ Session paused")
    }

    fun resumeSession() {
        _recordingStatus.value = "Recording..."
        Log.d(TAG, "▶️ Session resumed")
    }

    // ══════════════════════════════════════════════════════════════
    //  ALERT MANAGEMENT (NEW)
    // ══════════════════════════════════════════════════════════════

    fun dismissCurrentAlert() {
        repository.dismissCurrentAlert()
        _recordingStatus.value = "Alert dismissed"
        Log.d(TAG, "Alert dismissed")
    }

    fun dismissAlert(alertId: Long) {
        repository.dismissAlert(alertId)
    }

    fun clearAllAlerts() {
        repository.clearAlerts()
        _recordingStatus.value = "Alerts cleared"
        Log.d(TAG, "All alerts cleared")
    }

    // ══════════════════════════════════════════════════════════════
    //  DATA MANAGEMENT (NEW)
    // ══════════════════════════════════════════════════════════════

    fun clearHistory() {
        repository.clearHistory()
        _recordingStatus.value = "History cleared"
        Log.d(TAG, "History cleared")
    }

    fun resetAll() {
        stopSession()
        clearHistory()
        clearAllAlerts()
        repository.resetAll()
        forceUnlock()
        _recordingStatus.value = "System reset"
        _activityIntensity.value = 0f
        _blinkRate.value = 0f
        _saccadeCount.value = 0
        _currentSpeed.value = 50
        _bioControlEnabled.value = false
        Log.d(TAG, "System reset complete")
    }

    // ══════════════════════════════════════════════════════════════
    //  STATISTICS (NEW)
    // ══════════════════════════════════════════════════════════════

    fun getSessionSummary(): String {
        return sessionStats.value?.let { stats ->
            """
            📊 SESSION SUMMARY
            Duration: ${stats.formattedDuration}
            Activity Events: ${stats.totalActivityEvents}
            Avg Intensity: ${(stats.avgIntensity * 100).toInt()}%
            Peak Intensity: ${(stats.peakIntensity * 100).toInt()}%
            ━━━━━━━━━━━━━━━━
            Total Blinks: ${stats.totalBlinks}
            Total Saccades: ${stats.totalSaccades}
            ━━━━━━━━━━━━━━━━
            Signal Quality:
              EEG: ${(stats.avgEegQuality * 100).toInt()}%
              EOG: ${(stats.avgEogQuality * 100).toInt()}%
            Battery: ${stats.avgBattery}%
            """.trimIndent()
        } ?: "No active session"
    }

    fun getRecentActivitySummary(): String {
        val recent = activityHistory.value.takeLast(10)
        if (recent.isEmpty()) return "No recent activity"

        val totalDuration = recent.sumOf { it.duration }
        val avgIntensity = recent.map { it.intensity }.average()

        return """
        📈 RECENT ACTIVITY (Last 10)
        Total Duration: ${totalDuration / 1000}s
        Avg Intensity: ${(avgIntensity * 100).toInt()}%
        Events: ${recent.size}
        """.trimIndent()
    }

    // ══════════════════════════════════════════════════════════════
    //  QUALITY CHECKS (NEW)
    // ══════════════════════════════════════════════════════════════

    fun isSignalGood(): Boolean {
        return eegQuality.value == SignalQuality.GOOD ||
                eegQuality.value == SignalQuality.EXCELLENT
    }

    fun isConnected(): Boolean {
        return bleConnected.value && deviceConnected.value
    }

    fun canStartSession(): Boolean {
        return isConnected() && !isRecording.value && isSignalGood()
    }

    // ══════════════════════════════════════════════════════════════
    //  STATUS MESSAGING (NEW)
    // ══════════════════════════════════════════════════════════════

    fun getStatusMessage(): String {
        return when {
            !bleConnected.value -> "❌ BLE Disconnected"
            !deviceConnected.value -> "❌ Device Disconnected"
            !isSignalGood() -> "⚠️ Signal Quality Low"
            isRecording.value -> "🟢 Recording Active"
            else -> "✅ Ready"
        }
    }

    fun getSignalQualityMessage(): String {
        val eeg = eegQuality.value
        val eog = eogQuality.value
        return "EEG: ${eeg.displayName} | EOG: ${eog.displayName}"
    }

    // ══════════════════════════════════════════════════════════════
    //  CLEANUP
    // ══════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        lockJob?.cancel()
        if (_isRecording.value) {
            stopSession()
        }
        Log.d(TAG, "ViewModel cleared")
    }
}*/