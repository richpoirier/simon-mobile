package com.simon.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Button
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MainActivityTest {

    private lateinit var activityController: ActivityController<MainActivity>
    private lateinit var activity: MainActivity

    @Before
    fun setup() {
        activityController = Robolectric.buildActivity(MainActivity::class.java)
    }

    @Test
    fun `onCreate hides action bar and sets up UI`() {
        // Act
        activity = activityController.create().get()

        // Assert
        assertNull("Action bar should be hidden", activity.supportActionBar?.isShowing)
        
        // Verify buttons are set up
        val setDefaultButton = activity.findViewById<Button>(R.id.set_default_button)
        assertNotNull("Set default button should exist", setDefaultButton)
        
        val testButton = activity.findViewById<Button>(R.id.test_button)
        assertNotNull("Test button should exist", testButton)
    }

    @Test
    fun `set default button launches voice input settings`() {
        // Arrange
        activity = activityController.create().get()
        val setDefaultButton = activity.findViewById<Button>(R.id.set_default_button)

        // Act
        setDefaultButton.performClick()

        // Assert
        val expectedIntent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        val actualIntent = shadowOf(activity).nextStartedActivity
        assertEquals(expectedIntent.action, actualIntent.action)
        
        // Check toast message
        assertEquals("Please select Simon as your default assistant", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun `test button requests permissions when not granted`() {
        // Arrange
        activity = activityController.create().get()
        val testButton = activity.findViewById<Button>(R.id.test_button)
        
        // Mock permissions not granted - no need to set anything, default is not granted
        val shadowActivity = shadowOf(activity)

        // Act
        testButton.performClick()

        // Assert - should request permissions
        val requestedPermissions = shadowActivity.lastRequestedPermission
        assertNotNull("Should request permissions", requestedPermissions)
        assertTrue("Should request RECORD_AUDIO", 
            requestedPermissions.requestedPermissions.contains(Manifest.permission.RECORD_AUDIO))
    }

    @Test
    fun `test button launches voice session when permissions granted`() {
        // Arrange
        activity = activityController.create().get()
        val testButton = activity.findViewById<Button>(R.id.test_button)
        
        // Grant permissions using ShadowApplication
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        shadowApplication.grantPermissions(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )

        // Act
        testButton.performClick()

        // Assert
        val startedIntent = shadowOf(activity).nextStartedActivity
        assertEquals(VoiceSessionActivity::class.java.name, startedIntent.component?.className)
        assertTrue("Activity should be finishing", activity.isFinishing)
    }

    @Test
    fun `onRequestPermissionsResult launches voice session when all permissions granted`() {
        // Arrange
        activity = activityController.create().get()
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET)
        val grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED)

        // Act
        activity.onRequestPermissionsResult(
            100, // PERMISSION_REQUEST_CODE
            permissions,
            grantResults
        )

        // Assert
        val startedIntent = shadowOf(activity).nextStartedActivity
        assertEquals(VoiceSessionActivity::class.java.name, startedIntent.component?.className)
        assertTrue("Activity should be finishing", activity.isFinishing)
    }

    @Test
    fun `onRequestPermissionsResult shows toast and finishes when permissions denied`() {
        // Arrange
        activity = activityController.create().get()
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        val grantResults = intArrayOf(PackageManager.PERMISSION_DENIED)

        // Act
        activity.onRequestPermissionsResult(
            100, // PERMISSION_REQUEST_CODE
            permissions,
            grantResults
        )

        // Assert
        assertEquals("Audio permission is required for voice interaction", ShadowToast.getTextOfLatestToast())
        assertTrue("Activity should be finishing", activity.isFinishing)
    }

    @Test
    fun `onRequestPermissionsResult ignores non-matching request codes`() {
        // Arrange
        activity = activityController.create().get()
        val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        val grantResults = intArrayOf(PackageManager.PERMISSION_DENIED)

        // Act
        activity.onRequestPermissionsResult(
            999, // Different request code
            permissions,
            grantResults
        )

        // Assert - nothing should happen
        assertNull("No intent should be started", shadowOf(activity).nextStartedActivity)
        assertNull("No toast should be shown", ShadowToast.getLatestToast())
        assertFalse("Activity should not be finishing", activity.isFinishing)
    }

    @Test
    fun `checkPermissionsAndLaunch only requests missing permissions`() {
        // Arrange
        activity = activityController.create().get()
        val testButton = activity.findViewById<Button>(R.id.test_button)
        
        // Grant only INTERNET permission
        val shadowApplication = shadowOf(RuntimeEnvironment.getApplication())
        shadowApplication.grantPermissions(Manifest.permission.INTERNET)

        // Act
        testButton.performClick()

        // Assert - should only request RECORD_AUDIO
        val shadowActivity = shadowOf(activity)
        val requestedPermissions = shadowActivity.lastRequestedPermission
        assertNotNull("Should request permissions", requestedPermissions)
        assertEquals(1, requestedPermissions.requestedPermissions.size)
        assertEquals(Manifest.permission.RECORD_AUDIO, requestedPermissions.requestedPermissions[0])
    }
}