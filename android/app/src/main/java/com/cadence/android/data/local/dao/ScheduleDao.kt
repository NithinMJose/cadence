package com.cadence.android.data.local.dao

import androidx.room.*
import com.cadence.android.data.local.entity.CachedWeekSnapshotEntity
import com.cadence.android.data.local.entity.LocalTimeBlock
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM cached_week_snapshot WHERE id = 1")
    fun getSnapshotFlow(): Flow<CachedWeekSnapshotEntity?>

    @Query("SELECT * FROM cached_week_snapshot WHERE id = 1")
    suspend fun getSnapshot(): CachedWeekSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: CachedWeekSnapshotEntity)

    @Query("SELECT * FROM local_time_blocks ORDER BY startTimeEpochMs ASC")
    fun getAllBlocksFlow(): Flow<List<LocalTimeBlock>>

    @Query("SELECT * FROM local_time_blocks ORDER BY startTimeEpochMs ASC")
    suspend fun getAllBlocks(): List<LocalTimeBlock>

    @Query("SELECT * FROM local_time_blocks WHERE startTimeEpochMs >= :startMs AND endTimeEpochMs <= :endMs ORDER BY startTimeEpochMs ASC")
    suspend fun getBlocksInWindow(startMs: Long, endMs: Long): List<LocalTimeBlock>

    @Query("SELECT * FROM local_time_blocks WHERE startTimeEpochMs > :nowMs ORDER BY startTimeEpochMs ASC")
    suspend fun getFutureBlocks(nowMs: Long): List<LocalTimeBlock>

    @Query("SELECT * FROM local_time_blocks WHERE id = :id LIMIT 1")
    suspend fun getBlockById(id: String): LocalTimeBlock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<LocalTimeBlock>)

    @Query("UPDATE local_time_blocks SET status = :status WHERE id = :id")
    suspend fun updateBlockStatus(id: String, status: String)

    @Query("UPDATE local_time_blocks SET isEmergencyFloor = :isFloor, durationMin = :durationMin WHERE id = :id")
    suspend fun setEmergencyFloor(id: String, isFloor: Boolean, durationMin: Int = 3)

    @Query("DELETE FROM local_time_blocks")
    suspend fun clearBlocks()
}
