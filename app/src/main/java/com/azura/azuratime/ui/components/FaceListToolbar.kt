package com.azura.azuratime.ui.face

import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListToolbar(
    totalFaces: Int,
    onRefresh: () -> Unit,
    onDebug: () -> Unit,
    onCreateTest: () -> Unit,
    onVerifyDb: () -> Unit
) {
    TopAppBar(
        title = { Text("Face Management ($totalFaces)") },
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = onDebug) {
                Icon(Icons.Default.BugReport, contentDescription = "Debug Photos")
            }
            IconButton(onClick = onCreateTest) {
                Icon(Icons.Default.Add, contentDescription = "Create Test Face")
            }
            IconButton(onClick = onVerifyDb) {
                Icon(Icons.Default.Refresh, contentDescription = "Verify Database")
            }
        }
    )
}