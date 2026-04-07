package com.neurodrive.smartcar.domain

import com.neurodrive.smartcar.data.model.CommandType

/**
 * Represents the lock status of command execution
 * When a command is executed, it locks for a duration to prevent rapid command changes
 */
data class LockStatus(
    val isLocked: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalDurationSeconds: Int = 30,
    val lockedCommand: String = "",
    val lockedCommandType: CommandType = CommandType.NONE
) {
    /**
     * Progress from 0f (just locked) to 1f (about to unlock)
     */
    val progress: Float
        get() = if (totalDurationSeconds > 0 && isLocked) {
            1f - (remainingSeconds.toFloat() / totalDurationSeconds.toFloat())
        } else {
            0f
        }

    /**
     * Remaining progress from 1f (just locked) to 0f (about to unlock)
     */
    val remainingProgress: Float
        get() = if (totalDurationSeconds > 0 && isLocked) {
            remainingSeconds.toFloat() / totalDurationSeconds.toFloat()
        } else {
            0f
        }

    /**
     * Remaining time in milliseconds
     */
    val remainingTimeMs: Long
        get() = remainingSeconds * 1000L

    /**
     * Total duration in milliseconds
     */
    val totalDurationMs: Long
        get() = totalDurationSeconds * 1000L

    val isAboutToUnlock: Boolean
        get() = isLocked && remainingSeconds <= 5

    val isHalfway: Boolean
        get() = isLocked && remainingSeconds <= totalDurationSeconds / 2

    companion object {
        val UNLOCKED = LockStatus()

        fun locked(
            durationSeconds: Int = 30,
            command: String = "",
            commandType: CommandType = CommandType.NONE
        ): LockStatus {
            return LockStatus(
                isLocked = true,
                remainingSeconds = durationSeconds,
                totalDurationSeconds = durationSeconds,
                lockedCommand = command,
                lockedCommandType = commandType
            )
        }
    }
}