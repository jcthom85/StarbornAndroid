param(
    [string]$Device,
    [switch]$SkipDevice,
    [switch]$InstallDebug
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$reportDir = Join-Path $repoRoot "reports\fun-audit"
$artifactDir = Join-Path $repoRoot "playtests\artifacts\fun-audit"
New-Item -ItemType Directory -Force -Path $reportDir, $artifactDir | Out-Null
$androidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path ([Environment]::GetFolderPath("LocalApplicationData")) "Android\Sdk" }
$adbPath = Join-Path $androidHome "platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) { throw "adb.exe not found at $adbPath" }

$javaHome = "C:\Program Files\Android\Android Studio\jbr"
if (-not $env:JAVA_HOME -and (Test-Path $javaHome)) { $env:JAVA_HOME = $javaHome }
$env:GRADLE_USER_HOME = Join-Path $repoRoot ".gradle-codex"

Push-Location $repoRoot
try {
    & .\gradlew.bat :app:testDebugUnitTest --tests "com.example.starborn.domain.combat.FunAuditReportTest"
    if ($LASTEXITCODE -ne 0) { throw "Combat/skill audit failed." }

    if ($SkipDevice) { return }
    if ($InstallDebug) {
        & .\gradlew.bat :app:installDebug
        if ($LASTEXITCODE -ne 0) { throw "Debug install failed." }
    }
    if (-not $Device) {
        $Device = (& $adbPath devices | Select-String "\tdevice$").Line.Split("`t")[0]
    }
    if (-not $Device) { throw "No connected Android device found." }

    $flows = @(
        @{ Quest = "w3_sq14"; Name = "Corporate Espionage"; File = "fun_w3_corporate_espionage.yaml" },
        @{ Quest = "w4_sq19"; Name = "Quality Control"; File = "fun_w4_quality_control.yaml" },
        @{ Quest = "w5_sq24"; Name = "Ghost in the Shell"; File = "fun_w5_ghost_shell.yaml" },
        @{ Quest = "w6_sq27"; Name = "The HR Record"; File = "fun_w6_hr_record.yaml" }
    )
    $summaries = @()
    foreach ($flow in $flows) {
        & $adbPath -s $Device shell pm clear com.junewiregames.starborn.prealpha
        & $adbPath -s $Device shell am start -n com.junewiregames.starborn.prealpha/com.example.starborn.MainActivity
        Start-Sleep -Seconds 3
        & .\scripts\maestro.ps1 --device $Device test (Join-Path ".\playtests\maestro" $flow.File)
        if ($LASTEXITCODE -ne 0) { throw "Maestro failed for $($flow.Name)." }
        $telemetryPath = Join-Path $artifactDir "$($flow.Quest)-telemetry.jsonl"
        $telemetryLines = @(& $adbPath -s $Device exec-out run-as com.junewiregames.starborn.prealpha cat no_backup/playtest/fun-events.jsonl)
        $telemetryLines | Set-Content $telemetryPath
        $events = @(Get-Content $telemetryPath | Where-Object { $_.Trim() } | ForEach-Object { $_ | ConvertFrom-Json })
        $completed = $events | Where-Object { $_.event -eq "quest_completed" -and $_.quest_id -eq $flow.Quest } | Select-Object -Last 1
        $visitedRooms = @(
            $events | Where-Object { $_.event -in @("room_entered", "room_action_selected") } | ForEach-Object { $_.room_id }
        ) | Where-Object { $_ } | Sort-Object -Unique
        $summaries += [pscustomobject]@{
            quest_id = $flow.Quest
            title = $flow.Name
            duration_ms = $completed.duration_ms
            rooms_visited = @($visitedRooms).Count
            room_actions = @($events | Where-Object event -eq "room_action_selected").Count
            completed = $null -ne $completed
            screenshots = @("discovery", "complication", "payoff")
        }
    }
    $summaries | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $reportDir "representative-quests.json")
    $markdown = @("# Representative Quest Audit", "", "| Quest | Duration | Rooms | Actions | Completed |", "|---|---:|---:|---:|---|")
    $markdown += $summaries | ForEach-Object { "| $($_.title) | $($_.duration_ms) ms | $($_.rooms_visited) | $($_.room_actions) | $($_.completed) |" }
    $markdown += "", "Visual scorecards are recorded in docs/story/Fun_Factor_Audit.md after screenshot review."
    $markdown | Set-Content (Join-Path $reportDir "representative-quests.md")
} finally {
    Pop-Location
}
