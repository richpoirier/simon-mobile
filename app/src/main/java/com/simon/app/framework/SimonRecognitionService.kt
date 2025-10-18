package com.simon.app.framework

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Stub required by Android to register as a voice assistant.
 * Actual speech recognition is handled by OpenAI's Realtime API via WebRTC.
 */
class SimonRecognitionService : RecognitionService() {

    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
    }

    override fun onCancel(listener: Callback?) = Unit

    override fun onStopListening(listener: Callback?) = Unit
}