# Simon Mobile - Android Voice Assistant

Simon is a beautiful, minimal Android voice assistant that uses OpenAI's Realtime API for natural speech-to-speech conversations.

## Features

- **Voice Activation**: Activate via Android assistant button or direct app launch
- **Real-time Conversations**: Natural speech-to-speech using OpenAI's WebRTC integration
- **Beautiful UI**: Text-free interface with smooth ripple animations
- **Semantic VAD**: Intelligent voice activity detection for natural conversation flow
- **Lock Screen Support**: Works from the lock screen with screen wake

## Setup

1. Clone the repository
2. Copy `config.properties.example` to `config.properties`
3. Add your OpenAI API key to `config.properties`:
   ```properties
   openai.api.key=sk-your-api-key-here
   ```
4. Build and install the app

## Requirements

- Android device with API level 26+ (Android 8.0+)
- OpenAI API key with access to Realtime API
- Internet connection (WiFi or cellular)

## Architecture

The app uses:
- **WebRTC** for real-time audio streaming
- **Kotlin** for all application code
- **VoiceInteractionService** for system integration
- **Semantic VAD** for natural conversation endings

## Permissions

The app requires:
- `RECORD_AUDIO` - For voice input
- `INTERNET` - For OpenAI API connection
- `BIND_VOICE_INTERACTION` - For assistant button integration

## Development

Run tests:
```bash
./gradlew test
```

Build APK:
```bash
./gradlew assembleDebug
```

## License

Private project - not for redistribution