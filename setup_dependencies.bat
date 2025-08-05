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

:: Download H2 Database (pure Java SQLite alternative)
echo Downloading H2 Database...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/h2database/h2/2.2.224/h2-2.2.224.jar' -OutFile 'lib\h2-2.2.224.jar'"

:: Download SLF4J logging libraries
echo Downloading SLF4J API...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar' -OutFile 'lib\slf4j-api-2.0.9.jar'"

echo Downloading SLF4J Simple...
powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar' -OutFile 'lib\slf4j-simple-2.0.9.jar'"

echo Dependencies downloaded successfully!
echo You can now run: build_app_win.bat
