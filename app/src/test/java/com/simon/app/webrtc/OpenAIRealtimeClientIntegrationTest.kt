package com.simon.app.webrtc

import android.content.Context
import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.webrtc.*
import java.nio.ByteBuffer

@ExperimentalCoroutinesApi
class OpenAIRealtimeClientIntegrationTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockAssetManager: AssetManager
    @Mock
    private lateinit var mockListener: OpenAIRealtimeClient.Listener
    @Mock
    private lateinit var mockPeerConnectionFactory: PeerConnectionFactory
    @Mock
    private lateinit var mockPeerConnection: PeerConnection
    @Mock
    private lateinit var mockDataChannel: DataChannel
    @Mock
    private lateinit var mockAudioSource: AudioSource
    @Mock  
    private lateinit var mockAudioTrack: AudioTrack
    @Mock
    private lateinit var mockRtpTransceiver: RtpTransceiver
    @Mock
    private lateinit var mockRtpReceiver: RtpReceiver
    @Mock
    private lateinit var mockRemoteAudioTrack: MediaStreamTrack
    @Captor
    private lateinit var peerConnectionObserverCaptor: ArgumentCaptor<PeerConnection.Observer>
    @Captor
    private lateinit var dataChannelObserverCaptor: ArgumentCaptor<DataChannel.Observer>
    @Captor
    private lateinit var bufferCaptor: ArgumentCaptor<DataChannel.Buffer>

    private lateinit var server: MockWebServer
    private lateinit var client: OpenAIRealtimeClient
    private lateinit var httpClient: OkHttpClient

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()

        // Setup mock context - by default prompt file returns a simple prompt
        `when`(mockContext.assets).thenReturn(mockAssetManager)
        // Default prompt for tests that don't specifically test prompt loading
        `when`(mockAssetManager.open("simon_prompt.md")).thenAnswer {
            java.io.ByteArrayInputStream("# Test Prompt".toByteArray())
        }

        // Setup PeerConnectionFactory mock to capture the observer
        `when`(mockPeerConnectionFactory.createPeerConnection(any(PeerConnection.RTCConfiguration::class.java), peerConnectionObserverCaptor.capture()))
            .thenReturn(mockPeerConnection)
        
        `when`(mockPeerConnection.createDataChannel(anyString(), any(DataChannel.Init::class.java)))
            .thenReturn(mockDataChannel)
        `when`(mockPeerConnectionFactory.createAudioSource(any(MediaConstraints::class.java)))
            .thenReturn(mockAudioSource)
        `when`(mockPeerConnectionFactory.createAudioTrack(anyString(), any(AudioSource::class.java)))
            .thenReturn(mockAudioTrack)
        `when`(mockPeerConnection.addTrack(any(MediaStreamTrack::class.java), any()))
            .thenReturn(mock(RtpSender::class.java))

        // Mock connectionState to avoid timeout - return CONNECTED immediately
        `when`(mockPeerConnection.connectionState())
            .thenReturn(PeerConnection.PeerConnectionState.CONNECTED)

        // Mock createOffer to immediately trigger callback
        doAnswer { invocation ->
            val observer = invocation.getArgument<SdpObserver>(0)
            observer.onCreateSuccess(SessionDescription(SessionDescription.Type.OFFER, "dummy-offer-sdp"))
            null
        }.`when`(mockPeerConnection).createOffer(any(SdpObserver::class.java), any(MediaConstraints::class.java))

        // Mock setLocalDescription to immediately trigger callback
        doAnswer { invocation ->
            val observer = invocation.getArgument<SdpObserver>(0)
            observer.onSetSuccess()
            null
        }.`when`(mockPeerConnection).setLocalDescription(any(SdpObserver::class.java), any(SessionDescription::class.java))

        // Mock setRemoteDescription to call onSetSuccess
        doAnswer { invocation ->
            val observer = invocation.getArgument<SdpObserver>(0)
            observer.onSetSuccess()
            null
        }.`when`(mockPeerConnection).setRemoteDescription(any(SdpObserver::class.java), any(SessionDescription::class.java))

        // Create OkHttpClient that uses the MockWebServer
        httpClient = OkHttpClient.Builder().build()

        client = OpenAIRealtimeClient(
            context = mockContext,
            apiKey = "test-api-key",
            listener = mockListener,
            peerConnectionFactory = mockPeerConnectionFactory,
            baseUrl = server.url("/").toString(),
            ioDispatcher = testDispatcher,
            httpClient = httpClient
        )
    }

    @After
    fun teardown() {
        server.shutdown()
        Dispatchers.resetMain()
    }

    @Test
    fun `connect successfully completes handshake and sends session update with valid prompt`() = runTest(testScheduler) {
        // Arrange
        // Use default prompt setup from setup()
        val sdpAnswer = """
            v=0
            o=- 4596397390883466779 2 IN IP4 127.0.0.1
            s=-
            t=0 0
            a=group:BUNDLE audio
            a=msid-semantic: WMS
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(sdpAnswer))

        // Act
        client.connect()
        testScheduler.advanceUntilIdle()

        // Assert
        val recordedRequest = server.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS)
        assertNotNull("No request was made to the server", recordedRequest)
        assertEquals("POST", recordedRequest?.method)
        assertEquals("/", recordedRequest?.path)
        assert(recordedRequest?.getHeader("Content-Type")?.startsWith("multipart/form-data") == true)
        verify(mockPeerConnection).setLocalDescription(any(SdpObserver::class.java), any(SessionDescription::class.java))
        verify(mockPeerConnection).setRemoteDescription(any(SdpObserver::class.java), any(SessionDescription::class.java))
        
        peerConnectionObserverCaptor.value.onDataChannel(mockDataChannel)
        verify(mockDataChannel).registerObserver(dataChannelObserverCaptor.capture())

        `when`(mockDataChannel.state()).thenReturn(DataChannel.State.OPEN)
        dataChannelObserverCaptor.value.onStateChange()
        testScheduler.advanceUntilIdle()

        verify(mockDataChannel).send(bufferCaptor.capture())
        val sentBuffer = bufferCaptor.value.data
        val sentBytes = ByteArray(sentBuffer.remaining())
        sentBuffer.get(sentBytes)
        val sentJson = String(sentBytes, Charsets.UTF_8)
        val jsonObject = Gson().fromJson(sentJson, JsonObject::class.java)

        assertEquals("session.update", jsonObject.get("type").asString)
        val session = jsonObject.getAsJsonObject("session")
        assertEquals("realtime", session.get("type").asString)
        assertEquals("gpt-realtime", session.get("model").asString)

        // Verify output_modalities
        val outputModalities = session.getAsJsonArray("output_modalities")
        assertNotNull("output_modalities should exist", outputModalities)
        assertEquals(1, outputModalities.size())
        assertEquals("audio", outputModalities.get(0).asString)

        val audio = session.getAsJsonObject("audio")
        assertNotNull("audio object should exist", audio)

        // Verify audio.input configuration
        val input = audio.getAsJsonObject("input")
        assertNotNull("audio.input should exist", input)

        val inputFormat = input.getAsJsonObject("format")
        assertNotNull("audio.input.format should exist", inputFormat)
        assertEquals("audio/pcm", inputFormat.get("type").asString)
        assertEquals(24000, inputFormat.get("rate").asInt)

        val turnDetection = input.getAsJsonObject("turn_detection")
        assertNotNull("audio.input.turn_detection should exist", turnDetection)
        assertEquals("semantic_vad", turnDetection.get("type").asString)
        assertEquals(true, turnDetection.get("create_response").asBoolean)
        assertEquals(true, turnDetection.get("interrupt_response").asBoolean)

        // Verify audio.output configuration
        val output = audio.getAsJsonObject("output")
        assertNotNull("output object should exist", output)

        // Note: audio.output.format should NOT be present - it causes crashes!
        assertNotNull("voice should be set", output.get("voice"))
        assertEquals("ballad", output.get("voice").asString)

        assertNotNull("instructions should be set", session.get("instructions"))
    }

    @Test
    fun connect_whenHttpFails_callsOnError() = runTest(testScheduler) {
        // Arrange
        // This test doesn't open the data channel, so no prompt is needed
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        // Act
        client.connect()
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockListener).onError(contains("HTTP 500"))
        verify(mockListener, never()).onSessionStarted()
    }

    @Test
    fun onMessage_whenErrorReceived_callsOnError() = runTest(testScheduler) {
        // Arrange
        val sdpAnswer = "v=0\r\n"
        server.enqueue(MockResponse().setResponseCode(200).setBody(sdpAnswer))
        client.connect()
        testScheduler.advanceUntilIdle()
        // Ensure onDataChannel and registerObserver are called before trying to get dataChannelObserverCaptor.value
        peerConnectionObserverCaptor.value.onDataChannel(mockDataChannel) // This line might be needed if connect doesn't trigger it due to other setup issues
        verify(mockDataChannel).registerObserver(dataChannelObserverCaptor.capture())
        val observer = dataChannelObserverCaptor.value

        // Act
        val errorJson = """{"type": "error", "message": "Session timed out"}"""
        simulateServerMessage(observer, errorJson)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(mockListener).onError("Session timed out")
    }

    @Test
    fun onMessage_whenConversationEventsReceived_callsCorrectListenerMethods() = runTest(testScheduler) {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(200).setBody("v=0\r\n"))
        client.connect()
        testScheduler.advanceUntilIdle()
        // Ensure onDataChannel and registerObserver are called
        peerConnectionObserverCaptor.value.onDataChannel(mockDataChannel) 
        verify(mockDataChannel).registerObserver(dataChannelObserverCaptor.capture())
        val observer = dataChannelObserverCaptor.value
        val ordering = inOrder(mockListener)

        // Act & Assert - Test session events (these don't call listener methods but should be handled)
        simulateServerMessage(observer, """{"type": "session.created"}""")
        testScheduler.advanceUntilIdle()
        // No listener method called for session.created, but event is logged
        
        simulateServerMessage(observer, """{"type": "session.updated"}""")
        testScheduler.advanceUntilIdle()
        // No listener method called for session.updated, but event is logged

        simulateServerMessage(observer, """{"type": "input_audio_buffer.speech_started"}""")
        testScheduler.advanceUntilIdle()
        ordering.verify(mockListener).onSpeechStarted()

        simulateServerMessage(observer, """{"type": "input_audio_buffer.speech_stopped"}""")
        testScheduler.advanceUntilIdle()
        ordering.verify(mockListener).onSpeechStopped()

        simulateServerMessage(observer, """{"type": "response.created"}""")
        testScheduler.advanceUntilIdle()
        ordering.verify(mockListener).onResponseStarted()

        simulateServerMessage(observer, """{"type": "response.done"}""")
        testScheduler.advanceUntilIdle()
        ordering.verify(mockListener).onResponseCompleted()
    }

    @Test
    fun onTrack_whenRemoteAudioTrackReceived_enablesTrack() = runTest(testScheduler) {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(200).setBody("v=0\r\n"))
        
        // Set up the mock transceiver and track
        `when`(mockRtpTransceiver.receiver).thenReturn(mockRtpReceiver)
        `when`(mockRtpReceiver.track()).thenReturn(mockRemoteAudioTrack)
        `when`(mockRemoteAudioTrack.kind()).thenReturn(MediaStreamTrack.AUDIO_TRACK_KIND)
        
        // Act
        client.connect()
        testScheduler.advanceUntilIdle()
        
        // Get the PeerConnection.Observer and simulate receiving a remote track
        val pcObserver = peerConnectionObserverCaptor.value
        pcObserver.onTrack(mockRtpTransceiver)
        
        // Assert
        verify(mockRemoteAudioTrack).setEnabled(true)
    }

    @Test
    fun onTrack_whenRemoteVideoTrackReceived_doesNotEnableTrack() = runTest(testScheduler) {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(200).setBody("v=0\r\n"))
        
        // Set up the mock transceiver with a video track
        `when`(mockRtpTransceiver.receiver).thenReturn(mockRtpReceiver)
        `when`(mockRtpReceiver.track()).thenReturn(mockRemoteAudioTrack)
        `when`(mockRemoteAudioTrack.kind()).thenReturn(MediaStreamTrack.VIDEO_TRACK_KIND)
        
        // Act
        client.connect()
        testScheduler.advanceUntilIdle()
        
        // Get the PeerConnection.Observer and simulate receiving a video track
        val pcObserver = peerConnectionObserverCaptor.value
        pcObserver.onTrack(mockRtpTransceiver)
        
        // Assert - video track should not be enabled
        verify(mockRemoteAudioTrack, never()).setEnabled(true)
    }

    @Test
    fun onTrack_whenReceiverHasNoTrack_doesNotCrash() = runTest(testScheduler) {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(200).setBody("v=0\r\n"))
        
        // Set up the mock transceiver with no track
        `when`(mockRtpTransceiver.receiver).thenReturn(mockRtpReceiver)
        `when`(mockRtpReceiver.track()).thenReturn(null)
        
        // Act
        client.connect()
        testScheduler.advanceUntilIdle()
        
        // Get the PeerConnection.Observer and simulate receiving a transceiver with no track
        val pcObserver = peerConnectionObserverCaptor.value
        pcObserver.onTrack(mockRtpTransceiver)
        
        // Assert - should handle gracefully, no crash
        verify(mockRemoteAudioTrack, never()).setEnabled(true)
    }

    @Test
    fun sendOfferToOpenAI_whenSetRemoteDescriptionFails_callsOnError() = runTest(testScheduler) {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(200).setBody("v=0\r\n"))
        
        // Mock setRemoteDescription to call onSetFailure
        doAnswer { invocation ->
            val observer = invocation.getArgument<SdpObserver>(0)
            observer.onSetFailure("Failed to set remote description")
            null
        }.`when`(mockPeerConnection).setRemoteDescription(any(SdpObserver::class.java), any(SessionDescription::class.java))
        
        // Act
        client.connect()
        testScheduler.advanceUntilIdle()
        
        // Assert
        verify(mockListener).onError("Failed to set remote description")
    }

    @Test
    fun disconnect_closesPeerConnectionAndDataChannel() = runTest(testScheduler) {
        // Arrange
        server.enqueue(MockResponse().setResponseCode(200).setBody("v=0\r\n"))
        client.connect()
        testScheduler.advanceUntilIdle()
        // Ensure onDataChannel is called if client.connect() implies it
        peerConnectionObserverCaptor.value.onDataChannel(mockDataChannel)
        verify(mockPeerConnection).createDataChannel(anyString(), any(DataChannel.Init::class.java))


        // Act
        client.disconnect()

        // Assert
        verify(mockPeerConnection).close()
        verify(mockDataChannel).close()
    }

    private fun simulateServerMessage(observer: DataChannel.Observer, json: String) {
        val buffer = ByteBuffer.wrap(json.toByteArray())
        observer.onMessage(DataChannel.Buffer(buffer, false))
    }
}
