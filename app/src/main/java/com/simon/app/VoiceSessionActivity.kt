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

class VoiceSessionActivity : ComponentActivity() {

    private val viewModel: VoiceSessionViewModel by viewModels()
    private lateinit var presenter: VoiceSessionPresenter

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow activity to show on lock screen and turn on the screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        
        (getSystemService(KEYGUARD_SERVICE) as android.app.KeyguardManager).requestDismissKeyguard(this, null)
        
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
            initializeWebRTC()
            val configManager = ConfigManager(this)
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            
            presenter = VoiceSessionPresenter(
                context = this,
                configManager = configManager,
                audioManager = audioManager,
                onSessionStarted = { viewModel.setConnected(true) },
                onSessionError = { error ->
                    runOnUiThread {
                        viewModel.setError(error)
                        Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
                        finish()
                    }
                },
                onSpeechStarted = { viewModel.setUserSpeaking(true) },
                onSpeechStopped = { viewModel.setUserSpeaking(false) },
                onResponseStarted = { },
                onResponseCompleted = { },
                onSessionEnded = { runOnUiThread { finish() } }
            )
            peerConnectionFactory?.let { presenter.initialize(it) }
            
        } catch (e: Exception) {
            Log.e("VoiceSessionActivity", "Error initializing services", e)
            Toast.makeText(this, "Failed to initialize: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeWebRTC() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions())
        eglBase = EglBase.create()
        val audioDeviceModule = JavaAudioDeviceModule.builder(this)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase?.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase?.eglBaseContext, true, true))
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
    }

    private fun startVoiceSession() {
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
        presenter.cleanup()
        peerConnectionFactory?.dispose()
        eglBase?.release()
    }
}
