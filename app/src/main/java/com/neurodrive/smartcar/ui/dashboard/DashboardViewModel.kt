package com.neurodrive.smartcar.ui.dashboard

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.data.model.BioSignal
import com.neurodrive.smartcar.data.model.Command
import com.neurodrive.smartcar.data.model.CommandType
import com.neurodrive.smartcar.data.repository.BioSignalRepository
import com.neurodrive.smartcar.data.repository.BioSignalRepository.SignalQualityLevel
import com.neurodrive.smartcar.domain.BleManager
import com.neurodrive.smartcar.domain.BluetoothClassicManager
import com.neurodrive.smartcar.domain.LockStatus
import com.neurodrive.smartcar.domain.SignalProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val bluetoothClassicManager: BluetoothClassicManager,
    private val bioSignalRepository: BioSignalRepository,
    private val signalProcessor: SignalProcessor
) : ViewModel() {

    // ══════════════════════════════════
    //  BLE STATE (Headband)
    // ══════════════════════════════════

    val bleConnected: StateFlow<Boolean> = bleManager.connectionState

    // ══════════════════════════════════
    //  CLASSIC BT STATE (ESP32 Car)
    // ══════════════════════════════════

    val btConnected: StateFlow<Boolean> = bluetoothClassicManager.isConnected
    val discoveredClassicDevices: StateFlow<List<BluetoothDevice>> =
        bluetoothClassicManager.discoveredDevices
    val btConnectionMessage: StateFlow<String?> = bluetoothClassicManager.connectionMessage



    // ══════════════════════════════════
    //  REAL-TIME GRAPH DATA - ADD THIS!
    // ══════════════════════════════════

    private val _eegGraphData = MutableStateFlow<List<Float>>(emptyList())
    val eegGraphData: StateFlow<List<Float>> = _eegGraphData.asStateFlow()

    private val _eogGraphData = MutableStateFlow<List<Float>>(emptyList())
    val eogGraphData: StateFlow<List<Float>> = _eogGraphData.asStateFlow()

    // Band power graphs (updated every buffer calculation)
    private val _alphaGraphData = MutableStateFlow<List<Float>>(emptyList())
    val alphaGraphData: StateFlow<List<Float>> = _alphaGraphData.asStateFlow()

    private val _betaGraphData = MutableStateFlow<List<Float>>(emptyList())
    val betaGraphData: StateFlow<List<Float>> = _betaGraphData.asStateFlow()

    private val _thetaGraphData = MutableStateFlow<List<Float>>(emptyList())
    val thetaGraphData: StateFlow<List<Float>> = _thetaGraphData.asStateFlow()

    private val _gammaGraphData = MutableStateFlow<List<Float>>(emptyList())
    val gammaGraphData: StateFlow<List<Float>> = _gammaGraphData.asStateFlow()

    private val maxGraphPoints = 500  // ~2 seconds at 256 Hz
    private val maxBandPowerPoints = 100  // Keep last 100 band power updates

    // ══════════════════════════════════
    //  INIT - OBSERVE DATA
    // ══════════════════════════════════

    init {
        observeBleSignals()
        observeClassicBtSignals()
        observeConnectionStates()
        observePendingCommands()
        observeFilteredSignalsForGraphing() // ← ADD THIS
    }

    /**
     * Observe filtered signals for real-time graphing
     */
    private fun observeFilteredSignalsForGraphing() {
        // Observe EEG state for band powers
        viewModelScope.launch {
            signalProcessor.eegState.collect { eeg ->
                if (eeg.isStable) {
                    // Update band power graphs
                    updateGraphData(_alphaGraphData, eeg.alpha, maxBandPowerPoints)
                    updateGraphData(_betaGraphData, eeg.beta, maxBandPowerPoints)
                    updateGraphData(_thetaGraphData, eeg.theta, maxBandPowerPoints)
                    updateGraphData(_gammaGraphData, eeg.gamma, maxBandPowerPoints)
                }

                // Always update raw filtered EEG value for waveform graph
                updateGraphData(_eegGraphData, eeg.filteredValue, maxGraphPoints)
            }
        }

        // Observe EOG state
        viewModelScope.launch {
            signalProcessor.eogState.collect { eog ->
                updateGraphData(_eogGraphData, eog.filteredValue, maxGraphPoints)
            }
        }
    }

    /**
     * Helper to update graph data with circular buffer behavior
     */
    private fun updateGraphData(
        flow: MutableStateFlow<List<Float>>,
        newValue: Float,
        maxPoints: Int
    ) {
        val current = flow.value.toMutableList()
        current.add(newValue)
        if (current.size > maxPoints) {
            current.removeAt(0)
        }
        flow.value = current
    }


    // ══════════════════════════════════
    //  SIGNAL STATE
    // ══════════════════════════════════

    val currentSignal: StateFlow<BioSignal?> = bioSignalRepository.currentSignal
    val signalHistory: StateFlow<List<BioSignal>> = bioSignalRepository.signalHistory
    val currentCommand: StateFlow<Command?> = bioSignalRepository.currentCommand
    val signalQuality: StateFlow<SignalQualityLevel> = bioSignalRepository.signalQuality

    // ══════════════════════════════════
    //  COMMAND LOCK STATE
    // ══════════════════════════════════

    val lockStatus: StateFlow<LockStatus> = signalProcessor.lockStatus

    // ══════════════════════════════════
    //  SCAN STATE
    // ══════════════════════════════════

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Add this flow that exposes BLE discovered devices
    val discoveredBleDevices: StateFlow<List<BluetoothDevice>> = bleManager.discoveredDevices

    // ══════════════════════════════════
    //  INIT - OBSERVE DATA
    // ══════════════════════════════════

    init {
        observeBleSignals()
        observeClassicBtSignals()
        observeConnectionStates()
        observePendingCommands()
    }

    /**
     * Observe BLE headband bio signals and process them
     */
    private fun observeBleSignals() {
        viewModelScope.launch {
            bleManager.bioSignals.collect { signal ->
                signal?.let {
                    // Process through repository (quality check, alerts, command classification)
                    val command = bioSignalRepository.processSignal(it)

                    // If command generated, send to ESP32
                    command?.let { cmd ->
                        sendCommandToESP32(cmd)
                    }
                }
            }
        }
    }

    fun connectBleDevice(device: BluetoothDevice) {
        bleManager.connectToDevice(device)
    }

    /**
     * Observe Classic BT bio signals (if ESP32 sends EEG data back)
     */
    private fun observeClassicBtSignals() {
        viewModelScope.launch {
            bluetoothClassicManager.receivedBioSignal.collect { signal ->
                signal?.let {
                    bioSignalRepository.processSignal(it)
                }
            }
        }
    }

    /**
     * Observe connection state changes and update repository
     */
    private fun observeConnectionStates() {
        viewModelScope.launch {
            bleManager.connectionState.collect { connected ->
                bioSignalRepository.updateBleConnectionState(connected)
            }
        }

        viewModelScope.launch {
            bluetoothClassicManager.isConnected.collect { connected ->
                bioSignalRepository.updateEsp32ConnectionState(connected)
            }
        }
    }

    /**
     * Observe pending commands from ControlScreen manual controls
     */
    private fun observePendingCommands() {
        viewModelScope.launch {
            bioSignalRepository.pendingCommand.collect { command ->
                command?.let {
                    sendCommandToESP32(it)
                    bioSignalRepository.clearPendingCommand()
                }
            }
        }
    }

    // ══════════════════════════════════
    //  LOCK CONTROL
    // ══════════════════════════════════

    /**
     * Force unlock the current command (emergency button)
     */
    fun forceUnlock() {
        signalProcessor.forceUnlock()
        _scanMessage.value = "Command unlocked"
    }

    /**
     * Check if command is currently locked
     */
    fun isCommandLocked(): Boolean = signalProcessor.isLocked()

    /**
     * Get the currently locked command
     */
    fun getLockedCommand(): CommandType = signalProcessor.getLockedCommand()

    /**
     * Get remaining lock time in seconds
     */
    fun getRemainingLockSeconds(): Int = signalProcessor.getRemainingLockSeconds()

    // ══════════════════════════════════
    //  SEND COMMAND TO ESP32
    // ══════════════════════════════════

    private fun sendCommandToESP32(command: Command) {
        if (btConnected.value) {
            // Use command.value (0-1 normalized) instead of command.speed
            val speedPercent = (command.value * 100).toInt()
            val commandString = "${command.type.name}:$speedPercent"
            bluetoothClassicManager.sendData("$commandString\n".toByteArray())
        }
    }

    // ══════════════════════════════════
    //  BLE SCANNING (Headband)
    // ══════════════════════════════════

    fun startBLEScan() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isScanning.value = true
                _scanMessage.value = "Scanning for BLE devices..."
                bleManager.startScan()
            } catch (e: Exception) {
                _errorMessage.value = "BLE scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    @Suppress("unused")
    fun stopBLEScan() {
        bleManager.stopScan()
        _isScanning.value = false
    }

    // ══════════════════════════════════
    //  CLASSIC BT SCANNING (ESP32)
    // ══════════════════════════════════

    fun startClassicDiscovery() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isScanning.value = true
                _scanMessage.value = "Scanning for ESP32 devices..."
                val started = bluetoothClassicManager.startDiscovery()
                if (!started) {
                    _errorMessage.value =
                        "Failed to start Bluetooth scan. Check Bluetooth & Location."
                }
            } catch (e: Exception) {
                _errorMessage.value = "Scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    // ══════════════════════════════════
    //  CONNECT / DISCONNECT
    // ══════════════════════════════════

    fun connectESP32(macAddress: String) {
        _scanMessage.value = "Connecting to ESP32..."
        bluetoothClassicManager.connectToESP32(macAddress)
    }

    fun disconnectESP32() {
        bluetoothClassicManager.disconnect()
        _scanMessage.value = "Disconnected from ESP32"
    }

    fun disconnectBle() {
        bleManager.disconnect()
        _scanMessage.value = "Disconnected from BLE headband"
    }

    // ══════════════════════════════════
    //  MESSAGES
    // ══════════════════════════════════

    fun setScanMessage(message: String) {
        _scanMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ══════════════════════════════════
    //  CLEANUP
    // ══════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        bluetoothClassicManager.stopDiscovery()
        bluetoothClassicManager.disconnect()
        bleManager.stopScan()
        signalProcessor.reset()
    }
}