package com.voiceassistant.app

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mockk.*
import okhttp3.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class OpenAIRealtimeClientTest {
    
    private lateinit var client: OpenAIRealtimeClient
    private lateinit var mockListener: OpenAIRealtimeClient.RealtimeEventListener
    private lateinit var mockWebSocket: WebSocket
    private lateinit var webSocketListener: WebSocketListener
    private val gson = Gson()
    
    @Before
    fun setup() {
        mockListener = mockk(relaxed = true)
        mockWebSocket = mockk(relaxed = true)
        
        // Capture the WebSocketListener
        mockkConstructor(OkHttpClient.Builder::class)
        every { 
            anyConstructed<OkHttpClient.Builder>().build().newWebSocket(any(), capture(slot<WebSocketListener>()))
        } answers {
            webSocketListener = arg<WebSocketListener>(1)
            mockWebSocket
        }
        
        client = OpenAIRealtimeClient("test-api-key", mockListener)
    }
    
    @After
    fun teardown() {
        unmockkAll()
    }
    
    @Test
    fun `test continuous audio requirement for VAD`() {
        // Test that client sends audio continuously without gaps
        client.connect()
        
        // Simulate multiple audio chunks
        val audioData1 = ByteArray(320) { 1 }
        val audioData2 = ByteArray(320) { 2 }
        val audioData3 = ByteArray(320) { 3 }
        
        client.sendAudioInput(audioData1)
        client.sendAudioInput(audioData2)
        client.sendAudioInput(audioData3)
        
        // Verify all audio was sent without filtering
        verify(exactly = 3) { mockWebSocket.send(any<String>()) }
    }
    
    @Test
    fun `test session configuration includes proper VAD settings`() {
        client.connect()
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Capture the session configuration
        val configSlot = slot<String>()
        verify { mockWebSocket.send(capture(configSlot)) }
        
        val config = gson.fromJson(configSlot.captured, JsonObject::class.java)
        val session = config.getAsJsonObject("session")
        val turnDetection = session.getAsJsonObject("turn_detection")
        
        // Verify VAD configuration
        assertEquals("server_vad", turnDetection.get("type").asString)
        assertEquals(0.5, turnDetection.get("threshold").asDouble, 0.01)
        assertEquals(300, turnDetection.get("prefix_padding_ms").asInt)
        assertEquals(500, turnDetection.get("silence_duration_ms").asInt)
    }
    
    @Test
    fun `test interruption detection during active response`() {
        client.connect()
        
        // Simulate response started
        val responseCreated = JsonObject().apply {
            addProperty("type", "response.created")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(responseCreated))
        
        // Simulate speech detected during response
        val speechStarted = JsonObject().apply {
            addProperty("type", "input_audio_buffer.speech_started")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(speechStarted))
        
        // Verify interruption callback
        verify { mockListener.onInterruption() }
    }
    
    @Test
    fun `test no interruption when no active response`() {
        client.connect()
        
        // Simulate speech detected without active response
        val speechStarted = JsonObject().apply {
            addProperty("type", "input_audio_buffer.speech_started")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(speechStarted))
        
        // Verify no interruption callback
        verify(exactly = 0) { mockListener.onInterruption() }
    }
    
    @Test
    fun `test response cancellation handling`() {
        client.connect()
        
        // Start a response
        val responseCreated = JsonObject().apply {
            addProperty("type", "response.created")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(responseCreated))
        
        // Cancel the response
        client.cancelResponse()
        
        // Verify cancel event sent
        val cancelSlot = slot<String>()
        verify { mockWebSocket.send(capture(cancelSlot)) }
        val cancelEvent = gson.fromJson(cancelSlot.captured, JsonObject::class.java)
        assertEquals("response.cancel", cancelEvent.get("type").asString)
        
        // Simulate cancelled response
        val responseCancelled = JsonObject().apply {
            addProperty("type", "response.cancelled")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(responseCancelled))
        
        // Should not trigger error callback for cancellation
        verify(exactly = 0) { mockListener.onError(any()) }
    }
    
    @Test
    fun `test audio response handling`() {
        client.connect()
        
        // Simulate audio delta
        val audioData = "SGVsbG8gV29ybGQ=" // Base64 encoded "Hello World"
        val audioDelta = JsonObject().apply {
            addProperty("type", "response.audio.delta")
            addProperty("delta", audioData)
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(audioDelta))
        
        // Verify audio callback with decoded data
        val expectedBytes = android.util.Base64.decode(audioData, android.util.Base64.NO_WRAP)
        verify { mockListener.onAudioResponse(expectedBytes) }
    }
    
    @Test
    fun `test language configuration for English responses`() {
        client.connect()
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Capture the session configuration
        val configSlot = slot<String>()
        verify { mockWebSocket.send(capture(configSlot)) }
        
        val config = gson.fromJson(configSlot.captured, JsonObject::class.java)
        val session = config.getAsJsonObject("session")
        
        // Verify English instruction
        val instructions = session.get("instructions").asString
        assertTrue("Should specify English", instructions.contains("Always respond in English"))
        
        // Verify voice is set to echo (male)
        assertEquals("echo", session.get("voice").asString)
    }
    
    @Test
    fun `test error handling filters transient errors`() {
        client.connect()
        
        // Test response_not_found error (should be filtered)
        val notFoundError = JsonObject().apply {
            addProperty("type", "error")
            add("error", JsonObject().apply {
                addProperty("code", "response_not_found")
                addProperty("message", "Response not found")
            })
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(notFoundError))
        
        // Should not trigger error callback
        verify(exactly = 0) { mockListener.onError(any()) }
        
        // Test real error (should be reported)
        val realError = JsonObject().apply {
            addProperty("type", "error")
            add("error", JsonObject().apply {
                addProperty("code", "rate_limit_exceeded")
                addProperty("message", "Rate limit exceeded")
            })
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(realError))
        
        // Should trigger error callback
        verify { mockListener.onError("Rate limit exceeded") }
    }
    
    @Test
    fun `test audio complete callback`() {
        client.connect()
        
        // Simulate audio done event
        val audioDone = JsonObject().apply {
            addProperty("type", "response.audio.done")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(audioDone))
        
        // Verify callback
        verify { mockListener.onAudioComplete() }
    }
    
    @Test
    fun `test reconnection after disconnect`() {
        client.connect()
        client.disconnect()
        
        // Verify cleanup
        verify { mockWebSocket.close(1000, "Client disconnect") }
        
        // Should be able to connect again
        client.connect()
        verify(exactly = 2) { anyConstructed<OkHttpClient.Builder>().build().newWebSocket(any(), any()) }
    }
}