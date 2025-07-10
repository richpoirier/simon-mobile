#!/bin/bash

echo "Installing Voice Assistant to your Samsung Galaxy..."
echo ""

# Set up PATH
export PATH=$HOME/Android/Sdk/platform-tools:$PATH

# Check if device is connected
echo "Checking for connected devices..."
DEVICE=$(adb devices | grep -E "device$" | head -1 | awk '{print $1}')

if [ -z "$DEVICE" ]; then
    echo "❌ No device found!"
    echo ""
    echo "Please make sure:"
    echo "1. Your phone is connected via USB"
    echo "2. USB debugging is enabled"
    echo "3. You tapped 'Allow' on the USB debugging prompt"
    echo ""
    echo "Run 'adb devices' to check connection"
    exit 1
fi

echo "✓ Found device: $DEVICE"
echo ""

# Install APK
echo "Installing APK..."
adb install app/build/outputs/apk/debug/app-debug.apk

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Successfully installed!"
    echo ""
    echo "Next steps:"
    echo "1. Find 'Voice Assistant' in your app drawer"
    echo "2. Open it and grant microphone permission"
    echo "3. Add your OpenAI API key in the app settings"
    echo "4. Set as default assistant in Settings → Apps → Default apps"
else
    echo ""
    echo "❌ Installation failed"
fi