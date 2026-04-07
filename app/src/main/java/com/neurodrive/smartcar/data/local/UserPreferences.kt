package com.neurodrive.smartcar.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "UserPreferences"

        // ── Profile ─────────────────────────────────────
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USERNAME = stringPreferencesKey("username")
        val EMAIL = stringPreferencesKey("email")
        val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")

        // ── Bluetooth ───────────────────────────────────
        val ESP32_MAC_ADDRESS = stringPreferencesKey("esp32_mac")
        val BLE_DEVICE_NAME = stringPreferencesKey("ble_device_name")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")

        // ── Driving ─────────────────────────────────────
        val SPEED_LIMIT = intPreferencesKey("speed_limit")
        val SIGNAL_THRESHOLD = floatPreferencesKey("signal_threshold")
        val OBSTACLE_DISTANCE = floatPreferencesKey("obstacle_distance")
        val AUTO_BRAKE_ENABLED = booleanPreferencesKey("auto_brake")
        val BIO_CONTROL_DEFAULT = booleanPreferencesKey("bio_control_default")

        // ── Calibration ─────────────────────────────────
        val LAST_CALIBRATION_TIMESTAMP = longPreferencesKey("last_calibration")
        val EEG_BASELINE_ALPHA = floatPreferencesKey("eeg_alpha")
        val EEG_BASELINE_BETA = floatPreferencesKey("eeg_beta")
        val EMG_BASELINE = floatPreferencesKey("emg_baseline")

        // ── Accessibility ───────────────────────────────
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val LARGE_TOUCH_TARGETS = booleanPreferencesKey("large_touch_targets")
        val SCREEN_READER_OPTIMISED = booleanPreferencesKey("screen_reader_optimised")

        // ── Appearance ──────────────────────────────────
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val SELECTED_THEME = stringPreferencesKey("selected_theme")

        // ── Alerts ──────────────────────────────────────
        val ALERT_VIBRATION = booleanPreferencesKey("alert_vibration")
        val ALERT_SOUND = booleanPreferencesKey("alert_sound")
        val ALERT_VISUAL_FLASH = booleanPreferencesKey("alert_visual_flash")

        // ── Security ────────────────────────────────────
        val SESSION_TIMEOUT_MIN = intPreferencesKey("session_timeout")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val FAILED_LOGINS = intPreferencesKey("failed_login_count")
        val LOCKOUT_UNTIL = longPreferencesKey("lockout_until")

        // ── First-run ───────────────────────────────────
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val APP_VERSION_CODE = intPreferencesKey("app_version_code")
    }

    // ════════════════════════════════════════════════════════════════
    //  Safe Flow Helper (handles IOException gracefully)
    // ════════════════════════════════════════════════════════════════

    private fun <T> safeFlow(key: Preferences.Key<T>, default: T): Flow<T> =
        context.dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    Log.e(TAG, "Error reading $key", exception)
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { it[key] ?: default }

    // ════════════════════════════════════════════════════════════════
    //  Profile
    // ════════════════════════════════════════════════════════════════

    val isLoggedIn: Flow<Boolean> = safeFlow(IS_LOGGED_IN, false)
    val username: Flow<String> = safeFlow(USERNAME, "")
    val email: Flow<String> = safeFlow(EMAIL, "")
    val loginTimestamp: Flow<Long> = safeFlow(LOGIN_TIMESTAMP, 0L)

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit {
            it[IS_LOGGED_IN] = loggedIn
            if (loggedIn) {
                it[LOGIN_TIMESTAMP] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setUsername(username: String) {
        context.dataStore.edit { it[USERNAME] = username.trim() }
    }

    suspend fun setEmail(email: String) {
        context.dataStore.edit { it[EMAIL] = email.trim() }
    }

    // Convenience suspend function for login state
    suspend fun isLoggedInSync(): Boolean =
        context.dataStore.data.first()[IS_LOGGED_IN] ?: false

    // ════════════════════════════════════════════════════════════════
    //  Bluetooth
    // ════════════════════════════════════════════════════════════════

    val esp32MacAddress: Flow<String> = safeFlow(ESP32_MAC_ADDRESS, "")
    val bleDeviceName: Flow<String> = safeFlow(BLE_DEVICE_NAME, "MindWave")
    val autoReconnect: Flow<Boolean> = safeFlow(AUTO_RECONNECT, true)

    suspend fun setEsp32MacAddress(mac: String) {
        context.dataStore.edit { it[ESP32_MAC_ADDRESS] = mac.trim().uppercase() }
    }

    suspend fun setBleDeviceName(name: String) {
        context.dataStore.edit { it[BLE_DEVICE_NAME] = name.trim() }
    }

    suspend fun setAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_RECONNECT] = enabled }
    }

    // ════════════════════════════════════════════════════════════════
    //  Driving
    // ════════════════════════════════════════════════════════════════

    val speedLimit: Flow<Int> = safeFlow(SPEED_LIMIT, 100)
    val signalThreshold: Flow<Float> = safeFlow(SIGNAL_THRESHOLD, 10f)
    val obstacleDistance: Flow<Float> = safeFlow(OBSTACLE_DISTANCE, 15f)
    val autoBrakeEnabled: Flow<Boolean> = safeFlow(AUTO_BRAKE_ENABLED, true)
    val bioControlDefault: Flow<Boolean> = safeFlow(BIO_CONTROL_DEFAULT, true)

    suspend fun setSpeedLimit(limit: Int) {
        context.dataStore.edit { it[SPEED_LIMIT] = limit.coerceIn(0, 100) }
    }

    suspend fun setSignalThreshold(threshold: Float) {
        context.dataStore.edit { it[SIGNAL_THRESHOLD] = threshold.coerceIn(1f, 30f) }
    }

    suspend fun setObstacleDistance(distance: Float) {
        context.dataStore.edit { it[OBSTACLE_DISTANCE] = distance.coerceIn(5f, 100f) }
    }

    suspend fun setAutoBrakeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_BRAKE_ENABLED] = enabled }
    }

    suspend fun setBioControlDefault(enabled: Boolean) {
        context.dataStore.edit { it[BIO_CONTROL_DEFAULT] = enabled }
    }

    // ════════════════════════════════════════════════════════════════
    //  Calibration
    // ════════════════════════════════════════════════════════════════

    val lastCalibrationTimestamp: Flow<Long> = safeFlow(LAST_CALIBRATION_TIMESTAMP, 0L)
    val eegBaselineAlpha: Flow<Float> = safeFlow(EEG_BASELINE_ALPHA, 0f)
    val eegBaselineBeta: Flow<Float> = safeFlow(EEG_BASELINE_BETA, 0f)
    val emgBaseline: Flow<Float> = safeFlow(EMG_BASELINE, 0f)

    suspend fun saveCalibration(alpha: Float, beta: Float, emg: Float) {
        context.dataStore.edit {
            it[LAST_CALIBRATION_TIMESTAMP] = System.currentTimeMillis()
            it[EEG_BASELINE_ALPHA] = alpha
            it[EEG_BASELINE_BETA] = beta
            it[EMG_BASELINE] = emg
        }
        Log.i(TAG, "Calibration saved: alpha=$alpha, beta=$beta, emg=$emg")
    }

    // ════════════════════════════════════════════════════════════════
    //  Accessibility
    // ════════════════════════════════════════════════════════════════

    val highContrast: Flow<Boolean> = safeFlow(HIGH_CONTRAST, false)
    val fontScale: Flow<Float> = safeFlow(FONT_SCALE, 1f)
    val hapticFeedback: Flow<Boolean> = safeFlow(HAPTIC_FEEDBACK, true)
    val largeTouchTargets: Flow<Boolean> = safeFlow(LARGE_TOUCH_TARGETS, false)
    val screenReaderOptimised: Flow<Boolean> = safeFlow(SCREEN_READER_OPTIMISED, false)

    suspend fun setHighContrast(enabled: Boolean) {
        context.dataStore.edit { it[HIGH_CONTRAST] = enabled }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[FONT_SCALE] = scale.coerceIn(0.8f, 2f) }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setLargeTouchTargets(enabled: Boolean) {
        context.dataStore.edit { it[LARGE_TOUCH_TARGETS] = enabled }
    }

    suspend fun setScreenReaderOptimised(enabled: Boolean) {
        context.dataStore.edit { it[SCREEN_READER_OPTIMISED] = enabled }
    }

    // ════════════════════════════════════════════════════════════════
    //  Appearance
    // ════════════════════════════════════════════════════════════════

    val darkMode: Flow<Boolean> = safeFlow(DARK_MODE, false)
    val selectedTheme: Flow<String> = safeFlow(SELECTED_THEME, "Purple (Default)")

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setSelectedTheme(theme: String) {
        context.dataStore.edit { it[SELECTED_THEME] = theme }
    }

    // ════════════════════════════════════════════════════════════════
    //  Alerts
    // ════════════════════════════════════════════════════════════════

    val alertVibration: Flow<Boolean> = safeFlow(ALERT_VIBRATION, true)
    val alertSound: Flow<Boolean> = safeFlow(ALERT_SOUND, true)
    val alertVisualFlash: Flow<Boolean> = safeFlow(ALERT_VISUAL_FLASH, true)

    suspend fun setAlertVibration(enabled: Boolean) {
        context.dataStore.edit { it[ALERT_VIBRATION] = enabled }
    }

    suspend fun setAlertSound(enabled: Boolean) {
        context.dataStore.edit { it[ALERT_SOUND] = enabled }
    }

    suspend fun setAlertVisualFlash(enabled: Boolean) {
        context.dataStore.edit { it[ALERT_VISUAL_FLASH] = enabled }
    }

    // ════════════════════════════════════════════════════════════════
    //  Security
    // ════════════════════════════════════════════════════════════════

    val sessionTimeoutMin: Flow<Int> = safeFlow(SESSION_TIMEOUT_MIN, 30)
    val biometricEnabled: Flow<Boolean> = safeFlow(BIOMETRIC_ENABLED, false)
    val failedLoginCount: Flow<Int> = safeFlow(FAILED_LOGINS, 0)
    val lockoutUntil: Flow<Long> = safeFlow(LOCKOUT_UNTIL, 0L)

    suspend fun setSessionTimeoutMin(minutes: Int) {
        context.dataStore.edit { it[SESSION_TIMEOUT_MIN] = minutes.coerceIn(1, 120) }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setFailedLoginCount(count: Int) {
        context.dataStore.edit { it[FAILED_LOGINS] = count.coerceAtLeast(0) }
    }

    suspend fun incrementFailedLogins() {
        val current = context.dataStore.data.first()[FAILED_LOGINS] ?: 0
        context.dataStore.edit { it[FAILED_LOGINS] = current + 1 }
    }

    suspend fun resetFailedLogins() {
        context.dataStore.edit {
            it[FAILED_LOGINS] = 0
            it.remove(LOCKOUT_UNTIL)
        }
    }

    suspend fun setLockoutUntil(timestamp: Long) {
        context.dataStore.edit { it[LOCKOUT_UNTIL] = timestamp }
    }

    // ════════════════════════════════════════════════════════════════
    //  First-run / Onboarding
    // ════════════════════════════════════════════════════════════════

    val onboardingComplete: Flow<Boolean> = safeFlow(ONBOARDING_COMPLETE, false)
    val appVersionCode: Flow<Int> = safeFlow(APP_VERSION_CODE, 0)

    suspend fun setOnboardingComplete(completed: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = completed }
        Log.i(TAG, "Onboarding complete set to: $completed")
    }

    suspend fun setAppVersionCode(versionCode: Int) {
        context.dataStore.edit { it[APP_VERSION_CODE] = versionCode }
    }

    // ════════════════════════════════════════════════════════════════
    //  Reset Helpers
    // ════════════════════════════════════════════════════════════════

    suspend fun resetDrivingDefaults() {
        context.dataStore.edit {
            it[SPEED_LIMIT] = 100
            it[SIGNAL_THRESHOLD] = 10f
            it[OBSTACLE_DISTANCE] = 15f
            it[AUTO_BRAKE_ENABLED] = true
            it[BIO_CONTROL_DEFAULT] = true
        }
        Log.i(TAG, "Driving settings reset to defaults")
    }

    suspend fun resetAccessibilityDefaults() {
        context.dataStore.edit {
            it[HIGH_CONTRAST] = false
            it[FONT_SCALE] = 1f
            it[HAPTIC_FEEDBACK] = true
            it[LARGE_TOUCH_TARGETS] = false
            it[SCREEN_READER_OPTIMISED] = false
        }
        Log.i(TAG, "Accessibility settings reset to defaults")
    }

    // ════════════════════════════════════════════════════════════════
    //  Clear Operations
    // ════════════════════════════════════════════════════════════════

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
        Log.i(TAG, "All preferences cleared")
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(IS_LOGGED_IN)
            it.remove(USERNAME)
            it.remove(EMAIL)
            it.remove(LOGIN_TIMESTAMP)
            it.remove(FAILED_LOGINS)
            it.remove(LOCKOUT_UNTIL)
        }
        Log.i(TAG, "Session preferences cleared")
    }

    // ════════════════════════════════════════════════════════════════
    //  Convenience One-Shot Reads
    // ════════════════════════════════════════════════════════════════

    /**
     * Check if the current session is still valid based on login timestamp and timeout
     */
    suspend fun isSessionValid(): Boolean {
        val loggedIn = isLoggedIn.first()
        if (!loggedIn) return false

        val loginTs = loginTimestamp.first()
        val timeout = sessionTimeoutMin.first()
        val elapsed = System.currentTimeMillis() - loginTs

        return elapsed < timeout * 60_000L
    }

    /**
     * Check if the account is currently locked out
     */
    suspend fun isLockedOut(): Boolean {
        val until = lockoutUntil.first()
        return System.currentTimeMillis() < until
    }

    /**
     * Check if calibration data is fresh (within the specified age)
     */
    suspend fun isCalibrationFresh(maxAgeMs: Long = 24 * 60 * 60 * 1000L): Boolean {
        val ts = lastCalibrationTimestamp.first()
        return ts > 0 && (System.currentTimeMillis() - ts) < maxAgeMs
    }


}
