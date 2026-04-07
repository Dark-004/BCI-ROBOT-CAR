package com.neurodrive.smartcar.domain

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.neurodrive.smartcar.data.model.*
import com.neurodrive.smartcar.data.repository.BioSignalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bioSignalRepository: BioSignalRepository
) {
    companion object {
        private const val TAG = "BleManager"

        // ─────────────────────────────────────────────────────────────
        // FIX: UUIDs now match the ESP32 firmware exactly.
        //
        // ESP32 exposes two characteristics:
        //   COMMAND_CHAR_UUID  (...26aa) — 7-byte drive+steer+activity+eog+battery
        //   STATUS_CHAR_UUID   (...26ab) — 7-byte eegReady+eogReady+uptime+battery
        //
        // The old EEG_DATA_CHAR_UUID (...26a8) and EOG_DATA_CHAR_UUID (...26a9)
        // were never advertised by the ESP32, so notifications never arrived.
        // ─────────────────────────────────────────────────────────────
        val EEG_SERVICE_UUID: UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

        val COMMAND_CHAR_UUID: UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26aa")

        val STATUS_CHAR_UUID: UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26ab")

        val CCCD_UUID: UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // ─────────────────────────────────────────────────────────────
        // COMMAND packet layout (7 bytes, little-endian):
        //   [0]    driveCommand    uint8  (0=STOP 1=FWD 2=BWD 3=EMERGENCY)
        //   [1]    steerCommand    uint8  (0=STRAIGHT 1=LEFT 2=RIGHT)
        //   [2][3] activityLevel   int16  (little-endian, raw ADC variance)
        //   [4][5] eogValue        int16  (little-endian, raw ADC 0-4095)
        //   [6]    batteryPercent  uint8  (0–100)
        //
        // STATUS packet layout (7 bytes, little-endian):
        //   [0]          eegReady        uint8
        //   [1]          eogReady        uint8
        //   [2][3][4][5] uptimeSeconds   uint32 (little-endian)
        //   [6]          batteryPercent  uint8
        // ─────────────────────────────────────────────────────────────
        private const val COMMAND_PACKET_SIZE = 7
        private const val STATUS_PACKET_SIZE  = 7

        // EOG polarity correction — set true only if saccade directions are inverted.
        // Prefer fixing hardware (swap electrode leads) over software inversion.
        private const val EOG_INVERT_POLARITY = false

        private const val SCAN_TIMEOUT_MS              = 30_000L
        private const val SCAN_COOLDOWN_MS             = 2_000L
        private const val CONNECTION_TIMEOUT_MS        = 15_000L
        private const val SERVICE_DISCOVERY_DELAY_MS   = 600L
        private const val CONNECT_AFTER_SCAN_DELAY_MS  = 1_000L
        private const val NOTIFICATION_STAGGER_MS      = 500L

        // EOG noise floor calibration — updated from a quiet baseline window
        private const val EOG_NOISE_FLOOR_DEFAULT = 10f   // µV, updated at runtime
    }

    // ══════════════════════════════════
    //  PUBLIC STATE FLOWS
    // ══════════════════════════════════

    var connectedDeviceName: String? = null
        private set

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _bioSignals = MutableStateFlow<BioSignal?>(null)
    val bioSignals: StateFlow<BioSignal?> = _bioSignals.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _rssi = MutableStateFlow(0)
    val rssi: StateFlow<Int> = _rssi.asStateFlow()

    /** Battery percentage (0–100). -1 means not yet received. */
    private val _batteryPercent = MutableStateFlow(-1)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    // ══════════════════════════════════
    //  PRIVATE STATE
    // ══════════════════════════════════

    private var bluetoothGatt: BluetoothGatt? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scanLock = AtomicBoolean(false)
    private var lastScanStopTime = 0L
    private var connectionTimeoutRunnable: Runnable? = null

    private val scanTimeoutRunnable = Runnable {
        Log.d(TAG, "⏰ Scan timeout — auto-stopping")
        stopScan()
    }

    // ══════════════════════════════════
    //  EOG NOISE FLOOR (adaptive)
    //
    //  FIX: Instead of hardcoding 10µV, we track a running minimum
    //  RMS from quiet periods and use it for SNR calculation.
    // ══════════════════════════════════

    private var eogNoiseFloor = EOG_NOISE_FLOOR_DEFAULT
    private val eogCalibrationSamples = mutableListOf<Float>()
    private var isCalibrating = false

    fun startEOGCalibration() {
        eogCalibrationSamples.clear()
        isCalibrating = true
        Log.d(TAG, "🎯 EOG calibration started — keep eyes still for 3 seconds")
    }

    private fun updateEOGNoiseFloor(amplitude: Float) {
        if (!isCalibrating) return
        eogCalibrationSamples.add(amplitude)
        if (eogCalibrationSamples.size >= 225) {  // ~3 seconds at 75 Hz
            val rms = kotlin.math.sqrt(
                eogCalibrationSamples.map { it * it }.average()
            ).toFloat()
            eogNoiseFloor = maxOf(rms, 0.1f)
            isCalibrating = false
            Log.d(TAG, "✅ EOG noise floor calibrated: ${eogNoiseFloor}µV")
        }
    }

    // ══════════════════════════════════
    //  STATISTICS
    // ══════════════════════════════════

    private var totalSamples = 0L
    private var blinkCount   = 0
    private var saccadeCount = 0

    // ════════════════════════════════════════
    //  SCANNING
    // ════════════════════════════════════════

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (!scanLock.compareAndSet(false, true)) return

        try {
            if (_isScanning.value) {
                Log.w(TAG, "⚠️ Scan already in progress")
                return
            }

            val timeSinceLastStop = System.currentTimeMillis() - lastScanStopTime
            if (timeSinceLastStop < SCAN_COOLDOWN_MS && lastScanStopTime > 0) {
                Log.w(TAG, "⚠️ Scan cooldown — ${SCAN_COOLDOWN_MS - timeSinceLastStop}ms remaining")
                return
            }

            val adapter = bluetoothAdapter
            if (adapter == null || !adapter.isEnabled) {
                Log.e(TAG, "❌ Bluetooth adapter null or disabled")
                return
            }

            val scanner = adapter.bluetoothLeScanner ?: run {
                Log.e(TAG, "❌ BLE Scanner is NULL")
                return
            }

            if (!BlePermissionHelper.hasAllPermissions(context)) {
                Log.e(TAG, "❌ Missing permissions: ${BlePermissionHelper.getMissingPermissions(context)}")
                return
            }

            _discoveredDevices.value = emptyList()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0)
                .build()

            try {
                scanner.startScan(null, settings, scanCallback)
                _isScanning.value = true
                Log.d(TAG, "✅ Scan started")
                mainHandler.removeCallbacks(scanTimeoutRunnable)
                mainHandler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS)
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ SecurityException: ${e.message}")
                _isScanning.value = false
            } catch (e: Exception) {
                Log.e(TAG, "❌ Scan start failed: ${e.message}")
                _isScanning.value = false
            }
        } finally {
            scanLock.set(false)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        mainHandler.removeCallbacks(scanTimeoutRunnable)
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Stop scan error: ${e.message}")
        }
        _isScanning.value = false
        lastScanStopTime = System.currentTimeMillis()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address
            val currentList = _discoveredDevices.value
            if (currentList.none { it.address == address }) {
                _discoveredDevices.value = currentList + device
                val name = try { device.name ?: "Unknown" } catch (_: SecurityException) { "Unknown" }
                Log.d(TAG, "📱 Found: $name [$address] RSSI: ${result.rssi}")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            mainHandler.removeCallbacks(scanTimeoutRunnable)
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED                  -> "ALREADY_STARTED"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED  -> "APP_REGISTRATION_FAILED"
                SCAN_FAILED_INTERNAL_ERROR                   -> "INTERNAL_ERROR"
                SCAN_FAILED_FEATURE_UNSUPPORTED              -> "FEATURE_UNSUPPORTED"
                5                                            -> "OUT_OF_HARDWARE_RESOURCES"
                6                                            -> "SCANNING_TOO_FREQUENTLY"
                else                                         -> "UNKNOWN ($errorCode)"
            }
            Log.e(TAG, "❌ Scan failed: $errorMsg")
        }
    }

    // ════════════════════════════════════════
    //  CONNECTION
    // ════════════════════════════════════════

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        try {
            connectedDeviceName = device.name
            Log.d(TAG, "🔗 Connecting to: $connectedDeviceName")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ BLUETOOTH_CONNECT permission missing")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "❌ Adapter null or disabled")
            return
        }

        stopScan()
        closeGatt()
        resetStatistics()

        mainHandler.postDelayed({
            try {
                bluetoothGatt = device.connectGatt(
                    context, false, gattCallback, BluetoothDevice.TRANSPORT_LE
                )

                if (bluetoothGatt == null) {
                    Log.e(TAG, "❌ connectGatt() returned NULL")
                    return@postDelayed
                }

                connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                connectionTimeoutRunnable = Runnable {
                    if (!_connectionState.value) {
                        Log.e(TAG, "⏰ Connection timeout")
                        closeGatt()
                    }
                }
                mainHandler.postDelayed(connectionTimeoutRunnable!!, CONNECTION_TIMEOUT_MS)

            } catch (e: Exception) {
                Log.e(TAG, "❌ connectGatt exception: ${e.message}")
            }
        }, CONNECT_AFTER_SCAN_DELAY_MS)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        cancelConnectionTimeout()
        closeGatt()
        _connectionState.value = false
        connectedDeviceName = null
        _batteryPercent.value = -1
        resetStatistics()
        Log.d(TAG, "🔌 Disconnected")
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        bluetoothGatt?.let {
            try { it.disconnect(); it.close() } catch (e: Exception) {
                Log.e(TAG, "Error closing GATT: ${e.message}")
            }
        }
        bluetoothGatt = null
    }

    private fun cancelConnectionTimeout() {
        connectionTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        connectionTimeoutRunnable = null
    }

    // ════════════════════════════════════════
    //  GATT CALLBACK
    // ════════════════════════════════════════

    private val gattCallback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "✅ CONNECTED")
                        _connectionState.value = true
                        cancelConnectionTimeout()
                        mainHandler.postDelayed({ gatt.discoverServices() }, SERVICE_DISCOVERY_DELAY_MS)
                    } else {
                        Log.e(TAG, "❌ Connected but status=$status")
                        gatt.close()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.e(TAG, "❌ DISCONNECTED")
                    _connectionState.value = false
                    _batteryPercent.value = -1
                    cancelConnectionTimeout()
                    gatt.close()
                    bluetoothGatt = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "❌ Service discovery FAILED: $status")
                return
            }

            Log.d(TAG, "📋 Services discovered — subscribing to NeuroDrive characteristics")

            // FIX: Subscribe to COMMAND_CHAR_UUID (...26aa) and STATUS_CHAR_UUID (...26ab)
            // which are the UUIDs actually advertised by the ESP32 firmware.
            gatt.services.forEach { service ->
                service.characteristics.forEach { char ->
                    when (char.uuid) {
                        COMMAND_CHAR_UUID -> {
                            Log.d(TAG, "✅ Found COMMAND characteristic (...26aa)")
                            enableNotification(gatt, char)
                        }
                        STATUS_CHAR_UUID -> {
                            Log.d(TAG, "✅ Found STATUS characteristic (...26ab)")
                            mainHandler.postDelayed({
                                enableNotification(gatt, char)
                            }, NOTIFICATION_STAGGER_MS)
                        }
                    }
                }
            }
        }

        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            when (characteristic.uuid) {
                COMMAND_CHAR_UUID -> handleCommandPacket(data)
                STATUS_CHAR_UUID  -> handleStatusPacket(data)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        characteristic.getDescriptor(CCCD_UUID)?.let { descriptor ->
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
    }

    // ════════════════════════════════════════
    //  DATA HANDLING — COMMAND PACKET
    //
    //  FIX: Parser now matches the actual 7-byte ESP32 packet layout.
    //  Old code expected a 19-byte EEG packet with 0xEE header and
    //  IEEE-754 floats — none of which the ESP32 sends.
    //
    //  Layout:
    //    [0]    driveCommand    uint8
    //    [1]    steerCommand    uint8
    //    [2][3] activityLevel   int16 LE
    //    [4][5] eogValue        int16 LE
    //    [6]    batteryPercent  uint8
    // ════════════════════════════════════════

    private fun handleCommandPacket(data: ByteArray) {
        if (data.size < COMMAND_PACKET_SIZE) {
            Log.w(TAG, "⚠️ COMMAND packet too short: ${data.size} bytes (expected $COMMAND_PACKET_SIZE)")
            return
        }

        try {
            val driveCode    = data[0].toInt() and 0xFF
            val steerCode    = data[1].toInt() and 0xFF
            val activityLevel = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
            var eogValue     = (data[4].toInt() and 0xFF) or ((data[5].toInt() and 0xFF) shl 8)
            val battery      = data[6].toInt() and 0xFF

            // Apply polarity correction if needed
            if (EOG_INVERT_POLARITY) {
                eogValue = 4095 - eogValue  // invert around mid-scale for 12-bit ADC
            }

            _batteryPercent.value = battery.coerceIn(0, 100)

            // Map drive command code → MentalState
            val mentalState = when (driveCode) {
                1    -> MentalState.FOCUSED   // FORWARD
                2    -> MentalState.RELAXED   // BACKWARD
                3    -> MentalState.DEEP_SLEEP // EMERGENCY
                else -> MentalState.NEUTRAL   // STOP
            }

            // Map steer command code → SaccadeDirection
            val saccadeDirection = when (steerCode) {
                1    -> SaccadeDirection.LEFT
                2    -> SaccadeDirection.RIGHT
                else -> SaccadeDirection.NONE
            }

            // Normalize activity level to a 0–1 focusIndex for EEGSignal
            val focusIndex = (activityLevel.toFloat() / 100f).coerceIn(0f, 1f)

            // Normalize EOG value (0–4095 ADC) to signed amplitude around center (1800)
            val eogCenter     = 1800
            val eogAmplitude  = (eogValue - eogCenter).toFloat()
            val absAmplitude  = kotlin.math.abs(eogAmplitude)

            // FIX: Use adaptive noise floor for SNR instead of hardcoded 10µV
            updateEOGNoiseFloor(absAmplitude)
            val eogSNR = if (eogNoiseFloor > 0.1f && absAmplitude > 0) {
                20f * kotlin.math.log10(absAmplitude / eogNoiseFloor)
            } else 0f

            val eegSignal = EEGSignal(
                filteredValue    = activityLevel.toFloat(),
                alpha            = 0f,
                beta             = 0f,
                theta            = 0f,
                delta            = 0f,
                gamma            = 0f,
                totalPower       = activityLevel.toFloat(),
                alphaBetaRatio   = 0f,
                focusIndex       = focusIndex,
                relaxationIndex  = 1f - focusIndex,
                drowsinessIndex  = if (driveCode == 3) 1f else 0f,
                mentalState      = mentalState,
                snr              = if (activityLevel > 5) 15f else 0f,  // basic quality flag
                isStable         = activityLevel > 5,
                hasArtifact      = false,
                timestamp        = System.currentTimeMillis()
            )

            val eogSignal = EOGSignal(
                filteredValue    = eogAmplitude,
                amplitude        = absAmplitude,
                isBlinkDetected  = false,
                blinkStrength    = 0f,
                saccadeDirection = saccadeDirection,
                isSaccadeDetected = saccadeDirection != SaccadeDirection.NONE,
                eyeOpenness      = if (driveCode == 3) 0f else 1f,
                blinkCount       = blinkCount,
                saccadeCount     = saccadeCount,
                isStable         = eogSNR > 10f,
                snr              = eogSNR,
                timestamp        = System.currentTimeMillis()
            )

            if (saccadeDirection != SaccadeDirection.NONE) saccadeCount++

            val bioSignal = BioSignal.fromEEGAndEOG(eegSignal, eogSignal)
            bioSignalRepository.processSignal(bioSignal)
            _bioSignals.value = bioSignalRepository.currentSignal.value
            totalSamples++

            Log.v(TAG, "📦 CMD: drive=$driveCode steer=$steerCode activity=$activityLevel eog=$eogValue bat=$battery%")

        } catch (e: Exception) {
            Log.e(TAG, "❌ COMMAND packet parse error: ${e.message}")
        }
    }

    // ════════════════════════════════════════
    //  DATA HANDLING — STATUS PACKET
    //
    //  Layout:
    //    [0]          eegReady       uint8
    //    [1]          eogReady       uint8
    //    [2][3][4][5] uptimeSeconds  uint32 LE
    //    [6]          batteryPercent uint8
    // ════════════════════════════════════════

    private fun handleStatusPacket(data: ByteArray) {
        if (data.size < STATUS_PACKET_SIZE) {
            Log.w(TAG, "⚠️ STATUS packet too short: ${data.size} bytes")
            return
        }

        try {
            val eegReady  = data[0].toInt() != 0
            val eogReady  = data[1].toInt() != 0
            val uptime    = ByteBuffer.wrap(data, 2, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong()
            val battery   = data[6].toInt() and 0xFF

            _batteryPercent.value = battery.coerceIn(0, 100)

            Log.d(TAG, "📊 STATUS: eeg=$eegReady eog=$eogReady uptime=${uptime}s bat=$battery%")

        } catch (e: Exception) {
            Log.e(TAG, "❌ STATUS packet parse error: ${e.message}")
        }
    }

    // ════════════════════════════════════════
    //  STATISTICS
    // ════════════════════════════════════════

    fun getSignalStats(): SignalStats {
        return SignalStats(
            totalSamples        = totalSamples,
            isFilteringEnabled  = false,
            isStable            = _bioSignals.value?.isStable ?: false,
            blinkCount          = blinkCount,
            saccadeCount        = saccadeCount
        )
    }

    private fun resetStatistics() {
        totalSamples = 0
        blinkCount   = 0
        saccadeCount = 0
    }

    // ════════════════════════════════════════
    //  CLEANUP
    // ════════════════════════════════════════

    fun cleanup() {
        stopScan()
        cancelConnectionTimeout()
        mainHandler.removeCallbacksAndMessages(null)
        closeGatt()
        _connectionState.value = false
        _batteryPercent.value  = -1
        resetStatistics()
        Log.d(TAG, "🧹 BleManager cleaned up")
    }

    // ════════════════════════════════════════
    //  UTILITIES
    // ════════════════════════════════════════

    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}

// ════════════════════════════════════════
//  SIGNAL STATISTICS DATA CLASS
// ════════════════════════════════════════

data class SignalStats(
    val totalSamples       : Long    = 0,
    val artifactCount      : Long    = 0,
    val artifactRate       : Float   = 0f,
    val isFilteringEnabled : Boolean = false,
    val currentSNR         : Float   = 0f,
    val isStable           : Boolean = false,
    val blinkCount         : Int     = 0,
    val saccadeCount       : Int     = 0
)
