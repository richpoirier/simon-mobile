package com.voiceassistant.app

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.util.Log

class VoiceAssistantSessionService : VoiceInteractionSessionService() {
    companion object {
        private const val TAG = "VoiceAssistantSessionService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceAssistantSessionService onCreate")
    }

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        Log.d(TAG, "onNewSession called with args: $args")
        val session = VoiceAssistantSession(this)
        Log.d(TAG, "Created VoiceAssistantSession instance")
        return session
    }

    override fun onDestroy() {
        Log.d(TAG, "VoiceAssistantSessionService onDestroy")
        super.onDestroy()
    }
}