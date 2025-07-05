package com.voiceassistant.app

import android.content.Context
import android.util.Log
import java.io.IOException
import java.util.Properties

object ConfigManager {
    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE = "config.properties"
    private var properties: Properties? = null

    fun init(context: Context) {
        if (properties == null) {
            properties = Properties()
            try {
                context.assets.open(CONFIG_FILE).use { inputStream ->
                    properties?.load(inputStream)
                }
                Log.d(TAG, "Config loaded successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to load config file: ${e.message}")
                // Try to load from root directory for development
                try {
                    context.assets.open("../../../config.properties").use { inputStream ->
                        properties?.load(inputStream)
                    }
                    Log.d(TAG, "Config loaded from root directory")
                } catch (e2: IOException) {
                    Log.e(TAG, "Failed to load config from root: ${e2.message}")
                }
            }
        }
    }

    fun getApiKey(): String? {
        return properties?.getProperty("openai_api_key")?.takeIf { it != "your-api-key-here" }
    }
}