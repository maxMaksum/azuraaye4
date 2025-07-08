package com.azura.azuratime.utils

import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.db.CheckInEntity
import com.azura.azuratime.db.UserEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

object FirestoreConverters {
    // ==================== FaceEntity Conversions ====================
    fun faceToMap(face: FaceEntity): Map<String, Any> {
        return mapOf(
            "studentId" to face.studentId,
            "name" to face.name,
            "photoUrl" to (face.photoUrl ?: ""),
            "embedding" to face.embedding.toList(),
            "className" to face.className,
            "subClass" to face.subClass,
            "grade" to face.grade,
            "subGrade" to face.subGrade,
            "program" to face.program,
            "role" to face.role,
            "timestamp" to face.timestamp
        )
    }

    fun mapToFace(map: Map<String, Any>): FaceEntity {
        return FaceEntity(
            studentId = map["studentId"] as String,
            name = map["name"] as String,
            photoUrl = map["photoUrl"] as? String,
            embedding = convertToFloatArray(map["embedding"] as List<Double>),
            className = map["className"] as? String ?: "",
            subClass = map["subClass"] as? String ?: "",
            grade = map["grade"] as? String ?: "",
            subGrade = map["subGrade"] as? String ?: "",
            program = map["program"] as? String ?: "",
            role = map["role"] as? String ?: "",
            timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis()
        )
    }

    fun documentToFace(doc: DocumentSnapshot): FaceEntity? {
        return doc.data?.let { mapToFace(it) }
    }

    // ==================== CheckInEntity Conversions ====================
    fun checkInToMap(checkIn: CheckInEntity): Map<String, Any> {
        return mapOf(
            "id" to checkIn.id,
            "studentId" to checkIn.studentId,
            //"embedding" to checkIn.embedding.toList(),
            "name" to checkIn.name,
            "timestamp" to checkIn.timestamp
        )
    }

    fun mapToCheckIn(map: Map<String, Any>): CheckInEntity {
        return CheckInEntity(
            id = (map["id"] as? Int) ?: 0,
            studentId = map["studentId"] as String,
            //embedding = convertToFloatArray(map["embedding"] as List<Double>),
            name = map["name"] as String,
            timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis()
        )
    }

    fun documentToCheckIn(doc: DocumentSnapshot): CheckInEntity? {
        return doc.data?.let { mapToCheckIn(it) }
    }

    // ==================== UserEntity Conversions ====================
    fun userToMap(user: UserEntity): Map<String, Any> {
        return mapOf(
            "username" to user.username,
            "passwordHash" to user.passwordHash,
            "name" to user.name,
            "role" to user.role,
            "createdAt" to user.createdAt
        )
    }

    fun mapToUser(map: Map<String, Any>): UserEntity {
        return UserEntity(
            username = map["username"] as String,
            passwordHash = map["passwordHash"] as String,
            name = map["name"] as String,
            role = map["role"] as String,
            createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
        )
    }

    fun documentToUser(doc: DocumentSnapshot): UserEntity? {
        return doc.data?.let { mapToUser(it) }
    }

    // ==================== Helper Functions ====================
    private fun convertToFloatArray(doubleList: List<Double>): FloatArray {
        return FloatArray(doubleList.size) { i ->
            doubleList[i].toFloat()
        }
    }
}