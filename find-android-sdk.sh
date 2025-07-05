#!/bin/bash

echo "Searching for Android SDK..."
echo ""

# Common Android SDK locations on Mac
SDK_LOCATIONS=(
    "$HOME/Library/Android/sdk"
    "$HOME/Android/sdk"
    "/usr/local/android-sdk"
    "/opt/android-sdk"
    "$HOME/Library/Android/Sdk"
    "/Applications/Android Studio.app/Contents/sdk"
)

FOUND_SDK=""

# Check ANDROID_HOME first
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    echo "✓ Found via ANDROID_HOME: $ANDROID_HOME"
    FOUND_SDK="$ANDROID_HOME"
else
    # Search common locations
    for SDK_PATH in "${SDK_LOCATIONS[@]}"; do
        if [ -d "$SDK_PATH" ]; then
            echo "✓ Found at: $SDK_PATH"
            FOUND_SDK="$SDK_PATH"
            break
        fi
    done
fi

if [ -z "$FOUND_SDK" ]; then
    echo "❌ Android SDK not found!"
    echo ""
    echo "Please install Android Studio from:"
    echo "https://developer.android.com/studio"
    echo ""
    echo "Or set ANDROID_HOME to your SDK location:"
    echo "export ANDROID_HOME=/path/to/android-sdk"
    exit 1
fi

# Create local.properties
echo ""
echo "Creating local.properties with SDK location..."
echo "sdk.dir=$FOUND_SDK" > local.properties
echo "✓ Created local.properties"

# Also suggest setting ANDROID_HOME
echo ""
echo "To make this permanent, add to your ~/.zshrc or ~/.bash_profile:"
echo "export ANDROID_HOME=\"$FOUND_SDK\""
echo "export PATH=\"\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools:\$PATH\""