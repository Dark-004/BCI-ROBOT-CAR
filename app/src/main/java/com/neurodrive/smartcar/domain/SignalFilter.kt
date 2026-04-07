package com.neurodrive.smartcar.domain

import android.util.Log
import com.neurodrive.smartcar.data.model.MentalState
import com.neurodrive.smartcar.data.model.SaccadeDirection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class SignalFilter @Inject constructor() {

    // ════════════════════════════════════════
    //  CONSTANTS
    // ════════════════════════════════════════

    companion object {
        private const val TAG = "SignalFilter"

        // EEG Configuration
        const val EEG_SAMPLE_RATE = 256.0
        const val EEG_LOW_FREQ    = 0.5
        const val EEG_HIGH_FREQ   = 29.5

        // EOG Configuration
        const val EOG_SAMPLE_RATE = 75.0
        const val EOG_LOW_FREQ    = 0.16   // Better DC drift suppression
        const val EOG_HIGH_FREQ   = 15.0   // Better EMG suppression

        // Polarity correction — prefer hardware fix (swap electrode leads)
        const val EOG_INVERT_POLARITY = false

        // Notch filter
        const val NOTCH_FREQ = 50.0
        const val NOTCH_Q    = 30.0

        // Buffer sizes
        const val EEG_BUFFER_SIZE = 256
        const val EOG_BUFFER_SIZE = 75
        const val FFT_SIZE        = 256
        const val OVERLAP         = 128

        // EEG frequency bands
        const val DELTA_LOW  = 0.5;  const val DELTA_HIGH  = 4.0
        const val THETA_LOW  = 4.0;  const val THETA_HIGH  = 8.0
        const val ALPHA_LOW  = 8.0;  const val ALPHA_HIGH  = 13.0
        const val BETA_LOW   = 13.0; const val BETA_HIGH   = 30.0
        const val GAMMA_LOW  = 30.0; const val GAMMA_HIGH  = 100.0

        // Detection thresholds
        const val EEG_ARTIFACT_THRESHOLD          = 0.0f//150.0f
        const val EOG_ARTIFACT_THRESHOLD          = 1600//1000.0f
        const val EOG_BLINK_THRESHOLD             = 400.0f
        const val EOG_SACCADE_VELOCITY_THRESHOLD  = 200.0f
        const val EOG_SACCADE_MIN_AMPLITUDE       = 50.0f

        // Smoothing
        const val MOVING_AVG_WINDOW = 5

        // NOTE: calculateWelchPSD() uses a DFT loop — O(n²) at n=256.
        // This is intentionally kept simple for readability but will be
        // slow on a real device. Replace with an FFT library (e.g.
        // Apache Commons Math FastFourierTransformer) for production use.
        // See: implementation 'org.apache.commons:commons-math3:3.6.1'
    }

    // ════════════════════════════════════════
    //  SIGNAL TYPE ENUM
    // ════════════════════════════════════════

    enum class SignalType { EEG, EOG }

    // ════════════════════════════════════════
    //  BIQUAD FILTER SECTION
    // ════════════════════════════════════════

    data class BiquadSection(
        val b0: Float, val b1: Float, val b2: Float,
        val a1: Float, val a2: Float,
        var z1: Float = 0f, var z2: Float = 0f
    ) {
        fun process(input: Float): Float {
            val x = input - a1 * z1 - a2 * z2
            val output = b0 * x + b1 * z1 + b2 * z2
            z2 = z1; z1 = x
            return output
        }
        fun reset() { z1 = 0f; z2 = 0f }
    }

    // ════════════════════════════════════════
    //  EEG FILTER SECTIONS (0.5–29.5 Hz bandpass)
    // ════════════════════════════════════════

    private val eegFilterSections = listOf(
        BiquadSection(b0=0.00735282f, b1=0.01470564f, b2=0.00735282f, a1=-0.95391350f, a2=0.25311356f),
        BiquadSection(b0=1.00000000f, b1=2.00000000f, b2=1.00000000f, a1=-1.20596630f, a2=0.60558332f),
        BiquadSection(b0=1.00000000f, b1=-2.00000000f, b2=1.00000000f, a1=-1.97690645f, a2=0.97706395f),
        BiquadSection(b0=1.00000000f, b1=-2.00000000f, b2=1.00000000f, a1=-1.99071687f, a2=0.99086813f)
    )

    // ════════════════════════════════════════
    //  EOG FILTER SECTIONS (0.16–15 Hz bandpass)
    // ════════════════════════════════════════

    private val eogFilterSections = listOf(
        // Low-pass 15 Hz, stage 1
        BiquadSection(b0=0.1017f,  b1=0.2034f,  b2=0.1017f,  a1=-0.7524f, a2=0.1592f),
        // Low-pass 15 Hz, stage 2
        BiquadSection(b0=1.0f,     b1=2.0f,     b2=1.0f,     a1=-0.8012f, a2=0.4231f),
        // High-pass 0.16 Hz, stage 1
        BiquadSection(b0=1.0f,     b1=-2.0f,    b2=1.0f,     a1=-1.9867f, a2=0.9868f),
        // High-pass 0.16 Hz, stage 2
        BiquadSection(b0=1.0f,     b1=-2.0f,    b2=1.0f,     a1=-1.9911f, a2=0.9912f)
    )

    // ════════════════════════════════════════
    //  NOTCH FILTER (50 Hz)
    // ════════════════════════════════════════

    private val notchB: DoubleArray
    private val notchA: DoubleArray
    private var eegNotchZi = DoubleArray(2) { 0.0 }
    private var eogNotchZi = DoubleArray(2) { 0.0 }

    // ════════════════════════════════════════
    //  CIRCULAR BUFFERS
    //
    //  FIX: Removed eogBufferV — this is a single-channel horizontal
    //  EOG setup. The vertical buffer was declared but never written to,
    //  causing confusion. All vertical references are removed.
    // ════════════════════════════════════════

    private val eegBuffer = FloatArray(EEG_BUFFER_SIZE)
    private var eegBufferIndex = 0
    private var eegSamplesCollected = 0

    private val eogBuffer = FloatArray(EOG_BUFFER_SIZE)   // horizontal only
    private var eogBufferIndex = 0
    private var eogSamplesCollected = 0

    // Previous EOG values for velocity calculation
    private var prevEOGH    = 0f
    private var prevEOGTime = 0L

    // ════════════════════════════════════════
    //  FEATURE HISTORY (for smoothing)
    // ════════════════════════════════════════

    private val deltaPowerHistory = FloatArray(MOVING_AVG_WINDOW)
    private val thetaPowerHistory = FloatArray(MOVING_AVG_WINDOW)
    private val alphaPowerHistory = FloatArray(MOVING_AVG_WINDOW)
    private val betaPowerHistory  = FloatArray(MOVING_AVG_WINDOW)
    private val gammaPowerHistory = FloatArray(MOVING_AVG_WINDOW)
    private var eegHistoryIndex   = 0

    private val eogAmplitudeHistory = ArrayDeque<Float>(MOVING_AVG_WINDOW)

    // Real-time emission callbacks
    var onFilteredEEG: ((Float) -> Unit)? = null
    var onFilteredEOG: ((Float) -> Unit)? = null

    private fun emitFilteredEEG(value: Float) { onFilteredEEG?.invoke(value) }
    private fun emitFilteredEOG(value: Float) { onFilteredEOG?.invoke(value) }

    // ════════════════════════════════════════
    //  HANN WINDOW
    // ════════════════════════════════════════

    private val hannWindow = FloatArray(FFT_SIZE) { i ->
        (0.5 * (1 - cos(2.0 * PI * i / (FFT_SIZE - 1)))).toFloat()
    }

    // ════════════════════════════════════════
    //  INITIALIZATION
    // ════════════════════════════════════════

    init {
        val notchCoeffs = designIIRNotch(NOTCH_FREQ, NOTCH_Q, EEG_SAMPLE_RATE)
        notchB = notchCoeffs.first
        notchA = notchCoeffs.second

        // FIX: Log message now reflects actual filter parameters (was showing
        // stale "0.5 - 19.5 Hz" values after the EOG filter was updated).
        Log.d(TAG, "════════════════════════════════════════")
        Log.d(TAG, "  EEG/EOG Signal Filter Initialized")
        Log.d(TAG, "  EEG: $EEG_LOW_FREQ – $EEG_HIGH_FREQ Hz @ ${EEG_SAMPLE_RATE.toInt()} Hz")
        Log.d(TAG, "  EOG: $EOG_LOW_FREQ – $EOG_HIGH_FREQ Hz @ ${EOG_SAMPLE_RATE.toInt()} Hz (1-channel horizontal)")
        Log.d(TAG, "  Notch: ${NOTCH_FREQ.toInt()} Hz, Q=$NOTCH_Q")
        Log.d(TAG, "  Blink threshold:   $EOG_BLINK_THRESHOLD")
        Log.d(TAG, "  Saccade velocity:  $EOG_SACCADE_VELOCITY_THRESHOLD deg/s")
        Log.d(TAG, "════════════════════════════════════════")
    }

    // ════════════════════════════════════════
    //  OUTPUT DATA CLASSES
    // ════════════════════════════════════════

    data class FilteredEEG(
        val filteredValue    : Float      = 0f,
        val delta            : Float      = 0f,
        val theta            : Float      = 0f,
        val alpha            : Float      = 0f,
        val beta             : Float      = 0f,
        val gamma            : Float      = 0f,
        val totalPower       : Float      = 0f,
        val dominantBand     : String     = "",
        val alphaBetaRatio   : Float      = 0f,
        val thetaAlphaRatio  : Float      = 0f,
        val focusIndex       : Float      = 0f,
        val relaxationIndex  : Float      = 0f,
        val drowsinessIndex  : Float      = 0f,
        val mentalState      : MentalState = MentalState.UNKNOWN,
        val snr              : Float      = 0f,
        val isStable         : Boolean    = false,
        val hasArtifact      : Boolean    = false
    )

    data class FilteredEOG(
        val filteredValue     : Float           = 0f,
        val amplitude         : Float           = 0f,
        val isBlinkDetected   : Boolean         = false,
        val blinkStrength     : Float           = 0f,
        val isSaccadeDetected : Boolean         = false,
        val saccadeDirection  : SaccadeDirection = SaccadeDirection.NONE,
        val saccadeVelocity   : Float           = 0f,
        val eyePosition       : Float           = 0f,
        val eyeOpenness       : Float           = 1f,
        val isStable          : Boolean         = false,
        val hasArtifact       : Boolean         = false,
        val snr               : Float           = 0f
    )

    data class FilteredSignals(
        val eeg            : FilteredEEG = FilteredEEG(),
        val eog            : FilteredEOG = FilteredEOG(),
        val timestamp      : Long        = System.currentTimeMillis(),
        val drowsinessLevel: Float       = 0f,
        val attentionLevel : Float       = 0f
    )

    data class EEGFeatures(
        val E_delta          : Float      = 0f,
        val E_theta          : Float      = 0f,
        val E_alpha          : Float      = 0f,
        val E_beta           : Float      = 0f,
        val E_gamma          : Float      = 0f,
        val alphaBetaRatio   : Float      = 0f,
        val thetaAlphaRatio  : Float      = 0f,
        val thetaBetaRatio   : Float      = 0f,
        val deltaAlphaRatio  : Float      = 0f,
        val peakFrequency    : Float      = 0f,
        val spectralCentroid : Float      = 0f,
        val spectralEntropy  : Float      = 0f,
        val spectralSlope    : Float      = 0f,
        val focusIndex       : Float      = 0f,
        val relaxationIndex  : Float      = 0f,
        val drowsinessIndex  : Float      = 0f,
        val engagementIndex  : Float      = 0f,
        val predictedState   : MentalState = MentalState.UNKNOWN,
        val confidence       : Float      = 0f
    )

    data class EOGFeatures(
        val amplitude            : Float   = 0f,
        val peakToPeak           : Float   = 0f,
        val zeroCrossings        : Int     = 0,
        val blinkRate            : Float   = 0f,
        val blinkDuration        : Float   = 0f,
        val saccadeCount         : Int     = 0,
        val saccadeAmplitude     : Float   = 0f,
        val meanEyePosition      : Float   = 0f,
        val eyeMovementVariance  : Float   = 0f,
        val fixationCount        : Int     = 0,
        val fixationDuration     : Float   = 0f,
        val isDrowsy             : Boolean = false,
        val perclos              : Float   = 0f,
        val confidence           : Float   = 0f
    )

    // ════════════════════════════════════════
    //  FILTER DESIGN
    // ════════════════════════════════════════

    private fun designIIRNotch(freq: Double, q: Double, fs: Double): Pair<DoubleArray, DoubleArray> {
        val w0    = 2.0 * PI * freq / fs
        val alpha = sin(w0) / (2.0 * q)
        val a0    = 1.0 + alpha
        val b = doubleArrayOf(1.0 / a0, -2.0 * cos(w0) / a0, 1.0 / a0)
        val a = doubleArrayOf(1.0, -2.0 * cos(w0) / a0, (1.0 - alpha) / a0)
        return Pair(b, a)
    }

    private fun applyBiquadCascade(input: Float, sections: List<BiquadSection>): Float {
        var output = input
        sections.forEach { output = it.process(output) }
        return output
    }

    private fun lfilter(b: DoubleArray, a: DoubleArray, x: Double, zi: DoubleArray): Pair<Double, DoubleArray> {
        val n     = zi.size
        val newZi = zi.copyOf()
        val y     = b[0] * x + newZi[0]
        for (i in 0 until n - 1) {
            newZi[i] = b.getOrElse(i + 1) { 0.0 } * x - a.getOrElse(i + 1) { 0.0 } * y + newZi.getOrElse(i + 1) { 0.0 }
        }
        if (n > 0) {
            newZi[n - 1] = b.getOrElse(n) { 0.0 } * x - a.getOrElse(n) { 0.0 } * y
        }
        return Pair(y, newZi)
    }

    // ════════════════════════════════════════════════════════════════
    //  EEG PROCESSING
    // ════════════════════════════════════════════════════════════════

    fun processEEGSample(rawSample: Float): FilteredEEG {
        val (notchFiltered, newNotchZi) = lfilter(notchB, notchA, rawSample.toDouble(), eegNotchZi)
        eegNotchZi = newNotchZi

        val filteredValue = applyBiquadCascade(notchFiltered.toFloat(), eegFilterSections)
        emitFilteredEEG(filteredValue)

        val hasArtifact = abs(filteredValue) > EEG_ARTIFACT_THRESHOLD
        val cleanSample = if (hasArtifact) 0f else filteredValue

        eegBuffer[eegBufferIndex] = cleanSample
        eegBufferIndex = (eegBufferIndex + 1) % EEG_BUFFER_SIZE
        eegSamplesCollected++

        if (eegSamplesCollected < EEG_BUFFER_SIZE) {
            return FilteredEEG(filteredValue = filteredValue, hasArtifact = hasArtifact, isStable = false)
        }

        if (eegSamplesCollected % OVERLAP != 0) {
            return getSmoothedEEGOutput(filteredValue, hasArtifact)
        }

        val bufferArray = getEEGBufferArray()
        val (frequencies, psd) = calculateWelchPSD(bufferArray, EEG_SAMPLE_RATE.toFloat())
        val bandPowers = calculateBandPowers(frequencies, psd)

        deltaPowerHistory[eegHistoryIndex] = bandPowers["delta"] ?: 0f
        thetaPowerHistory[eegHistoryIndex] = bandPowers["theta"] ?: 0f
        alphaPowerHistory[eegHistoryIndex] = bandPowers["alpha"] ?: 0f
        betaPowerHistory[eegHistoryIndex]  = bandPowers["beta"]  ?: 0f
        gammaPowerHistory[eegHistoryIndex] = bandPowers["gamma"] ?: 0f
        eegHistoryIndex = (eegHistoryIndex + 1) % MOVING_AVG_WINDOW

        return getSmoothedEEGOutput(filteredValue, hasArtifact)
    }

    private fun getSmoothedEEGOutput(filteredValue: Float, hasArtifact: Boolean): FilteredEEG {
        val delta = deltaPowerHistory.average().toFloat()
        val theta = thetaPowerHistory.average().toFloat()
        val alpha = alphaPowerHistory.average().toFloat()
        val beta  = betaPowerHistory.average().toFloat()
        val gamma = gammaPowerHistory.average().toFloat()
        val totalPower = delta + theta + alpha + beta + gamma

        val alphaBetaRatio  = if (beta  > 0.0001f) alpha / beta  else 0f
        val thetaAlphaRatio = if (alpha > 0.0001f) theta / alpha else 0f
        val focusIndex      = if (alpha + theta > 0.0001f) beta / (alpha + theta) else 0f
        val relaxationIndex = alphaBetaRatio
        val drowsinessIndex = if (beta > 0.0001f) (theta + alpha) / beta else 0f

        val bands = mapOf("delta" to delta, "theta" to theta, "alpha" to alpha, "beta" to beta, "gamma" to gamma)
        val dominantBand = bands.maxByOrNull { it.value }?.key ?: ""

        val mentalState = classifyMentalState(delta, theta, alpha, beta, gamma, totalPower)

        val signalPower = alpha + beta
        val noisePower  = maxOf(delta * 0.3f, 0.001f)
        val snr         = 10f * log10(signalPower / noisePower)
        val isStable    = !hasArtifact && snr > 0f && totalPower > 0.1f && eegSamplesCollected >= EEG_BUFFER_SIZE

        return FilteredEEG(
            filteredValue   = filteredValue,
            delta           = delta,
            theta           = theta,
            alpha           = alpha,
            beta            = beta,
            gamma           = gamma,
            totalPower      = totalPower,
            dominantBand    = dominantBand,
            alphaBetaRatio  = alphaBetaRatio,
            thetaAlphaRatio = thetaAlphaRatio,
            focusIndex      = focusIndex,
            relaxationIndex = relaxationIndex,
            drowsinessIndex = drowsinessIndex,
            mentalState     = mentalState,
            snr             = snr,
            isStable        = isStable,
            hasArtifact     = hasArtifact
        )
    }

    private fun classifyMentalState(
        delta: Float, theta: Float, alpha: Float,
        beta: Float, gamma: Float, totalPower: Float
    ): MentalState {
        if (totalPower < 0.01f) return MentalState.UNKNOWN
        val nDelta = delta / totalPower
        val nTheta = theta / totalPower
        val nAlpha = alpha / totalPower
        val nBeta  = beta  / totalPower
        return when {
            nDelta > 0.5f                       -> MentalState.DEEP_SLEEP
            nTheta > 0.4f && nDelta > 0.2f      -> MentalState.LIGHT_SLEEP
            nAlpha > 0.3f && nTheta > 0.25f     -> MentalState.MEDITATIVE
            nAlpha > 0.35f                      -> MentalState.RELAXED
            nBeta  > 0.5f                       -> MentalState.STRESSED
            nBeta  > 0.35f                      -> MentalState.FOCUSED
            nAlpha > 0.2f && nBeta > 0.2f       -> MentalState.ALERT
            else                                -> MentalState.UNKNOWN
        }
    }

    private fun calculateBandPowers(frequencies: FloatArray, psd: FloatArray): Map<String, Float> {
        val bands = mapOf(
            "delta" to Pair(DELTA_LOW, DELTA_HIGH),
            "theta" to Pair(THETA_LOW, THETA_HIGH),
            "alpha" to Pair(ALPHA_LOW, ALPHA_HIGH),
            "beta"  to Pair(BETA_LOW,  BETA_HIGH),
            "gamma" to Pair(GAMMA_LOW, minOf(GAMMA_HIGH, EEG_SAMPLE_RATE / 2))
        )
        return bands.mapValues { (_, range) ->
            frequencies.indices
                .filter { frequencies[it] >= range.first && frequencies[it] <= range.second }
                .sumOf { psd[it].toDouble() }.toFloat() * 1000f
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  EOG PROCESSING (single-channel horizontal)
    //
    //  FIX: Removed eogBufferV (vertical channel) — it was declared but
    //  never written to, always zero. Cleaned up all dead vertical code.
    // ════════════════════════════════════════════════════════════════

    fun processEOGSample(rawSample: Float): FilteredEOG {
        val currentTime = System.currentTimeMillis()

        val polarityCorrected = if (EOG_INVERT_POLARITY) -rawSample else rawSample

        val (notchFiltered, newEogNotchZi) = lfilter(notchB, notchA, polarityCorrected.toDouble(), eogNotchZi)
        eogNotchZi = newEogNotchZi

        val filteredValue = applyBiquadCascade(notchFiltered.toFloat(), eogFilterSections)
        emitFilteredEOG(filteredValue)

        val hasArtifact = abs(filteredValue) > EOG_ARTIFACT_THRESHOLD
        val cleanSample = if (hasArtifact) 0f else filteredValue

        eogBuffer[eogBufferIndex] = cleanSample
        eogBufferIndex = (eogBufferIndex + 1) % EOG_BUFFER_SIZE
        eogSamplesCollected++

        val instantVelocity = if (prevEOGTime > 0) {
            val dt = (currentTime - prevEOGTime) / 1000f
            if (dt > 0 && dt < 0.5f) abs(cleanSample - prevEOGH) / dt else 0f
        } else 0f

        prevEOGH    = cleanSample
        prevEOGTime = currentTime

        val quickSaccade   = instantVelocity > EOG_SACCADE_VELOCITY_THRESHOLD
        val quickDirection = if (quickSaccade) determineQuickDirection1D(cleanSample, prevEOGH)
        else SaccadeDirection.NONE

        if (eogSamplesCollected < EOG_BUFFER_SIZE) {
            return FilteredEOG(
                filteredValue     = filteredValue,
                amplitude         = abs(filteredValue),
                isSaccadeDetected = quickSaccade,
                saccadeDirection  = quickDirection,
                saccadeVelocity   = instantVelocity,
                hasArtifact       = hasArtifact,
                isStable          = false
            )
        }

        val bufferArray      = getEOGBufferArray(eogBuffer)
        val maxVal           = bufferArray.maxOrNull() ?: 0f
        val minVal           = bufferArray.minOrNull() ?: 0f
        val amplitude        = maxVal - minVal
        val smoothedAmplitude = movingAverage(amplitude, eogAmplitudeHistory)

        val (isBlinkDetected, blinkStrength)                     = detectBlink(bufferArray)
        val (isSaccadeDetected, saccadeDirection, saccadeVelocity) = detectSaccade1D(bufferArray)

        val eyePosition  = estimateEyePosition(bufferArray)
        val eyeOpenness  = if (isBlinkDetected) 0f else 1f - (blinkStrength / 800f).coerceIn(0f, 1f)

        val signalPower  = rms(bufferArray)
        val noisePower   = calculateNoiseFloor(bufferArray)
        val snr          = if (noisePower > 0.0001f) 20f * log10(signalPower / noisePower) else 0f
        val isStable     = !hasArtifact && snr > 10f

        if (isSaccadeDetected || isBlinkDetected) {
            Log.d(TAG, "👁️ EOG: amp=%.1f blink=%s dir=%s vel=%.1f snr=%.1f dB".format(
                smoothedAmplitude, isBlinkDetected, saccadeDirection, saccadeVelocity, snr
            ))
        }

        return FilteredEOG(
            filteredValue     = filteredValue,
            amplitude         = smoothedAmplitude,
            isBlinkDetected   = isBlinkDetected,
            blinkStrength     = blinkStrength,
            isSaccadeDetected = isSaccadeDetected,
            saccadeDirection  = saccadeDirection,
            saccadeVelocity   = saccadeVelocity,
            eyePosition       = eyePosition,
            eyeOpenness       = eyeOpenness,
            isStable          = isStable,
            hasArtifact       = hasArtifact,
            snr               = snr
        )
    }

    private fun determineQuickDirection1D(current: Float, prev: Float): SaccadeDirection {
        val delta = current - prev
        return when {
            abs(delta) < 10f -> SaccadeDirection.NONE
            delta > 0        -> SaccadeDirection.RIGHT
            else             -> SaccadeDirection.LEFT
        }
    }

    private fun detectBlink(samples: FloatArray): Pair<Boolean, Float> {
        if (samples.size < 5) return Pair(false, 0f)
        val peakToPeak = (samples.maxOrNull() ?: 0f) - (samples.minOrNull() ?: 0f)
        if (peakToPeak < EOG_BLINK_THRESHOLD) return Pair(false, peakToPeak)

        var maxDerivative = 0f
        for (i in 1 until samples.size) {
            val d = abs(samples[i] - samples[i - 1])
            if (d > maxDerivative) maxDerivative = d
        }

        val hasRapidOnset = maxDerivative > peakToPeak * 0.2f
        val isBlink       = hasRapidOnset && peakToPeak >= EOG_BLINK_THRESHOLD

        if (isBlink) Log.v(TAG, "✅ BLINK: p2p=%.1f deriv=%.1f".format(peakToPeak, maxDerivative))
        return Pair(isBlink, peakToPeak)
    }

    private fun detectSaccade1D(samples: FloatArray): Triple<Boolean, SaccadeDirection, Float> {
        if (samples.size < 3) return Triple(false, SaccadeDirection.NONE, 0f)

        val maxVelocity = (1 until samples.size).maxOfOrNull { i ->
            abs((samples[i] - samples[i - 1]) * EOG_SAMPLE_RATE.toFloat())
        } ?: 0f

        if (maxVelocity < EOG_SACCADE_VELOCITY_THRESHOLD) return Triple(false, SaccadeDirection.NONE, 0f)

        val delta         = samples.last() - samples.first()
        val totalAmplitude = abs(delta)
        if (totalAmplitude < EOG_SACCADE_MIN_AMPLITUDE) return Triple(false, SaccadeDirection.NONE, maxVelocity)

        val direction = if (delta > 0) SaccadeDirection.RIGHT else SaccadeDirection.LEFT
        Log.d(TAG, "🎯 SACCADE: vel=%.1f amp=%.1f dir=%s".format(maxVelocity, totalAmplitude, direction))
        return Triple(true, direction, maxVelocity)
    }

    private fun estimateEyePosition(samples: FloatArray): Float = samples.average().toFloat()

    // ════════════════════════════════════════
    //  PERCLOS
    //
    //  FIX: Original implementation checked abs(samples[i]) > threshold,
    //  which detects high-amplitude blink events, not sustained eye closure.
    //  PERCLOS = percentage of time the eye is ≥80% closed.
    //  We approximate this by tracking when eyeOpenness drops below 0.2,
    //  estimated from amplitude relative to a rolling baseline.
    // ════════════════════════════════════════

    private fun calculatePerclos(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        val baseline = samples.map { abs(it) }.average().toFloat()
        if (baseline < 0.001f) return 0f

        var closedSamples = 0
        for (sample in samples) {
            // Eye is considered ≥80% closed when amplitude is ≥80% of baseline peak
            val openness = 1f - (abs(sample) / (baseline * 2f)).coerceIn(0f, 1f)
            if (openness < 0.2f) closedSamples++
        }
        return closedSamples.toFloat() / samples.size
    }

    // ════════════════════════════════════════
    //  EOG FEATURE EXTRACTION
    // ════════════════════════════════════════

    fun extractEOGFeatures(samples: FloatArray): EOGFeatures {
        val maxVal    = samples.maxOrNull() ?: 0f
        val minVal    = samples.minOrNull() ?: 0f
        val amplitude = maxVal - minVal
        val mean      = samples.average().toFloat()

        var zeroCrossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i - 1] - mean) * (samples[i] - mean) < 0) zeroCrossings++
        }

        val variance = samples.map { (it - mean).pow(2) }.average().toFloat()

        val (blinkCount, avgBlinkDuration) = countBlinksWithDuration(samples)
        val durationSeconds = samples.size / EOG_SAMPLE_RATE.toFloat()
        val blinkRate = blinkCount * 60f / durationSeconds

        val saccadeInfo = countSaccades(samples)
        val (fixationCount, avgFixationDuration) = detectFixations(samples)
        val perclos  = calculatePerclos(samples)
        val isDrowsy = detectDrowsiness(blinkRate, avgBlinkDuration, perclos)

        return EOGFeatures(
            amplitude           = amplitude,
            peakToPeak          = amplitude,
            zeroCrossings       = zeroCrossings,
            blinkRate           = blinkRate,
            blinkDuration       = avgBlinkDuration,
            saccadeCount        = saccadeInfo.first,
            saccadeAmplitude    = saccadeInfo.second,
            meanEyePosition     = mean,
            eyeMovementVariance = variance,
            fixationCount       = fixationCount,
            fixationDuration    = avgFixationDuration,
            isDrowsy            = isDrowsy,
            perclos             = perclos,
            confidence          = if (amplitude > 10f) 0.8f else 0.3f
        )
    }

    private fun countBlinksWithDuration(samples: FloatArray): Pair<Int, Float> {
        var count = 0; var totalDuration = 0f; var i = 0
        val windowSize = (0.5 * EOG_SAMPLE_RATE).toInt()
        while (i < samples.size - windowSize) {
            val window = samples.sliceArray(i until minOf(i + windowSize, samples.size))
            if (detectBlink(window).first) { count++; totalDuration += 200f; i += windowSize } else i++
        }
        return Pair(count, if (count > 0) totalDuration / count else 0f)
    }

    private fun countSaccades(samples: FloatArray): Pair<Int, Float> {
        var count = 0; var totalAmplitude = 0f; var i = 0
        val windowSize = (0.1 * EOG_SAMPLE_RATE).toInt()
        while (i < samples.size - windowSize) {
            val window = samples.sliceArray(i until minOf(i + windowSize, samples.size))
            val velocity = calculateWindowVelocity(window)
            if (velocity > EOG_SACCADE_VELOCITY_THRESHOLD) { count++; totalAmplitude += velocity; i += windowSize } else i++
        }
        return Pair(count, if (count > 0) totalAmplitude / count else 0f)
    }

    private fun calculateWindowVelocity(samples: FloatArray): Float {
        if (samples.size < 2) return 0f
        return (1 until samples.size).maxOfOrNull { i ->
            abs(samples[i] - samples[i - 1]) * EOG_SAMPLE_RATE.toFloat()
        } ?: 0f
    }

    private fun detectFixations(samples: FloatArray): Pair<Int, Float> {
        val windowSize = (0.2 * EOG_SAMPLE_RATE).toInt()
        var fixationCount = 0; var totalFixationDuration = 0f
        var inFixation = false; var fixationStart = 0; var i = 0
        while (i < samples.size - windowSize) {
            val window   = samples.sliceArray(i until i + windowSize)
            val mean     = window.average().toFloat()
            val variance = window.map { (it - mean).pow(2) }.average().toFloat()
            if (variance < 10f) {
                if (!inFixation) { inFixation = true; fixationStart = i }
            } else if (inFixation) {
                fixationCount++
                totalFixationDuration += (i - fixationStart) / EOG_SAMPLE_RATE.toFloat() * 1000f
                inFixation = false
            }
            i += windowSize / 2
        }
        return Pair(fixationCount, if (fixationCount > 0) totalFixationDuration / fixationCount else 0f)
    }

    private fun detectDrowsiness(blinkRate: Float, blinkDuration: Float, perclos: Float): Boolean =
        blinkRate < 10f || blinkRate > 25f || blinkDuration > 300f || perclos > 0.15f

    // ════════════════════════════════════════
    //  PSD CALCULATION (Welch / DFT)
    // ════════════════════════════════════════

    private fun calculateWelchPSD(samples: FloatArray, sampleRate: Float): Pair<FloatArray, FloatArray> {
        val n = minOf(samples.size, FFT_SIZE)
        val windowedSamples = FloatArray(n) { i -> if (i < samples.size) samples[i] * hannWindow[i] else 0f }
        var windowPower = 0f
        for (i in 0 until n) windowPower += hannWindow[i] * hannWindow[i]

        val magnitudes = FloatArray(n / 2)
        for (k in 0 until n / 2) {
            var realSum = 0.0; var imagSum = 0.0
            for (t in 0 until n) {
                val angle = 2.0 * PI * k * t / n
                realSum += windowedSamples[t] * cos(angle)
                imagSum -= windowedSamples[t] * sin(angle)
            }
            magnitudes[k] = ((realSum * realSum + imagSum * imagSum) / (sampleRate * windowPower)).toFloat()
        }

        val freqResolution = sampleRate / n
        val frequencies    = FloatArray(n / 2) { k -> k * freqResolution }
        return Pair(frequencies, magnitudes)
    }

    fun extractEEGFeatures(samples: FloatArray): EEGFeatures {
        val (frequencies, psd) = calculateWelchPSD(samples, EEG_SAMPLE_RATE.toFloat())
        val bandPowers = calculateBandPowers(frequencies, psd)
        val delta = bandPowers["delta"] ?: 0f; val theta = bandPowers["theta"] ?: 0f
        val alpha = bandPowers["alpha"] ?: 0f; val beta  = bandPowers["beta"]  ?: 0f
        val gamma = bandPowers["gamma"] ?: 0f

        val alphaBetaRatio  = if (beta  > 0.0001f) alpha / beta  else 0f
        val thetaAlphaRatio = if (alpha > 0.0001f) theta / alpha else 0f
        val thetaBetaRatio  = if (beta  > 0.0001f) theta / beta  else 0f
        val deltaAlphaRatio = if (alpha > 0.0001f) delta / alpha else 0f
        val focusIndex      = if (alpha + theta > 0.0001f) beta / (alpha + theta) else 0f
        val drowsinessIndex = if (beta > 0.0001f) (theta + alpha) / beta else 0f

        val peakIndex        = psd.indices.maxByOrNull { psd[it] } ?: 0
        val peakFrequency    = frequencies[peakIndex]
        var sumFPsd = 0f; var sumPsd = 0f
        psd.indices.forEach { i -> sumFPsd += frequencies[i] * psd[i]; sumPsd += psd[i] }
        val spectralCentroid = if (sumPsd > 0) sumFPsd / sumPsd else 0f

        var entropy = 0f
        if (sumPsd > 0) psd.forEach { p -> if (p > 0) { val prob = p / sumPsd; entropy -= prob * log2(prob) } }
        val spectralEntropy = if (psd.size > 1) entropy / log2(psd.size.toFloat()) else 0f
        val spectralSlope   = calculateSpectralSlope(frequencies, psd)
        val totalPower      = delta + theta + alpha + beta + gamma

        return EEGFeatures(
            E_delta = delta, E_theta = theta, E_alpha = alpha, E_beta = beta, E_gamma = gamma,
            alphaBetaRatio = alphaBetaRatio, thetaAlphaRatio = thetaAlphaRatio,
            thetaBetaRatio = thetaBetaRatio, deltaAlphaRatio = deltaAlphaRatio,
            peakFrequency = peakFrequency, spectralCentroid = spectralCentroid,
            spectralEntropy = spectralEntropy, spectralSlope = spectralSlope,
            focusIndex = focusIndex, relaxationIndex = alphaBetaRatio,
            drowsinessIndex = drowsinessIndex, engagementIndex = focusIndex,
            predictedState = classifyMentalState(delta, theta, alpha, beta, gamma, totalPower),
            confidence = minOf(1f, sumPsd / 10f).coerceIn(0f, 1f)
        )
    }

    private fun calculateSpectralSlope(frequencies: FloatArray, psd: FloatArray): Float {
        val logF   = (1 until frequencies.size).filter { frequencies[it] > 0 && psd[it] > 0 }.map { ln(frequencies[it].toDouble()).toFloat() }
        val logPsd = (1 until frequencies.size).filter { frequencies[it] > 0 && psd[it] > 0 }.map { ln(psd[it].toDouble()).toFloat() }
        return linearRegressionSlope(logF, logPsd)
    }

    private fun linearRegressionSlope(x: List<Float>, y: List<Float>): Float {
        val n = x.size; if (n < 2) return 0f
        val sumX = x.sum(); val sumY = y.sum()
        val sumXY = x.zip(y).sumOf { (a, b) -> (a * b).toDouble() }.toFloat()
        val sumX2 = x.sumOf { (it * it).toDouble() }.toFloat()
        val denom = n * sumX2 - sumX * sumX
        return if (abs(denom) > 0.0001f) (n * sumXY - sumX * sumY) / denom else 0f
    }

    // ════════════════════════════════════════
    //  BUFFER PROCESSING
    // ════════════════════════════════════════

    fun processEEGBuffer(rawSamples: FloatArray): EEGFeatures {
        val filtered = FloatArray(rawSamples.size)
        rawSamples.forEachIndexed { i, sample ->
            val (notchFiltered, newZi) = lfilter(notchB, notchA, sample.toDouble(), eegNotchZi)
            eegNotchZi = newZi
            filtered[i] = applyBiquadCascade(notchFiltered.toFloat(), eegFilterSections)
        }
        return extractEEGFeatures(filtered)
    }

    fun processEOGBuffer(rawSamples: FloatArray): EOGFeatures {
        val filtered = FloatArray(rawSamples.size) { i -> applyBiquadCascade(rawSamples[i], eogFilterSections) }
        return extractEOGFeatures(filtered)
    }

    // ════════════════════════════════════════
    //  UTILITY METHODS
    // ════════════════════════════════════════

    private fun getEEGBufferArray(): FloatArray = FloatArray(EEG_BUFFER_SIZE) { i ->
        eegBuffer[(eegBufferIndex + i) % EEG_BUFFER_SIZE]
    }

    private fun getEOGBufferArray(buffer: FloatArray): FloatArray = FloatArray(EOG_BUFFER_SIZE) { i ->
        buffer[(eogBufferIndex + i) % EOG_BUFFER_SIZE]
    }

    fun movingAverage(value: Float, history: ArrayDeque<Float>): Float {
        history.addLast(value)
        if (history.size > MOVING_AVG_WINDOW) history.removeFirst()
        return if (history.isNotEmpty()) history.average().toFloat() else value
    }

    fun rms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        return sqrt(samples.fold(0.0) { acc, v -> acc + v * v } / samples.size).toFloat()
    }

    private fun calculateNoiseFloor(samples: FloatArray): Float {
        val windowSize = samples.size / 4
        if (windowSize < 2) return 0.0001f
        var minRms = Float.MAX_VALUE
        for (i in 0 until samples.size - windowSize step windowSize / 2) {
            val r = rms(samples.sliceArray(i until i + windowSize))
            if (r < minRms && r > 0) minRms = r
        }
        return maxOf(minRms, 0.0001f)
    }

    private fun log2(x: Float): Float = (ln(x.toDouble()) / ln(2.0)).toFloat()

    // ════════════════════════════════════════
    //  RESET
    // ════════════════════════════════════════

    fun reset() { resetEEG(); resetEOG(); Log.d(TAG, "Filter reset complete") }

    fun resetEEG() {
        eegBuffer.fill(0f); eegBufferIndex = 0; eegSamplesCollected = 0
        eegNotchZi = DoubleArray(2) { 0.0 }; eegFilterSections.forEach { it.reset() }
        deltaPowerHistory.fill(0f); thetaPowerHistory.fill(0f); alphaPowerHistory.fill(0f)
        betaPowerHistory.fill(0f);  gammaPowerHistory.fill(0f); eegHistoryIndex = 0
    }

    fun resetEOG() {
        eogBuffer.fill(0f); eogBufferIndex = 0; eogSamplesCollected = 0
        eogNotchZi = DoubleArray(2) { 0.0 }; eogFilterSections.forEach { it.reset() }
        eogAmplitudeHistory.clear(); prevEOGH = 0f; prevEOGTime = 0L
    }
}/*
package com.neurodrive.smartcar.domain

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class SignalFilter @Inject constructor() {

    companion object {
        private const val TAG = "SimpleSignalFilter"

        // EEG: 0.5–30 Hz @ 256 Hz
        const val EEG_SAMPLE_RATE = 256f

        // EOG: 0.16–15 Hz @ 75 Hz
        const val EOG_SAMPLE_RATE = 75f

        // Thresholds
        const val EEG_ARTIFACT_THRESHOLD = 150f
        const val EOG_BLINK_THRESHOLD = 400f
        const val EOG_SACCADE_THRESHOLD = 200f
    }

    enum class SignalType { EEG, EOG }

    // ════════════════════════════════════════
    //  SIMPLE BIQUAD FILTER
    // ════════════════════════════════════════

    data class SimpleBiquad(
        val b0: Float, val b1: Float, val b2: Float,
        val a1: Float, val a2: Float,
        var z1: Float = 0f, var z2: Float = 0f
    ) {
        fun process(x: Float): Float {
            val y = b0 * x + z1
            z1 = b1 * x - a1 * y + z2
            z2 = b2 * x - a2 * y
            return y
        }
        fun reset() { z1 = 0f; z2 = 0f }
    }

    // ════════════════════════════════════════
    //  EEG FILTER (0.5–30 Hz)
    // ════════════════════════════════════════

    private val eegFilter = listOf(
        // HP 0.5 Hz
        SimpleBiquad(b0=0.9934f, b1=-1.9868f, b2=0.9934f, a1=-1.9868f, a2=0.9869f),
        // LP 30 Hz
        SimpleBiquad(b0=0.0924f, b1=0.1848f, b2=0.0924f, a1=-1.1429f, a2=0.4128f)
    )

    // ════════════════════════════════════════
    //  EOG FILTER (0.16–15 Hz)
    // ════════════════════════════════════════

    private val eogFilter = listOf(
        // HP 0.16 Hz
        SimpleBiquad(b0=0.9979f, b1=-1.9958f, b2=0.9979f, a1=-1.9958f, a2=0.9981f),
        // LP 15 Hz
        SimpleBiquad(b0=0.1017f, b1=0.2034f, b2=0.1017f, a1=-0.7524f, a2=0.1592f)
    )

    // ════════════════════════════════════════
    //  BUFFERS FOR STATISTICS
    // ════════════════════════════════════════

    private val eegBuffer = FloatArray(256)
    private var eegIdx = 0

    private val eogBuffer = FloatArray(75)
    private var eogIdx = 0

    // Real-time callbacks
    var onFilteredEEG: ((Float) -> Unit)? = null
    var onFilteredEOG: ((Float) -> Unit)? = null

    // ════════════════════════════════════════
    //  SIMPLE OUTPUT
    // ════════════════════════════════════════

    data class FilteredSignal(
        val filteredValue: Float = 0f,
        val amplitude: Float = 0f,
        val rms: Float = 0f,
        val hasArtifact: Boolean = false,
        val snr: Float = 0f
    )

    data class ThighActivity(
        val eeg: FilteredSignal = FilteredSignal(),
        val eog: FilteredSignal = FilteredSignal(),
        val timestamp: Long = System.currentTimeMillis(),
        val isMoving: Boolean = false,
        val intensity: Float = 0f  // 0.0–1.0
    )

    // ════════════════════════════════════════
    //  EEG PROCESSING
    // ════════════════════════════════════════

    fun processEEGSample(rawValue: Float): FilteredSignal {
        // Apply cascade of 2 biquad filters
        var filtered = rawValue
        eegFilter.forEach { filtered = it.process(filtered) }

        onFilteredEEG?.invoke(filtered)

        // Check artifact
        val hasArtifact = abs(filtered) > EEG_ARTIFACT_THRESHOLD

        // Store in buffer
        eegBuffer[eegIdx] = filtered
        eegIdx = (eegIdx + 1) % eegBuffer.size

        // Calculate RMS and amplitude
        val rmsValue = rms(eegBuffer)
        val amplitude = eegBuffer.maxOrNull()?.minus(eegBuffer.minOrNull() ?: 0f) ?: 0f
        val snr = if (rmsValue > 0.001f) 20 * log10(rmsValue) else 0f

        return FilteredSignal(
            filteredValue = filtered,
            amplitude = amplitude,
            rms = rmsValue,
            hasArtifact = hasArtifact,
            snr = snr
        )
    }

    // ════════════════════════════════════════
    //  EOG PROCESSING
    // ════════════════════════════════════════

    fun processEOGSample(rawValue: Float): FilteredSignal {
        // Apply cascade of 2 biquad filters
        var filtered = rawValue
        eogFilter.forEach { filtered = it.process(filtered) }

        onFilteredEOG?.invoke(filtered)

        // Check artifact
        val hasArtifact = abs(filtered) > EOG_BLINK_THRESHOLD

        // Store in buffer
        eogBuffer[eogIdx] = filtered
        eogIdx = (eogIdx + 1) % eogBuffer.size

        // Calculate RMS and amplitude
        val rmsValue = rms(eogBuffer)
        val amplitude = eogBuffer.maxOrNull()?.minus(eogBuffer.minOrNull() ?: 0f) ?: 0f
        val snr = if (rmsValue > 0.001f) 20 * log10(rmsValue) else 0f

        return FilteredSignal(
            filteredValue = filtered,
            amplitude = amplitude,
            rms = rmsValue,
            hasArtifact = hasArtifact,
            snr = snr
        )
    }

    // ════════════════════════════════════════
    //  THIGH ACTIVITY DETECTION
    // ════════════════════════════════════════

    fun detectThighActivity(eegSample: Float, eogSample: Float): ThighActivity {
        val eeg = processEEGSample(eegSample)
        val eog = processEOGSample(eogSample)

        // Simple movement detection: high EOG amplitude + RMS changes
        val isMoving = eog.amplitude > 100f || eog.rms > 50f

        // Intensity: normalized RMS
        val intensity = (eeg.rms / 100f).coerceIn(0f, 1f)

        Log.d(TAG, "📊 EEG: %.1f mV (RMS) | EOG: %.1f mV | Moving: $isMoving | Intensity: %.2f".format(
            eeg.rms, eog.rms, intensity
        ))

        return ThighActivity(
            eeg = eeg,
            eog = eog,
            isMoving = isMoving,
            intensity = intensity
        )
    }

    // ════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════

    private fun rms(samples: FloatArray): Float {
        val mean = samples.average().toFloat()
        return sqrt(samples.map { (it - mean).pow(2) }.average()).toFloat()
    }

    fun reset() {
        eegFilter.forEach { it.reset() }
        eogFilter.forEach { it.reset() }
        eegBuffer.fill(0f)
        eogBuffer.fill(0f)
        eegIdx = 0
        eogIdx = 0
        Log.d(TAG, "Filter reset")
    }
}*/