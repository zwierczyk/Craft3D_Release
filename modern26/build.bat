@echo off
rem Craft3D Modern - build (Windows). Wymaga: java/javac w PATH (IntelliJ ma).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out\classes
dir /s /b src\*.java > out\sources.txt
javac -encoding UTF-8 -cp "lib\*" -d out\classes @out\sources.txt
if errorlevel 1 exit /b 1
echo [build] DONE - out\classes
