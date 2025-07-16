package com.azura.azuratime.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.PhoneIdEntity
import kotlinx.coroutines.launch
import android.provider.Settings

@Composable
fun PhoneRegistrationScreen(
    userId: String,
    onSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }  // Add this line
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get phone ID (Android ID)
    val androidId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
    }

    LaunchedEffect(userId) {
        if (userId.isBlank()) {
            error = "Invalid user session"
        }
    }

    if (success) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Success",
                 tint = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))
            Text("Device Registered Successfully!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("Your device is now registered and secured", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onBack) {
                Text("Continue to App")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Device Registration", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("User Account", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("ID: $userId", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Device Information", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Device ID: $androidId", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
            }
            Button(
                onClick = {
                    loading = true
                    coroutineScope.launch {
                        try {
                            val db = AppDatabase.getInstance(context)
                            db.phoneIdDao().insertPhoneId(
                                PhoneIdEntity(
                                    phoneId = androidId,
                                    userId = userId,
                                    email = if (email.isBlank()) null else email
                                )
                            )
                            registerDeviceWithServer(userId, androidId)
                            val generatedKey = generateSecureKey(userId, androidId)
                            onSuccess(generatedKey)
                            success = true
                        } catch (e: Exception) {
                            error = e.localizedMessage ?: "Registration failed"
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register This Device")
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}

private suspend fun registerDeviceWithServer(userId: String, deviceId: String) {
    kotlinx.coroutines.delay(2000)
}

private fun generateSecureKey(userId: String, deviceId: String): String {
    val timestamp = System.currentTimeMillis()
    val baseString = "$userId|$deviceId|$timestamp"
    return "SECURE_KEY_${baseString.hashCode().toString(16).uppercase()}"
}
