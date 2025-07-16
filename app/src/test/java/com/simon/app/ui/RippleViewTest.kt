package com.simon.app.ui

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class RippleViewTest {
    
    private lateinit var context: Context
    private lateinit var rippleView: RippleView
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = RuntimeEnvironment.getApplication()
        rippleView = RippleView(context)
    }
    
    @Test
    fun `test RippleView initializes successfully`() {
        assert(rippleView != null)
    }
    
    @Test
    fun `test startListeningAnimation does not throw`() {
        rippleView.startListeningAnimation()
    }
    
    @Test
    fun `test startSpeakingAnimation does not throw`() {
        rippleView.startSpeakingAnimation()
    }
    
    @Test
    fun `test switching between animations does not throw`() {
        rippleView.startListeningAnimation()
        rippleView.startSpeakingAnimation()
        rippleView.startListeningAnimation()
    }
}