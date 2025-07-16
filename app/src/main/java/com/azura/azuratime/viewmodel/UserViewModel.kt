package com.azura.azuratime.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.azura.azuratime.db.AppDatabase
import com.azura.azuratime.db.UserEntity
import com.azura.azuratime.repository.AuthRepository
import com.azura.azuratime.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserEntity) : LoginState()
    data class Error(val message: String) : LoginState()
}

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getInstance(application).userDao()
    private val repository = UserRepository(userDao)
    private val authRepository = AuthRepository(userDao)

    private val _dynamicKey = MutableStateFlow<String?>(null)
    val dynamicKey: StateFlow<String?> get() = _dynamicKey

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> get() = _currentUser

    var loginState by mutableStateOf<LoginState>(LoginState.Idle)
        private set

    fun setDynamicKey(key: String) {
        viewModelScope.launch {
            _dynamicKey.value = key
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            loginState = LoginState.Loading
            try {
                delay(1000) // Simulate network delay
                val user = UserEntity(
                    username = username,
                    passwordHash = "hashed_$password",
                    name = "User Name",
                    role = "user",
                    phoneId = ""
                )
                _currentUser.value = user
                loginState = LoginState.Success(user)
            } catch (e: Exception) {
                loginState = LoginState.Error("Login failed: ${e.message}")
            }
        }
    }

    fun resetLoginState() {
        loginState = LoginState.Idle
    }

    private fun hashPassword(password: String): String {
        // Implement actual password hashing
        return "hashed_$password"
    }

    fun loginFirebase(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = authRepository.loginFirebase(email, password)
            if (user != null) {
                withContext(Dispatchers.Main) {
                    _currentUser.value = user
                    onSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("Firebase login failed")
                }
            }
        }
    }

    fun loginOffline(username: String, passwordHash: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUserByUsername(username)
            if (user != null && user.passwordHash == passwordHash) {
                withContext(Dispatchers.Main) {
                    _currentUser.value = user
                    onSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError("Username atau password salah")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.Main) {
            authRepository.logout()
            _currentUser.value = null
        }
    }

    fun registerUser(user: UserEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertUser(user)
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Gagal mendaftar user")
                }
            }
        }
    }

    fun getUsersByRole(role: String, onResult: (List<UserEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = repository.getUsersByRole(role)
            withContext(Dispatchers.Main) {
                onResult(users)
            }
        }
    }

    fun deleteUser(user: UserEntity, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteUser(user)
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun generateKeyFromServer() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.getIdToken(true)
            ?.addOnSuccessListener { result ->
                val idToken = result.token ?: return@addOnSuccessListener
                sendToServer(idToken)
            }
    }

    private fun sendToServer(idToken: String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = """{"idToken":"$idToken"}""".toRequestBody(mediaType)

        val request = Request.Builder()
            .url("http://192.168.1.8:5000/generate-key")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("AzuraKey", "Failed to send request: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        Log.d("AzuraKey", "Key received: $responseBody")
                        val keyData = JSONObject(responseBody)
                        val key = keyData.getJSONObject("key").getString("key")
                        _dynamicKey.value = key
                    }
                } else {
                    Log.e("AzuraKey", "Error: ${response.code}")
                }
            }
        })
    }
}
