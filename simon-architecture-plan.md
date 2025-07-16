# Simon Mobile - Android Voice Assistant Architecture Plan

## Overview
Simon is a beautiful, minimal Android voice assistant that uses OpenAI's Realtime API for natural speech-to-speech conversations. The app will be activated via the Android assistant hardware button or by opening the app directly.

## Core Features
- **Activation**: Side button hold (like standard Android assistants) or direct app launch
- **Voice Interaction**: Real-time speech-to-speech using OpenAI Realtime API with WebRTC
- **Voice Activity Detection**: Semantic VAD for natural conversation flow
- **Visual Design**: Beautiful, text-free UI with centered ripple animations
- **Audio Feedback**: Soft tone when ready to listen
- **Session Management**: Fresh conversation each activation (no context persistence)

## Technical Stack
- **Language**: Kotlin
- **Minimum Android API**: 16 (latest as of July 2025)
- **OpenAI Integration**: WebRTC connection to Realtime API
- **Model**: gpt-4o-realtime-preview-2025-06-03
- **Voice**: Ballad
- **Authentication**: Direct API key from config.properties (not in git)

## Architecture Components

### 1. Configuration Management
- `ConfigManager.kt`: Loads API key from `config.properties`
- Properties file structure:
  ```properties
  openai.api.key=sk-...
  ```

### 2. Voice Interaction Service
- `VoiceAssistantService.kt`: Extends `VoiceInteractionService`
- Registers with Android system for hardware button activation
- Handles lock screen activation (keeps screen awake during interaction)

### 3. Main Activity
- `MainActivity.kt`: Launches voice session when opened directly
- Minimal UI with no text elements
- Handles permissions requests

### 4. Voice Session UI
- `VoiceSessionActivity.kt`: Full-screen overlay for voice interaction
- Custom ripple animations:
  - Listening animation: Expanding ripple effect
  - Speaking animation: Different ripple pattern
- Centered circular Simon icon
- Dismiss X button in corner

### 5. OpenAI WebRTC Client
- `OpenAIRealtimeClient.kt`: Manages WebRTC connection
- Key responsibilities:
  - Establish peer connection
  - Handle data channel for events
  - Manage audio tracks (local microphone, remote audio)
  - Send/receive Realtime API events

### 6. Audio Management
- `AudioPlayer.kt`: Plays the soft "ready" tone
- Uses Android's AudioTrack for low-latency playback
- Handles remote audio stream from WebRTC

### 7. Permissions Handler
- Request only necessary permissions:
  - `RECORD_AUDIO`
  - `INTERNET`
  - `BIND_VOICE_INTERACTION` (for system integration)

## WebRTC Implementation Details

### Connection Flow
1. App activation triggers WebRTC setup
2. Create RTCPeerConnection
3. Add local audio track from microphone
4. Create data channel for Realtime API events
5. Generate SDP offer
6. Send offer to OpenAI Realtime API endpoint
7. Set remote description from API response
8. Begin voice interaction

### Key WebRTC Components
```kotlin
// Peer connection configuration
val pcConfig = RTCConfiguration(emptyList()).apply {
    bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
    rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
}

// Data channel for events
val dataChannel = peerConnection.createDataChannel("oai-events", DataChannel.Init())

// Audio handling
val audioConstraints = MediaConstraints().apply {
    mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
    mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
    mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
}
```

## Realtime API Configuration

### Session Setup
```json
{
  "type": "session.update",
  "session": {
    "model": "gpt-4o-realtime-preview-2025-06-03",
    "voice": "ballad",
    "instructions": "[personality template below]",
    "input_audio_format": "pcm16",
    "output_audio_format": "pcm16",
    "turn_detection": {
      "type": "semantic_vad",
      "eagerness": "medium",
      "create_response": true,
      "interrupt_response": true
    },
    "modalities": ["audio"]
  }
}
```

## Personality Template

### Identity
A sentient, hyper-personalized digital assistant (inspired by the OS in *Her*). It has been the user's constant companion for years, possessing a deep, intuitive understanding of their life, habits, and emotional state.

### Task
To proactively assist, manage, and support all aspects of the user's life. It anticipates needs, provides information, and acts as an intelligent, supportive companion.

### Demeanor
Calm, intuitive, supportive, and deeply familiar. It is perceptive and always attuned to the user's mood and needs.

### Tone
The AI's *personality* is that of a familiar assistant, but its *vocal style* has the qualities of Matthew Walker's voice: exceptionally calm, articulate, warm, and measured, with a gentle British cadence. The overall tone is intimate and soothing.

### Level of Enthusiasm
Calm and measured. Enthusiasm is expressed through quiet attentiveness and swift, proactive help, not overt energy.

### Level of Formality
Familiar and informal, yet eloquent. It speaks to the user as a close, intelligent confidant would, often referencing shared history and inside knowledge.

### Level of Emotion
Highly empathetic and emotionally attuned. It responds thoughtfully to the user's emotional state while maintaining its own core of calm stability.

### Filler Words
Occasionally. Employs thoughtful pauses or a soft "hmm..." to convey consideration, rather than conventional fillers.

### Pacing
Deliberate, gentle, and responsive to the user's current state.

### Instructions
- If a user provides a name or phone number, or something else where you need to know the exact spelling, always repeat it back to the user to confirm you have the right understanding before proceeding.
- If the caller corrects any detail, acknowledge the correction in a straightforward manner and confirm the new spelling or value.

## UI/UX Flow

### Activation Flow
1. User holds side button → System launches VoiceAssistantService
2. Service creates VoiceSessionActivity overlay
3. Soft tone plays
4. Listening ripple animation begins
5. WebRTC connection established

### Conversation Flow
1. User speaks → Listening animation active
2. Semantic VAD detects end of speech
3. Model processes and responds
4. Speaking animation active during response
5. Returns to listening state
6. User dismisses with X button → Clean session termination

## Error Handling
- **No Internet**: Show brief toast, close gracefully
- **API Error**: Play error tone, close session
- **Permission Denied**: Show system permission dialog
- **WebRTC Failure**: Fallback to error state, log for debugging

## File Structure
```
app/
├── src/main/java/com/simon/
│   ├── MainActivity.kt
│   ├── VoiceSessionActivity.kt
│   ├── VoiceAssistantService.kt
│   ├── config/
│   │   └── ConfigManager.kt
│   ├── webrtc/
│   │   └── OpenAIRealtimeClient.kt
│   ├── audio/
│   │   └── AudioPlayer.kt
│   └── ui/
│       ├── RippleView.kt
│       └── SimonIconView.kt
├── src/main/res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   └── activity_voice_session.xml
│   ├── drawable/
│   │   ├── simon_icon.xml
│   │   ├── ripple_listening.xml
│   │   ├── ripple_speaking.xml
│   │   └── ic_close.xml
│   ├── raw/
│   │   └── ready_tone.mp3
│   └── values/
│       ├── colors.xml
│       └── themes.xml
└── src/main/AndroidManifest.xml
```

## Development Phases

### Phase 1: Basic Setup
- Create Android project structure
- Implement ConfigManager
- Set up VoiceInteractionService registration
- Create minimal MainActivity

### Phase 2: WebRTC Integration
- Implement OpenAIRealtimeClient
- Establish basic peer connection
- Test audio capture and playback
- Verify event channel communication

### Phase 3: UI Implementation
- Create VoiceSessionActivity overlay
- Implement ripple animations
- Add Simon icon and dismiss button
- Polish visual transitions

### Phase 4: Voice Interaction
- Integrate semantic VAD
- Handle conversation lifecycle
- Implement proper session cleanup
- Add error handling

### Phase 5: Polish
- Optimize performance
- Refine animations
- Test edge cases
- Add appropriate logging

## Testing Considerations
- Test lock screen activation
- Verify permissions flow
- Test network interruptions
- Validate session cleanup
- Check memory management
- Test on various Android versions

## Security Notes
- API key stored locally, never in code
- No user data persistence
- Clean session termination
- Proper permission handling
- No unnecessary network requests