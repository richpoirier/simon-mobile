package com.simon.app

import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.simon.app.audio.AudioPlayer
import com.simon.app.config.ConfigManager
import com.simon.app.presentation.VoiceSessionPresenter
import com.simon.app.ui.AudioVisualizerScreen
import com.simon.app.ui.VoiceSessionViewModel
import com.simon.app.ui.theme.SimonTheme
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class VoiceSessionActivity : ComponentActivity() {

    private val viewModel: VoiceSessionViewModel by viewModels()
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var presenter: VoiceSessionPresenter

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow activity to show on lock screen and turn on the screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        // Optional: Dismiss keyguard to show activity above lock screen
        // Note: This shows the activity but doesn't unlock the device
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initializeServices()

        setContent {
            SimonTheme {
                AudioVisualizerScreen(
                    viewModel = viewModel,
                    onCloseClick = { finish() }
                )

                // Start voice session when ready
                LaunchedEffect(Unit) {
                    startVoiceSession()
                }
            }
        }
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
                    runOnUiThread {
                        viewModel.setConnected(true)
                    }
                },
                onSessionError = { error ->
                    runOnUiThread {
                        viewModel.setError(error)
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        finish()
                    }
                },
                onSpeechStarted = { 
                    runOnUiThread { viewModel.setUserSpeaking(true) }
                },
                onSpeechStopped = { 
                    runOnUiThread { viewModel.setUserSpeaking(false) }
                },
                onResponseStarted = {
                    // No UI change needed when assistant starts speaking
                },
                onResponseCompleted = {
                    // No UI change needed when assistant stops speaking
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
        // Start in disconnected state
        viewModel.setConnected(false)
        presenter.startListening()
    }

    private fun hideSystemUI() {
        window.insetsController?.let {
            it.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
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
