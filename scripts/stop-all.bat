@echo off
setlocal enabledelayedexpansion

:: =============================================
:: OAISS CHAIN - Windows unified stop script
:: scripts\stop-all.bat [--with-fabric]
::   --with-fabric    force stop Fabric network
:: =============================================
:: OAISS CHAIN - Windows 统一停止脚本
:: 用法: scripts\stop-all.bat [--with-fabric]
::   --with-fabric    同时停止 Fabric 网络
:: =============================================

set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

set "FORCE_FABRIC=false"

:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="--with-fabric" (
    set "FORCE_FABRIC=true"
    shift
    goto :parse_args
)
echo [ERROR] Unknown argument: %~1
exit /b 1

:args_done

echo.
echo ========================================
echo  OAISS CHAIN Stop
echo ========================================
echo.

:: 1. Stop backend (Java process on :8080)
echo [STOP] Stopping backend process...
call "%PROJECT_ROOT%\scripts\stop-backend.bat" >nul 2>&1

:: 2. Stop frontend (node on :5173)
echo [STOP] Stopping frontend process...
powershell -NoProfile -Command "Get-Process node -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like '*vite*' } | Stop-Process -Force -ErrorAction SilentlyContinue" >nul 2>&1

:: 3. Stop infrastructure containers (MySQL, Redis, MinIO, ML)
echo [STOP] Stopping infrastructure containers...
docker compose -f docker-compose.infra.yml down

:: 4. Stop full-stack containers (if started via docker-compose.yml)
docker compose -f docker-compose.yml down 2>nul

:: 5. Auto-detect and stop Fabric containers
set "FABRIC_RUNNING=false"
docker ps --format '{{.Names}}' 2>nul | findstr "orderer.example.com" >nul 2>&1
if not errorlevel 1 set "FABRIC_RUNNING=true"
docker ps --format '{{.Names}}' 2>nul | findstr "peer0.org1.example.com" >nul 2>&1
if not errorlevel 1 set "FABRIC_RUNNING=true"

if "%FABRIC_RUNNING%"=="true" goto :stop_fabric
if "%FORCE_FABRIC%"=="true" goto :stop_fabric
goto :after_fabric

:stop_fabric
echo [STOP] Stopping Fabric network...
docker compose -f docker-compose.fabric.yml down
if errorlevel 1 (
    echo [WARN] Failed to stop Fabric network
)
goto :after_fabric_done

:after_fabric
echo [SKIP] Fabric containers not running

:after_fabric_done

echo.
echo [STOP] All services stopped.
echo.

endlocal