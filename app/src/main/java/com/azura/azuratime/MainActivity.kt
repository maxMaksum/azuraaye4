package com.azura.azuratime

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import com.azura.azuratime.session.SessionManager

import com.azura.azuratime.ui.auth.AuthNavHost
import com.azura.azuratime.ui.auth.WelcomeScreen
import com.azura.azuratime.ml.FaceRecognizer
import com.azura.azuratime.util.InsertTestCheckInRecord
import com.azura.azuratime.viewmodel.UserViewModel
import com.azura.protect.NativeIntegrity
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private val _integrityCheckPassed = MutableStateFlow(false)
    // Simplified navigation state
    sealed class AppState {
        object Welcome : AppState()
        object Auth : AppState()
        object Main : AppState()
        object Loading : AppState()
        data class Error(val message: String) : AppState()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            userViewModel.dynamicKey.collect { key ->
                val isValid = if (!key.isNullOrEmpty()) {
                    Log.i("JAVA_DEBUG", "Passing key: '" + key + "' (hex: " + key.first().code.toString(16) + ")")
                    // NativeIntegrity.checkAppIntegrity(this@MainActivity, key) // REMOVED
                    true // or implement new integrity check here
                } else {
                    false
                }
                _integrityCheckPassed.value = isValid
            }
        }
        try {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            InsertTestCheckInRecord.insert(this)
            setContent {
                val hasKey by userViewModel.dynamicKey.collectAsState()
                val integrityPassed by _integrityCheckPassed.collectAsState()
                val context = LocalContext.current
                val sessionManager = remember { SessionManager(context) }
                val isUserAuthenticated by remember { mutableStateOf(sessionManager.isLoggedIn()) }
                // Navigation state management
                var appState by remember { mutableStateOf<AppState>(AppState.Welcome) }
                // Handle navigation state changes
                LaunchedEffect(hasKey, integrityPassed, isUserAuthenticated) {
                    when {
                        appState is AppState.Main -> return@LaunchedEffect
                        hasKey.isNullOrEmpty() -> appState = AppState.Welcome
                        !isUserAuthenticated -> appState = AppState.Welcome
                        integrityPassed -> appState = AppState.Main
                    }
                }
                when (val state = appState) {
                    AppState.Welcome -> {
                        WelcomeScreen(
                            onLogin = { appState = AppState.Auth },
                            onSignup = { appState = AppState.Auth },
                            onEmailRegister = { appState = AppState.Auth },
                            onAdminRegister = { appState = AppState.Auth },
                            onPhoneRegister = { appState = AppState.Auth },
                            isDeviceRegistered = integrityPassed,
                            isUserAuthenticated = isUserAuthenticated
                        )
                    }
                    AppState.Auth -> {
                        AuthNavHost(
                            navController = rememberNavController(),
                            startDestination = "login"
                        )
                    }
                    AppState.Main -> {
                        MainAppContent(integrityPassed)
                    }
                    AppState.Loading -> {
                        SplashScreen()
                    }
                    is AppState.Error -> {
                        ErrorScreen(state.message) {
                            appState = AppState.Welcome
                        }
                    }
                }
            }
            lifecycleScope.launch {
                _integrityCheckPassed.collect { passed ->
                    if (passed) {
                        withContext(Dispatchers.IO) {
                            try {
                                FaceRecognizer.initialize(applicationContext)
                                Log.d("FaceRecognizer", "Initialization successful")
                            } catch (e: Exception) {
                                Log.e("FaceRecognizer", "Initialization failed", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            setContent { 
                ErrorScreen(e.message ?: "Unknown error") {
                    finish()
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        FaceRecognizer.close()
    }
}

@Composable
fun MainAppContent(integrityPassed: Boolean) {
    if (!integrityPassed) {
        // Show lock screen if integrity check fails
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("App Locked", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text("A valid device key is required to unlock AzuraTime.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Text("Please log out and fetch your device key from the dashboard.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
    } else {
        // Show the real main app content with bottom navigation
        com.azura.azuratime.ui.MainScreen()
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Initializing AzuraTime...", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Error Occurred", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
