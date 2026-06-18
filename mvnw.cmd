@ECHO OFF
REM ─────────────────────────────────────────────────────────────────────────────
REM Maven Wrapper startup script for Windows
REM Automatically downloads Apache Maven 3.9.6 if not already present.
REM ─────────────────────────────────────────────────────────────────────────────

SETLOCAL EnableDelayedExpansion

SET MAVEN_VERSION=3.9.6
SET MAVEN_BASE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%-bin
SET MAVEN_HOME=%MAVEN_BASE%\apache-maven-%MAVEN_VERSION%
SET MAVEN_DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip
SET MAVEN_ARCHIVE=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip

IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  ECHO Downloading Apache Maven %MAVEN_VERSION%...
  IF NOT EXIST "%MAVEN_BASE%" (
    MKDIR "%MAVEN_BASE%"
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "try { " ^
    "  [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; " ^
    "  Invoke-WebRequest -Uri '%MAVEN_DOWNLOAD_URL%' -OutFile '%MAVEN_ARCHIVE%' -UseBasicParsing; " ^
    "  Expand-Archive -Path '%MAVEN_ARCHIVE%' -DestinationPath '%MAVEN_BASE%' -Force; " ^
    "  Remove-Item '%MAVEN_ARCHIVE%' -Force; " ^
    "  Write-Host 'Maven %MAVEN_VERSION% installed successfully.'; " ^
    "} catch { Write-Error $_.Exception.Message; exit 1 }"
  IF ERRORLEVEL 1 (
    ECHO ERROR: Failed to download or extract Maven. Please install Maven manually.
    EXIT /B 1
  )
  ECHO Maven %MAVEN_VERSION% installed to %MAVEN_HOME%
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
ENDLOCAL
