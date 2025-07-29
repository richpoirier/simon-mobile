# Simon Mobile - Android Voice Assistant

Simon is a beautiful, minimal Android voice assistant that uses OpenAI's Realtime API for natural speech-to-speech conversations. It functions as a system-level digital assistant that can replace Google Assistant or other default assistants on Android devices.

## Features

- **System Integration**: Works as default Android digital assistant via side button
- **Real-time Conversations**: Natural speech-to-speech using OpenAI's WebRTC integration
- **Beautiful UI**: Text-free interface with smooth ripple animations
- **Semantic VAD**: Intelligent voice activity detection for natural conversation flow
- **Lock Screen Support**: Works from the lock screen with screen wake
- **Audio Quality**: High-quality audio with echo cancellation and noise suppression

## Setup

1. Clone the repository
2. Copy `config.properties.example` to `config.properties` in the project root
3. Add your OpenAI API key to `config.properties`:
   ```properties
   openai.api.key=sk-your-api-key-here
   ```
4. Build and install the app:
   ```bash
   ./gradlew installDebug
   ```
5. Set Simon as your default assistant:
   - Go to Settings → Apps → Default apps → Digital assistant app
   - Select "Simon" from the list
   - The side button/assistant gesture will now launch Simon

## Requirements

- Android device with API level 26+ (Android 8.0+)
- OpenAI API key with access to Realtime API
- Internet connection (WiFi or cellular)
- Android Studio Arctic Fox or later (for development)

## Architecture

The app uses:
- **WebRTC** for real-time audio streaming with OpenAI
- **Kotlin** for all application code
- **VoiceInteractionService** for Android system integration
- **Semantic VAD** for natural conversation endings
- **JavaAudioDeviceModule** for proper audio routing on Android

## Project Structure

```
app/src/main/java/com/simon/app/
├── audio/                 # Audio playback components
│   └── AudioPlayer.kt
├── config/               # Configuration management
│   └── ConfigManager.kt
├── ui/                   # Custom UI components
│   └── RippleView.kt
├── webrtc/               # WebRTC and OpenAI integration
│   └── OpenAIRealtimeClient.kt
├── MainActivity.kt       # Main launcher activity
├── VoiceAssistantService.kt       # System voice service
├── VoiceAssistantSession.kt       # Voice session handler
├── VoiceAssistantSessionService.kt # Session service
├── VoiceSessionActivity.kt        # Voice interaction UI
└── SimonRecognitionService.kt     # Recognition service stub
```

## Permissions

The app requires:
- `RECORD_AUDIO` - For voice input
- `INTERNET` - For OpenAI API connection
- `ACCESS_NETWORK_STATE` - For network monitoring
- `MODIFY_AUDIO_SETTINGS` - For audio routing control

## Development

Run tests:
```bash
./gradlew test
```

Build APK:
```bash
./gradlew assembleDebug
```

Clean and rebuild:
```bash
./gradlew clean assembleDebug
```

### Development on WSL

If you're developing on WSL (Windows Subsystem for Linux), you may encounter issues with Android SDK build tools. To resolve:

1. Install Android Studio on Windows (not WSL)
2. Use Android Studio's SDK Manager to install Build Tools 36.0.0
3. Either develop in Android Studio directly, or ensure your WSL `local.properties` points to the Windows SDK path

## Troubleshooting

### Simon doesn't appear in Digital Assistant settings
- Make sure the app is properly installed
- Try clearing Settings app data: `adb shell pm clear com.android.settings`
- Rebuild and reinstall the app

### Side button doesn't launch Simon
- Ensure Simon is selected as the default digital assistant
- If previously using an older version, the system may have cached the old package name
- Use ADB to manually set the assistant:
  ```bash
  adb shell settings put secure assistant "com.simon.app/.VoiceAssistantService"
  adb shell settings put secure voice_interaction_service "com.simon.app/.VoiceAssistantService"
  ```

### No audio playback
- Check that your device volume is not muted
- Ensure the app has all required permissions
- The app uses speaker output by default for voice responses

## License

Private project - not for redistribution