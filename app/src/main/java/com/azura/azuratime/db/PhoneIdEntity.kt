package com.azura.azuratime.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_ids")
data class PhoneIdEntity(
    @PrimaryKey val phoneId: String,
    val userId: String? = null,  // Add this field
    val email: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
