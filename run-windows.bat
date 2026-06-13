@echo off
setlocal
title Commissioning-GPT
cd /d "%~dp0backend"

REM --- Run first-time setup automatically if the venv is missing ---
if not exist ".venv\Scripts\activate.bat" (
  echo No environment found - running first-time setup ...
  call "%~dp0setup-windows.bat"
  if not exist ".venv\Scripts\activate.bat" (
    echo [ERROR] Setup did not complete. Aborting.
    pause
    exit /b 1
  )
)

call ".venv\Scripts\activate.bat"

echo.
echo ==================================================
echo   Commissioning-GPT is starting...
echo   Open your browser at:  http://127.0.0.1:8000
echo   ^(Press Ctrl+C in this window to stop^)
echo ==================================================
echo.

REM Open the default browser, then start the server in the foreground.
start "" http://127.0.0.1:8000
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000

endlocal
