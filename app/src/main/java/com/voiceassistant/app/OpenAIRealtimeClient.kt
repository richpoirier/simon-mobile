package com.voiceassistant.app

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

class OpenAIRealtimeClient(
    private val apiKey: String,
    private val listener: RealtimeEventListener
) {
    companion object {
        private const val TAG = "OpenAIRealtimeClient"
        private const val REALTIME_API_URL = "wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01"
    }

    private var webSocket: WebSocket? = null
    private var isResponseActive = false
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private var scope: CoroutineScope? = CoroutineScope(Dispatchers.IO + SupervisorJob())

    interface RealtimeEventListener {
        fun onConnected()
        fun onAudioResponse(audioData: ByteArray)
        fun onTextResponse(text: String)
        fun onError(error: String)
        fun onDisconnected()
        fun onInterruption()
        fun onAudioComplete()
    }

    fun connect() {
        val request = Request.Builder()
            .url(REALTIME_API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                listener.onConnected()
                initializeSession()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerEvent(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Handle binary audio data if needed
                Log.d(TAG, "Received binary message: ${bytes.size} bytes")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                listener.onError("Connection failed: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                listener.onDisconnected()
            }
        })
    }

    private fun initializeSession() {
        val sessionUpdate = JsonObject().apply {
            addProperty("type", "session.update")
            add("session", JsonObject().apply {
                add("modalities", gson.toJsonTree(listOf("text", "audio")))
                addProperty("instructions", "You are Simon, a helpful and friendly AI assistant. Be concise, personable, and natural in your responses.")
                addProperty("voice", "echo")
                addProperty("input_audio_format", "pcm16")
                addProperty("output_audio_format", "pcm16")
                add("turn_detection", JsonObject().apply {
                    addProperty("type", "server_vad")
                    addProperty("threshold", 0.5) // Balanced threshold for responsiveness
                    addProperty("prefix_padding_ms", 300)
                    addProperty("silence_duration_ms", 500) // Faster turn detection
                })
            })
        }
        sendEvent(sessionUpdate)
    }

    fun sendAudioInput(audioData: ByteArray) {
        val audioAppend = JsonObject().apply {
            addProperty("type", "input_audio_buffer.append")
            addProperty("audio", android.util.Base64.encodeToString(audioData, android.util.Base64.NO_WRAP))
        }
        sendEvent(audioAppend)
    }

    fun sendTextInput(text: String) {
        val conversation = JsonObject().apply {
            addProperty("type", "conversation.item.create")
            add("item", JsonObject().apply {
                addProperty("type", "message")
                addProperty("role", "user")
                add("content", gson.toJsonTree(listOf(
                    JsonObject().apply {
                        addProperty("type", "input_text")
                        addProperty("text", text)
                    }
                )))
            })
        }
        sendEvent(conversation)
        
        // Trigger response
        val responseCreate = JsonObject().apply {
            addProperty("type", "response.create")
        }
        sendEvent(responseCreate)
    }

    fun cancelResponse() {
        if (isResponseActive) {
            val cancel = JsonObject().apply {
                addProperty("type", "response.cancel")
            }
            sendEvent(cancel)
            Log.d(TAG, "Sent response.cancel")
        } else {
            Log.d(TAG, "No active response to cancel")
        }
    }

    private fun sendEvent(event: JsonObject) {
        val json = gson.toJson(event)
        Log.d(TAG, "Sending event: $json")
        webSocket?.send(json)
    }

    private fun handleServerEvent(jsonString: String) {
        try {
            val event = gson.fromJson(jsonString, JsonObject::class.java)
            val type = event.get("type")?.asString ?: return
            
            Log.d(TAG, "Received event: $type")
            
            when (type) {
                "response.created" -> {
                    isResponseActive = true
                    Log.d(TAG, "Response started")
                }
                "response.done" -> {
                    isResponseActive = false
                    Log.d(TAG, "Response completed")
                }
                "response.audio.done" -> {
                    Log.d(TAG, "Audio response done")
                    // Notify that audio is complete
                    listener.onAudioComplete()
                }
                "response.audio.delta" -> {
                    val delta = event.get("delta")?.asString ?: return
                    val audioData = android.util.Base64.decode(delta, android.util.Base64.NO_WRAP)
                    listener.onAudioResponse(audioData)
                }
                "response.text.done" -> {
                    val text = event.get("text")?.asString ?: return
                    listener.onTextResponse(text)
                }
                "input_audio_buffer.speech_started" -> {
                    Log.d(TAG, "User started speaking - interrupting")
                    listener.onInterruption()
                }
                "response.cancelled" -> {
                    isResponseActive = false
                    Log.d(TAG, "Response cancelled")
                    listener.onInterruption()
                }
                "response.cancel_failed" -> {
                    val error = event.getAsJsonObject("error")
                    val code = error?.get("code")?.asString
                    Log.w(TAG, "Cancel failed: $code - likely no active response")
                    // Don't treat this as a fatal error
                }
                "error" -> {
                    val error = event.getAsJsonObject("error")
                    val code = error?.get("code")?.asString
                    val message = error?.get("message")?.asString ?: "Unknown error"
                    
                    // Don't show cancellation errors to the user
                    if (code == "response_not_found" || message.contains("cancel", ignoreCase = true)) {
                        Log.d(TAG, "Ignoring cancellation error: $message")
                    } else {
                        listener.onError(message)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling server event", e)
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
        scope?.cancel()
        scope = null
    }
}