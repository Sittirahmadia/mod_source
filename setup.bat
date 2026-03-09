@echo off
echo === HyperBoost V5 - Gradle Setup ===

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java 21+ is required. Please install JDK 21.
    pause
    exit /b 1
)

where gradle >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo Gradle found. Generating wrapper...
    gradle wrapper --gradle-version=8.8
    echo Done! Now run: gradlew.bat build
) else (
    echo Gradle not found.
    echo Please install Gradle 8.8 from https://gradle.org/releases/
    echo Then run: gradle wrapper
    echo Then run: gradlew.bat build
)
pause
