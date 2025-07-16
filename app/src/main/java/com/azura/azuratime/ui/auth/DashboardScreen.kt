@file:OptIn(ExperimentalMaterial3Api::class)

package com.azura.azuratime.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.azura.azuratime.KeyStoreHelper
import com.azura.azuratime.ui.components.AzuraButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// Key fetch state sealed class
sealed class KeyFetchState {
    object Idle : KeyFetchState()
    object Loading : KeyFetchState()
    data class Success(val key: String, val signature: String) : KeyFetchState()
    data class Error(val message: String) : KeyFetchState()
}

fun isKeyValid(key: String?): Boolean {
    // Example: key must be at least 32 chars and alphanumeric
    return !key.isNullOrEmpty() && key.length >= 32 && key.all { it.isLetterOrDigit() }
}

@Composable
fun DashboardScreen(
    role: String,
    name: String,
    isDeviceRegistered: Boolean,
    isUserAuthenticated: Boolean,
    onLogout: () -> Unit,
    onManageUsers: (() -> Unit)? = null,
    onManageFaces: (() -> Unit)? = null,
    onGoToMain: (() -> Unit)? = null,
    onDatabaseSync: (() -> Unit)? = null,
    onDeveloperSettings: (() -> Unit)? = null,
    onRegisterDevice: (() -> Unit)? = null,
    userViewModel: com.azura.azuratime.viewmodel.UserViewModel
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { com.azura.azuratime.session.SessionManager(context) }
    val deviceId = remember { android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) }
    val userUid = sessionManager.getUid() ?: ""
    var showIds by remember { mutableStateOf(false) }
    var keyFetchState by remember { mutableStateOf<KeyFetchState>(KeyFetchState.Idle) }
    val key by userViewModel.dynamicKey.collectAsState()
    var showNoKeyDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Dashboard - $role") },
                actions = {
                    AzuraButton(onClick = {
                        sessionManager.clearUid() // Clear UID on logout
                        onLogout()
                    }, text = "Logout")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Device registration status banner
            DeviceRegistrationBanner(isRegistered = isDeviceRegistered)
            Spacer(Modifier.height(16.dp))
            // Button to show UUID and deviceId
            AzuraButton(
                onClick = { showIds = !showIds },
                text = if (showIds) "Hide UUID & Device ID" else "Show UUID & Device ID",
                modifier = Modifier.fillMaxWidth()
            )
            if (showIds) {
                Spacer(Modifier.height(8.dp))
                Text("UUID: $userUid", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("Device ID: $deviceId", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
            }
            // Device registration button (always enabled, always clickable)
            AzuraButton(
                onClick = onRegisterDevice ?: {},
                text = "Register This Device",
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            )
            Spacer(Modifier.height(16.dp))
            // Fetch Key button with state management
            AzuraButton(
                onClick = {
                    keyFetchState = KeyFetchState.Loading
                    coroutineScope.launch {
                        try {
                            val (key, signature) = fetchEncryptionKey(
                                deviceId = deviceId,
                                userUid = userUid,
                                secret = "12345"
                            )
                            keyFetchState = KeyFetchState.Success(key, signature)
                            saveEncryptedKeyWithKeystore(key, context)
                            userViewModel.setDynamicKey(key)
                        } catch (e: Exception) {
                            keyFetchState = KeyFetchState.Error(e.message ?: "Unknown error")
                        }
                    }
                },
                text = "Fetch Key",
                modifier = Modifier.fillMaxWidth(),
                enabled = keyFetchState !is KeyFetchState.Loading
            )
            // Show key fetch status
            when (keyFetchState) {
                is KeyFetchState.Loading -> {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
                is KeyFetchState.Success -> {
                    Spacer(Modifier.height(8.dp))
                    // Always use decrypted key from Keystore
                    val decryptedKey = loadDecryptedKeyWithKeystore(context)
                    val signature = (keyFetchState as KeyFetchState.Success).signature
                    Text("Generated Key: "+ (decryptedKey?.take(8) ?: "<none>") + "...", style = MaterialTheme.typography.bodyMedium)

                    // ✅ Call native verifyFetchedKey with decrypted key
                    LaunchedEffect(decryptedKey, signature) {
                        try {
                            if (decryptedKey != null) {
                                val result = com.azura.protect.NativeIntegrity.verifyFetchedKey(
                                    context, decryptedKey, signature, deviceId, userUid
                                )
                                android.util.Log.i("JNI-DEBUG", "Key HMAC verify result: $result")
                            } else {
                                android.util.Log.e("JNI-DEBUG", "Decrypted key is null!")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("JNI-DEBUG", "Key HMAC verify failed: ${e.message}")
                        }
                    }
                }
                is KeyFetchState.Error -> {
                    Spacer(Modifier.height(8.dp))
                    val error = (keyFetchState as KeyFetchState.Error).message
                    Text("Error: ${error}", color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
            // Dashboard content - fixed role check
            when (role) {
                "admin" -> AdminDashboard(
                    name = name,
                    isDeviceRegistered = isDeviceRegistered,
                    onManageUsers = { onManageUsers?.invoke() },
                    onManageFaces = { onManageFaces?.invoke() },
                    onGoToMain = onGoToMain,
                    onDatabaseSync = onDatabaseSync,
                    onDeveloperSettings = onDeveloperSettings,
                    onRegisterDevice = { onRegisterDevice?.invoke() },
                    hasKey = isKeyValid(key),
                    showNoKeyAlert = { showNoKeyDialog = true }
                )
                "guru" -> TeacherDashboard(
                    name = name,
                    onGoToMain = {
                        if (isDeviceRegistered) {
                            onGoToMain?.invoke()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Device not registered! Please contact admin.",
                                    withDismissAction = true
                                )
                            }
                        }
                    }
                )
                "siswa", "" -> UserDashboard(
                    name = name,
                    onGoToMain = {
                        if (isDeviceRegistered) {
                            onGoToMain?.invoke()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Device not registered! Please contact admin.",
                                    withDismissAction = true
                                )
                            }
                        }
                    },
                    hasKey = isKeyValid(key),
                    showNoKeyAlert = { showNoKeyDialog = true }
                )
                else -> UserDashboard(
                    name = name,
                    onGoToMain = {
                        if (isDeviceRegistered) {
                            onGoToMain?.invoke()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Device not registered! Please contact admin.",
                                    withDismissAction = true
                                )
                            }
                        }
                    },
                    hasKey = isKeyValid(key),
                    showNoKeyAlert = { showNoKeyDialog = true }
                )
            }
        }
    }
    if (showNoKeyDialog) {
        AlertDialog(
            onDismissRequest = { showNoKeyDialog = false },
            title = { Text("No Permission") },
            text = { Text("You have no permission. Please fetch your device key first.") },
            confirmButton = {
                Button(onClick = { showNoKeyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun DeviceRegistrationBanner(isRegistered: Boolean) {
    val backgroundColor = if (isRegistered) Color(0xFF4CAF50) else Color(0xFFF44336)
    val icon = if (isRegistered) Icons.Filled.CheckCircle else Icons.Filled.Warning
    val text = if (isRegistered) "Device Registered & Secure" else "Device Not Registered - Limited Functionality"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Registration Status",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AdminDashboard(
    name: String,
    isDeviceRegistered: Boolean,
    onManageUsers: () -> Unit,
    onManageFaces: () -> Unit,
    onGoToMain: (() -> Unit)? = null,
    onDatabaseSync: (() -> Unit)? = null,
    onDeveloperSettings: (() -> Unit)? = null,
    onRegisterDevice: (() -> Unit)? = null,
    hasKey: Boolean = true, // Add this parameter
    showNoKeyAlert: () -> Unit = {} // Add this callback
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // Name with registration status icon
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Welcome Admin $name!", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            if (isDeviceRegistered) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "Device Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Device Not Registered",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        // Device registration button (only show if device is not registered)
        if (!isDeviceRegistered && onRegisterDevice != null) {
            AzuraButton(
                onClick = { onRegisterDevice() },
                text = "Register This Device",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
        AzuraButton(onClick = onManageUsers, text = "Manage Users", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = onManageFaces, text = "Manage Face Data", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = { onDatabaseSync?.invoke() }, text = "Database Sync & Status", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = { onDeveloperSettings?.invoke() }, text = "Developer Settings", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = {
            if (hasKey) {
                onGoToMain?.invoke()
            } else {
                showNoKeyAlert()
            }
        }, text = "Go to Main Menu", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun TeacherDashboard(name: String, onGoToMain: (() -> Unit)? = null) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Welcome Teacher $name!", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        AzuraButton(onClick = { /* TODO */ }, text = "View Attendance", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = { /* TODO */ }, text = "Manage Classes", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = { onGoToMain?.invoke() }, text = "Go to Main Menu", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun UserDashboard(
    name: String,
    onGoToMain: (() -> Unit)? = null,
    hasKey: Boolean = true, // Add this parameter
    showNoKeyAlert: () -> Unit = {} // Add this callback
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Welcome $name!", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        AzuraButton(onClick = { /* TODO */ }, text = "Check In", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = { /* TODO */ }, text = "View Attendance History", modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        AzuraButton(onClick = {
            if (hasKey) {
                onGoToMain?.invoke()
            } else {
                showNoKeyAlert()
            }
        }, text = "Go to Main Menu", modifier = Modifier.fillMaxWidth())
    }
}

fun saveEncryptedKeyWithKeystore(key: String, context: android.content.Context) {
    KeyStoreHelper.generateKeyIfNeeded()
    val (iv, cipherText) = KeyStoreHelper.encrypt(key)
    val prefs = context.getSharedPreferences("CryptoPrefs", android.content.Context.MODE_PRIVATE)
    prefs.edit()
        .putString("key_iv", android.util.Base64.encodeToString(iv, android.util.Base64.DEFAULT))
        .putString("key_cipher", android.util.Base64.encodeToString(cipherText, android.util.Base64.DEFAULT))
        .apply()
}

fun loadDecryptedKeyWithKeystore(context: android.content.Context): String? {
    val prefs = context.getSharedPreferences("CryptoPrefs", android.content.Context.MODE_PRIVATE)
    val ivStr = prefs.getString("key_iv", null)
    val cipherStr = prefs.getString("key_cipher", null)
    if (ivStr == null || cipherStr == null) return null
    val iv = android.util.Base64.decode(ivStr, android.util.Base64.DEFAULT)
    val cipherText = android.util.Base64.decode(cipherStr, android.util.Base64.DEFAULT)
    return KeyStoreHelper.decrypt(iv, cipherText)
}

private suspend fun fetchEncryptionKey(
    deviceId: String,
    userUid: String,
    secret: String
): Pair<String, String> = withContext(Dispatchers.IO) {
    val url = "http://192.168.1.8:3000/generateKey"
    val jsonBody = org.json.JSONObject().apply {
        put("phoneId", deviceId)
        put("uid", userUid)
        put("secret", secret)
    }.toString()
    val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(url)
        .post(requestBody)
        .addHeader("Content-Type", "application/json")
        .build()
    val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string()
    if (!response.isSuccessful) {
        throw Exception("HTTP ${response.code}: ${responseBody ?: "No response"}")
    }
    try {
        val json = org.json.JSONObject(responseBody ?: throw Exception("Empty response"))
        if (json.has("error")) {
            throw Exception(json.getString("error"))
        }
        val key = json.getString("key")
        if (!json.has("signature")) {
            throw Exception("No value for signature in backend response")
        }
        val signature = json.getString("signature")
        key to signature
    } catch (e: Exception) {
        throw Exception("Invalid response: ${e.message}")
    }
}
