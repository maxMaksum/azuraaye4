package com.azura.azuratime.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.azura.azuratime.repository.EmailAuthRepository
import com.azura.azuratime.utils.sha256
import com.azura.azuratime.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun EmailRegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    onAdminRegisterSuccess: () -> Unit = onRegisterSuccess // default fallback
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // FIX: get context at top
    val userViewModel: UserViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return UserViewModel(context.applicationContext as android.app.Application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    )
    val db = com.azura.azuratime.db.AppDatabase.getInstance(context)
    val phoneIdDao = db.phoneIdDao()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Register with Email", style = MaterialTheme.typography.headlineMedium)
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
                        // Get phone ID (Android ID) using context from composable
                        val phoneId = android.provider.Settings.Secure.getString(
                            context.applicationContext.contentResolver,
                            android.provider.Settings.Secure.ANDROID_ID
                        ) ?: ""
                        // Insert user into local DB with default role
                        val userEntity = com.azura.azuratime.db.UserEntity(
                            username = email,
                            passwordHash = password.sha256(),
                            role = "user",
                            phoneId = phoneId
                        )
                        userViewModel.registerUser(userEntity, onSuccess = {}, onError = { error = it })
                        // Store phoneId in new PhoneIdEntity table
                        phoneIdDao.insertPhoneId(com.azura.azuratime.db.PhoneIdEntity(username = email, phoneId = phoneId))
                        // Store phoneId in Firestore under 'users' collection
                        com.azura.azuratime.utils.FirebaseUtils.setDocument(
                            collection = "users",
                            documentId = email,
                            data = mapOf(
                                "username" to email,
                                "role" to "user",
                                "phoneId" to phoneId
                            )
                        )
                        // If admin, navigate to admin dashboard
                        if (userEntity.role == "admin") {
                            onAdminRegisterSuccess()
                            return@launch
                        }
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
            Text("Register")
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onBackToLogin) {
            Text("Back to Login")
        }
    }
}
