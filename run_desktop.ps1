# Starborn Desktop Fast Launcher
$ErrorActionPreference = "Stop"

$jdkPaths = @(
    "C:\Program Files\Android\Android Studio\jbr",
    "C:\Program Files\Android\Android Studio\jre",
    $env:JAVA_HOME
)

foreach ($path in $jdkPaths) {
    if ($path -and (Test-Path "$path\bin\java.exe")) {
        $env:JAVA_HOME = $path
        Write-Host "Using JDK at: $env:JAVA_HOME" -ForegroundColor Cyan
        break
    }
}

Write-Host "Launching Starborn Desktop (Compose Multiplatform)..." -ForegroundColor Green
./gradlew :desktopApp:run
