package com.simon.app.config

import android.content.Context
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

class ConfigManager(private val context: Context) {
    
    private val properties = Properties()
    
    init {
        loadProperties()
    }
    
    private fun loadProperties() {
        try {
            context.assets.open("config.properties").use { inputStream ->
                properties.load(inputStream)
            }
        } catch (e: IOException) {
            throw IllegalStateException("config.properties not found in assets. Please copy config.properties to app/src/main/assets/", e)
        }
    }
    
    fun getOpenAIApiKey(): String {
        return properties.getProperty("openai.api.key")
            ?: throw IllegalStateException("OpenAI API key not found in config.properties")
    }
    
    fun getProperty(key: String, defaultValue: String? = null): String? {
        return properties.getProperty(key, defaultValue)
    }
}