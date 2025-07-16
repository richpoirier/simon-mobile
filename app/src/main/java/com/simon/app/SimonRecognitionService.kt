package com.simon.app

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

class SimonRecognitionService : RecognitionService() {
    
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        // We don't actually use this - OpenAI handles voice recognition
        listener?.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
    }
    
    override fun onCancel(listener: Callback?) {
        // Nothing to cancel
    }
    
    override fun onStopListening(listener: Callback?) {
        // Nothing to stop
    }
}