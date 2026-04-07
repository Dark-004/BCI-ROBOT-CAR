package com.neurodrive.smartcar.domain

import android.util.Log
import com.neurodrive.smartcar.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalProcessor @Inject constructor(
    private val signalFilter: SignalFilter
) {

    companion object {
        private const val TAG = "SignalProcessor"

        private const val HIGH_CONFIDENCE_THRESHOLD   = 0.8f
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.6f

        private const val FOCUS_THRESHOLD     = 0.6f
        private const val RELAXATION_THRESHOLD = 1.5f
        private const val DROWSINESS_THRESHOLD = 1.2f

        private const val BLINK_STRENGTH_THRESHOLD  = 80f
        private const val TRIPLE_BLINK_INTERVAL_MS  = 800L   // all 3 blinks must fall within this window

        private const val LOCK_DURATION_SECONDS = 5
    }

    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ════════════════════════════════════════════════════════════════
    //  REAL-TIME FILTERED SIGNALS FOR GRAPHING
    // ════════════════════════════════════════════════════════════════

    private val _eegFilteredSignal = MutableSharedFlow<Float>(replay = 0)
    val eegFilteredSignal: SharedFlow<Float> = _eegFilteredSignal.asSharedFlow()

    private val _eogFilteredSignal = MutableSharedFlow<Float>(replay = 0)
    val eogFilteredSignal: SharedFlow<Float> = _eogFilteredSignal.asSharedFlow()

    private val _alphaPower = MutableSharedFlow<Float>(replay = 0)
    val alphaPower: SharedFlow<Float> = _alphaPower.asSharedFlow()

    private val _betaPower = MutableSharedFlow<Float>(replay = 0)
    val betaPower: SharedFlow<Float> = _betaPower.asSharedFlow()

    private val _thetaPower = MutableSharedFlow<Float>(replay = 0)
    val thetaPower: SharedFlow<Float> = _thetaPower.asSharedFlow()

    private val _gammaPower = MutableSharedFlow<Float>(replay = 0)
    val gammaPower: SharedFlow<Float> = _gammaPower.asSharedFlow()

    // ════════════════════════════════════════════════════════════════
    //  LOCK STATE
    // ════════════════════════════════════════════════════════════════

    private val _lockStatus = MutableStateFlow(LockStatus.UNLOCKED)
    val lockStatus: StateFlow<LockStatus> = _lockStatus.asStateFlow()

    private var lockJob: Job? = null
    private var lockedCommandType: CommandType = CommandType.NONE
    private var lockStartTime: Long = 0L
    private var lockDurationMs: Long = LOCK_DURATION_SECONDS * 1000L

    // ════════════════════════════════════════════════════════════════
    //  STATE FLOWS
    // ════════════════════════════════════════════════════════════════

    private val _eegState     = MutableStateFlow(SignalFilter.FilteredEEG())
    val eegState: StateFlow<SignalFilter.FilteredEEG> = _eegState.asStateFlow()

    private val _eogState     = MutableStateFlow(SignalFilter.FilteredEOG())
    val eogState: StateFlow<SignalFilter.FilteredEOG> = _eogState.asStateFlow()

    private val _combinedState = MutableStateFlow(SignalFilter.FilteredSignals())
    val combinedState: StateFlow<SignalFilter.FilteredSignals> = _combinedState.asStateFlow()

    // ════════════════════════════════════════════════════════════════
    //  EVENT DETECTION STATE
    // ════════════════════════════════════════════════════════════════

    private var lastMentalState      = MentalState.UNKNOWN
    private var blinkTimes           = mutableListOf<Long>()
    private var lastEyeOpenness      = 1f
    private var eyesClosedStartTime  = 0L
    private var lastCommandTime      = 0L
    private var lastCommandType: CommandType? = null

    // ════════════════════════════════════════════════════════════════
    //  EVENT FLOWS
    // ════════════════════════════════════════════════════════════════

    private val _eegEvents = MutableSharedFlow<EEGEvent>(replay = 0)
    val eegEvents: SharedFlow<EEGEvent> = _eegEvents.asSharedFlow()

    private val _eogEvents = MutableSharedFlow<EOGEvent>(replay = 0)
    val eogEvents: SharedFlow<EOGEvent> = _eogEvents.asSharedFlow()

    // ════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════

    init {
        signalFilter.onFilteredEEG = { value ->
            processingScope.launch { _eegFilteredSignal.emit(value) }
        }
        signalFilter.onFilteredEOG = { value ->
            processingScope.launch { _eogFilteredSignal.emit(value) }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SAMPLE PROCESSING
    // ════════════════════════════════════════════════════════════════

    fun processEEGSample(rawSample: Float) {
        val result = signalFilter.processEEGSample(rawSample)
        _eegState.value = result
        if (result.isStable) {
            processingScope.launch {
                _alphaPower.emit(result.alpha)
                _betaPower.emit(result.beta)
                _thetaPower.emit(result.theta)
                _gammaPower.emit(result.gamma)
            }
        }
    }

    fun processEOGSample(rawSample: Float) {
        val result = signalFilter.processEOGSample(rawSample)
        _eogState.value = result
    }

    fun processBothSamples(eegSample: Float, eogSample: Float) {
        processEEGSample(eegSample)
        processEOGSample(eogSample)
        _combinedState.value = SignalFilter.FilteredSignals(
            eeg            = _eegState.value,
            eog            = _eogState.value,
            timestamp      = System.currentTimeMillis(),
            drowsinessLevel = calculateCombinedDrowsiness(),
            attentionLevel  = calculateCombinedAttention()
        )
    }

    fun updateFromBioSignal(signal: BioSignal) {
        val filteredEEG = SignalFilter.FilteredEEG(
            filteredValue   = signal.eeg.filteredValue,
            delta           = signal.eeg.delta,
            theta           = signal.eeg.theta,
            alpha           = signal.eeg.alpha,
            beta            = signal.eeg.beta,
            gamma           = signal.eeg.gamma,
            totalPower      = signal.eeg.alpha + signal.eeg.beta + signal.eeg.theta + signal.eeg.delta + signal.eeg.gamma,
            dominantBand    = "",
            alphaBetaRatio  = if (signal.eeg.beta > 0.0001f) signal.eeg.alpha / signal.eeg.beta else 0f,
            thetaAlphaRatio = if (signal.eeg.alpha > 0.0001f) signal.eeg.theta / signal.eeg.alpha else 0f,
            focusIndex      = signal.eeg.focusIndex,
            relaxationIndex = signal.eeg.relaxationIndex,
            drowsinessIndex = signal.eeg.drowsinessIndex,
            mentalState     = signal.eeg.mentalState,
            snr             = 0f,
            isStable        = signal.eeg.isStable,
            hasArtifact     = false
        )

        val filteredEOG = SignalFilter.FilteredEOG(
            filteredValue     = signal.eog.filteredValue,
            amplitude         = signal.eog.amplitude,
            isBlinkDetected   = signal.eog.isBlinkDetected,
            blinkStrength     = signal.eog.blinkStrength,
            isSaccadeDetected = signal.eog.isSaccadeDetected,
            saccadeDirection  = signal.eog.saccadeDirection,
            saccadeVelocity   = signal.eog.saccadeVelocity,
            eyePosition       = signal.eog.eyePosition,
            eyeOpenness       = signal.eog.eyeOpenness,
            isStable          = signal.eog.isStable,
            hasArtifact       = false,
            snr               = 0f
        )

        _eegState.value = filteredEEG
        _eogState.value = filteredEOG
        _combinedState.value = SignalFilter.FilteredSignals(
            eeg             = filteredEEG,
            eog             = filteredEOG,
            timestamp       = signal.timestamp,
            drowsinessLevel = signal.drowsinessLevel,
            attentionLevel  = signal.attentionLevel
        )

        processingScope.launch {
            _eegFilteredSignal.emit(filteredEEG.filteredValue)
            _eogFilteredSignal.emit(filteredEOG.filteredValue)
            if (filteredEEG.isStable) {
                _alphaPower.emit(filteredEEG.alpha)
                _betaPower.emit(filteredEEG.beta)
                _thetaPower.emit(filteredEEG.theta)
                _gammaPower.emit(filteredEEG.gamma)
            }
        }
    }

    private fun calculateCombinedDrowsiness(): Float {
        val eeg = _eegState.value; val eog = _eogState.value
        val eegDrowsiness = eeg.drowsinessIndex.coerceIn(0f, 2f) / 2f
        val eogDrowsiness = if (eog.eyeOpenness < 0.5f) 0.7f else 0f
        return (eegDrowsiness * 0.6f + eogDrowsiness * 0.4f).coerceIn(0f, 1f)
    }

    private fun calculateCombinedAttention(): Float = _eegState.value.focusIndex.coerceIn(0f, 1f)

    // ════════════════════════════════════════════════════════════════
    //  LOCK STATUS MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    fun getLockStatus(): LockStatus = _lockStatus.value
    fun isLocked(): Boolean = _lockStatus.value.isLocked
    fun getLockedCommand(): CommandType = lockedCommandType
    fun getRemainingLockTime(): Long = _lockStatus.value.remainingSeconds * 1000L
    fun getRemainingLockSeconds(): Int = _lockStatus.value.remainingSeconds

    private fun startLock(commandType: CommandType, commandName: String) {
        lockJob?.cancel()
        lockedCommandType = commandType
        lockStartTime     = System.currentTimeMillis()
        _lockStatus.value = LockStatus.locked(
            durationSeconds = LOCK_DURATION_SECONDS,
            command         = commandName,
            commandType     = commandType
        )
        Log.d(TAG, "🔒 Locked: $commandName for ${LOCK_DURATION_SECONDS}s")
        lockJob = processingScope.launch {
            for (remaining in LOCK_DURATION_SECONDS downTo 1) {
                _lockStatus.value = _lockStatus.value.copy(remainingSeconds = remaining)
                delay(1000L)
            }
            unlock()
        }
    }

    private fun unlock() {
        lockJob?.cancel(); lockJob = null
        lockedCommandType = CommandType.NONE; lockStartTime = 0L
        _lockStatus.value = LockStatus.UNLOCKED
        Log.d(TAG, "🔓 Unlocked")
    }

    fun forceUnlock() { unlock(); Log.d(TAG, "🔓 Force unlocked") }

    // ════════════════════════════════════════════════════════════════
    //  CLASSIFY INTENT
    //
    //  FIX 1: Stability and SNR checks are now the FIRST thing evaluated,
    //  before any classification work is done. Previously classifyEOGIntent()
    //  was called before the stability guard, computing a result that was
    //  immediately discarded.
    //
    //  FIX 2: Removed the large commented-out priority block. The active
    //  implementation below it is the real code path.
    //
    //  FIX 3: STRESSED no longer triggers EMERGENCY_STOP here at priority-1
    //  because classifyEEGIntent() already handles it at priority-3. The
    //  priority-1 safety block only handles sleep states and extreme drowsiness.
    // ════════════════════════════════════════════════════════════════

    fun classifyIntent(signal: BioSignal): Command? {
        val currentTime = System.currentTimeMillis()

        // ── Guard 1: Signal quality ──────────────────────────────────
        // FIX: These guards run before any classification work.
        if (!signal.isStable) return null
        if (signal.eeg.snr < 3f && signal.eog.snr < 3f) return null

        // ── Guard 2: Lock ────────────────────────────────────────────
        if (_lockStatus.value.isLocked) return null

        // ── Guard 3: Command cooldown (EOG turns bypass this) ────────
        val isEOGTurnAllowed = currentTime - lastCommandTime >= 1000L  // 1 s debounce for turns
        val isCooldownPassed = currentTime - lastCommandTime >= 5000L

        // ════════════════════════════════════════
        // Priority 1: SAFETY — sleep / extreme drowsiness
        // FIX: STRESSED removed from here — it's handled in classifyEEGIntent()
        //      to avoid issuing two EMERGENCY_STOPs per call.
        // ════════════════════════════════════════

        if (signal.eeg.mentalState == MentalState.DEEP_SLEEP  ||
            signal.eeg.mentalState == MentalState.LIGHT_SLEEP  ||
            signal.drowsinessLevel > 0.8f) {

            val cmd = Command(
                type        = CommandType.EMERGENCY_STOP,
                value       = 0f,
                confidence  = HIGH_CONFIDENCE_THRESHOLD,
                source      = CommandSource.EEG,
                description = "🚨 Safety: ${signal.eeg.mentalState.displayName}",
                mentalState = signal.mentalState,
                timestamp   = currentTime
            )
            lastCommandTime = currentTime
            startLock(cmd.type, cmd.type.displayName)
            return cmd
        }

        // ════════════════════════════════════════
        // Priority 2: EOG — steering (left / right)
        // ════════════════════════════════════════

        if (isEOGTurnAllowed) {
            val eogCommand = classifyEOGIntent(signal.eog, currentTime)
            if (eogCommand != null &&
                (eogCommand.type == CommandType.TURN_LEFT || eogCommand.type == CommandType.TURN_RIGHT)) {
                lastCommandTime = currentTime
                lastCommandType = eogCommand.type
                startLock(eogCommand.type, eogCommand.type.displayName)
                Log.d(TAG, "👁️ EOG: ${eogCommand.type} (${eogCommand.confidence})")
                return eogCommand
            }
        }

        // ════════════════════════════════════════
        // Priority 3: EEG — movement (forward / backward / stop / emergency)
        // ════════════════════════════════════════

        if (isCooldownPassed) {
            val eegCommand = classifyEEGIntent(signal.eeg, currentTime)
            if (eegCommand != null) {
                lastCommandTime = currentTime
                lastCommandType = eegCommand.type
                startLock(eegCommand.type, eegCommand.type.displayName)
                Log.d(TAG, "🧠 EEG: ${eegCommand.type} (${eegCommand.confidence})")
                return eegCommand
            }
        }

        return null
    }

    // ════════════════════════════════════════════════════════════════
    //  EOG INTENT CLASSIFICATION
    //
    //  FIX: Triple-blink purge window now matches TRIPLE_BLINK_INTERVAL_MS.
    //  Previously blinkTimes purged entries older than 3000ms, so blinks
    //  from up to 3 seconds ago could combine with new ones and falsely
    //  trigger EMERGENCY_STOP.
    // ════════════════════════════════════════════════════════════════

    private fun classifyEOGIntent(eog: EOGSignal, currentTime: Long): Command? {
        if (!eog.isStable || eog.snr < 8f) return null

        // ─── Triple blink → EMERGENCY STOP ───────────────────────────
        if (eog.isBlinkDetected && eog.blinkStrength > BLINK_STRENGTH_THRESHOLD) {
            blinkTimes.add(currentTime)

            // FIX: Purge window matches detection window (800ms).
            // Old code kept blinks for 3000ms, causing false positives.
            blinkTimes.removeAll { currentTime - it > TRIPLE_BLINK_INTERVAL_MS }

            if (blinkTimes.size >= 3) {
                val recentBlinks  = blinkTimes.takeLast(3)
                val totalInterval = recentBlinks.last() - recentBlinks.first()
                if (totalInterval < TRIPLE_BLINK_INTERVAL_MS) {
                    blinkTimes.clear()
                    return Command(
                        type        = CommandType.EMERGENCY_STOP,
                        value       = 0f,
                        confidence  = HIGH_CONFIDENCE_THRESHOLD,
                        source      = CommandSource.EOG,
                        description = "👁️ Triple blink → EMERGENCY STOP",
                        timestamp   = currentTime
                    )
                }
            }
        }

        // ─── Saccade → steering ───────────────────────────────────────
        if (eog.isSaccadeDetected) {
            if (eog.saccadeVelocity < 250f) return null

            val velocityFactor  = (eog.saccadeVelocity / 1000f).coerceIn(0.3f, 1f)
            val amplitudeFactor = (eog.amplitude / 200f).coerceIn(0.3f, 1f)
            val confidence      = (velocityFactor * 0.6f + amplitudeFactor * 0.4f).coerceIn(0.5f, 1f)

            if (confidence < 0.65f) return null

            return when (eog.saccadeDirection) {
                SaccadeDirection.LEFT -> Command(
                    type        = CommandType.TURN_LEFT,
                    value       = confidence,
                    confidence  = confidence,
                    source      = CommandSource.EOG,
                    description = "👁️← Look LEFT → Turn LEFT (${(confidence * 100).toInt()}%)",
                    timestamp   = currentTime
                )
                SaccadeDirection.RIGHT -> Command(
                    type        = CommandType.TURN_RIGHT,
                    value       = confidence,
                    confidence  = confidence,
                    source      = CommandSource.EOG,
                    description = "👁️→ Look RIGHT → Turn RIGHT (${(confidence * 100).toInt()}%)",
                    timestamp   = currentTime
                )
                else -> null
            }
        }

        return null
    }

    // ════════════════════════════════════════════════════════════════
    //  EEG INTENT CLASSIFICATION
    // ════════════════════════════════════════════════════════════════

    private fun classifyEEGIntent(eeg: EEGSignal, currentTime: Long): Command? {
        if (!eeg.isStable || eeg.snr < 3f) return null
        val totalPower = eeg.totalPower
        if (totalPower < 0.01f) return null

        val nAlpha = eeg.alpha / totalPower
        val nBeta  = eeg.beta  / totalPower
        val nTheta = eeg.theta / totalPower
        val nDelta = eeg.delta / totalPower

        // ─── Focus → FORWARD ─────────────────────────────────────────
        if (eeg.mentalState == MentalState.FOCUSED       ||
            eeg.mentalState == MentalState.CONCENTRATING ||
            eeg.mentalState == MentalState.ALERT) {

            val speed      = (eeg.focusIndex.coerceIn(0f, 2f) / 2f * 0.8f).coerceIn(0.2f, 1f)
            val confidence = when {
                nBeta > 0.5f  -> 0.95f
                nBeta > 0.4f  -> 0.85f
                nBeta > 0.35f -> 0.7f
                else          -> 0.5f
            }
            if (confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return Command(
                    type        = CommandType.FORWARD,
                    value       = speed,
                    confidence  = confidence,
                    source      = CommandSource.EEG,
                    description = "🧠⬆️ FOCUS → FORWARD (${(speed * 100).toInt()}%)",
                    mentalState = eeg.mentalState,
                    timestamp   = currentTime
                )
            }
        }

        // ─── Relax → BACKWARD ────────────────────────────────────────
        if (eeg.mentalState == MentalState.RELAXED    ||
            eeg.mentalState == MentalState.MEDITATIVE) {

            val speed      = (eeg.relaxationIndex.coerceIn(0f, 3f) / 3f * 0.5f).coerceIn(0.1f, 0.5f)
            val confidence = when {
                nAlpha > 0.45f -> 0.9f
                nAlpha > 0.35f -> 0.75f
                nAlpha > 0.25f -> 0.6f
                else           -> 0.4f
            }
            if (confidence >= MEDIUM_CONFIDENCE_THRESHOLD) {
                return Command(
                    type        = CommandType.BACKWARD,
                    value       = speed,
                    confidence  = confidence,
                    source      = CommandSource.EEG,
                    description = "🧠⬇️ RELAX → BACKWARD (${(speed * 100).toInt()}%)",
                    mentalState = eeg.mentalState,
                    timestamp   = currentTime
                )
            }
        }

        // ─── Neutral → STOP ──────────────────────────────────────────
        if (eeg.mentalState == MentalState.NEUTRAL ||
            eeg.mentalState == MentalState.UNKNOWN) {

            val maxNormalized = maxOf(nAlpha, nBeta, nTheta, nDelta)
            if (maxNormalized < 0.35f) {
                return Command(
                    type        = CommandType.STOP,
                    value       = 0f,
                    confidence  = 0.7f,
                    source      = CommandSource.EEG,
                    description = "🧠🛑 NEUTRAL → STOP",
                    mentalState = eeg.mentalState,
                    timestamp   = currentTime
                )
            }
        }

        // ─── Sleep / Stress → EMERGENCY STOP ─────────────────────────
        if (eeg.mentalState == MentalState.STRESSED   ||
            eeg.mentalState == MentalState.DEEP_SLEEP ||
            eeg.mentalState == MentalState.LIGHT_SLEEP) {

            return Command(
                type        = CommandType.EMERGENCY_STOP,
                value       = 0f,
                confidence  = HIGH_CONFIDENCE_THRESHOLD,
                source      = CommandSource.EEG,
                description = "🧠🚨 ${eeg.mentalState.displayName} → EMERGENCY STOP",
                mentalState = eeg.mentalState,
                timestamp   = currentTime
            )
        }

        return null
    }

    // ════════════════════════════════════════════════════════════════
    //  COMBINED INTENT CLASSIFICATION
    // ════════════════════════════════════════════════════════════════

    private fun classifyCombinedIntent(signal: BioSignal, currentTime: Long): Command? {
        if (!signal.isStable) return null

        if (signal.drowsinessLevel > 0.7f) {
            return Command(
                type        = CommandType.EMERGENCY_STOP,
                value       = 0f,
                confidence  = signal.drowsinessLevel,
                source      = CommandSource.COMBINED,
                description = "Critical drowsiness — ${signal.getDrowsinessDescription()}",
                mentalState = signal.mentalState,
                timestamp   = currentTime
            )
        }

        if (signal.attentionLevel > 0.7f && signal.drowsinessLevel < 0.3f) {
            return Command(
                type        = CommandType.MAINTAIN_SPEED,
                value       = signal.attentionLevel,
                confidence  = signal.attentionLevel,
                source      = CommandSource.COMBINED,
                description = "Good attention — maintain speed",
                mentalState = signal.mentalState,
                timestamp   = currentTime
            )
        }

        if (signal.attentionLevel < 0.3f) {
            return Command(
                type        = CommandType.ALERT_DRIVER,
                value       = 1f - signal.attentionLevel,
                confidence  = MEDIUM_CONFIDENCE_THRESHOLD,
                source      = CommandSource.COMBINED,
                description = "Low attention — ${signal.getAttentionDescription()}",
                mentalState = signal.mentalState,
                timestamp   = currentTime
            )
        }

        return null
    }

    // ════════════════════════════════════════════════════════════════
    //  CONTROL
    // ════════════════════════════════════════════════════════════════

    fun reset() {
        signalFilter.reset()
        lastMentalState     = MentalState.UNKNOWN
        blinkTimes.clear()
        lastEyeOpenness     = 1f
        eyesClosedStartTime = 0
        lastCommandTime     = 0
        lastCommandType     = null
        forceUnlock()
        _eegState.value      = SignalFilter.FilteredEEG()
        _eogState.value      = SignalFilter.FilteredEOG()
        _combinedState.value = SignalFilter.FilteredSignals()
        Log.d(TAG, "Processor reset complete")
    }

    fun cleanup() {
        lockJob?.cancel()
        processingScope.cancel()
    }
}/*
package com.neurodrive.smartcar.domain

import android.util.Log
import com.neurodrive.smartcar.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleSignalProcessor @Inject constructor(
    private val signalFilter: SignalFilter
) {

    companion object {
        private const val TAG = "SimpleSignalProcessor"

        // Thresholds for activity detection
        const val MOVEMENT_THRESHOLD = 100f  // mV amplitude
        const val RMS_THRESHOLD = 50f        // mV RMS
        const val HIGH_ACTIVITY_THRESHOLD = 0.7f
    }

    private val processingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ════════════════════════════════════════════════════════════════
    //  REAL-TIME SIGNAL FLOWS
    // ════════════════════════════════════════════════════════════════

    private val _eegFiltered = MutableSharedFlow<Float>(replay = 0)
    val eegFiltered: SharedFlow<Float> = _eegFiltered.asSharedFlow()

    private val _eogFiltered = MutableSharedFlow<Float>(replay = 0)
    val eogFiltered: SharedFlow<Float> = _eogFiltered.asSharedFlow()

    private val _activity = MutableStateFlow<ThighActivity?>(null)
    val activity: StateFlow<ThighActivity?> = _activity.asStateFlow()

    // ════════════════════════════════════════════════════════════════
    //  STATE
    // ════════════════════════════════════════════════════════════════

    private val _processingState = MutableStateFlow<ProcessingState>(ProcessingState.IDLE)
    val processingState: StateFlow<ProcessingState> = _processingState.asStateFlow()

    private var activityStartTime = 0L
    private var lastActivityTime = 0L
    private var activityCount = 0

    enum class ProcessingState { IDLE, ANALYZING, MOVING, REST }

    data class ThighActivity(
        val timestamp: Long,
        val eegRms: Float,
        val eogAmplitude: Float,
        val isMoving: Boolean,
        val intensity: Float,           // 0.0–1.0
        val activityType: ActivityType,
        val duration: Long              // milliseconds
    )

    enum class ActivityType {
        NONE, SUBTLE, MODERATE, INTENSE
    }

    // ════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════

    init {
        signalFilter.onFilteredEEG = { value ->
            processingScope.launch { _eegFiltered.emit(value) }
        }
        signalFilter.onFilteredEOG = { value ->
            processingScope.launch { _eogFiltered.emit(value) }
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  SAMPLE PROCESSING
    // ════════════════════════════════════════════════════════════════

    fun processEEGSample(rawValue: Float) {
        val filtered = signalFilter.processEEGSample(rawValue)
        processingScope.launch {
            _eegFiltered.emit(filtered.filteredValue)
            updateActivity(eegRms = filtered.rms)
        }
    }

    fun processEOGSample(rawValue: Float) {
        val filtered = signalFilter.processEOGSample(rawValue)
        processingScope.launch {
            _eogFiltered.emit(filtered.filteredValue)
            updateActivity(eogAmplitude = filtered.amplitude)
        }
    }

    fun processBothSamples(eegRaw: Float, eogRaw: Float) {
        val eeg = signalFilter.processEEGSample(eegRaw)
        val eog = signalFilter.processEOGSample(eogRaw)

        processingScope.launch {
            _eegFiltered.emit(eeg.filteredValue)
            _eogFiltered.emit(eog.filteredValue)
            updateActivity(eegRms = eeg.rms, eogAmplitude = eog.amplitude)
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  ACTIVITY DETECTION
    // ════════════════════════════════════════════════════════════════

    private var lastEegRms = 0f
    private var lastEogAmplitude = 0f

    private fun updateActivity(eegRms: Float? = null, eogAmplitude: Float? = null) {
        if (eegRms != null) lastEegRms = eegRms
        if (eogAmplitude != null) lastEogAmplitude = eogAmplitude

        val currentTime = System.currentTimeMillis()

        // ─── Detect movement ─────────────────────────────────────────
        val isMoving = lastEogAmplitude > MOVEMENT_THRESHOLD || lastEegRms > RMS_THRESHOLD

        // ─── Classify activity intensity ──────────────────────────────
        val intensity = when {
            lastEegRms > 150f || lastEogAmplitude > 300f -> 1.0f      // Intense
            lastEegRms > 100f || lastEogAmplitude > 200f -> 0.7f      // Moderate
            lastEegRms > 50f  || lastEogAmplitude > 100f -> 0.4f      // Subtle
            else -> 0.0f                                                // None
        }

        val activityType = when {
            intensity > 0.8f -> ActivityType.INTENSE
            intensity > 0.5f -> ActivityType.MODERATE
            intensity > 0.2f -> ActivityType.SUBTLE
            else             -> ActivityType.NONE
        }

        // ─── Track activity duration ─────────────────────────────────
        if (isMoving) {
            if (activityStartTime == 0L) {
                activityStartTime = currentTime
                activityCount++
                _processingState.value = ProcessingState.MOVING
            }
            lastActivityTime = currentTime
        } else {
            if (activityStartTime > 0L && currentTime - lastActivityTime > 500L) {
                activityStartTime = 0L
                _processingState.value = ProcessingState.IDLE
            }
        }

        val duration = if (activityStartTime > 0) currentTime - activityStartTime else 0L

        // ─── Emit activity ───────────────────────────────────────────
        _activity.value = ThighActivity(
            timestamp = currentTime,
            eegRms = lastEegRms,
            eogAmplitude = lastEogAmplitude,
            isMoving = isMoving,
            intensity = intensity,
            activityType = activityType,
            duration = duration
        )

        // ─── Logging ──────────────────────────────────────────────────
        if (isMoving) {
            Log.d(TAG, "📊 EEG: %.1f mV | EOG: %.1f mV | Intensity: %.1f%% | Duration: %d ms".format(
                lastEegRms, lastEogAmplitude, intensity * 100, duration
            ))
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  QUERY METHODS
    // ════════════════════════════════════════════════════════════════

    fun getCurrentActivity(): ThighActivity? = _activity.value

    fun isMoving(): Boolean = _activity.value?.isMoving ?: false

    fun getIntensity(): Float = _activity.value?.intensity ?: 0f

    fun getActivityDuration(): Long = _activity.value?.duration ?: 0L

    fun getActivityCount(): Int = activityCount

    fun getAverageIntensity(): Float {
        val current = _activity.value
        return current?.intensity ?: 0f
    }

    // ════════════════════════════════════════════════════════════════
    //  STATISTICS
    // ════════════════════════════════════════════════════════════════

    data class ActivityStats(
        val totalActivities: Int,
        val avgIntensity: Float,
        val totalDuration: Long,
        val peakIntensity: Float
    )

    private var peakIntensity = 0f

    fun getStats(): ActivityStats {
        val current = _activity.value
        if (current != null && current.intensity > peakIntensity) {
            peakIntensity = current.intensity
        }

        return ActivityStats(
            totalActivities = activityCount,
            avgIntensity = getAverageIntensity(),
            totalDuration = getActivityDuration(),
            peakIntensity = peakIntensity
        )
    }

    // ════════════════════════════════════════════════════════════════
    //  CONTROL
    // ════════════════════════════════════════════════════════════════

    fun reset() {
        signalFilter.reset()
        activityStartTime = 0L
        lastActivityTime = 0L
        activityCount = 0
        lastEegRms = 0f
        lastEogAmplitude = 0f
        peakIntensity = 0f
        _processingState.value = ProcessingState.IDLE
        _activity.value = null
        Log.d(TAG, "Processor reset complete")
    }

    fun cleanup() {
        processingScope.cancel()
    }
}*/