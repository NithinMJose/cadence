package com.cadence.android.router

import android.content.Context
import com.cadence.android.alarm.CadenceAlarmManager
import com.cadence.android.data.local.CadenceDatabase
import com.cadence.android.data.local.entity.PendingCommandEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deterministic outcome of an on-device fast-path command execution.
 */
data class FastPathResult(
    val success: Boolean,
    val commandType: String,
    val targetId: String?,
    val message: String,
    val latencyMs: Long
)

/**
 * On-device Fast-Path Router executing offline actions (done, snooze, floor) in <10ms.
 */
class FastPathRouter(private val context: Context) {

    private val db = CadenceDatabase.getInstance(context)
    private val scheduleDao = db.scheduleDao()
    private val pendingDao = db.pendingCommandDao()
    private val alarmManager = CadenceAlarmManager(context)

    suspend fun markDone(blockId: String): FastPathResult = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()
        scheduleDao.updateBlockStatus(blockId, "COMPLETED")
        alarmManager.cancelAlarmForBlock(blockId)
        pendingDao.insertCommand(
            PendingCommandEntity(
                commandType = "COMPLETE_BLOCK",
                targetId = blockId,
                payloadJson = "{\"actual_duration_min\":45}"
            )
        )
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000
        FastPathResult(
            success = true,
            commandType = "COMPLETE_BLOCK",
            targetId = blockId,
            message = "Block marked completed in ${elapsedMs}ms",
            latencyMs = elapsedMs
        )
    }

    suspend fun snooze(blockId: String, minutes: Int = 30): FastPathResult = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()
        val block = scheduleDao.getBlockById(blockId)
            ?: return@withContext FastPathResult(false, "SNOOZE", blockId, "Block not found", 0L)

        val shiftMs = minutes.toLong() * 60_000L
        val newStart = block.startTimeEpochMs + shiftMs
        val newEnd = block.endTimeEpochMs + shiftMs

        scheduleDao.updateBlockTimes(blockId, newStart, newEnd)
        val updatedBlock = block.copy(startTimeEpochMs = newStart, endTimeEpochMs = newEnd)
        alarmManager.scheduleAlarmForBlock(updatedBlock)

        pendingDao.insertCommand(
            PendingCommandEntity(
                commandType = "SNOOZE",
                targetId = blockId,
                payloadJson = "{\"snooze_minutes\":$minutes,\"new_start_ms\":$newStart}"
            )
        )
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000
        FastPathResult(
            success = true,
            commandType = "SNOOZE",
            targetId = blockId,
            message = "Snoozed by ${minutes}m in ${elapsedMs}ms",
            latencyMs = elapsedMs
        )
    }

    suspend fun emergencyFloor(blockId: String): FastPathResult = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()
        scheduleDao.setEmergencyFloor(blockId, isFloor = true, durationMin = 3)
        pendingDao.insertCommand(
            PendingCommandEntity(
                commandType = "EMERGENCY_FLOOR",
                targetId = blockId,
                payloadJson = "{\"emergency_floor\":true,\"duration_min\":3}"
            )
        )
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000
        FastPathResult(
            success = true,
            commandType = "EMERGENCY_FLOOR",
            targetId = blockId,
            message = "Downgraded to 3-min Emergency Floor in ${elapsedMs}ms",
            latencyMs = elapsedMs
        )
    }

    suspend fun executeCommand(rawCommand: String): FastPathResult {
        val parts = rawCommand.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) {
            return FastPathResult(false, "UNKNOWN", null, "Empty command", 0L)
        }
        return when (parts[0].lowercase()) {
            "done" -> if (parts.size > 1) markDone(parts[1]) else FastPathResult(false, "DONE", null, "Missing blockId", 0L)
            "snooze" -> {
                val blockId = if (parts.size > 1) parts[1] else return FastPathResult(false, "SNOOZE", null, "Missing blockId", 0L)
                val mins = if (parts.size > 2) parts[2].replace("m", "").toIntOrNull() ?: 30 else 30
                snooze(blockId, mins)
            }
            "floor" -> if (parts.size > 1) emergencyFloor(parts[1]) else FastPathResult(false, "FLOOR", null, "Missing blockId", 0L)
            else -> FastPathResult(false, "UNKNOWN", null, "Unrecognized command: ${parts[0]}", 0L)
        }
    }
}
