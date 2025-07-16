package com.simon.app

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.simon.app.audio.AudioPlayer
import com.simon.app.config.ConfigManager
import com.simon.app.ui.RippleView
import com.simon.app.webrtc.OpenAIRealtimeClient
import kotlinx.coroutines.*

class VoiceSessionActivity : AppCompatActivity(), OpenAIRealtimeClient.Listener {
    
    private lateinit var rippleView: RippleView
    private lateinit var closeButton: ImageButton
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var realtimeClient: OpenAIRealtimeClient
    private lateinit var configManager: ConfigManager
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_session)
        
        // Keep screen on during voice interaction
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Hide system UI for full screen experience
        hideSystemUI()
        
        setupViews()
        initializeServices()
        startVoiceSession()
    }
    
    private fun setupViews() {
        rippleView = findViewById(R.id.ripple_view)
        closeButton = findViewById(R.id.close_button)
        
        closeButton.setOnClickListener {
            finish()
        }
    }
    
    private fun initializeServices() {
        try {
            configManager = ConfigManager(this)
            audioPlayer = AudioPlayer(this)
            
            realtimeClient = OpenAIRealtimeClient(
                context = this,
                apiKey = configManager.getOpenAIApiKey(),
                listener = this
            )
        } catch (e: Exception) {
            android.util.Log.e("VoiceSessionActivity", "Error initializing services", e)
            android.widget.Toast.makeText(this, "Failed to initialize: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun startVoiceSession() {
        scope.launch {
            try {
                 audioPlayer.playReadyTone()
                android.util.Log.d("VoiceSessionActivity", "Skipping audio playback for debugging")
                
                // Start listening animation
                rippleView.startListeningAnimation()
                
                // Connect to OpenAI
                realtimeClient.connect()
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("VoiceSessionActivity", "Error starting voice session", e)
                android.widget.Toast.makeText(this@VoiceSessionActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                delay(2000)
                finish()
            }
        }
    }
    
    override fun onSessionStarted() {
        // Session successfully started
        android.util.Log.d("VoiceSessionActivity", "Session started, setting up audio routing")
        
        // Set up audio routing for voice call
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
    }
    
    override fun onSpeechStarted() {
        runOnUiThread {
            rippleView.startListeningAnimation()
        }
    }
    
    override fun onSpeechStopped() {
        // User stopped speaking, waiting for response
    }
    
    override fun onResponseStarted() {
        runOnUiThread {
            rippleView.startSpeakingAnimation()
        }
    }
    
    override fun onResponseCompleted() {
        runOnUiThread {
            rippleView.startListeningAnimation()
        }
    }
    
    override fun onError(error: String) {
        android.util.Log.e("VoiceSessionActivity", "OpenAI client error: $error")
        runOnUiThread {
            android.widget.Toast.makeText(this, "Error: $error", android.widget.Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onSessionEnded() {
        runOnUiThread {
            finish()
        }
    }
    
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        realtimeClient.disconnect()
        audioPlayer.release()
        
        // Restore audio mode
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.mode = android.media.AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }
}