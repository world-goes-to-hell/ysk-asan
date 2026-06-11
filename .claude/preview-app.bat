@echo off
rem Claude Preview backend launcher. Uses localtest profile (ysk_asan_test schema isolation).
rem NOTE: explicit path required - NoDefaultCurrentDirectoryInExePath blocks bare "gradlew.bat".
cd /d "%~dp0.."
call "%~dp0..\gradlew.bat" bootRun --args=--spring.profiles.active=local,localtest
