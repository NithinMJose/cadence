package com.cadence.android.data.repository

import android.content.Context
import com.cadence.android.alarm.CadenceAlarmManager
import com.cadence.android.data.local.CadenceDatabase
import com.cadence.android.data.local.entity.CachedWeekSnapshotEntity
import com.cadence.android.data.local.entity.LocalTimeBlock
import com.cadence.android.data.local.entity.LocalTopicCard
import com.cadence.android.data.local.entity.PendingCommandEntity
import com.cadence.android.data.remote.*
import com.cadence.android.router.FastPathResult
import com.cadence.android.router.FastPathRouter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class CadenceRepository(private val context: Context) {

    private val db = CadenceDatabase.getInstance(context)
    private val scheduleDao = db.scheduleDao()
    private val topicCardDao = db.topicCardDao()
    private val pendingDao = db.pendingCommandDao()
    private val api = NetworkClient.getApiService(context)
    private val alarmManager = CadenceAlarmManager(context)
    val fastPathRouter = FastPathRouter(context)

    val blocksFlow: Flow<List<LocalTimeBlock>> = scheduleDao.getAllBlocksFlow()
    val snapshotFlow: Flow<CachedWeekSnapshotEntity?> = scheduleDao.getSnapshotFlow()
    val pendingCountFlow: Flow<Int> = pendingDao.getPendingCountFlow()
    val dueCardsFlow: Flow<List<LocalTopicCard>> = topicCardDao.getDueCardsFlow()

    suspend fun syncWithBackend(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            flushPendingCommands()

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

            val futureBlocks = scheduleDao.getFutureBlocks(nowMs)
            alarmManager.scheduleAlarmsForBlocks(futureBlocks)

            // Fetch and cache due cards for offline commute sprint
            val cardsResp = api.getDueCards()
            if (cardsResp.isSuccessful && cardsResp.body() != null) {
                val localCards = cardsResp.body()!!.map { c ->
                    LocalTopicCard(
                        id = c.id,
                        goalId = c.goalId,
                        title = c.title,
                        conceptSummary = c.conceptSummary,
                        topicTag = c.topicTag,
                        stability = c.stability,
                        difficulty = c.difficulty,
                        elapsedDays = c.elapsedDays,
                        scheduledDays = c.scheduledDays,
                        reps = c.reps,
                        lapses = c.lapses,
                        state = c.state,
                        emergencyFloorPrompt = c.emergencyFloorPrompt,
                        rubricJson = c.rubricJson,
                        nextReviewEpochMs = parseIsoToEpoch(c.nextReviewAt ?: ""),
                        lastReviewEpochMs = c.lastReviewAt?.let { parseIsoToEpoch(it) },
                        isLocallyGraded = false
                    )
                }
                topicCardDao.insertCards(localCards)
            }

            Result.success(localBlocks.size)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    suspend fun completeBlock(blockId: String, actualMin: Int = 45): FastPathResult = fastPathRouter.markDone(blockId)
    suspend fun downgradeToEmergencyFloor(blockId: String): FastPathResult = fastPathRouter.emergencyFloor(blockId)

    suspend fun gradeCardOptimistic(cardId: String, grade: Int): FastPathResult = withContext(Dispatchers.IO) {
        val startNano = System.nanoTime()
        topicCardDao.markLocallyGraded(cardId, true)
        pendingDao.insertCommand(
            PendingCommandEntity(
                commandType = "GRADE_CARD",
                targetId = cardId,
                payloadJson = "{\"grade\":$grade}"
            )
        )
        val elapsedMs = (System.nanoTime() - startNano) / 1_000_000
        try { flushPendingCommands() } catch (_: Exception) {}
        FastPathResult(true, "GRADE_CARD", cardId, "Graded card with rating $grade in ${elapsedMs}ms", elapsedMs)
    }

    suspend fun proposeReplan(prompt: String): Result<ReplanProposalResponseDto> = withContext(Dispatchers.IO) {
        try {
            val resp = api.proposeReplan(ReplanProposeRequestDto(userMessage = prompt))
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Proposal error ${resp.code()}: ${resp.message()}"))
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    suspend fun confirmReplan(proposalId: String): Result<ReplanConfirmResponseDto> = withContext(Dispatchers.IO) {
        try {
            val resp = api.confirmReplan(proposalId)
            if (resp.isSuccessful && resp.body() != null) {
                syncWithBackend()
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Confirm error ${resp.code()}: ${resp.message()}"))
            }
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    suspend fun flushPendingCommands() {
        val pending = pendingDao.getAllPending()
        for (cmd in pending) {
            try {
                when (cmd.commandType) {
                    "COMPLETE_BLOCK" -> if (cmd.targetId != null) {
                        val resp = api.completeBlock(cmd.targetId, CompleteBlockRequestDto(actualDurationMin = 45))
                        if (resp.isSuccessful) pendingDao.deleteCommand(cmd.commandId)
                    }
                    "GRADE_CARD" -> if (cmd.targetId != null) {
                        val grade = if (cmd.payloadJson.contains("\"grade\":1")) 1
                        else if (cmd.payloadJson.contains("\"grade\":2")) 2
                        else if (cmd.payloadJson.contains("\"grade\":4")) 4 else 3
                        val resp = api.reviewCard(cmd.targetId, CardReviewRequestDto(grade = grade))
                        if (resp.isSuccessful) pendingDao.deleteCommand(cmd.commandId)
                    }
                    else -> pendingDao.deleteCommand(cmd.commandId)
                }
            } catch (_: Exception) {
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
