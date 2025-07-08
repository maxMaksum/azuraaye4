package com.azura.azuratime.repository

import android.content.Context
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.CheckInEntity
import com.azura.azuratime.sync.CheckInSyncWorker

class CheckInRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).checkInDao()

    suspend fun insertAndSync(checkIn: CheckInEntity) {
        dao.insert(checkIn)
        CheckInSyncWorker.enqueue(context)
    }
}
