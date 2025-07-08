#!/bin/bash

echo "Android Voice Assistant Build Script (WSL)"
echo "=========================================="
echo ""

# Check if we're in WSL
if ! grep -qi microsoft /proc/version; then
    echo "This script is designed for WSL. Use build.sh instead."
    exit 1
fi

# Check for Java
if ! java -version &>/dev/null; then
    echo "❌ Java is not installed in WSL. Please install Java 11 or higher."
    echo "   Run: sudo apt update && sudo apt install openjdk-17-jdk"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Java $JAVA_VERSION found, but Java 11+ is required!"
    exit 1
fi

echo "✓ Using Java $JAVA_VERSION"
java -version
echo ""

# Find Android SDK
if [ -z "$ANDROID_HOME" ]; then
    # Common locations for Android SDK in WSL
    if [ -d "/mnt/c/Users/$USER/AppData/Local/Android/Sdk" ]; then
        export ANDROID_HOME="/mnt/c/Users/$USER/AppData/Local/Android/Sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    else
        echo "❌ Android SDK not found. Please set ANDROID_HOME"
        exit 1
    fi
fi

echo "✓ Using Android SDK: $ANDROID_HOME"

# Create local.properties if needed
if [ ! -f "local.properties" ]; then
    echo "Creating local.properties..."
    echo "sdk.dir=$ANDROID_HOME" > local.properties
fi

# Check for config.properties
if [ ! -f "config.properties" ]; then
    echo "Creating config.properties from example..."
    cp config.properties.example config.properties
    echo "⚠️  Don't forget to add your OpenAI API key to config.properties!"
    echo ""
fi

# Build the app
echo "Building app..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Build successful!"
    echo "  APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo ""
    echo "❌ Build failed. See errors above."
fi