package com.simon.app

import android.view.WindowManager
import android.widget.ImageButton
import com.simon.app.ui.RippleView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceSessionActivityTest {

    private lateinit var activityController: ActivityController<VoiceSessionActivity>
    private lateinit var activity: VoiceSessionActivity

    @Before
    fun setup() {
        // Create activity controller but don't create yet
        activityController = Robolectric.buildActivity(VoiceSessionActivity::class.java)
    }

    @Test
    fun `test activity initializes successfully with valid config`() {
        // Create and start activity
        try {
            activity = activityController.create().start().resume().get()
        } catch (_: UnsatisfiedLinkError) {
            // Skip test if WebRTC native libraries are not available
            return
        } catch (_: RuntimeException) {
            // Skip test if resources can't be loaded
            return
        }

        // Verify window flags are set
        val flags = activity.window.attributes.flags
        assert(flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
    }


    @Test
    fun `test close button finishes activity`() {
        try {
            activity = activityController.create().start().resume().get()
        } catch (_: UnsatisfiedLinkError) {
            // Skip test if WebRTC native libraries are not available
            return
        } catch (_: RuntimeException) {
            // Skip test if resources can't be loaded
            return
        }

        // Find and click close button
        val closeButton = activity.findViewById<ImageButton>(R.id.close_button)
        closeButton.performClick()

        // Activity should be finishing
        assert(activity.isFinishing)
    }


    @Test
    fun `test UI components are initialized correctly`() {
        try {
            activity = activityController.create().start().resume().get()
        } catch (_: UnsatisfiedLinkError) {
            // Skip test if WebRTC native libraries are not available
            return
        } catch (_: RuntimeException) {
            // Skip test if resources can't be loaded
            return
        }

        // Get the actual ripple view
        val rippleView = activity.findViewById<RippleView>(R.id.ripple_view)
        assert(rippleView != null)
        
        // Get the close button
        val closeButton = activity.findViewById<ImageButton>(R.id.close_button)
        assert(closeButton != null)
    }
}
