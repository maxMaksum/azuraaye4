package com.azura.azuratime.repository

import android.util.Log
import com.azura.azuratime.db.UserDao
import com.azura.azuratime.db.UserEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(private val userDao: UserDao) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun loginFirebase(email: String, password: String): UserEntity? {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            if (firebaseUser != null) {
                // Optionally, fetch user profile from Firestore or create a UserEntity
                val user = UserEntity(
                    username = firebaseUser.email ?: firebaseUser.uid,
                    passwordHash = "", // Don't store password
                    name = firebaseUser.displayName ?: firebaseUser.email ?: "",
                    role = "user", // Set role as needed
                    createdAt = System.currentTimeMillis()
                )
                userDao.insertUser(user) // Cache for offline
                user
            } else null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Firebase login failed", e)
            null
        }
    }

    suspend fun loginOffline(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser() = firebaseAuth.currentUser
}
