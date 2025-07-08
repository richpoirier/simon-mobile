@echo off
echo Android Voice Assistant Setup for Windows
echo ==========================================
echo.

:: Check for Java
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo [X] Java is not installed. Please install Java 11 or higher.
    echo     You can download it from: https://adoptium.net/
    exit /b 1
) else (
    echo [✓] Java is installed
    java -version
    echo.
)

:: Check for Android SDK
if "%ANDROID_HOME%"=="" (
    echo [!] ANDROID_HOME is not set. Looking for Android SDK...
    
    :: Common Android SDK locations on Windows
    if exist "%LOCALAPPDATA%\Android\Sdk" (
        set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
        echo [✓] Found Android SDK at: %LOCALAPPDATA%\Android\Sdk
        echo     Add this to your System Environment Variables:
        echo     ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
    ) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk" (
        set ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk
        echo [✓] Found Android SDK at: %USERPROFILE%\AppData\Local\Android\Sdk
        echo     Add this to your System Environment Variables:
        echo     ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk
    ) else if exist "C:\Android\Sdk" (
        set ANDROID_HOME=C:\Android\Sdk
        echo [✓] Found Android SDK at: C:\Android\Sdk
        echo     Add this to your System Environment Variables:
        echo     ANDROID_HOME=C:\Android\Sdk
    ) else (
        echo [X] Android SDK not found. Please install Android Studio or Android SDK.
        echo     Download from: https://developer.android.com/studio
        exit /b 1
    )
) else (
    echo [✓] ANDROID_HOME is set to: %ANDROID_HOME%
)
echo.

:: Download gradle wrapper if not present
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo Downloading Gradle wrapper...
    if not exist "gradle\wrapper" mkdir gradle\wrapper
    powershell -Command "Invoke-WebRequest -Uri 'https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar' -OutFile 'gradle\wrapper\gradle-wrapper.jar'"
    echo [✓] Gradle wrapper downloaded
)

:: Create local.properties with SDK location
if not exist "local.properties" (
    echo sdk.dir=%ANDROID_HOME:\=/% > local.properties
    echo [✓] Created local.properties with SDK location
)

:: Check if config.properties exists
if not exist "config.properties" (
    echo Creating config.properties from example...
    copy config.properties.example config.properties >nul
    echo [✓] Created config.properties
    echo.
    echo [!] IMPORTANT: Edit config.properties and add your OpenAI API key
    echo     The file is already gitignored for security
) else (
    echo [✓] config.properties already exists
)

echo.
echo Setup complete! To build the app, run:
echo   build.bat
echo.
echo The APK will be in: app\build\outputs\apk\debug\
echo.