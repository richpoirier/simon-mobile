package com.simon.app.webrtc

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.webrtc.*

class OpenAIRealtimeClientIntegrationTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockListener: OpenAIRealtimeClient.Listener
    @Mock
    private lateinit var mockPeerConnectionFactory: PeerConnectionFactory
    @Mock
    private lateinit var mockPeerConnection: PeerConnection
    @Mock
    private lateinit var mockDataChannel: DataChannel
    @Captor
    private lateinit var sdpObserverCaptor: ArgumentCaptor<SdpObserver>
    @Captor
    private lateinit var peerConnectionObserverCaptor: ArgumentCaptor<PeerConnection.Observer>
    @Captor
    private lateinit var dataChannelObserverCaptor: ArgumentCaptor<DataChannel.Observer>
    @Captor
    private lateinit var bufferCaptor: ArgumentCaptor<DataChannel.Buffer>

    private lateinit var server: MockWebServer
    private lateinit var client: OpenAIRealtimeClient

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        server = MockWebServer()
        server.start()

        `when`(mockPeerConnectionFactory.createPeerConnection(any(PeerConnection.RTCConfiguration::class.java), peerConnectionObserverCaptor.capture()))
            .thenReturn(mockPeerConnection)
        `when`(mockPeerConnection.createDataChannel(anyString(), any(DataChannel.Init::class.java)))
            .thenReturn(mockDataChannel)
        `when`(mockPeerConnection.createOffer(sdpObserverCaptor.capture(), any(MediaConstraints::class.java)))
            .then {
                // Immediately succeed with a dummy offer
                sdpObserverCaptor.value.onCreateSuccess(SessionDescription(SessionDescription.Type.OFFER, "dummy-offer-sdp"))
            }

        client = OpenAIRealtimeClient(
            context = mockContext,
            apiKey = "test-api-key",
            listener = mockListener,
            peerConnectionFactory = mockPeerConnectionFactory,
            baseUrl = server.url("/").toString()
        )
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun `connect successfully completes handshake and sends session update`() = runTest {
        // Arrange: Server expects an SDP offer and will respond with a valid answer
        val sdpAnswer = "v=0
o=- 4596397390883466779 2 IN IP4 127.0.0.1
s=-
t=0 0
a=group:BUNDLE audio
a=msid-semantic: WMS
"
        server.enqueue(MockResponse().setResponseCode(200).setBody(sdpAnswer))
        
        // Act
        client.connect()

        // Assert: Verify the HTTP request was made correctly
        val recordedRequest = server.takeRequest()
        assertEquals("POST", recordedRequest.method)
        assertEquals("Bearer test-api-key", recordedRequest.getHeader("Authorization"))
        assertEquals("application/sdp", recordedRequest.getHeader("Content-Type"))

        // Assert: Verify the client sets the remote description from the server
        verify(mockPeerConnection).setLocalDescription(any(), any())
        verify(mockPeerConnection).setRemoteDescription(any(), any())
        
        // Trigger the DataChannel to open
        `when`(mockDataChannel.state()).thenReturn(DataChannel.State.OPEN)
        verify(mockDataChannel).registerObserver(dataChannelObserverCaptor.capture())
        dataChannelObserverCaptor.value.onStateChange()

        // Assert: Verify the correct session update JSON is sent
        verify(mockDataChannel).send(bufferCaptor.capture())
        val sentBuffer = bufferCaptor.value.data
        val sentJson = String(sentBuffer.array(), Charsets.UTF_8)
        val gson = Gson()
        val jsonObject = gson.fromJson(sentJson, JsonObject::class.java)

        assertEquals("session.update", jsonObject.get("type").asString)
        assertTrue(jsonObject.has("session"))
        val session = jsonObject.getAsJsonObject("session")
        assertEquals("semantic_vad", session.getAsJsonObject("turn_detection").get("type").asString)
    }
}
