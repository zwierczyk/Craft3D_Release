@echo off
rem Uruchomienie Craft3D Modern (Windows).
cd /d "%~dp0"
if not exist out\classes call build.bat
java -cp "out\classes;lib\*" craft3dmodern.test.AssetsExtractor
java -cp "out\classes;lib\*" craft3dmodern.Main %*
