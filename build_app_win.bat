@echo off
setlocal enabledelayedexpansion

rem Paths
set ROOTDIR=%~dp0
set ROOTDIR=%ROOTDIR:~0,-1%
set BUILDDIR=%ROOTDIR%\build
set DISTDIR=%ROOTDIR%\dist
set JAVADIR=%ROOTDIR%\java

rem Configure GraalVM
echo Debug: GRAALVM_HOME = [%GRAALVM_HOME%]
echo Debug: JAVA_HOME = [%JAVA_HOME%]
echo.
set GRAAL_CONFIGURED=0
if defined GRAALVM_HOME (
    set "JAVA_HOME=%GRAALVM_HOME%"
    set "PATH=%GRAALVM_HOME%\bin;%PATH%"
    echo Using GRAALVM_HOME: %GRAALVM_HOME%
    set GRAAL_CONFIGURED=1
)

if %GRAAL_CONFIGURED%==0 (
    if defined JAVA_HOME (
        echo Using JAVA_HOME assuming it is GraalVM: %JAVA_HOME%
        set "PATH=%JAVA_HOME%\bin;%PATH%"
        set GRAAL_CONFIGURED=1
    )
)

if %GRAAL_CONFIGURED%==0 (
    echo ERROR: Neither GRAALVM_HOME nor JAVA_HOME environment variables are set
    echo Please set GRAALVM_HOME to your GraalVM installation directory
    echo Example: set GRAALVM_HOME=C:\Program Files\GraalVM\graalvm-jdk-22
    exit /b 1
)

echo.
echo Checking Java version:
java -version
echo.

rem Clean previous build artifacts
echo Cleaning previous build...
if exist "%BUILDDIR%" rmdir /s /q "%BUILDDIR%" 2>nul
if exist "%DISTDIR%" rmdir /s /q "%DISTDIR%" 2>nul
mkdir "%BUILDDIR%" 2>nul
mkdir "%DISTDIR%" 2>nul

rem Build classpath from lib directory
echo Building classpath...
set LIB_CLASSPATH=
for %%f in (lib\*.jar) do (
    if "!LIB_CLASSPATH!"=="" (
        set LIB_CLASSPATH=%%f
    ) else (
        set LIB_CLASSPATH=!LIB_CLASSPATH!;%%f
    )
)

rem Compile Java source code
echo Compiling Java...
javac -cp "%LIB_CLASSPATH%" -d "%BUILDDIR%" java\*.java
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java compilation failed
    exit /b 1
)

rem Create a new JAR file with compiled classes
echo Creating JAR...
cd /d "%BUILDDIR%"
jar cfe "%ROOTDIR%\papiconverter.jar" org.sharlychess.papiconverter.PapiConverter .
if %ERRORLEVEL% neq 0 (
    echo ERROR: JAR creation failed
    exit /b 1
)

rem Build native image
echo Building native binary...
cd /d "%ROOTDIR%"
native-image --no-fallback ^
  -H:+UnlockExperimentalVMOptions ^
  -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json ^
  -H:ResourceConfigurationFiles=src/main/resources/META-INF/native-image/resource-config.json ^
  -H:+JNI ^
  --enable-url-protocols=http,https ^
  -cp "%ROOTDIR%\papiconverter.jar;%LIB_CLASSPATH%" org.sharlychess.papiconverter.PapiConverter "%DISTDIR%\papi-converter"

if %ERRORLEVEL% neq 0 (
    echo ERROR: Native image build failed
    exit /b 1
)

rem Clean up intermediate files
echo Cleaning up intermediate files...
if exist "%ROOTDIR%\papiconverter.jar" del "%ROOTDIR%\papiconverter.jar"

rem Copy static resources to distribution
echo Copying static resources...
if exist "%ROOTDIR%\static" xcopy /E /I "%ROOTDIR%\static" "%DISTDIR%\static" >nul

echo.
echo Build completed successfully! 
echo Native binary is located at: %DISTDIR%\papi-converter.exe
echo.
echo Usage examples:
echo   %DISTDIR%\papi-converter.exe input.json output.papi
echo   %DISTDIR%\papi-converter.exe input.papi output.json
echo   %DISTDIR%\papi-converter.exe --playerdb Data.mdb players.sql
