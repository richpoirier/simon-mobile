package com.simon.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Hide action bar for minimal UI
        supportActionBar?.hide()
        
        // Setup UI
        setupUI()
    }
    
    private fun checkPermissionsAndLaunch() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            launchVoiceSession()
        } else {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                launchVoiceSession()
            } else {
                Toast.makeText(
                    this,
                    "Audio permission is required for voice interaction",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
    
    private fun launchVoiceSession() {
        val intent = Intent(this, VoiceSessionActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun setupUI() {
        // Setup buttons
        findViewById<Button>(R.id.set_default_button).setOnClickListener {
            val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
            try {
                startActivity(intent)
                Toast.makeText(this, "Please select Simon as your default assistant", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Fallback to general assistant settings
                val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
                startActivity(fallbackIntent)
                Toast.makeText(this, "Go to Apps > Default apps > Digital assistant app", Toast.LENGTH_LONG).show()
            }
        }
        
        findViewById<Button>(R.id.test_button).setOnClickListener {
            checkPermissionsAndLaunch()
        }
    }
}