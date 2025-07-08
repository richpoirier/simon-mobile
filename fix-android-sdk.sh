#!/bin/bash

echo "Fixing Android SDK installation..."
echo ""

cd ~/Android

# Download if cmdline-tools.zip exists but wasn't extracted
if [ ! -f "cmdline-tools.zip" ] && [ ! -f "cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Downloading Android command line tools..."
    curl -L -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
fi

# Extract using Python since unzip is not available
if [ -f "cmdline-tools.zip" ]; then
    echo "Extracting using Python..."
    python3 << 'EOF'
import zipfile
import os
with zipfile.ZipFile('cmdline-tools.zip', 'r') as zip_ref:
    zip_ref.extractall('.')
print("✓ Extracted successfully")
EOF
    
    # Move to correct location
    if [ -d "cmdline-tools" ] && [ ! -d "cmdline-tools/latest" ]; then
        mv cmdline-tools cmdline-tools-temp
        mkdir -p cmdline-tools
        mv cmdline-tools-temp cmdline-tools/latest
    fi
    
    rm -f cmdline-tools.zip
fi

# Set up environment
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# Create SDK directory
mkdir -p "$ANDROID_HOME"

# Check if sdkmanager exists
if [ -f "$HOME/Android/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo ""
    echo "Installing Android SDK components..."
    cd "$HOME/Android/cmdline-tools/latest/bin"
    
    # Accept licenses
    yes | ./sdkmanager --sdk_root="$ANDROID_HOME" --licenses
    
    # Install required components
    ./sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-34" "build-tools;33.0.1"
    
    echo ""
    echo "✓ Android SDK setup complete!"
else
    echo "❌ sdkmanager not found"
    ls -la "$HOME/Android/cmdline-tools/"
fi

cd "$OLDPWD"