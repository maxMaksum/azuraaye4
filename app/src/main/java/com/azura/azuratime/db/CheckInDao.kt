package com.azura.azuratime.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Insert
    suspend fun insert(checkIn: CheckInEntity)

    @Query("SELECT * FROM check_in ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_in WHERE isSynced = 0")
    suspend fun getUnsynced(): List<CheckInEntity>

    @Query("UPDATE check_in SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Int>)
}
