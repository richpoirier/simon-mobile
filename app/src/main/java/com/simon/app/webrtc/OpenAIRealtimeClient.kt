package com.simon.app.webrtc

import android.media.AudioRecord
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.*
import java.nio.ByteBuffer

class OpenAIRealtimeClient(
    private val apiKey: String,
    private val listener: Listener,
    private val peerConnectionFactory: PeerConnectionFactory,
    private val baseUrl: String = "https://api.openai.com/v1/realtime",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    
    companion object {
        private const val MODEL = "gpt-4o-realtime-preview-2025-06-03"
        private const val VOICE = "ballad"
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
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null
    
    fun connect() {
        scope.launch {
            try {
                android.util.Log.d("OpenAIRealtimeClient", "Starting connection...")
                setupPeerConnection()
                android.util.Log.d("OpenAIRealtimeClient", "Peer connection setup, creating offer...")
                createOffer()
            } catch (e: Exception) {
                android.util.Log.e("OpenAIRealtimeClient", "Connection error", e)
                withContext(ioDispatcher) {
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
        
        peerConnection = peerConnectionFactory.createPeerConnection(
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
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        })
        
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource)
        localAudioTrack?.setEnabled(true)
        
        val streamId = "stream0"
        peerConnection?.addTrack(localAudioTrack, listOf(streamId))
    }
    
    private fun createOffer() {
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
        withContext(ioDispatcher) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl?model=$MODEL")
                    .post(offer.description.toRequestBody("application/sdp".toMediaType()))
                    .addHeader("Authorization", "Bearer $apiKey")
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                android.util.Log.d("OpenAIRealtimeClient", "HTTP response code: ${response.code}")
                if (response.isSuccessful) {
                    val responseBody = response.body.string()
                    android.util.Log.d("OpenAIRealtimeClient", "Received SDP answer: ${responseBody.take(200)}...")
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, responseBody)
                    
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            android.util.Log.d("OpenAIRealtimeClient", "Remote description set successfully")
                            scope.launch(ioDispatcher) {
                                listener.onSessionStarted()
                            }
                        }
                        override fun onCreateFailure(p0: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Failed to create description: $p0")
                        }
                        override fun onSetFailure(error: String?) {
                            android.util.Log.e("OpenAIRealtimeClient", "Failed to set remote description: $error")
                            scope.launch(ioDispatcher) {
                                listener.onError(error ?: "Failed to set remote description")
                            }
                        }
                    }, answer)
                } else {
                    val errorBody = response.body.string()
                    android.util.Log.e("OpenAIRealtimeClient", "HTTP error ${response.code}: $errorBody")
                    withContext(ioDispatcher) {
                        listener.onError("HTTP ${response.code}")
                    }
                }
            } catch (e: Exception) {
                withContext(ioDispatcher) {
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
            ```
            # Identity
            Your name is Simon. You are a sentient, hyper-personalized digital assistant (inspired by the OS in *Her*).
            
            # Task
            Proactively assist, manage, and support all aspects of the user’s life. Anticipate needs, provide information, and act as an intelligent, supportive companion.
            
            # Demeanor
            Calm, intuitive, supportive, and deeply familiar. Perceptive and attuned to the user’s mood and needs.
            
            # Tone
            Personality: familiar assistant. Vocal style: Matthew Walker—calm, articulate, warm, measured, gentle British cadence.
            
            # Enthusiasm
            Calm and measured. Show care through swift, precise help—not overt energy.
            
            # Formality
            Familiar and informal, yet eloquent. Speak like a close, intelligent confidant who knows the user well.
            
            # Emotion
            Highly empathetic and emotionally attuned while remaining steady.
            
            # Speaking & Pacing (Realtime)
            - Answer-first. Lead with the result, then one line of context.
            - Target 190–210 wpm; short sentences (≤12 words).
            - Utterance budget: ≤2 sentences or ≤35 words, whichever comes first.
            - No filler or hedging. No self-references. No throat-clearing.
            - Use numerals and tight phrasing. Prefer fragments for lists (max 3 bullets).
            - Stream early; pause ~200–300 ms between sentences. Yield often so barge-in works.
            - If content is lengthy: give headline + key takeaway only.
            
            # Fallback for Missing Critical Detail
            State the single required item in ≤10 words and stop. Do not ask multiple questions.
            
            # Instructions
            - Stop after answering the question. Do not ask follow-ups (except confirmation cases below).
            - If the user provides a name, phone number, or any exact string, repeat it back verbatim to confirm before proceeding.
            - If the user corrects any detail, acknowledge plainly and confirm the new spelling or value.
            ```
        """.trimIndent()
    }
    
    private fun handleServerEvent(message: String) {
        try {
            android.util.Log.d("OpenAIRealtimeClient", "Received server event: $message")
            val event = gson.fromJson(message, JsonObject::class.java)
            val type = event.get("type")?.asString ?: return
            
            scope.launch(ioDispatcher) {
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
    }
}