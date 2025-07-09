package com.azura.azuratime.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object UserFirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserPhoneId(email: String): String? {
        return try {
            val doc = db.collection("users").document(email).get().await()
            doc.getString("phoneId")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUserPhoneId(email: String, phoneId: String): Boolean {
        return try {
            db.collection("users").document(email).update("phoneId", phoneId).await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
