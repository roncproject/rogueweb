@echo off
:: ─────────────────────────────────────────────────────────────────────────────
::  Rogue AWS – Local Development Script (Windows)
::  Builds and runs the game server locally.
::  Open http://localhost:8080 in your browser after starting.
:: ─────────────────────────────────────────────────────────────────────────────

setlocal EnableDelayedExpansion

:: ── Check for Java ───────────────────────────────────────────────────────────
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Install Java 17+ from https://adoptium.net
    pause & exit /b 1
)

:: ── Check for Maven ──────────────────────────────────────────────────────────
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found.
    echo Download from https://maven.apache.org/download.cgi
    echo Then add Maven's bin\ folder to your PATH.
    pause & exit /b 1
)

:: ── Build ────────────────────────────────────────────────────────────────────
echo.
echo [Rogue] Building...
mvn package -DskipTests -q
if errorlevel 1 (
    echo [ERROR] Build failed. Run  mvn package  to see full error output.
    pause & exit /b 1
)

echo [Rogue] Build OK.  Starting server on http://localhost:8080
echo [Rogue] Press Ctrl+C to stop.
echo.

:: ── Run ──────────────────────────────────────────────────────────────────────
:: Open the browser automatically after a short delay
start "" /b cmd /c "timeout /t 3 >nul && start http://localhost:8080"

java -jar target\rogue-aws.jar
