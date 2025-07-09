package com.azura.azuratime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.CheckInEntity
import com.azura.azuratime.db.FaceCache
import com.azura.azuratime.sync.CheckInSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CheckInViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val checkInDao = database.checkInDao()
    private val context = application.applicationContext

    fun getAllCheckIns(): Flow<List<CheckInEntity>> {
        return checkInDao.getAll()
    }

    fun insertCheckIn(checkIn: CheckInEntity) {
        viewModelScope.launch {
            // Cooling down logic: only allow if not within cooldown
            if (FaceCache.canCheckIn(checkIn.studentId, context)) {
                checkInDao.insert(checkIn)
                FaceCache.recordCheckIn(checkIn.studentId, context)
                CheckInSyncWorker.enqueue(context)
            } else {
                // Optionally, you can show a message to the user here (e.g., via a callback or LiveData)
                // For now, just skip duplicate check-in
                // Log.d("CheckInViewModel", "Check-in blocked by cooldown for ${checkIn.studentId}")
            }
        }
    }
}
