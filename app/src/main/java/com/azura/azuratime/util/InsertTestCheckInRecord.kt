package com.azura.azuratime.util

import android.content.Context
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.CheckInEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InsertTestCheckInRecord {
    fun insert(context: Context) {
        val db = AppDatabase.getInstance(context)
        val dao = db.checkInDao()
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(
                CheckInEntity(
                    studentId = "test_student_id",
                    name = "Test User",
                    timestamp = System.currentTimeMillis(),
                    isSynced = false
                )
            )
        }
    }
}
