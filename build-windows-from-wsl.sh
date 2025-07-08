#!/bin/bash

echo "Running Windows build from WSL..."
echo ""

# Convert WSL path to Windows path
WIN_PATH=$(wslpath -w "$(pwd)")

# Run PowerShell script
powershell.exe -ExecutionPolicy Bypass -File "$WIN_PATH\\build-windows.ps1"