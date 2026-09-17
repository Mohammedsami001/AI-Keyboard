package com.aikeyboard.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aikeyboard.app.network.ApiClient
import com.aikeyboard.app.network.dto.AuthRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenStore: TokenStore
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val res = apiClient.api.login(AuthRequestDto(email, pass))
                tokenStore.saveTokens(res.accessToken, res.refreshToken)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }
    
    fun register(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val res = apiClient.api.register(AuthRequestDto(email, pass))
                tokenStore.saveTokens(res.accessToken, res.refreshToken)
                _authState.value = AuthState.Success
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Register failed")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val msg: String) : AuthState()
}
