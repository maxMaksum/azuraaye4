package com.azura.azuratime.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.google.firebase.Timestamp

object FirestoreInitializer {
    private const val TAG = "FirestoreInit"
    private const val PREFS_NAME = "FirestoreInitializerPrefs"
    private const val PREF_INIT_KEY = "firestore_initialized_v1"

    // Initialize all collections (call this once during app setup)
    fun runInitialization(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(PREF_INIT_KEY, false)) {
            Log.d(TAG, "Collections already initialized")
            Toast.makeText(context, "Collections already initialized", Toast.LENGTH_SHORT).show()
            throw AlreadyInitializedException()
        }

        try {
            initializeClassOptions()
            initializeSubClassOptions()
            initializeGradeOptions()
            initializeSubGradeOptions()
            initializeProgramOptions()
            initializeRoleOptions()
            initializeFaces()
            initializeCheckIns()

            prefs.edit().putBoolean(PREF_INIT_KEY, true).apply()
            Log.d(TAG, "Firestore initialization completed")
            Toast.makeText(context, "Firestore initialized successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            Toast.makeText(context, "Initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }

    // Function to reset initialization (optional)
    fun resetInitialization(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_INIT_KEY).apply()
        Toast.makeText(context, "Initialization reset!", Toast.LENGTH_SHORT).show()
        // Immediately trigger Firestore collection creation after reset
        try {
            runInitialization(context)
        } catch (e: Exception) {
            // AlreadyInitializedException or other errors are handled in runInitialization
        }
    }

    private fun initializeClassOptions() {
        val data = mapOf(
            "name" to "Mathematics",
            "code" to "MATH101",
            "description" to "Core mathematics principles."
        )
        FirebaseUtils.setDocument(
            collection = "classOptions",
            documentId = "math_option",
            data = data,
            onSuccess = { Log.d(TAG, "classOptions initialized") },
            onFailure = { e -> Log.e(TAG, "classOptions error", e) }
        )
    }

    private fun initializeSubClassOptions() {
        val data = mapOf(
            "name" to "Algebra I",
            "parentClass" to "Mathematics",
            "difficulty" to "Beginner"
        )
        FirebaseUtils.setDocument(
            collection = "subClassOptions",
            documentId = "algebra_i_option",
            data = data
        )
    }

    private fun initializeGradeOptions() {
        val data = mapOf(
            "level" to 10,
            "description" to "Tenth Grade"
        )
        FirebaseUtils.setDocument(
            collection = "gradeOptions",
            documentId = "grade_10",
            data = data
        )
    }

    private fun initializeSubGradeOptions() {
        val data = mapOf(
            "name" to "Semester 1",
            "parentGrade" to "grade_10",
            "startDate" to "2023-09-01"
        )
        FirebaseUtils.setDocument(
            collection = "subGradeOptions",
            documentId = "semester_1",
            data = data
        )
    }

    private fun initializeProgramOptions() {
        val data = mapOf(
            "title" to "Science Enrichment Program",
            "duration" to "1 year",
            "targetAudience" to "Middle School"
        )
        FirebaseUtils.setDocument(
            collection = "programOptions",
            documentId = "science_program",
            data = data
        )
    }

    private fun initializeRoleOptions() {
        val data = mapOf(
            "name" to "Teacher",
            "permissions" to listOf("edit_grades", "manage_students")
        )
        FirebaseUtils.setDocument(
            collection = "roleOptions",
            documentId = "teacher_role",
            data = data
        )
    }

    private fun initializeFaces() {
        val data = mapOf(
            "userId" to "user123",
            "imageUrl" to "gs://your-bucket/user123_face.jpg",
            "detectionTime" to Timestamp.now()
        )
        FirebaseUtils.setDocument(
            collection = "faces",
            documentId = "user123_face_id",
            data = data
        )
    }

    private fun initializeCheckIns() {
        val data = mapOf(
            "userId" to "user456",
            "location" to "Main Hall",
            "timestamp" to Timestamp.now()
        )
        FirebaseUtils.setDocument(
            collection = "checkIns",
            documentId = "checkin_user456_${System.currentTimeMillis()}",
            data = data
        )
    }
}

class AlreadyInitializedException : Exception("Collections already initialized")