package com.simon.app.framework

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Creates VoiceAssistantSession instances when the voice interaction service requests a new session.
 * Required component of Android's voice interaction framework.
 */
class VoiceAssistantSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = VoiceAssistantSession(this)
}