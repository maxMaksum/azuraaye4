package com.azura.azuratime.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * In‑memory cache for face embeddings to avoid repeated database reads.
 */
object FaceCache {
    private val cache = mutableListOf<Pair<String, FloatArray>>()

    // --- Cooling down logic ---
    private val lastCheckInMap = mutableMapOf<String, Long>()
    private const val COOLDOWN_MILLIS = 60_000L // 1 minute
    private const val PREFS_NAME = "azura_checkin_cooldown"

    /**
     * Returns true if the studentId is allowed to check in (not within cooldown).
     * Call this before saving a check-in.
     *
     * Disables check-in for test users (studentId or name contains 'test').
     * Persists cooldown state in SharedPreferences for robustness.
     */
    fun canCheckIn(studentId: String, context: Context? = null): Boolean {
        val idLower = studentId.lowercase()
        if (idLower.contains("test")) return false
        val now = System.currentTimeMillis()
        val last = lastCheckInMap[studentId] ?: run {
            // Try to load from SharedPreferences if context is provided
            context?.let {
                val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getLong(studentId, 0L)
            } ?: 0L
        }
        return (now - last) > COOLDOWN_MILLIS
    }

    /**
     * Call this after a successful check-in to update the last check-in time.
     * Also persists to SharedPreferences if context is provided.
     */
    fun recordCheckIn(studentId: String, context: Context? = null) {
        val now = System.currentTimeMillis()
        lastCheckInMap[studentId] = now
        context?.let {
            val prefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(studentId, now).apply()
        }
    }
    // --- End cooling down logic ---

    /**
     * Loads all faces from the database on the IO dispatcher, caching them the first time.
     */
    suspend fun load(context: Context): List<Pair<String, FloatArray>> =
        withContext(Dispatchers.IO) {
            if (cache.isEmpty()) {
                Log.d("FaceCache", "Cache is empty, loading from database")
                // Retrieve all stored FaceEntity objects
                val faces = AppDatabase
                    .getInstance(context)
                    .faceDao()
                    .getAllFaces()

                Log.d("FaceCache", "Loaded ${faces.size} faces from database")
                faces.forEachIndexed { index, face ->
                    Log.d("FaceCache", "Face $index: ${face.name} (${face.studentId}), embedding size: ${face.embedding.size}")
                }

                // Map entities to name-embedding pairs and bulk-add to cache
                val pairs = faces.map { faceEntity: FaceEntity ->
                    faceEntity.name to faceEntity.embedding
                }

                cache.addAll(pairs)
                Log.d("FaceCache", "Added ${pairs.size} faces to cache")
            } else {
                Log.d("FaceCache", "Using cached data: ${cache.size} faces")
            }
            cache
        }

    /**
     * Loads all faces with their student IDs from the database.
     */
    suspend fun loadWithStudentIds(context: Context): List<Triple<String, String, FloatArray>> =
        withContext(Dispatchers.IO) {
            // Retrieve all stored FaceEntity objects
            val faces = AppDatabase
                .getInstance(context)
                .faceDao()
                .getAllFaces()

            // Map entities to studentId-name-embedding triples
            faces.map { faceEntity: FaceEntity ->
                Triple(faceEntity.studentId, faceEntity.name, faceEntity.embedding)
            }
        }

    /**
     * Clears the in-memory cache.
     */
    fun clear() {
        Log.d("FaceCache", "Clearing cache (had ${cache.size} faces)")
        cache.clear()
    }

    /**
     * Refreshes the cache by clearing and reloading from database.
     * Use this only when you know the database has been updated.
     */
    suspend fun refresh(context: Context): List<Pair<String, FloatArray>> {
        clear()
        return load(context)
    }
}
