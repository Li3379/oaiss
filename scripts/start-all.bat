@echo off
setlocal enabledelayedexpansion

:: =============================================
:: OAISS CHAIN - Windows start-all script
:: Usage: scripts\start-all.bat [options]
::   --with-fabric    start Fabric network too
::   --skip-frontend  skip frontend dev server
::   --skip-backend   skip backend dev server
::   --infra-only     start infra only
:: =============================================

for %%I in ("%~dp0..") do set "PROJECT_ROOT=%%~fI"
cd /d "%PROJECT_ROOT%"
set "SCRIPT_DIR=%~dp0"

set "WITH_FABRIC=false"
set "SKIP_FRONTEND=false"
set "SKIP_BACKEND=false"
set "INFRA_ONLY=false"

:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="--with-fabric" (
    set "WITH_FABRIC=true"
    shift
    goto :parse_args
)
if "%~1"=="--skip-frontend" (
    set "SKIP_FRONTEND=true"
    shift
    goto :parse_args
)
if "%~1"=="--skip-backend" (
    set "SKIP_BACKEND=true"
    shift
    goto :parse_args
)
if "%~1"=="--infra-only" (
    set "INFRA_ONLY=true"
    shift
    goto :parse_args
)
echo [ERROR] Unknown argument: %~1
exit /b 1

:args_done

if exist ".env" (
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        set "line=%%a"
        if not "!line!"=="" if not "!line:~0,1!"=="#" (
            set "%%a=%%b"
        )
    )
)

for /f "delims=" %%I in ('wsl.exe wslpath -a "%PROJECT_ROOT%\scripts\bootstrap-fabric.sh" 2^>nul') do set "BOOTSTRAP_FABRIC_WSL=%%I"

echo.
echo ========================================
echo  OAISS CHAIN Start
echo ========================================
echo.

echo [START] Starting infra services...
docker compose -f docker-compose.infra.yml up -d
if errorlevel 1 (
    echo [ERROR] Failed to start infra services
    exit /b 1
)

echo [START] Waiting for MySQL...
:wait_mysql
docker inspect --format="{{.State.Health.Status}}" oaiss-mysql 2>nul | findstr "healthy" >nul
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto :wait_mysql
)
echo [START] MySQL is ready

echo [START] Waiting for Redis...
:wait_redis
docker inspect --format="{{.State.Health.Status}}" oaiss-redis 2>nul | findstr "healthy" >nul
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto :wait_redis
)
echo [START] Redis is ready

echo [START] Waiting for MinIO...
:wait_minio
docker inspect --format="{{.State.Health.Status}}" oaiss-minio 2>nul | findstr "healthy" >nul
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    goto :wait_minio
)
echo [START] MinIO is ready

if "%INFRA_ONLY%"=="true" (
    echo [START] Infra-only mode enabled
    goto :done
)

if "%WITH_FABRIC%"=="true" (
    echo [START] Starting Fabric network...
    docker compose -f docker-compose.fabric.yml up -d
    if errorlevel 1 (
        echo [ERROR] Failed to start Fabric network
        exit /b 1
    )
    echo [START] Waiting for Fabric containers...
    timeout /t 10 /nobreak >nul
    echo [START] Bootstrapping Fabric channel and chaincode...
    if not defined BOOTSTRAP_FABRIC_WSL (
        echo [ERROR] Failed to resolve WSL path for bootstrap-fabric.sh
        exit /b 1
    )
    wsl.exe bash "%BOOTSTRAP_FABRIC_WSL%"
    if errorlevel 1 (
        echo [ERROR] Fabric bootstrap failed
        exit /b 1
    )
) else (
    echo [WARN] Fabric startup skipped
)

if "%SKIP_BACKEND%"=="true" (
    echo [WARN] Backend startup skipped
    goto :skip_backend
)

set "BACKEND_LAUNCH_ARGS="
if "%WITH_FABRIC%"=="true" set "BACKEND_LAUNCH_ARGS=-WithFabric"

echo [START] Starting backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\scripts\launch-backend.ps1" %BACKEND_LAUNCH_ARGS%
if errorlevel 1 (
    echo [ERROR] Failed to launch backend helper
    exit /b 1
)
echo [START] Backend launch requested, waiting for health...

set /a BACKEND_WAIT_COUNT=0
:wait_backend
timeout /t 5 /nobreak >nul
curl -sf http://localhost:8080/api/v1/actuator/health >nul 2>&1
if not errorlevel 1 goto :backend_ready
set /a BACKEND_WAIT_COUNT+=1
if !BACKEND_WAIT_COUNT! GEQ 24 (
    echo [ERROR] Backend did not become healthy within 120 seconds
    exit /b 1
)
goto :wait_backend

:backend_ready
echo [START] Backend is ready

:skip_backend

if "%SKIP_FRONTEND%"=="true" (
    echo [WARN] Frontend startup skipped
    goto :skip_frontend
)

echo [START] Starting frontend...
cd /d "%PROJECT_ROOT%\oaiss-chain-frontend"
start "OAISS Frontend" cmd /c "npm run dev"
cd /d "%PROJECT_ROOT%"
echo [START] Frontend window started

:skip_frontend

:done
echo.
echo ========================================
echo  OAISS CHAIN services started
echo    Backend:  http://localhost:8080/api/v1
echo    Frontend: http://localhost:5173
echo    ML:       http://localhost:8001
echo    MinIO:    http://localhost:9003
if "%WITH_FABRIC%"=="true" (
    echo    Orderer:  http://localhost:7050
    echo    Peer:     http://localhost:7051
)
echo ========================================
echo.
echo Use scripts\stop-all.bat to stop all services.
echo.

endlocal
