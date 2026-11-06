@echo off
title RosterX Cloudflare Public Tunnel
color 0B

echo ================================================================
echo   CLOUDFLARE PUBLIC TUNNEL FOR ROSTERX
echo ================================================================
echo.
echo Tunneling local server (http://localhost:8000) to Cloudflare's
echo global edge network with a secure public HTTPS URL...
echo.

where cloudflared >nul 2>&1
if %ERRORLEVEL% equ 0 (
    cloudflared tunnel --url http://localhost:8000
    goto end
)

if exist "C:\Program Files (x86)\cloudflared\cloudflared.exe" (
    "C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://localhost:8000
    goto end
)

if exist "C:\Program Files\cloudflared\cloudflared.exe" (
    "C:\Program Files\cloudflared\cloudflared.exe" tunnel --url http://localhost:8000
    goto end
)

echo [ERROR] cloudflared.exe was not found on your system.
echo Please run: winget install --id Cloudflare.cloudflared
pause

:end
