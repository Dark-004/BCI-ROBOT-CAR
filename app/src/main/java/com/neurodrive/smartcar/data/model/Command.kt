package com.neurodrive.smartcar.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ════════════════════════════════════════════════════════════════
//  COMMAND TYPE
// ════════════════════════════════════════════════════════════════


enum class CommandType(val code: Int, val displayName: String) {
    // Movement Commands
    FORWARD(1, "Forward"),
    BACKWARD(2, "Backward"),
    TURN_LEFT(3, "Turn Left"),
    TURN_RIGHT(4, "Turn Right"),
    STOP(5, "Stop"),


    // Speed Commands
    ACCELERATE(10, "Accelerate"),
    DECELERATE(11, "Decelerate"),
    CRUISE(12, "Cruise"),
    MAINTAIN_SPEED(13, "Maintain Speed"),

    // Safety Commands
    EMERGENCY_STOP(20, "Emergency Stop"),
    ALERT_DRIVER(21, "Alert Driver"),

    // Interaction Commands
    CONFIRM(30, "Confirm"),
    CANCEL(31, "Cancel"),
    SELECT(32, "Select"),

    // System Commands
    CALIBRATE(40, "Calibrate"),
    RESET(41, "Reset"),

    // No Action
    IDLE(98, "Idle"),
    NONE(0, "None");

    companion object {
        fun fromCode(code: Int): CommandType {
            return values().find { it.code == code } ?: NONE
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  COMMAND SOURCE
// ════════════════════════════════════════════════════════════════

enum class CommandSource(val displayName: String) {
    EEG("Brain Signal"),
    EOG("Eye Movement"),
    COMBINED("Combined Signal"),
    MANUAL("Manual Input"),
    VOICE("Voice Command"),
    GESTURE("Gesture"),
    SYSTEM("System")
}

// ════════════════════════════════════════════════════════════════
//  COMMAND DATA CLASS
// ════════════════════════════════════════════════════════════════

@Parcelize
data class Command(
    val id: Long = System.currentTimeMillis(),
    val type: CommandType,
    val value: Float = 0f,                    // Command intensity/value (0-1)
    val confidence: Float = 100f,               // Classification confidence (0-1)
    val source: CommandSource = CommandSource.EEG,
    val description: String = "",
    val mentalState: MentalState = MentalState.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val isExecuted: Boolean = false,
    val executionResult: String = ""
) : Parcelable {

    // ════════════════════════════════════════
    //  COMPUTED PROPERTIES
    // ════════════════════════════════════════

    val isHighConfidence: Boolean
        get() = confidence >= 0.8f

    val isMediumConfidence: Boolean
        get() = confidence >= 0.6f && confidence < 0.8f

    val isLowConfidence: Boolean
        get() = confidence < 0.6f

    val isMovementCommand: Boolean
        get() = type in listOf(
            CommandType.FORWARD,
            CommandType.BACKWARD,
            CommandType.TURN_LEFT,
            CommandType.TURN_RIGHT,
            CommandType.STOP
        )

    val isSpeedCommand: Boolean
        get() = type in listOf(
            CommandType.ACCELERATE,
            CommandType.DECELERATE,
            CommandType.CRUISE,
            CommandType.MAINTAIN_SPEED
        )

    val isSafetyCommand: Boolean
        get() = type in listOf(
            CommandType.EMERGENCY_STOP,
            CommandType.ALERT_DRIVER
        )

    val isInteractionCommand: Boolean
        get() = type in listOf(
            CommandType.CONFIRM,
            CommandType.CANCEL,
            CommandType.SELECT
        )

    val confidencePercentage: Int
        get() = (confidence * 100).toInt()

    val valuePercentage: Int
        get() = (value * 100).toInt()

    // ════════════════════════════════════════
    //  DISPLAY METHODS
    // ════════════════════════════════════════

    fun getDisplayString(): String {
        return "${type.displayName} (${confidencePercentage}%)"
    }

    fun getDetailedString(): String {
        return buildString {
            append("Command: ${type.displayName}\n")
            append("Source: ${source.displayName}\n")
            append("Confidence: ${confidencePercentage}%\n")
            append("Value: ${valuePercentage}%\n")
            if (description.isNotBlank()) {
                append("Description: $description\n")
            }
            if (mentalState != MentalState.UNKNOWN) {
                append("Mental State: ${mentalState.displayName}")
            }
        }
    }

    // ════════════════════════════════════════
    //  ESP32 PACKET CONVERSION
    // ════════════════════════════════════════

    /**
     * Convert command to bytes for ESP32 transmission
     * Format: [HEADER(1)] [TYPE(1)] [VALUE(1)] [CONFIDENCE(1)] [CHECKSUM(1)]
     */
    fun toBytes(): ByteArray {
        val header: Byte = 0xCC.toByte()
        val typeCode: Byte = type.code.toByte()
        val valueInt: Byte = (value * 255).toInt().coerceIn(0, 255).toByte()
        val confInt: Byte = (confidence * 255).toInt().coerceIn(0, 255).toByte()

        val checksum: Byte = (header.toInt() xor typeCode.toInt() xor
                valueInt.toInt() xor confInt.toInt()).toByte()

        return byteArrayOf(header, typeCode, valueInt, confInt, checksum)
    }

    /**
     * Convert command to string for serial transmission
     */
    fun toSerialString(): String {
        return "CMD:${type.code}:${(value * 100).toInt()}:${(confidence * 100).toInt()}\n"
    }

    companion object {
        val NONE = Command(type = CommandType.NONE)

        /**
         * Create command from ESP32 response bytes
         */
        fun fromBytes(bytes: ByteArray): Command? {
            if (bytes.size < 5) return null
            if (bytes[0] != 0xCC.toByte()) return null

            val typeCode = bytes[1].toInt() and 0xFF
            val value = (bytes[2].toInt() and 0xFF) / 255f
            val confidence = (bytes[3].toInt() and 0xFF) / 255f
            val checksum = bytes[4]

            // Verify checksum
            val calcChecksum: Byte = (bytes[0].toInt() xor bytes[1].toInt() xor
                    bytes[2].toInt() xor bytes[3].toInt()).toByte()
            if (calcChecksum != checksum) return null

            return Command(
                type = CommandType.fromCode(typeCode),
                value = value,
                confidence = confidence,
                source = CommandSource.SYSTEM
            )
        }

        /**
         * Create movement command
         */
        fun movement(
            type: CommandType,
            intensity: Float = 1f,
            source: CommandSource = CommandSource.EEG
        ): Command {
            require(type in listOf(
                CommandType.FORWARD, CommandType.BACKWARD,
                CommandType.TURN_LEFT, CommandType.TURN_RIGHT,
                CommandType.STOP
            )) { "Invalid movement command type" }

            return Command(
                type = type,
                value = intensity.coerceIn(0f, 1f),
                confidence = 1f,
                source = source,
                description = "Movement: ${type.displayName}"
            )
        }

        /**
         * Create emergency stop command
         */
        fun emergencyStop(
            source: CommandSource = CommandSource.SYSTEM,
            reason: String = "Emergency stop triggered"
        ): Command {
            return Command(
                type = CommandType.EMERGENCY_STOP,
                value = 0f,
                confidence = 1f,
                source = source,
                description = reason
            )
        }

        /**
         * Create alert command
         */
        fun alert(
            level: Float = 1f,
            reason: String = "Driver alert"
        ): Command {
            return Command(
                type = CommandType.ALERT_DRIVER,
                value = level.coerceIn(0f, 1f),
                confidence = 1f,
                source = CommandSource.SYSTEM,
                description = reason
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  COMMAND HISTORY
// ════════════════════════════════════════════════════════════════

data class CommandHistory(
    val commands: List<Command> = emptyList(),
    val maxSize: Int = 100
) {
    val size: Int get() = commands.size
    val isEmpty: Boolean get() = commands.isEmpty()
    val isNotEmpty: Boolean get() = commands.isNotEmpty()

    val lastCommand: Command? get() = commands.lastOrNull()

    fun add(command: Command): CommandHistory {
        val newList = (commands + command).takeLast(maxSize)
        return copy(commands = newList)
    }

    fun clear(): CommandHistory = copy(commands = emptyList())

    fun getByType(type: CommandType): List<Command> {
        return commands.filter { it.type == type }
    }

    fun getBySource(source: CommandSource): List<Command> {
        return commands.filter { it.source == source }
    }

    fun getInTimeRange(startTime: Long, endTime: Long): List<Command> {
        return commands.filter { it.timestamp in startTime..endTime }
    }

    fun getStatistics(): CommandStatistics {
        if (commands.isEmpty()) return CommandStatistics()

        val typeDistribution = commands.groupBy { it.type }
            .mapValues { it.value.size.toFloat() / commands.size }

        val sourceDistribution = commands.groupBy { it.source }
            .mapValues { it.value.size.toFloat() / commands.size }

        val avgConfidence = commands.map { it.confidence }.average().toFloat()

        val mostCommonType = typeDistribution.maxByOrNull { it.value }?.key
        val mostCommonSource = sourceDistribution.maxByOrNull { it.value }?.key

        return CommandStatistics(
            totalCommands = commands.size,
            typeDistribution = typeDistribution,
            sourceDistribution = sourceDistribution,
            averageConfidence = avgConfidence,
            mostCommonType = mostCommonType,
            mostCommonSource = mostCommonSource
        )
    }
}

data class CommandStatistics(
    val totalCommands: Int = 0,
    val typeDistribution: Map<CommandType, Float> = emptyMap(),
    val sourceDistribution: Map<CommandSource, Float> = emptyMap(),
    val averageConfidence: Float = 0f,
    val mostCommonType: CommandType? = null,
    val mostCommonSource: CommandSource? = null
)

















/*
package com.neurodrive.smartcar.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


// ════════════════════════════════════════════════════════════════
//  COMMAND TYPE (UNCHANGED)
// ════════════════════════════════════════════════════════════════

enum class CommandType(val code: Int, val displayName: String) {
    // Movement Commands
    FORWARD(1, "Forward"),
    BACKWARD(2, "Backward"),
    TURN_LEFT(3, "Turn Left"),
    TURN_RIGHT(4, "Turn Right"),
    STOP(5, "Stop"),

    // Speed Commands
    ACCELERATE(10, "Accelerate"),
    DECELERATE(11, "Decelerate"),
    CRUISE(12, "Cruise"),
    MAINTAIN_SPEED(13, "Maintain Speed"),

    // Safety Commands
    EMERGENCY_STOP(20, "Emergency Stop"),
    ALERT_DRIVER(21, "Alert Driver"),

    // Interaction Commands
    CONFIRM(30, "Confirm"),
    CANCEL(31, "Cancel"),
    SELECT(32, "Select"),

    // System Commands
    CALIBRATE(40, "Calibrate"),
    RESET(41, "Reset"),

    // No Action
    IDLE(98, "Idle"),
    NONE(0, "None");

    companion object {
        fun fromCode(code: Int): CommandType {
            return values().find { it.code == code } ?: NONE
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  COMMAND SOURCE (UNCHANGED)
// ════════════════════════════════════════════════════════════════

enum class CommandSource(val displayName: String) {
    EEG("Brain Signal"),
    EOG("Eye Movement"),
    COMBINED("Combined Signal"),
    MANUAL("Manual Input"),
    VOICE("Voice Command"),
    GESTURE("Gesture"),
    SYSTEM("System")
}

// ════════════════════════════════════════════════════════════════
//  COMMAND DATA CLASS (UNCHANGED)
// ════════════════════════════════════════════════════════════════

@Parcelize
data class Command(
    val id: Long = System.currentTimeMillis(),
    val type: CommandType,
    val value: Float = 0f,                    // Command intensity/value (0-1)
    val confidence: Float = 0f,               // Classification confidence (0-1)
    val source: CommandSource = CommandSource.EEG,
    val description: String = "",
    val mentalState: MentalState = MentalState.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis(),
    val isExecuted: Boolean = false,
    val executionResult: String = ""
) : Parcelable {

    // ════════════════════════════════════════
    //  COMPUTED PROPERTIES
    // ════════════════════════════════════════

    val isHighConfidence: Boolean
        get() = confidence >= 0.8f

    val isMediumConfidence: Boolean
        get() = confidence >= 0.6f && confidence < 0.8f

    val isLowConfidence: Boolean
        get() = confidence < 0.6f

    val isMovementCommand: Boolean
        get() = type in listOf(
            CommandType.FORWARD,
            CommandType.BACKWARD,
            CommandType.TURN_LEFT,
            CommandType.TURN_RIGHT,
            CommandType.STOP
        )

    val isSpeedCommand: Boolean
        get() = type in listOf(
            CommandType.ACCELERATE,
            CommandType.DECELERATE,
            CommandType.CRUISE,
            CommandType.MAINTAIN_SPEED
        )

    val isSafetyCommand: Boolean
        get() = type in listOf(
            CommandType.EMERGENCY_STOP,
            CommandType.ALERT_DRIVER
        )

    val isInteractionCommand: Boolean
        get() = type in listOf(
            CommandType.CONFIRM,
            CommandType.CANCEL,
            CommandType.SELECT
        )

    val confidencePercentage: Int
        get() = (confidence * 100).toInt()

    val valuePercentage: Int
        get() = (value * 100).toInt()

    // ════════════════════════════════════════
    //  DISPLAY METHODS
    // ════════════════════════════════════════

    fun getDisplayString(): String {
        return "${type.displayName} (${confidencePercentage}%)"
    }

    fun getDetailedString(): String {
        return buildString {
            append("Command: ${type.displayName}\n")
            append("Source: ${source.displayName}\n")
            append("Confidence: ${confidencePercentage}%\n")
            append("Value: ${valuePercentage}%\n")
            if (description.isNotBlank()) {
                append("Description: $description\n")
            }
            if (mentalState != MentalState.UNKNOWN) {
                append("Mental State: ${mentalState.displayName}")
            }
        }
    }

    // ════════════════════════════════════════
    //  ESP32 PACKET CONVERSION
    // ════════════════════════════════════════

    /**
     * Convert command to bytes for ESP32 transmission
     * Format: [HEADER(1)] [TYPE(1)] [VALUE(1)] [CONFIDENCE(1)] [CHECKSUM(1)]
     */
    fun toBytes(): ByteArray {
        val header: Byte = 0xCC.toByte()
        val typeCode: Byte = type.code.toByte()
        val valueInt: Byte = (value * 255).toInt().coerceIn(0, 255).toByte()
        val confInt: Byte = (confidence * 255).toInt().coerceIn(0, 255).toByte()

        val checksum: Byte = (header.toInt() xor typeCode.toInt() xor
                valueInt.toInt() xor confInt.toInt()).toByte()

        return byteArrayOf(header, typeCode, valueInt, confInt, checksum)
    }

    /**
     * Convert command to string for serial transmission
     */
    fun toSerialString(): String {
        return "CMD:${type.code}:${(value * 100).toInt()}:${(confidence * 100).toInt()}\n"
    }

    companion object {
        val NONE = Command(type = CommandType.NONE)

        /**
         * Create command from ESP32 response bytes
         */
        fun fromBytes(bytes: ByteArray): Command? {
            if (bytes.size < 5) return null
            if (bytes[0] != 0xCC.toByte()) return null

            val typeCode = bytes[1].toInt() and 0xFF
            val value = (bytes[2].toInt() and 0xFF) / 255f
            val confidence = (bytes[3].toInt() and 0xFF) / 255f
            val checksum = bytes[4]

            // Verify checksum
            val calcChecksum: Byte = (bytes[0].toInt() xor bytes[1].toInt() xor
                    bytes[2].toInt() xor bytes[3].toInt()).toByte()
            if (calcChecksum != checksum) return null

            return Command(
                type = CommandType.fromCode(typeCode),
                value = value,
                confidence = confidence,
                source = CommandSource.SYSTEM
            )
        }

        /**
         * Create movement command
         */
        fun movement(
            type: CommandType,
            intensity: Float = 1f,
            source: CommandSource = CommandSource.EEG
        ): Command {
            require(type in listOf(
                CommandType.FORWARD, CommandType.BACKWARD,
                CommandType.TURN_LEFT, CommandType.TURN_RIGHT,
                CommandType.STOP
            )) { "Invalid movement command type" }

            return Command(
                type = type,
                value = intensity.coerceIn(0f, 1f),
                confidence = 1f,
                source = source,
                description = "Movement: ${type.displayName}"
            )
        }

        /**
         * Create emergency stop command
         */
        fun emergencyStop(
            source: CommandSource = CommandSource.SYSTEM,
            reason: String = "Emergency stop triggered"
        ): Command {
            return Command(
                type = CommandType.EMERGENCY_STOP,
                value = 0f,
                confidence = 1f,
                source = source,
                description = reason
            )
        }

        /**
         * Create alert command
         */
        fun alert(
            level: Float = 1f,
            reason: String = "Driver alert"
        ): Command {
            return Command(
                type = CommandType.ALERT_DRIVER,
                value = level.coerceIn(0f, 1f),
                confidence = 1f,
                source = CommandSource.SYSTEM,
                description = reason
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  COMMAND HISTORY (UNCHANGED)
// ════════════════════════════════════════════════════════════════

data class CommandHistory(
    val commands: List<Command> = emptyList(),
    val maxSize: Int = 100
) {
    val size: Int get() = commands.size
    val isEmpty: Boolean get() = commands.isEmpty()
    val isNotEmpty: Boolean get() = commands.isNotEmpty()

    val lastCommand: Command? get() = commands.lastOrNull()

    fun add(command: Command): CommandHistory {
        val newList = (commands + command).takeLast(maxSize)
        return copy(commands = newList)
    }

    fun clear(): CommandHistory = copy(commands = emptyList())

    fun getByType(type: CommandType): List<Command> {
        return commands.filter { it.type == type }
    }

    fun getBySource(source: CommandSource): List<Command> {
        return commands.filter { it.source == source }
    }

    fun getInTimeRange(startTime: Long, endTime: Long): List<Command> {
        return commands.filter { it.timestamp in startTime..endTime }
    }

    fun getStatistics(): CommandStatistics {
        if (commands.isEmpty()) return CommandStatistics()

        val typeDistribution = commands.groupBy { it.type }
            .mapValues { it.value.size.toFloat() / commands.size }

        val sourceDistribution = commands.groupBy { it.source }
            .mapValues { it.value.size.toFloat() / commands.size }

        val avgConfidence = commands.map { it.confidence }.average().toFloat()

        val mostCommonType = typeDistribution.maxByOrNull { it.value }?.key
        val mostCommonSource = sourceDistribution.maxByOrNull { it.value }?.key

        return CommandStatistics(
            totalCommands = commands.size,
            typeDistribution = typeDistribution,
            sourceDistribution = sourceDistribution,
            averageConfidence = avgConfidence,
            mostCommonType = mostCommonType,
            mostCommonSource = mostCommonSource
        )
    }
}

data class CommandStatistics(
    val totalCommands: Int = 0,
    val typeDistribution: Map<CommandType, Float> = emptyMap(),
    val sourceDistribution: Map<CommandSource, Float> = emptyMap(),
    val averageConfidence: Float = 0f,
    val mostCommonType: CommandType? = null,
    val mostCommonSource: CommandSource? = null
)

// ════════════════════════════════════════════════════════════════
//  ACTIVITY TYPE (NEW)
// ════════════════════════════════════════════════════════════════


*/