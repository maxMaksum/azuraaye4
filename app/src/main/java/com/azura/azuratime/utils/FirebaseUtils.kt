package com.azura.azuratime.utils

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.db.CheckInEntity
import com.azura.azuratime.db.UserEntity
import kotlinx.coroutines.tasks.await

object FirebaseUtils {
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // Add or update a document
    fun setDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any>,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        db.collection(collection).document(documentId)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    // Add a document with auto-generated ID
    fun addDocument(
        collection: String,
        data: Map<String, Any>,
        onSuccess: ((String) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        db.collection(collection)
            .add(data)
            .addOnSuccessListener { docRef -> onSuccess?.invoke(docRef.id) }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    // Delete a document
    fun deleteDocument(
        collection: String,
        documentId: String,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        db.collection(collection).document(documentId)
            .delete()
            .addOnSuccessListener { onSuccess?.invoke() }
            .addOnFailureListener { e -> onFailure?.invoke(e) }
    }

    // ===== New functions with entity conversion =====
    fun setEntityDocument(
        collection: String,
        documentId: String,
        entity: Any,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val data = when (entity) {
            is FaceEntity -> FirestoreConverters.faceToMap(entity)
            is CheckInEntity -> FirestoreConverters.checkInToMap(entity)
            is UserEntity -> FirestoreConverters.userToMap(entity)
            else -> throw IllegalArgumentException("Unsupported entity type")
        }

        setDocument(collection, documentId, data, onSuccess, onFailure)
    }

    fun addEntityDocument(
        collection: String,
        entity: Any,
        onSuccess: ((String) -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val data = when (entity) {
            is FaceEntity -> FirestoreConverters.faceToMap(entity)
            is CheckInEntity -> FirestoreConverters.checkInToMap(entity)
            is UserEntity -> FirestoreConverters.userToMap(entity)
            else -> throw IllegalArgumentException("Unsupported entity type")
        }

        addDocument(collection, data, onSuccess, onFailure)
    }

    suspend fun getEntityDocument(
        collection: String,
        documentId: String,
        entityType: Class<*>
    ): Any? {
        return try {
            val document = db.collection(collection).document(documentId).get().await()
            when (entityType) {
                FaceEntity::class.java -> FirestoreConverters.documentToFace(document)
                CheckInEntity::class.java -> FirestoreConverters.documentToCheckIn(document)
                UserEntity::class.java -> FirestoreConverters.documentToUser(document)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
