@echo off
echo Android Voice Assistant Build Script
echo ====================================
echo.

:: Check Java version
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "JAVA_VERSION=%%j"
if %JAVA_VERSION% LSS 11 (
    echo [X] Java %JAVA_VERSION% found, but Java 11+ is required!
    echo     Please install Java 11+ from: https://adoptium.net/
    exit /b 1
)

echo [✓] Using Java %JAVA_VERSION%
java -version
echo.

:: Check for Android SDK
if not exist "local.properties" (
    echo Setting up Android SDK location...
    call find-android-sdk.bat
    if %errorlevel% neq 0 (
        exit /b 1
    )
    echo.
)

:: Check for config.properties
if not exist "config.properties" (
    echo Creating config.properties from example...
    copy config.properties.example config.properties >nul
    echo [!] Don't forget to add your OpenAI API key to config.properties!
    echo.
)

:: Run the build
echo Building app...
call gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo [✓] Build successful!
    echo   APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo [X] Build failed. See errors above.
)