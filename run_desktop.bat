@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
echo Using JDK at: %JAVA_HOME%
echo Launching Starborn Desktop (Compose Multiplatform)...
call gradlew.bat :desktopApp:run
pause
