package com.simon.app

import android.content.Intent
import com.simon.app.framework.VoiceAssistantService
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController

@RunWith(RobolectricTestRunner::class)
class VoiceAssistantServiceTest {

    private lateinit var serviceController: ServiceController<VoiceAssistantService>
    private lateinit var service: VoiceAssistantService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        serviceController = Robolectric.buildService(VoiceAssistantService::class.java)
        service = serviceController.create().get()
    }

    @Test
    fun `test onBind can be called without crashing`() {
        val intent = Intent()
        val binder = service.onBind(intent)
        assert(binder == null)
    }

    @Test
    fun `test onUnbind can be called without crashing`() {
        val intent = Intent()
        val result = service.onUnbind(intent)
        assert(!result)
    }
}
