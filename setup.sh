#!/bin/bash

echo "Android Voice Assistant Setup"
echo "============================"
echo ""

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
echo "To build the app, run:"
echo "  ./gradlew assembleDebug"
echo ""
echo "The APK will be in: app/build/outputs/apk/debug/"