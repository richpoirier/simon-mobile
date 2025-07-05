# Android Voice Assistant with OpenAI Realtime API

This Android app integrates with the OpenAI Realtime Voice API to create a digital assistant that can respond to voice queries.

## Features

- Voice interaction through Android's VoiceInteractionService
- Real-time voice-to-voice conversations using OpenAI's Realtime API
- WebSocket-based communication for low latency
- Audio recording and playback
- Integration with Android's assistant framework

## Setup

### Prerequisites
- Java 11 or higher
- Android SDK (via Android Studio or command line tools)

### Mac Setup
1. **Run the setup script**:
   ```bash
   ./setup-mac.sh
   ```
   This will check dependencies and download the Gradle wrapper.

2. **OpenAI API Key**: 
   - Get an API key from [OpenAI Platform](https://platform.openai.com)
   - Ensure you have access to the Realtime API (currently in beta)
   - Edit `config.properties` (created by setup script):
     ```properties
     openai_api_key=sk-your-actual-api-key-here
     ```

3. **Build the app**:
   ```bash
   ./gradlew assembleDebug
   ```

### Manual Setup (if setup script fails)
1. Install Java 11+: https://adoptium.net/
2. Install Android Studio: https://developer.android.com/studio
3. Set ANDROID_HOME in your shell profile:
   ```bash
   echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
   ```
4. Download Gradle wrapper manually:
   ```bash
   mkdir -p gradle/wrapper
   curl -L https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar \
        -o gradle/wrapper/gradle-wrapper.jar
   ```

3. **Install and configure**:
   - Install the APK on your Android device or emulator
   - Open the app to verify API key is configured
   - On a real device: Tap "Setup as Default Assistant" to configure the app as your voice assistant

## Running in Emulator

Yes, you can run this in an Android emulator! However, note:
- Voice assistant integration requires Android 6.0+ (API level 23+)
- The emulator has limited voice interaction capabilities
- You can test the UI and API connectivity, but full voice assistant functionality works best on real devices
- Make sure your emulator has:
  - Microphone support enabled
  - Internet access for API calls
  - Google Play Services (for better voice support)

## Technical Details

### Core Components

- **VoiceAssistantService**: Implements Android's VoiceInteractionService
- **VoiceAssistantSession**: Handles voice interaction sessions
- **OpenAIRealtimeClient**: WebSocket client for OpenAI Realtime API
- **AudioPlayer**: Handles audio playback of AI responses

### Audio Configuration

- Sample Rate: 16kHz
- Format: PCM 16-bit
- Channels: Mono

### Permissions Required

- RECORD_AUDIO
- INTERNET
- BIND_VOICE_INTERACTION

## Usage

Once configured as the default assistant:
1. Trigger the assistant (e.g., long-press home button or "Hey Google" replacement)
2. Speak your query
3. The assistant will process your voice input and respond with synthesized speech

## Notes

- The Realtime API is currently in beta with 15-minute session limits
- Ensure you have a stable internet connection for best performance
- API usage will incur costs based on OpenAI's pricing