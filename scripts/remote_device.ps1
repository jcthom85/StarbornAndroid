<#
.SYNOPSIS
    Remote Wireless Device Management & Instant Deployment for Starborn Android.

.DESCRIPTION
    Enables remote wireless debugging and rapid iteration (10-15s deploy cycles)
    over Tailscale or local network.

.EXAMPLE
    # 1. Pair your phone (first-time setup only):
    .\scripts\remote_device.ps1 pair 100.x.y.z:37123 123456

    # 2. Connect to your phone:
    .\scripts\remote_device.ps1 connect 100.x.y.z:38455

    # 3. Hot deploy (build debug + install + launch in ~10-15s):
    .\scripts\remote_device.ps1 deploy

    # 4. Stream real-time logs from phone:
    .\scripts\remote_device.ps1 logs

    # 5. Capture screenshot from phone:
    .\scripts\remote_device.ps1 screenshot
#>

param(
    [Parameter(Position = 0)]
    [ValidateSet('pair', 'connect', 'deploy', 'install', 'launch', 'logs', 'devices', 'screenshot', 'status')]
    [string]$Action = 'deploy',

    [Parameter(Position = 1)]
    [string]$Target,

    [Parameter(Position = 2)]
    [string]$PairCode
)

$ErrorActionPreference = 'Stop'
$ConfigPath = Join-Path $PSScriptRoot ".remote_device.json"

function Get-AdbPath {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $localSdk = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $localSdk) { return $localSdk }
    throw "adb.exe not found in PATH or Android SDK platform-tools."
}

$Adb = Get-AdbPath

function Save-DeviceConfig($ipPort) {
    @{ Target = $ipPort; LastConnected = (Get-Date).ToString("o") } | ConvertTo-Json | Set-Content -Path $ConfigPath -Encoding UTF8
}

function Get-SavedTarget {
    if (Test-Path $ConfigPath) {
        try {
            $data = Get-Content $ConfigPath -Raw | ConvertFrom-Json
            return $data.Target
        } catch { }
    }
    return $null
}

switch ($Action) {
    'pair' {
        if (-not $Target -or -not $PairCode) {
            Write-Host "Usage: .\scripts\remote_device.ps1 pair <IP:PAIRING_PORT> <6_DIGIT_CODE>" -ForegroundColor Yellow
            Write-Host "Example: .\scripts\remote_device.ps1 pair 100.101.102.103:37123 849201" -ForegroundColor Cyan
            exit 1
        }
        Write-Host "Pairing with $Target using code $PairCode..." -ForegroundColor Cyan
        & $Adb pair $Target $PairCode
    }

    'connect' {
        $dest = if ($Target) { $Target } else { Get-SavedTarget }
        if (-not $dest) {
            Write-Host "Usage: .\scripts\remote_device.ps1 connect <IP:PORT>" -ForegroundColor Yellow
            Write-Host "Example: .\scripts\remote_device.ps1 connect 100.101.102.103:38455" -ForegroundColor Cyan
            exit 1
        }
        Write-Host "Connecting to wireless ADB target: $dest..." -ForegroundColor Cyan
        & $Adb connect $dest
        Save-DeviceConfig $dest
        & $Adb devices
    }

    'devices' {
        Write-Host "Connected ADB Devices:" -ForegroundColor Cyan
        & $Adb devices -l
    }

    'status' {
        Write-Host "=== ADB Devices ===" -ForegroundColor Cyan
        & $Adb devices -l
        $saved = Get-SavedTarget
        if ($saved) {
            Write-Host "`nSaved Target: $saved" -ForegroundColor Gray
        }
    }

    'deploy' {
        # Allow passing port or IP:PORT directly to deploy (e.g. .\scripts\remote_device.ps1 deploy 38455)
        if ($Target) {
            $dest = if ($Target -match '^\d+$') { "100.81.103.3:$Target" } else { $Target }
            Write-Host "Connecting to target: $dest..." -ForegroundColor Cyan
            & $Adb connect $dest
            Save-DeviceConfig $dest
        }

        $saved = Get-SavedTarget
        $connectedDevices = (& $Adb devices) | Where-Object { $_ -match '\bdevice\b' -and $_ -notmatch 'List of devices' }
        if (-not $connectedDevices) {
            if ($saved) {
                Write-Host "Attempting reconnect to saved target: $saved..." -ForegroundColor Yellow
                & $Adb connect $saved
                $connectedDevices = (& $Adb devices) | Where-Object { $_ -match '\bdevice\b' -and $_ -notmatch 'List of devices' }
            }
        }

        if (-not $connectedDevices) {
            Write-Host "`n❌ No Android device connected via ADB." -ForegroundColor Red
            Write-Host "Check the port on your phone's 'Wireless Debugging' screen and run:" -ForegroundColor Yellow
            Write-Host "  .\scripts\remote_device.ps1 deploy <PORT>" -ForegroundColor Cyan
            Write-Host "Example: .\scripts\remote_device.ps1 deploy 38455`n" -ForegroundColor Gray
            exit 1
        }

        # Select target device (prefer wireless target over local emulator)
        $deviceTarget = if ($saved -and ($connectedDevices -match [regex]::Escape($saved))) {
            $saved
        } else {
            $remoteMatch = $connectedDevices | Where-Object { $_ -match '(\d+\.\d+\.\d+\.\d+:\d+)' } | Select-Object -First 1
            if ($remoteMatch -and ($remoteMatch -match '(\d+\.\d+\.\d+\.\d+:\d+)')) {
                $Matches[1]
            } else {
                ($connectedDevices | Select-Object -First 1).Split()[0].Trim()
            }
        }

        Write-Host "Target device: $deviceTarget" -ForegroundColor Cyan
        Write-Host "Building Debug APK..." -ForegroundColor Cyan
        $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
        $gradle = Join-Path $PSScriptRoot "..\gradlew.bat"
        & $gradle :app:assembleDebug
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        $apk = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
        if (-not (Test-Path $apk)) { throw "Built APK not found at $apk" }

        Write-Host "Installing APK to $deviceTarget (fast install)..." -ForegroundColor Cyan
        & $Adb -s $deviceTarget install -r -d -g $apk
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

        Write-Host "Launching Starborn on $deviceTarget..." -ForegroundColor Green
        & $Adb -s $deviceTarget shell am start -n com.junewiregames.starborn.prealpha/com.example.starborn.MainActivity
        Write-Host "`n⚡ Deploy complete in seconds!" -ForegroundColor Green
    }

    'install' {
        $saved = Get-SavedTarget
        $deviceArg = if ($saved) { @("-s", $saved) } else { @() }
        $apk = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
        if (-not (Test-Path $apk)) {
            Write-Host "APK not found. Run .\scripts\remote_device.ps1 deploy to build and install." -ForegroundColor Red
            exit 1
        }
        Write-Host "Installing APK directly..." -ForegroundColor Cyan
        & $Adb @deviceArg install -r -d -g $apk
    }

    'launch' {
        $saved = Get-SavedTarget
        $deviceArg = if ($saved) { @("-s", $saved) } else { @() }
        Write-Host "Launching Starborn on device..." -ForegroundColor Green
        & $Adb @deviceArg shell am start -n com.junewiregames.starborn.prealpha/com.example.starborn.MainActivity
    }

    'logs' {
        $saved = Get-SavedTarget
        $deviceArg = if ($saved) { @("-s", $saved) } else { @() }
        Write-Host "Streaming Starborn logcat (Ctrl+C to stop)..." -ForegroundColor Cyan
        & $Adb @deviceArg logcat -v color -s Starborn:V StarbornAudio:V AndroidRuntime:E System.err:E
    }

    'screenshot' {
        $saved = Get-SavedTarget
        $deviceArg = if ($saved) { @("-s", $saved) } else { @() }
        $outDir = Join-Path $PSScriptRoot "..\build\screenshots"
        if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }
        $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
        $localPath = Join-Path $outDir "screenshot_$timestamp.png"
        
        Write-Host "Capturing screenshot from device..." -ForegroundColor Cyan
        & $Adb @deviceArg exec-out screencap -p > $localPath
        Write-Host "Saved screenshot to: $localPath" -ForegroundColor Green
    }
}
