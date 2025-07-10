#!/bin/bash

echo "Starting local web server to serve APK..."
echo ""

# Get local IP address
IP=$(hostname -I | awk '{print $1}')
PORT=8080

echo "📱 On your Samsung S25 Edge:"
echo "1. Make sure you're on the same WiFi network"
echo "2. Open your browser and go to:"
echo ""
echo "   http://$IP:$PORT/app-debug.apk"
echo ""
echo "3. Download will start automatically"
echo "4. Once downloaded, tap to install"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start Python HTTP server in the APK directory
cd app/build/outputs/apk/debug/
python3 -m http.server $PORT