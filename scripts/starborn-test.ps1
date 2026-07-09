param(
    [string] $Suite = "smoke",
    [string[]] $Tag = @(),
    [string[]] $Flow = @(),
    [string] $Device,
    [switch] $InstallDebug,
    [switch] $SkipGradle,
    [switch] $SkipMaestro,
    [switch] $NoTelemetryPull,
    [switch] $List
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$planPath = Join-Path $repoRoot "playtests\starborn-test-plan.json"

if (-not (Test-Path -LiteralPath $planPath)) {
    throw "Missing test plan: $planPath"
}

$plan = Get-Content -Raw -LiteralPath $planPath | ConvertFrom-Json
$flowsById = @{}
$plan.flows | ForEach-Object { $flowsById[$_.id] = $_ }
$suiteObj = $plan.suites | Where-Object { $_.id -eq $Suite } | Select-Object -First 1

function Write-PlanList {
    Write-Host "Suites"
    $plan.suites | ForEach-Object {
        Write-Host ("  {0} - {1}" -f $_.id, $_.description)
    }
    Write-Host ""
    Write-Host "Flows"
    $plan.flows | Sort-Object id | ForEach-Object {
        Write-Host ("  {0} [{1}] - {2}" -f $_.id, ($_.tags -join ","), $_.qualityQuestion)
    }
}

if ($List) {
    Write-PlanList
    exit 0
}

if (-not $suiteObj -and $Flow.Count -eq 0 -and $Tag.Count -eq 0) {
    throw "Unknown suite '$Suite'. Use -List to see available suites."
}

$selectedFlows = @()
if ($Flow.Count -gt 0) {
    foreach ($flowId in $Flow) {
        if (-not $flowsById.ContainsKey($flowId)) {
            throw "Unknown flow '$flowId'. Use -List to see available flows."
        }
        $selectedFlows += $flowsById[$flowId]
    }
} elseif ($Tag.Count -gt 0) {
    $selectedFlows = @($plan.flows | Where-Object {
        $flowTags = @($_.tags)
        foreach ($wanted in $Tag) {
            if ($wanted -notin $flowTags) { return $false }
        }
        return $true
    })
} elseif ($suiteObj.flowIds -contains "*") {
    $selectedFlows = @($plan.flows | Sort-Object id)
} else {
    foreach ($flowId in $suiteObj.flowIds) {
        if (-not $flowsById.ContainsKey($flowId)) {
            throw "Suite '$Suite' references unknown flow '$flowId'."
        }
        $selectedFlows += $flowsById[$flowId]
    }
}

if ($selectedFlows.Count -eq 0) {
    throw "No flows selected."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportRoot = Join-Path $repoRoot "reports\playtests\$timestamp"
$logRoot = Join-Path $reportRoot "logs"
$screenshotRoot = Join-Path $reportRoot "screenshots"
New-Item -ItemType Directory -Force -Path $reportRoot, $logRoot, $screenshotRoot | Out-Null

function Invoke-CapturedProcess {
    param(
        [string] $Name,
        [string] $FilePath,
        [string[]] $ArgumentList,
        [string] $WorkingDirectory,
        [string] $StdoutPath,
        [string] $StderrPath
    )

    $started = Get-Date
    Push-Location $WorkingDirectory
    try {
        & $FilePath @ArgumentList 1> $StdoutPath 2> $StderrPath
        $exitCode = $LASTEXITCODE
        if ($null -eq $exitCode) {
            $exitCode = if ($?) { 0 } else { 1 }
        }
    } finally {
        Pop-Location
    }
    $ended = Get-Date
    [PSCustomObject]@{
        name = $Name
        exitCode = $exitCode
        startedAt = $started.ToString("o")
        endedAt = $ended.ToString("o")
        durationSeconds = [math]::Round(($ended - $started).TotalSeconds, 2)
        stdout = $StdoutPath
        stderr = $StderrPath
    }
}

function Get-ConnectedDeviceIds {
    $output = & (Join-Path $PSScriptRoot "adb.ps1") devices
    if ($LASTEXITCODE -ne 0) {
        throw "adb devices failed."
    }
    @($output | Where-Object { $_ -match "^\S+\s+device$" } | ForEach-Object { ($_ -split "\s+")[0] })
}

function Invoke-AdbBestEffort {
    param([string[]] $AdbArgs)
    try {
        & (Join-Path $PSScriptRoot "adb.ps1") @AdbArgs | Out-Null
    } catch {
        Write-Warning $_.Exception.Message
    }
}

function Invoke-AdbOutputBestEffort {
    param([string[]] $AdbArgs)
    try {
        & (Join-Path $PSScriptRoot "adb.ps1") @AdbArgs
    } catch {
        Write-Warning $_.Exception.Message
        @()
    }
}

Push-Location $repoRoot
$previousAndroidSerial = $env:ANDROID_SERIAL
try {
    $results = @()
    $gradleResults = @()
    $runStarted = Get-Date

    if (-not $SkipGradle) {
        $gradleTasks = @()
        if ($suiteObj) {
            $gradleTasks = @($suiteObj.gradleTasks)
        }
        if ($gradleTasks.Count -gt 0) {
            Write-Host "Running Gradle checks: $($gradleTasks -join ' ')"
            $gradleResult = Invoke-CapturedProcess `
                -Name "gradle" `
                -FilePath (Join-Path $repoRoot "gradlew.bat") `
                -ArgumentList ($gradleTasks + @("--console=plain")) `
                -WorkingDirectory $repoRoot `
                -StdoutPath (Join-Path $logRoot "gradle.out.log") `
                -StderrPath (Join-Path $logRoot "gradle.err.log")
            $gradleResults += $gradleResult
            if ($gradleResult.exitCode -ne 0) {
                Write-Warning "Gradle checks failed with exit code $($gradleResult.exitCode). Maestro flows will still run unless -SkipMaestro was supplied."
            }
        }
    }

    $deviceId = $Device
    if ($SkipMaestro) {
        foreach ($flowObj in $selectedFlows) {
            $results += [PSCustomObject]@{
                id = $flowObj.id
                status = "skipped"
                exitCode = $null
                qualityQuestion = $flowObj.qualityQuestion
                file = $flowObj.file
                tags = @($flowObj.tags)
                debugScenario = $flowObj.debugScenario
                stdout = $null
                stderr = $null
                screenshots = @()
                durationSeconds = 0
            }
        }
    } else {
        if ([string]::IsNullOrWhiteSpace($deviceId)) {
            $devices = Get-ConnectedDeviceIds
            if ($devices.Count -eq 0) {
                throw "No Android device is visible to adb. Re-run with -SkipMaestro for registry/report checks only."
            }
            if ($devices.Count -gt 1) {
                throw "Multiple Android devices are connected ($($devices -join ', ')). Re-run with -Device <id>."
            }
            $deviceId = $devices[0]
        }
        $env:ANDROID_SERIAL = $deviceId

        Invoke-AdbBestEffort -AdbArgs @("-s", $deviceId, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
        Invoke-AdbBestEffort -AdbArgs @("-s", $deviceId, "shell", "wm", "dismiss-keyguard")

        if ($InstallDebug) {
            Write-Host "Installing debug APK on $deviceId"
            $installResult = Invoke-CapturedProcess `
                -Name "installDebug" `
                -FilePath (Join-Path $repoRoot "gradlew.bat") `
                -ArgumentList @(":app:installDebug", "--console=plain") `
                -WorkingDirectory $repoRoot `
                -StdoutPath (Join-Path $logRoot "installDebug.out.log") `
                -StderrPath (Join-Path $logRoot "installDebug.err.log")
            $gradleResults += $installResult
            if ($installResult.exitCode -ne 0) {
                throw "installDebug failed with exit code $($installResult.exitCode)."
            }
        }

        foreach ($flowObj in $selectedFlows) {
            $flowStart = Get-Date
            $flowPath = Join-Path $repoRoot ($flowObj.file -replace "/", "\")
            if (-not (Test-Path -LiteralPath $flowPath)) {
                $results += [PSCustomObject]@{
                    id = $flowObj.id
                    status = "missing"
                    exitCode = 1
                    qualityQuestion = $flowObj.qualityQuestion
                    file = $flowObj.file
                    stdout = $null
                    stderr = $null
                    screenshots = @()
                    durationSeconds = 0
                }
                continue
            }

            Write-Host "Running Maestro flow: $($flowObj.id)"
            $stdoutPath = Join-Path $logRoot "$($flowObj.id).out.log"
            $stderrPath = Join-Path $logRoot "$($flowObj.id).err.log"
            $result = Invoke-CapturedProcess `
                -Name $flowObj.id `
                -FilePath "powershell" `
                -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $repoRoot "scripts\maestro.ps1"), "--device", $deviceId, "test", $flowPath) `
                -WorkingDirectory $repoRoot `
                -StdoutPath $stdoutPath `
                -StderrPath $stderrPath

            $screenshots = @()
            Get-ChildItem -LiteralPath $repoRoot -Filter "*.png" -File |
                Where-Object { $_.LastWriteTime -ge $flowStart } |
                ForEach-Object {
                    $target = Join-Path $screenshotRoot $_.Name
                    Copy-Item -LiteralPath $_.FullName -Destination $target -Force
                    $screenshots += (Resolve-Path -LiteralPath $target).Path
                }

            $results += [PSCustomObject]@{
                id = $flowObj.id
                status = if ($result.exitCode -eq 0) { "passed" } else { "failed" }
                exitCode = $result.exitCode
                qualityQuestion = $flowObj.qualityQuestion
                file = $flowObj.file
                tags = @($flowObj.tags)
                debugScenario = $flowObj.debugScenario
                stdout = $stdoutPath
                stderr = $stderrPath
                screenshots = $screenshots
                durationSeconds = $result.durationSeconds
            }
        }
    }

    if (-not $NoTelemetryPull -and -not [string]::IsNullOrWhiteSpace($deviceId)) {
        $telemetryPath = Join-Path $reportRoot "telemetry_tail.jsonl"
        $crashPath = Join-Path $reportRoot "crash_files.txt"
        Invoke-AdbOutputBestEffort -AdbArgs @("-s", $deviceId, "shell", "run-as", $plan.appId, "sh", "-c", "cat files/telemetry/*.jsonl 2>/dev/null | tail -n 200") |
            Out-File -FilePath $telemetryPath -Encoding utf8
        Invoke-AdbOutputBestEffort -AdbArgs @("-s", $deviceId, "shell", "run-as", $plan.appId, "sh", "-c", "ls -l files/crash 2>/dev/null || true") |
            Out-File -FilePath $crashPath -Encoding utf8
    }

    $summary = [PSCustomObject]@{
        suite = $Suite
        tags = $Tag
        flowsRequested = @($selectedFlows.id)
        startedAt = $runStarted.ToString("o")
        endedAt = (Get-Date).ToString("o")
        device = $deviceId
        reportRoot = $reportRoot
        gradle = $gradleResults
        flows = $results
    }
    $summary | ConvertTo-Json -Depth 8 | Out-File -FilePath (Join-Path $reportRoot "report.json") -Encoding utf8

    $passed = @($results | Where-Object { $_.status -eq "passed" }).Count
    $skipped = @($results | Where-Object { $_.status -eq "skipped" }).Count
    $failed = @($results | Where-Object { $_.status -in @("failed", "missing") }).Count
    $markdown = @()
    $markdown += "# Starborn Playtest Report"
    $markdown += ""
    $markdown += ('- Suite: `{0}`' -f $Suite)
    $markdown += ('- Device: `{0}`' -f $deviceId)
    $markdown += "- Passed: $passed"
    $markdown += "- Skipped: $skipped"
    $markdown += "- Failed/Missing: $failed"
    $markdown += ('- Report: `{0}`' -f $reportRoot)
    $markdown += ""
    $markdown += "## Flow Results"
    foreach ($result in $results) {
        $markdown += ""
        $markdown += "### $($result.id): $($result.status)"
        $markdown += "- Question: $($result.qualityQuestion)"
        $markdown += ('- File: `{0}`' -f $result.file)
        $markdown += ('- Tags: `{0}`' -f (@($result.tags) -join ', '))
        if ($result.debugScenario) { $markdown += ('- Debug scenario: `{0}`' -f $result.debugScenario) }
        $markdown += "- Duration: $($result.durationSeconds)s"
        $markdown += ('- Logs: `{0}`, `{1}`' -f $result.stdout, $result.stderr)
        foreach ($screenshot in @($result.screenshots)) {
            $markdown += ('- Screenshot: `{0}`' -f $screenshot)
        }
    }
    $markdown += ""
    $markdown += "## Quality Review"
    $markdown += "Use `playtests/quality/starborn_quality_scorecard.md` to score plot clarity, emotion, conflict, intuition, and fun for failed or high-priority flows."
    $markdown | Out-File -FilePath (Join-Path $reportRoot "report.md") -Encoding utf8

    Write-Host ""
    Write-Host "Report written to $reportRoot"
    Write-Host "Passed: $passed; Failed/Missing: $failed"
    if ($failed -gt 0) { exit 1 }
}
finally {
    if ([string]::IsNullOrWhiteSpace($previousAndroidSerial)) {
        Remove-Item Env:\ANDROID_SERIAL -ErrorAction SilentlyContinue
    } else {
        $env:ANDROID_SERIAL = $previousAndroidSerial
    }
    Pop-Location
}
