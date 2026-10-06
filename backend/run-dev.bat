@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

if exist ".env" (
    echo [EVOQ EMS] Loading environment variables from backend\.env...
    for /f "usebackq tokens=1,* delims==" %%A in (".env") do (
        set "KEY=%%A"
        set "VAL=%%B"
        if not "!KEY!"=="" (
            if not "!KEY:~0,1!"=="#" (
                set "VAL=!VAL:"=!"
                set "VAL=!VAL:'=!"
                set "!KEY!=!VAL!"
            )
        )
    )
) else (
    echo [EVOQ EMS] No backend\.env file found. Using system defaults.
)

echo [EVOQ EMS] Starting Spring Boot backend...
call mvnw.cmd spring-boot:run
