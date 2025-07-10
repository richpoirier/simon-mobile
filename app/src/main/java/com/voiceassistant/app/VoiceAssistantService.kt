package com.voiceassistant.app

import android.service.voice.VoiceInteractionService
import android.util.Log

class VoiceAssistantService : VoiceInteractionService() {
    companion object {
        private const val TAG = "VoiceAssistantService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceAssistantService created")
    }

    override fun onReady() {
        super.onReady()
        Log.d(TAG, "VoiceAssistantService ready")
    }

    override fun onShutdown() {
        super.onShutdown()
        Log.d(TAG, "VoiceAssistantService shutdown")
    }
}