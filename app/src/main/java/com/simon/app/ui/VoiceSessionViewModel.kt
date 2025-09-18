package com.simon.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceSessionUiState(
    val isConnected: Boolean = false,
    val isUserSpeaking: Boolean = false,
    val errorMessage: String? = null
)

class VoiceSessionViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(VoiceSessionUiState())
    val uiState: StateFlow<VoiceSessionUiState> = _uiState.asStateFlow()
    
    fun setConnected(connected: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isConnected = connected,
                errorMessage = null
            )
        }
    }
    
    fun setUserSpeaking(speaking: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUserSpeaking = speaking
            )
        }
    }
    
    fun setError(message: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(errorMessage = message)
        }
    }
}