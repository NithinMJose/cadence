package com.cadence.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.*
import com.cadence.android.sync.SyncWorker
import java.util.concurrent.TimeUnit

class CadenceApplication : Application() {

    companion object {
        const val SESSION_REMINDERS_CHANNEL_ID = "cadence_session_reminders"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        schedulePeriodicSync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_session_reminders)
            val descriptionText = getString(R.string.channel_session_reminders_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(SESSION_REMINDERS_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cadence_periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }
}
