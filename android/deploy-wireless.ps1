# RosterX Wireless Deploy & Launch Script
# Compiles Android sources, installs APK over Wi-Fi, and launches the app.

param(
    [string]$DeviceIp = ""
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "🚀 RosterX Wireless Android Build & Deploy Pipeline" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Check ADB connection
if ($DeviceIp -ne "") {
    Write-Host "📡 Connecting to wireless target: $DeviceIp..." -ForegroundColor Yellow
    adb connect $DeviceIp
}

$devices = adb devices | Where-Object { $_ -match "\tdevice$" }
if (-not $devices) {
    Write-Host "❌ No connected ADB device found over Wi-Fi or USB!" -ForegroundColor Red
    Write-Host "💡 To connect:" -ForegroundColor Yellow
    Write-Host "   1. Enable 'Wireless debugging' in Developer Options." -ForegroundColor Yellow
    Write-Host "   2. Run: adb connect <phone_ip>:<port>" -ForegroundColor Yellow
    Write-Host "   Or pass the IP: .\deploy-wireless.ps1 -DeviceIp <phone_ip>:<port>" -ForegroundColor Yellow
    exit 1
}

Write-Host "📱 Target Device Connected:" -ForegroundColor Green
$devices | ForEach-Object { Write-Host "   $_" -ForegroundColor Green }

# 2. Compile and Install APK via Gradle
Write-Host "`n🔨 Compiling sources and installing APK wirelessly via Gradle..." -ForegroundColor Cyan
.\gradlew.bat installDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Gradle build/install failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

# 3. Launch the app on device
Write-Host "`n🚀 Launching RosterX on device..." -ForegroundColor Green
adb shell am start -n com.rosterx/.MainActivity

Write-Host "`n✅ Deployed & launched successfully on your phone!" -ForegroundColor Green

