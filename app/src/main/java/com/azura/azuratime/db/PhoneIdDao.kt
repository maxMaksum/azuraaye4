package com.azura.azuratime.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhoneIdDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoneId(phoneIdEntity: PhoneIdEntity)

    @Query("SELECT * FROM phone_ids WHERE phoneId = :phoneId LIMIT 1")
    suspend fun getPhoneRegistration(phoneId: String): PhoneIdEntity?
}
