package com.simon.app.presentation

import android.media.AudioManager
import com.simon.app.config.ConfigManager
import com.simon.app.webrtc.OpenAIRealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.webrtc.PeerConnectionFactory

class VoiceSessionPresenter(
    private val configManager: ConfigManager,
    private val audioManager: AudioManager?,
    private val onSessionStarted: () -> Unit = {},
    private val onSessionError: (String) -> Unit = {},
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechStopped: () -> Unit = {},
    private val onResponseStarted: () -> Unit = {},
    private val onResponseCompleted: () -> Unit = {},
    private val onSessionEnded: () -> Unit = {}
) {
    
    private var openAIClient: OpenAIRealtimeClient? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isListening = false

    fun initialize(peerConnectionFactory: PeerConnectionFactory) {
        // Enable speaker
        enableSpeaker()
        
        // Initialize WebRTC client with API key from config
        val apiKey = configManager.getOpenAIApiKey()
        if (apiKey.isEmpty()) {
            onSessionError("API key not configured")
            return
        }
        
        openAIClient = OpenAIRealtimeClient(
            apiKey = apiKey,
            listener = createClientListener(),
            peerConnectionFactory = peerConnectionFactory
        )
    }

    fun startListening() {
        if (isListening) return
        
        isListening = true
        scope.launch {
            openAIClient?.connect()
        }
    }

    fun stopListening() {
        if (!isListening) return
        
        isListening = false
        openAIClient?.disconnect()
        onSessionEnded()
    }

    private fun enableSpeaker() {
        audioManager?.apply {
            mode = AudioManager.MODE_IN_COMMUNICATION
            val speakerDevice = availableCommunicationDevices.firstOrNull { 
                it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            speakerDevice?.let {
                setCommunicationDevice(it)
            }
        }
    }

    fun cleanup() {
        stopListening()
        scope.cancel()
        openAIClient = null
        audioManager?.apply {
            mode = AudioManager.MODE_NORMAL
            clearCommunicationDevice()
        }
    }

    private fun createClientListener() = object : OpenAIRealtimeClient.Listener {
        override fun onSessionStarted() = this@VoiceSessionPresenter.onSessionStarted()
        override fun onSessionEnded() = this@VoiceSessionPresenter.onSessionEnded()
        override fun onSpeechStarted() = this@VoiceSessionPresenter.onSpeechStarted()
        override fun onSpeechStopped() = this@VoiceSessionPresenter.onSpeechStopped()
        override fun onResponseStarted() = this@VoiceSessionPresenter.onResponseStarted()
        override fun onResponseCompleted() = this@VoiceSessionPresenter.onResponseCompleted()
        override fun onError(error: String) = this@VoiceSessionPresenter.onSessionError(error)
    }

    fun isListening(): Boolean = isListening
}