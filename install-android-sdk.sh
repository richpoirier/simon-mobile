#!/bin/bash

echo "Android SDK Command Line Tools Installer"
echo "========================================"
echo ""

# Check if SDK already exists
if [ -d "$HOME/Android/Sdk" ] && [ -f "$HOME/Android/Sdk/platform-tools/adb" ]; then
    echo "✓ Android SDK already installed at: $HOME/Android/Sdk"
    exit 0
fi

# Create directories
echo "Creating Android SDK directories..."
mkdir -p ~/Android/cmdline-tools
cd ~/Android

# Download command line tools
echo "Downloading Android command line tools..."
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
curl -L -o cmdline-tools.zip "$CMDLINE_TOOLS_URL"

# Extract
echo "Extracting tools..."
unzip -q cmdline-tools.zip
rm cmdline-tools.zip

# Move to correct location
mv cmdline-tools latest
mkdir -p cmdline-tools
mv latest cmdline-tools/

# Create SDK directory
mkdir -p ~/Android/Sdk

# Accept licenses and install required packages
echo ""
echo "Installing Android SDK components..."
echo "This will download several hundred MB of data..."
echo ""

cd ~/Android/cmdline-tools/latest/bin

# Accept all licenses
yes | ./sdkmanager --sdk_root="$HOME/Android/Sdk" --licenses

# Install required packages
./sdkmanager --sdk_root="$HOME/Android/Sdk" --install \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;33.0.1"

echo ""
echo "✓ Android SDK installed successfully!"
echo ""
echo "Add these lines to your ~/.bashrc or ~/.zshrc:"
echo ""
echo "export ANDROID_HOME=\$HOME/Android/Sdk"
echo "export PATH=\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/tools:\$PATH"
echo ""
echo "Then run: source ~/.bashrc"