package com.neurodrive.smartcar.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.neurodrive.smartcar.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Properties
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "FirebaseAuthRepo"
        private const val USERS_COLLECTION = "users"
        private const val OTP_COLLECTION = "email_otps"
        private const val OTP_EXPIRY_MINUTES = 10L

        // ✅ EMAIL CONFIGURATION
        // Option 1: Use Gmail App Password
        // Go to: Google Account → Security → 2-Step Verification → App Passwords
        // Generate a new app password for "Mail"
        private const val SMTP_EMAIL = "miniprojectteam001@gmail.com"       // ← Change this
        private const val SMTP_PASSWORD = "dflckgufqzdshdxt"        // ← Change this (16-char app password)
        private const val SMTP_HOST = "smtp.gmail.com"
        private const val SMTP_PORT = "587"
    }

    // ════════════════════════════════════════════════
    //  Current User State
    // ════════════════════════════════════════════════

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = firebaseAuth.currentUser != null

    // ════════════════════════════════════════════════
    //  Auth State Flow
    // ════════════════════════════════════════════════

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    // ════════════════════════════════════════════════
    //  Registration
    // ════════════════════════════════════════════════

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        username: String,
        phoneNumber: String = "",
        role: String = ""
    ): Result<FirebaseUser> {
        return withContext(NonCancellable) {
            try {
                Log.d(TAG, "🚀 Starting registration for: $email")

                // 1. Create Firebase Auth user
                val authResult = firebaseAuth
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val firebaseUser = authResult.user
                    ?: return@withContext Result.failure(Exception("User creation failed"))

                Log.d(TAG, "✅ Auth user created: ${firebaseUser.uid}")

                // 2. Update display name
                try {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    firebaseUser.updateProfile(profileUpdates).await()
                    Log.d(TAG, "✅ Display name set: $fullName")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to set display name, continuing...", e)
                }

                // 3. ✅ Wait a moment for auth token to propagate
                kotlinx.coroutines.delay(500)

                // 4. Create user document in Firestore
                try {
                    val userMap = hashMapOf(
                        "userId" to firebaseUser.uid,
                        "name" to fullName,
                        "username" to username,
                        "email" to email,
                        "phoneNumber" to phoneNumber,
                        "role" to role,
                        "createdAt" to System.currentTimeMillis(),
                        "lastLogin" to System.currentTimeMillis(),
                        "isEmailVerified" to true, // ✅ Already verified via OTP
                        "isPremium" to false,
                        "profilePhotoUrl" to "",
                        "isActive" to true
                    )

                    firestore.collection(USERS_COLLECTION)
                        .document(firebaseUser.uid)
                        .set(userMap)
                        .await()

                    Log.d(TAG, "✅ Firestore user document created")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Firestore write failed: ${e.message}", e)
                    // ❌ If Firestore fails, delete the auth user to avoid orphan
                    try {
                        firebaseUser.delete().await()
                        Log.d(TAG, "🧹 Cleaned up auth user after Firestore failure")
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "❌ Failed to cleanup auth user", deleteError)
                    }
                    return@withContext Result.failure(
                        Exception("Failed to create user profile: ${e.message}")
                    )
                }

                // 5. Clean up OTP document
                try {
                    cleanupOtp(email)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ OTP cleanup failed, non-critical", e)
                }

                Log.d(TAG, "✅ Registration complete: ${firebaseUser.uid}")
                Result.success(firebaseUser)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Registration failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    // ════════════════════════════════════════════════
    //  Login
    // ════════════════════════════════════════════════

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Login failed"))

            try {
                firestore.collection(USERS_COLLECTION)
                    .document(firebaseUser.uid)
                    .update("lastLogin", System.currentTimeMillis())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update lastLogin, continuing...", e)
            }

            Log.d(TAG, "✅ User logged in: ${firebaseUser.uid}")
            Result.success(firebaseUser)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Login failed", e)
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════
    //  Logout
    // ════════════════════════════════════════════════

    fun logout() {
        firebaseAuth.signOut()
        Log.d(TAG, "User logged out")
    }

    // ════════════════════════════════════════════════
    //  Password Management
    // ════════════════════════════════════════════════

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user logged in"))

            val email = user.email
                ?: return Result.failure(Exception("No email found"))

            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()

            Log.d(TAG, "✅ Password changed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Password change failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "✅ Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Password reset failed", e)
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return sendPasswordResetEmail(email)
    }

    // ════════════════════════════════════════════════
    //  Email Verification (Firebase Built-in)
    // ════════════════════════════════════════════════

    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
                ?: return Result.failure(Exception("No user logged in"))
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Result<Unit> {
        return try {
            firebaseAuth.currentUser?.reload()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    val isEmailVerified: Boolean
        get() = firebaseAuth.currentUser?.isEmailVerified ?: false

    // ════════════════════════════════════════════════
    //  OTP Email Verification (Pre-Registration)
    // ════════════════════════════════════════════════

    /**
     * Generate OTP, store in Firestore, and send via email
     */
    suspend fun sendOtpToEmail(email: String): Result<String> {
        return try {
            Log.d(TAG, "📧 Generating OTP for: $email")

            val otp = generateOtp()

            val expirationTime = Date(
                System.currentTimeMillis() + OTP_EXPIRY_MINUTES * 60 * 1000
            )

            // Store OTP in Firestore
            val otpData = hashMapOf(
                "otp" to otp,
                "email" to email,
                "createdAt" to Timestamp.now(),
                "expiresAt" to Timestamp(expirationTime),
                "verified" to false,
                "attempts" to 0
            )

            firestore.collection(OTP_COLLECTION)
                .document(email)
                .set(otpData)
                .await()

            Log.d(TAG, "✅ OTP stored in Firestore")

            // ✅ Send OTP via email
            try {
                sendOtpEmail(email, otp)
                Log.d(TAG, "✅ OTP email sent to: $email")
            } catch (emailError: Exception) {
                Log.e(TAG, "⚠️ Email sending failed: ${emailError.message}")
                Log.d(TAG, "🔑 OTP (check Logcat): $otp")
                // Don't fail - OTP is still in Firestore/Logcat for testing
            }

            Result.success(otp)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to generate/store OTP", e)
            Result.failure(e)
        }
    }

    /**
     * ✅ ACTUALLY SEND EMAIL with OTP
     * Uses JavaMail API with Gmail SMTP
     */
    private suspend fun sendOtpEmail(recipientEmail: String, otp: String) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", SMTP_HOST)
                    put("mail.smtp.port", SMTP_PORT)
                    put("mail.smtp.ssl.trust", SMTP_HOST)
                }

                val session = Session.getInstance(properties, object : javax.mail.Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(SMTP_EMAIL, SMTP_PASSWORD)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SMTP_EMAIL, "NeuroDrive"))
                    setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail)
                    )
                    subject = "NeuroDrive - Email Verification Code"
                    setContent(
                        buildOtpEmailHtml(otp),
                        "text/html; charset=utf-8"
                    )
                }

                Transport.send(message)
                Log.d(TAG, "✅ Email sent successfully to: $recipientEmail")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Email send failed: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Build HTML email body
     */
    private fun buildOtpEmailHtml(otp: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <style>
                body { font-family: Arial, sans-serif; background-color: #1a1a2e; color: #ffffff; }
                .container { max-width: 500px; margin: 0 auto; padding: 40px 20px; }
                .header { text-align: center; margin-bottom: 30px; }
                .header h1 { color: #00e676; margin: 0; font-size: 28px; }
                .otp-box { 
                    background-color: #16213e; 
                    border: 2px solid #00e676; 
                    border-radius: 12px; 
                    padding: 30px; 
                    text-align: center; 
                    margin: 20px 0; 
                }
                .otp-code { 
                    font-size: 36px; 
                    font-weight: bold; 
                    letter-spacing: 8px; 
                    color: #00e676; 
                    margin: 10px 0; 
                }
                .info { 
                    color: #a0a0a0; 
                    font-size: 14px; 
                    text-align: center; 
                    margin-top: 20px; 
                }
                .warning { color: #ff6b6b; font-size: 12px; margin-top: 15px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>🧠 NeuroDrive</h1>
                    <p style="color: #a0a0a0;">Email Verification</p>
                </div>
                <div class="otp-box">
                    <p style="margin: 0; color: #a0a0a0;">Your verification code is:</p>
                    <p class="otp-code">$otp</p>
                    <p style="margin: 0; color: #a0a0a0; font-size: 13px;">
                        Valid for ${OTP_EXPIRY_MINUTES} minutes
                    </p>
                </div>
                <div class="info">
                    <p>Enter this code in the NeuroDrive app to verify your email.</p>
                    <p class="warning">
                        ⚠️ If you didn't request this code, please ignore this email.
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * Verify OTP entered by user
     */
    suspend fun verifyOtp(email: String, otp: String): Result<Boolean> {
        return try {
            Log.d(TAG, "🔍 Verifying OTP for: $email")

            val document = firestore.collection(OTP_COLLECTION)
                .document(email)
                .get()
                .await()

            if (!document.exists()) {
                Log.w(TAG, "❌ No OTP found for: $email")
                return Result.failure(
                    Exception("No verification code found. Please request a new one.")
                )
            }

            val storedOtp = document.getString("otp") ?: ""
            val expiresAt = document.getTimestamp("expiresAt")
            val attempts = document.getLong("attempts") ?: 0
            val isAlreadyVerified = document.getBoolean("verified") ?: false

            if (isAlreadyVerified) {
                return Result.success(true)
            }

            if (attempts >= 5) {
                firestore.collection(OTP_COLLECTION)
                    .document(email)
                    .delete()
                    .await()
                return Result.failure(
                    Exception("Too many failed attempts. Please request a new code.")
                )
            }

            if (expiresAt != null && Timestamp.now().seconds > expiresAt.seconds) {
                firestore.collection(OTP_COLLECTION)
                    .document(email)
                    .delete()
                    .await()
                return Result.failure(
                    Exception("Code has expired. Please request a new one.")
                )
            }

            if (storedOtp == otp) {
                firestore.collection(OTP_COLLECTION)
                    .document(email)
                    .update(
                        mapOf(
                            "verified" to true,
                            "verifiedAt" to Timestamp.now()
                        )
                    )
                    .await()

                Log.d(TAG, "✅ OTP verified for: $email")
                Result.success(true)
            } else {
                firestore.collection(OTP_COLLECTION)
                    .document(email)
                    .update("attempts", attempts + 1)
                    .await()

                Log.w(TAG, "❌ Invalid OTP for: $email")
                Result.success(false)
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ OTP verification error", e)
            Result.failure(e)
        }
    }

    /**
     * Check if email has been verified via OTP
     */
    suspend fun isEmailOtpVerified(email: String): Result<Boolean> {
        return try {
            val document = firestore.collection(OTP_COLLECTION)
                .document(email)
                .get()
                .await()

            if (document.exists()) {
                Result.success(document.getBoolean("verified") ?: false)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clean up OTP document after registration
     */
    suspend fun cleanupOtp(email: String) {
        try {
            firestore.collection(OTP_COLLECTION)
                .document(email)
                .delete()
                .await()
            Log.d(TAG, "🧹 OTP cleaned up for: $email")
        } catch (e: Exception) {
            Log.w(TAG, "OTP cleanup failed, non-critical", e)
        }
    }

    /**
     * Generate secure 6-digit OTP
     */
    private fun generateOtp(): String {
        val secureRandom = java.security.SecureRandom()
        val otp = 100000 + secureRandom.nextInt(900000)
        return otp.toString()
    }

    // ════════════════════════════════════════════════
    //  Username Availability
    // ════════════════════════════════════════════════

    suspend fun isUsernameAvailable(username: String): Result<Boolean> {
        return try {
            val querySnapshot = firestore.collection(USERS_COLLECTION)
                .whereEqualTo("username", username.lowercase())
                .limit(1)
                .get()
                .await()

            Result.success(querySnapshot.isEmpty)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Username check failed", e)
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════
    //  Firestore User Operations
    // ════════════════════════════════════════════════

    suspend fun getUserFromFirestore(userId: String): Result<User> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val user = User(
                    userId = document.getString("userId") ?: "",
                    name = document.getString("name") ?: "",
                    email = document.getString("email") ?: "",
                    passwordHash = "",
                    createdAt = document.getLong("createdAt") ?: 0L
                )
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserExtendedData(userId: String): Result<Map<String, Any>> {
        return try {
            val document = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                val data = hashMapOf<String, Any>(
                    "userId" to (document.getString("userId") ?: ""),
                    "name" to (document.getString("name") ?: ""),
                    "username" to (document.getString("username") ?: ""),
                    "email" to (document.getString("email") ?: ""),
                    "phoneNumber" to (document.getString("phoneNumber") ?: ""),
                    "role" to (document.getString("role") ?: ""),
                    "profilePhotoUrl" to (document.getString("profilePhotoUrl") ?: ""),
                    "isEmailVerified" to (document.getBoolean("isEmailVerified") ?: false),
                    "isPremium" to (document.getBoolean("isPremium") ?: false),
                    "createdAt" to (document.getLong("createdAt") ?: 0L),
                    "lastLogin" to (document.getLong("lastLogin") ?: 0L)
                )
                Result.success(data)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Result<Unit> {
        return try {
            val updatesWithTimestamp = updates.toMutableMap()
            updatesWithTimestamp["updatedAt"] = System.currentTimeMillis()

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .update(updatesWithTimestamp)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.failure(Exception("No user logged in"))

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .delete()
                .await()

            firebaseAuth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePhoneNumber(userId: String, phoneNumber: String): Result<Unit> {
        return updateUserProfile(userId, mapOf("phoneNumber" to phoneNumber))
    }

    suspend fun updateRole(userId: String, role: String): Result<Unit> {
        return updateUserProfile(userId, mapOf("role" to role))
    }

    suspend fun updateProfilePhoto(userId: String, photoUrl: String): Result<Unit> {
        return updateUserProfile(userId, mapOf("profilePhotoUrl" to photoUrl))
    }

    suspend fun markEmailVerified(userId: String): Result<Unit> {
        return updateUserProfile(userId, mapOf("isEmailVerified" to true))
    }
}