package com.azura.azuratime.ui.auth

import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.azura.azuratime.repository.AzureTimeRepositoryImpl
import com.azura.azuratime.session.SessionManager
import com.azura.azuratime.viewmodel.DeviceRegistrationViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch

@Composable
fun DeviceRegistrationScreen(navController: NavController) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { AzureTimeRepositoryImpl() }
    // Create ViewModel with proper factory syntax
    val viewModel: DeviceRegistrationViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DeviceRegistrationViewModel(repository, sessionManager)
            }
        }
    )
    val registrationState by viewModel.registrationState.collectAsState()
    val deviceId = remember {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }
    val userUid = sessionManager.getUid() ?: ""
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(registrationState) {
        when (registrationState) {
            is DeviceRegistrationViewModel.RegistrationState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Device registered successfully!")
                }
                navController.navigate("dashboard") {
                    popUpTo("device_registration") { inclusive = true }
                }
            }
            is DeviceRegistrationViewModel.RegistrationState.Error -> {
                val error = (registrationState as DeviceRegistrationViewModel.RegistrationState.Error).message
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Device Registration",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            when (registrationState) {
                is DeviceRegistrationViewModel.RegistrationState.Loading -> {
                    CircularProgressIndicator()
                }
                else -> {
                    Button(
                        onClick = { viewModel.registerDevice(deviceId) },
                        enabled = registrationState !is DeviceRegistrationViewModel.RegistrationState.Loading && userUid.isNotBlank()
                    ) {
                        Text("Register This Device")
                    }
                    if (userUid.isBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "User not authenticated. Please sign in first.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
