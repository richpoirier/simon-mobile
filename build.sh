#!/bin/bash

echo "Android Voice Assistant Build Script (Linux)"
echo "============================================"
echo ""

# Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed!"
    echo "   Run: sudo apt install openjdk-17-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 11 ]; then
    echo "❌ Java $JAVA_VERSION found, but Java 11+ is required!"
    echo "   Run: sudo apt install openjdk-17-jdk"
    exit 1
fi

echo "✓ Using Java $JAVA_VERSION"
echo ""

# Check for local.properties
if [ ! -f "local.properties" ]; then
    if [ -z "$ANDROID_HOME" ]; then
        echo "❌ ANDROID_HOME not set and local.properties missing!"
        echo "   Run: ./setup-linux.sh"
        exit 1
    fi
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    echo "✓ Created local.properties"
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