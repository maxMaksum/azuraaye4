package com.azura.azuratime.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.azura.azuratime.db.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class CheckInSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        // Cooldown: Only sync if at least 1 minute has passed since last sync
        val prefs = applicationContext.getSharedPreferences("checkin_sync_prefs", Context.MODE_PRIVATE)
        val lastSync = prefs.getLong("last_sync_time", 0L)
        val now = System.currentTimeMillis()
        if (now - lastSync < 60_000) {
            // Less than 1 minute since last sync, skip
            return Result.success()
        }
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.checkInDao()
        val firestore = FirebaseFirestore.getInstance()
        return try {
            val unsynced = dao.getUnsynced()
            if (unsynced.isEmpty()) return Result.success()
            val batch = firestore.batch()
            val checkInsRef = firestore.collection("Checkins")
            val idsToMark = mutableListOf<Int>()
            for (checkIn in unsynced) {
                // Skip syncing test users
                if (checkIn.studentId.contains("test", ignoreCase = true)) continue
                val doc = checkInsRef.document()
                val data = hashMapOf(
                    "studentId" to checkIn.studentId,
                    "name" to checkIn.name,
                    "timestamp" to checkIn.timestamp,
                    "syncedAt" to Date().time
                )
                batch.set(doc, data)
                idsToMark.add(checkIn.id)
            }
            batch.commit().await()
            dao.markAsSynced(idsToMark)
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<CheckInSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
