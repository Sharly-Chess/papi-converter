@echo off
setlocal enabledelayedexpansion

echo Setting up PAPI Converter dependencies...

:: Create lib directory
if not exist lib mkdir lib

:: Download Jackcess and dependencies
echo Downloading Jackcess...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/healthmarketscience/jackcess/jackcess/4.0.5/jackcess-4.0.5.jar' -OutFile 'lib\jackcess-4.0.5.jar'"

echo Downloading Apache Commons Lang...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar' -OutFile 'lib\commons-lang3-3.12.0.jar'"

echo Downloading Apache Commons Logging...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/commons-logging/commons-logging/1.2/commons-logging-1.2.jar' -OutFile 'lib\commons-logging-1.2.jar'"

:: Download Jackson JSON libraries
echo Downloading Jackson Core...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar' -OutFile 'lib\jackson-core-2.15.2.jar'"

echo Downloading Jackson Databind...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar' -OutFile 'lib\jackson-databind-2.15.2.jar'"

echo Downloading Jackson Annotations...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar' -OutFile 'lib\jackson-annotations-2.15.2.jar'"

echo Dependencies downloaded successfully!
echo You can now run: build_app_win.bat
