@echo off
set DIR=%~dp0
"%DIR%\jre-win\bin\java.exe" -cp "%DIR%\dist\java\papiconverter.jar" org.sharlychess.papiconverter.PapiConverter %*
