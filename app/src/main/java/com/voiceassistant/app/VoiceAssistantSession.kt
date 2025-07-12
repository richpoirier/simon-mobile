package com.voiceassistant.app

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.PowerManager
import android.service.voice.VoiceInteractionSession
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.view.animation.LinearInterpolator
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
    private var isSpeaking = false
    private var scope: CoroutineScope? = null
    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var circleOuter: View
    private lateinit var circleMiddle: View
    private lateinit var circleInner: View
    private var pulseAnimator: AnimatorSet? = null
    private var audioPlayer: AudioPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreateContentView(): View? {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.voice_session_layout, null)
        statusText = view.findViewById(R.id.status_text)
        hintText = view.findViewById(R.id.hint_text)
        circleOuter = view.findViewById(R.id.circle_outer)
        circleMiddle = view.findViewById(R.id.circle_middle)
        circleInner = view.findViewById(R.id.circle_inner)
        
        // Set UI to show on lockscreen
        setUiEnabled(true)
        setKeepAwake(true)
        
        // Apply theme and make fullscreen
        context.setTheme(R.style.Theme_VoiceSession)
        
        return view
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        Log.d(TAG, "onShow called with args: $args, showFlags: $showFlags")
        
        // Clean up any previous session state
        cleanupPreviousSession()
        
        // Create a new coroutine scope for this session
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        // Keep screen on when launched from lockscreen
        if ((showFlags and SHOW_WITH_SCREENSHOT) != 0) {
            Log.d(TAG, "Launched from lockscreen - keeping screen on")
            // Voice interaction sessions handle this differently
            setKeepAwake(true)
        }
        
        // Acquire wake lock to keep session alive when screen is off
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // Use FULL_WAKE_LOCK for lockscreen scenarios
        val wakeLockLevel = if ((showFlags and SHOW_WITH_SCREENSHOT) != 0) {
            Log.d(TAG, "Using SCREEN_BRIGHT_WAKE_LOCK for lockscreen")
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
        } else {
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
        }
        
        wakeLock = powerManager.newWakeLock(
            wakeLockLevel,
            "VoiceAssistant::SessionWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
        Log.d(TAG, "Wake lock acquired with level: $wakeLockLevel")
        
        // Initialize config manager
        ConfigManager.init(context)
        
        // Get API key from config
        val apiKey = ConfigManager.getApiKey()
        Log.d(TAG, "API key present: ${!apiKey.isNullOrEmpty()}")
        if (apiKey.isNullOrEmpty()) {
            updateStatus("Please configure OpenAI API key in config.properties")
            return
        }

        audioPlayer = AudioPlayer(context)
        openAIClient = OpenAIRealtimeClient(apiKey, this)
        openAIClient?.connect()
        
        // Start pulse animation
        startPulseAnimation()
    }

    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        Log.d(TAG, "Handling assist")
        startListening()
    }

    private fun startListening() {
        if (isRecording || isSpeaking) {
            Log.d(TAG, "Already recording or speaking")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = minBufferSize * 2
        Log.d(TAG, "Audio buffer size: $bufferSize")
        
        if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No audio recording permission")
            updateStatus("Audio recording permission required")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                updateStatus("Failed to initialize audio")
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            updateStatus("Listening...")
            Log.d(TAG, "Started recording")

            scope?.launch {
                val buffer = ByteArray(bufferSize)
                var silenceFrames = 0
                val silenceThreshold = 1000 // Amplitude threshold for silence
                val maxSilenceFrames = 30 // About 1 second of silence at 16kHz
                
                while (isRecording && !isSpeaking) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        openAIClient?.sendAudioInput(buffer.copyOf(readSize))
                        
                        // Check if this frame is silence
                        val maxAmplitude = getMaxAmplitude(buffer, readSize)
                        if (maxAmplitude < silenceThreshold) {
                            silenceFrames++
                            if (silenceFrames > maxSilenceFrames) {
                                // User has stopped speaking
                                Log.d(TAG, "Detected end of speech")
                                stopListening()
                                openAIClient?.commitAudioAndGetResponse()
                                break
                            }
                        } else {
                            silenceFrames = 0 // Reset on speech
                        }
                    }
                }
                Log.d(TAG, "Stopped recording")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            updateStatus("Error: ${e.message}")
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
        scope?.launch(Dispatchers.Main) {
            statusText.text = status
            
            // Update hint text based on status
            when {
                status.contains("Listening", ignoreCase = true) -> {
                    hintText.text = "Speak your question, then pause..."
                    hintText.alpha = 0.8f
                }
                status.contains("Processing", ignoreCase = true) -> {
                    hintText.text = "Thinking..."
                    hintText.alpha = 0.6f
                }
                status.contains("Response", ignoreCase = true) -> {
                    hintText.text = "Simon is speaking..."
                    hintText.alpha = 0.6f
                }
                status.contains("Ready", ignoreCase = true) -> {
                    hintText.text = "Ask another question..."
                    hintText.alpha = 0.6f
                }
                else -> {
                    hintText.text = "Say \"Hey Simon\" to start"
                    hintText.alpha = 0.6f
                }
            }
        }
    }


    // OpenAIRealtimeClient.RealtimeEventListener implementation
    override fun onConnected() {
        updateStatus("Connected to OpenAI")
    }

    override fun onAudioResponse(audioData: ByteArray) {
        if (!isSpeaking) {
            isSpeaking = true
            stopListening() // Stop recording while speaking
            Log.d(TAG, "Assistant started speaking")
        }
        audioPlayer?.playAudio(audioData)
    }

    override fun onTextResponse(text: String) {
        updateStatus("Response: $text")
    }

    override fun onError(error: String) {
        Log.e(TAG, "OpenAI error: $error")
        // Only show critical errors to user, not transient ones
        if (!error.contains("cancel", ignoreCase = true) && 
            !error.contains("response_not_found", ignoreCase = true)) {
            updateStatus("Error: $error")
            // Try to recover by resetting the listening state
            scope?.launch {
                delay(1000)
                updateStatus("Ready - try speaking again")
            }
        }
    }

    override fun onDisconnected() {
        updateStatus("Disconnected")
    }
    
    private fun getMaxAmplitude(buffer: ByteArray, size: Int): Int {
        var maxAmplitude = 0
        var i = 0
        while (i < size - 1) {
            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
            val amplitude = kotlin.math.abs(sample)
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude
            }
            i += 2
        }
        return maxAmplitude
    }

    override fun onAudioComplete() {
        Log.d(TAG, "Audio playback complete")
        isSpeaking = false
        updateStatus("Ready")
        // Automatically start listening again for the next query
        startListening()
    }


    private fun cleanupPreviousSession() {
        Log.d(TAG, "Cleaning up previous session")
        
        // Stop any ongoing recording
        stopListening()
        
        // Disconnect OpenAI client
        openAIClient?.disconnect()
        openAIClient = null
        
        // Release audio player
        audioPlayer?.release()
        audioPlayer = null
        
        // Cancel any ongoing coroutines
        scope?.cancel()
        
        // Release wake lock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "Wake lock released in cleanup")
            }
        }
        wakeLock = null
        
        // Reset speaking state
        isSpeaking = false
    }
    
    private fun startPulseAnimation() {
        scope?.launch(Dispatchers.Main) {
            // Create pulse animations for each circle
            val outerScaleX = ObjectAnimator.ofFloat(circleOuter, "scaleX", 1f, 1.1f)
            val outerScaleY = ObjectAnimator.ofFloat(circleOuter, "scaleY", 1f, 1.1f)
            val outerAlpha = ObjectAnimator.ofFloat(circleOuter, "alpha", 0.3f, 0.1f)
            
            val middleScaleX = ObjectAnimator.ofFloat(circleMiddle, "scaleX", 1f, 1.2f)
            val middleScaleY = ObjectAnimator.ofFloat(circleMiddle, "scaleY", 1f, 1.2f)
            val middleAlpha = ObjectAnimator.ofFloat(circleMiddle, "alpha", 0.5f, 0.2f)
            
            pulseAnimator = AnimatorSet().apply {
                playTogether(outerScaleX, outerScaleY, outerAlpha, middleScaleX, middleScaleY, middleAlpha)
                duration = 2000
                interpolator = LinearInterpolator()
                // Loop the animation
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        if (isRecording) {
                            start()
                        }
                    }
                })
                start()
            }
        }
    }
    
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
    }
    
    override fun onHide() {
        super.onHide()
        stopPulseAnimation()
        cleanupPreviousSession()
    }
}