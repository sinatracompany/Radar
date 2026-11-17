@echo off
setlocal
cd /d "%~dp0"
echo SignalScope v0.1.0 - assemble an APK without testing
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Build-Windows.ps1"
if errorlevel 1 (
  echo.
  echo APK compilation failed. Review the error messages above.
  pause
  exit /b 1
)
echo.
echo APK saved at: %~dp0release\SignalScope-v0.1.0-debug.apk
pause
