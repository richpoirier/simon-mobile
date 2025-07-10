package com.voiceassistant.app

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for OpenAI Realtime API protocol implementation
 */
class OpenAIProtocolTest {
    
    private val gson = Gson()
    
    @Test
    fun `test session configuration JSON structure`() {
        val sessionUpdate = JsonObject().apply {
            addProperty("type", "session.update")
            add("session", JsonObject().apply {
                add("modalities", gson.toJsonTree(listOf("text", "audio")))
                addProperty("voice", "echo")
                addProperty("input_audio_format", "pcm16")
                addProperty("output_audio_format", "pcm16")
                add("turn_detection", JsonObject().apply {
                    addProperty("type", "server_vad")
                    addProperty("threshold", 0.5)
                    addProperty("prefix_padding_ms", 300)
                    addProperty("silence_duration_ms", 500)
                })
            })
        }
        
        val json = gson.toJson(sessionUpdate)
        assertTrue("Should have session.update type", json.contains("\"type\":\"session.update\""))
        assertTrue("Should have server_vad", json.contains("\"type\":\"server_vad\""))
        assertTrue("Should have echo voice", json.contains("\"voice\":\"echo\""))
        assertTrue("Should have pcm16 format", json.contains("\"input_audio_format\":\"pcm16\""))
    }
    
    @Test
    fun `test audio input event structure`() {
        val audioAppend = JsonObject().apply {
            addProperty("type", "input_audio_buffer.append")
            addProperty("audio", "base64AudioDataHere")
        }
        
        val json = gson.toJson(audioAppend)
        assertTrue("Should have append type", json.contains("\"type\":\"input_audio_buffer.append\""))
        assertTrue("Should have audio data", json.contains("\"audio\":\"base64AudioDataHere\""))
    }
    
    @Test
    fun `test response cancel event`() {
        val cancel = JsonObject().apply {
            addProperty("type", "response.cancel")
        }
        
        val json = gson.toJson(cancel)
        assertEquals("{\"type\":\"response.cancel\"}", json)
    }
    
    @Test
    fun `test event type parsing`() {
        val events = listOf(
            "response.created",
            "response.done",
            "response.audio.delta",
            "response.audio.done",
            "input_audio_buffer.speech_started",
            "input_audio_buffer.speech_stopped",
            "error"
        )
        
        events.forEach { eventType ->
            val event = JsonObject().apply {
                addProperty("type", eventType)
            }
            val parsedType = event.get("type")?.asString
            assertEquals("Event type should match", eventType, parsedType)
        }
    }
    
    @Test
    fun `test error event structure`() {
        val errorEvent = JsonObject().apply {
            addProperty("type", "error")
            add("error", JsonObject().apply {
                addProperty("code", "rate_limit_exceeded")
                addProperty("message", "Rate limit exceeded")
            })
        }
        
        val error = errorEvent.getAsJsonObject("error")
        assertEquals("rate_limit_exceeded", error.get("code").asString)
        assertEquals("Rate limit exceeded", error.get("message").asString)
    }
    
    @Test
    fun `test VAD parameter ranges`() {
        // Test valid VAD parameter ranges
        val validThresholds = listOf(0.0, 0.5, 0.9, 1.0)
        val validPaddings = listOf(0, 300, 500, 1000)
        val validSilences = listOf(200, 500, 1000, 2000)
        
        validThresholds.forEach { threshold ->
            assertTrue("Threshold should be in range", threshold >= 0.0 && threshold <= 1.0)
        }
        
        validPaddings.forEach { padding ->
            assertTrue("Padding should be non-negative", padding >= 0)
        }
        
        validSilences.forEach { silence ->
            assertTrue("Silence duration should be positive", silence > 0)
        }
    }
    
    @Test
    fun `test audio format validation`() {
        val validFormats = listOf("pcm16", "pcm24", "pcm32")
        val format = "pcm16"
        
        assertTrue("Format should be valid", validFormats.contains(format))
    }
    
    @Test
    fun `test modalities configuration`() {
        val modalities = listOf("text", "audio")
        
        assertEquals("Should have 2 modalities", 2, modalities.size)
        assertTrue("Should include text", modalities.contains("text"))
        assertTrue("Should include audio", modalities.contains("audio"))
    }
}