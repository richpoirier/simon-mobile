package com.simon.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.view.View

class VoiceAssistantSession(private val context: Context) : VoiceInteractionSession(context) {
    
    override fun onCreateContentView(): View? {
        // We launch a separate activity instead of showing UI in the session
        return null
    }
    
    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        
        // TODO: Investigate proper assistant architecture - current approach launches a separate
        // activity and immediately closes the session, which seems like a workaround.
        // Consider either:
        // 1. Using the session's own UI (onCreateContentView) instead of launching an activity
        // 2. Keeping the session alive and communicating with the activity
        // 3. Using the session's voice interaction capabilities directly
        // The 500ms delay hack suggests we're fighting the framework instead of working with it.
        
        // Launch the voice session activity
        val intent = Intent(context, VoiceSessionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            putExtra("launched_from_assistant", true)
        }
        context.startActivity(intent)
        
        // Finish the session after a short delay to allow the activity to start
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            finish()
        }, 500)
    }
}