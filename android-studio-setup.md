# Android Studio with WSL Setup

## Quick Start (Recommended)

1. **Open project from Windows Explorer**:
   - Press `Win + R`
   - Type: `\\wsl$\Ubuntu\home\rich\src\simon-mobile`
   - Right-click → "Open with Android Studio"

2. **Or use the provided batch file**:
   - From Windows: Run `\\wsl$\Ubuntu\home\rich\src\simon-mobile\open-in-android-studio.bat`

## Android Studio Configuration

### Terminal Integration
Already configured in `.idea/terminal.xml` to use WSL

### Gradle Settings
- File → Settings → Build, Execution, Deployment → Build Tools → Gradle
- Gradle JDK: Use Android Studio's embedded JDK
- ✓ Enable "Use Gradle from: 'gradle-wrapper.properties'"

### SDK Location
- File → Project Structure → SDK Location
- Android SDK: Use your Windows Android SDK (typically `C:\Users\[YourName]\AppData\Local\Android\Sdk`)

### Git Integration
- File → Settings → Version Control → Git
- Path: `\\wsl$\Ubuntu\usr\bin\git`

## Running/Debugging

### Device Connection
- USB debugging works normally (adb runs on Windows)
- WiFi debugging: Same as before
- Emulator: Runs on Windows, accessible from WSL

### Build from Android Studio
- Click "Run" button as normal
- Gradle executes in WSL automatically
- APK installs via Windows adb

### Terminal Commands in Android Studio
- Open terminal (Alt + F12)
- You're now in WSL bash!
- Run: `./gradlew build`, `git status`, etc.

## Performance Tips

1. **Exclude WSL paths from Windows Defender**:
   ```powershell
   # Run in PowerShell as Admin
   Add-MpPreference -ExclusionPath "\\wsl$\Ubuntu"
   ```

2. **Increase WSL2 memory** (create `.wslconfig` in Windows home):
   ```ini
   [wsl2]
   memory=8GB
   processors=4
   ```

3. **File Watching**: Add to `gradle.properties`:
   ```properties
   # Better file watching for WSL
   org.gradle.vfs.watch=true
   ```

## Troubleshooting

### Slow Gradle Builds
- Keep project files in WSL (not /mnt/c/)
- Use `--parallel` flag: `./gradlew build --parallel`

### Can't Open Project
- Ensure path uses `\\wsl$\` not `\\wsl.localhost\`
- Check WSL is running: `wsl --list --running`

### Git Line Endings
Already configured in `.gitattributes`

## Alternative: IntelliJ IDEA Ultimate
Has better WSL integration if you have a license