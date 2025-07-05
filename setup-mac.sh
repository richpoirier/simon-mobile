#!/bin/bash

echo "Android Voice Assistant Setup for Mac"
echo "====================================="
echo ""

# Check for Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 11 or higher."
    echo "   You can download it from: https://adoptium.net/"
    exit 1
else
    echo "✓ Java is installed"
    java -version
    echo ""
fi

# Check for Android SDK
if [ -z "$ANDROID_HOME" ]; then
    echo "⚠️  ANDROID_HOME is not set. Looking for Android SDK..."
    
    # Common Android SDK locations on Mac
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        echo "✓ Found Android SDK at: $ANDROID_HOME"
        echo "   Add this to your ~/.zshrc or ~/.bash_profile:"
        echo "   export ANDROID_HOME=$ANDROID_HOME"
    else
        echo "❌ Android SDK not found. Please install Android Studio or Android SDK."
        echo "   Download from: https://developer.android.com/studio"
        exit 1
    fi
else
    echo "✓ ANDROID_HOME is set to: $ANDROID_HOME"
fi
echo ""

# Download gradle wrapper if not present
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "Downloading Gradle wrapper..."
    mkdir -p gradle/wrapper
    curl -L https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar \
         -o gradle/wrapper/gradle-wrapper.jar
    echo "✓ Gradle wrapper downloaded"
fi

# Check if config.properties exists
if [ ! -f "config.properties" ]; then
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
echo ""
echo "If you get a 'gradlew: command not found' error, run:"
echo "  chmod +x gradlew"