@echo off
REM ============================================================
REM  Craft3D - Windows launcher
REM  Wymagania: Java 11 lub nowsza
REM ============================================================

setlocal
cd /d "%~dp0"

REM Sprawdz Jave
java -version >nul 2>&1
if errorlevel 1 (
    echo.
    echo  [BLAD] Java nie jest zainstalowana lub nie ma jej w PATH.
    echo  Pobierz Jave 11+ ze strony: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

REM Uruchom gre z 2GB heap dla wiekszego swiata + lightmap
java -Xmx2G -Xms512M -cp "Craft3D.jar;lib\*" craft3dgl.MinecraftGL

if errorlevel 1 (
    echo.
    echo  [BLAD] Gra zakonczyla sie z bledem. Sprawdz logs\crash.log
    pause
)

endlocal
