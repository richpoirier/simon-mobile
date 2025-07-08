@echo off
echo Searching for Android SDK...
echo.

set "FOUND_SDK="

:: Check ANDROID_HOME first
if defined ANDROID_HOME (
    if exist "%ANDROID_HOME%" (
        echo [✓] Found via ANDROID_HOME: %ANDROID_HOME%
        set "FOUND_SDK=%ANDROID_HOME%"
        goto :found
    )
)

:: Common Android SDK locations on Windows
set "SDK_LOCATIONS=%LOCALAPPDATA%\Android\Sdk;%USERPROFILE%\AppData\Local\Android\Sdk;C:\Android\Sdk;C:\android-sdk;%ProgramFiles%\Android\android-sdk;%ProgramFiles(x86)%\Android\android-sdk"

for %%p in (%SDK_LOCATIONS%) do (
    if exist "%%p" (
        echo [✓] Found at: %%p
        set "FOUND_SDK=%%p"
        goto :found
    )
)

echo [X] Android SDK not found!
echo.
echo Please install Android Studio from:
echo https://developer.android.com/studio
echo.
echo Or set ANDROID_HOME to your SDK location:
echo set ANDROID_HOME=C:\path\to\android-sdk
exit /b 1

:found
:: Create local.properties with forward slashes
echo.
echo Creating local.properties with SDK location...
set "SDK_PATH=%FOUND_SDK:\=/%"
echo sdk.dir=%SDK_PATH%> local.properties
echo [✓] Created local.properties

:: Also suggest setting ANDROID_HOME
echo.
echo To make this permanent, add to your System Environment Variables:
echo ANDROID_HOME=%FOUND_SDK%
echo PATH=%%ANDROID_HOME%%\platform-tools;%%ANDROID_HOME%%\tools;%%PATH%%