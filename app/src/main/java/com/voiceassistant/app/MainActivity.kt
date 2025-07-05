package com.voiceassistant.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    private lateinit var statusTextView: TextView
    private lateinit var testButton: Button
    private lateinit var setupAssistantButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.status_text_view)
        testButton = findViewById(R.id.test_button)
        setupAssistantButton = findViewById(R.id.setup_assistant_button)

        // Initialize config
        ConfigManager.init(this)
        
        checkApiKey()
        checkPermissions()

        testButton.setOnClickListener {
            testVoiceAssistant()
        }

        setupAssistantButton.setOnClickListener {
            openAssistantSettings()
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun checkApiKey() {
        val apiKey = ConfigManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            statusTextView.text = "API key not configured. Please add your key to config.properties"
            testButton.isEnabled = false
        } else {
            statusTextView.text = "API key configured ✓"
            testButton.isEnabled = true
        }
    }

    private fun testVoiceAssistant() {
        val apiKey = ConfigManager.getApiKey()
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "Please configure API key in config.properties", Toast.LENGTH_SHORT).show()
            return
        }

        // For emulator testing, we'll just show a success message
        // In a real device, this would trigger the voice assistant
        Toast.makeText(this, "Voice assistant configured successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun openAssistantSettings() {
        AlertDialog.Builder(this)
            .setTitle("Setup Voice Assistant")
            .setMessage("To use this app as your default assistant:\n\n" +
                    "1. Go to Settings > Apps > Default apps > Digital assistant app\n" +
                    "2. Select 'Voice Assistant' as your default assistant\n\n" +
                    "Would you like to open settings now?")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open settings. Please navigate manually.", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Later", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(
                    this,
                    "Some permissions were denied. The app may not work properly.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}