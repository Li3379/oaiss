@echo off
setlocal enabledelayedexpansion

:: =============================================
:: OAISS CHAIN - Windows backend start script
:: Usage: scripts\start-backend.bat [options]
::   --with-fabric    use local,fabric profiles
:: =============================================

set "PROJECT_ROOT=%~dp0.."
cd /d "%PROJECT_ROOT%"

set "WITH_FABRIC=false"

:parse_args
if "%~1"=="" goto :args_done
if "%~1"=="--with-fabric" ( set "WITH_FABRIC=true" & shift & goto :parse_args )
echo [ERROR] 未知参数: %~1
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

if "%WITH_FABRIC%"=="true" (
    set "SPRING_PROFILES_ACTIVE=local,fabric"
    set "SPRING_ARGS=-Dspring-boot.run.profiles=local,fabric"
) else (
    set "SPRING_PROFILES_ACTIVE=local"
    set "SPRING_ARGS=-Dspring-boot.run.profiles=local"
)

echo.
echo ========================================
echo  OAISS CHAIN Backend
echo  Profiles: %SPRING_PROFILES_ACTIVE%
echo ========================================
echo.

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    echo [WARN] Port 8080 is already in use by PID %%p
    echo [WARN] Run scripts\stop-backend.bat first, then retry.
    endlocal
    exit /b 1
)

cd /d "%PROJECT_ROOT%\oaiss-chain-backend"
call mvn spring-boot:run %SPRING_ARGS%

endlocal
