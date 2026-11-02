@echo off
title RosterX - Portable JDK 17 Non-Admin Setup
echo ============================================================
echo   Installing Portable OpenJDK 17 (Zero Admin Privileges Needed)
echo ============================================================
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup_jdk17.ps1"
echo.
pause

