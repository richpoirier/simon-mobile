package com.voiceassistant.app

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log

class VoiceAssistantSessionService : VoiceInteractionSessionService() {
    companion object {
        private const val TAG = "VoiceAssistantSessionService"
    }

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.d(TAG, "Creating new voice interaction session")
        return VoiceAssistantSession(this)
    }
}