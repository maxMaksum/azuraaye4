package com.azura.azuratime.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azura.azuratime.repository.AzureTimeRepository
import com.azura.azuratime.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DeviceRegistrationViewModel(
    private val repository: AzureTimeRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    sealed class RegistrationState {
        object Idle : RegistrationState()
        object Loading : RegistrationState()
        object Success : RegistrationState()
        data class Error(val message: String) : RegistrationState()
    }

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    fun registerDevice(deviceId: String) {
        viewModelScope.launch {
            _registrationState.value = RegistrationState.Loading
            try {
                val uid = sessionManager.getUid()
                if (uid != null && deviceId.isNotBlank()) {
                    repository.registerDevice(deviceId, uid)
                    _registrationState.value = RegistrationState.Success
                } else {
                    _registrationState.value = RegistrationState.Error("User not authenticated or invalid device ID")
                }
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }
}
