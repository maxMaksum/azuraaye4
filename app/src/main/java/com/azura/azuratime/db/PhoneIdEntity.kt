package com.azura.azuratime.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "phone_ids")
data class PhoneIdEntity(
    @PrimaryKey val username: String, // or userId if you have one
    val phoneId: String
)
