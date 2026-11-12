@echo off
rem Remix Studio launcher: double-click to set up (first run) and start the app.
setlocal
cd /d "%~dp0"
title Remix Studio
set "PORT=3000"

rem --- Find Python -----------------------------------------------------------
set "PY="
py -3 --version >nul 2>&1 && set "PY=py -3"
if not defined PY python --version >nul 2>&1 && set "PY=python"
if not defined PY goto :no_python

rem --- ffmpeg is needed to convert SoundCloud streams -------------------------
where ffmpeg >nul 2>&1
if errorlevel 1 goto :no_ffmpeg

rem --- First run: create the environment and install dependencies -------------
if exist ".venv\Scripts\python.exe" goto :have_venv
echo Creating Python environment...
%PY% -m venv .venv
if errorlevel 1 goto :failed
:have_venv

if exist ".venv\.installed" goto :start
echo Installing dependencies (first run only, this can take a few minutes)...
".venv\Scripts\python.exe" -m pip install --disable-pip-version-check -r requirements.txt
if errorlevel 1 goto :failed
echo ok> ".venv\.installed"

:start
echo.
echo Remix Studio is starting at http://localhost:%PORT%
echo Keep this window open while you use it. Close it to stop.
echo.
rem Open the browser a few seconds from now, once the server is listening.
start "" /b cmd /c "ping -n 4 127.0.0.1 >nul & start http://localhost:%PORT%"
".venv\Scripts\python.exe" -m uvicorn server:app --port %PORT%
if errorlevel 1 goto :failed
goto :eof

:no_python
echo Python was not found.
echo Install Python 3.10 or newer from https://www.python.org/downloads/
echo and tick "Add python.exe to PATH" in the installer, then run this again.
goto :stop

:no_ffmpeg
echo ffmpeg was not found.
echo Install it by running this in a terminal, then run this file again:
echo     winget install Gyan.FFmpeg
goto :stop

:failed
echo.
echo Something went wrong - see the messages above.
echo If port %PORT% is already in use, close the other Remix Studio window first.

:stop
echo.
pause
