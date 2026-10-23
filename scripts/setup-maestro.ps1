param([switch]$InstallPlaytest, [string]$Device, [string]$ArchivePath)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'maestro-common.ps1')
$maestroRepoRoot = Split-Path -Parent $PSScriptRoot
$config = Get-StarbornMaestroConfig
$java = Get-StarbornJava
$target = [IO.Path]::GetFullPath((Join-Path $maestroRepoRoot "tools/maestro/cli-$($config.version)"))
$allowed = [IO.Path]::GetFullPath((Join-Path $maestroRepoRoot 'tools/maestro')) + [IO.Path]::DirectorySeparatorChar
if (-not $target.StartsWith($allowed, [StringComparison]::OrdinalIgnoreCase)) { throw 'Invalid tool installation path.' }
if (-not (Test-Path -LiteralPath (Join-Path $target 'maestro/lib'))) {
    if (Test-Path -LiteralPath $target) { throw "Incomplete installation at $target. Move it aside before retrying." }
    if (-not $ArchivePath) {
        $cache = Join-Path $maestroRepoRoot '.maestro-home/downloads'
        New-Item -ItemType Directory -Force -Path $cache | Out-Null
        $ArchivePath = Join-Path $cache "maestro-$($config.version).zip"
        if (-not (Test-Path -LiteralPath $ArchivePath)) {
            $partial = "$ArchivePath.partial"
            $curl = Get-Command curl.exe -ErrorAction SilentlyContinue
            if ($curl) {
                & $curl.Source -L --fail --retry 2 --connect-timeout 20 --output $partial $config.archiveUrl
                if ($LASTEXITCODE -ne 0) { throw 'Maestro download failed; rerun setup to retry.' }
            } else {
                $previousProgress = $ProgressPreference
                try { $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri $config.archiveUrl -OutFile $partial }
                finally { $ProgressPreference = $previousProgress }
            }
            if ((Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash -ne $config.sha256) { throw 'Downloaded Maestro checksum does not match the pinned release.' }
            Move-Item -LiteralPath $partial -Destination $ArchivePath
        }
    }
    if ((Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA256).Hash -ne $config.sha256) { throw "Maestro archive checksum does not match pinned version $($config.version)." }
    Expand-Archive -LiteralPath $ArchivePath -DestinationPath $target
}
$version = (& $java -classpath (Join-Path $target 'maestro/lib/*') maestro.cli.AppKt --version | Out-String).Trim()
if ($LASTEXITCODE -ne 0 -or $version -ne $config.version) { throw "Expected Maestro $($config.version); got: $version" }
Write-Host "Maestro $version is ready."
if ($InstallPlaytest) {
    $sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Android/Sdk' }
    $adb = Join-Path $sdkRoot 'platform-tools/adb.exe'
    if (-not $Device) { $Device = $env:ANDROID_SERIAL }
    if (-not $Device) {
        $devices = @(& $adb devices | Where-Object { $_ -match '^\S+\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
        if ($devices.Count -ne 1) { throw 'Connect one device, or supply -Device <serial>.' }
        $Device = $devices[0]
    }
    $booted = (& $adb -s $Device shell getprop sys.boot_completed 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or $booted -ne '1') { throw "Wait for Android to finish booting on $Device, then rerun setup." }
    $previousSerial = $env:ANDROID_SERIAL
    $previousJava = $env:JAVA_HOME
    try {
        if ($Device) { $env:ANDROID_SERIAL = $Device }
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
        Push-Location $maestroRepoRoot
        try { & .\gradlew.bat :app:installPlaytest --console=plain; if ($LASTEXITCODE -ne 0) { throw 'Playtest installation failed.' } }
        finally { Pop-Location }
    } finally { $env:ANDROID_SERIAL = $previousSerial; $env:JAVA_HOME = $previousJava }
}
Write-Host 'Next: .\scripts\maestro.ps1 test .\playtests\maestro\suite_smoke.yaml'
