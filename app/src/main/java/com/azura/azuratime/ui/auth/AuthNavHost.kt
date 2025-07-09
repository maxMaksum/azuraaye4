package com.azura.azuratime.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.azura.azuratime.session.SessionManager
import com.azura.azuratime.viewmodel.UserViewModel

@Composable
fun AuthNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val userViewModel: UserViewModel = viewModel()

    LaunchedEffect(Unit) {
        if (sessionManager.isLoggedIn()) {
            navController.navigate("dashboard") { popUpTo(0) }
        }
    }

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onLogin = { navController.navigate("login") },
                onSignup = { navController.navigate("register") },
                onEmailRegister = { navController.navigate("email_register") },
                onAdminRegister = { navController.navigate("admin_register") }
            )
        }
        composable("email_register") {
            EmailRegisterScreen(
                onRegisterSuccess = { navController.navigate("login") },
                onBackToLogin = { navController.navigate("login") }
            )
        }
        composable("login") {
            LoginScreen(userViewModel,
                onLoginSuccess = { role ->
                    // Always save session with fallback for name
                    val user = userViewModel.currentUser.value
                    if (user != null) {
                        val safeUser = user.copy(name = user.name ?: user.username)
                        sessionManager.saveUserSession(safeUser)
                    }
                    navController.navigate("dashboard") { popUpTo(0) }
                },
                onBackToSignup = { navController.navigate("register") }
            )
        }
        composable("dashboard") {
            val role = sessionManager.getRole() ?: ""
            val name = sessionManager.getName() ?: ""
            DashboardScreen(
                role = role,
                name = name,
                onLogout = {
                    sessionManager.clearSession()
                    navController.navigate("welcome") { popUpTo(0) }
                },
                onManageUsers = { navController.navigate("user_management") },
                onManageFaces = { navController.navigate("face_management") },
                onGoToMain = { navController.navigate("main_screen") { popUpTo(0) } },
                onDatabaseSync = { navController.navigate("database_sync") },
                onDeveloperSettings = { navController.navigate("developer_settings") }
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
        composable("register") {
            EmailRegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("login")
                },
                onBackToLogin = {
                    navController.navigate("login")
                }
            )
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
    }
}
