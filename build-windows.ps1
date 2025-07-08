# PowerShell script to build Android app
Write-Host "Android Voice Assistant Build Script (PowerShell)" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Get the directory where this script is located
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Change to the project directory
Set-Location $ScriptDir

# Check for Java
try {
    $javaVersion = & java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "[OK] Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "[X] Java is not installed or not in PATH" -ForegroundColor Red
    Write-Host "    Please install Java 11+ from: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# Check for local.properties
if (-not (Test-Path "local.properties")) {
    Write-Host "Creating local.properties..." -ForegroundColor Yellow
    
    # Find Android SDK
    $androidHome = $env:ANDROID_HOME
    if (-not $androidHome) {
        $possiblePaths = @(
            "$env:LOCALAPPDATA\Android\Sdk",
            "$env:USERPROFILE\AppData\Local\Android\Sdk",
            "C:\Android\Sdk"
        )
        
        foreach ($path in $possiblePaths) {
            if (Test-Path $path) {
                $androidHome = $path
                break
            }
        }
    }
    
    if ($androidHome) {
        $sdkPath = $androidHome -replace '\\', '/'
        "sdk.dir=$sdkPath" | Out-File -FilePath "local.properties" -Encoding ASCII
        Write-Host "[OK] Created local.properties with SDK: $androidHome" -ForegroundColor Green
    } else {
        Write-Host "[X] Android SDK not found. Please install Android Studio" -ForegroundColor Red
        exit 1
    }
}

# Check for config.properties
if (-not (Test-Path "config.properties")) {
    Copy-Item "config.properties.example" "config.properties"
    Write-Host "[!] Created config.properties - do not forget to add your OpenAI API key!" -ForegroundColor Yellow
}

# Run the build
Write-Host ""
Write-Host "Building app..." -ForegroundColor Cyan
& .\gradlew.bat assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[OK] Build successful!" -ForegroundColor Green
    Write-Host "     APK location: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[X] Build failed. See errors above." -ForegroundColor Red
}