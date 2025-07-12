#!/bin/bash

echo "🔧 Fixing VS Code Kotlin/Java imports..."
echo "========================================"

# 1. Set JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    echo "Setting JAVA_HOME..."
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
    echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> ~/.bashrc
fi

# 2. Clean and rebuild
echo ""
echo "📦 Refreshing dependencies..."
./gradlew clean build -x test --refresh-dependencies

# 3. Generate IDE files
echo ""
echo "🔄 Generating IDE metadata..."
./gradlew --stop
./gradlew cleanBuildCache
./gradlew build -x test

# 4. Clear VS Code Java workspace
echo ""
echo "🧹 Clearing VS Code caches..."
rm -rf ~/.config/Code/User/workspaceStorage/*

echo ""
echo "✅ Done! Now in VS Code:"
echo "   1. Press Ctrl+Shift+P"
echo "   2. Run 'Developer: Reload Window'"
echo "   3. Run 'Java: Reload Projects'"
echo "   4. Run 'Kotlin: Restart Language Server'"
echo ""
echo "If imports are still red:"
echo "   - Wait 30-60 seconds for indexing"
echo "   - Open a .kt file to trigger Kotlin LS"
echo "   - The project will still build fine!"