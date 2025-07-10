package com.voiceassistant.app

import android.content.Context
import android.media.AudioRecord
import android.os.Bundle
import android.os.PowerManager
import android.service.voice.VoiceInteractionSession
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class IntegrationTest {
    
    private lateinit var context: Context
    private lateinit var session: VoiceAssistantSession
    private lateinit var mockWebSocket: WebSocket
    private lateinit var webSocketListener: WebSocketListener
    private lateinit var mockPowerManager: PowerManager
    private lateinit var mockWakeLock: PowerManager.WakeLock
    private val testDispatcher = StandardTestDispatcher()
    private val gson = Gson()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        mockPowerManager = mockk(relaxed = true)
        mockWakeLock = mockk(relaxed = true)
        mockWebSocket = mockk(relaxed = true)
        
        every { context.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager
        every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock
        every { mockWakeLock.isHeld } returns true
        every { context.checkSelfPermission(any()) } returns android.content.pm.PackageManager.PERMISSION_GRANTED
        
        // Mock AudioRecord
        mockkStatic(AudioRecord::class)
        every { AudioRecord.getMinBufferSize(any(), any(), any()) } returns 640
        
        // Mock OkHttpClient to capture WebSocketListener
        mockkConstructor(OkHttpClient.Builder::class)
        every { 
            anyConstructed<OkHttpClient.Builder>().build().newWebSocket(any(), capture(slot<WebSocketListener>()))
        } answers {
            webSocketListener = arg<WebSocketListener>(1)
            mockWebSocket
        }
        
        // Mock ConfigManager
        mockkObject(ConfigManager)
        every { ConfigManager.init(any()) } just Runs
        every { ConfigManager.getApiKey() } returns "test-api-key"
        
        session = VoiceAssistantSession(context)
    }
    
    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Test
    fun `test full interruption flow from speech detection to audio stop`() {
        // Initialize session
        session.onShow(Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Simulate WebSocket connection
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Start assistant response
        val responseCreated = JsonObject().apply {
            addProperty("type", "response.created")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(responseCreated))
        
        // Send audio response chunks
        val audioDelta = JsonObject().apply {
            addProperty("type", "response.audio.delta")
            addProperty("delta", android.util.Base64.encodeToString(ByteArray(320) { 1 }, android.util.Base64.NO_WRAP))
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(audioDelta))
        
        // User starts speaking (interruption)
        val speechStarted = JsonObject().apply {
            addProperty("type", "input_audio_buffer.speech_started")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(speechStarted))
        
        // Verify interruption handling
        verify { mockWebSocket.send(match { it.contains("response.cancel") }) }
        
        // Simulate response cancelled
        val responseCancelled = JsonObject().apply {
            addProperty("type", "response.cancelled")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(responseCancelled))
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify system is ready for new input
        // Audio should continue to be sent for new interaction
        verify(atLeast = 1) { mockWebSocket.send(match { it.contains("input_audio_buffer.append") }) }
    }
    
    @Test
    fun `test session recovery after audio routing change`() {
        // This simulates the Bluetooth connection/disconnection scenario
        
        // Initialize session
        session.onShow(Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Simulate WebSocket connection
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Start audio handling
        session.onHandleAssist(VoiceInteractionSession.AssistState())
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Simulate audio routing failure (like Bluetooth disconnect)
        // Session should recover gracefully
        
        // Hide and show again (simulating force-kill and restart)
        session.onHide()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Clear previous mocks
        clearMocks(mockWebSocket)
        
        // Re-show session
        session.onShow(Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should reconnect successfully
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Verify session is functional again
        session.onHandleAssist(VoiceInteractionSession.AssistState())
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should be sending audio again
        verify(atLeast = 1) { mockWebSocket.send(match { it.contains("input_audio_buffer.append") }) }
    }
    
    @Test
    fun `test continuous audio stream maintains VAD functionality`() {
        // Test that audio keeps streaming even during playback
        
        session.onShow(Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST)
        testDispatcher.scheduler.advanceUntilIdle()
        
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Start listening
        session.onHandleAssist(VoiceInteractionSession.AssistState())
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Clear initial audio sends
        clearMocks(mockWebSocket, answers = false)
        
        // Start assistant speaking
        val responseCreated = JsonObject().apply {
            addProperty("type", "response.created")
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(responseCreated))
        
        val audioDelta = JsonObject().apply {
            addProperty("type", "response.audio.delta")
            addProperty("delta", android.util.Base64.encodeToString(ByteArray(320) { 1 }, android.util.Base64.NO_WRAP))
        }
        webSocketListener.onMessage(mockWebSocket, gson.toJson(audioDelta))
        
        // Advance time to allow audio recording to continue
        testDispatcher.scheduler.advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        
        // Verify audio is still being sent during playback
        verify(atLeast = 1) { mockWebSocket.send(match { it.contains("input_audio_buffer.append") }) }
    }
    
    @Test
    fun `test proper error recovery maintains session stability`() {
        session.onShow(Bundle(), VoiceInteractionSession.SHOW_WITH_ASSIST)
        testDispatcher.scheduler.advanceUntilIdle()
        
        webSocketListener.onOpen(mockWebSocket, mockk())
        
        // Simulate various errors
        val errors = listOf(
            JsonObject().apply {
                addProperty("type", "error")
                add("error", JsonObject().apply {
                    addProperty("code", "response_not_found")
                    addProperty("message", "Response not found")
                })
            },
            JsonObject().apply {
                addProperty("type", "response.cancel_failed")
                add("error", JsonObject().apply {
                    addProperty("code", "response_not_found")
                })
            }
        )
        
        errors.forEach { error ->
            webSocketListener.onMessage(mockWebSocket, gson.toJson(error))
        }
        
        // Session should remain functional
        session.onHandleAssist(VoiceInteractionSession.AssistState())
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should still be sending audio
        verify(atLeast = 1) { mockWebSocket.send(match { it.contains("input_audio_buffer.append") }) }
    }
    
    @Test
    fun `test lockscreen launch with proper wake management`() {
        // Test lockscreen-specific behavior
        
        session.onShow(Bundle(), VoiceInteractionSession.SHOW_WITH_SCREENSHOT)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify screen wake lock for lockscreen
        verify { 
            mockPowerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                any()
            )
        }
        
        // Verify wake lock is acquired
        verify { mockWakeLock.acquire(any()) }
        
        // Session should function normally
        webSocketListener.onOpen(mockWebSocket, mockk())
        session.onHandleAssist(VoiceInteractionSession.AssistState())
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Hide session
        session.onHide()
        
        // Verify wake lock is released
        verify { mockWakeLock.release() }
    }
}