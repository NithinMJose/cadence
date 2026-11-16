package com.cadence.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cadence.android.data.local.dao.PendingCommandDao
import com.cadence.android.data.local.dao.ScheduleDao
import com.cadence.android.data.local.dao.TopicCardDao
import com.cadence.android.data.local.entity.CachedWeekSnapshotEntity
import com.cadence.android.data.local.entity.LocalTimeBlock
import com.cadence.android.data.local.entity.LocalTopicCard
import com.cadence.android.data.local.entity.PendingCommandEntity

@Database(
    entities = [
        CachedWeekSnapshotEntity::class,
        LocalTimeBlock::class,
        PendingCommandEntity::class,
        LocalTopicCard::class
    ],
    version = 2,
    exportSchema = false
)
abstract class CadenceDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun pendingCommandDao(): PendingCommandDao
    abstract fun topicCardDao(): TopicCardDao

    companion object {
        @Volatile
        private var INSTANCE: CadenceDatabase? = null

        fun getInstance(context: Context): CadenceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CadenceDatabase::class.java,
                    "cadence_local.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
