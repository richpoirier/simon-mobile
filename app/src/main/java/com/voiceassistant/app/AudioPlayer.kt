package com.voiceassistant.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue

class AudioPlayer(private val context: Context) {
    companion object {
        private const val TAG = "AudioPlayer"
        // OpenAI sends 24kHz audio for voice responses
        private const val SAMPLE_RATE = 24000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private var isPlaying = false
    private var scope: CoroutineScope? = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        initAudioTrack()
        startPlaybackLoop()
    }

    private fun initAudioTrack() {
        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        Log.d(TAG, "AudioTrack buffer size: $bufferSize for sample rate: $SAMPLE_RATE")
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_CONFIG)
            .setEncoding(AUDIO_FORMAT)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        
        // Speed up playback by 15% (1.15x speed)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val params = audioTrack?.playbackParams
                params?.setSpeed(1.15f)
                params?.let { audioTrack?.playbackParams = it }
                Log.d(TAG, "Set playback speed to 1.15x")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set playback speed", e)
        }
        
        isPlaying = true
        Log.d(TAG, "AudioTrack initialized and playing")
    }

    private fun startPlaybackLoop() {
        scope?.launch {
            while (isPlaying) {
                try {
                    val audioData = audioQueue.take()
                    audioTrack?.write(audioData, 0, audioData.size)
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Playback loop interrupted")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in playback loop", e)
                }
            }
        }
    }

    fun playAudio(audioData: ByteArray) {
        try {
            audioQueue.offer(audioData)
            Log.d(TAG, "Queued ${audioData.size} bytes of audio, queue size: ${audioQueue.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error queuing audio", e)
        }
    }


    fun release() {
        isPlaying = false
        scope?.cancel()
        scope = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        audioQueue.clear()
    }
}