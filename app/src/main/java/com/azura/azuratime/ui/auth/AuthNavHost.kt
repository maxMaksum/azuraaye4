package com.azura.azuratime.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.azura.azuratime.db.UserEntity
import com.azura.azuratime.session.SessionManager
import com.azura.azuratime.viewmodel.UserViewModel
import android.util.Log

@Composable
fun AuthNavHost(
    navController: NavHostController,
    startDestination: String = "login"
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userViewModel: UserViewModel = viewModel()

    // Handle back press to return to WelcomeScreen
    BackHandler(enabled = true) {
        if (navController.previousBackStackEntry == null) {
            // If there's no previous back stack entry, perform the desired action
            // For example, navigate to the welcome screen or show a confirmation dialog
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn()) {
            navController.navigate("dashboard") { popUpTo(0) }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("welcome") {
            WelcomeScreen(
                onLogin = { navController.navigate("login") },
                onSignup = { navController.navigate("register") },
                onEmailRegister = { navController.navigate("email_register") },
                onAdminRegister = { navController.navigate("admin_register") },
                onPhoneRegister = { navController.navigate("phone_register") },
                isDeviceRegistered = true, // integrityCheckPassed
                isUserAuthenticated = sessionManager.isLoggedIn(),
                onDeviceRegister = { navController.navigate("device_registration") }
            )
        }
        composable("email_register") {
            EmailRegisterScreen(
                onRegisterSuccess = { navController.navigate("login") },
                onBackToLogin = { navController.navigate("login") }
            )
        }
        composable("login") {
            LoginScreen(
                userViewModel = userViewModel,
                onLoginSuccess = { user ->
                    sessionManager.saveUserSession(user)
                    navController.navigate("dashboard") { popUpTo(0) }
                },
                onBackToSignup = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterUserScreen(
                onUserRegistered = { navController.popBackStack() },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("dashboard") {
            val role = sessionManager.getRole() ?: ""
            val name = sessionManager.getName() ?: ""
            DashboardScreen(
                role = role,
                name = name,
                isDeviceRegistered = true, // integrityCheckPassed
                isUserAuthenticated = sessionManager.isLoggedIn(),
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate("welcome") { popUpTo(0) }
                },
                onManageUsers = { navController.navigate("user_management") },
                onManageFaces = { navController.navigate("face_management") },
                onGoToMain = { /* onNavigateToMain() */ },
                onDatabaseSync = { navController.navigate("database_sync") },
                onDeveloperSettings = { navController.navigate("developer_settings") },
                onRegisterDevice = {
                    navController.navigate("device_registration")
                },
                userViewModel = userViewModel // Pass the viewmodel here
            )
        }
        composable("main_screen") {
            com.azura.azuratime.ui.MainScreen()
        }
        composable("user_management") {
            com.azura.azuratime.ui.admin.UserManagementScreen(
                onBack = { navController.popBackStack() },
                onNavigateToDeveloperSettings = { navController.navigate("developer_settings") }
            )
        }
        composable("face_management") {
            com.azura.azuratime.ui.admin.FaceManagementScreen(onBack = { navController.popBackStack() })
        }
        composable("developer_settings") {
            com.azura.azuratime.ui.settings.DeveloperSettingsScreen()
        }
        composable("database_sync") {
            com.azura.azuratime.ui.admin.DatabaseSyncScreen()
        }
        composable("admin_register") {
            AdminRegisterScreen(
                onRegisterSuccess = { navController.navigate("login") },
                onBackToLogin = { navController.navigate("login") }
            )
        }
        composable("phone_register") {
            val userId = sessionManager.getUserId() ?: ""
            PhoneRegistrationScreen(
                userId = userId,  // Pass user ID
                onSuccess = { key ->
                    userViewModel.setDynamicKey(key)
                    // onKeySet(key) // This callback is not used in the new structure
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("device_registration") {
            DeviceRegistrationScreen(
                navController = navController
            )
        }
    }
}
