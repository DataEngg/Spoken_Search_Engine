@REM ----------------------------------------------------------------------------
@REM  Copyright 2001-2006 The Apache Software Foundation.
@REM
@REM  Licensed under the Apache License, Version 2.0 (the "License");
@REM  you may not use this file except in compliance with the License.
@REM  You may obtain a copy of the License at
@REM
@REM       http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM  Unless required by applicable law or agreed to in writing, software
@REM  distributed under the License is distributed on an "AS IS" BASIS,
@REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@REM  See the License for the specific language governing permissions and
@REM  limitations under the License.
@REM ----------------------------------------------------------------------------
@REM
@REM   Copyright (c) 2001-2006 The Apache Software Foundation.  All rights
@REM   reserved.

@echo off

set ERROR_CODE=0

:init
@REM Decide how to startup depending on the version of windows

@REM -- Win98ME
if NOT "%OS%"=="Windows_NT" goto Win9xArg

@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" @setlocal

@REM -- 4NT shell
if "%eval[2+2]" == "4" goto 4NTArgs

@REM -- Regular WinNT shell
set CMD_LINE_ARGS=%*
goto WinNTGetScriptDir

@REM The 4NT Shell from jp software
:4NTArgs
set CMD_LINE_ARGS=%$
goto WinNTGetScriptDir

:Win9xArg
@REM Slurp the command line arguments.  This loop allows for an unlimited number
@REM of arguments (up to the command line limit, anyway).
set CMD_LINE_ARGS=
:Win9xApp
if %1a==a goto Win9xGetScriptDir
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto Win9xApp

:Win9xGetScriptDir
set SAVEDIR=%CD%
%0\
cd %0\..\.. 
set BASEDIR=%CD%
cd %SAVEDIR%
set SAVE_DIR=
goto repoSetup

:WinNTGetScriptDir
set BASEDIR=%~dp0\..

:repoSetup
set REPO=


if "%JAVACMD%"=="" set JAVACMD=java

if "%REPO%"=="" set REPO=%BASEDIR%\lib

set CLASSPATH="%BASEDIR%"\etc;"%REPO%"\xmlenc-0.52.jar;"%REPO%"\galagosearch-tupleflow-1.04.jar;"%REPO%"\maven-surefire-plugin-2.17.jar;"%REPO%"\maven-plugin-api-2.0.9.jar;"%REPO%"\maven-surefire-common-2.17.jar;"%REPO%"\surefire-booter-2.17.jar;"%REPO%"\maven-artifact-2.0.9.jar;"%REPO%"\plexus-utils-1.5.1.jar;"%REPO%"\maven-plugin-descriptor-2.0.9.jar;"%REPO%"\plexus-container-default-1.0-alpha-9-stable-1.jar;"%REPO%"\maven-project-2.0.9.jar;"%REPO%"\maven-settings-2.0.9.jar;"%REPO%"\maven-profile-2.0.9.jar;"%REPO%"\maven-artifact-manager-2.0.9.jar;"%REPO%"\maven-plugin-registry-2.0.9.jar;"%REPO%"\maven-model-2.0.9.jar;"%REPO%"\maven-core-2.0.9.jar;"%REPO%"\maven-plugin-parameter-documenter-2.0.9.jar;"%REPO%"\maven-reporting-api-2.0.9.jar;"%REPO%"\wagon-provider-api-1.0-beta-2.jar;"%REPO%"\maven-repository-metadata-2.0.9.jar;"%REPO%"\maven-error-diagnostics-2.0.9.jar;"%REPO%"\maven-monitor-2.0.9.jar;"%REPO%"\classworlds-1.1.jar;"%REPO%"\commons-lang3-3.1.jar;"%REPO%"\surefire-api-2.17.jar;"%REPO%"\maven-toolchain-2.0.9.jar;"%REPO%"\maven-plugin-annotations-3.2.jar;"%REPO%"\stringtemplate-3.0.jar;"%REPO%"\antlr-2.7.7.jar;"%REPO%"\jetty-embedded-6.1.5.jar;"%REPO%"\jetty-6.1.5.jar;"%REPO%"\jetty-util-6.1.5.jar;"%REPO%"\servlet-api-2.5-6.1.5.jar;"%REPO%"\jsp-api-2.1.jar;"%REPO%"\galagosearch-core-1.04.jar

set ENDORSED_DIR=
if NOT "%ENDORSED_DIR%" == "" set CLASSPATH="%BASEDIR%"\%ENDORSED_DIR%\*;%CLASSPATH%

if NOT "%CLASSPATH_PREFIX%" == "" set CLASSPATH=%CLASSPATH_PREFIX%;%CLASSPATH%

@REM Reaching here means variables are defined and arguments have been captured
:endInit

%JAVACMD% %JAVA_OPTS% -Xms256m -Xmx512m -classpath %CLASSPATH% -Dapp.name="galago" -Dapp.repo="%REPO%" -Dapp.home="%BASEDIR%" -Dbasedir="%BASEDIR%" org.galagosearch.core.tools.App %CMD_LINE_ARGS%
if %ERRORLEVEL% NEQ 0 goto error
goto end

:error
if "%OS%"=="Windows_NT" @endlocal
set ERROR_CODE=%ERRORLEVEL%

:end
@REM set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" goto endNT

@REM For old DOS remove the set variables from ENV - we assume they were not set
@REM before we started - at least we don't leave any baggage around
set CMD_LINE_ARGS=
goto postExec

:endNT
@REM If error code is set to 1 then the endlocal was done already in :error.
if %ERROR_CODE% EQU 0 @endlocal


:postExec

if "%FORCE_EXIT_ON_ERROR%" == "on" (
  if %ERROR_CODE% NEQ 0 exit %ERROR_CODE%
)

exit /B %ERROR_CODE%
