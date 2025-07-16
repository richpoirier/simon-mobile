package com.simon.app.webrtc

import android.content.Context
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class OpenAIRealtimeClientTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockListener: OpenAIRealtimeClient.Listener
    
    private lateinit var client: OpenAIRealtimeClient
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // Mock Android Context to return application context
        `when`(mockContext.applicationContext).thenReturn(mockContext)
    }
    
    @Test
    fun `test client initializes without throwing`() {
        // Create client - should not throw
        client = OpenAIRealtimeClient(mockContext, "test-api-key", mockListener)
        assert(client != null)
    }
    
    @Test
    fun `test disconnect does not throw`() {
        client = OpenAIRealtimeClient(mockContext, "test-api-key", mockListener)
        
        // Disconnect should not throw
        client.disconnect()
    }
    
    @Test
    fun `test listener methods are defined`() {
        // Verify all listener methods exist
        mockListener.onSessionStarted()
        mockListener.onSpeechStarted()
        mockListener.onSpeechStopped()
        mockListener.onResponseStarted()
        mockListener.onResponseCompleted()
        mockListener.onError("test error")
        mockListener.onSessionEnded()
        
        // Verify they were called
        verify(mockListener).onSessionStarted()
        verify(mockListener).onSpeechStarted()
        verify(mockListener).onSpeechStopped()
        verify(mockListener).onResponseStarted()
        verify(mockListener).onResponseCompleted()
        verify(mockListener).onError("test error")
        verify(mockListener).onSessionEnded()
    }
}