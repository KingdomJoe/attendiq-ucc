@echo off
REM AttendIQ Desktop — production JAR launcher (connects to Render via api.properties)
REM Requires Java 17+

setlocal enabledelayedexpansion
set "DIR=%~dp0"
set "JAR="

for %%F in ("%DIR%..\desktop\target\attendance-desktop-*.jar") do (
  echo %%~nxF | findstr /i "original" >nul || set "JAR=%%F"
)

if not defined JAR (
  for %%F in ("%DIR%attendance-desktop-*.jar") do (
    echo %%~nxF | findstr /i "original" >nul || set "JAR=%%F"
  )
)

if not defined JAR (
  echo Could not find attendance-desktop-*.jar
  echo Download from: https://github.com/KingdomJoe/attendiq-ucc/releases
  exit /b 1
)

echo Starting AttendIQ Desktop: %JAR%
java -jar "%JAR%"
pause
