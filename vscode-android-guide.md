# VS Code for Android/Kotlin Development

## ✅ Installed Extensions

- **Kotlin Language Support** (`fwcd.kotlin`) - Syntax highlighting, code completion
- **Java Extension Pack** (`vscjava.vscode-java-pack`) - Java/Gradle support
  - Includes: Java Language Support, Debugger, Test Runner, Maven, Gradle

## 🚀 Quick Start

### Open Project
```bash
cd ~/src/simon-mobile
code .
```

### Key Features Now Available

1. **Kotlin Support**
   - Syntax highlighting
   - Code completion
   - Go to definition (F12)
   - Find references (Shift+F12)
   - Format code (Shift+Alt+F)

2. **Gradle Integration**
   - Run tasks from VS Code
   - View Gradle projects in sidebar
   - Auto-import dependencies

3. **Tasks** (Ctrl+Shift+P → "Tasks: Run Task")
   - Build Debug
   - Install Debug
   - Run Tests
   - Clean
   - Logcat

### Keyboard Shortcuts

| Action | Shortcut |
|--------|----------|
| Build | Ctrl+Shift+B |
| Run Task | Ctrl+Shift+P → "Tasks: Run Task" |
| Terminal | Ctrl+` |
| Command Palette | Ctrl+Shift+P |
| Go to File | Ctrl+P |
| Find in Files | Ctrl+Shift+F |

### Running/Building

1. **Build APK**:
   - Press `Ctrl+Shift+B` (default build task)
   - Or: Terminal → `./gradlew assembleDebug`

2. **Install & Run**:
   - `Ctrl+Shift+P` → "Tasks: Run Task" → "Install Debug"
   - Or: Terminal → `./gradlew installDebug`

3. **View Logs**:
   - `Ctrl+Shift+P` → "Tasks: Run Task" → "Logcat"
   - Or: Terminal → `adb logcat -s VoiceAssistant:*`

### File Navigation

- `Ctrl+P` - Quick file open
- `Ctrl+Shift+O` - Go to symbol in file
- `Ctrl+T` - Go to symbol in workspace

### Gradle Tasks (from sidebar)

1. Click Gradle icon in sidebar
2. Expand `simon-mobile` → `app` → `Tasks`
3. Double-click any task to run

### Terminal Commands

```bash
# Build
./gradlew build

# Install
./gradlew installDebug

# Run tests
./gradlew test

# Clean
./gradlew clean

# Continuous build
./gradlew build --continuous
```

## 🔧 Troubleshooting

### Java Home Not Found
If you see Java errors, check:
```bash
echo $JAVA_HOME
# If empty, add to ~/.bashrc:
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
```

### Gradle Sync Issues
- `Ctrl+Shift+P` → "Java: Reload Projects"
- Or restart VS Code

### Kotlin Not Working
- Make sure `.kt` files show "Kotlin" in bottom right
- `Ctrl+Shift+P` → "Kotlin: Restart Language Server"

## 📱 Device Management

### Connect Device
```bash
# USB
adb devices

# WiFi (after USB setup)
adb tcpip 5555
adb connect <device-ip>:5555
```

### Install APK
```bash
# From VS Code terminal
./gradlew installDebug

# Or directly
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🎯 VS Code vs Android Studio

### VS Code Advantages
- Lightweight and fast
- Better terminal integration
- Excellent for editing and git
- Works great in WSL

### Android Studio Advantages
- Full Android SDK integration
- Visual layout editor
- Built-in emulator
- Better debugging tools

### Recommendation
Use VS Code for:
- Quick edits
- Git operations
- Writing code
- Running builds

Use Android Studio for:
- Layout design
- Debugging complex issues
- Profiling
- Initial project setup

## 🔍 Additional Extensions (Optional)

```bash
# XML support for layouts
code --install-extension redhat.vscode-xml

# Git visualization
code --install-extension eamodio.gitlens

# Better file icons
code --install-extension vscode-icons-team.vscode-icons
```