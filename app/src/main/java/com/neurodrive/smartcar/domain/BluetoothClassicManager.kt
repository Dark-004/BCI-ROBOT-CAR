package com.neurodrive.smartcar.domain

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.neurodrive.smartcar.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.neurodrive.smartcar.data.model.BioSignal
import com.neurodrive.smartcar.data.model.ESP32Alert
import com.neurodrive.smartcar.data.model.ESP32SensorData


@Singleton
class BluetoothClassicManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BTClassicManager"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECT_TIMEOUT_MS = 15000L
        private const val READ_BUFFER_SIZE = 1024
        private const val DISCOVERY_TIMEOUT_MS = 30000L
    }

    // ══════════════════════════════════
    //  PERMISSION HELPER
    // ══════════════════════════════════

    /**
     * Returns true when the app holds all Bluetooth permissions required on the
     * running API level.
     *  - API 31+ (Android 12): BLUETOOTH_SCAN + BLUETOOTH_CONNECT
     *  - API 30 and below:     BLUETOOTH
     *
     * Call this before any Bluetooth operation that the OS may reject, then
     * still wrap the call in try/catch SecurityException to handle the race
     * where the user revokes a permission between the check and the call.
     */
    private fun hasBluetoothPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }

    // ══════════════════════════════════
    //  PUBLIC STATE FLOWS
    // ══════════════════════════════════

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    private val _receivedBioSignal = MutableStateFlow<BioSignal?>(null)
    val receivedBioSignal: StateFlow<BioSignal?> = _receivedBioSignal.asStateFlow()

    private val _receivedSensorData = MutableStateFlow<SensorData?>(null)
    val receivedSensorData: StateFlow<SensorData?> = _receivedSensorData.asStateFlow()

    // ══════════════════════════════════
    //  PRIVATE STATE
    // ══════════════════════════════════

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isDiscovering = false
    private val discoveredAddresses = mutableSetOf<String>()
    var connectedDeviceName: String? = null
        private set

    // ══════════════════════════════════
    //  DISCOVERY RECEIVER
    // ══════════════════════════════════

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    device?.let {
                        if (!discoveredAddresses.contains(it.address)) {
                            discoveredAddresses.add(it.address)
                            _discoveredDevices.value = _discoveredDevices.value + it

                            // device.name requires BLUETOOTH_CONNECT on API 31+
                            val name = try {
                                it.name ?: "Unknown"
                            } catch (e: SecurityException) {
                                Log.w(TAG, "Permission denied reading device name: ${e.message}")
                                "Unknown"
                            }
                            Log.d(TAG, "📱 Found: $name [${it.address}]")
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "🔍 Discovery finished")
                    isDiscovering = false
                    _connectionMessage.value =
                        "Discovery complete. Found ${_discoveredDevices.value.size} devices."
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "🔍 Discovery started")
                    isDiscovering = true
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            Log.d(TAG, "✅ Receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register receiver: ${e.message}")
        }
    }

    // ══════════════════════════════════
    //  DISCOVERY
    // ══════════════════════════════════

    fun startDiscovery(): Boolean {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "❌ Bluetooth permission not granted")
            _connectionMessage.value = "Bluetooth permission required"
            return false
        }

        val adapter = bluetoothAdapter ?: run {
            Log.e(TAG, "❌ Bluetooth adapter is null")
            _connectionMessage.value = "Bluetooth not available"
            return false
        }

        return try {
            if (!adapter.isEnabled) {
                Log.e(TAG, "❌ Bluetooth is disabled")
                _connectionMessage.value = "Please enable Bluetooth"
                return false
            }

            discoveredAddresses.clear()
            _discoveredDevices.value = emptyList()

            if (adapter.isDiscovering) adapter.cancelDiscovery()

            val started = adapter.startDiscovery()
            if (started) {
                _connectionMessage.value = "Scanning for devices..."
                Log.d(TAG, "✅ Discovery started")
                scope.launch {
                    delay(DISCOVERY_TIMEOUT_MS)
                    if (isDiscovering) stopDiscovery()
                }
            } else {
                _connectionMessage.value = "Failed to start discovery"
                Log.e(TAG, "❌ Failed to start discovery")
            }
            started
        } catch (e: SecurityException) {
            // Catches any race where permission is revoked after our check above
            Log.e(TAG, "❌ Security exception during discovery: ${e.message}")
            _connectionMessage.value = "Bluetooth permission denied"
            false
        }
    }

    fun stopDiscovery() {
        if (!hasBluetoothPermission()) return
        try {
            bluetoothAdapter?.cancelDiscovery()
            isDiscovering = false
            Log.d(TAG, "⏹️ Discovery stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied stopping discovery: ${e.message}")
        }
    }

    // ══════════════════════════════════
    //  CONNECTION
    // ══════════════════════════════════

    fun connectToESP32(macAddress: String) {
        scope.launch {
            if (!hasBluetoothPermission()) {
                Log.e(TAG, "❌ Bluetooth permission not granted")
                _connectionMessage.value = "Bluetooth permission required"
                return@launch
            }

            try {
                Log.d(TAG, "🔗 Connecting to ESP32: $macAddress")
                _connectionMessage.value = "Connecting..."

                // cancelDiscovery requires BLUETOOTH_SCAN on API 31+;
                // safe here because hasBluetoothPermission() passed above.
                bluetoothAdapter?.cancelDiscovery()

                val device = bluetoothAdapter?.getRemoteDevice(macAddress)
                if (device == null) {
                    Log.e(TAG, "❌ Device not found: $macAddress")
                    _isConnected.value = false
                    _connectionMessage.value = "Device not found"
                    return@launch
                }

                // device.name requires BLUETOOTH_CONNECT on API 31+
                val deviceName = try {
                    device.name ?: "Unknown"
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission denied reading device name: ${e.message}")
                    "Unknown"
                }
                Log.d(TAG, "📱 Device: $deviceName")
                connectedDeviceName = deviceName

                closeConnection()

                // Give the adapter time to fully stop discovery before opening a socket.
                // Without this delay ESP32 SPP connections fail intermittently.
                delay(500)

                Log.d(TAG, "🔧 Creating RFCOMM socket...")
                // createRfcommSocketToServiceRecord requires BLUETOOTH_CONNECT on API 31+
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                Log.d(TAG, "🔧 Connecting socket...")
                withTimeout(12_000) {
                    bluetoothSocket?.connect()
                }

                inputStream  = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                _isConnected.value = true
                _connectionMessage.value = "Connected to $deviceName!"
                Log.d(TAG, "✅ Connected to ESP32!")

                startReading()

            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "❌ Connection timeout")
                _isConnected.value = false
                _connectionMessage.value = "Connection timeout. Try again."
                closeConnection()
            } catch (e: IOException) {
                Log.e(TAG, "❌ Connection failed: ${e.message}")
                _isConnected.value = false
                _connectionMessage.value = "Connection failed: ${e.message}"
                closeConnection()
                tryFallbackConnection(macAddress)
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission denied during connection: ${e.message}")
                _isConnected.value = false
                _connectionMessage.value = "Bluetooth permission denied"
                closeConnection()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Unexpected error: ${e.message}")
                _isConnected.value = false
                _connectionMessage.value = "Error: ${e.message}"
                closeConnection()
            }
        }
    }

    /**
     * Fallback when the UUID-based connect fails.
     *
     * Uses the hidden `createRfcommSocket(channel)` API and tries RFCOMM
     * channels 1, 2, and 3 in order — ESP32 SPP almost always lands on one
     * of these. For each channel we:
     *   1. Create a fresh socket (the previous one must be closed first).
     *   2. Wait 300 ms so the radio has settled.
     *   3. Connect with a 10-second timeout.
     *   4. Perform a non-blocking peek read to confirm the socket is truly
     *      open — a "read ret: -1" here means the channel is wrong, so we
     *      close and try the next one.
     */
    private suspend fun tryFallbackConnection(macAddress: String) {
        if (!hasBluetoothPermission()) return

        val device = try {
            bluetoothAdapter?.getRemoteDevice(macAddress)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied in fallback: ${e.message}")
            _connectionMessage.value = "Bluetooth permission denied"
            return
        } ?: return

        val createRfcommSocket = try {
            device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "❌ Reflection method unavailable: ${e.message}")
            _connectionMessage.value = "Connection failed. Make sure ESP32 is on."
            return
        }

        for (channel in 1..3) {
            Log.d(TAG, "🔄 Fallback: trying RFCOMM channel $channel…")
            _connectionMessage.value = "Retrying (channel $channel)…"

            closeConnection()           // ensure previous socket is fully closed
            delay(300)                  // let the radio settle between attempts

            try {
                bluetoothSocket = createRfcommSocket.invoke(device, channel) as BluetoothSocket

                withTimeout(10_000) {
                    bluetoothSocket?.connect()
                }

                // Validate: a connected-but-dead socket returns -1 immediately.
                // Read 0 bytes with a tiny timeout just to exercise the stream.
                val probe = ByteArray(1)
                bluetoothSocket?.inputStream?.let { stream ->
                    // available() is non-blocking; if it throws the socket is broken
                    stream.available()
                }

                // Socket looks healthy — commit streams and start reading
                inputStream  = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                _isConnected.value = true
                _connectionMessage.value = "Connected (channel $channel)!"
                Log.d(TAG, "✅ Fallback connected on channel $channel!")

                startReading()
                return                  // success — stop trying further channels

            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission denied on channel $channel: ${e.message}")
                _connectionMessage.value = "Bluetooth permission denied"
                closeConnection()
                return                  // no point retrying if permission is gone
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "⏱ Timeout on channel $channel, trying next…")
                closeConnection()
            } catch (e: IOException) {
                Log.w(TAG, "⚠️ IO error on channel $channel: ${e.message}, trying next…")
                closeConnection()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error on channel $channel: ${e.message}, trying next…")
                closeConnection()
            }
        }

        // All channels exhausted
        Log.e(TAG, "❌ All fallback channels failed for $macAddress")
        _isConnected.value = false
        _connectionMessage.value = "Connection failed. Make sure ESP32 is paired and on."
    }

    fun disconnect() {
        connectionJob?.cancel()
        readJob?.cancel()
        closeConnection()
        _connectionMessage.value = "Disconnected"
        Log.d(TAG, "🔌 Disconnected")
    }

    private fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
        inputStream = null
        outputStream = null
        bluetoothSocket = null
        _isConnected.value = false
    }

    // ══════════════════════════════════
    //  DATA READING
    // ══════════════════════════════════

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(READ_BUFFER_SIZE)
            val stringBuilder = StringBuilder()
            Log.d(TAG, "📖 Started reading data...")

            while (isActive && _isConnected.value) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1

                    if (bytesRead > 0) {
                        stringBuilder.append(String(buffer, 0, bytesRead))
                        var newlineIndex: Int
                        while (stringBuilder.indexOf("\n").also { newlineIndex = it } != -1) {
                            val line = stringBuilder.substring(0, newlineIndex).trim()
                            stringBuilder.delete(0, newlineIndex + 1)
                            if (line.isNotEmpty()) processReceivedData(line)
                        }
                    } else if (bytesRead == -1) {
                        Log.e(TAG, "❌ End of stream - connection lost")
                        _connectionMessage.value = "Connection lost"
                        closeConnection()
                        break
                    }
                } catch (e: IOException) {
                    if (_isConnected.value) {
                        Log.e(TAG, "❌ Read error: ${e.message}")
                        _connectionMessage.value = "Read error"
                        closeConnection()
                    }
                    break
                }
            }
            Log.d(TAG, "📖 Stopped reading")
        }
    }

    private fun processReceivedData(line: String) {
        Log.v(TAG, "📩 Received: $line")
        when {
            line.startsWith("EEG:") || line.startsWith("BIO:") || line.contains("|EOG:") ->
                parseBioSignalData(line)?.let { _receivedBioSignal.value = it }

            line.startsWith("SENSOR:") || line.startsWith("DIST:") ->
                parseSensorData(line)?.let { _receivedSensorData.value = it }

            line.startsWith("STATUS:") ->
                _connectionMessage.value = line.removePrefix("STATUS:")

            line.startsWith("ACK:") ->
                Log.d(TAG, "✅ Command acknowledged: ${line.removePrefix("ACK:")}")

            line.contains(",") && !line.contains(":") ->
                parseLegacyBioSignalData(line)?.let { _receivedBioSignal.value = it }

            else -> Log.d(TAG, "📨 Unknown data: $line")
        }
    }

    // ════════════════════════════════════════
    //  PARSE BIO SIGNAL DATA - EEG + EOG
    // ════════════════════════════════════════

    private fun parseBioSignalData(data: String): BioSignal? {
        return try {
            val parts = data.trim().split("|")
            when {
                parts.size >= 2          -> parseNewFormatBioSignal(parts)
                data.startsWith("EEG:") -> parseEegOnlyData(data)
                else                     -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse bio signal: ${e.message}")
            null
        }
    }

    private fun parseNewFormatBioSignal(parts: List<String>): BioSignal {
        val eegData = parts.getOrNull(0)?.removePrefix("EEG:")?.split(",") ?: listOf()
        val eegSignal = EEGSignal(
            delta       = eegData.getOrNull(0)?.toFloatOrNull() ?: 0f,
            theta       = eegData.getOrNull(1)?.toFloatOrNull() ?: 0f,
            alpha       = eegData.getOrNull(2)?.toFloatOrNull() ?: 0f,
            beta        = eegData.getOrNull(3)?.toFloatOrNull() ?: 0f,
            gamma       = eegData.getOrNull(4)?.toFloatOrNull() ?: 0f,
            snr         = eegData.getOrNull(5)?.toFloatOrNull() ?: 0f,
            mentalState = MentalState.fromCode(eegData.getOrNull(6)?.toIntOrNull() ?: 0),
            isStable    = true,
            hasArtifact = false
        )

        val eogData       = parts.getOrNull(1)?.removePrefix("EOG:")?.split(",") ?: listOf()
        val blinkDetected = eogData.getOrNull(1)?.toIntOrNull() == 1
        val saccadeCode   = eogData.getOrNull(2)?.toIntOrNull() ?: 0
        val eogSignal = EOGSignal(
            amplitude         = eogData.getOrNull(0)?.toFloatOrNull() ?: 0f,
            isBlinkDetected   = blinkDetected,
            blinkStrength     = if (blinkDetected) eogData.getOrNull(0)?.toFloatOrNull() ?: 0f else 0f,
            isSaccadeDetected = saccadeCode != 0,
            saccadeDirection  = SaccadeDirection.fromCode(saccadeCode),
            eyePosition       = eogData.getOrNull(3)?.toFloatOrNull() ?: 0f,
            eyeOpenness       = eogData.getOrNull(4)?.toFloatOrNull() ?: 1f,
            snr               = eogData.getOrNull(5)?.toFloatOrNull() ?: 0f,
            isStable          = true
        )

        return BioSignal.fromEEGAndEOG(eegSignal, eogSignal)
    }

    private fun parseEegOnlyData(data: String): BioSignal {
        val values = data.removePrefix("EEG:").split(",")
        val eegSignal = EEGSignal(
            delta    = values.getOrNull(0)?.toFloatOrNull() ?: 0f,
            theta    = values.getOrNull(1)?.toFloatOrNull() ?: 0f,
            alpha    = values.getOrNull(2)?.toFloatOrNull() ?: 0f,
            beta     = values.getOrNull(3)?.toFloatOrNull() ?: 0f,
            gamma    = values.getOrNull(4)?.toFloatOrNull() ?: 0f,
            isStable = true
        )
        return BioSignal.fromEEGAndEOG(eegSignal, EOGSignal())
    }

    private fun parseLegacyBioSignalData(data: String): BioSignal? {
        return try {
            val values = data.split(",")
            if (values.size < 4) return null

            val eegSignal = EEGSignal(
                alpha    = values.getOrNull(0)?.toFloatOrNull() ?: 0f,
                beta     = values.getOrNull(1)?.toFloatOrNull() ?: 0f,
                theta    = values.getOrNull(2)?.toFloatOrNull() ?: 0f,
                gamma    = values.getOrNull(3)?.toFloatOrNull() ?: 0f,
                isStable = true
            )
            val eogSignal = if (values.size >= 7) {
                EOGSignal(
                    amplitude        = values.getOrNull(4)?.toFloatOrNull() ?: 0f,
                    isBlinkDetected  = values.getOrNull(5)?.toIntOrNull() == 1,
                    saccadeDirection = SaccadeDirection.fromCode(values.getOrNull(6)?.toIntOrNull() ?: 0),
                    isStable         = true
                )
            } else EOGSignal()

            BioSignal.fromEEGAndEOG(eegSignal, eogSignal)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse legacy bio signal: ${e.message}")
            null
        }
    }

    // ════════════════════════════════════════
    //  PARSE SENSOR DATA
    // ════════════════════════════════════════

    private fun parseSensorData(data: String): SensorData? {
        return try {
            val values = data
                .removePrefix("SENSOR:")
                .removePrefix("DIST:")
                .split(",")
            SensorData(
                distanceFront = values.getOrNull(0)?.toFloatOrNull() ?: 0f,
                distanceLeft  = values.getOrNull(1)?.toFloatOrNull() ?: 0f,
                distanceRight = values.getOrNull(2)?.toFloatOrNull() ?: 0f,
                distanceRear  = values.getOrNull(3)?.toFloatOrNull() ?: 0f,
                batteryLevel  = values.getOrNull(4)?.toFloatOrNull() ?: 100f,
                speed         = values.getOrNull(5)?.toFloatOrNull() ?: 0f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse sensor data: ${e.message}")
            null
        }
    }

    // ══════════════════════════════════
    //  SEND DATA
    // ══════════════════════════════════

    fun sendData(data: ByteArray): Boolean {
        if (!_isConnected.value) {
            Log.w(TAG, "⚠️ Not connected - cannot send data")
            return false
        }
        return try {
            outputStream?.write(data)
            outputStream?.flush()
            Log.d(TAG, "📤 Sent: ${String(data).trim()}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "❌ Send failed: ${e.message}")
            _connectionMessage.value = "Send failed"
            false
        }
    }

    fun sendCommand(command: String): Boolean = sendData("$command\n".toByteArray())

    fun sendCommand(command: Command): Boolean {
        val speedPercent  = (command.value * 100).toInt()
        val commandString = "CMD:${command.type.name}:$speedPercent:${command.confidence}"
        return sendCommand(commandString)
    }

    // ══════════════════════════════════
    //  PAIRED DEVICES
    // ══════════════════════════════════

    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "❌ Bluetooth permission not granted")
            return emptyList()
        }
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied getting paired devices: ${e.message}")
            emptyList()
        }
    }

    fun getPairedESP32Devices(): List<BluetoothDevice> =
        getPairedDevices().filter { device ->
            val name = try { device.name ?: "" } catch (e: SecurityException) { "" }
            name.contains("ESP32",      ignoreCase = true) ||
                    name.contains("NeuroDrive", ignoreCase = true) ||
                    name.contains("SmartCar",   ignoreCase = true) ||
                    name.contains("HC-",        ignoreCase = true)
        }

    // ══════════════════════════════════
    //  CLEANUP
    // ══════════════════════════════════

    fun cleanup() {
        try {
            stopDiscovery()
            disconnect()
            context.unregisterReceiver(discoveryReceiver)
            scope.cancel()
            Log.d(TAG, "🧹 BluetoothClassicManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }
}/*
package com.neurodrive.smartcar.domain

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.neurodrive.smartcar.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothClassicManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "BTClassicManager"
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val CONNECT_TIMEOUT_MS = 15000L
        private const val READ_BUFFER_SIZE = 1024
        private const val DISCOVERY_TIMEOUT_MS = 30000L
    }

    // ══════════════════════════════════════════════════════════════
    //  PERMISSION HELPER
    // ══════════════════════════════════════════════════════════════

    /**
     * Check if app has required Bluetooth permissions for current API level
     * - API 31+ (Android 12): BLUETOOTH_SCAN + BLUETOOTH_CONNECT
     * - API 30 and below: BLUETOOTH
     */
    private fun hasBluetoothPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }

    // ══════════════════════════════════════════════════════════════
    //  PUBLIC STATE FLOWS
    // ══════════════════════════════════════════════════════════════

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    // Raw signal data
    private val _rawEegValue = MutableStateFlow(0f)
    val rawEegValue: StateFlow<Float> = _rawEegValue.asStateFlow()

    private val _rawEogValue = MutableStateFlow(0f)
    val rawEogValue: StateFlow<Float> = _rawEogValue.asStateFlow()

    // Signal quality
    private val _signalQuality = MutableStateFlow(SignalQuality.NO_SIGNAL)
    val signalQuality: StateFlow<SignalQuality> = _signalQuality.asStateFlow()

    private val _batteryPercent = MutableStateFlow(-1)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    // ══════════════════════════════════════════════════════════════
    //  PRIVATE STATE
    // ══════════════════════════════════════════════════════════════

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        manager.adapter
    }

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var connectionJob: Job? = null
    private var readJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var isDiscovering = false
    private val discoveredAddresses = mutableSetOf<String>()
    var connectedDeviceName: String? = null
        private set

    // ══════════════════════════════════════════════════════════════
    //  DISCOVERY RECEIVER
    // ══════════════════════════════════════════════════════════════

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(
                                BluetoothDevice.EXTRA_DEVICE,
                                BluetoothDevice::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }

                    device?.let {
                        if (!discoveredAddresses.contains(it.address)) {
                            discoveredAddresses.add(it.address)
                            _discoveredDevices.value = _discoveredDevices.value + it

                            val name = try {
                                it.name ?: "Unknown"
                            } catch (e: SecurityException) {
                                "Unknown"
                            }
                            Log.d(TAG, "📱 Found: $name [${it.address}]")
                        }
                    }
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "🔍 Discovery finished — ${_discoveredDevices.value.size} devices found")
                    isDiscovering = false
                    _connectionMessage.value = "Discovery complete"
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "🔍 Discovery started")
                    isDiscovering = true
                }
            }
        }
    }

    init {
        registerReceiver()
    }

    private fun registerReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            Log.d(TAG, "✅ Discovery receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register receiver: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DISCOVERY
    // ══════════════════════════════════════════════════════════════

    fun startDiscovery(): Boolean {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "❌ Bluetooth permission not granted")
            _connectionMessage.value = "Bluetooth permission required"
            return false
        }

        val adapter = bluetoothAdapter ?: run {
            Log.e(TAG, "❌ Bluetooth adapter is null")
            _connectionMessage.value = "Bluetooth not available"
            return false
        }

        return try {
            if (!adapter.isEnabled) {
                Log.e(TAG, "❌ Bluetooth is disabled")
                _connectionMessage.value = "Please enable Bluetooth"
                return false
            }

            discoveredAddresses.clear()
            _discoveredDevices.value = emptyList()

            if (adapter.isDiscovering) adapter.cancelDiscovery()

            val started = adapter.startDiscovery()
            if (started) {
                _connectionMessage.value = "🔍 Scanning..."
                Log.d(TAG, "✅ Discovery started")

                // Auto-stop after timeout
                scope.launch {
                    delay(DISCOVERY_TIMEOUT_MS)
                    if (isDiscovering) stopDiscovery()
                }
            } else {
                _connectionMessage.value = "Failed to start discovery"
                Log.e(TAG, "❌ Failed to start discovery")
            }
            started
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Security exception: ${e.message}")
            _connectionMessage.value = "Bluetooth permission denied"
            false
        }
    }

    fun stopDiscovery() {
        if (!hasBluetoothPermission()) return
        try {
            bluetoothAdapter?.cancelDiscovery()
            isDiscovering = false
            Log.d(TAG, "⏹️ Discovery stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied: ${e.message}")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  CONNECTION
    // ══════════════════════════════════════════════════════════════

    fun connectToDevice(macAddress: String) {
        scope.launch {
            if (!hasBluetoothPermission()) {
                Log.e(TAG, "❌ Bluetooth permission not granted")
                _connectionMessage.value = "Bluetooth permission required"
                return@launch
            }

            try {
                Log.d(TAG, "🔗 Connecting to: $macAddress")
                _connectionMessage.value = "Connecting..."

                bluetoothAdapter?.cancelDiscovery()

                val device = bluetoothAdapter?.getRemoteDevice(macAddress)
                if (device == null) {
                    Log.e(TAG, "❌ Device not found: $macAddress")
                    _isConnected.value = false
                    _connectionMessage.value = "Device not found"
                    return@launch
                }

                val deviceName = try {
                    device.name ?: "Unknown"
                } catch (e: SecurityException) {
                    "Unknown"
                }
                Log.d(TAG, "📱 Connecting to: $deviceName")
                connectedDeviceName = deviceName

                closeConnection()
                delay(500)  // Let radio settle

                Log.d(TAG, "🔧 Creating socket...")
                bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                Log.d(TAG, "🔧 Connecting socket...")
                withTimeout(12_000) {
                    bluetoothSocket?.connect()
                }

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                _isConnected.value = true
                _connectionMessage.value = "✅ Connected to $deviceName"
                Log.d(TAG, "✅ Connected!")

                startReading()

            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "❌ Connection timeout")
                _isConnected.value = false
                _connectionMessage.value = "Connection timeout"
                closeConnection()
                tryFallbackConnection(macAddress)
            } catch (e: IOException) {
                Log.e(TAG, "❌ Connection failed: ${e.message}")
                _isConnected.value = false
                _connectionMessage.value = "Connection failed"
                closeConnection()
                tryFallbackConnection(macAddress)
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission denied: ${e.message}")
                _isConnected.value = false
                _connectionMessage.value = "Permission denied"
                closeConnection()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}")
                _isConnected.value = false
                _connectionMessage.value = "Error: ${e.message}"
                closeConnection()
            }
        }
    }

    private suspend fun tryFallbackConnection(macAddress: String) {
        if (!hasBluetoothPermission()) return

        val device = try {
            bluetoothAdapter?.getRemoteDevice(macAddress)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Permission denied in fallback: ${e.message}")
            return
        } ?: return

        val createRfcommSocket = try {
            device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "❌ Reflection failed: ${e.message}")
            _connectionMessage.value = "Connection failed"
            return
        }

        for (channel in 1..3) {
            Log.d(TAG, "🔄 Trying channel $channel...")
            _connectionMessage.value = "Retrying (channel $channel)..."

            closeConnection()
            delay(300)

            try {
                bluetoothSocket = createRfcommSocket.invoke(device, channel) as BluetoothSocket

                withTimeout(10_000) {
                    bluetoothSocket?.connect()
                }

                bluetoothSocket?.inputStream?.available()

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                _isConnected.value = true
                _connectionMessage.value = "✅ Connected (channel $channel)"
                Log.d(TAG, "✅ Connected on channel $channel!")

                startReading()
                return

            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Permission denied on channel $channel")
                return
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "⏱ Timeout on channel $channel")
                closeConnection()
            } catch (e: IOException) {
                Log.w(TAG, "⚠️ IO error on channel $channel")
                closeConnection()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error on channel $channel")
                closeConnection()
            }
        }

        Log.e(TAG, "❌ All fallback channels failed")
        _isConnected.value = false
        _connectionMessage.value = "Connection failed"
    }

    fun disconnect() {
        connectionJob?.cancel()
        readJob?.cancel()
        closeConnection()
        _connectionMessage.value = "🔌 Disconnected"
        Log.d(TAG, "🔌 Disconnected")
    }

    private fun closeConnection() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing: ${e.message}")
        }
        inputStream = null
        outputStream = null
        bluetoothSocket = null
        _isConnected.value = false
    }

    // ══════════════════════════════════════════════════════════════
    //  DATA READING
    // ══════════════════════════════════════════════════════════════

    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(READ_BUFFER_SIZE)
            val stringBuilder = StringBuilder()
            Log.d(TAG, "📖 Started reading data...")

            while (isActive && _isConnected.value) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1

                    if (bytesRead > 0) {
                        stringBuilder.append(String(buffer, 0, bytesRead))
                        var newlineIndex: Int
                        while (stringBuilder.indexOf("\n").also { newlineIndex = it } != -1) {
                            val line = stringBuilder.substring(0, newlineIndex).trim()
                            stringBuilder.delete(0, newlineIndex + 1)
                            if (line.isNotEmpty()) processReceivedData(line)
                        }
                    } else if (bytesRead == -1) {
                        Log.e(TAG, "❌ Connection lost")
                        _connectionMessage.value = "Connection lost"
                        closeConnection()
                        break
                    }
                } catch (e: IOException) {
                    if (_isConnected.value) {
                        Log.e(TAG, "❌ Read error: ${e.message}")
                        _connectionMessage.value = "Read error"
                        closeConnection()
                    }
                    break
                }
            }
            Log.d(TAG, "📖 Stopped reading")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  PARSE RECEIVED DATA
    // ══════════════════════════════════════════════════════════════

    private fun processReceivedData(line: String) {
        Log.v(TAG, "📩 Received: $line")
        when {
            // EEG raw value: "EEG:123.45"
            line.startsWith("EEG:") -> {
                val value = line.removePrefix("EEG:").toFloatOrNull() ?: 0f
                _rawEegValue.value = value
                Log.v(TAG, "📊 EEG: ${value.toInt()} µV")
            }

            // EOG raw value: "EOG:456.78"
            line.startsWith("EOG:") -> {
                val value = line.removePrefix("EOG:").toFloatOrNull() ?: 0f
                _rawEogValue.value = value
                Log.v(TAG, "👁️ EOG: ${value.toInt()} µV")
            }

            // Battery: "BAT:85"
            line.startsWith("BAT:") -> {
                val battery = line.removePrefix("BAT:").toIntOrNull() ?: -1
                _batteryPercent.value = battery.coerceIn(0, 100)
                Log.v(TAG, "🔋 Battery: $battery%")
            }

            // Signal quality: "QUALITY:GOOD"
            line.startsWith("QUALITY:") -> {
                val quality = line.removePrefix("QUALITY:").uppercase()
                _signalQuality.value = when (quality) {
                    "EXCELLENT" -> SignalQuality.EXCELLENT
                    "GOOD" -> SignalQuality.GOOD
                    "FAIR" -> SignalQuality.FAIR
                    "POOR" -> SignalQuality.POOR
                    else -> SignalQuality.NO_SIGNAL
                }
                Log.v(TAG, "📶 Quality: $quality")
            }

            // Status message: "STATUS:Ready"
            line.startsWith("STATUS:") -> {
                val message = line.removePrefix("STATUS:")
                _connectionMessage.value = message
                Log.d(TAG, "📨 Status: $message")
            }

            // ACK: "ACK:Command received"
            line.startsWith("ACK:") -> {
                Log.d(TAG, "✅ ACK: ${line.removePrefix("ACK:")}")
            }

            else -> Log.d(TAG, "📨 Unknown: $line")
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  SEND DATA
    // ══════════════════════════════════════════════════════════════

    fun sendData(data: ByteArray): Boolean {
        if (!_isConnected.value) {
            Log.w(TAG, "⚠️ Not connected")
            return false
        }
        return try {
            outputStream?.write(data)
            outputStream?.flush()
            Log.d(TAG, "📤 Sent: ${String(data).trim()}")
            true
        } catch (e: IOException) {
            Log.e(TAG, "❌ Send failed: ${e.message}")
            _connectionMessage.value = "Send failed"
            false
        }
    }

    fun sendCommand(command: String): Boolean {
        return sendData("$command\n".toByteArray())
    }

    // ══════════════════════════════════════════════════════════════
    //  PAIRED DEVICES
    // ══════════════════════════════════════════════════════════════

    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) {
            Log.e(TAG, "❌ Bluetooth permission not granted")
            return emptyList()
        }
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied: ${e.message}")
            emptyList()
        }
    }

    fun getPairedESP32Devices(): List<BluetoothDevice> =
        getPairedDevices().filter { device ->
            val name = try { device.name ?: "" } catch (e: SecurityException) { "" }
            name.contains("ESP32", ignoreCase = true) ||
                    name.contains("NeuroDrive", ignoreCase = true) ||
                    name.contains("SmartCar", ignoreCase = true) ||
                    name.contains("HC-", ignoreCase = true)
        }

    // ══════════════════════════════════════════════════════════════
    //  CLEANUP
    // ══════════════════════════════════════════════════════════════

    fun cleanup() {
        try {
            stopDiscovery()
            disconnect()
            context.unregisterReceiver(discoveryReceiver)
            scope.cancel()
            Log.d(TAG, "🧹 Cleanup complete")
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }
}*/