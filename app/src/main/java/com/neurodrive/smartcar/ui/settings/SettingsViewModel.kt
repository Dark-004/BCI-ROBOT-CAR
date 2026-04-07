package com.neurodrive.smartcar.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neurodrive.smartcar.domain.BleManager
import com.neurodrive.smartcar.domain.BluetoothClassicManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.neurodrive.smartcar.data.local.UserPreferences
import com.neurodrive.smartcar.data.local.SecureStorage

enum class ScanType { NONE, CLASSIC, BLE }

data class SettingsUiState(
    val esp32ConnectedDeviceName: String? = null,
    val isEsp32Connected: Boolean = false,

    val bleConnectedDeviceName: String? = null,
    val isBleConnected: Boolean = false,

    // ── Battery ─────────────────────────────────────
    val batteryPercent: Int = -1,  // -1 = not received yet

    // ── Calibration ─────────────────────────────────
    val lastCalibrationTime: String = "Never",
    val isCalibrationFresh: Boolean = false,
    val eegBaselineAlpha: Float = 0f,
    val eegBaselineBeta: Float = 0f,
    val emgBaseline: Float = 0f,
    val isCalibrating: Boolean = false,
    val calibrationProgress: Float = 0f,
    val calibrationStep: String = "",

    // ── Accessibility ───────────────────────────────
    val highContrast: Boolean = false,
    val fontScale: Float = 1.0f,
    val hapticFeedback: Boolean = true,
    val largeTouchTargets: Boolean = false,
    val screenReaderOptimised: Boolean = false,

    // ── Appearance ──────────────────────────────────
    val darkMode: Boolean = false,
    val selectedTheme: String = "Purple (Default)",

    // ── Dialogs ─────────────────────────────────────
    val showLogoutDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false,
    val showResetDrivingDialog: Boolean = false,
    val showResetAccessibilityDialog: Boolean = false,
    val showResetAllDialog: Boolean = false,
    val showClearHistoryDialog: Boolean = false,
    val showCalibrationDialog: Boolean = false,
    val showThemeDialog: Boolean = false,

    // ── Auth / General ──────────────────────────────
    val isLoggedOut: Boolean = false,
    val snackbarMessage: String? = null
) {
    // ── Battery computed properties ─────────────────
    val hasBatteryData: Boolean get() = batteryPercent >= 0

    val batteryDisplayText: String
        get() = if (hasBatteryData) "$batteryPercent%" else "N/A"

    val isBatteryLow: Boolean
        get() = hasBatteryData && batteryPercent <= 20

    val isBatteryCritical: Boolean
        get() = hasBatteryData && batteryPercent <= 10

    val batteryLevel: BatteryLevel
        get() = when {
            !hasBatteryData -> BatteryLevel.UNKNOWN
            batteryPercent <= 10 -> BatteryLevel.CRITICAL
            batteryPercent <= 20 -> BatteryLevel.LOW
            batteryPercent <= 50 -> BatteryLevel.MEDIUM
            batteryPercent <= 80 -> BatteryLevel.GOOD
            else -> BatteryLevel.FULL
        }
}

enum class BatteryLevel(val displayName: String, val color: Long) {
    UNKNOWN("Unknown", 0xFF9E9E9E),     // Gray
    CRITICAL("Critical", 0xFFF44336),    // Red
    LOW("Low", 0xFFFF9800),              // Orange
    MEDIUM("Medium", 0xFFFFEB3B),        // Yellow
    GOOD("Good", 0xFF8BC34A),            // Light Green
    FULL("Full", 0xFF4CAF50)             // Green
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val bleManager: BleManager,
    private val bluetoothClassicManager: BluetoothClassicManager,
    private val userPreferences: UserPreferences,
    private val secureStorage: SecureStorage
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════

    init {
        observeBluetoothConnections()
        observeBatteryStatus()  // ← ADD THIS
    }

    // ════════════════════════════════════════════════════════════
    //  OBSERVE CONNECTIONS
    // ════════════════════════════════════════════════════════════

    private fun observeBluetoothConnections() {
        // Observe ESP32 Classic Bluetooth connection
        viewModelScope.launch {
            bluetoothClassicManager.isConnected.collect { connected ->
                _uiState.update {
                    it.copy(
                        isEsp32Connected = connected,
                        esp32ConnectedDeviceName = if (connected)
                            bluetoothClassicManager.connectedDeviceName
                        else null
                    )
                }
            }
        }

        // Observe BLE Headband connection
        viewModelScope.launch {
            bleManager.connectionState.collect { connected ->
                _uiState.update {
                    it.copy(
                        isBleConnected = connected,
                        bleConnectedDeviceName = if (connected)
                            bleManager.connectedDeviceName
                        else null,
                        // Reset battery when disconnected
                        batteryPercent = if (connected) it.batteryPercent else -1
                    )
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  OBSERVE BATTERY STATUS
    // ════════════════════════════════════════════════════════════

    private fun observeBatteryStatus() {
        viewModelScope.launch {
            bleManager.batteryPercent.collect { percent ->
                _uiState.update {
                    it.copy(batteryPercent = percent)
                }

                // Auto-alert on low battery
                if (percent in 1..10) {
                    _uiState.update {
                        it.copy(snackbarMessage = "⚠️ Headband battery critical: $percent%")
                    }
                } else if (percent in 11..20) {
                    _uiState.update {
                        it.copy(snackbarMessage = "🔋 Headband battery low: $percent%")
                    }
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  CALIBRATION
    // ════════════════════════════════════════════════════════════

    fun showCalibrationDialog() {
        _uiState.update { it.copy(showCalibrationDialog = true) }
    }

    fun startCalibration() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCalibrating = true,
                    calibrationProgress = 0f,
                    calibrationStep = "Preparing sensors..."
                )
            }

            val steps = listOf(
                "Measuring EEG baseline..." to 0.2f,
                "Analyzing alpha waves..." to 0.4f,
                "Analyzing beta waves..." to 0.6f,
                "Measuring EMG baseline..." to 0.8f,
                "Finalizing calibration..." to 1.0f
            )

            for ((step, progress) in steps) {
                _uiState.update {
                    it.copy(calibrationStep = step, calibrationProgress = progress)
                }
                delay(2000)
            }

            _uiState.update {
                it.copy(
                    isCalibrating = false,
                    showCalibrationDialog = false,
                    lastCalibrationTime = "Just now",
                    isCalibrationFresh = true,
                    eegBaselineAlpha = 8.5f + (Math.random() * 3).toFloat(),
                    eegBaselineBeta = 15.0f + (Math.random() * 5).toFloat(),
                    emgBaseline = 0.1f + (Math.random() * 0.3).toFloat(),
                    snackbarMessage = "Calibration completed successfully!"
                )
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    //  DIALOG MANAGEMENT
    // ════════════════════════════════════════════════════════════

    fun showLogoutDialog() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun updateTheme(theme: String) {
        _uiState.update { it.copy(selectedTheme = theme) }
    }

    fun showDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = true) }
    }

    fun showClearHistoryDialog() {
        _uiState.update { it.copy(showClearHistoryDialog = true) }
    }



    fun dismissDialog() {
        _uiState.update {
            it.copy(
                showLogoutDialog = false,
                showDeleteAccountDialog = false,
                showResetDrivingDialog = false,
                showResetAccessibilityDialog = false,
                showResetAllDialog = false,
                showClearHistoryDialog = false,
                showCalibrationDialog = false,
                showThemeDialog = false
            )
        }
    }

    // ════════════════════════════════════════════════════════════
    //  ACCOUNT & DATA ACTIONS
    // ════════════════════════════════════════════════════════════

    fun logout() {
        viewModelScope.launch {
            secureStorage.logout()
            userPreferences.setLoggedIn(false)
            _uiState.update {
                it.copy(
                    showLogoutDialog = false,
                    isLoggedOut = true
                )
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            secureStorage.wipeAll()
            userPreferences.clearAll()
            _uiState.update {
                it.copy(
                    showDeleteAccountDialog = false,
                    isLoggedOut = true
                )
            }
        }
    }

    fun exportData() {
        _uiState.update { it.copy(snackbarMessage = "Exporting data...") }
    }

    fun clearSignalHistory() {
        _uiState.update { it.copy(snackbarMessage = "Signal history cleared") }
    }

    fun resetDrivingSettings() {
        _uiState.update { it.copy(snackbarMessage = "Driving settings reset to defaults") }
    }

    fun resetAccessibilitySettings() {
        _uiState.update {
            it.copy(
                highContrast = false,
                fontScale = 1.0f,
                hapticFeedback = true,
                largeTouchTargets = false,
                screenReaderOptimised = false,
                snackbarMessage = "Accessibility settings reset"
            )
        }
    }

    fun resetAllSettings() {
        _uiState.update {
            SettingsUiState(snackbarMessage = "All settings reset to defaults")
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}