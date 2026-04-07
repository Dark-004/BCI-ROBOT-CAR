// ui/viewmodel/ScanViewModel.kt
package com.neurodrive.smartcar.ui.viewmodel

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import com.neurodrive.smartcar.domain.BleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val bleManager: BleManager
) : ViewModel() {

    val discoveredDevices = bleManager.discoveredDevices
    val connectionState = bleManager.connectionState
    val bioSignals = bleManager.bioSignals
    val isScanning = bleManager.isScanning  // ← Use this in UI

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        bleManager.connectToDevice(device)
    }

    fun disconnect() {
        bleManager.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}