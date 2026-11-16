package com.cadence.android.data.repository

import android.content.Context
import com.cadence.android.alarm.CadenceAlarmManager
import com.cadence.android.data.local.CadenceDatabase
import com.cadence.android.data.local.entity.CachedWeekSnapshotEntity
import com.cadence.android.data.local.entity.LocalTimeBlock
import com.cadence.android.data.local.entity.PendingCommandEntity
import com.cadence.android.data.remote.CompleteBlockRequestDto
import com.cadence.android.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CadenceRepository(private val context: Context) {

    private val db = CadenceDatabase.getInstance(context)
    private val scheduleDao = db.scheduleDao()
    private val pendingDao = db.pendingCommandDao()
    private val api = NetworkClient.getApiService(context)
    private val alarmManager = CadenceAlarmManager(context)

    val blocksFlow: Flow<List<LocalTimeBlock>> = scheduleDao.getAllBlocksFlow()
    val snapshotFlow: Flow<CachedWeekSnapshotEntity?> = scheduleDao.getSnapshotFlow()
    val pendingCountFlow: Flow<Int> = pendingDao.getPendingCountFlow()

    suspend fun syncWithBackend(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Flush any queued offline commands first
            flushPendingCommands()

            // 2. Fetch fresh 7-day schedule from backend
            val response = api.getWeeklySchedule()
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }

            val data = response.body()!!
            val nowMs = System.currentTimeMillis()
            val startMs = parseIsoToEpoch(data.windowStart)
            val endMs = parseIsoToEpoch(data.windowEnd)

            val snapshot = CachedWeekSnapshotEntity(
                windowStartEpochMs = startMs,
                windowEndEpochMs = endMs,
                timezone = data.timezone,
                flexCreditsMinutes = data.flexCreditsMinutes,
                recoveryModeActive = data.recoveryModeActive,
                totalBlocksCount = data.totalBlocks,
                lastSyncedEpochMs = nowMs
            )
            scheduleDao.insertSnapshot(snapshot)

            val localBlocks = data.blocks.map { b ->
                LocalTimeBlock(
                    id = b.id,
                    title = b.title,
                    topicTag = b.topicTag,
                    startTimeEpochMs = parseIsoToEpoch(b.startTime),
                    endTimeEpochMs = parseIsoToEpoch(b.endTime),
                    status = b.status,
                    deviceType = b.deviceType,
                    durationMin = b.plannedDurationMin,
                    isEmergencyFloor = b.isEmergencyFloor,
                    warmupBriefJson = b.warmupBriefJson
                )
            }
            scheduleDao.insertBlocks(localBlocks)

            // 3. Register exact local alarms for all upcoming blocks
            val futureBlocks = scheduleDao.getFutureBlocks(nowMs)
            alarmManager.scheduleAlarmsForBlocks(futureBlocks)

            Result.success(localBlocks.size)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    suspend fun completeBlock(blockId: String, actualMin: Int) = withContext(Dispatchers.IO) {
        // Optimistic offline update in <10ms
        scheduleDao.updateBlockStatus(blockId, "COMPLETED")
        pendingDao.insertCommand(
            PendingCommandEntity(
                commandType = "COMPLETE_BLOCK",
                targetId = blockId,
                payloadJson = "{\"actual_duration_min\":$actualMin}"
            )
        )
        // Fire-and-forget background flush attempt
        try { flushPendingCommands() } catch (_: Exception) {}
    }

    suspend fun downgradeToEmergencyFloor(blockId: String) = withContext(Dispatchers.IO) {
        // Optimistic offline update in <10ms
        scheduleDao.setEmergencyFloor(blockId, isFloor = true, durationMin = 3)
        pendingDao.insertCommand(
            PendingCommandEntity(
                commandType = "EMERGENCY_FLOOR",
                targetId = blockId,
                payloadJson = "{\"emergency_floor\":true}"
            )
        )
    }

    private suspend fun flushPendingCommands() {
        val pending = pendingDao.getAllPending()
        for (cmd in pending) {
            try {
                if (cmd.commandType == "COMPLETE_BLOCK" && cmd.targetId != null) {
                    val resp = api.completeBlock(cmd.targetId, CompleteBlockRequestDto(actualDurationMin = 45))
                    if (resp.isSuccessful) pendingDao.deleteCommand(cmd.commandId)
                } else {
                    pendingDao.deleteCommand(cmd.commandId)
                }
            } catch (_: Exception) {
                // Keep in outbox for next retry
                break
            }
        }
    }

    private fun parseIsoToEpoch(isoStr: String): Long {
        return try {
            ZonedDateTime.parse(isoStr, DateTimeFormatter.ISO_DATE_TIME).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
