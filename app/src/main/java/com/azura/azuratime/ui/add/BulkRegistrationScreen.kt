// BulkRegistrationScreen.kt

package com.azura.azuratime.ui.add

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azura.azuratime.ui.components.AzuraOutlinedButton
import com.azura.azuratime.utils.FaceRegistrationHelper
import com.azura.azuratime.utils.PhotoProcessingUtils
import com.azura.azuratime.utils.PhotoStorageUtils
import com.azura.azuratime.viewmodel.BulkRegistrationViewModel
import com.azura.azuratime.viewmodel.FaceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRegistrationScreen(
    faceViewModel: FaceViewModel = viewModel(),
) {
    val context = LocalContext.current
    val bulkViewModel: BulkRegistrationViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BulkRegistrationViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BulkRegistrationViewModel(faceViewModel) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    })

    val bulkState by bulkViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var studentId by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var embedding by remember { mutableStateOf<FloatArray?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val bmp = PhotoStorageUtils.loadBitmapFromUri(context, it)
            if (bmp != null) {
                coroutineScope.launch {
                    val result = PhotoProcessingUtils.processBitmapForFaceEmbedding(context, bmp)
                    if (result != null) {
                        val (faceBitmap, emb) = result
                        bitmap = faceBitmap
                        embedding = emb
                        feedback = null
                    } else {
                        bitmap = bmp
                        embedding = null
                        feedback = "No face detected. Please try another photo."
                    }
                }
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(it)?.lowercase() ?: ""

            var name: String? = null
            contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    name = cursor.getString(nameIndex)
                }
            }

            val isCsv = mimeType.contains("csv") || name?.endsWith(".csv", ignoreCase = true) == true

            if (isCsv) {
                fileUri = it
                fileName = name ?: "selected_file.csv"
                bulkViewModel.resetState()
                bulkViewModel.prepareProcessing(context, it)
            } else {
                feedback = "Unsupported file type. Only CSV files are accepted"
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bulk Registration") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Single Registration
            Text("Single Registration", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = studentId,
                onValueChange = { studentId = it },
                label = { Text("ID") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { photoLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Photo")
            }

            Spacer(Modifier.height(12.dp))

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))
            }

            feedback?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(vertical = 8.dp))
            }

            Button(
                onClick = {
                    isProcessing = true
                    if (bitmap != null && embedding != null && name.isNotBlank() && studentId.isNotBlank()) {
                        val photoPath = PhotoStorageUtils.saveFacePhoto(context, bitmap!!, studentId)
                        if (photoPath != null) {
                            faceViewModel.registerFace(
                                studentId = studentId,
                                name = name,
                                embedding = embedding!!,
                                photoUrl = photoPath,
                                onSuccess = {
                                    feedback = "Registration successful!"
                                    isProcessing = false
                                },
                                onDuplicate = {
                                    feedback = "User already registered!"
                                    isProcessing = false
                                }
                            )
                        } else {
                            feedback = "Failed to save photo."
                            isProcessing = false
                        }
                    } else {
                        feedback = "Please fill all fields and select a valid photo."
                        isProcessing = false
                    }
                },
                enabled = !isProcessing && bitmap != null && embedding != null &&
                        name.isNotBlank() && studentId.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Register Student")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Batch Registration
            Text("Batch Registration", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))

            AzuraOutlinedButton(
                onClick = { fileLauncher.launch("*/*") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !bulkState.isProcessing,
                text = "Select CSV File"
            )

            fileName?.let { name ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = name.take(40).let { if (name.length > 40) "$it..." else it },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                fileUri = null
                                fileName = null
                                bulkViewModel.resetState()
                            }
                        ) {
                            Icon(Icons.Default.Close, "Remove file")
                        }
                    }
                }
            }

            if (fileUri != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    if (bulkState.estimatedTime.isNotEmpty()) {
                        Text(
                            text = bulkState.estimatedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (bulkState.currentPhotoType.isNotEmpty()) {
                        Text(bulkState.currentPhotoType, style = MaterialTheme.typography.bodySmall)
                    }

                    if (bulkState.currentPhotoSize.isNotEmpty()) {
                        Text(bulkState.currentPhotoSize, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (fileUri != null && !bulkState.isProcessing) {
                Button(
                    onClick = { bulkViewModel.processCsvFile(context, fileUri!!) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start Batch Processing")
                }
            }

            if (bulkState.isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LinearProgressIndicator(
                            progress = bulkState.progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(bulkState.status, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (bulkState.results.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing Results Summary:", style = MaterialTheme.typography.titleMedium)
                Text("Successful: ${bulkState.successCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Duplicates: ${bulkState.duplicateCount}", style = MaterialTheme.typography.bodyMedium)
                Text("Errors: ${bulkState.errorCount}", style = MaterialTheme.typography.bodyMedium)

                // Trigger refresh so FaceListScreen shows new faces
                LaunchedEffect(bulkState.results) {
                    com.azura.azuratime.db.FaceCache.refresh(context)
                    faceViewModel.reloadFaces(context)
                }
            }
        }
    }
}