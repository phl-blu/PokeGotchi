@echo off
setlocal
set GITHUB_CLIENT_ID=Ov23li1fNemWB2pz0nfX
echo ========================================
echo GitHub Integration Test
echo ========================================
echo.
echo Compiling project...
call mvn compile test-compile -q -DskipTests
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo.
echo Running GitHub Integration Test...
echo.
call mvn exec:java -Dexec.mainClass=com.tamagotchi.committracker.github.GitHubIntegrationManualTest -Dexec.classpathScope=test -q
echo.
pause
