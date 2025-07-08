package com.azura.azuratime.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_in")
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: String,
    val name: String,
    val timestamp: Long,
    val isSynced: Boolean = false // New field for sync status
)
