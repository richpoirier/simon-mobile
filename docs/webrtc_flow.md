# WebRTC Connection Flow - Happy Path

This document shows the chronological flow through all callbacks when establishing a WebRTC connection with OpenAI's Realtime API.

## Timeline of Events

### 1. User Initiates Connection
```
USER ACTION: Triggers assistant (side button press)
↓
VoiceSessionActivity.onCreate()
↓
VoiceSessionActivity.initializeServices()
↓
client = OpenAIRealtimeClient(...)
↓
client.connect() ← ENTRY POINT
```

### 2. Connection Setup Phase
```
connect() 
├─> launches coroutine
└─> calls setupPeerConnection()
    ├─> Creates PeerConnection with factory
    ├─> Creates DataChannel "oai-events"
    │   └─> DataChannel.Observer registered (but not triggered yet - channel is CONNECTING)
    ├─> calls setupLocalAudio()
    │   ├─> Creates AudioSource with constraints
    │   ├─> Creates AudioTrack from AudioSource
    │   └─> Adds AudioTrack to PeerConnection
    └─> Returns to connect()
```

### 3. Offer Creation Phase
```
connect() continues
└─> calls createOffer()
    └─> peerConnection.createOffer() with SdpObserver
        └─> [CALLBACK] SdpObserver.onCreateSuccess(sdp)
            ├─> Receives SDP offer describing our capabilities
            └─> calls peerConnection.setLocalDescription() with new SdpObserver
                └─> [CALLBACK] SdpObserver.onSetSuccess()
                    └─> launches coroutine
                        └─> calls sendOfferToOpenAI(sdp)
```

### 4. HTTP Signaling Phase
```
sendOfferToOpenAI(sdp)
├─> Builds HTTP request with SDP offer as body
├─> Adds Authorization header with API key
├─> Sends POST to https://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2025-06-03
├─> Waits for response...
└─> Receives HTTP 200 with SDP answer in body
    └─> calls peerConnection.setRemoteDescription() with SdpObserver
        └─> [CALLBACK] SdpObserver.onSetSuccess()
            ├─> WebRTC connection is now ESTABLISHED
            └─> calls listener.onSessionStarted()
                └─> UI shows connection successful
```

### 5. DataChannel Opens (Parallel to above)
```
After setRemoteDescription succeeds, DataChannel automatically opens
└─> [CALLBACK] DataChannel.Observer.onStateChange()
    ├─> state == OPEN
    └─> calls sendSessionUpdate()
        ├─> Creates JSON with session config (voice, personality, instructions)
        └─> calls sendDataChannelMessage(json)
            └─> Sends through DataChannel to OpenAI
```

### 6. Session Configuration Acknowledgment
```
OpenAI processes session update
└─> [CALLBACK] DataChannel.Observer.onMessage(buffer)
    ├─> Receives: {"type": "session.created", ...}
    └─> calls handleServerEvent(message)
        └─> Logs "Session created"
```

### 7. Audio Starts Flowing (Automatic)
```
At this point, WebRTC handles audio automatically:

USER'S VOICE:
Microphone 
→ AudioTrack (with echo cancellation, noise suppression)
→ PeerConnection (encodes with Opus codec)
→ Network (RTP packets over UDP)
→ OpenAI servers

AI'S VOICE:
OpenAI servers
→ Network (RTP packets)
→ PeerConnection (decodes Opus)
→ [CALLBACK] PeerConnection.Observer.onTrack(transceiver)
    └─> track.setEnabled(true)
→ Automatically plays through speaker
```

### 8. Conversation Events Flow
```
USER STARTS SPEAKING:
OpenAI detects speech in audio stream
└─> [CALLBACK] DataChannel.Observer.onMessage(buffer)
    ├─> Receives: {"type": "input_audio_buffer.speech_started"}
    └─> handleServerEvent()
        └─> listener.onSpeechStarted()
            └─> UI shows user is speaking

USER STOPS SPEAKING:
OpenAI detects silence/end of sentence
└─> [CALLBACK] DataChannel.Observer.onMessage(buffer)
    ├─> Receives: {"type": "input_audio_buffer.speech_stopped"}
    └─> handleServerEvent()
        └─> listener.onSpeechStopped()
            └─> UI shows user stopped

AI STARTS RESPONDING:
└─> [CALLBACK] DataChannel.Observer.onMessage(buffer)
    ├─> Receives: {"type": "response.created"}
    └─> handleServerEvent()
        └─> listener.onResponseStarted()
            └─> UI shows AI is speaking

AI's audio automatically plays through speaker via WebRTC

AI FINISHES RESPONDING:
└─> [CALLBACK] DataChannel.Observer.onMessage(buffer)
    ├─> Receives: {"type": "response.done"}
    └─> handleServerEvent()
        └─> listener.onResponseCompleted()
            └─> UI shows AI finished
```

### 9. Disconnection Flow
```
USER ACTION: Closes assistant
↓
VoiceSessionActivity.onDestroy()
└─> client.disconnect()
    ├─> scope.cancel() - Stops all coroutines
    ├─> dataChannel.close() - Closes event channel
    ├─> localAudioTrack.dispose() - Stops microphone
    └─> peerConnection.close() - Ends WebRTC connection
```

## Summary of Callback Chain

1. **connect()** → launches coroutine
2. **createOffer()** → triggers **SdpObserver.onCreateSuccess**
3. **setLocalDescription()** → triggers **SdpObserver.onSetSuccess**
4. **sendOfferToOpenAI()** → HTTP request/response
5. **setRemoteDescription()** → triggers **SdpObserver.onSetSuccess**
6. Connection established → triggers **DataChannel.Observer.onStateChange**
7. **sendSessionUpdate()** → sends config
8. Ongoing: **DataChannel.Observer.onMessage** → handles all events
9. Ongoing: **PeerConnection.Observer.onTrack** → receives AI audio

## Key Points

- **Callbacks are nested**: Each callback triggers the next step
- **Audio is automatic**: Once connected, WebRTC handles all audio transport
- **DataChannel is for events only**: Audio never goes through DataChannel
- **Two parallel paths**: Connection establishment and DataChannel setup happen simultaneously
- **Coroutines manage async**: Kotlin coroutines handle the async nature without blocking