package com.cadence.android.data.local.dao

import androidx.room.*
import com.cadence.android.data.local.entity.PendingCommandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingCommandDao {

    @Query("SELECT * FROM pending_commands ORDER BY createdAtEpochMs ASC")
    fun getAllPendingFlow(): Flow<List<PendingCommandEntity>>

    @Query("SELECT * FROM pending_commands ORDER BY createdAtEpochMs ASC")
    suspend fun getAllPending(): List<PendingCommandEntity>

    @Query("SELECT COUNT(*) FROM pending_commands")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: PendingCommandEntity)

    @Query("DELETE FROM pending_commands WHERE commandId = :commandId")
    suspend fun deleteCommand(commandId: String)

    @Query("DELETE FROM pending_commands")
    suspend fun clearAll()
}
