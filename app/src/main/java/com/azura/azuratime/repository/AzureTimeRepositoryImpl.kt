package com.azura.azuratime.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AzureTimeRepositoryImpl : AzureTimeRepository {
    private val firestore = FirebaseFirestore.getInstance()

    override suspend fun registerDevice(deviceId: String, uid: String) {
        val deviceData = hashMapOf(
            "deviceId" to deviceId,
            "userId" to uid,
            "registeredAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("devices")
            .document(deviceId)
            .set(deviceData)
            .await()
    }
}
