package com.azura.azuratime.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azura.azuratime.ui.components.AzuraButton
import com.azura.azuratime.ui.components.AzuraOutlinedButton

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onSignup: () -> Unit,
    onEmailRegister: () -> Unit,
    onAdminRegister: () -> Unit,
    onPhoneRegister: () -> Unit,
    onDeviceRegister: (() -> Unit)? = null,
    isDeviceRegistered: Boolean,
    isUserAuthenticated: Boolean
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to AzuraTime", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(32.dp))
            if (!isDeviceRegistered) {
                Text(
                    "Device registration required",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(16.dp))
            }
            if (!isUserAuthenticated) {
                Text(
                    "Please sign in to continue",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
            }
            AzuraButton(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth(),
                text = "Sign In"
            )
            Spacer(Modifier.height(16.dp))
            AzuraOutlinedButton(
                onClick = onSignup,
                modifier = Modifier.fillMaxWidth(),
                text = "Create Account"
            )
            Spacer(Modifier.height(32.dp))
            Text("Device Registration", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AzuraOutlinedButton(
                onClick = onPhoneRegister,
                modifier = Modifier.fillMaxWidth(),
                text = "Register This Device",
                enabled = isUserAuthenticated && !isDeviceRegistered
            )
            AzuraButton(
                onClick = onDeviceRegister ?: {},
                modifier = Modifier.fillMaxWidth(),
                text = "Register This Device"
            )
            if (!isUserAuthenticated) {
                Text(
                    "Sign in to register your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isDeviceRegistered) {
                Text(
                    "Device already registered",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
