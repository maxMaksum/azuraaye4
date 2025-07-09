package com.azura.azuratime.ui.auth

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azura.azuratime.repository.EmailAuthRepository
import com.azura.azuratime.viewmodel.UserViewModel
import com.azura.azuratime.db.UserEntity
import com.azura.azuratime.utils.sha256
import kotlinx.coroutines.launch

@Composable
fun AdminRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val userViewModel: UserViewModel = viewModel()
    val context = LocalContext.current

    // Get phone ID (Android ID)
    val phoneId = remember {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: Build.SERIAL
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Admin Registration", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(Modifier.height(16.dp))
        Text("Phone ID: $phoneId", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (password != confirmPassword) {
                    error = "Passwords do not match"
                    return@Button
                }
                loading = true
                coroutineScope.launch {
                    val result = EmailAuthRepository.registerWithEmail(email, password)
                    if (result.isSuccess) {
                        val userEntity = UserEntity(
                            username = email,
                            passwordHash = password.sha256(),
                            role = "admin",
                            phoneId = phoneId // You may need to add this field to UserEntity
                        )
                        userViewModel.registerUser(userEntity, onSuccess = {}, onError = { error = it })
                        onRegisterSuccess()
                    } else {
                        error = result.exceptionOrNull()?.localizedMessage ?: "Registration failed"
                    }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register as Admin")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }
    }
}
