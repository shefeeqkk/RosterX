# RosterX - Cloudflare Tunnel Launcher
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  CLOUDFLARE PUBLIC TUNNEL FOR ROSTERX" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Forwarding traffic from Cloudflare Edge to http://localhost:8000..." -ForegroundColor Yellow
Write-Host ""

$cfPath = (Get-Command cloudflared -ErrorAction SilentlyContinue)?.Source
if (-not $cfPath) {
    $candidates = @(
        "C:\Program Files (x86)\cloudflared\cloudflared.exe",
        "C:\Program Files\cloudflared\cloudflared.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { $cfPath = $c; break }
    }
}

if (-not $cfPath) {
    Write-Host "[ERROR] cloudflared is not installed or not in PATH." -ForegroundColor Red
    Write-Host "Install it using: winget install --id Cloudflare.cloudflared" -ForegroundColor White
    exit 1
}

& $cfPath tunnel --url http://localhost:8000
