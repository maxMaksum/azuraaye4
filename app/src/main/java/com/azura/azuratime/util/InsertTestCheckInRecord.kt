package com.azura.azuratime.util

import android.content.Context
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.CheckInEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object InsertTestCheckInRecord {
    fun insert(context: Context) {
        try {
            val db = AppDatabase.getInstance(context)
            val dao = db.checkInDao()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    dao.insert(
                        CheckInEntity(
                            studentId = "test_student_id",
                            name = "Test User",
                            timestamp = System.currentTimeMillis(),
                            isSynced = false
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("InsertTestCheckInRecord", "Exception inserting test check-in", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("InsertTestCheckInRecord", "Exception in insert()", e)
        }
    }
}
