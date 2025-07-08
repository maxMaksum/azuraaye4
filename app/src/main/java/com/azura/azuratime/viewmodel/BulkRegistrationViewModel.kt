package com.azura.azuratime.viewmodel

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azura.azuratime.utils.BulkPhotoProcessor
import com.azura.azuratime.utils.CsvImportUtils
import com.azura.azuratime.utils.PhotoProcessingUtils
import com.azura.azuratime.utils.PhotoStorageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.azura.azuratime.model.ProcessResult

class BulkRegistrationViewModel(
    private val faceViewModel: FaceViewModel
) : ViewModel() {
    private val _state = MutableStateFlow(ProcessingState())
    val state: StateFlow<ProcessingState> = _state.asStateFlow()

    private var photoSources = listOf<String>()

    fun prepareProcessing(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                photoSources = csvResult.students.map { it.photoUrl }
                
                val seconds = BulkPhotoProcessor.estimateProcessingTime(photoSources)
                val estimate = when {
                    seconds > 120 -> "${seconds / 60} minutes"
                    seconds > 60 -> "1 minute ${seconds % 60} seconds"
                    else -> "$seconds seconds"
                }
                
                _state.value = _state.value.copy(
                    estimatedTime = "Estimated time: $estimate"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    estimatedTime = "Time estimate unavailable"
                )
            }
        }
    }

    fun processCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                _state.value = ProcessingState(
                    isProcessing = true,
                    status = "Parsing CSV file...",
                    estimatedTime = state.value.estimatedTime
                )

                val csvResult = CsvImportUtils.parseCsvFile(context, uri)
                if (csvResult.students.isEmpty()) {
                    _state.value = _state.value.copy(
                        isProcessing = false,
                        status = "No valid students found in CSV",
                        errorCount = csvResult.errors.size
                    )
                    return@launch
                }

                val results = mutableListOf<ProcessResult>()
                var successCount = 0
                var duplicateCount = 0
                var errorCount = 0
                val totalStudents = csvResult.students.size

                csvResult.students.forEachIndexed { index, student ->
                    try {
                        val photoType = BulkPhotoProcessor.getPhotoSourceType(student.photoUrl)
                        _state.value = _state.value.copy(
                            progress = (index + 1).toFloat() / totalStudents,
                            status = "Processing ${index + 1}/$totalStudents: ${student.name}",
                            currentPhotoType = "Photo source: $photoType",
                            currentPhotoSize = ""
                        )

                        val result = processStudent(context, student)
                        when {
                            result.status == "Registered" -> successCount++
                            result.status.startsWith("Duplicate") -> duplicateCount++
                            else -> errorCount++
                        }
                        results.add(result)
                        
                        _state.value = _state.value.copy(
                            currentPhotoSize = "Photo size: ${formatFileSize(result.photoSize)}"
                        )
                    } catch (e: Exception) {
                        errorCount++
                        results.add(
                            ProcessResult(
                                studentId = student.studentId,
                                name = student.name,
                                status = "Error",
                                error = e.message ?: "Unknown error"
                            )
                        )
                    }
                }

                _state.value = ProcessingState(
                    isProcessing = false,
                    results = results,
                    successCount = successCount,
                    duplicateCount = duplicateCount,
                    errorCount = errorCount,
                    status = "Processed $successCount students successfully"
                )
            } catch (e: Exception) {
                _state.value = ProcessingState(
                    isProcessing = false,
                    status = "Processing failed: ${e.message}",
                    errorCount = 1
                )
            }
        }
    }

    private suspend fun processStudent(
        context: Context,
        student: CsvImportUtils.CsvStudentData
    ): ProcessResult {
        val photoResult = BulkPhotoProcessor.processPhotoSource(
            context = context,
            photoSource = student.photoUrl,
            studentId = student.studentId
        )

        if (!photoResult.success) {
            return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = photoResult.error ?: "Photo processing failed",
                photoSize = photoResult.originalSize
            )
        }
        
        val bitmap = BitmapFactory.decodeFile(photoResult.localPhotoUrl)
            ?: return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = "Failed to load processed photo",
                photoSize = photoResult.originalSize
            )
        
        val embeddingResult = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bitmap)
            ?: return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = "No face detected",
                photoSize = photoResult.originalSize
            )

        val (faceBitmap, embedding) = embeddingResult

        val photoPath = PhotoStorageUtils.saveFacePhoto(context, faceBitmap, student.studentId)
            ?: return ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = "Failed to save face photo",
                photoSize = photoResult.originalSize
            )

        // Save to database using FaceViewModel
        faceViewModel.registerFace(
            studentId = student.studentId,
            name = student.name,
            embedding = embedding,
            photoUrl = photoPath,
            className = student.className ?: "",
            subClass = student.subClass ?: "",
            grade = student.grade ?: "",
            subGrade = student.subGrade ?: "",
            program = student.program ?: "",
            role = student.role ?: "",
            onSuccess = {},
            onDuplicate = {}
        )

        return try {
            ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Registered",
                photoSize = photoResult.processedSize
            )
        } catch (e: Exception) {
            ProcessResult(
                studentId = student.studentId,
                name = student.name,
                status = "Error",
                error = e.message ?: "Registration failed",
                photoSize = photoResult.originalSize
            )
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size == 0L -> "0 KB"
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }

    fun resetState() {
        _state.value = ProcessingState()
    }
}