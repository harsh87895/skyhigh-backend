@echo off
REM SkyHigh Core - Quick Start Script for Windows
REM This script starts the entire application using Docker Compose

echo ================================
echo SkyHigh Core - Digital Check-In
echo ================================
echo.

echo [1/3] Checking Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not installed or not running!
    echo Please install Docker Desktop from https://www.docker.com/
    pause
    exit /b 1
)
echo ✓ Docker is installed

echo.
echo [2/3] Starting services (PostgreSQL, Redis, Application)...
docker-compose up -d

if errorlevel 1 (
    echo ERROR: Failed to start services!
    pause
    exit /b 1
)

echo.
echo [3/3] Waiting for application to start...
timeout /t 15 /nobreak >nul

echo.
echo ================================
echo ✓ Application Started Successfully!
echo ================================
echo.
echo Access Points:
echo   - API Base: http://localhost:8080/api/v1
echo   - Swagger UI: http://localhost:8080/swagger-ui.html
echo   - Health Check: http://localhost:8080/actuator/health
echo.
echo Quick Test:
echo   curl http://localhost:8080/api/v1/flights/SK101/seats
echo.
echo View Logs:
echo   docker-compose logs -f app
echo.
echo Stop Application:
echo   docker-compose down
echo.
echo For more information, see QUICK_START.md
echo ================================
echo.

REM Open Swagger UI in default browser
timeout /t 2 /nobreak >nul
start http://localhost:8080/swagger-ui.html

pause

