
package com.neurodrive.smartcar.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore



import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton




@Singleton
class FirebaseDataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository:FirebaseAuthRepository
) {
    companion object {
        private const val TAG = "FirebaseDataRepo"
        private const val SETTINGS_COLLECTION = "user_settings"
        private const val BIO_SIGNALS_COLLECTION = "bio_signals"
        private const val COMMANDS_COLLECTION = "commands"
        private const val SESSIONS_COLLECTION = "sessions"
    }

// ════════════════════════════════════════════════
//  User Settings
// ════════════════════════════════════════════════

    suspend fun saveSettings(settings: Map<String, Any>): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .set(settings)
                .await()

            Log.d(TAG, "Settings saved to Firebase")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
            Result.failure(e)
        }
    }

    suspend fun getSettings(): Result<Map<String, Any>> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val document = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.success(emptyMap())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get settings", e)
            Result.failure(e)
        }
    }


    suspend fun clearBioSignalHistory(): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val collection = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(BIO_SIGNALS_COLLECTION)

            val documents = collection.get().await()

            documents.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "Bio signal history cleared")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
            Result.failure(e)
        }
    }

    suspend fun clearCommandHistory(): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val collection = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(COMMANDS_COLLECTION)

            val documents = collection.get().await()

            documents.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "Command history cleared")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear command history", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllData(): Result<Unit> {
        return try {
            clearBioSignalHistory()
            clearCommandHistory()

            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            // Clear sessions
            val sessions = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(SESSIONS_COLLECTION)
                .get()
                .await()

            sessions.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "All data cleared")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all data", e)
            Result.failure(e)
        }
    }
}


// ════════════════════════════════════════════════
// Calibration Data Classes
// ════════════════════════════════════════════════

data class EEGCalibration(
    val deltaBaseline: Float = 0f,
    val thetaBaseline: Float = 0f,
    val alphaBaseline: Float = 0f,
    val betaBaseline: Float = 0f,
    val gammaBaseline: Float = 0f,
    val snrBaseline: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class EOGCalibration(
    val amplitudeBaseline: Float = 0f,
    val blinkThreshold: Float = 100f,
    val saccadeThreshold: Float = 50f,
    val timestamp: Long = System.currentTimeMillis()
)
/*package com.neurodrive.smartcar.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.neurodrive.smartcar.data.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository
) {
    companion object {
        private const val TAG = "FirebaseDataRepo"
        private const val SETTINGS_COLLECTION = "user_settings"
        private const val BIO_SIGNALS_COLLECTION = "bio_signals"
        private const val COMMANDS_COLLECTION = "commands"
        private const val SESSIONS_COLLECTION = "sessions"
    }

    // ════════════════════════════════════════════════
    //  User Settings
    // ════════════════════════════════════════════════

    suspend fun saveSettings(settings: Map<String, Any>): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .set(settings)
                .await()

            Log.d(TAG, "Settings saved to Firebase")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
            Result.failure(e)
        }
    }

    suspend fun getSettings(): Result<Map<String, Any>> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val document = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .get()
                .await()

            if (document.exists()) {
                Result.success(document.data ?: emptyMap())
            } else {
                Result.success(emptyMap())
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get settings", e)
            Result.failure(e)
        }
    }
}
    // ════════════════════════════════════════════════
    //  Bio-Signal History (Cloud Backup)
    // ════════════════════════════════════════════════

    suspend fun saveBioSignal(signal: BioSignal): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val signalMap = hashMapOf(
                "timestamp" to signal.timestamp,

                // EEG Data
                "eeg_filteredValue" to signal.eeg.filteredValue,
                "eeg_delta" to signal.eeg.delta,
                "eeg_theta" to signal.eeg.theta,
                "eeg_alpha" to signal.eeg.alpha,
                "eeg_beta" to signal.eeg.beta,
                "eeg_gamma" to signal.eeg.gamma,
                "eeg_totalPower" to signal.eeg.totalPower,
                "eeg_dominantBand" to signal.eeg.dominantBand,
                "eeg_alphaBetaRatio" to signal.eeg.alphaBetaRatio,
                "eeg_focusIndex" to signal.eeg.focusIndex,
                "eeg_relaxationIndex" to signal.eeg.relaxationIndex,
                "eeg_drowsinessIndex" to signal.eeg.drowsinessIndex,
                "eeg_mentalState" to signal.eeg.mentalState.code,
                "eeg_snr" to signal.eeg.snr,
                "eeg_isStable" to signal.eeg.isStable,
                "eeg_hasArtifact" to signal.eeg.hasArtifact,

                // EOG Data
                "eog_filteredValue" to signal.eog.filteredValue,
                "eog_amplitude" to signal.eog.amplitude,
                "eog_blinkCount" to signal.eog.blinkCount,
                "eog_blinkRate" to signal.eog.blinkRate,
                "eog_saccadeCount" to signal.eog.saccadeCount,
                "eog_eyePosition" to signal.eog.eyePosition,
                "eog_eyeOpenness" to signal.eog.eyeOpenness,
                "eog_perclos" to signal.eog.perclos,
                "eog_snr" to signal.eog.snr,
                "eog_isStable" to signal.eog.isStable,

                // Combined Metrics
                "drowsinessLevel" to signal.drowsinessLevel,
                "attentionLevel" to signal.attentionLevel,
                "stressLevel" to signal.stressLevel,
                "fatigueLevel" to signal.fatigueLevel,
                "alertLevel" to signal.alertLevel.code,
                "isStable" to signal.isStable,
                "overallSnr" to signal.overallSnr
            )

            firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(BIO_SIGNALS_COLLECTION)
                .add(signalMap)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bio signal", e)
            Result.failure(e)
        }
    }

    suspend fun getBioSignalHistory(limit: Int = 100): Result<List<BioSignal>> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val querySnapshot = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(BIO_SIGNALS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val signals = querySnapshot.documents.mapNotNull { doc ->
                try {
                    // Parse EEG Signal
                    val eeg = EEGSignal(
                        filteredValue = doc.getDouble("eeg_filteredValue")?.toFloat() ?: 0f,
                        delta = doc.getDouble("eeg_delta")?.toFloat() ?: 0f,
                        theta = doc.getDouble("eeg_theta")?.toFloat() ?: 0f,
                        alpha = doc.getDouble("eeg_alpha")?.toFloat() ?: 0f,
                        beta = doc.getDouble("eeg_beta")?.toFloat() ?: 0f,
                        gamma = doc.getDouble("eeg_gamma")?.toFloat() ?: 0f,
                        totalPower = doc.getDouble("eeg_totalPower")?.toFloat() ?: 0f,
                        dominantBand = doc.getString("eeg_dominantBand") ?: "",
                        alphaBetaRatio = doc.getDouble("eeg_alphaBetaRatio")?.toFloat() ?: 0f,
                        focusIndex = doc.getDouble("eeg_focusIndex")?.toFloat() ?: 0f,
                        relaxationIndex = doc.getDouble("eeg_relaxationIndex")?.toFloat() ?: 0f,
                        drowsinessIndex = doc.getDouble("eeg_drowsinessIndex")?.toFloat() ?: 0f,
                        mentalState = MentalState.fromCode(doc.getLong("eeg_mentalState")?.toInt() ?: 0),
                        snr = doc.getDouble("eeg_snr")?.toFloat() ?: 0f,
                        isStable = doc.getBoolean("eeg_isStable") ?: false,
                        hasArtifact = doc.getBoolean("eeg_hasArtifact") ?: false
                    )

                    // Parse EOG Signal
                    val eog = EOGSignal(
                        filteredValue = doc.getDouble("eog_filteredValue")?.toFloat() ?: 0f,
                        amplitude = doc.getDouble("eog_amplitude")?.toFloat() ?: 0f,
                        blinkCount = doc.getLong("eog_blinkCount")?.toInt() ?: 0,
                        blinkRate = doc.getDouble("eog_blinkRate")?.toFloat() ?: 0f,
                        saccadeCount = doc.getLong("eog_saccadeCount")?.toInt() ?: 0,
                        eyePosition = doc.getDouble("eog_eyePosition")?.toFloat() ?: 0f,
                        eyeOpenness = doc.getDouble("eog_eyeOpenness")?.toFloat() ?: 1f,
                        perclos = doc.getDouble("eog_perclos")?.toFloat() ?: 0f,
                        snr = doc.getDouble("eog_snr")?.toFloat() ?: 0f,
                        isStable = doc.getBoolean("eog_isStable") ?: false
                    )

                    // Create BioSignal
                    BioSignal(
                        eeg = eeg,
                        eog = eog,
                        drowsinessLevel = doc.getDouble("drowsinessLevel")?.toFloat() ?: 0f,
                        attentionLevel = doc.getDouble("attentionLevel")?.toFloat() ?: 0f,
                        stressLevel = doc.getDouble("stressLevel")?.toFloat() ?: 0f,
                        fatigueLevel = doc.getDouble("fatigueLevel")?.toFloat() ?: 0f,
                        alertLevel = AlertLevel.fromCode(doc.getLong("alertLevel")?.toInt() ?: 0),
                        isStable = doc.getBoolean("isStable") ?: false,
                        overallSnr = doc.getDouble("overallSnr")?.toFloat() ?: 0f,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse bio signal document", e)
                    null
                }
            }

            Result.success(signals)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get bio signal history", e)
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════
    //  Command History
    // ════════════════════════════════════════════════

    suspend fun saveCommand(command: Command): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val commandMap = hashMapOf(
                "id" to command.id,
                "type" to command.type.name,
                "typeCode" to command.type.code,
                "value" to command.value,
                "confidence" to command.confidence,
                "source" to command.source.name,
                "description" to command.description,
                "mentalState" to command.mentalState.code,
                "timestamp" to command.timestamp,
                "isExecuted" to command.isExecuted,
                "executionResult" to command.executionResult
            )

            firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(COMMANDS_COLLECTION)
                .add(commandMap)
                .await()

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save command", e)
            Result.failure(e)
        }
    }

    suspend fun getCommandHistory(limit: Int = 100): Result<List<Command>> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val querySnapshot = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(COMMANDS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val commands = querySnapshot.documents.mapNotNull { doc ->
                try {
                    Command(
                        id = doc.getLong("id") ?: System.currentTimeMillis(),
                        type = CommandType.fromCode(doc.getLong("typeCode")?.toInt() ?: 0),
                        value = doc.getDouble("value")?.toFloat() ?: 0f,
                        confidence = doc.getDouble("confidence")?.toFloat() ?: 0f,
                        source = try {
                            CommandSource.valueOf(doc.getString("source") ?: "SYSTEM")
                        } catch (e: Exception) {
                            CommandSource.SYSTEM
                        },
                        description = doc.getString("description") ?: "",
                        mentalState = MentalState.fromCode(doc.getLong("mentalState")?.toInt() ?: 0),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        isExecuted = doc.getBoolean("isExecuted") ?: false,
                        executionResult = doc.getString("executionResult") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse command document", e)
                    null
                }
            }

            Result.success(commands)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get command history", e)
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════
    //  Calibration Data
    // ════════════════════════════════════════════════

    suspend fun saveCalibration(
        eegBaseline: EEGCalibration,
        eogBaseline: EOGCalibration
    ): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val calibrationMap = hashMapOf(
                // EEG Calibration
                "eeg_deltaBaseline" to eegBaseline.deltaBaseline,
                "eeg_thetaBaseline" to eegBaseline.thetaBaseline,
                "eeg_alphaBaseline" to eegBaseline.alphaBaseline,
                "eeg_betaBaseline" to eegBaseline.betaBaseline,
                "eeg_gammaBaseline" to eegBaseline.gammaBaseline,
                "eeg_snrBaseline" to eegBaseline.snrBaseline,

                // EOG Calibration
                "eog_amplitudeBaseline" to eogBaseline.amplitudeBaseline,
                "eog_blinkThreshold" to eogBaseline.blinkThreshold,
                "eog_saccadeThreshold" to eogBaseline.saccadeThreshold,

                // Timestamp
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .update("calibration", calibrationMap)
                .await()

            Log.d(TAG, "Calibration saved to Firebase")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save calibration", e)
            Result.failure(e)
        }
    }

    suspend fun getCalibration(): Result<Pair<EEGCalibration, EOGCalibration>?> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val document = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val calibration = document.get("calibration") as? Map<String, Any>

            if (calibration != null) {
                val eegCalibration = EEGCalibration(
                    deltaBaseline = (calibration["eeg_deltaBaseline"] as? Number)?.toFloat() ?: 0f,
                    thetaBaseline = (calibration["eeg_thetaBaseline"] as? Number)?.toFloat() ?: 0f,
                    alphaBaseline = (calibration["eeg_alphaBaseline"] as? Number)?.toFloat() ?: 0f,
                    betaBaseline = (calibration["eeg_betaBaseline"] as? Number)?.toFloat() ?: 0f,
                    gammaBaseline = (calibration["eeg_gammaBaseline"] as? Number)?.toFloat() ?: 0f,
                    snrBaseline = (calibration["eeg_snrBaseline"] as? Number)?.toFloat() ?: 0f
                )

                val eogCalibration = EOGCalibration(
                    amplitudeBaseline = (calibration["eog_amplitudeBaseline"] as? Number)?.toFloat() ?: 0f,
                    blinkThreshold = (calibration["eog_blinkThreshold"] as? Number)?.toFloat() ?: 100f,
                    saccadeThreshold = (calibration["eog_saccadeThreshold"] as? Number)?.toFloat() ?: 50f
                )

                Result.success(Pair(eegCalibration, eogCalibration))
            } else {
                Result.success(null)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get calibration", e)
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════
    //  Session Statistics
    // ════════════════════════════════════════════════

    suspend fun saveSessionStatistics(stats: SessionStatistics): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val statsMap = hashMapOf(
                "sessionId" to stats.sessionId,
                "startTime" to stats.startTime,
                "endTime" to stats.endTime,
                "durationMs" to stats.durationMs,

                // EEG Stats
                "avgAlpha" to stats.avgAlpha,
                "avgBeta" to stats.avgBeta,
                "avgTheta" to stats.avgTheta,
                "avgFocusIndex" to stats.avgFocusIndex,
                "avgDrowsinessIndex" to stats.avgDrowsinessIndex,
                "dominantState" to stats.dominantState.code,

                // EOG Stats
                "totalBlinks" to stats.totalBlinks,
                "avgBlinkRate" to stats.avgBlinkRate,
                "totalSaccades" to stats.totalSaccades,
                "avgPerclos" to stats.avgPerclos,

                // Alert Stats
                "drowsinessWarnings" to stats.drowsinessWarnings,
                "criticalAlerts" to stats.criticalAlerts,
                "avgDrowsinessLevel" to stats.avgDrowsinessLevel,
                "avgAttentionLevel" to stats.avgAttentionLevel,
                "maxDrowsinessLevel" to stats.maxDrowsinessLevel,

                // Quality
                "avgSignalQuality" to stats.avgSignalQuality,
                "signalLossPercentage" to stats.signalLossPercentage
            )

            firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(SESSIONS_COLLECTION)
                .document(stats.sessionId)
                .set(statsMap)
                .await()

            Log.d(TAG, "Session statistics saved")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session statistics", e)
            Result.failure(e)
        }
    }

    suspend fun getSessionHistory(limit: Int = 30): Result<List<SessionStatistics>> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val querySnapshot = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(SESSIONS_COLLECTION)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val sessions = querySnapshot.documents.mapNotNull { doc ->
                try {
                    SessionStatistics(
                        sessionId = doc.getString("sessionId") ?: "",
                        startTime = doc.getLong("startTime") ?: 0L,
                        endTime = doc.getLong("endTime") ?: 0L,
                        durationMs = doc.getLong("durationMs") ?: 0L,
                        avgAlpha = doc.getDouble("avgAlpha")?.toFloat() ?: 0f,
                        avgBeta = doc.getDouble("avgBeta")?.toFloat() ?: 0f,
                        avgTheta = doc.getDouble("avgTheta")?.toFloat() ?: 0f,
                        avgFocusIndex = doc.getDouble("avgFocusIndex")?.toFloat() ?: 0f,
                        avgDrowsinessIndex = doc.getDouble("avgDrowsinessIndex")?.toFloat() ?: 0f,
                        dominantState = MentalState.fromCode(doc.getLong("dominantState")?.toInt() ?: 0),
                        totalBlinks = doc.getLong("totalBlinks")?.toInt() ?: 0,
                        avgBlinkRate = doc.getDouble("avgBlinkRate")?.toFloat() ?: 0f,
                        totalSaccades = doc.getLong("totalSaccades")?.toInt() ?: 0,
                        avgPerclos = doc.getDouble("avgPerclos")?.toFloat() ?: 0f,
                        drowsinessWarnings = doc.getLong("drowsinessWarnings")?.toInt() ?: 0,
                        criticalAlerts = doc.getLong("criticalAlerts")?.toInt() ?: 0,
                        avgDrowsinessLevel = doc.getDouble("avgDrowsinessLevel")?.toFloat() ?: 0f,
                        avgAttentionLevel = doc.getDouble("avgAttentionLevel")?.toFloat() ?: 0f,
                        maxDrowsinessLevel = doc.getDouble("maxDrowsinessLevel")?.toFloat() ?: 0f,
                        avgSignalQuality = doc.getDouble("avgSignalQuality")?.toFloat() ?: 0f,
                        signalLossPercentage = doc.getDouble("signalLossPercentage")?.toFloat() ?: 0f
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse session document", e)
                    null
                }
            }

            Result.success(sessions)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get session history", e)
            Result.failure(e)
        }
    }

    // ════════════════════════════════════════════════
    //  Clear Data
    // ════════════════════════════════════════════════

    suspend fun clearBioSignalHistory(): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val collection = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(BIO_SIGNALS_COLLECTION)

            val documents = collection.get().await()

            documents.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "Bio signal history cleared")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear history", e)
            Result.failure(e)
        }
    }

    suspend fun clearCommandHistory(): Result<Unit> {
        return try {
            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            val collection = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(COMMANDS_COLLECTION)

            val documents = collection.get().await()

            documents.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "Command history cleared")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear command history", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllData(): Result<Unit> {
        return try {
            clearBioSignalHistory()
            clearCommandHistory()

            val userId = authRepository.currentUserId
                ?: return Result.failure(Exception("User not logged in"))

            // Clear sessions
            val sessions = firestore.collection(SETTINGS_COLLECTION)
                .document(userId)
                .collection(SESSIONS_COLLECTION)
                .get()
                .await()

            sessions.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            Log.d(TAG, "All data cleared")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all data", e)
            Result.failure(e)
        }
    }
}

// ════════════════════════════════════════════════
//  Calibration Data Classes
// ════════════════════════════════════════════════

data class EEGCalibration(
    val deltaBaseline: Float = 0f,
    val thetaBaseline: Float = 0f,
    val alphaBaseline: Float = 0f,
    val betaBaseline: Float = 0f,
    val gammaBaseline: Float = 0f,
    val snrBaseline: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class EOGCalibration(
    val amplitudeBaseline: Float = 0f,
    val blinkThreshold: Float = 100f,
    val saccadeThreshold: Float = 50f,
    val timestamp: Long = System.currentTimeMillis()

 /*// ════════════════════════════════════════════════
// Bio-Signal History (Cloud Backup)
// ════════════════════════════════════════════════

suspend fun saveBioSignal(signal: BioSignal): Result<Unit> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val signalMap = hashMapOf(
            "timestamp" to signal.timestamp,

            // EEG Data
            "eeg_filteredValue" to signal.eeg.filteredValue,
            "eeg_delta" to signal.eeg.delta,
            "eeg_theta" to signal.eeg.theta,
            "eeg_alpha" to signal.eeg.alpha,
            "eeg_beta" to signal.eeg.beta,
            "eeg_gamma" to signal.eeg.gamma,
            "eeg_totalPower" to signal.eeg.totalPower,
            "eeg_dominantBand" to signal.eeg.dominantBand,
            "eeg_alphaBetaRatio" to signal.eeg.alphaBetaRatio,
            "eeg_focusIndex" to signal.eeg.focusIndex,
            "eeg_relaxationIndex" to signal.eeg.relaxationIndex,
            "eeg_drowsinessIndex" to signal.eeg.drowsinessIndex,
            "eeg_mentalState" to signal.eeg.mentalState.code,
            "eeg_snr" to signal.eeg.snr,
            "eeg_isStable" to signal.eeg.isStable,
            "eeg_hasArtifact" to signal.eeg.hasArtifact,

            // EOG Data
            "eog_filteredValue" to signal.eog.filteredValue,
            "eog_amplitude" to signal.eog.amplitude,
            "eog_blinkCount" to signal.eog.blinkCount,
            "eog_blinkRate" to signal.eog.blinkRate,
            "eog_saccadeCount" to signal.eog.saccadeCount,
            "eog_eyePosition" to signal.eog.eyePosition,
            "eog_eyeOpenness" to signal.eog.eyeOpenness,
            "eog_perclos" to signal.eog.perclos,
            "eog_snr" to signal.eog.snr,
            "eog_isStable" to signal.eog.isStable,

            // Combined Metrics
            "drowsinessLevel" to signal.drowsinessLevel,
            "attentionLevel" to signal.attentionLevel,
            "stressLevel" to signal.stressLevel,
            "fatigueLevel" to signal.fatigueLevel,
            "alertLevel" to signal.alertLevel.code,
            "isStable" to signal.isStable,
            "overallSnr" to signal.overallSnr
        )

        firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .collection(BIO_SIGNALS_COLLECTION)
            .add(signalMap)
            .await()

        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to save bio signal", e)
        Result.failure(e)
    }
}

suspend fun getBioSignalHistory(limit: Int = 100): Result<List<BioSignal>> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val querySnapshot = firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .collection(BIO_SIGNALS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val signals = querySnapshot.documents.mapNotNull { doc ->
            try {
                // Parse EEG Signal
                val eeg = EEGSignal(
                    filteredValue = doc.getDouble("eeg_filteredValue")?.toFloat() ?: 0f,
                    delta = doc.getDouble("eeg_delta")?.toFloat() ?: 0f,
                    theta = doc.getDouble("eeg_theta")?.toFloat() ?: 0f,
                    alpha = doc.getDouble("eeg_alpha")?.toFloat() ?: 0f,
                    beta = doc.getDouble("eeg_beta")?.toFloat() ?: 0f,
                    gamma = doc.getDouble("eeg_gamma")?.toFloat() ?: 0f,
                    totalPower = doc.getDouble("eeg_totalPower")?.toFloat() ?: 0f,
                    dominantBand = doc.getString("eeg_dominantBand") ?: "",
                    alphaBetaRatio = doc.getDouble("eeg_alphaBetaRatio")?.toFloat() ?: 0f,
                    focusIndex = doc.getDouble("eeg_focusIndex")?.toFloat() ?: 0f,
                    relaxationIndex = doc.getDouble("eeg_relaxationIndex")?.toFloat() ?: 0f,
                    drowsinessIndex = doc.getDouble("eeg_drowsinessIndex")?.toFloat() ?: 0f,
                    mentalState = MentalState.fromCode(doc.getLong("eeg_mentalState")?.toInt() ?: 0),
                    snr = doc.getDouble("eeg_snr")?.toFloat() ?: 0f,
                    isStable = doc.getBoolean("eeg_isStable") ?: false,
                    hasArtifact = doc.getBoolean("eeg_hasArtifact") ?: false
                )

                // Parse EOG Signal
                val eog = EOGSignal(
                    filteredValue = doc.getDouble("eog_filteredValue")?.toFloat() ?: 0f,
                    amplitude = doc.getDouble("eog_amplitude")?.toFloat() ?: 0f,
                    blinkCount = doc.getLong("eog_blinkCount")?.toInt() ?: 0,
                    blinkRate = doc.getDouble("eog_blinkRate")?.toFloat() ?: 0f,
                    saccadeCount = doc.getLong("eog_saccadeCount")?.toInt() ?: 0,
                    eyePosition = doc.getDouble("eog_eyePosition")?.toFloat() ?: 0f,
                    eyeOpenness = doc.getDouble("eog_eyeOpenness")?.toFloat() ?: 1f,
                    perclos = doc.getDouble("eog_perclos")?.toFloat() ?: 0f,
                    snr = doc.getDouble("eog_snr")?.toFloat() ?: 0f,
                    isStable = doc.getBoolean("eog_isStable") ?: false
                )

                // Create BioSignal
                BioSignal(
                    eeg = eeg,
                    eog = eog,
                    drowsinessLevel = doc.getDouble("drowsinessLevel")?.toFloat() ?: 0f,
                    attentionLevel = doc.getDouble("attentionLevel")?.toFloat() ?: 0f,
                    stressLevel = doc.getDouble("stressLevel")?.toFloat() ?: 0f,
                    fatigueLevel = doc.getDouble("fatigueLevel")?.toFloat() ?: 0f,
                    alertLevel = AlertLevel.fromCode(doc.getLong("alertLevel")?.toInt() ?: 0),
                    isStable = doc.getBoolean("isStable") ?: false,
                    overallSnr = doc.getDouble("overallSnr")?.toFloat() ?: 0f,
                    timestamp = doc.getLong("timestamp") ?: 0L
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse bio signal document", e)
                null
            }
        }

        Result.success(signals)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get bio signal history", e)
        Result.failure(e)
    }
}

// ════════════════════════════════════════════════
//  Command History
// ════════════════════════════════════════════════

suspend fun saveCommand(command: Command): Result<Unit> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val commandMap = hashMapOf(
            "id" to command.id,
            "type" to command.type.name,
            "typeCode" to command.type.code,
            "value" to command.value,
            "confidence" to command.confidence,
            "source" to command.source.name,
            "description" to command.description,
            "mentalState" to command.mentalState.code,
            "timestamp" to command.timestamp,
            "isExecuted" to command.isExecuted,
            "executionResult" to command.executionResult
        )

        firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .collection(COMMANDS_COLLECTION)
            .add(commandMap)
            .await()

        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to save command", e)
        Result.failure(e)
    }
}

suspend fun getCommandHistory(limit: Int = 100): Result<List<Command>> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val querySnapshot = firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .collection(COMMANDS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val commands = querySnapshot.documents.mapNotNull { doc ->
            try {
                Command(
                    id = doc.getLong("id") ?: System.currentTimeMillis(),
                    type = CommandType.fromCode(doc.getLong("typeCode")?.toInt() ?: 0),
                    value = doc.getDouble("value")?.toFloat() ?: 0f,
                    confidence = doc.getDouble("confidence")?.toFloat() ?: 0f,
                    source = try {
                        CommandSource.valueOf(doc.getString("source") ?: "SYSTEM")
                    } catch (e: Exception) {
                        CommandSource.SYSTEM
                    },
                    description = doc.getString("description") ?: "",
                    mentalState = MentalState.fromCode(doc.getLong("mentalState")?.toInt() ?: 0),
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    isExecuted = doc.getBoolean("isExecuted") ?: false,
                    executionResult = doc.getString("executionResult") ?: ""
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse command document", e)
                null
            }
        }

        Result.success(commands)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get command history", e)
        Result.failure(e)
    }
}

// ════════════════════════════════════════════════
//  Calibration Data
// ════════════════════════════════════════════════

suspend fun saveCalibration(
    eegBaseline: EEGCalibration,
    eogBaseline: EOGCalibration
): Result<Unit> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val calibrationMap = hashMapOf(
            // EEG Calibration
            "eeg_deltaBaseline" to eegBaseline.deltaBaseline,
            "eeg_thetaBaseline" to eegBaseline.thetaBaseline,
            "eeg_alphaBaseline" to eegBaseline.alphaBaseline,
            "eeg_betaBaseline" to eegBaseline.betaBaseline,
            "eeg_gammaBaseline" to eegBaseline.gammaBaseline,
            "eeg_snrBaseline" to eegBaseline.snrBaseline,

            // EOG Calibration
            "eog_amplitudeBaseline" to eogBaseline.amplitudeBaseline,
            "eog_blinkThreshold" to eogBaseline.blinkThreshold,
            "eog_saccadeThreshold" to eogBaseline.saccadeThreshold,

            // Timestamp
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .update("calibration", calibrationMap)
            .await()

        Log.d(TAG, "Calibration saved to Firebase")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to save calibration", e)
        Result.failure(e)
    }
}

suspend fun getCalibration(): Result<Pair<EEGCalibration, EOGCalibration>?> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val document = firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .get()
            .await()

        @Suppress("UNCHECKED_CAST")
        val calibration = document.get("calibration") as? Map<String, Any>

        if (calibration != null) {
            val eegCalibration = EEGCalibration(
                deltaBaseline = (calibration["eeg_deltaBaseline"] as? Number)?.toFloat() ?: 0f,
                thetaBaseline = (calibration["eeg_thetaBaseline"] as? Number)?.toFloat() ?: 0f,
                alphaBaseline = (calibration["eeg_alphaBaseline"] as? Number)?.toFloat() ?: 0f,
                betaBaseline = (calibration["eeg_betaBaseline"] as? Number)?.toFloat() ?: 0f,
                gammaBaseline = (calibration["eeg_gammaBaseline"] as? Number)?.toFloat() ?: 0f,
                snrBaseline = (calibration["eeg_snrBaseline"] as? Number)?.toFloat() ?: 0f
            )

            val eogCalibration = EOGCalibration(
                amplitudeBaseline = (calibration["eog_amplitudeBaseline"] as? Number)?.toFloat() ?: 0f,
                blinkThreshold = (calibration["eog_blinkThreshold"] as? Number)?.toFloat() ?: 100f,
                saccadeThreshold = (calibration["eog_saccadeThreshold"] as? Number)?.toFloat() ?: 50f
            )

            Result.success(Pair(eegCalibration, eogCalibration))
        } else {
            Result.success(null)
        }

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get calibration", e)
        Result.failure(e)
    }
}

// ════════════════════════════════════════════════
//  Session Statistics
// ════════════════════════════════════════════════

suspend fun saveSessionStatistics(stats: SessionStatistics): Result<Unit> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val statsMap = hashMapOf(
            "sessionId" to stats.sessionId,
            "startTime" to stats.startTime,
            "endTime" to stats.endTime,
            "durationMs" to stats.durationMs,

            // EEG Stats
            "avgAlpha" to stats.avgAlpha,
            "avgBeta" to stats.avgBeta,
            "avgTheta" to stats.avgTheta,
            "avgFocusIndex" to stats.avgFocusIndex,
            "avgDrowsinessIndex" to stats.avgDrowsinessIndex,
            "dominantState" to stats.dominantState.code,

            // EOG Stats
            "totalBlinks" to stats.totalBlinks,
            "avgBlinkRate" to stats.avgBlinkRate,
            "totalSaccades" to stats.totalSaccades,
            "avgPerclos" to stats.avgPerclos,

            // Alert Stats
            "drowsinessWarnings" to stats.drowsinessWarnings,
            "criticalAlerts" to stats.criticalAlerts,
            "avgDrowsinessLevel" to stats.avgDrowsinessLevel,
            "avgAttentionLevel" to stats.avgAttentionLevel,
            "maxDrowsinessLevel" to stats.maxDrowsinessLevel,

            // Quality
            "avgSignalQuality" to stats.avgSignalQuality,
            "signalLossPercentage" to stats.signalLossPercentage
        )

        firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .collection(SESSIONS_COLLECTION)
            .document(stats.sessionId)
            .set(statsMap)
            .await()

        Log.d(TAG, "Session statistics saved")
        Result.success(Unit)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to save session statistics", e)
        Result.failure(e)
    }
}

suspend fun getSessionHistory(limit: Int = 30): Result<List<SessionStatistics>> {
    return try {
        val userId = authRepository.currentUserId
            ?: return Result.failure(Exception("User not logged in"))

        val querySnapshot = firestore.collection(SETTINGS_COLLECTION)
            .document(userId)
            .collection(SESSIONS_COLLECTION)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()

        val sessions = querySnapshot.documents.mapNotNull { doc ->
            try {
                SessionStatistics(
                    sessionId = doc.getString("sessionId") ?: "",
                    startTime = doc.getLong("startTime") ?: 0L,
                    endTime = doc.getLong("endTime") ?: 0L,
                    durationMs = doc.getLong("durationMs") ?: 0L,
                    avgAlpha = doc.getDouble("avgAlpha")?.toFloat() ?: 0f,
                    avgBeta = doc.getDouble("avgBeta")?.toFloat() ?: 0f,
                    avgTheta = doc.getDouble("avgTheta")?.toFloat() ?: 0f,
                    avgFocusIndex = doc.getDouble("avgFocusIndex")?.toFloat() ?: 0f,
                    avgDrowsinessIndex = doc.getDouble("avgDrowsinessIndex")?.toFloat() ?: 0f,
                    dominantState = MentalState.fromCode(doc.getLong("dominantState")?.toInt() ?: 0),
                    totalBlinks = doc.getLong("totalBlinks")?.toInt() ?: 0,
                    avgBlinkRate = doc.getDouble("avgBlinkRate")?.toFloat() ?: 0f,
                    totalSaccades = doc.getLong("totalSaccades")?.toInt() ?: 0,
                    avgPerclos = doc.getDouble("avgPerclos")?.toFloat() ?: 0f,
                    drowsinessWarnings = doc.getLong("drowsinessWarnings")?.toInt() ?: 0,
                    criticalAlerts = doc.getLong("criticalAlerts")?.toInt() ?: 0,
                    avgDrowsinessLevel = doc.getDouble("avgDrowsinessLevel")?.toFloat() ?: 0f,
                    avgAttentionLevel = doc.getDouble("avgAttentionLevel")?.toFloat() ?: 0f,
                    maxDrowsinessLevel = doc.getDouble("maxDrowsinessLevel")?.toFloat() ?: 0f,
                    avgSignalQuality = doc.getDouble("avgSignalQuality")?.toFloat() ?: 0f,
                    signalLossPercentage = doc.getDouble("signalLossPercentage")?.toFloat() ?: 0f
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse session document", e)
                null
            }
        }

        Result.success(sessions)

    } catch (e: Exception) {
        Log.e(TAG, "Failed to get session history", e)
        Result.failure(e)
    }
}*/

// ════════════════════════════════════════════════
//  Clear Data
// ════════════════════════════════════════════════
)*/