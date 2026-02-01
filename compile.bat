@echo off
echo ╔════════════════════════════════════════════════════╗
echo ║   FINANCIAL ENGINE - Compilare                    ║
echo ╚════════════════════════════════════════════════════╝
echo.

cd src
echo Compilare în curs...

javac -d ../bin -encoding UTF-8 Main.java model/*.java server/*.java client/*.java util/*.java

if %errorlevel% neq 0 (
    echo.
    echo ❌ EROARE la compilare!
    pause
    exit /b %errorlevel%
)

echo.
echo ✓ Compilare reușită! Fișierele .class sunt în directorul 'bin'
echo.
echo Pentru a rula sistemul:
echo   cd bin
echo   java Main
echo.
echo sau rulează run.bat pentru compilare + rulare automată
pause
