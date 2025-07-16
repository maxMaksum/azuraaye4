package com.azura.azuratime.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azura.azuratime.session.SessionManager
import com.azura.azuratime.ui.components.AzuraFormField
import com.azura.azuratime.utils.sha256
import com.azura.azuratime.viewmodel.UserViewModel
import com.azura.azuratime.viewmodel.LoginState

@Composable
fun LoginScreen(
    userViewModel: UserViewModel,
    onLoginSuccess: (com.azura.azuratime.db.UserEntity) -> Unit,
    onBackToSignup: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Get context and sessionManager once at the top of the composable
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    // Handle login process
    LaunchedEffect(userViewModel.loginState) {
        when (userViewModel.loginState) {
            is LoginState.Success -> {
                userViewModel.currentUser.value?.let { user ->
                    // Save Firebase UID to SessionManager
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    firebaseUser?.uid?.let { uid ->
                        sessionManager.clearUid() // Clear old UID before saving new one
                        sessionManager.saveUid(uid)
                    }
                    onLoginSuccess(user)
                }
                userViewModel.resetLoginState()
            }
            is LoginState.Error -> {
                error = (userViewModel.loginState as LoginState.Error).message
                isLoading = false
                userViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = onBackToSignup) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Sign Up")
            }
        }
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        AzuraFormField(
            value = username,
            onValueChange = { username = it },
            label = "Username",
            isError = username.isBlank(),
            helperText = if (username.isBlank()) "Username is required" else null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        AzuraFormField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isError = password.isBlank(),
            helperText = if (password.isBlank()) "Password is required" else null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (username.isBlank() || password.isBlank()) {
                    error = "Please fill all fields"
                } else {
                    isLoading = true
                    error = null
                    userViewModel.login(username, password)
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Logging in..." else "Login")
        }
    }
}
