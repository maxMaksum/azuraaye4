package com.azura.azuratime.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object EmailAuthRepository {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EmailAuthRepository", "Registration failed", e)
            Result.failure(e)
        }
    }

    fun sendEmailVerification(): Result<Unit> {
        val user = firebaseAuth.currentUser
        return if (user != null) {
            user.sendEmailVerification()
            Result.success(Unit)
        } else {
            Result.failure(Exception("No user logged in"))
        }
    }
}
