@echo off
setlocal

:: =============================================
:: OAISS CHAIN - Windows 统一停止脚本
:: 用法: scripts\stop-all.bat [选项]
::   --with-fabric    同时停止 Fabric 网络
:: =============================================

set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

set WITH_FABRIC=false

:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="--with-fabric" ( set "WITH_FABRIC=true" & shift & goto :parse_args )
echo [ERROR] 未知参数: %~1
exit /b 1

:args_done

echo.
echo ========================================
echo  OAISS CHAIN 停止
echo ========================================
echo.

echo [STOP] 停止后端进程...
call "%PROJECT_ROOT%\scripts\stop-backend.bat" >nul 2>&1

:: 停止基础设施容器
echo [STOP] 停止基础设施容器...
docker compose -f docker-compose.infra.yml down

:: 停止全栈容器（如用 docker compose -f docker-compose.yml 启动）
echo [STOP] 停止全栈容器（如有）...
docker compose -f docker-compose.yml down 2>nul

:: 停止 Fabric
if "%WITH_FABRIC%"=="true" (
    echo [STOP] 停止 Fabric 网络...
    docker compose -f docker-compose.fabric.yml down
)

echo.
echo [STOP] 所有服务已停止
echo.

endlocal
