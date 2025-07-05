#!/bin/bash

# Build script that ensures Java 11+ is used

echo "Android Voice Assistant Build Script"
echo "===================================="
echo ""

# Check if Java 11+ is available
if command -v /usr/libexec/java_home &> /dev/null; then
    # On macOS, use java_home to find Java 11+
    JAVA_11_HOME=$(/usr/libexec/java_home -v 11+ 2>/dev/null)
    if [ -n "$JAVA_11_HOME" ]; then
        export JAVA_HOME="$JAVA_11_HOME"
        echo "✓ Using Java from: $JAVA_HOME"
    else
        echo "❌ Java 11 or higher not found!"
        echo "   Please install from: https://adoptium.net/"
        exit 1
    fi
else
    # On other systems, check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -lt 11 ]; then
        echo "❌ Java $JAVA_VERSION found, but Java 11+ is required!"
        echo "   Current JAVA_HOME: $JAVA_HOME"
        echo "   Please install Java 11+ from: https://adoptium.net/"
        exit 1
    fi
fi

echo ""
java -version
echo ""

# Check for Android SDK
if [ ! -f "local.properties" ]; then
    echo "Setting up Android SDK location..."
    ./find-android-sdk.sh
    if [ $? -ne 0 ]; then
        exit 1
    fi
    echo ""
fi

# Check for config.properties
if [ ! -f "config.properties" ]; then
    echo "Creating config.properties from example..."
    cp config.properties.example config.properties
    echo "⚠️  Don't forget to add your OpenAI API key to config.properties!"
    echo ""
fi

# Run the build
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