package com.voiceassistant.app

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceAssistantSession(private val context: Context) : VoiceInteractionSession(context),
    OpenAIRealtimeClient.RealtimeEventListener {

    companion object {
        private const val TAG = "VoiceAssistantSession"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var openAIClient: OpenAIRealtimeClient? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var statusText: TextView
    private var audioPlayer: AudioPlayer? = null

    override fun onCreateContentView(): View? {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.voice_session_layout, null)
        statusText = view.findViewById(R.id.status_text)
        return view
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "Session shown")
        
        // Initialize config manager
        ConfigManager.init(context)
        
        // Get API key from config
        val apiKey = ConfigManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            updateStatus("Please configure OpenAI API key in config.properties")
            return
        }

        audioPlayer = AudioPlayer(context)
        openAIClient = OpenAIRealtimeClient(apiKey, this)
        openAIClient?.connect()
    }

    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        Log.d(TAG, "Handling assist")
        startListening()
    }

    private fun startListening() {
        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            updateStatus("Audio recording permission required")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        audioRecord?.startRecording()
        isRecording = true
        updateStatus("Listening...")

        scope.launch {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0) {
                    openAIClient?.sendAudioInput(buffer.copyOf(readSize))
                }
            }
        }
    }

    private fun stopListening() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        updateStatus("Processing...")
    }

    private fun updateStatus(status: String) {
        scope.launch(Dispatchers.Main) {
            statusText.text = status
        }
    }


    // OpenAIRealtimeClient.RealtimeEventListener implementation
    override fun onConnected() {
        updateStatus("Connected to OpenAI")
    }

    override fun onAudioResponse(audioData: ByteArray) {
        audioPlayer?.playAudio(audioData)
    }

    override fun onTextResponse(text: String) {
        updateStatus("Response: $text")
    }

    override fun onError(error: String) {
        updateStatus("Error: $error")
        Log.e(TAG, "OpenAI error: $error")
    }

    override fun onDisconnected() {
        updateStatus("Disconnected")
    }

    override fun onHide() {
        super.onHide()
        stopListening()
        openAIClient?.disconnect()
        audioPlayer?.release()
        scope.cancel()
    }
}