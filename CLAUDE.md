# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Simon Mobile is an Android voice assistant that uses OpenAI's Realtime API for natural speech-to-speech conversations. It functions as a system-level digital assistant that can replace Google Assistant on Android devices.

## Build Commands

```bash
# Build and install debug APK
./gradlew installDebug

# Build APK only
./gradlew assembleDebug

# Clean build
./gradlew clean assembleDebug

# Run unit tests (avoid - see notes below)
./gradlew test

# Run specific test class
./gradlew :app:testDebugUnitTest --tests "*ClassName"

# Sync dependencies
./gradlew --refresh-dependencies
```

## Testing Considerations

**Important:** Unit tests that involve WebRTC components will fail with `UnsatisfiedLinkError` because native libraries (`jingle_peerconnection_so`) are not available in the test environment. Tests handle this by catching the error and skipping. Do not attempt to fix this by loading native libraries in tests.

## Architecture

### Core Components

1. **VoiceAssistantService** (`VoiceInteractionService`)
   - System-level service registered as default assistant
   - Launched by Android when user triggers assistant (side button/gesture)
   - Creates VoiceAssistantSession instances

2. **VoiceSessionActivity** 
   - UI for voice interactions (ripple animation)
   - Initializes WebRTC PeerConnectionFactory
   - Manages OpenAIRealtimeClient lifecycle
   - Handles audio routing (speaker/mic)

3. **OpenAIRealtimeClient** (`webrtc/`)
   - WebRTC client for OpenAI's Realtime API
   - Manages PeerConnection and DataChannel
   - Handles SDP offer/answer exchange
   - Sends session configuration with personality instructions

4. **SimonRecognitionService**
   - Stub implementation of RecognitionService
   - Required for Android voice service registration
   - Not actually used (OpenAI handles recognition)

### WebRTC Integration

The app uses WebRTC for real-time audio streaming:
- **PeerConnectionFactory**: Created in VoiceSessionActivity with JavaAudioDeviceModule for Android audio
- **DataChannel**: Used for sending/receiving OpenAI events (session.update, response events)
- **Audio Track**: Local audio captured and sent via WebRTC
- **EglBase**: Required for WebRTC even in audio-only apps

### Configuration

- API key stored in `config.properties` (not in version control)
- ConfigManager reads from BuildConfig at runtime
- Personality instructions hardcoded in OpenAIRealtimeClient

## Key Dependencies

- **WebRTC**: `io.github.webrtc-sdk:android:137.7151.03`
- **Kotlin Coroutines**: For async operations
- **OkHttp**: HTTP client for SDP exchange
- **Robolectric**: Android unit testing (v4.16-beta-1 for SDK 36 support)

## Development Gotchas

1. **Gradle Configuration**: Uses new syntax (`=` instead of method calls) for properties
2. **Java Version**: Requires Java 21 (jvmToolchain(21))
3. **Target SDK**: Currently 36 (Android 15)
4. **Build Tools**: May have issues on WSL - use Android Studio on Windows
5. **Mockito**: Configured as Java agent to avoid self-attachment warnings

## System Integration

To work as default assistant:
1. App must be installed
2. User must select Simon in Settings → Apps → Default apps → Digital assistant app
3. Or use ADB: `adb shell settings put secure assistant "com.simon.app/.VoiceAssistantService"`

## File Structure Notes

- Main app code: `/app/src/main/java/com/simon/app/`
- Tests: `/app/src/test/java/com/simon/app/`
- Resources: `/app/src/main/res/`
- Voice interaction config: `/app/src/main/res/xml/voice_interaction_service.xml`