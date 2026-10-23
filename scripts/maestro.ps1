param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MaestroArgs
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'maestro-common.ps1')
$maestroRepoRoot = Split-Path -Parent $PSScriptRoot
$config = Get-StarbornMaestroConfig
$installation = Join-Path $maestroRepoRoot "tools/maestro/cli-$($config.version)/maestro"
if (-not (Test-Path -LiteralPath (Join-Path $installation 'lib'))) { throw 'Run .\scripts\setup-maestro.ps1 first. See docs/testing/maestro.md.' }
$java = Get-StarbornJava
$isTest = $MaestroArgs -contains 'test'
$informational = @($MaestroArgs | Where-Object { $_ -in @('--help','-h','--version','-v') }).Count -gt 0
$runRoot = Join-Path $maestroRepoRoot ('playtests/artifacts/maestro/' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0,8))
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
$stateRoot = Join-Path $maestroRepoRoot '.maestro-home'
$runTemp = Join-Path $stateRoot ('tmp/' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $runTemp | Out-Null
$cliArgs = @($MaestroArgs)
if ($isTest -and -not $informational) {
    foreach ($argument in $cliArgs) {
        if ($argument -match '(?i)APP_ID=([^\s]+)' -and $Matches[1] -ne $config.appId) { throw "Tests must target $($config.appId), not the campaign app." }
    }
    $cliArgs += @('-e', "APP_ID=$($config.appId)", '--format', 'JUNIT', '--output', (Join-Path $runRoot 'report.xml'),
        '--debug-output', $runRoot, '--test-output-dir', $runRoot)
    $sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Android/Sdk' }
    $adb = Join-Path $sdkRoot 'platform-tools/adb.exe'
    if (-not (Test-Path -LiteralPath $adb)) { throw 'Install Android SDK platform-tools or set ANDROID_HOME.' }
    $device = $env:ANDROID_SERIAL
    for ($i = 0; $i -lt $MaestroArgs.Count; $i++) {
        if ($MaestroArgs[$i] -in @('--device','--udid')) { $device = $MaestroArgs[$i+1] }
        elseif ($MaestroArgs[$i] -match '^--(?:device|udid)=(.+)$') { $device = $Matches[1] }
    }
    if (-not $device) {
        $devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
        if ($devices.Count -ne 1) { throw 'Connect one test device/emulator, or supply --device <serial>.' }
        $device = $devices[0]
    }
    $installed = & $adb -s $device shell pm path $config.appId 2>&1
    if ($LASTEXITCODE -ne 0 -or -not ($installed -match '^package:')) { throw "Install the isolated app: .\scripts\setup-maestro.ps1 -InstallPlaytest -Device $device" }
    & $adb -s $device shell input keyevent KEYCODE_WAKEUP | Out-Null
    & $adb -s $device shell wm dismiss-keyguard | Out-Null
    if (-not (@($MaestroArgs | Where-Object { $_ -match '^--(?:device|udid)(=|$)' }).Count)) {
        $cliArgs = @('--device', $device) + $cliArgs
    }
}
# Direct Java invocation preserves arguments without cmd.exe reinterpretation.
# All environment overrides are child-only; never change HOME/USERPROFILE.
$start = New-Object System.Diagnostics.ProcessStartInfo
$start.FileName = $java
$start.WorkingDirectory = $maestroRepoRoot
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$javaArgs = @("-Duser.home=$stateRoot", "-Djava.io.tmpdir=$runTemp", '-classpath', (Join-Path $installation 'lib/*'), 'maestro.cli.AppKt') + $cliArgs
$start.Arguments = ($javaArgs | ForEach-Object { ConvertTo-StarbornNativeArgument $_ }) -join ' '
$start.EnvironmentVariables['MAESTRO_CLI_NO_ANALYTICS'] = 'true'
$start.EnvironmentVariables['MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED'] = 'true'
$start.EnvironmentVariables['MAESTRO_DISABLE_UPDATE_CHECK'] = 'true'
$start.EnvironmentVariables['TEMP'] = $runTemp
$start.EnvironmentVariables['TMP'] = $runTemp
$start.EnvironmentVariables['LOCALAPPDATA'] = Join-Path $stateRoot 'AppData/Local'
$start.EnvironmentVariables['APPDATA'] = Join-Path $stateRoot 'AppData/Roaming'
New-Item -ItemType Directory -Force -Path (Join-Path $stateRoot 'AppData/Local/mobile_dev/maestro/Logs'), (Join-Path $stateRoot 'AppData/Roaming') | Out-Null
if ($isTest -and -not $informational) {
    $start.EnvironmentVariables['ANDROID_HOME'] = $sdkRoot
    $start.EnvironmentVariables['PATH'] = (Split-Path -Parent $adb) + ';' + $env:PATH
}
$process = New-Object System.Diagnostics.Process
$process.StartInfo = $start
try {
    [void]$process.Start()
    $stdoutTask = $process.StandardOutput.ReadToEndAsync()
    $stderrTask = $process.StandardError.ReadToEndAsync()
    $timeoutSeconds = if ($env:STARBORN_MAESTRO_TIMEOUT_SECONDS) { [int]$env:STARBORN_MAESTRO_TIMEOUT_SECONDS } else { 900 }
    $timedOut = -not $process.WaitForExit($timeoutSeconds * 1000)
    if ($timedOut) { $process.Kill(); $process.WaitForExit() }
    $stdout = $stdoutTask.GetAwaiter().GetResult()
    $stderr = $stderrTask.GetAwaiter().GetResult()
    $stdout | Out-File -LiteralPath (Join-Path $runRoot 'stdout.log') -Encoding utf8
    $stderr | Out-File -LiteralPath (Join-Path $runRoot 'stderr.log') -Encoding utf8
    Write-Output $stdout
    if ($stderr) { Write-Output $stderr }
    $result = $process.ExitCode
    if ($timedOut) { $result = 1; Write-Warning "Maestro exceeded $timeoutSeconds seconds. See the run artifacts." }
    if ($isTest -and -not $informational -and $result -eq 0) {
        $report = Join-Path $runRoot 'report.xml'
        if (-not (Test-Path -LiteralPath $report)) { $result = 1; Write-Warning 'No JUnit report: this is not a passing run.' }
        else {
            [xml]$xml = Get-Content -LiteralPath $report -Raw
            if ($xml.SelectNodes('//testcase').Count -eq 0 -or $xml.SelectNodes('//failure|//error').Count -gt 0 -or
                $xml.SelectNodes('//testcase').Count -eq $xml.SelectNodes('//testcase/skipped').Count) { $result = 1 }
        }
    }
    Write-Host "Maestro artifacts: $runRoot"
    exit $result
} finally {
    $process.Dispose()
    # Keep native temp files in ignored .maestro-home; Windows may still map them.
}
