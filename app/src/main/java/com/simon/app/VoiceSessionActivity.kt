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
import com.simon.app.ui.RippleView
import com.simon.app.webrtc.OpenAIRealtimeClient
import kotlinx.coroutines.*
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.audio.JavaAudioDeviceModule

class VoiceSessionActivity : AppCompatActivity(), OpenAIRealtimeClient.Listener {

    private lateinit var rippleView: RippleView
    private lateinit var closeButton: ImageButton
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var realtimeClient: OpenAIRealtimeClient
    private lateinit var configManager: ConfigManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var eglBase: EglBase? = null

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
            configManager = ConfigManager(this)
            audioPlayer = AudioPlayer(this)

            // WebRTC requires a graphics context for its video components, even in an audio-only app.
            // EglBase provides this necessary handle to the device's graphics system.
            PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions())
            eglBase = EglBase.create()
            
            // This factory is responsible for creating PeerConnection objects. We create it here
            // so we can inject it into the OpenAIRealtimeClient. This makes the client more
            // testable, as we can provide a mock factory in our tests.
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

            realtimeClient = OpenAIRealtimeClient(
                apiKey = configManager.getOpenAIApiKey(),
                listener = this,
                peerConnectionFactory = peerConnectionFactory!!
            )
        } catch (e: Exception) {
            Log.e("VoiceSessionActivity", "Error initializing services", e)
            Toast.makeText(this, "Failed to initialize: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startVoiceSession() {
        scope.launch {
            try {
                audioPlayer.playReadyTone()
                rippleView.startListeningAnimation()
                realtimeClient.connect()
            } catch (e: Exception) {
                onError("Error starting voice session: ${e.message}")
            }
        }
    }

    override fun onSessionStarted() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true
    }

    override fun onSpeechStarted() { runOnUiThread { rippleView.startListeningAnimation() } }
    override fun onSpeechStopped() {}
    override fun onResponseStarted() { runOnUiThread { rippleView.startSpeakingAnimation() } }
    override fun onResponseCompleted() { runOnUiThread { rippleView.startListeningAnimation() } }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onSessionEnded() { runOnUiThread { finish() } }

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
        scope.cancel()
        realtimeClient.disconnect()
        audioPlayer.release()
        
        // It's crucial to release the factory and graphics context to free up
        // native resources and prevent memory leaks.
        peerConnectionFactory?.dispose()
        eglBase?.release()
        
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
    }
}
