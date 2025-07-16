package com.azura.azuratime.repository

interface AzureTimeRepository {
    suspend fun registerDevice(deviceId: String, uid: String)
}
