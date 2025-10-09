package com.simon.app.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceSessionUiState(
    val isConnected: Boolean = false,
    val isUserSpeaking: Boolean = false,
    val errorMessage: String? = null
)

class VoiceSessionViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceSessionUiState())
    val uiState = _uiState.asStateFlow()

    fun setConnected(connected: Boolean) {
        _uiState.value = _uiState.value.copy(isConnected = connected)
    }

    fun setUserSpeaking(speaking: Boolean) {
        _uiState.value = _uiState.value.copy(isUserSpeaking = speaking)
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
