package com.cadence.android.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cadence.android.data.repository.CadenceRepository

/**
 * Background WorkManager worker synchronizing offline outbox and pulling fresh schedule.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = CadenceRepository(applicationContext)
        val syncResult = repo.syncWithBackend()
        return if (syncResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
