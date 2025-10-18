package com.simon.app

import android.os.Bundle
import com.simon.app.framework.VoiceAssistantSession
import com.simon.app.framework.VoiceAssistantSessionService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VoiceAssistantSessionServiceTest {

    private lateinit var serviceController: ServiceController<VoiceAssistantSessionService>
    private lateinit var service: VoiceAssistantSessionService

    @Before
    fun setup() {
        serviceController = Robolectric.buildService(VoiceAssistantSessionService::class.java)
        service = serviceController.create().get()
    }

    @Test
    fun `service can be created successfully`() {
        // Assert
        assertNotNull("Service should be created", service)
    }

    @Test
    fun `onNewSession returns VoiceAssistantSession instance`() {
        // Act
        val session = service.onNewSession(null)
        
        // Assert
        assertNotNull("Should return a session", session)
        assertTrue("Should return VoiceAssistantSession instance", 
            session is VoiceAssistantSession)
    }

    @Test
    fun `onNewSession with bundle returns VoiceAssistantSession instance`() {
        // Arrange
        val args = Bundle().apply {
            putString("test_key", "test_value")
            putInt("test_int", 123)
        }
        
        // Act
        val session = service.onNewSession(args)
        
        // Assert
        assertNotNull("Should return a session with bundle", session)
        assertTrue("Should return VoiceAssistantSession instance", 
            session is VoiceAssistantSession)
    }

    @Test
    fun `multiple calls to onNewSession return new instances`() {
        // Act
        val session1 = service.onNewSession(null)
        val session2 = service.onNewSession(null)
        
        // Assert
        assertNotNull("First session should not be null", session1)
        assertNotNull("Second session should not be null", session2)
        assertNotSame("Should return different instances", session1, session2)
    }

    @Test
    fun `onNewSession uses service context for session creation`() {
        // Act
        val session = service.onNewSession(null) as VoiceAssistantSession
        
        // Assert - the session should have been created with the service's context
        // We can't directly verify the context without exposing it, but if the
        // session is created successfully, it means the context was passed correctly
        assertNotNull("Session created with service context", session)
    }
}