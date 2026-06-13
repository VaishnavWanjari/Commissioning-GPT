@echo off
setlocal
title Commissioning-GPT - Setup
echo ==================================================
echo   Commissioning-GPT  -  First-time setup (no admin)
echo ==================================================
echo.

cd /d "%~dp0backend"

REM --- Locate a per-user Python (the 'py' launcher is preferred) ---
set "PY="
where py >nul 2>nul && set "PY=py -3"
if not defined PY (
  where python >nul 2>nul && set "PY=python"
)
if not defined PY (
  echo [ERROR] Python was not found on your PATH.
  echo.
  echo   Install Python 3.11+ WITHOUT admin rights:
  echo     1. Download from https://www.python.org/downloads/windows/
  echo     2. Run the installer and UNCHECK "Install for all users"
  echo        ^(this is a per-user install and needs NO administrator^)
  echo     3. CHECK "Add python.exe to PATH", then install.
  echo.
  pause
  exit /b 1
)
echo Using Python: %PY%

REM --- Create a local virtual environment (entirely in your user space) ---
if not exist ".venv" (
  echo Creating virtual environment ...
  %PY% -m venv .venv || (echo [ERROR] Could not create venv & pause & exit /b 1)
)

call ".venv\Scripts\activate.bat"

echo Installing dependencies ^(first run may take a few minutes^) ...
python -m pip install --upgrade pip
pip install -r requirements.txt || (
  echo [ERROR] Dependency installation failed. Check your internet connection.
  pause
  exit /b 1
)

REM --- Create the .env config if missing ---
if not exist ".env" (
  copy ".env.example" ".env" >nul
  echo.
  echo [ACTION REQUIRED] Open  backend\.env  and set your key:
  echo                   OPENAI_API_KEY=sk-...
)

echo.
echo Setup complete. You can now run  run-windows.bat
echo.
pause
endlocal
