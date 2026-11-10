# ==============================================================================
# RosterX Safe Build Runner - Multi-Agent Mutex Lock & Daemon Protection
# Prevents concurrent build daemon collisions, file locks, and memory crashes
# ==============================================================================
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs = @("assembleDebug")
)

$ErrorActionPreference = "Continue"
if (Test-Path variable:PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$LockFile = Join-Path $ScriptDir "build.lock"
$TimeoutSeconds = 240
$WaitIntervalSeconds = 3
$Elapsed = 0

Write-Host "🛡️ [SafeBuild] Requesting build access for: ./gradlew $GradleArgs" -ForegroundColor Cyan

# 1. Acquire Build Lock with Stale Lock Detection
while (Test-Path $LockFile) {
    try {
        $LockContent = Get-Content $LockFile -Raw -ErrorAction SilentlyContinue
        $LockData = $LockContent | ConvertFrom-Json -ErrorAction SilentlyContinue
        $LockPid = $LockData.pid
        
        $IsRunning = $false
        if ($LockPid) {
            $Proc = Get-Process -Id $LockPid -ErrorAction SilentlyContinue
            if ($Proc) { $IsRunning = $true }
        }
        
        if (-not $IsRunning) {
            Write-Host "⚠️ [SafeBuild] Detected stale lock from dead PID $LockPid. Cleaning up..." -ForegroundColor Yellow
            Remove-Item $LockFile -Force -ErrorAction SilentlyContinue
            break
        }
        
        if ($Elapsed -ge $TimeoutSeconds) {
            Write-Error "❌ [SafeBuild] Timed out waiting for previous build (PID: $LockPid, Agent: $($LockData.agent)) to complete after $TimeoutSeconds seconds."
            exit 1
        }
        
        Write-Host "⏳ [SafeBuild] Another agent (PID: $LockPid, Task: $($LockData.task)) is building. Waiting... (${Elapsed}s/${TimeoutSeconds}s)" -ForegroundColor Yellow
        Start-Sleep -Seconds $WaitIntervalSeconds
        $Elapsed += $WaitIntervalSeconds
    } catch {
        # Lock file might be in the middle of being deleted
        Start-Sleep -Seconds 1
        $Elapsed += 1
    }
}

# 2. Register Lock
$LockInfo = @{
    pid = $PID
    agent = if ($env:ANTIGRAVITY_AGENT) { $env:ANTIGRAVITY_AGENT } else { "AntigravityAgent" }
    task = ($GradleArgs -join " ")
    timestamp = (Get-Date).ToString("o")
} | ConvertTo-Json -Compress

Set-Content -Path $LockFile -Value $LockInfo -Force
Write-Host "🔒 [SafeBuild] Acquired build lock (PID: $PID). Commencing build..." -ForegroundColor Green

# 3. Execute Build with Mutex Protection
$ExitCode = 0
try {
    # Check if free memory is critically low (< 600MB), run garbage cleanup
    $FreeMemMB = [math]::round((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1KB, 0)
    if ($FreeMemMB -lt 600) {
        Write-Host "⚠️ [SafeBuild] Low system RAM (${FreeMemMB}MB free). Performing pre-build daemon trim..." -ForegroundColor Yellow
        [System.GC]::Collect()
    }

    $GradleCmd = Join-Path $ScriptDir "gradlew.bat"
    & $GradleCmd $GradleArgs
    $ExitCode = $LASTEXITCODE
} finally {
    # 4. Always Release Lock
    if (Test-Path $LockFile) {
        Remove-Item $LockFile -Force -ErrorAction SilentlyContinue
        Write-Host "🔓 [SafeBuild] Released build lock." -ForegroundColor Green
    }
}

if ($ExitCode -eq 0) {
    Write-Host "✅ [SafeBuild] Build succeeded!" -ForegroundColor Green
} else {
    Write-Host "❌ [SafeBuild] Build failed with exit code $ExitCode." -ForegroundColor Red
}

exit $ExitCode
