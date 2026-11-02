# RosterX - Portable JDK 17 Non-Admin Setup Script
# Requires ZERO Administrator privileges. Installs into your user profile.

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  RosterX: Installing Portable OpenJDK 17 (No Admin Needed) " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

$destDir = "$HOME\.jdks"
if (!(Test-Path $destDir)) {
    New-Item -ItemType Directory -Path $destDir -Force | Out-Null
}

$zipUrl = "https://aka.ms/download-jdk/microsoft-jdk-17.0.13-windows-x64.zip"
$zipFile = "$destDir\jdk17.zip"

Write-Host "`n1. Downloading Microsoft OpenJDK 17 portable zip (~180MB)..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $zipUrl -OutFile $zipFile

Write-Host "`n2. Extracting to $destDir..." -ForegroundColor Yellow
Expand-Archive -Path $zipFile -DestinationPath $destDir -Force
Remove-Item $zipFile -Force

# Standardize directory name to $destDir\jdk-17
$extracted = Get-ChildItem -Path $destDir -Directory | Where-Object { $_.Name -like "*jdk*" -and $_.Name -ne "jdk-17" } | Select-Object -First 1
if ($extracted) {
    if (Test-Path "$destDir\jdk-17") {
        Remove-Item "$destDir\jdk-17" -Recurse -Force
    }
    Rename-Item -Path $extracted.FullName -NewName "jdk-17"
}

$finalJdkDir = "$destDir\jdk-17"

if (Test-Path "$finalJdkDir\bin\java.exe") {
    Write-Host "`n3. Configuring User-level JAVA_HOME & PATH..." -ForegroundColor Yellow
    # Set User-level environment variables (Requires ZERO admin access)
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $finalJdkDir, "User")

    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if ($userPath -notlike "*$finalJdkDir\bin*") {
        [Environment]::SetEnvironmentVariable("Path", "$finalJdkDir\bin;$userPath", "User")
    }

    Write-Host "`n============================================================" -ForegroundColor Green
    Write-Host "  ✅ SUCCESS: OpenJDK 17 is installed and configured!" -ForegroundColor Green
    Write-Host "  Location: $finalJdkDir" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "`n👉 Please RELOAD or RESTART your IDE (Ctrl+Shift+P -> Developer: Reload Window)" -ForegroundColor Cyan
} else {
    Write-Host "`n[ERROR] Extraction check failed. Please check your internet connection." -ForegroundColor Red
}

