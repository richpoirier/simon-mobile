@echo off
echo Checking Java configuration...
echo.

echo Java version:
java -version 2>&1
echo.

echo JAVA_HOME: %JAVA_HOME%
echo.

:: Check Java version number
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "JAVA_VERSION=%%j"
echo Detected Java major version: %JAVA_VERSION%
echo.

if %JAVA_VERSION% LSS 11 (
    echo [X] Java %JAVA_VERSION% is too old. Java 11 or higher is required.
    echo.
    echo Please install Java 11+ from:
    echo https://adoptium.net/
    echo.
    echo After installation, you may need to:
    echo 1. Set JAVA_HOME environment variable
    echo 2. Add %%JAVA_HOME%%\bin to your PATH
) else (
    echo [✓] Java %JAVA_VERSION% is compatible
)
echo.

:: Show all Java installations if we can find them
echo Looking for Java installations...
if exist "%ProgramFiles%\Java" (
    echo Found in Program Files:
    dir /b "%ProgramFiles%\Java"
)
if exist "%ProgramFiles(x86)%\Java" (
    echo Found in Program Files (x86):
    dir /b "%ProgramFiles(x86)%\Java"
)
if exist "%ProgramFiles%\Eclipse Adoptium" (
    echo Found Eclipse Adoptium:
    dir /b "%ProgramFiles%\Eclipse Adoptium"
)
echo.