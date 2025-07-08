package com.azura.azuratime.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.sync.CheckInSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class DatabaseSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val _syncStatus = MutableStateFlow("Idle")
    val syncStatus: StateFlow<String> = _syncStatus

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val context get() = getApplication<Application>().applicationContext
    private val db = AppDatabase.getInstance(context)
    private val checkInDao = db.checkInDao()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val unsynced = withContext(Dispatchers.IO) { checkInDao.getUnsynced() }
            _pendingCount.value = unsynced.size
            // Optionally, load last sync time from persistent storage
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Syncing..."
            try {
                CheckInSyncWorker.enqueue(context)
                // Wait a bit and refresh status
                kotlinx.coroutines.delay(2000)
                refreshStatus()
                _syncStatus.value = "Success"
            } catch (e: Exception) {
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                _isSyncing.value = false
                _lastSyncTime.value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            }
        }
    }
}
