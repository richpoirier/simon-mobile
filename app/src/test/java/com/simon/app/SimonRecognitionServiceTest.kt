package com.simon.app

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
class SimonRecognitionServiceTest {

    private lateinit var serviceController: ServiceController<SimonRecognitionService>
    private lateinit var service: SimonRecognitionService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        serviceController = Robolectric.buildService(SimonRecognitionService::class.java)
        service = serviceController.create().get()
    }

    @Test
    fun `test onStartListening calls onError`() {
        val onStartListeningMethod: Method = SimonRecognitionService::class.java.getDeclaredMethod(
            "onStartListening",
            Intent::class.java,
            RecognitionService.Callback::class.java
        )
        onStartListeningMethod.isAccessible = true

        val mockCallback = mock(RecognitionService.Callback::class.java)
        onStartListeningMethod.invoke(service, null, mockCallback)
        verify(mockCallback).error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
    }

    @Test
    fun `test onStopListening does not throw`() {
        val onStopListeningMethod: Method = SimonRecognitionService::class.java.getDeclaredMethod(
            "onStopListening",
            RecognitionService.Callback::class.java
        )
        onStopListeningMethod.isAccessible = true
        onStopListeningMethod.invoke(service, null)
    }

    @Test
    fun `test onCancel does not throw`() {
        val onCancelMethod: Method = SimonRecognitionService::class.java.getDeclaredMethod(
            "onCancel",
            RecognitionService.Callback::class.java
        )
        onCancelMethod.isAccessible = true
        onCancelMethod.invoke(service, null)
    }
}
