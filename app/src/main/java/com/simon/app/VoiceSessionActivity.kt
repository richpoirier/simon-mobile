package com.simon.app

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.simon.app.audio.AudioPlayer
import com.simon.app.config.ConfigManager
import com.simon.app.presentation.VoiceSessionPresenter
import com.simon.app.ui.RippleView
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class VoiceSessionActivity : AppCompatActivity() {

    private lateinit var rippleView: RippleView
    private lateinit var closeButton: ImageButton
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var presenter: VoiceSessionPresenter

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_session)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setupViews()
        initializeServices()
        startVoiceSession()
    }

    private fun setupViews() {
        rippleView = findViewById(R.id.ripple_view)
        closeButton = findViewById(R.id.close_button)
        closeButton.setOnClickListener { finish() }
    }

    private fun initializeServices() {
        try {
            audioPlayer = AudioPlayer(this)
            
            // Initialize WebRTC components
            initializeWebRTC()
            
            // Create presenter with callbacks for UI updates
            val configManager = ConfigManager(this)
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            
            presenter = VoiceSessionPresenter(
                configManager = configManager,
                audioManager = audioManager,
                onSpeakerEnabled = { /* Speaker enabled by presenter */ },
                onSessionStarted = { 
                    // Launch coroutine to play the ready tone
                    activityScope.launch {
                        audioPlayer.playReadyTone()
                    }
                },
                onSessionError = { error ->
                    runOnUiThread {
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        finish()
                    }
                },
                onSpeechStarted = { 
                    runOnUiThread { rippleView.startListeningAnimation() }
                },
                onSpeechStopped = { /* No UI update needed */ },
                onResponseStarted = { 
                    runOnUiThread { rippleView.startSpeakingAnimation() }
                },
                onResponseCompleted = { 
                    runOnUiThread { rippleView.startListeningAnimation() }
                },
                onSessionEnded = { 
                    runOnUiThread { finish() }
                }
            )
            
            // Initialize presenter with PeerConnectionFactory
            peerConnectionFactory?.let {
                presenter.initialize(it)
            }
            
        } catch (e: Exception) {
            Log.e("VoiceSessionActivity", "Error initializing services", e)
            Toast.makeText(this, "Failed to initialize: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeWebRTC() {
        // WebRTC requires a graphics context for its video components, even in an audio-only app.
        // EglBase provides this necessary handle to the device's graphics system.
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions())
        eglBase = EglBase.create()
        
        // This factory is responsible for creating PeerConnection objects.
        val audioDeviceModule = JavaAudioDeviceModule.builder(this)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                override fun onWebRtcAudioRecordInitError(errorMessage: String?) { Log.e("VoiceSessionActivity", "AudioRecordInitError: $errorMessage") }
                override fun onWebRtcAudioRecordStartError(errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?, errorMessage: String?) { Log.e("VoiceSessionActivity", "AudioRecordStartError: $errorMessage") }
                override fun onWebRtcAudioRecordError(errorMessage: String?) { Log.e("VoiceSessionActivity", "AudioRecordError: $errorMessage") }
            })
            .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                override fun onWebRtcAudioTrackInitError(errorMessage: String?) { Log.e("VoiceSessionActivity", "AudioTrackInitError: $errorMessage") }
                override fun onWebRtcAudioTrackStartError(errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?, errorMessage: String?) { Log.e("VoiceSessionActivity", "AudioTrackStartError: $errorMessage") }
                override fun onWebRtcAudioTrackError(errorMessage: String?) { Log.e("VoiceSessionActivity", "AudioTrackError: $errorMessage") }
            })
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase?.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase?.eglBaseContext, true, true))
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }

    private fun startVoiceSession() {
        rippleView.startListeningAnimation()
        presenter.startListening()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
        presenter.cleanup()
        audioPlayer.release()
        
        // It's crucial to release the factory and graphics context to free up
        // native resources and prevent memory leaks.
        peerConnectionFactory?.dispose()
        eglBase?.release()
    }
}
