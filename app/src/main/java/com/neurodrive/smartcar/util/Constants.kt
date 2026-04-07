package com.neurodrive.smartcar.util

import java.util.UUID

/**
 * Application-wide constants
 * Centralized configuration for the entire app
 */
object Constants {

    // ════════════════════════════════════════════════════════════════
    //  App Information
    // ════════════════════════════════════════════════════════════════

    const val APP_NAME = "NeuroDrive"
    const val APP_DESCRIPTION = "Bio-Signal Controlled Smart Car"
    const val APP_VERSION = "1.0.0"
    const val DATABASE_NAME = "neurodrive_db"
    const val DATABASE_VERSION = 1

    // ════════════════════════════════════════════════════════════════
    //  Navigation Routes
    // ════════════════════════════════════════════════════════════════

    object Routes {
        // Auth Flow
        const val SPLASH = "splash"
        const val ONBOARDING = "onboarding"
        const val LOGIN = "login"
        const val REGISTER = "register"
        const val FORGOT_PASSWORD = "forgot_password"

        const val EMAIL_VERIFICATION = "email_verification/{email}"

        // Main App
        const val DASHBOARD = "dashboard"
        const val CONTROL = "control"
        const val SETTINGS = "settings"
        const val ALERTS = "alerts"
        const val PROFILE = "profile"
        const val CALIBRATION = "calibration"
        const val HELP = "help"

        // Additional Screens
        const val DEVICE_CONNECTION = "device_connection"
        const val SIGNAL_HISTORY = "signal_history"
        const val BLE_SCANNER = "ble_scanner"
        const val ESP32_SCANNER = "esp32_scanner"
        const val COMMAND_LOG = "command_log"
        const val ABOUT = "about"
        const val PRIVACY_POLICY = "privacy_policy"
        const val TERMS_OF_SERVICE = "terms_of_service"

        // Debug (only in debug builds)
        const val DEBUG_MENU = "debug_menu"
        const val FIREBASE_TEST = "firebase_test"
    }

    // ════════════════════════════════════════════════════════════════
    //  Firebase Collections
    // ════════════════════════════════════════════════════════════════

    object Firebase {
        // Collections
        const val USERS_COLLECTION = "users"
        const val SETTINGS_COLLECTION = "user_settings"
        const val BIO_SIGNALS_COLLECTION = "bio_signals"
        const val COMMANDS_COLLECTION = "commands"
        const val ALERTS_COLLECTION = "alerts"
        const val SESSIONS_COLLECTION = "sessions"
        const val CALIBRATION_COLLECTION = "calibrations"

        // User Document Fields
        const val FIELD_USER_ID = "userId"
        const val FIELD_EMAIL = "email"
        const val FIELD_NAME = "name"
        const val FIELD_USERNAME = "username"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_LAST_LOGIN = "lastLogin"
        const val FIELD_PROFILE_PICTURE = "profilePicture"
        const val FIELD_IS_PREMIUM = "isPremium"
        const val FIELD_DEVICES = "devices"

        // Settings Fields
        const val FIELD_SPEED_LIMIT = "speedLimit"
        const val FIELD_SIGNAL_THRESHOLD = "signalThreshold"
        const val FIELD_OBSTACLE_DISTANCE = "obstacleDistance"
        const val FIELD_AUTO_BRAKE = "autoBrake"
        const val FIELD_BIO_CONTROL_DEFAULT = "bioControlDefault"
        const val FIELD_DARK_MODE = "darkMode"
        const val FIELD_HIGH_CONTRAST = "highContrast"
        const val FIELD_FONT_SCALE = "fontScale"
        const val FIELD_HAPTIC_FEEDBACK = "hapticFeedback"
        const val FIELD_ALERT_VIBRATION = "alertVibration"
        const val FIELD_ALERT_SOUND = "alertSound"

        // Bio Signal Fields
        const val FIELD_TIMESTAMP = "timestamp"
        const val FIELD_EEG_ALPHA = "eegAlpha"
        const val FIELD_EEG_BETA = "eegBeta"
        const val FIELD_EMG_ACTIVITY = "emgActivity"
        const val FIELD_SNR = "snr"
        const val FIELD_IS_STABLE = "isStable"

        // Query Limits
        const val DEFAULT_QUERY_LIMIT = 100
        const val MAX_QUERY_LIMIT = 1000
        const val BIO_SIGNAL_SYNC_LIMIT = 200
    }

    // ════════════════════════════════════════════════════════════════
    //  Bluetooth Low Energy (BLE) - Bio-Signal Headband
    // ════════════════════════════════════════════════════════════════

    object BLE {
        // Service & Characteristic UUIDs (MindWave / Generic)
        val SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Custom NeuroDrive BLE Service (if using custom hardware)
        val NEURODRIVE_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef0")
        val NEURODRIVE_DATA_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef1")
        val NEURODRIVE_CONTROL_CHAR_UUID: UUID = UUID.fromString("12345678-1234-5678-1234-56789abcdef2")

        // Scanning
        const val SCAN_TIMEOUT_MS = 10_000L
        const val SCAN_PERIOD_MS = 30_000L
        const val CONNECTION_TIMEOUT_MS = 15_000L

        // Device Names
        const val DEFAULT_HEADBAND_NAME = "MindWave"
        val SUPPORTED_DEVICE_NAMES = listOf(
            "MindWave",
            "MindWave Mobile",
            "NeuroSky",
            "NeuroDrive-Headband",
            "BCI-Device"
        )

        // Signal Parameters
        const val SAMPLE_RATE_HZ = 256
        const val FRAME_SIZE_BYTES = 20
        const val NOTIFY_INTERVAL_MS = 4 // ~256 Hz

        // Signal Quality
        const val MIN_SNR_THRESHOLD = 10f // dB
        const val GOOD_SNR_THRESHOLD = 15f
        const val EXCELLENT_SNR_THRESHOLD = 20f

        // Reconnection
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val RECONNECT_DELAY_MS = 2000L
        const val RECONNECT_BACKOFF_MULTIPLIER = 1.5f
    }

    // ════════════════════════════════════════════════════════════════
    //  Bluetooth Classic - ESP32 Car
    // ════════════════════════════════════════════════════════════════

    object BluetoothClassic {
        // SPP UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        // Device Names
        const val DEFAULT_ESP32_NAME = "NeuroDrive-Car"
        val SUPPORTED_DEVICE_NAMES = listOf(
            "NeuroDrive-Car",
            "ESP32-Car",
            "SmartCar-BT",
            "HC-05",
            "HC-06"
        )

        // Connection
        const val CONNECTION_TIMEOUT_MS = 15_000L
        const val SOCKET_TIMEOUT_MS = 5_000L

        // Packet Structure
        const val COMMAND_PACKET_SIZE = 8
        const val TELEMETRY_PACKET_SIZE = 16
        const val START_BYTE: Byte = 0x55
        const val TELEMETRY_START_BYTE: Byte = 0xAA.toByte()

        // Command Codes
        const val CMD_STOP: Byte = 0x01
        const val CMD_FORWARD: Byte = 0x02
        const val CMD_LEFT: Byte = 0x03
        const val CMD_RIGHT: Byte = 0x04
        const val CMD_BRAKE: Byte = 0x05
        const val CMD_REVERSE: Byte = 0x06
        const val CMD_CALIBRATE: Byte = 0x07
        const val CMD_STATUS: Byte = 0x08
        const val CMD_RESET: Byte = 0x09

        // Response Codes
        const val RESP_OK: Byte = 0x00
        const val RESP_ERROR: Byte = 0x01
        const val RESP_BUSY: Byte = 0x02
        const val RESP_LOW_BATTERY: Byte = 0x03
        const val RESP_OBSTACLE: Byte = 0x04

        // Telemetry
        const val TELEMETRY_INTERVAL_MS = 100L
        const val MAX_RECONNECT_ATTEMPTS = 3
    }

    // ════════════════════════════════════════════════════════════════
    //  Signal Processing
    // ════════════════════════════════════════════════════════════════

    object SignalProcessing {
        // EEG Bands (Hz)
        const val DELTA_MIN = 0.5f
        const val DELTA_MAX = 4f
        const val THETA_MIN = 4f
        const val THETA_MAX = 8f
        const val ALPHA_MIN = 8f
        const val ALPHA_MAX = 13f
        const val BETA_MIN = 13f
        const val BETA_MAX = 30f
        const val GAMMA_MIN = 30f
        const val GAMMA_MAX = 100f

        // EMG
        const val EMG_MIN_THRESHOLD = 5f
        const val EMG_MAX_THRESHOLD = 100f

        // Classification Thresholds
        const val FORWARD_BETA_THRESHOLD = 15f
        const val TURN_EMG_THRESHOLD = 10f
        const val TURN_ALPHA_THRESHOLD = 8f
        const val TURN_BETA_THRESHOLD = 12f

        // Smoothing
        const val SMOOTHING_WINDOW_SIZE = 5
        const val MOVING_AVERAGE_WINDOW = 10

        // Confidence
        const val MIN_CONFIDENCE_THRESHOLD = 0.7f
        const val HIGH_CONFIDENCE_THRESHOLD = 0.9f

        // History
        const val MAX_SIGNAL_HISTORY = 200
        const val MAX_COMMAND_LOG = 500
        const val MAX_ALERT_HISTORY = 100
    }

    // ════════════════════════════════════════════════════════════════
    //  Driving & Safety
    // ════════════════════════════════════════════════════════════════

    object Driving {
        // Speed
        const val DEFAULT_SPEED = 70
        const val MIN_SPEED = 0
        const val MAX_SPEED = 100
        const val SPEED_INCREMENT = 10

        // Obstacle Detection
        const val DEFAULT_OBSTACLE_DISTANCE_CM = 15f
        const val MIN_OBSTACLE_DISTANCE_CM = 5f
        const val MAX_OBSTACLE_DISTANCE_CM = 100f
        const val CRITICAL_OBSTACLE_DISTANCE_CM = 10f

        // Battery
        const val BATTERY_LOW_PERCENT = 15
        const val BATTERY_CRITICAL_PERCENT = 5

        // Emergency
        const val EMERGENCY_BRAKE_SPEED = 0
        const val EMERGENCY_STOP_TIMEOUT_MS = 100L

        // Command Debounce
        const val COMMAND_DEBOUNCE_MS = 100L
        const val RAPID_COMMAND_THRESHOLD_MS = 50L
    }

    // ════════════════════════════════════════════════════════════════
    //  Security
    // ════════════════════════════════════════════════════════════════

    object Security {
        // Login
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 60_000L // 1 minute
        const val LOCKOUT_DURATION_EXTENDED_MS = 300_000L // 5 minutes

        // Session
        const val DEFAULT_SESSION_TIMEOUT_MS = 30 * 60_000L // 30 minutes
        const val MIN_SESSION_TIMEOUT_MS = 5 * 60_000L // 5 minutes
        const val MAX_SESSION_TIMEOUT_MS = 120 * 60_000L // 2 hours
        const val SESSION_REFRESH_THRESHOLD_MS = 5 * 60_000L // 5 minutes

        // Password
        const val PASSWORD_MIN_LENGTH = 8
        const val PASSWORD_MAX_LENGTH = 128
        const val PASSWORD_EXPIRY_DAYS = 90
        const val PASSWORD_EXPIRY_WARNING_DAYS = 80

        // PIN
        const val PIN_MIN_LENGTH = 4
        const val PIN_MAX_LENGTH = 8

        // Hashing
        const val SALT_LENGTH_BYTES = 32
        const val HASH_ALGORITHM = "SHA-256"
        const val HASH_ITERATIONS = 10_000
        const val TOKEN_LENGTH_BYTES = 32

        // Biometric
        const val BIOMETRIC_VALIDITY_DURATION_SECONDS = 60
    }

    // ════════════════════════════════════════════════════════════════
    //  UI & Accessibility
    // ════════════════════════════════════════════════════════════════

    object UI {
        // Touch Targets
        const val MIN_TOUCH_TARGET_DP = 48
        const val LARGE_TOUCH_TARGET_DP = 64

        // Font Scale
        const val DEFAULT_FONT_SCALE = 1.0f
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 2.0f

        // Animation
        const val DEFAULT_ANIMATION_DURATION_MS = 300
        const val FAST_ANIMATION_DURATION_MS = 150
        const val SLOW_ANIMATION_DURATION_MS = 500

        // Refresh Rates
        const val DASHBOARD_REFRESH_MS = 100L // 10 Hz
        const val SIGNAL_GRAPH_REFRESH_MS = 50L // 20 Hz
        const val STATUS_REFRESH_MS = 1000L // 1 Hz

        // Limits
        const val MAX_SNACKBAR_DURATION_MS = 5000L
        const val TOOLTIP_DELAY_MS = 500L

        // Haptic
        const val HAPTIC_SHORT_MS = 30L
        const val HAPTIC_MEDIUM_MS = 50L
        const val HAPTIC_LONG_MS = 100L
    }

    // ════════════════════════════════════════════════════════════════
    //  URLs
    // ════════════════════════════════════════════════════════════════

    object URLs {
        const val BASE_URL = "https://api.neurodrive.app/v1/"
        const val BASE_URL_DEV = "https://dev-api.neurodrive.app/v1/"
        const val BASE_URL_STAGING = "https://staging-api.neurodrive.app/v1/"

        const val SUPPORT_EMAIL = "support@neurodrive.app"
        const val PRIVACY_POLICY = "https://neurodrive.app/privacy"
        const val TERMS_OF_SERVICE = "https://neurodrive.app/terms"
        const val HELP_CENTER = "https://help.neurodrive.app"
        const val FAQ = "https://neurodrive.app/faq"
        const val WEBSITE = "https://neurodrive.app"

        const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.neurodrive.smartcar"
    }

    // ════════════════════════════════════════════════════════════════
    //  Intent Actions & Extras
    // ════════════════════════════════════════════════════════════════

    object Intents {
        // Actions
        const val ACTION_EMERGENCY_STOP = "com.neurodrive.action.EMERGENCY_STOP"
        const val ACTION_RECONNECT_BLE = "com.neurodrive.action.RECONNECT_BLE"
        const val ACTION_RECONNECT_ESP32 = "com.neurodrive.action.RECONNECT_ESP32"
        const val ACTION_START_CONTROL = "com.neurodrive.action.START_CONTROL"
        const val ACTION_STOP_CONTROL = "com.neurodrive.action.STOP_CONTROL"
        const val ACTION_CALIBRATE = "com.neurodrive.action.CALIBRATE"

        // Extras
        const val EXTRA_DEVICE_ADDRESS = "device_address"
        const val EXTRA_DEVICE_NAME = "device_name"
        const val EXTRA_SIGNAL_TYPE = "signal_type"
        const val EXTRA_ERROR_MESSAGE = "error_message"
        const val EXTRA_COMMAND_TYPE = "command_type"
    }

    // ════════════════════════════════════════════════════════════════
    //  Notification Channels
    // ════════════════════════════════════════════════════════════════

    object Notifications {
        // Channel IDs
        const val CHANNEL_ALERTS = "neurodrive_alerts"
        const val CHANNEL_CONNECTION = "neurodrive_connection"
        const val CHANNEL_FOREGROUND = "neurodrive_foreground_service"
        const val CHANNEL_UPDATES = "neurodrive_updates"

        // Channel Names
        const val CHANNEL_ALERTS_NAME = "Alerts & Warnings"
        const val CHANNEL_CONNECTION_NAME = "Connection Status"
        const val CHANNEL_FOREGROUND_NAME = "Background Service"
        const val CHANNEL_UPDATES_NAME = "App Updates"

        // Notification IDs
        const val NOTIFICATION_FOREGROUND = 1
        const val NOTIFICATION_ALERT = 2
        const val NOTIFICATION_CONNECTION = 3
        const val NOTIFICATION_UPDATE = 4
    }

    // ════════════════════════════════════════════════════════════════
    //  Preferences Keys
    // ════════════════════════════════════════════════════════════════

    object PreferencesKeys {
        // DataStore name
        const val DATASTORE_NAME = "neurodrive_preferences"

        // Session
        const val IS_LOGGED_IN = "is_logged_in"
        const val USERNAME = "username"
        const val USER_ID = "user_id"
        const val LOGIN_TIMESTAMP = "login_timestamp"
        const val SESSION_TOKEN = "session_token"

        // Onboarding
        const val ONBOARDING_COMPLETE = "onboarding_complete"
        const val FIRST_LAUNCH = "first_launch"

        // Bluetooth
        const val ESP32_MAC_ADDRESS = "esp32_mac"
        const val BLE_DEVICE_NAME = "ble_device_name"
        const val AUTO_RECONNECT = "auto_reconnect"

        // Driving
        const val SPEED_LIMIT = "speed_limit"
        const val SIGNAL_THRESHOLD = "signal_threshold"
        const val OBSTACLE_DISTANCE = "obstacle_distance"
        const val AUTO_BRAKE = "auto_brake"
        const val BIO_CONTROL_DEFAULT = "bio_control_default"

        // Accessibility
        const val HIGH_CONTRAST = "high_contrast"
        const val FONT_SCALE = "font_scale"
        const val HAPTIC_FEEDBACK = "haptic_feedback"
        const val LARGE_TOUCH_TARGETS = "large_touch_targets"
        const val SCREEN_READER_OPTIMISED = "screen_reader_optimised"

        // Appearance
        const val DARK_MODE = "dark_mode"
        const val THEME_COLOR = "theme_color"

        // Alerts
        const val ALERT_VIBRATION = "alert_vibration"
        const val ALERT_SOUND = "alert_sound"
        const val ALERT_VISUAL_FLASH = "alert_visual_flash"

        // Security
        const val SESSION_TIMEOUT = "session_timeout"
        const val BIOMETRIC_ENABLED = "biometric_enabled"

        // Calibration
        const val LAST_CALIBRATION = "last_calibration"
        const val EEG_BASELINE_ALPHA = "eeg_baseline_alpha"
        const val EEG_BASELINE_BETA = "eeg_baseline_beta"
        const val EMG_BASELINE = "emg_baseline"

        // Analytics
        const val ANALYTICS_ENABLED = "analytics_enabled"
        const val CRASH_REPORTING_ENABLED = "crash_reporting_enabled"
    }

    // ════════════════════════════════════════════════════════════════
    //  Error Codes
    // ════════════════════════════════════════════════════════════════

    object ErrorCodes {
        // Authentication
        const val AUTH_INVALID_CREDENTIALS = "auth/invalid-credentials"
        const val AUTH_USER_NOT_FOUND = "auth/user-not-found"
        const val AUTH_WRONG_PASSWORD = "auth/wrong-password"
        const val AUTH_EMAIL_IN_USE = "auth/email-already-in-use"
        const val AUTH_WEAK_PASSWORD = "auth/weak-password"
        const val AUTH_TOO_MANY_REQUESTS = "auth/too-many-requests"
        const val AUTH_NETWORK_ERROR = "auth/network-request-failed"

        // Bluetooth
        const val BT_NOT_ENABLED = "bt/not-enabled"
        const val BT_NOT_SUPPORTED = "bt/not-supported"
        const val BT_CONNECTION_FAILED = "bt/connection-failed"
        const val BT_DEVICE_NOT_FOUND = "bt/device-not-found"
        const val BT_PERMISSION_DENIED = "bt/permission-denied"

        // Signal
        const val SIGNAL_LOW_QUALITY = "signal/low-quality"
        const val SIGNAL_LOST = "signal/lost"
        const val SIGNAL_CALIBRATION_REQUIRED = "signal/calibration-required"

        // General
        const val UNKNOWN_ERROR = "error/unknown"
        const val NETWORK_ERROR = "error/network"
        const val TIMEOUT_ERROR = "error/timeout"
    }

    // ════════════════════════════════════════════════════════════════
    //  Feature Flags
    // ════════════════════════════════════════════════════════════════

    object Features {
        const val VOICE_CONTROL = false
        const val CLOUD_SYNC = true
        const val ANALYTICS = true
        const val CRASH_REPORTING = true
        const val ADVANCED_ANALYTICS = false
        const val EXPORT_DATA = true
        const val GOOGLE_SIGN_IN = true
        const val BIOMETRIC_AUTH = true
        const val DARK_MODE = true
        const val MULTI_DEVICE = false
        const val REMOTE_CONTROL = false
    }

    // ════════════════════════════════════════════════════════════════
    //  Debug Constants (Only used in debug builds)
    // ════════════════════════════════════════════════════════════════

    object Debug {
        const val MOCK_ESP32_MAC = "AA:BB:CC:DD:EE:FF"
        const val MOCK_BLE_DEVICE = "Mock-Headband"
        const val ENABLE_LOGGING = true
        const val ENABLE_MOCK_DATA = true
        const val ENABLE_STRICT_MODE = true
        const val SHOW_DEBUG_MENU = true
    }
}