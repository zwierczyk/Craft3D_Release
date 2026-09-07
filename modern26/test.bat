@echo off
rem Testy headless (Windows).
cd /d "%~dp0"
if not exist out\classes call build.bat
java -cp "out\classes;lib\*" craft3dmodern.test.AssetsExtractor
if not exist out\testclasses mkdir out\testclasses
javac -encoding UTF-8 -cp "out\classes" -d out\testclasses src\craft3dmodern\test\AssetsTest.java
java -cp "out\classes;out\testclasses;lib\*" craft3dmodern.test.AssetsTest
if errorlevel 1 exit /b 1
