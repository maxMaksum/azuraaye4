package com.azura.azuratime.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.azura.azuratime.utils.FirestoreInitializer

@Composable
fun DeveloperSettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("dev_settings", Context.MODE_PRIVATE) }
    var showFirestoreBtn by remember { 
        mutableStateOf(prefs.getBoolean("show_firestore_btn", false)) 
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Developer Settings", style = MaterialTheme.typography.headlineSmall)
        
        SwitchRow(
            checked = showFirestoreBtn,
            onCheckedChange = {
                showFirestoreBtn = it
                prefs.edit().putBoolean("show_firestore_btn", it).apply()
            },
            text = "Show Firestore Initialization Button"
        )

        Button(
            onClick = { FirestoreInitializer.resetInitialization(context) },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Reset Initialization Flag")
        }
    }
}

@Composable
fun SwitchRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}