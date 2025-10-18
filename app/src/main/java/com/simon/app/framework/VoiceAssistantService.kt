package com.simon.app.framework

import android.service.voice.VoiceInteractionService

/**
 * System-level voice interaction service registered as the device's digital assistant.
 * Android launches this when the user triggers the assistant (side button/gesture).
 */
class VoiceAssistantService : VoiceInteractionService()