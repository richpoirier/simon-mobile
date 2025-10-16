package com.simon.app.webrtc

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.*
import org.webrtc.*
import java.io.IOException
import java.nio.ByteBuffer

class OpenAIRealtimeClient(
    private val context: Context,
    private val apiKey: String,
    private val listener: Listener,
    private val peerConnectionFactory: PeerConnectionFactory,
    private val baseUrl: String = "https://api.openai.com/v1/realtime/calls",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
) {

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

    fun connect() {
        scope.launch {
            setupPeerConnection()
            createOffer()
        }
    }

    private fun setupPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
        )
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
        }

        peerConnection = peerConnectionFactory.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {}
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}

                override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
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
                        else -> {}
                    }
                }

                override fun onAddStream(stream: MediaStream) {}
                override fun onRemoveStream(stream: MediaStream) {}
                override fun onDataChannel(channel: DataChannel) {}
                override fun onRenegotiationNeeded() {}
                override fun onAddTrack(receiver: RtpReceiver, streams: Array<out MediaStream>) {}

                override fun onTrack(transceiver: RtpTransceiver) {
                    transceiver.receiver?.track()?.let { track ->
                        if (track.kind() == MediaStreamTrack.AUDIO_TRACK_KIND) {
                            track.setEnabled(true)
                        }
                    }
                }
            }
        )

        val dcInit = DataChannel.Init().apply {
            ordered = true
        }
        dataChannel = peerConnection?.createDataChannel("oai-events", dcInit)

        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}

            override fun onStateChange() {
                if (dataChannel?.state() == DataChannel.State.OPEN) {
                    try {
                        sendSessionUpdate()
                    } catch (e: Exception) {
                        scope.launch(ioDispatcher) {
                            listener.onError("Failed to load prompt: ${e.message}")
                        }
                    }
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                val message = String(data, Charsets.UTF_8)
                handleServerEvent(message)
            }
        })

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

        peerConnection?.addTrack(localAudioTrack, listOf("stream0"))
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription) {}
                    override fun onCreateFailure(p0: String) {}

                    override fun onSetSuccess() {
                        scope.launch {
                            sendOfferToOpenAI(sdp)
                        }
                    }

                    override fun onSetFailure(p0: String) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String) {}

            override fun onCreateFailure(error: String) {
                listener.onError(error)
            }
        }, constraints)
    }

    private suspend fun sendOfferToOpenAI(offer: SessionDescription) {
        withContext(ioDispatcher) {
            val sessionConfigJson = gson.toJson(createSessionConfig())

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("sdp", offer.description)
                .addFormDataPart("session", sessionConfigJson)
                .build()

            val request = Request.Builder()
                .url(baseUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: IOException) {
                listener.onError("Network error: ${e.message}")
                return@withContext
            }

            if (response.isSuccessful) {
                val responseBody = try {
                    response.body.string()
                } catch (e: IOException) {
                    listener.onError("Error reading response: ${e.message}")
                    return@withContext
                }

                val answer = SessionDescription(SessionDescription.Type.ANSWER, responseBody)

                peerConnection?.setRemoteDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription) {}
                    override fun onCreateFailure(p0: String) {}

                    override fun onSetSuccess() {
                        scope.launch(ioDispatcher) {
                            listener.onSessionStarted()
                        }
                    }

                    override fun onSetFailure(error: String) {
                        scope.launch(ioDispatcher) {
                            listener.onError(error)
                        }
                    }
                }, answer)
            } else {
                val errorBody = response.body.string()
                listener.onError("HTTP ${response.code}: $errorBody")
            }
        }
    }

    private fun createSessionConfig(): JsonObject {
        return JsonObject().apply {
            addProperty("type", "realtime")
            addProperty("model", "gpt-realtime")
            add("output_modalities", gson.toJsonTree(listOf("audio")))
            add("audio", JsonObject().apply {
                add("input", JsonObject().apply {
                    add("format", JsonObject().apply {
                        addProperty("type", "audio/pcm")
                        addProperty("rate", 24000)
                    })
                    add("turn_detection", JsonObject().apply {
                        addProperty("type", "semantic_vad")
                        addProperty("eagerness", "low")
                        addProperty("create_response", true)
                        addProperty("interrupt_response", true)
                    })
                })
                add("output", JsonObject().apply {
                    addProperty("voice", "ballad")
                })
            })
            addProperty("instructions", getPersonalityInstructions())
        }
    }

    private fun sendSessionUpdate() {
        val sessionUpdate = JsonObject().apply {
            addProperty("type", "session.update")
            add("session", createSessionConfig())
        }

        sendDataChannelMessage(gson.toJson(sessionUpdate))
    }

    private fun getPersonalityInstructions(): String {
        return context.assets.open("simon_prompt.md").bufferedReader().use { it.readText() }
    }

    private fun handleServerEvent(message: String) {
        val event = try {
            gson.fromJson(message, JsonObject::class.java)
        } catch (_: com.google.gson.JsonSyntaxException) {
            return
        }

        val type = event.get("type")?.asString ?: return

        scope.launch(ioDispatcher) {
            when (type) {
                "input_audio_buffer.speech_started" -> listener.onSpeechStarted()
                "input_audio_buffer.speech_stopped" -> listener.onSpeechStopped()
                "response.created" -> listener.onResponseStarted()
                "response.done" -> listener.onResponseCompleted()
                "error" -> {
                    val errorMessage = event.get("message")?.asString ?: "Unknown error"
                    listener.onError(errorMessage)
                }
            }
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

        dataChannel?.close()
        dataChannel = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null
    }
}
