# Fix Unresolved References in VS Code

## Quick Fixes (Try in order)

### 1. Reload VS Code Window
- `Ctrl+Shift+P` → "Developer: Reload Window"

### 2. Clean Java Workspace  
- `Ctrl+Shift+P` → "Java: Reload Projects"
- `Ctrl+Shift+P` → "Java: Force Java Compilation" → "Full"
- `Ctrl+Shift+P` → "Java: Clean Workspace"

### 3. Kotlin Language Server
- `Ctrl+Shift+P` → "Kotlin: Restart Language Server"

### 4. Build from Terminal
```bash
# This ensures all dependencies are downloaded
./gradlew build -x test
```

### 5. Clear VS Code Caches
```bash
# Close VS Code first
rm -rf ~/.config/Code/CachedData/*
rm -rf ~/.config/Code/Cache/*
rm -rf ~/.config/Code/CachedExtensionVSIXs/*
```

### 6. Check Java Home
```bash
# Make sure JAVA_HOME is set
echo $JAVA_HOME
# If not set, add to ~/.bashrc:
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### 7. VS Code Settings
Check `.vscode/settings.json` has correct Java path:
```json
"java.home": "/usr/lib/jvm/java-11-openjdk-amd64"
```

### 8. Gradle Sync
```bash
# Force dependency sync
./gradlew --refresh-dependencies dependencies
```

### 9. If Still Not Working
Create this file to force indexing:

`.vscode/tasks.json` → Add this task:
```json
{
    "label": "Sync Gradle",
    "type": "shell",
    "command": "./gradlew",
    "args": ["--refresh-dependencies", "build", "-x", "test"],
    "problemMatcher": "$gradle"
}
```

Then run: `Ctrl+Shift+P` → "Tasks: Run Task" → "Sync Gradle"

## Common Issues

### Red underlines everywhere
- Usually means Java/Kotlin extension isn't running
- Check bottom right corner - should show "Java" and file type

### Specific imports not found
- Dependencies might not be downloaded
- Run: `./gradlew dependencies`

### Android SDK classes not found
- VS Code doesn't have full Android SDK support
- This is normal - code will still compile

### Still having issues?
1. Use Android Studio for complex refactoring
2. VS Code is best for editing/quick changes
3. Command line builds always work even if VS Code shows errors