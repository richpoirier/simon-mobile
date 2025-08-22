package com.simon.app.webrtc

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.*
import java.io.IOException
import java.nio.ByteBuffer

/**
 * WebRTC client for OpenAI's Realtime API - handles real-time audio streaming using WebRTC protocol.
 * 
 * WebRTC (Web Real-Time Communication) enables peer-to-peer audio/video communication.
 * This implementation uses it for bi-directional audio streaming with OpenAI's servers.
 * 
 * Key WebRTC concepts used:
 * - PeerConnection: Manages the connection between this client and OpenAI's server
 * - DataChannel: Side channel for sending/receiving control messages (not audio)
 * - MediaStream/Track: Handles actual audio data flow
 * - SDP (Session Description Protocol): Negotiates connection parameters
 * - ICE (Interactive Connectivity Establishment): Establishes network path (though OpenAI doesn't use it)
 */
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
    
    /**
     * Initiates WebRTC connection to OpenAI's Realtime API.
     * 
     * Connection flow:
     * 1. Create and configure PeerConnection with audio capabilities
     * 2. Set up DataChannel for control messages
     * 3. Add local audio track from microphone
     * 4. Create SDP offer describing our capabilities
     * 5. Send offer to OpenAI via HTTP and receive SDP answer
     * 6. Connection established, audio flows through WebRTC
     */
    fun connect() {
        scope.launch {
            android.util.Log.d("OpenAIRealtimeClient", "Starting connection...")
            setupPeerConnection()
            android.util.Log.d("OpenAIRealtimeClient", "Peer connection setup, creating offer...")
            createOffer()
        }
    }
    
    /**
     * Sets up the WebRTC PeerConnection - the core object managing the connection.
     * 
     * PeerConnection handles:
     * - Media transport (audio streams)
     * - Network negotiation
     * - Codec negotiation
     * - Connection state management
     */
    private fun setupPeerConnection() {
        // ICE servers help establish connection through NATs/firewalls
        // OpenAI doesn't require STUN/TURN servers as connection is direct
        val iceServers = listOf<PeerConnection.IceServer>()
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            // Bundle all media streams over single connection for efficiency
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            // Multiplex RTP (media) and RTCP (control) on same port
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            // Disable TCP candidates - use UDP only for lower latency
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            // Gather ICE candidates once at start, not continuously
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        }
        
        // Create the PeerConnection with observer for state changes
        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                // ICE = Interactive Connectivity Establishment
                // These callbacks handle network path discovery (not used by OpenAI)
                override fun onIceCandidate(candidate: IceCandidate) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
                
                // Connection state monitoring - important for detecting disconnections
                override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
                    android.util.Log.d("OpenAIRealtimeClient", "Connection state changed to: $state")
                    when (state) {
                        PeerConnection.PeerConnectionState.FAILED -> {
                            scope.launch(ioDispatcher) {
                                listener.onError("Connection failed")
                                listener.onSessionEnded()
                            }
                        }
                        PeerConnection.PeerConnectionState.DISCONNECTED -> {
                            scope.launch(ioDispatcher) {
                                listener.onSessionEnded()
                            }
                        }
                        PeerConnection.PeerConnectionState.CONNECTED -> {
                            android.util.Log.d("OpenAIRealtimeClient", "PeerConnection fully connected")
                        }
                        else -> {}
                    }
                }
                
                // Legacy stream callbacks (we use tracks instead)
                override fun onAddStream(stream: MediaStream) {}
                override fun onRemoveStream(stream: MediaStream) {}
                
                // DataChannel created by remote peer (not used - we create our own)
                override fun onDataChannel(channel: DataChannel) {}
                
                // Renegotiation needed when media changes
                override fun onRenegotiationNeeded() {}
                
                // Legacy track callback
                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}
                
                /**
                 * Called when remote audio track is received from OpenAI.
                 * This track contains the AI's voice responses.
                 * WebRTC automatically plays this through the device speaker.
                 */
                override fun onTrack(transceiver: RtpTransceiver) {
                    transceiver.receiver?.track()?.let { track ->
                        if (track.kind() == MediaStreamTrack.AUDIO_TRACK_KIND) {
                            // Enable the track to start receiving audio
                            track.setEnabled(true)
                        }
                    }
                }
            }
        )
        
        /**
         * DataChannel provides a bidirectional message channel alongside the media streams.
         * Used for sending control messages, not audio data.
         * 
         * OpenAI uses this for:
         * - Session configuration (voice selection, instructions)
         * - Speech detection events (start/stop speaking)
         * - Response events (AI started/finished talking)
         * - Error messages
         */
        val dcInit = DataChannel.Init().apply {
            ordered = true  // Messages arrive in order sent
        }
        dataChannel = peerConnection?.createDataChannel("oai-events", dcInit)
        
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            
            /**
             * DataChannel state lifecycle:
             * CONNECTING -> OPEN -> CLOSING -> CLOSED
             * We send session config when channel opens
             */
            override fun onStateChange() {
                val state = dataChannel?.state()
                android.util.Log.d("OpenAIRealtimeClient", "DataChannel state changed to: $state")
                if (state == DataChannel.State.OPEN) {
                    android.util.Log.d("OpenAIRealtimeClient", "DataChannel opened, sending session update...")
                    sendSessionUpdate()  // Configure AI personality, voice, etc.
                }
            }
            
            /**
             * Receives JSON messages from OpenAI through the DataChannel.
             * These are control messages, not audio data.
             */
            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val message = String(data, Charsets.UTF_8)
                handleServerEvent(message)  // Parse and handle JSON events
            }
        })
        
        // Add local audio track from microphone
        setupLocalAudio()
    }
    
    /**
     * Sets up local audio capture from device microphone.
     * 
     * Audio flow:
     * Microphone -> AudioSource -> AudioTrack -> PeerConnection -> Network -> OpenAI
     * 
     * The audio processing pipeline includes echo cancellation and noise suppression
     * to improve voice quality for the AI.
     */
    private fun setupLocalAudio() {
        // Create audio source with constraints for voice optimization
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints().apply {
            // These are Google's WebRTC audio processing features
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))  // Remove echo
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))   // Normalize volume
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))  // Remove background noise
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))    // Remove low frequency noise
        })
        
        // Create audio track that will capture from microphone
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio0", audioSource)
        localAudioTrack?.setEnabled(true)  // Start capturing immediately
        
        // Add track to peer connection - this makes audio available for transmission
        val streamId = "stream0"  // Stream ID groups related tracks
        peerConnection?.addTrack(localAudioTrack, listOf(streamId))
    }
    
    /**
     * Creates an SDP (Session Description Protocol) offer.
     * 
     * SDP describes the media capabilities of this client:
     * - Supported audio codecs (Opus, PCMU, etc.)
     * - Network information
     * - Media format preferences
     * 
     * This offer is sent to OpenAI, which responds with an SDP answer
     * describing its capabilities, establishing the connection parameters.
     */
    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            // Tell WebRTC we want to receive audio from the remote peer (OpenAI)
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            // We don't use video in this voice assistant
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        
        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                // Set our local description - this configures our side of the connection
                peerConnection?.setLocalDescription(object : SdpObserver {
                    // onCreateSuccess/onCreateFailure not needed - we're setting, not creating
                    override fun onCreateSuccess(p0: SessionDescription) {}
                    override fun onCreateFailure(p0: String) {}
                    
                    override fun onSetSuccess() {
                        // Once local description is set, send offer to OpenAI
                        scope.launch {
                            sendOfferToOpenAI(sdp)
                        }
                    }
                    
                    // onSetFailure rarely happens - would indicate invalid SDP we just created
                    override fun onSetFailure(p0: String) {}
                }, sdp)
            }
            // onSetSuccess/onSetFailure not relevant for createOffer - only for set operations
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}
            
            override fun onCreateFailure(error: String) {
                listener.onError(error)
            }
        }, constraints)
    }
    
    /**
     * Sends SDP offer to OpenAI's HTTP endpoint and receives SDP answer.
     * 
     * This is the "signaling" phase of WebRTC:
     * 1. We send our SDP offer via HTTP POST
     * 2. OpenAI responds with SDP answer
     * 3. We set the answer as remote description
     * 4. WebRTC connection is established
     * 
     * Note: Unlike typical WebRTC, OpenAI uses HTTP for signaling instead of WebSockets.
     * The actual media flows through the WebRTC connection, not HTTP.
     */
    private suspend fun sendOfferToOpenAI(offer: SessionDescription) {
        withContext(ioDispatcher) {
            // Send SDP offer as HTTP POST with API key authentication
            val request = Request.Builder()
                .url("$baseUrl?model=$MODEL")
                .post(offer.description.toRequestBody("application/sdp".toMediaType()))
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: IOException) {
                android.util.Log.e("OpenAIRealtimeClient", "Network error", e)
                listener.onError("Network error: ${e.message}")
                return@withContext
            }
            
            android.util.Log.d("OpenAIRealtimeClient", "HTTP response code: ${response.code}")
            if (response.isSuccessful) {
                // Response body contains OpenAI's SDP answer
                val responseBody = try {
                    response.body.string()
                } catch (e: IOException) {
                    android.util.Log.e("OpenAIRealtimeClient", "Error reading response", e)
                    listener.onError("Error reading response: ${e.message}")
                    return@withContext
                }
                
                android.util.Log.d("OpenAIRealtimeClient", "Received SDP answer: ${responseBody.take(200)}...")
                val answer = SessionDescription(SessionDescription.Type.ANSWER, responseBody)
                
                // Set remote description - this completes the WebRTC handshake
                peerConnection?.setRemoteDescription(object : SdpObserver {
                    // onCreateSuccess/onCreateFailure not needed - we're setting, not creating
                    override fun onCreateSuccess(p0: SessionDescription) {}
                    override fun onCreateFailure(p0: String) {}
                    
                    override fun onSetSuccess() {
                        // Connection established! Audio can now flow both ways
                        android.util.Log.d("OpenAIRealtimeClient", "Remote description set successfully")
                        scope.launch(ioDispatcher) {
                            listener.onSessionStarted()
                        }
                    }
                    
                    override fun onSetFailure(error: String) {
                        android.util.Log.e("OpenAIRealtimeClient", "Failed to set remote description: $error")
                        scope.launch(ioDispatcher) {
                            listener.onError(error)
                        }
                    }
                }, answer)
            } else {
                val errorBody = try {
                    response.body.string()
                } catch (_: IOException) {
                    "Unable to read error response"
                }
                android.util.Log.e("OpenAIRealtimeClient", "HTTP error ${response.code}: $errorBody")
                listener.onError("HTTP ${response.code}")
            }
        }
    }
    
    /**
     * Sends session configuration to OpenAI through the DataChannel.
     * This must be sent after the DataChannel opens to configure the AI behavior.
     * 
     * Configuration includes:
     * - Model selection (GPT-4 Realtime)
     * - Voice selection (personality and accent)
     * - System instructions (personality, behavior)
     * - Audio format (PCM16 for compatibility)
     * - Turn detection (when AI should respond)
     */
    private fun sendSessionUpdate() {
        val sessionUpdate = JsonObject().apply {
            addProperty("type", "session.update")
            add("session", JsonObject().apply {
                addProperty("model", MODEL)
                addProperty("voice", VOICE)  // Voice personality
                addProperty("instructions", getPersonalityInstructions())
                addProperty("input_audio_format", "pcm16")  // 16-bit PCM audio
                addProperty("output_audio_format", "pcm16")
                add("turn_detection", JsonObject().apply {
                    // Semantic VAD = Voice Activity Detection based on meaning, not just sound
                    addProperty("type", "semantic_vad")
                    addProperty("eagerness", "medium")  // How quickly to respond
                    addProperty("create_response", true)  // Auto-respond when user stops
                    addProperty("interrupt_response", true)  // Allow interrupting AI
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
    
    /**
     * Handles JSON events received from OpenAI through the DataChannel.
     * 
     * Event types:
     * - session.created/updated: Configuration acknowledged
     * - input_audio_buffer.speech_started: User started speaking
     * - input_audio_buffer.speech_stopped: User stopped speaking
     * - response.created: AI started generating response
     * - response.done: AI finished response
     * - error: Something went wrong
     * 
     * These events coordinate the conversation flow and UI updates.
     */
    private fun handleServerEvent(message: String) {
        android.util.Log.d("OpenAIRealtimeClient", "Received server event: $message")
        
        val event = try {
            gson.fromJson(message, JsonObject::class.java)
        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("OpenAIRealtimeClient", "Invalid JSON from server: $message", e)
            return
        }
        
        val type = event.get("type")?.asString ?: return
        
        scope.launch(ioDispatcher) {
            when (type) {
                "session.created", "session.updated" -> {
                    android.util.Log.d("OpenAIRealtimeClient", "Session $type")
                    // Session configuration accepted by server
                }
                "input_audio_buffer.speech_started" -> {
                    // OpenAI detected user started speaking
                    listener.onSpeechStarted()
                }
                "input_audio_buffer.speech_stopped" -> {
                    // OpenAI detected user stopped speaking
                    listener.onSpeechStopped()
                }
                "response.created" -> {
                    // AI is starting to generate/speak response
                    listener.onResponseStarted()
                }
                "response.done" -> {
                    // AI finished speaking
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
    }
    
    /**
     * Sends a message through the DataChannel to OpenAI.
     * Messages must be JSON formatted strings.
     */
    private fun sendDataChannelMessage(message: String) {
        dataChannel?.let { channel ->
            if (channel.state() == DataChannel.State.OPEN) {
                val buffer = ByteBuffer.wrap(message.toByteArray())
                // false = not binary data (it's text/JSON)
                channel.send(DataChannel.Buffer(buffer, false))
            }
        }
    }
    
    /**
     * Cleanly disconnects the WebRTC session.
     * 
     * Cleanup order matters:
     * 1. Cancel coroutines to stop async operations
     * 2. Stop audio recording
     * 3. Close DataChannel
     * 4. Dispose audio track
     * 5. Close and dispose PeerConnection
     * 
     * This ensures resources are freed and the connection is properly terminated.
     */
    fun disconnect() {
        scope.cancel()
        
        // Close DataChannel for events
        dataChannel?.close()
        dataChannel = null
        
        // Dispose local audio track
        localAudioTrack?.dispose()
        localAudioTrack = null
        
        // Close the WebRTC connection
        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }
}