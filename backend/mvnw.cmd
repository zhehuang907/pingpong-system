@echo off
setlocal
set "BASE_DIR=%~dp0"
set "MVN_CMD=%BASE_DIR%tools\apache-maven-3.9.9\bin\mvn.cmd"

if exist "%MVN_CMD%" (
  call "%MVN_CMD%" %*
  exit /b %ERRORLEVEL%
)

where mvn >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  call mvn %*
  exit /b %ERRORLEVEL%
)

echo Maven not found. Expected bundled Maven at %MVN_CMD% or a global mvn command.
exit /b 1
