@echo off
echo ╔════════════════════════════════════════════════════╗
echo ║   FINANCIAL ENGINE - Compilare si Rulare          ║
echo ╚════════════════════════════════════════════════════╝
echo.

cd src

echo [1/2] Compilare...
javac -d ../bin -encoding UTF-8 Main.java model/*.java server/*.java client/*.java util/*.java

if %errorlevel% neq 0 (
    echo.
    echo ❌ EROARE la compilare!
    pause
    exit /b %errorlevel%
)

echo ✓ Compilare reușită!
echo.
echo [2/2] Rulare sistem...
echo.

cd ../bin
java Main

echo.
echo Apasă orice tastă pentru a închide...
pause > nul
