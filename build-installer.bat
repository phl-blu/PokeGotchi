@echo off
setlocal

echo ========================================
echo  PokeGotchi - Build Portable App
echo ========================================
echo.

REM Step 1: Build the project and copy dependencies
echo [1/2] Building project and collecting dependencies...
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo Done.
echo.

REM Step 2: Run jpackage to create a portable app-image (no WiX needed)
echo [2/2] Running jpackage to create portable app...

set INPUT_DIR=target\jpackage-input
set OUTPUT_DIR=target\PokeGotchi
set APP_NAME=PokeGotchi
set MAIN_JAR=commit-tracker.jar
set MAIN_CLASS=com.tamagotchi.committracker.TamagotchiCommitTrackerApp

REM Clean previous output
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"

REM Remove duplicate okio jar (okio-jvm is the correct one for JVM)
if exist "%INPUT_DIR%\okio-3.6.0.jar" del "%INPUT_DIR%\okio-3.6.0.jar"

REM Find JavaFX jars in the input dir for module path
set JAVAFX_MODS=%INPUT_DIR%

jpackage ^
  --type app-image ^
  --input "%INPUT_DIR%" ^
  --dest "target" ^
  --name "%APP_NAME%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --app-version "1.0.0" ^
  --java-options "--module-path app" ^
  --java-options "--add-modules=javafx.controls,javafx.fxml,javafx.swing,javafx.graphics,javafx.base" ^
  --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo.
    echo ERROR: jpackage failed!
    echo Make sure you are using JDK 17+ (not just JRE^): javac -version
    pause
    exit /b 1
)

echo.
echo Done! Portable app created at: target\%APP_NAME%\
echo.
echo To distribute: zip the target\%APP_NAME%\ folder and share it.
echo Users just unzip and double-click %APP_NAME%.exe - no Java needed.
echo.

REM Optional: create a zip automatically
echo Creating zip archive...
powershell -Command "Compress-Archive -Path 'target\%APP_NAME%' -DestinationPath 'target\%APP_NAME%-1.0.0-windows.zip' -Force"
echo Zip created: target\%APP_NAME%-1.0.0-windows.zip
echo.
pause
