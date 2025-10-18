package com.simon.app.framework

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionSession
import com.simon.app.voice.VoiceSessionActivity

/**
 * Bridge session that launches VoiceSessionActivity for actual voice interaction.
 * Required by Android's VoiceInteractionService framework.
 */
class VoiceAssistantSession(private val context: Context) : VoiceInteractionSession(context) {

    override fun onCreateContentView() = null

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        val intent = Intent(context, VoiceSessionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("launched_from_assistant", true)
        }
        context.startActivity(intent)

        // Allow activity to start before closing session
        Handler(Looper.getMainLooper()).postDelayed({ finish() }, 500)
    }
}