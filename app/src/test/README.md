# Simon Voice Assistant - Unit Tests

This directory contains comprehensive unit tests for the Simon voice assistant Android app. The tests cover all the major issues and edge cases encountered during development.

## Test Coverage

### VoiceAssistantSessionTest
Tests the core voice interaction session functionality:
- **Session cleanup** - Ensures proper resource cleanup to prevent the "only works after force-kill" issue
- **Continuous audio streaming** - Verifies audio continues streaming during playback for interruption detection
- **Interruption handling** - Tests that interruptions properly clear audio queue and cancel responses
- **Audio routing recovery** - Tests recovery after Bluetooth audio routing changes
- **VAD threshold** - Verifies phantom interruption prevention through proper threshold handling
- **Language settings** - Ensures English-only responses
- **Wake lock management** - Tests proper wake lock levels for lockscreen scenarios
- **Audio completion** - Verifies isSpeaking flag management

### OpenAIRealtimeClientTest
Tests the OpenAI Realtime API WebSocket client:
- **Continuous audio requirement** - Verifies uninterrupted audio streaming for VAD
- **Session configuration** - Tests proper VAD settings (threshold, padding, silence duration)
- **Interruption detection** - Tests speech_started events during active responses
- **Response cancellation** - Verifies proper cancel event handling
- **Audio response handling** - Tests base64 decoding and callback invocation
- **Language configuration** - Verifies English instruction and voice settings
- **Error filtering** - Tests that transient errors (response_not_found) are filtered
- **Audio completion** - Tests audio done event handling
- **Reconnection** - Verifies clean disconnect and reconnect capability

### AudioPlayerTest
Tests the audio playback system:
- **Queue management** - Tests ordered playback of audio chunks
- **Clear queue** - Verifies immediate playback stop on interruption
- **Audio routing** - Tests VOICE_CALL stream usage for echo cancellation
- **Error handling** - Tests graceful handling of audio write failures
- **Resource cleanup** - Verifies proper release of AudioTrack
- **Thread safety** - Tests concurrent audio additions
- **Latency optimization** - Verifies low-latency configuration

### IntegrationTest
Tests the interaction between components:
- **Full interruption flow** - Tests complete flow from speech detection to audio stop
- **Session recovery** - Tests recovery after audio routing changes (Bluetooth scenario)
- **Continuous VAD** - Verifies audio streaming continues during playback
- **Error recovery** - Tests session stability after various errors
- **Lockscreen launch** - Tests wake lock management for lockscreen scenarios

### ConfigManagerTest
Tests API key configuration management:
- **Successful loading** - Tests normal API key loading from config.properties
- **Missing file handling** - Tests graceful handling of missing config
- **Empty/missing key** - Tests null return for invalid configurations
- **Special characters** - Tests API keys with special characters
- **Caching** - Verifies config is only read once
- **Error handling** - Tests graceful error recovery

## Running Tests

### Run all tests:
```bash
./run-tests.sh
```

### Run specific test class:
```bash
./gradlew test --tests "com.voiceassistant.app.VoiceAssistantSessionTest"
```

### Run with coverage:
```bash
./gradlew testDebugUnitTest
```

## Key Issues Addressed

1. **Audio Not Responding** - Tests ensure continuous audio streaming to OpenAI
2. **Force-Kill Required** - Session cleanup tests prevent resource conflicts
3. **Bluetooth Audio Issues** - Tests verify recovery from audio routing changes
4. **Phantom Interruptions** - VAD threshold tests prevent false speech detection
5. **Interruptions Not Working** - Tests ensure audio streams during playback
6. **Language Issues** - Tests verify English-only configuration
7. **Lockscreen Functionality** - Tests proper wake lock management

## Test Dependencies

- JUnit 4.13.2 - Core testing framework
- MockK 1.13.8 - Kotlin mocking library
- Coroutines Test 1.7.3 - Testing coroutines and async code
- Robolectric 4.11.1 - Android framework mocking