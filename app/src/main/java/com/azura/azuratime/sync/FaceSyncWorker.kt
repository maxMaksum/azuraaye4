package com.azura.azuratime.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.utils.FirestoreConverters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FaceSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.faceDao()
        val firestore = FirebaseFirestore.getInstance()
        val facesCollection = firestore.collection("Faces")
        return try {
            // 1. Upload all local faces to Firestore (upsert by studentId)
            val localFaces = dao.getAllFaces()
            for (face in localFaces) {
                // Skip syncing test users
                if (face.studentId.contains("test", ignoreCase = true)) continue
                val data = FirestoreConverters.faceToMap(face)
                facesCollection.document(face.studentId).set(data).await()
            }
            // 2. Download all faces from Firestore and upsert into Room
            val snapshot = facesCollection.get().await()
            val remoteFaces = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { FirestoreConverters.mapToFace(it) }
            }
            for (face in remoteFaces) {
                dao.insert(face) // OnConflictStrategy.REPLACE
            }
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
            val request = OneTimeWorkRequestBuilder<FaceSyncWorker>()
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
