@echo off
REM AttendIQ Desktop — production launcher (connects to Render)
REM Requires Java 17+

set JAR=%~dp0attendance-desktop-1.0.0-SNAPSHOT.jar
if not exist "%JAR%" (
  echo Place this script next to attendance-desktop-*.jar
  exit /b 1
)

java -jar "%JAR%"
pause
