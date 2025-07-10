package com.voiceassistant.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceRecognitionService : RecognitionService() {
    companion object {
        private const val TAG = "VoiceRecognitionService"
    }

    override fun onStartListening(recognizerIntent: Intent, listener: Callback) {
        Log.d(TAG, "onStartListening called")
        // For now, just return an error since we're using OpenAI's API
        listener.error(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
    }

    override fun onStopListening(listener: Callback) {
        Log.d(TAG, "onStopListening called")
    }

    override fun onCancel(listener: Callback) {
        Log.d(TAG, "onCancel called")
    }
}