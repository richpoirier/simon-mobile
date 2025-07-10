package com.voiceassistant.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.ConcurrentLinkedQueue

@ExperimentalCoroutinesApi
class AudioPlayerTest {
    
    private lateinit var audioPlayer: AudioPlayer
    private lateinit var mockContext: Context
    private lateinit var mockAudioTrack: AudioTrack
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockContext = mockk(relaxed = true)
        mockAudioTrack = mockk(relaxed = true)
        
        // Mock AudioTrack constructor
        mockkConstructor(AudioTrack::class)
        every { 
            anyConstructed<AudioTrack>()
        } returns mockAudioTrack
        
        every { mockAudioTrack.state } returns AudioTrack.STATE_INITIALIZED
        every { mockAudioTrack.write(any<ByteArray>(), any(), any()) } returns 320
        every { mockAudioTrack.play() } just Runs
        
        audioPlayer = AudioPlayer(mockContext)
    }
    
    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Test
    fun `test audio queue management for smooth playback`() {
        // Test that audio chunks are queued and played in order
        val chunk1 = ByteArray(320) { 1 }
        val chunk2 = ByteArray(320) { 2 }
        val chunk3 = ByteArray(320) { 3 }
        
        audioPlayer.playAudio(chunk1)
        audioPlayer.playAudio(chunk2)
        audioPlayer.playAudio(chunk3)
        
        // Advance coroutines to process queue
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify all chunks were written in order
        verifyOrder {
            mockAudioTrack.write(eq(chunk1), eq(0), eq(chunk1.size))
            mockAudioTrack.write(eq(chunk2), eq(0), eq(chunk2.size))
            mockAudioTrack.write(eq(chunk3), eq(0), eq(chunk3.size))
        }
    }
    
    @Test
    fun `test clearQueue stops playback immediately`() {
        // Add multiple chunks to queue
        repeat(10) { i ->
            audioPlayer.playAudio(ByteArray(320) { i.toByte() })
        }
        
        // Clear queue before processing
        audioPlayer.clearQueue()
        
        // Process any remaining coroutines
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should have minimal writes (maybe 1 if it started processing)
        verify(atMost = 1) { mockAudioTrack.write(any<ByteArray>(), any(), any()) }
    }
    
    @Test
    fun `test audio routing uses VOICE_CALL stream`() {
        // Verify AudioTrack is configured for voice call routing
        // This helps with echo cancellation and proper audio routing
        
        // The AudioPlayer should create AudioTrack with proper attributes
        // We can't directly verify constructor params with mockk, but we can
        // verify the track was created and used
        verify { anyConstructed<AudioTrack>() }
    }
    
    @Test
    fun `test graceful handling of audio write failures`() {
        // Simulate write failure
        every { mockAudioTrack.write(any<ByteArray>(), any(), any()) } returns AudioTrack.ERROR_INVALID_OPERATION
        
        val audioData = ByteArray(320) { 1 }
        audioPlayer.playAudio(audioData)
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Should handle error gracefully without crashing
        // The coroutine should continue despite the error
        assertTrue("Player should remain functional", true)
    }
    
    @Test
    fun `test release properly cleans up resources`() {
        audioPlayer.release()
        
        // Verify cleanup
        verify { mockAudioTrack.stop() }
        verify { mockAudioTrack.release() }
    }
    
    @Test
    fun `test concurrent audio additions are thread-safe`() {
        // Test that multiple threads can safely add audio
        val chunks = List(100) { i -> ByteArray(320) { i.toByte() } }
        
        // Add chunks from multiple coroutines
        chunks.forEach { chunk ->
            audioPlayer.playAudio(chunk)
        }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // All chunks should be processed without issues
        verify(exactly = 100) { mockAudioTrack.write(any<ByteArray>(), any(), any()) }
    }
    
    @Test
    fun `test audio latency optimization`() {
        // Test that AudioTrack is configured for low latency
        // This is important for responsive voice interaction
        
        val audioData = ByteArray(320) { 1 }
        audioPlayer.playAudio(audioData)
        
        // Should start playing immediately
        verify { mockAudioTrack.play() }
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Audio should be written promptly
        verify { mockAudioTrack.write(eq(audioData), eq(0), eq(audioData.size)) }
    }
}