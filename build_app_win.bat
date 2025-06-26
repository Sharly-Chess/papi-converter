@echo off
setlocal enabledelayedexpansion

set ROOTDIR=%~dp0
set ROOTDIR=%ROOTDIR:~0,-1%
set BUILDDIR=%ROOTDIR%\build
set DISTDIR=%ROOTDIR%\dist
set JAVADIR=%ROOTDIR%\java

:: classpath for libraries
set CP=%ROOTDIR%\lib\jackcess-4.0.5.jar;%ROOTDIR%\lib\commons-lang3-3.12.0.jar;%ROOTDIR%\lib\commons-logging-1.2.jar;%ROOTDIR%\lib\jackson-core-2.15.2.jar;%ROOTDIR%\lib\jackson-databind-2.15.2.jar;%ROOTDIR%\lib\jackson-annotations-2.15.2.jar

echo Cleaning previous build...
rmdir /s /q %BUILDDIR%
rmdir /s /q %DISTDIR%
mkdir %BUILDDIR%\classes
mkdir %DISTDIR%\java

echo Compiling Java...
javac --release 21 -cp %CP% -d %BUILDDIR%\classes %JAVADIR%\*.java

echo Extracting dependencies...
cd /d %BUILDDIR%\classes
jar xf %ROOTDIR%\lib\jackcess-4.0.5.jar
jar xf %ROOTDIR%\lib\commons-lang3-3.12.0.jar
jar xf %ROOTDIR%\lib\commons-logging-1.2.jar
jar xf %ROOTDIR%\lib\jackson-core-2.15.2.jar
jar xf %ROOTDIR%\lib\jackson-databind-2.15.2.jar
jar xf %ROOTDIR%\lib\jackson-annotations-2.15.2.jar

echo Removing META-INF to avoid conflicts...
rmdir /s /q META-INF

echo Creating fat JAR...
jar cfe %DISTDIR%\java\papiconverter.jar org.sharlychess.papiconverter.PapiConverter .
cd /d %ROOTDIR%
