package com.neurodrive.smartcar

import android.os.Build

/**
 * Central configuration object for NeuroDrive application
 * Contains app metadata, feature flags, and configuration constants
 */
object AppConfig {

    // ════════════════════════════════════════════════════════════════
    //  APP METADATA
    // ════════════════════════════════════════════════════════════════

    const val VERSION_NAME = "1.0.0"
    const val VERSION_CODE = 1
    const val APP_NAME = "NeuroDrive"
    const val PACKAGE_NAME = "com.neurodrive.smartcar"

    // Build info (NOT const - uses BuildConfig which is not compile-time constant)
    val BUILD_TYPE: String get() = BuildConfig.BUILD_TYPE  // "debug" or "release"
    val IS_DEBUG: Boolean get() = BuildConfig.DEBUG

    // ════════════════════════════════════════════════════════════════
    //  BLUETOOTH CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    // BLE Service & Characteristics UUIDs (Match ESP32 code)
    const val BLE_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    const val BLE_COMMAND_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26aa"
    const val BLE_STATUS_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26ab"
    const val BLE_EEG_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
    const val BLE_EOG_CHAR_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a9"

    // BLE Device Names (for filtering scan results)
    const val ESP32_DEVICE_NAME = "NeuroDrive-Control"
    const val EEG_HEADBAND_NAME = "NeuroDrive"  // Your EEG/EOG headband name

    // BLE Scan & Connection
    const val BLE_SCAN_DURATION_MS = 10_000L     // 10 seconds
    const val BLE_CONNECTION_TIMEOUT_MS = 15_000L // 15 seconds
    const val BLE_RECONNECT_DELAY_MS = 3_000L     // 3 seconds
    const val BLE_MAX_RECONNECT_ATTEMPTS = 5

    // ════════════════════════════════════════════════════════════════
    //  SIGNAL PROCESSING CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    // EEG Thresholds (match ESP32 settings)
    const val EEG_FOCUS_THRESHOLD = 60       // Activity > 60 = FORWARD
    const val EEG_RELAX_THRESHOLD = 15       // Activity < 15 = BACKWARD
    const val EEG_DROWSY_THRESHOLD = 5       // Activity < 5 = EMERGENCY

    // EOG Thresholds
    const val EOG_CENTER_VALUE = 1800        // Baseline (looking straight)
    const val EOG_LEFT_LIMIT = 1600          // Below this = LEFT
    const val EOG_RIGHT_LIMIT = 2000         // Above this = RIGHT
    const val EOG_DIRECTION_THRESHOLD = 0.3f // Threshold for left/right detection

    // Signal Quality
    const val MIN_SIGNAL_QUALITY = 0.5f      // 0.0 - 1.0 scale
    const val SNR_THRESHOLD_DB = 8.0f        // Minimum SNR in dB

    // Sampling Rates
    const val EEG_SAMPLE_RATE_HZ = 256
    const val EOG_SAMPLE_RATE_HZ = 75

    // ════════════════════════════════════════════════════════════════
    //  COMMAND CONTROL CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    const val COMMAND_COOLDOWN_MS = 2_000L       // 2 seconds between commands
    const val COMMAND_STABILITY_COUNT = 3        // Require 3 consecutive matches
    const val EOG_DEBOUNCE_MS = 500L             // 0.5 seconds for steering

    // Command Lock
    const val COMMAND_LOCK_DURATION_MS = 5_000L  // 5 seconds lock after command

    // ════════════════════════════════════════════════════════════════
    //  BATTERY CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    const val BATTERY_CRITICAL_PERCENT = 10      // Critical warning
    const val BATTERY_LOW_PERCENT = 20           // Low battery warning
    const val BATTERY_UPDATE_INTERVAL_MS = 5_000L // Update every 5 seconds

    // Battery time estimation (hours per 100%)
    const val BATTERY_FULL_RUNTIME_HOURS = 8.0f

    // ════════════════════════════════════════════════════════════════
    //  CALIBRATION CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    const val CALIBRATION_DURATION_MS = 30_000L  // 30 seconds
    const val CALIBRATION_VALIDITY_HOURS = 24    // Calibration valid for 24h
    const val CALIBRATION_MIN_SAMPLES = 100

    // Calibration Phases
    const val CALIBRATION_PHASE_RELAX = 10_000L  // 10s relaxed
    const val CALIBRATION_PHASE_FOCUS = 10_000L  // 10s focused
    const val CALIBRATION_PHASE_NEUTRAL = 10_000L // 10s neutral

    // ════════════════════════════════════════════════════════════════
    //  CAR CONTROL CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    // Speed Limits (0.0 - 1.0 scale)
    const val DEFAULT_MAX_SPEED = 0.7f
    const val MIN_SPEED = 0.0f
    const val MAX_SPEED = 1.0f
    const val SPEED_INCREMENT = 0.1f

    // Steering Sensitivity
    const val DEFAULT_STEERING_SENSITIVITY = 0.5f
    const val MIN_STEERING_SENSITIVITY = 0.1f
    const val MAX_STEERING_SENSITIVITY = 1.0f

    // Safety
    const val OBSTACLE_DETECTION_ENABLED = true
    const val MIN_OBSTACLE_DISTANCE_CM = 30      // Stop if obstacle < 30cm
    const val EMERGENCY_STOP_ENABLED = true

    // ════════════════════════════════════════════════════════════════
    //  DATA & PRIVACY CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    // Data Retention
    const val SIGNAL_HISTORY_MAX_DAYS = 30       // Keep last 30 days
    const val COMMAND_LOG_MAX_ENTRIES = 10_000   // Keep last 10k commands
    const val MAX_DATABASE_SIZE_MB = 100         // Max DB size

    // Privacy
    const val ANONYMIZE_EXPORTED_DATA = true
    const val ENABLE_CRASH_REPORTING_DEFAULT = false     // Set true for production
    const val ENABLE_ANALYTICS_DEFAULT = false           // Set true for production

    // Export
    const val EXPORT_FORMAT = "JSON"             // JSON or CSV
    const val EXPORT_INCLUDE_RAW_SIGNALS = false

    // ════════════════════════════════════════════════════════════════
    //  UI CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    // Theme
    const val DEFAULT_THEME = "Purple (Default)"
    val AVAILABLE_THEMES = listOf(
        "Purple (Default)",
        "Blue",
        "Green",
        "Orange",
        "Red",
        "Teal"
    )

    // Accessibility
    const val DEFAULT_FONT_SCALE = 1.0f
    const val MIN_FONT_SCALE = 0.8f
    const val MAX_FONT_SCALE = 1.5f
    const val DEFAULT_HIGH_CONTRAST = false
    const val DEFAULT_HAPTIC_FEEDBACK = true

    // Update Intervals
    const val UI_UPDATE_INTERVAL_MS = 100L       // Update UI every 100ms
    const val STATUS_UPDATE_INTERVAL_MS = 2_000L // Status every 2s
    const val SIGNAL_GRAPH_UPDATE_MS = 50L       // Graph at 20 FPS

    // ════════════════════════════════════════════════════════════════
    //  NETWORK & API CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    const val API_BASE_URL = "https://api.neurodrive.app/v1/"
    const val API_TIMEOUT_SECONDS = 30
    const val API_MAX_RETRIES = 3

    // URLs
    const val PRIVACY_POLICY_URL = "https://neurodrive.app/privacy"
    const val TERMS_OF_SERVICE_URL = "https://neurodrive.app/terms"
    const val SUPPORT_EMAIL = "support@neurodrive.app"
    const val FEEDBACK_URL = "https://neurodrive.app/feedback"
    const val GITHUB_REPO_URL = "https://github.com/neurodrive/smartcar"

    // ════════════════════════════════════════════════════════════════
    //  FEATURE FLAGS
    // ════════════════════════════════════════════════════════════════

    const val ENABLE_EEG_CONTROL = true
    const val ENABLE_EOG_CONTROL = true
    const val ENABLE_EMERGENCY_DETECTION = true
    const val ENABLE_VOICE_COMMANDS = false      // Future feature
    const val ENABLE_ML_PREDICTIONS = false      // Future feature
    const val ENABLE_CLOUD_SYNC = false          // Future feature
    const val ENABLE_MULTI_USER = false          // Future feature

    // Experimental Features (debug only) - NOT const, uses IS_DEBUG
    val ENABLE_RAW_SIGNAL_VIEW: Boolean get() = IS_DEBUG
    val ENABLE_ADVANCED_DIAGNOSTICS: Boolean get() = IS_DEBUG
    val ENABLE_DEVELOPER_MENU: Boolean get() = IS_DEBUG

    // ════════════════════════════════════════════════════════════════
    //  LOGGING CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    // NOT const - uses IS_DEBUG
    val LOG_LEVEL: String get() = if (IS_DEBUG) "DEBUG" else "INFO"
    val ENABLE_FILE_LOGGING: Boolean get() = IS_DEBUG

    const val MAX_LOG_FILE_SIZE_MB = 10
    const val LOG_RETENTION_DAYS = 7

    // Log Tags
    const val LOG_TAG_BLE = "NeuroDrive:BLE"
    const val LOG_TAG_SIGNAL = "NeuroDrive:Signal"
    const val LOG_TAG_COMMAND = "NeuroDrive:Command"
    const val LOG_TAG_CAR = "NeuroDrive:Car"
    const val LOG_TAG_UI = "NeuroDrive:UI"

    // ════════════════════════════════════════════════════════════════
    //  PREFERENCES KEYS
    // ════════════════════════════════════════════════════════════════

    object PrefsKeys {
        const val PREFS_NAME = "neurodrive_prefs"

        // User
        const val USER_ID = "user_id"
        const val USER_NAME = "user_name"
        const val IS_LOGGED_IN = "is_logged_in"

        // Calibration
        const val LAST_CALIBRATION_TIME = "last_calibration_time"
        const val EEG_ALPHA_BASELINE = "eeg_alpha_baseline"
        const val EEG_BETA_BASELINE = "eeg_beta_baseline"
        const val EOG_LEFT_BASELINE = "eog_left_baseline"
        const val EOG_RIGHT_BASELINE = "eog_right_baseline"

        // Settings
        const val MAX_SPEED = "max_speed"
        const val STEERING_SENSITIVITY = "steering_sensitivity"
        const val OBSTACLE_DISTANCE = "obstacle_distance"
        const val THEME = "theme"
        const val FONT_SCALE = "font_scale"
        const val HIGH_CONTRAST = "high_contrast"
        const val HAPTIC_FEEDBACK = "haptic_feedback"

        // BLE
        const val LAST_ESP32_ADDRESS = "last_esp32_address"
        const val LAST_EEG_ADDRESS = "last_eeg_address"
        const val AUTO_RECONNECT = "auto_reconnect"

        // Privacy
        const val ANALYTICS_ENABLED = "analytics_enabled"
        const val CRASH_REPORTING_ENABLED = "crash_reporting_enabled"

        // Onboarding
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val TUTORIAL_SHOWN = "tutorial_shown"
    }

    // ════════════════════════════════════════════════════════════════
    //  DATABASE CONFIGURATION
    // ════════════════════════════════════════════════════════════════

    const val DATABASE_NAME = "neurodrive_db"
    const val DATABASE_VERSION = 1

    // Table names
    const val TABLE_SIGNALS = "bio_signals"
    const val TABLE_COMMANDS = "commands"
    const val TABLE_SESSIONS = "sessions"
    const val TABLE_CALIBRATIONS = "calibrations"

    // ════════════════════════════════════════════════════════════════
    //  DEVICE COMPATIBILITY
    // ════════════════════════════════════════════════════════════════

    // NOT const - Build.VERSION_CODES are not compile-time constants
    // Using literal values instead
    const val MIN_ANDROID_SDK = 26  // Android 8.0 Oreo
    const val TARGET_ANDROID_SDK = 34  // Android 14

    val MIN_ANDROID_VERSION: Int get() = MIN_ANDROID_SDK
    val TARGET_ANDROID_VERSION: Int get() = TARGET_ANDROID_SDK

    const val REQUIRES_BLE = true
    const val REQUIRES_BLUETOOTH_5 = false  // Optional, but recommended
    const val REQUIRES_LOCATION = true      // For BLE scanning

    // ════════════════════════════════════════════════════════════════
    //  CONSTANTS & LIMITS
    // ════════════════════════════════════════════════════════════════

    const val MAX_SESSION_DURATION_HOURS = 4     // Auto-stop after 4 hours
    const val MIN_SESSION_DURATION_SECONDS = 10  // Ignore sessions < 10s

    const val MAX_COMMAND_QUEUE_SIZE = 100
    const val MAX_SIGNAL_BUFFER_SIZE = 1000

    const val NETWORK_CHECK_INTERVAL_MS = 60_000L // Check every minute

    // ════════════════════════════════════════════════════════════════
    //  HELPER FUNCTIONS
    // ════════════════════════════════════════════════════════════════

    /**
     * Get full version string (e.g., "1.0.0 (1) - Debug")
     */
    fun getFullVersionString(): String {
        return "$VERSION_NAME ($VERSION_CODE)${if (IS_DEBUG) " - Debug" else ""}"
    }

    /**
     * Check if device meets minimum requirements
     */
    fun isDeviceCompatible(): Boolean {
        return Build.VERSION.SDK_INT >= MIN_ANDROID_SDK
    }

    /**
     * Get device info string
     */
    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})"
    }

    /**
     * Check if feature is enabled
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return when (feature) {
            "eeg" -> ENABLE_EEG_CONTROL
            "eog" -> ENABLE_EOG_CONTROL
            "emergency" -> ENABLE_EMERGENCY_DETECTION
            "voice" -> ENABLE_VOICE_COMMANDS
            "ml" -> ENABLE_ML_PREDICTIONS
            "cloud" -> ENABLE_CLOUD_SYNC
            "raw_signals" -> ENABLE_RAW_SIGNAL_VIEW
            "diagnostics" -> ENABLE_ADVANCED_DIAGNOSTICS
            "developer" -> ENABLE_DEVELOPER_MENU
            else -> false
        }
    }

    /**
     * Get log tag for component
     */
    fun getLogTag(component: String): String {
        return when (component) {
            "ble" -> LOG_TAG_BLE
            "signal" -> LOG_TAG_SIGNAL
            "command" -> LOG_TAG_COMMAND
            "car" -> LOG_TAG_CAR
            "ui" -> LOG_TAG_UI
            else -> "NeuroDrive:$component"
        }
    }

    /**
     * Check if calibration is fresh (within validity period)
     */
    fun isCalibrationFresh(lastCalibrationTimeMs: Long): Boolean {
        val validityMs = CALIBRATION_VALIDITY_HOURS * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastCalibrationTimeMs) < validityMs
    }

    /**
     * Format battery time remaining
     */
    fun formatBatteryTime(percent: Int): String {
        val totalMinutes = (percent * BATTERY_FULL_RUNTIME_HOURS * 60).toInt() / 100
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    /**
     * Get battery status description
     */
    fun getBatteryStatus(percent: Int): BatteryStatus {
        return when {
            percent <= BATTERY_CRITICAL_PERCENT -> BatteryStatus.CRITICAL
            percent <= BATTERY_LOW_PERCENT -> BatteryStatus.LOW
            percent <= 50 -> BatteryStatus.MEDIUM
            else -> BatteryStatus.GOOD
        }
    }

    /**
     * Check if running on Android version
     */
    fun isAtLeastAndroid(sdkVersion: Int): Boolean {
        return Build.VERSION.SDK_INT >= sdkVersion
    }

    /**
     * Check if running on Android 12+
     */
    fun isAtLeastAndroid12(): Boolean = isAtLeastAndroid(31)

    /**
     * Check if running on Android 13+
     */
    fun isAtLeastAndroid13(): Boolean = isAtLeastAndroid(33)

    // ════════════════════════════════════════════════════════════════
    //  ENUMS
    // ════════════════════════════════════════════════════════════════

    enum class BatteryStatus {
        GOOD,      // > 50%
        MEDIUM,    // 21-50%
        LOW,       // 11-20%
        CRITICAL   // <= 10%
    }
}