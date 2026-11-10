@echo off
REM ==============================================================================
REM RosterX Safe Build Runner Batch Entrypoint
REM ==============================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0safe_build.ps1" %*
exit /b %ERRORLEVEL%
