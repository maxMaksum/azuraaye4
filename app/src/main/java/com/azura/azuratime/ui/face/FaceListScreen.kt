package com.azura.azuratime.ui.face

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azura.azuratime.db.FaceEntity
import com.azura.azuratime.viewmodel.FaceViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.azura.azuratime.sync.FaceSyncWorker
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(
    viewModel: FaceViewModel = viewModel(),
    onEditUser: (FaceEntity) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val allFaces by viewModel.faceList.collectAsStateWithLifecycle(emptyList())
    val filteredFaces = allFaces.filter { face ->
        face.name.contains(searchQuery, ignoreCase = true)
    }

    var editingFace by remember { mutableStateOf<FaceEntity?>(null) }
    var editName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            FaceListToolbar(
                totalFaces = allFaces.size,
                onRefresh = { /* implement refresh logic */ },
                onDebug = { /* implement debug logic */ },
                onCreateTest = { /* implement test face creation */ },
                onVerifyDb = { /* implement db verification */ }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { FaceSyncWorker.enqueue(context) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Sync, contentDescription = "Sync Faces")
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            FaceListSearchBar(searchQuery, onQueryChange = { searchQuery = it })
            Spacer(Modifier.height(16.dp))

            if (filteredFaces.isEmpty()) {
                FaceListEmptyState(allFaces.size, filteredFaces.size, searchQuery)
            } else {
                FaceList(
                    faces = filteredFaces,
                    onEdit = { face ->
                        editingFace = face
                        editName = face.name
                    },
                    onEditPhoto = { /* implement photo edit logic */ },
                    onDelete = { face -> viewModel.deleteFace(face) }
                )
            }
        }
    }

    // Show edit dialog when needed
    editingFace?.let { face ->
        EditFaceDialog(
            face = face,
            name = editName,
            onNameChange = { editName = it },
            onSave = {
                viewModel.updateFace(face.copy(name = editName)) { editingFace = null }
            },
            onCancel = { editingFace = null }
        )
    }
}