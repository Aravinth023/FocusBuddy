@echo off
setlocal

echo Checking Java...
where java >nul 2>nul
if errorlevel 1 (
    echo.
    echo ERROR: Java was not found on your PATH.
    echo Please install Java 17 or newer from https://adoptium.net
    echo and make sure "java" and "javac" are available in a new
    echo Command Prompt window before running this script again.
    echo.
    pause
    exit /b 1
)

where javac >nul 2>nul
if errorlevel 1 (
    echo.
    echo ERROR: javac ^(the Java compiler^) was not found on your PATH.
    echo You may have only a Java Runtime ^(JRE^) installed.
    echo Please install a full Java Development Kit ^(JDK^) 17+ from
    echo https://adoptium.net and try again.
    echo.
    pause
    exit /b 1
)

if not exist out (
    mkdir out
)

if not exist data (
    mkdir data
)

echo Compiling FocusBuddy...
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
    echo.
    echo Compilation failed. See the errors above.
    echo.
    pause
    exit /b 1
)

echo Starting FocusBuddy...
java -cp out FocusBuddy

endlocal
