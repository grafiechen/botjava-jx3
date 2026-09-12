@echo off
chcp 65001 >nul
setlocal EnableExtensions

set "APP_HOME=%~dp0"
if not defined JAR_NAME set "JAR_NAME=botjava-0.0.1.jar"
if not defined JAVA_BIN set "JAVA_BIN=java"
if not defined JAVA_OPTS set "JAVA_OPTS=-Xms128m -Xmx512m"
set "JAVA_ENCODING_OPTS=-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
if not defined SERVER_PORT set "SERVER_PORT=8081"

if exist "%JAR_NAME%" (
    for %%I in ("%JAR_NAME%") do set "JAR_PATH=%%~fI"
) else (
    set "JAR_PATH=%APP_HOME%%JAR_NAME%"
)

if defined CONFIG_FILE goto resolve_config
if exist "%APP_HOME%application.yml" (
    set "CONFIG_FILE=%APP_HOME%application.yml"
    goto config_ready
)
if exist "%APP_HOME%config\application.yml" (
    set "CONFIG_FILE=%APP_HOME%config\application.yml"
    goto config_ready
)
echo Configuration file not found: application.yml or config\application.yml
if not defined NO_PAUSE pause
exit /b 1

:resolve_config
if exist "%CONFIG_FILE%" (
    for %%I in ("%CONFIG_FILE%") do set "CONFIG_FILE=%%~fI"
    goto config_ready
)
if exist "%APP_HOME%%CONFIG_FILE%" (
    for %%I in ("%APP_HOME%%CONFIG_FILE%") do set "CONFIG_FILE=%%~fI"
    goto config_ready
)
echo Configuration file not found: %CONFIG_FILE%
if not defined NO_PAUSE pause
exit /b 1

:config_ready
if not exist "%JAR_PATH%" (
    echo JAR file not found: %JAR_PATH%
    if not defined NO_PAUSE pause
    exit /b 1
)

set "CONFIG_URI=file:%CONFIG_FILE:\=/%"
pushd "%APP_HOME%"
echo.
echo Starting botjava in the current CMD window...
echo JAR:    %JAR_PATH%
echo Config: %CONFIG_FILE%
echo Port:   %SERVER_PORT%
echo JVM:    %JAVA_OPTS%
echo Charset: UTF-8
echo Press Ctrl+C to stop the application.
echo.

"%JAVA_BIN%" %JAVA_OPTS% %JAVA_ENCODING_OPTS% -jar "%JAR_PATH%" "--spring.config.additional-location=%CONFIG_URI%" "--server.port=%SERVER_PORT%" "--logging.charset.console=UTF-8"
set "EXIT_CODE=%ERRORLEVEL%"

popd
echo.
echo botjava stopped, exitCode=%EXIT_CODE%
if not defined NO_PAUSE pause
exit /b %EXIT_CODE%