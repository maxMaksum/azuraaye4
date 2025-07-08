package com.azura.azuratime.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.azura.azuratime.viewmodel.DatabaseSyncViewModel

@Composable
fun DatabaseSyncScreen(
    viewModel: DatabaseSyncViewModel = viewModel(
        viewModelStoreOwner = LocalViewModelStoreOwner.current!!
    )
) {
    val syncStatus by viewModel.syncStatus.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Database Sync & Status", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text("Pending records: $pendingCount")
        Text("Last sync: ${lastSyncTime ?: "Never"}")
        Text("Status: $syncStatus")
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.syncNow() }, enabled = !isSyncing) {
            if (isSyncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("Sync Now")
        }
    }
}
