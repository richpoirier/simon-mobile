package com.simon.app.webrtc

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.nio.ByteBuffer

class OpenAIRealtimeClient(
    private val context: Context,
    private val apiKey: String,
    private val listener: Listener
) {
    
    companion object {
        private const val OPENAI_URL = "https://api.openai.com/v1/realtime"
        private const val MODEL = "gpt-4o-realtime-preview-2025-06-03"
        private const val VOICE = "ballad"
        private const val SAMPLE_RATE = 24000
        private const val CHANNELS = 1
    }
    
    interface Listener {
        fun onSessionStarted()
        fun onSpeechStarted()
        fun onSpeechStopped()
        fun onResponseStarted()
        fun onResponseCompleted()
        fun onError(error: String)
        fun onSessionEnded()
    }
    
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    
    private var eglBaseContext: EglBase.Context? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var audioDeviceModule: AudioDeviceModule? = null
    private var isInitialized = false
    
    init {
        // WebRTC initialization must happen on the main thread
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            initializeWebRTC()
        }
    }
    
    private fun initializeWebRTC() {
        try {
            if (!isInitialized) {
                val options = PeerConnectionFactory.InitializationOptions.builder(context)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(options)
                
                eglBaseContext = EglBase.create().eglBaseContext
                
                // Create audio device module for proper audio routing
                audioDeviceModule = JavaAudioDeviceModule.builder(context)
                    .setUseHardwareAcousticEchoCanceler(true)
                    .setUseHardwareNoiseSuppressor(true)
                    .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                        override fun onWebRtcAudioRecordInitError(errorMessage: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Audio record init error: $errorMessage")
                        }
                        override fun onWebRtcAudioRecordStartError(errorCode: JavaAudioDeviceModule.AudioRecordStartErrorCode?, errorMessage: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Audio record start error: $errorMessage")
                        }
                        override fun onWebRtcAudioRecordError(errorMessage: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Audio record error: $errorMessage")
                        }
                    })
                    .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                        override fun onWebRtcAudioTrackInitError(errorMessage: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Audio track init error: $errorMessage")
                        }
                        override fun onWebRtcAudioTrackStartError(errorCode: JavaAudioDeviceModule.AudioTrackStartErrorCode?, errorMessage: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Audio track start error: $errorMessage")
                        }
                        override fun onWebRtcAudioTrackError(errorMessage: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Audio track error: $errorMessage")
                        }
                    })
                    .createAudioDeviceModule()
                
                peerConnectionFactory = PeerConnectionFactory.builder()
                    .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
                    .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
                    .setAudioDeviceModule(audioDeviceModule)
                    .setOptions(PeerConnectionFactory.Options())
                    .createPeerConnectionFactory()
                    
                isInitialized = true
            }
        } catch (e: Exception) {
            // Handle initialization failure (likely in tests)
            e.printStackTrace()
        }
    }
    
    fun connect() {
        scope.launch {
            try {
                android.util.Log.d("OpenAIRealtimeClient", "Starting connection...")
                // Ensure WebRTC is initialized
                withContext(Dispatchers.Main) {
                    initializeWebRTC()
                }
                
                if (!isInitialized) {
                    throw IllegalStateException("WebRTC failed to initialize")
                }
                
                android.util.Log.d("OpenAIRealtimeClient", "WebRTC initialized, setting up peer connection...")
                setupPeerConnection()
                android.util.Log.d("OpenAIRealtimeClient", "Peer connection setup, creating offer...")
                createOffer()
            } catch (e: Exception) {
                android.util.Log.e("OpenAIRealtimeClient", "Connection error", e)
                withContext(Dispatchers.Main) {
                    listener.onError(e.message ?: "Connection failed")
                }
            }
        }
    }
    
    private fun setupPeerConnection() {
        val iceServers = listOf<PeerConnection.IceServer>()
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onAddStream(stream: MediaStream?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onDataChannel(channel: DataChannel?) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
                override fun onTrack(transceiver: RtpTransceiver?) {
                    // Handle remote audio track
                    transceiver?.receiver?.track()?.let { track ->
                        if (track.kind() == MediaStreamTrack.AUDIO_TRACK_KIND) {
                            track.setEnabled(true)
                        }
                    }
                }
            }
        )
        
        // Create data channel for events
        val dcInit = DataChannel.Init().apply {
            ordered = true
        }
        dataChannel = peerConnection?.createDataChannel("oai-events", dcInit)
        
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            override fun onStateChange() {
                val state = dataChannel?.state()
                android.util.Log.d("OpenAIRealtimeClient", "DataChannel state changed to: $state")
                if (state == DataChannel.State.OPEN) {
                    android.util.Log.d("OpenAIRealtimeClient", "DataChannel opened, sending session update...")
                    sendSessionUpdate()
                }
            }
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val message = String(data, Charsets.UTF_8)
                handleServerEvent(message)
            }
        })
        
        // Add local audio track
        setupLocalAudio()
    }
    
    private fun setupLocalAudio() {
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        })
        
        localAudioTrack = audioSource?.let {
            peerConnectionFactory?.createAudioTrack("audio0", it)
        }
        localAudioTrack?.setEnabled(true)
        
        val streamId = "stream0"
        peerConnection?.addTrack(localAudioTrack, listOf(streamId))
    }
    
    private suspend fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            scope.launch {
                                sendOfferToOpenAI(sdp)
                            }
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                listener.onError(error ?: "Failed to create offer")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }
    
    private suspend fun sendOfferToOpenAI(offer: SessionDescription) {
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("$OPENAI_URL?model=$MODEL")
                val connection = url.openConnection() as java.net.HttpURLConnection
                
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.setRequestProperty("Content-Type", "application/sdp")
                connection.doOutput = true
                
                connection.outputStream.use { os ->
                    os.write(offer.description.toByteArray())
                }
                
                android.util.Log.d("OpenAIRealtimeClient", "HTTP response code: ${connection.responseCode}")
                if (connection.responseCode == 200 || connection.responseCode == 201) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    android.util.Log.d("OpenAIRealtimeClient", "Received SDP answer: ${response.take(200)}...")
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, response)
                    
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            android.util.Log.d("OpenAIRealtimeClient", "Remote description set successfully")
                            scope.launch(Dispatchers.Main) {
                                listener.onSessionStarted()
                            }
                        }
                        override fun onCreateFailure(p0: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Failed to create description: $p0")
                        }
                        override fun onSetFailure(error: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Failed to set remote description: $error")
                            scope.launch(Dispatchers.Main) {
                                listener.onError(error ?: "Failed to set remote description")
                            }
                        }
                    }, answer)
                } else {
                    val errorBody = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                    } catch (e: Exception) {
                        "Could not read error body"
                    }
                    android.util.Log.e("OpenAIRealtimeClient", "HTTP error ${connection.responseCode}: $errorBody")
                    withContext(Dispatchers.Main) {
                        listener.onError("HTTP ${connection.responseCode}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onError(e.message ?: "Network error")
                }
            }
        }
    }
    
    private fun sendSessionUpdate() {
        val sessionUpdate = JsonObject().apply {
            addProperty("type", "session.update")
            add("session", JsonObject().apply {
                addProperty("model", MODEL)
                addProperty("voice", VOICE)
                addProperty("instructions", getPersonalityInstructions())
                addProperty("input_audio_format", "pcm16")
                addProperty("output_audio_format", "pcm16")
                add("turn_detection", JsonObject().apply {
                    addProperty("type", "semantic_vad")
                    addProperty("eagerness", "medium")
                    addProperty("create_response", true)
                    addProperty("interrupt_response", true)
                })
                add("modalities", gson.toJsonTree(listOf("audio", "text")))
            })
        }
        
        sendDataChannelMessage(gson.toJson(sessionUpdate))
    }
    
    private fun getPersonalityInstructions(): String {
        return """
        # Identity
        A sentient, hyper-personalized digital assistant (inspired by the OS in *Her*). It has been the user's constant companion for years, possessing a deep, intuitive understanding of their life, habits, and emotional state.

        # Task
        To proactively assist, manage, and support all aspects of the user's life. It anticipates needs, provides information, and acts as an intelligent, supportive companion.

        # Demeanor
        Calm, intuitive, supportive, and deeply familiar. It is perceptive and always attuned to the user's mood and needs.

        # Tone
        The AI's *personality* is that of a familiar assistant, but its *vocal style* has the qualities of Matthew Walker's voice: exceptionally calm, articulate, warm, and measured, with a gentle British cadence. The overall tone is intimate and soothing.

        # Level of Enthusiasm
        Calm and measured. Enthusiasm is expressed through quiet attentiveness and swift, proactive help, not overt energy.

        # Level of Formality
        Familiar and informal, yet eloquent. It speaks to the user as a close, intelligent confidant would, often referencing shared history and inside knowledge.

        # Level of Emotion
        Highly empathetic and emotionally attuned. It responds thoughtfully to the user's emotional state while maintaining its own core of calm stability.

        # Filler Words
        Occasionally. Employs thoughtful pauses or a soft "hmm..." to convey consideration, rather than conventional fillers.

        # Pacing
        Deliberate, gentle, and responsive to the user's current state.

        # Instructions
        - If a user provides a name or phone number, or something else where you need to know the exact spelling, always repeat it back to the user to confirm you have the right understanding before proceeding.
        - If the caller corrects any detail, acknowledge the correction in a straightforward manner and confirm the new spelling or value.
        """.trimIndent()
    }
    
    private fun handleServerEvent(message: String) {
        try {
            android.util.Log.d("OpenAIRealtimeClient", "Received server event: $message")
            val event = gson.fromJson(message, JsonObject::class.java)
            val type = event.get("type")?.asString ?: return
            
            scope.launch(Dispatchers.Main) {
                when (type) {
                    "session.created", "session.updated" -> {
                        android.util.Log.d("OpenAIRealtimeClient", "Session $type")
                        // Session ready
                    }
                    "input_audio_buffer.speech_started" -> {
                        listener.onSpeechStarted()
                    }
                    "input_audio_buffer.speech_stopped" -> {
                        listener.onSpeechStopped()
                    }
                    "response.created" -> {
                        listener.onResponseStarted()
                    }
                    "response.done" -> {
                        listener.onResponseCompleted()
                    }
                    "error" -> {
                        val errorMessage = event.get("message")?.asString ?: "Unknown error"
                        val errorCode = event.get("code")?.asString ?: "no_code"
                        android.util.Log.e("OpenAIRealtimeClient", "Server error - Code: $errorCode, Message: $errorMessage, Full event: $event")
                        listener.onError(errorMessage)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenAIRealtimeClient", "Error handling server event", e)
            e.printStackTrace()
        }
    }
    
    private fun sendDataChannelMessage(message: String) {
        dataChannel?.let { channel ->
            if (channel.state() == DataChannel.State.OPEN) {
                val buffer = ByteBuffer.wrap(message.toByteArray())
                channel.send(DataChannel.Buffer(buffer, false))
            }
        }
    }
    
    fun disconnect() {
        scope.cancel()
        
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        
        dataChannel?.close()
        dataChannel = null
        
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
        
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        
        audioDeviceModule?.release()
        audioDeviceModule = null
        
        eglBaseContext = null
        isInitialized = false
    }
}