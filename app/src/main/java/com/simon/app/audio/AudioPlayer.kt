package com.simon.app.audio

import android.content.Context
import android.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AudioPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    suspend fun playReadyTone() = withContext(Dispatchers.IO) {
        try {
            // Request audio focus
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            
            audioManager.requestAudioFocus(focusRequest)
            
            // Generate a simple tone programmatically since we don't have the audio file
            playGeneratedTone()
        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback to generated tone
            playGeneratedTone()
        }
    }
    
    private fun playGeneratedTone() {
        // Generate a soft 440Hz tone for 200ms
        val duration = 0.2 // seconds
        val sampleRate = 44100
        val numSamples = (duration * sampleRate).toInt()
        val samples = DoubleArray(numSamples)
        val buffer = ByteArray(2 * numSamples)
        
        // Generate sine wave
        for (i in 0 until numSamples) {
            val angle = 2.0 * Math.PI * i / (sampleRate / 440.0)
            samples[i] = Math.sin(angle) * 0.3 // Lower volume for soft tone
        }
        
        // Convert to 16-bit PCM
        var idx = 0
        for (sample in samples) {
            val value = (sample * 32767).toInt().toShort()
            buffer[idx++] = (value.toInt() and 0x00ff).toByte()
            buffer[idx++] = ((value.toInt() and 0xff00) shr 8).toByte()
        }
        
        // Play the generated tone
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        
        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}