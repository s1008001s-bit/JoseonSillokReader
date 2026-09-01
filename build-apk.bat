@echo off
if not exist gradlew.bat (echo Android Studio에서 프로젝트를 한번 열어 Gradle Wrapper를 생성하세요.&pause&exit /b 1)
call gradlew.bat assembleRelease
if errorlevel 1 exit /b 1
echo APK: app\build\outputs\apk\release\app-release.apk
pause
