@echo off
setlocal enabledelayedexpansion

set ROOTDIR=%~dp0
set ROOTDIR=%ROOTDIR:~0,-1%
set BUILDDIR=%ROOTDIR%\build
set DISTDIR=%ROOTDIR%\dist
set JAVADIR=%ROOTDIR%\java

echo Cleaning previous build...
rmdir /s /q %BUILDDIR%
rmdir /s /q %DISTDIR%
mkdir %BUILDDIR%\classes
mkdir %DISTDIR%\java

echo Compiling Java...
javac --release 21 -d %BUILDDIR%\classes %JAVADIR%\PapiConverter.java

echo Creating runnable JAR...
cd /d %BUILDDIR%\classes
jar cfe %DISTDIR%\java\papiconverter.jar org.sharlychess.papiconverter.PapiConverter org\sharlychess\papiconverter\PapiConverter.class
cd /d %ROOTDIR%
