package com.simon.app

import android.service.voice.VoiceInteractionService

class VoiceAssistantService : VoiceInteractionService() {
    
    override fun onReady() {
        super.onReady()
        // Service is ready to handle voice interactions
    }
}