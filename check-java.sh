#!/bin/bash

echo "Checking Java configuration..."
echo ""

echo "Java version:"
java -version 2>&1 | head -3
echo ""

echo "JAVA_HOME: $JAVA_HOME"
echo ""

if command -v /usr/libexec/java_home &> /dev/null; then
    echo "Available Java versions on Mac:"
    /usr/libexec/java_home -V 2>&1 | grep -E "^\s"
    echo ""
    
    echo "Java 11+ locations:"
    /usr/libexec/java_home -v 11+ 2>/dev/null || echo "No Java 11+ found"
fi

echo ""
echo "To use Java 11 or higher, you can:"
echo "1. Set JAVA_HOME before running gradle:"
echo "   export JAVA_HOME=\$(/usr/libexec/java_home -v 11)"
echo "   ./gradlew assembleDebug"
echo ""
echo "2. Or add this to your ~/.zshrc or ~/.bash_profile:"
echo "   export JAVA_HOME=\$(/usr/libexec/java_home -v 11)"
echo ""
echo "3. Or install Java 11+ from:"
echo "   https://adoptium.net/"