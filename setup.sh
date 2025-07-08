#!/bin/bash

echo "Android Voice Assistant Setup for Linux/WSL"
echo "==========================================="
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed."
    echo ""
    echo "To install Java 17, run:"
    echo "  sudo apt update"
    echo "  sudo apt install openjdk-17-jdk"
    echo ""
    exit 1
else
    echo "✓ Java is installed"
    java -version
    echo ""
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Java $JAVA_VERSION found, but Java 11+ is required!"
    echo "   Install Java 17 with: sudo apt install openjdk-17-jdk"
    exit 1
fi

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME is not set. Looking for Android SDK..."
    
    # Common Android SDK locations
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        echo "✓ Found Android SDK at: $ANDROID_HOME"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
        echo "✓ Found Android SDK at: $ANDROID_HOME"
    else
        echo "❌ Android SDK not found."
        echo ""
        echo "To install Android SDK without Android Studio:"
        echo "  1. Download command line tools from:"
        echo "     https://developer.android.com/studio#command-tools"
        echo "  2. Extract to ~/Android"
        echo "  3. Run:"
        echo "     mkdir -p ~/Android/Sdk"
        echo "     cd ~/Android"
        echo "     ./cmdline-tools/bin/sdkmanager --sdk_root=~/Android/Sdk --install \"platform-tools\" \"platforms;android-34\" \"build-tools;33.0.1\""
        echo "  4. Add to ~/.bashrc:"
        echo "     export ANDROID_HOME=\$HOME/Android/Sdk"
        echo "     export PATH=\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools:\$PATH"
        exit 1
    fi
else
    echo "✓ ANDROID_HOME is set to: $ANDROID_HOME"
fi

# Create local.properties
if [ ! -f "local.properties" ]; then
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo "✓ Created local.properties"
fi

# Download gradle wrapper if not present
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo ""
    echo "Downloading Gradle wrapper..."
    mkdir -p gradle/wrapper
    curl -L https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar \
         -o gradle/wrapper/gradle-wrapper.jar
    echo "✓ Gradle wrapper downloaded"
fi

# Check if config.properties exists
if [ ! -f "config.properties" ]; then
    echo ""
    echo "Creating config.properties from example..."
    cp config.properties.example config.properties
    echo "✓ Created config.properties"
    echo ""
    echo "⚠️  IMPORTANT: Edit config.properties and add your OpenAI API key"
    echo "   The file is already gitignored for security"
else
    echo "✓ config.properties already exists"
fi

echo ""
echo "Setup complete! To build the app, run:"
echo "  ./gradlew assembleDebug"
echo ""
echo "The APK will be in: app/build/outputs/apk/debug/"