package com.neurodrive.smartcar.data.local

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted key-value store built on top of [EncryptedSharedPreferences].
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SecureStorage"

        // Preferences file name (encrypted on disk)
        private const val PREFS_FILE_NAME = "neurodrive_secure_prefs"

        // ── Key constants ───────────────────────────
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_PASSWORD_SALT = "password_salt"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_LAST_LOGIN = "last_login"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ESP32_PAIRING_KEY = "esp32_pairing_key"
        private const val KEY_BLE_PAIRING_KEY = "ble_pairing_key"
        private const val KEY_BIOMETRIC_TOKEN = "biometric_token"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_ACCOUNT_CREATED = "account_created"
        private const val KEY_PASSWORD_CHANGED = "password_changed"

        // ── Security parameters ─────────────────────
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 60_000L          // 1 minute
        const val DEFAULT_SESSION_TIMEOUT_MS = 30 * 60_000L // 30 minutes
        private const val SALT_LENGTH_BYTES = 32
        private const val HASH_ALGORITHM = "SHA-256"
        private const val HASH_ITERATIONS = 10_000
        private const val TOKEN_LENGTH_BYTES = 32
    }

    private val masterKey: MasterKey by lazy {
        try {
            val spec = KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            MasterKey.Builder(context)
                .setKeyGenParameterSpec(spec)
                .build()
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to create MasterKey with custom spec, falling back", e)
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "EncryptedSharedPreferences creation failed", e)
            handleCorruptedPreferences()
        } catch (e: IOException) {
            Log.e(TAG, "EncryptedSharedPreferences IO error", e)
            handleCorruptedPreferences()
        }
    }

    private fun handleCorruptedPreferences(): SharedPreferences {
        Log.w(TAG, "Deleting corrupted preferences file and recreating")
        context.deleteSharedPreferences(PREFS_FILE_NAME)
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val secureRandom = SecureRandom()

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        var hash = digest.digest(salt + password.toByteArray(Charsets.UTF_8))

        repeat(HASH_ITERATIONS - 1) {
            hash = digest.digest(hash)
        }

        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun generateToken(): String {
        val bytes = ByteArray(TOKEN_LENGTH_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun saveCredentials(username: String, password: String): Boolean {
        return try {
            val salt = generateSalt()
            val hash = hashPassword(password, salt)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)

            sharedPreferences.edit()
                .putString(KEY_USERNAME, username.trim())
                .putString(KEY_PASSWORD_HASH, hash)
                .putString(KEY_PASSWORD_SALT, saltBase64)
                .putLong(KEY_ACCOUNT_CREATED, System.currentTimeMillis())
                .putLong(KEY_PASSWORD_CHANGED, System.currentTimeMillis())
                .apply()

            Log.i(TAG, "Credentials saved for user: $username")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save credentials", e)
            false
        }
    }

    fun verifyPassword(password: String): Boolean {
        return try {
            val storedHash = sharedPreferences.getString(KEY_PASSWORD_HASH, null)
                ?: return false
            val saltBase64 = sharedPreferences.getString(KEY_PASSWORD_SALT, null)
                ?: return false
            val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
            val inputHash = hashPassword(password, salt)

            MessageDigest.isEqual(
                storedHash.toByteArray(Charsets.UTF_8),
                inputHash.toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Password verification failed", e)
            false
        }
    }

    fun getUsername(): String? =
        sharedPreferences.getString(KEY_USERNAME, null)

    fun hasStoredCredentials(): Boolean =
        sharedPreferences.getString(KEY_PASSWORD_HASH, null) != null

    fun resetFailedAttempts() {
        sharedPreferences.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    // ════════════════════════════════════════════════
    //  Session & Login
    // ════════════════════════════════════════════════

    fun isSessionValid(): Boolean {
        val sessionExpiry = sharedPreferences.getLong(KEY_SESSION_EXPIRY, 0)
        return sessionExpiry > System.currentTimeMillis() && getSessionToken() != null
    }

    fun isLockedOut(): Boolean {
        val lockoutUntil = sharedPreferences.getLong(KEY_LOCKOUT_UNTIL, 0)
        return lockoutUntil > System.currentTimeMillis()
    }

    fun getRemainingLockoutMs(): Long {
        val lockoutUntil = sharedPreferences.getLong(KEY_LOCKOUT_UNTIL, 0)
        return (lockoutUntil - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun attemptLogin(username: String, password: String): LoginResult {
        if (isLockedOut()) {
            val remainingMs = getRemainingLockoutMs()
            return LoginResult.LockedOut(remainingMs)
        }

        val storedUsername = getUsername()
        val passwordValid = verifyPassword(password)
        val isValid = storedUsername == username.trim() && passwordValid

        return if (isValid) {
            resetFailedAttempts()
            val sessionToken = createSession()
            val isFirstLogin = sharedPreferences.getLong(KEY_LAST_LOGIN, 0) == 0L
            LoginResult.Success(sessionToken, isFirstLogin)
        } else {
            incrementFailedAttempts()
            val attempts = getFailedAttempts()
            val remainingAttempts = MAX_FAILED_ATTEMPTS - attempts

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                sharedPreferences.edit().putLong(KEY_LOCKOUT_UNTIL, lockoutUntil).apply()
                LoginResult.LockedOut(LOCKOUT_DURATION_MS)
            } else {
                LoginResult.Failed(
                    attemptsRemaining = remainingAttempts,
                    message = "Invalid username or password"
                )
            }
        }
    }

    fun authenticateWithBiometric(): LoginResult {
        if (isLockedOut()) {
            val remainingMs = getRemainingLockoutMs()
            return LoginResult.LockedOut(remainingMs)
        }

        val token = getBiometricToken()

        return if (token != null) {
            resetFailedAttempts()
            val sessionToken = createSession()
            LoginResult.Success(sessionToken, false)
        } else {
            incrementFailedAttempts()
            val attempts = getFailedAttempts()
            val remainingAttempts = MAX_FAILED_ATTEMPTS - attempts

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                sharedPreferences.edit().putLong(KEY_LOCKOUT_UNTIL, lockoutUntil).apply()
                LoginResult.LockedOut(LOCKOUT_DURATION_MS)
            } else {
                LoginResult.Failed(
                    attemptsRemaining = remainingAttempts,
                    message = "Biometric authentication failed"
                )
            }
        }
    }

    fun hasBiometricToken(): Boolean =
        getBiometricToken() != null

    fun getFailedAttempts(): Int =
        sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0)

    private fun incrementFailedAttempts() {
        val attempts = getFailedAttempts() + 1
        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
    }

    fun createSession(timeoutMs: Long = DEFAULT_SESSION_TIMEOUT_MS): String {
        val token = generateToken()
        val expiry = System.currentTimeMillis() + timeoutMs

        sharedPreferences.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putLong(KEY_SESSION_EXPIRY, expiry)
            .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            .apply()

        Log.i(TAG, "Session created, expires at $expiry")
        return token
    }

    fun clearSession() {
        sharedPreferences.edit()
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_SESSION_EXPIRY)
            .apply()
    }

    fun getSessionToken(): String? =
        sharedPreferences.getString(KEY_SESSION_TOKEN, null)

    fun saveBiometricToken(): String {
        val token = generateToken()
        sharedPreferences.edit()
            .putString(KEY_BIOMETRIC_TOKEN, token)
            .apply()
        return token
    }

    fun getBiometricToken(): String? =
        sharedPreferences.getString(KEY_BIOMETRIC_TOKEN, null)

    fun clearBiometricToken() {
        sharedPreferences.edit().remove(KEY_BIOMETRIC_TOKEN).apply()
    }

    // ── PIN management ───────────────────────────────────────────
    fun hasPin(): Boolean =
        sharedPreferences.getString(KEY_PIN_HASH, null) != null

    fun savePin(pin: String): Boolean {
        return try {
            val salt = generateSalt()
            val hash = hashPassword(pin, salt)
            sharedPreferences.edit()
                .putString(KEY_PIN_HASH, hash)
                .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save PIN", e)
            false
        }
    }

    fun clearPin() {
        sharedPreferences.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .apply()
    }

    // ── Password expiry check ────────────────────────────────────
    fun isPasswordExpired(maxAgeMs: Long): Boolean {
        val lastChanged = sharedPreferences.getLong(KEY_PASSWORD_CHANGED, 0)
        if (lastChanged == 0L) return false
        return System.currentTimeMillis() - lastChanged > maxAgeMs
    }

    // ── Change password ──────────────────────────────────────────
    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (!verifyPassword(oldPassword)) return false

        return try {
            val salt = generateSalt()
            val hash = hashPassword(newPassword, salt)
            sharedPreferences.edit()
                .putString(KEY_PASSWORD_HASH, hash)
                .putString(KEY_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putLong(KEY_PASSWORD_CHANGED, System.currentTimeMillis())
                .apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to change password", e)
            false
        }
    }

    // ── Logout ───────────────────────────────────────────────────
    fun logout() {
        sharedPreferences.edit()
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_SESSION_EXPIRY)
            .remove(KEY_BIOMETRIC_TOKEN)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .remove(KEY_LOCKOUT_UNTIL)
            .apply()
    }

    // ── Wipe all data (delete account) ───────────────────────────
    fun wipeAll() {
        sharedPreferences.edit().clear().apply()
    }
}

sealed class LoginResult {
    data class Success(
        val sessionToken: String,
        val isFirstLogin: Boolean
    ) : LoginResult()

    data class Failed(
        val attemptsRemaining: Int,
        val message: String
    ) : LoginResult()

    data class LockedOut(
        val remainingMs: Long
    ) : LoginResult()
}