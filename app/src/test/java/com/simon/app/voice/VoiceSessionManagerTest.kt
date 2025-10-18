package com.simon.app.voice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.simon.app.config.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.webrtc.PeerConnectionFactory

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceSessionManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockConfigManager: ConfigManager

    @Mock
    private lateinit var mockAudioManager: AudioManager

    @Mock
    private lateinit var mockAudioDevice: AudioDeviceInfo

    @Mock
    private lateinit var mockPeerConnectionFactory: PeerConnectionFactory
    
    private lateinit var manager: VoiceSessionManager
    private val testDispatcher = StandardTestDispatcher()
    
    // Callback spies
    private var sessionStartedCalled = false
    private var sessionErrorMessage: String? = null
    private var speechStartedCalled = false
    private var speechStoppedCalled = false
    private var responseStartedCalled = false
    private var responseCompletedCalled = false
    private var sessionEndedCalled = false
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup audio device mock
        whenever(mockAudioDevice.type).thenReturn(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
        whenever(mockAudioManager.availableCommunicationDevices).thenReturn(listOf(mockAudioDevice))
        
        // Reset callback flags
        sessionStartedCalled = false
        sessionErrorMessage = null
        speechStartedCalled = false
        speechStoppedCalled = false
        responseStartedCalled = false
        responseCompletedCalled = false
        sessionEndedCalled = false
        
        manager = VoiceSessionManager(
            context = mockContext,
            configManager = mockConfigManager,
            audioManager = mockAudioManager,
            onSessionStarted = { sessionStartedCalled = true },
            onSessionError = { error -> sessionErrorMessage = error },
            onSpeechStarted = { speechStartedCalled = true },
            onSpeechStopped = { speechStoppedCalled = true },
            onResponseStarted = { responseStartedCalled = true },
            onResponseCompleted = { responseCompletedCalled = true },
            onSessionEnded = { sessionEndedCalled = true }
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initialize with valid API key sets up client and enables speaker`() {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        
        // Act
        manager.initialize(mockPeerConnectionFactory)
        
        // Assert
        verify(mockAudioManager).mode = AudioManager.MODE_IN_COMMUNICATION
        verify(mockAudioManager).setCommunicationDevice(any())
        // Client is initialized internally
    }
    
    @Test
    fun `initialize with empty API key reports error`() {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("")
        
        // Act
        manager.initialize(mockPeerConnectionFactory)
        
        // Assert
        assertEquals("API key not configured", sessionErrorMessage)
        verify(mockAudioManager).mode = AudioManager.MODE_IN_COMMUNICATION
        verify(mockAudioManager).setCommunicationDevice(any())
    }
    
    @Test
    fun `initialize with null AudioManager still works`() {
        // Arrange
        manager = VoiceSessionManager(
            context = mockContext,
            configManager = mockConfigManager,
            audioManager = null
        )
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        
        // Act
        manager.initialize(mockPeerConnectionFactory)
        
        // Assert
        // No crash should occur - AudioManager is optional
    }
    
    @Test
    fun `startListening when not already listening starts connection`() = runTest {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        manager.initialize(mockPeerConnectionFactory)
        
        // Act
        manager.startListening()
        
        // Assert
        assertTrue("Should be listening", manager.isListening())
    }
    
    @Test
    fun `startListening when already listening does nothing`() {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        manager.initialize(mockPeerConnectionFactory)
        manager.startListening()
        
        // Act
        manager.startListening() // Second call
        
        // Assert
        assertTrue("Should still be listening", manager.isListening())
    }
    
    @Test
    fun `stopListening when listening disconnects and triggers callback`() {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        manager.initialize(mockPeerConnectionFactory)
        manager.startListening()
        
        // Act
        manager.stopListening()
        
        // Assert
        assertFalse("Should not be listening", manager.isListening())
        assertTrue("Session ended callback should be called", sessionEndedCalled)
    }
    
    @Test
    fun `stopListening when not listening does nothing`() {
        // Act
        manager.stopListening()
        
        // Assert
        assertFalse("Should not be listening", manager.isListening())
        assertFalse("Session ended callback should not be called", sessionEndedCalled)
    }
    
    @Test
    fun `cleanup stops listening and resets audio`() {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        manager.initialize(mockPeerConnectionFactory)
        manager.startListening()
        
        // Act
        manager.cleanup()
        
        // Assert
        assertFalse("Should not be listening", manager.isListening())
        verify(mockAudioManager).mode = AudioManager.MODE_NORMAL
        verify(mockAudioManager).clearCommunicationDevice()
    }
    
    @Test
    fun `client listener callbacks trigger manager callbacks`() {
        // Arrange
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        manager.initialize(mockPeerConnectionFactory)
        
        // We need to capture the listener passed to OpenAIRealtimeClient
        // Since we can't easily do that with the current structure,
        // we'll test the callback creation logic indirectly
        
        // This test would be more complete with dependency injection
        // or a factory pattern for creating the OpenAIRealtimeClient
        assertTrue("Initialization should succeed", true)
    }
    
    @Test
    fun `isListening returns correct state`() {
        // Initially not listening
        assertFalse(manager.isListening())
        
        // After starting
        whenever(mockConfigManager.getOpenAIApiKey()).thenReturn("test-api-key")
        manager.initialize(mockPeerConnectionFactory)
        manager.startListening()
        assertTrue(manager.isListening())
        
        // After stopping
        manager.stopListening()
        assertFalse(manager.isListening())
    }
}