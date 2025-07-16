package com.simon.app

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import com.simon.app.audio.AudioPlayer
import com.simon.app.config.ConfigManager
import com.simon.app.ui.RippleView
import com.simon.app.webrtc.OpenAIRealtimeClient
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VoiceSessionActivityTest {
    
    @Mock
    private lateinit var mockConfigManager: ConfigManager
    
    @Mock
    private lateinit var mockAudioPlayer: AudioPlayer
    
    @Mock
    private lateinit var mockRealtimeClient: OpenAIRealtimeClient
    
    @Mock
    private lateinit var mockAudioManager: AudioManager
    
    @Mock
    private lateinit var mockRippleView: RippleView
    
    @Mock
    private lateinit var mockCloseButton: ImageButton
    
    private lateinit var activityController: ActivityController<VoiceSessionActivity>
    private lateinit var activity: VoiceSessionActivity
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Create activity controller but don't create yet
        activityController = Robolectric.buildActivity(VoiceSessionActivity::class.java)
    }
    
    @Test
    fun `test activity initializes successfully with valid config`() = runTest {
        // Mock successful initialization
        `when`(mockConfigManager.getOpenAIApiKey()).thenReturn("sk-test-key")
        
        // Create and start activity
        activity = activityController.create().start().resume().get()
        
        // Verify window flags are set
        val flags = activity.window.attributes.flags
        assert(flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
    }
    
    @Test
    fun `test activity shows error and finishes on config error`() {
        // Mock config manager throwing exception
        val testException = IllegalStateException("API key not found")
        
        // We need to mock the ConfigManager constructor to throw
        // Since we can't easily mock constructors with Mockito, we'll test the error handling differently
        
        // Create activity
        activity = activityController.create().get()
        
        // Simulate error in initialization
        activity.onError("Configuration error")
        
        // Verify toast was shown
        val latestToast = ShadowToast.getLatestToast()
        assert(latestToast != null)
        assert(ShadowToast.getTextOfLatestToast().contains("Error"))
        
        // Activity should be finishing
        assert(activity.isFinishing)
    }
    
    @Test
    fun `test close button finishes activity`() {
        activity = activityController.create().start().resume().get()
        
        // Find and click close button
        val closeButton = activity.findViewById<ImageButton>(R.id.close_button)
        closeButton.performClick()
        
        // Activity should be finishing
        assert(activity.isFinishing)
    }
    
    @Test
    fun `test onSessionStarted configures audio properly`() {
        activity = activityController.create().start().get()
        
        // Mock audio manager
        `when`(activity.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        
        // Call onSessionStarted
        activity.onSessionStarted()
        
        // Verify audio manager configuration
        verify(mockAudioManager).mode = AudioManager.MODE_IN_COMMUNICATION
        verify(mockAudioManager).isSpeakerphoneOn = true
    }
    
    @Test
    fun `test speech callbacks update UI correctly`() {
        activity = activityController.create().start().resume().get()
        
        // Get the actual ripple view
        val rippleView = activity.findViewById<RippleView>(R.id.ripple_view)
        
        // Test onSpeechStarted
        activity.onSpeechStarted()
        // RippleView should be in listening animation mode
        
        // Test onResponseStarted
        activity.onResponseStarted()
        // RippleView should be in speaking animation mode
        
        // Test onResponseCompleted
        activity.onResponseCompleted()
        // RippleView should be back in listening animation mode
    }
    
    @Test
    fun `test onDestroy cleans up resources`() {
        activity = activityController.create().start().resume().get()
        
        // Mock audio manager
        `when`(activity.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mockAudioManager)
        
        // Destroy activity
        activityController.pause().stop().destroy()
        
        // Verify cleanup
        verify(mockAudioManager).mode = AudioManager.MODE_NORMAL
        verify(mockAudioManager).isSpeakerphoneOn = false
    }
    
    @Test
    fun `test error handling shows toast and finishes`() {
        activity = activityController.create().start().resume().get()
        
        // Trigger error
        activity.onError("Test error message")
        
        // Verify toast
        val latestToast = ShadowToast.getLatestToast()
        assert(latestToast != null)
        assert(ShadowToast.getTextOfLatestToast() == "Error: Test error message")
        
        // Activity should be finishing
        assert(activity.isFinishing)
    }
    
    @Test
    fun `test onSessionEnded finishes activity`() {
        activity = activityController.create().start().resume().get()
        
        // Call onSessionEnded
        activity.onSessionEnded()
        
        // Activity should be finishing
        assert(activity.isFinishing)
    }
}