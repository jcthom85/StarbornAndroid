param(
    [switch]$SkipGradle,
    [switch]$SkipTests,
    [switch]$SkipLint,
    [switch]$SkipMaestro,
    [switch]$InstallDebug,
    [string]$Device
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$previousAndroidSerial = $env:ANDROID_SERIAL
$previousGradleUserHome = $env:GRADLE_USER_HOME
$previousJavaToolOptions = $env:JAVA_TOOL_OPTIONS
$previousGradleOpts = $env:GRADLE_OPTS
$previousKotlinCompilerExecutionStrategy = $env:KOTLIN_COMPILER_EXECUTION_STRATEGY
$previousTemp = $env:TEMP
$previousTmp = $env:TMP
$previousLocalAppData = $env:LOCALAPPDATA
$previousAppData = $env:APPDATA
$previousAndroidUserHome = $env:ANDROID_USER_HOME

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Script
    )

    Write-Host ""
    Write-Host "==> $Name"
    $previousStepErrorAction = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $Script
        $stepExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousStepErrorAction
    }
    if ($stepExitCode -ne 0) {
        throw "$Name failed with exit code $stepExitCode."
    }
}

function Get-ConnectedDeviceIds {
    $output = & (Join-Path $PSScriptRoot "adb.ps1") devices
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed with exit code $LASTEXITCODE."
    }

    return @(
        $output |
            Where-Object { $_ -match "^\S+\s+device$" } |
            ForEach-Object { ($_ -split "\s+")[0] }
    )
}

function Invoke-AdbBestEffort {
    param(
        [string]$DeviceId,
        [string[]]$CommandArgs
    )

    & (Join-Path $PSScriptRoot "adb.ps1") -s $DeviceId @CommandArgs | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "adb $($CommandArgs -join ' ') failed with exit code $LASTEXITCODE. Maestro may fail if the device is locked."
    }
}

Push-Location $repoRoot
try {
    $gradleUserHome = Join-Path $repoRoot ".gradle-codex"
    $workspaceTmp = Join-Path $gradleUserHome "tmp"
    $workspaceLocalAppData = Join-Path $gradleUserHome "AppData\Local"
    $workspaceAppData = Join-Path $gradleUserHome "AppData\Roaming"
    $workspaceAndroidHome = Join-Path $gradleUserHome ".android"
    New-Item -ItemType Directory -Force -Path `
        $gradleUserHome, `
        $workspaceTmp, `
        (Join-Path $workspaceLocalAppData "kotlin\daemon"), `
        $workspaceAppData, `
        $workspaceAndroidHome | Out-Null

    $env:GRADLE_USER_HOME = $gradleUserHome
    $env:TEMP = $workspaceTmp
    $env:TMP = $workspaceTmp
    $env:LOCALAPPDATA = $workspaceLocalAppData
    $env:APPDATA = $workspaceAppData
    $env:ANDROID_USER_HOME = $workspaceAndroidHome
    $env:KOTLIN_COMPILER_EXECUTION_STRATEGY = "in-process"
    $tmpOption = "-Djava.io.tmpdir=$workspaceTmp"
    $userHomeOption = "-Duser.home=$gradleUserHome"
    $env:GRADLE_OPTS = (($previousGradleOpts, $tmpOption, $userHomeOption) | Where-Object { $_ } | Select-Object -Unique) -join " "
    $gradleArgs = @(
        "--no-daemon",
        "-Dkotlin.compiler.execution.strategy=in-process",
        "-Dkotlin.daemon.enabled=false",
        "-Djava.io.tmpdir=$workspaceTmp"
    )

    if (-not $SkipGradle) {
        Invoke-Step "World 1 asset and integrity validators" {
            & .\gradlew.bat @gradleArgs :app:validateWorld1Assets :app:runAssetIntegrity
        }
    }

    if (-not $SkipTests) {
        Invoke-Step "JVM unit tests" {
            & .\gradlew.bat @gradleArgs :app:testDebugUnitTest
        }
    }

    if (-not $SkipLint) {
        Invoke-Step "Android lint" {
            & .\gradlew.bat @gradleArgs :app:lintDebug
        }
    }

    if ($SkipMaestro -and -not $InstallDebug) {
        Write-Host ""
        Write-Host "Skipping Maestro flows by request."
        return
    }

    $deviceId = $Device
    if ([string]::IsNullOrWhiteSpace($deviceId)) {
        $devices = Get-ConnectedDeviceIds
        if ($devices.Count -eq 0) {
            Write-Warning "No Android device is visible to adb. Skipping Maestro flows."
            return
        }
        if ($devices.Count -gt 1) {
            throw "Multiple Android devices are connected ($($devices -join ', ')). Re-run with -Device <id>."
        }
        $deviceId = $devices[0]
    }

    $env:ANDROID_SERIAL = $deviceId
    Invoke-AdbBestEffort -DeviceId $deviceId -CommandArgs @("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    Invoke-AdbBestEffort -DeviceId $deviceId -CommandArgs @("shell", "wm", "dismiss-keyguard")

    if ($InstallDebug) {
        Invoke-Step "Install debug APK on $deviceId" {
            & .\gradlew.bat @gradleArgs :app:installDebug
        }
    }

    if ($SkipMaestro) {
        Write-Host ""
        Write-Host "Skipping Maestro flows by request."
        return
    }

    $flows = @(
        "smoke_launch.yaml",
        "start_new_game.yaml",
        "early_tutorial_dismiss.yaml",
        "early_exploration_navigation.yaml",
        "room_keyword_inspection.yaml",
        "early_jed_dialogue.yaml",
        "mainquest_wake_up_call.yaml",
        "room_entities_tray.yaml",
        "room_item_pickup.yaml",
        "presence_stress.yaml",
        "presence_combat_return.yaml",
        "shop_scrapper_contraband.yaml",
        "sidequest_system_flush.yaml",
        "sidequest_scavenger_stash.yaml",
        "heavy_lifting_training.yaml",
        "checkpoint_badge_gate.yaml",
        "mainquest_the_echo.yaml",
        "mainquest_red_alert.yaml",
        "mainquest_the_launch.yaml",
        "debug_hub1.yaml",
        "debug_hub2.yaml",
        "first_combat_entry.yaml",
        "enemy_party_combat.yaml",
        "combat_target_prompt.yaml",
        "combat_command_menu.yaml",
        "combat_abilities_menu.yaml",
        "combat_menu_dismiss.yaml",
        "combat_enemy_status_rail.yaml",
        "combat_flashbang_fx.yaml",
        "save_load_roundtrip.yaml",
        "debug_full_inventory_menu.yaml"
    )

    $flowPaths = $flows | ForEach-Object { Join-Path "playtests\maestro" $_ }
    Invoke-Step "Maestro World 1 suite on $deviceId" {
        & .\scripts\maestro.ps1 --device $deviceId test @flowPaths
    }
}
finally {
    if ([string]::IsNullOrWhiteSpace($previousGradleUserHome)) {
        Remove-Item Env:\GRADLE_USER_HOME -ErrorAction SilentlyContinue
    } else {
        $env:GRADLE_USER_HOME = $previousGradleUserHome
    }
    if ([string]::IsNullOrWhiteSpace($previousJavaToolOptions)) {
        Remove-Item Env:\JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue
    } else {
        $env:JAVA_TOOL_OPTIONS = $previousJavaToolOptions
    }
    if ([string]::IsNullOrWhiteSpace($previousGradleOpts)) {
        Remove-Item Env:\GRADLE_OPTS -ErrorAction SilentlyContinue
    } else {
        $env:GRADLE_OPTS = $previousGradleOpts
    }
    if ([string]::IsNullOrWhiteSpace($previousKotlinCompilerExecutionStrategy)) {
        Remove-Item Env:\KOTLIN_COMPILER_EXECUTION_STRATEGY -ErrorAction SilentlyContinue
    } else {
        $env:KOTLIN_COMPILER_EXECUTION_STRATEGY = $previousKotlinCompilerExecutionStrategy
    }
    if ([string]::IsNullOrWhiteSpace($previousTemp)) {
        Remove-Item Env:\TEMP -ErrorAction SilentlyContinue
    } else {
        $env:TEMP = $previousTemp
    }
    if ([string]::IsNullOrWhiteSpace($previousTmp)) {
        Remove-Item Env:\TMP -ErrorAction SilentlyContinue
    } else {
        $env:TMP = $previousTmp
    }
    if ([string]::IsNullOrWhiteSpace($previousLocalAppData)) {
        Remove-Item Env:\LOCALAPPDATA -ErrorAction SilentlyContinue
    } else {
        $env:LOCALAPPDATA = $previousLocalAppData
    }
    if ([string]::IsNullOrWhiteSpace($previousAppData)) {
        Remove-Item Env:\APPDATA -ErrorAction SilentlyContinue
    } else {
        $env:APPDATA = $previousAppData
    }
    if ([string]::IsNullOrWhiteSpace($previousAndroidUserHome)) {
        Remove-Item Env:\ANDROID_USER_HOME -ErrorAction SilentlyContinue
    } else {
        $env:ANDROID_USER_HOME = $previousAndroidUserHome
    }
    if ([string]::IsNullOrWhiteSpace($previousAndroidSerial)) {
        Remove-Item Env:\ANDROID_SERIAL -ErrorAction SilentlyContinue
    } else {
        $env:ANDROID_SERIAL = $previousAndroidSerial
    }
    Pop-Location
}
