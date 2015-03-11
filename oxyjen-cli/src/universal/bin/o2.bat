@setlocal enabledelayedexpansion

@echo off

set "OXYJEN_HOME=%~dp0\\.."
set "APP_LIB_DIR=%OXYJEN_HOME%\lib\"

if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set "_JAVACMD=%JAVA_HOME%\bin\java.exe"
)

if "%_JAVACMD%"=="" set _JAVACMD=java

rem We use the value of the JAVA_OPTS environment variable if defined, rather than the config.
set _JAVA_OPTS=%JAVA_OPTS%

set "APP_CLASSPATH=%OXYJEN_HOME%\*;%APP_LIB_DIR%\*"
set "APP_MAIN_CLASS=org.oxyjen.Main"

"%_JAVACMD%" !_JAVA_OPTS! -cp "%APP_CLASSPATH%" %APP_MAIN_CLASS% %*

@endlocal

exit /B %ERRORLEVEL%
