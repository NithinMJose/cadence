package com.cadence.android.data.local.dao

import androidx.room.*
import com.cadence.android.data.local.entity.LocalTopicCard
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicCardDao {

    @Query("SELECT * FROM local_topic_cards WHERE isLocallyGraded = 0 ORDER BY nextReviewEpochMs ASC")
    fun getDueCardsFlow(): Flow<List<LocalTopicCard>>

    @Query("SELECT * FROM local_topic_cards WHERE isLocallyGraded = 0 ORDER BY nextReviewEpochMs ASC")
    suspend fun getDueCards(): List<LocalTopicCard>

    @Query("SELECT * FROM local_topic_cards ORDER BY nextReviewEpochMs ASC")
    suspend fun getAllCards(): List<LocalTopicCard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<LocalTopicCard>)

    @Query("UPDATE local_topic_cards SET isLocallyGraded = :isGraded WHERE id = :id")
    suspend fun markLocallyGraded(id: String, isGraded: Boolean = true)

    @Query("DELETE FROM local_topic_cards")
    suspend fun clearCards()
}
