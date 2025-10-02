package com.simon.app.config

import android.content.Context
import java.util.Properties

class ConfigManager(private val context: Context) {

    private val properties = Properties().apply {
        context.assets.open("config.properties").use { load(it) }
    }
    
    fun getOpenAIApiKey(): String {
        return properties.getProperty("openai.api.key")
            ?: throw IllegalStateException("OpenAI API key not found in config.properties")
    }
}