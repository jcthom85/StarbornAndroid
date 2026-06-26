param(
    [switch]$StrictCatalog
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root "app/src/main/assets"
$rawDir = Join-Path $root "app/src/main/res/raw"
$minAudioBytes = 4096

function Read-AssetJson($name) {
    Get-Content (Join-Path $assets $name) -Raw | ConvertFrom-Json
}

function Normalize-CueId($cueId) {
    if ([string]::IsNullOrWhiteSpace($cueId)) { return $null }
    return ([string]$cueId).Trim().ToLowerInvariant().Replace("-", "_")
}

function Read-FileHeader($path, [int]$count) {
    if (-not (Test-Path -LiteralPath $path)) { return $null }
    $stream = [System.IO.File]::OpenRead($path)
    try {
        $buffer = [byte[]]::new($count)
        $read = $stream.Read($buffer, 0, $count)
        if ($read -lt $count) { return $null }
        return ,$buffer
    } finally {
        $stream.Dispose()
    }
}

function Get-RawAudioFile($cueId) {
    $normalized = Normalize-CueId $cueId
    if (-not $normalized) { return $null }
    return Get-ChildItem -LiteralPath $rawDir -File -ErrorAction SilentlyContinue | Where-Object { $_.BaseName -eq $normalized } | Select-Object -First 1
}

function Has-RawAudio($cueId) {
    return $null -ne (Get-RawAudioFile $cueId)
}

function Get-RawAudioIssue($file) {
    if ($file.Length -lt $minAudioBytes) {
        return "$($file.Name) is only $($file.Length) bytes"
    }
    $header = Read-FileHeader $file.FullName 12
    if ($null -eq $header) {
        return "$($file.Name) is too short to inspect"
    }
    if ($file.Extension -ieq ".mp3") {
        $hasId3 = [System.Text.Encoding]::ASCII.GetString($header, 0, 3) -eq "ID3"
        $hasFrameSync = $header[0] -eq 0xFF -and (($header[1] -band 0xE0) -eq 0xE0)
        if (-not ($hasId3 -or $hasFrameSync)) {
            return "$($file.Name) is not a recognizable MP3"
        }
    } elseif ($file.Extension -ieq ".wav") {
        $riff = [System.Text.Encoding]::ASCII.GetString($header, 0, 4)
        $wave = [System.Text.Encoding]::ASCII.GetString($header, 8, 4)
        if ($riff -ne "RIFF" -or $wave -ne "WAVE") {
            return "$($file.Name) is not a recognizable WAV"
        }
    } else {
        return "$($file.Name) uses unsupported extension '$($file.Extension)'"
    }
    return $null
}

function Split-CueLayers($raw) {
    if ([string]::IsNullOrWhiteSpace($raw)) { return @() }
    return @(([string]$raw) -split '[+,;]' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

$catalog = Read-AssetJson "audio_catalog.json"
$bindings = Read-AssetJson "audio_bindings.json"

$catalogIds = New-Object System.Collections.Generic.HashSet[string]
foreach ($track in @($catalog.tracks)) {
    $normalized = Normalize-CueId $track.id
    if ($normalized) { [void]$catalogIds.Add($normalized) }
}
foreach ($cue in @($catalog.cues)) {
    $normalized = Normalize-CueId $cue.id
    if ($normalized) { [void]$catalogIds.Add($normalized) }
}

$references = New-Object System.Collections.Generic.List[object]

function Add-Reference($source, $path, $cueId, $kind) {
    foreach ($part in Split-CueLayers $cueId) {
        $references.Add([pscustomobject]@{
            Source = $source
            Path = $path
            CueId = $part
            Kind = $kind
        })
    }
}

foreach ($sectionName in @("music", "ambience", "weather", "ui", "battle")) {
    $section = $bindings.$sectionName
    if ($null -eq $section) { continue }
    foreach ($entry in $section.PSObject.Properties) {
        Add-Reference "audio_bindings.json" "$sectionName.$($entry.Name)" $entry.Value "binding"
    }
}

$audioKeys = New-Object System.Collections.Generic.HashSet[string]
foreach ($key in @("voice", "vo_cue", "voice_cue", "voicecue", "audio_cue", "audiocue", "audio_cue_id", "audiocueid", "cue_id")) {
    [void]$audioKeys.Add($key)
}

function Visit-Json($value, $source, $path) {
    if ($null -eq $value) { return }
    if ($value -is [System.Array]) {
        for ($i = 0; $i -lt $value.Count; $i++) {
            Visit-Json $value[$i] $source "$path.$i"
        }
        return
    }
    if ($value -is [pscustomobject]) {
        foreach ($prop in $value.PSObject.Properties) {
            $key = $prop.Name.ToLowerInvariant()
            $childPath = if ([string]::IsNullOrWhiteSpace($path)) { $prop.Name } else { "$path.$($prop.Name)" }
            if ($audioKeys.Contains($key) -and $prop.Value -is [string]) {
                Add-Reference $source $childPath $prop.Value "json"
            }
            Visit-Json $prop.Value $source $childPath
        }
    }
}

foreach ($file in Get-ChildItem -LiteralPath $assets -Filter "*.json" -File) {
    if ($file.Name -in @("audio_bindings.json", "audio_catalog.json")) { continue }
    try {
        $json = Get-Content $file.FullName -Raw | ConvertFrom-Json
        Visit-Json $json $file.Name ""
    } catch {
        Write-Warning "Skipping unreadable JSON '$($file.Name)': $($_.Exception.Message)"
    }
}

$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

foreach ($ref in $references) {
    $normalized = Normalize-CueId $ref.CueId
    if (-not $normalized) { continue }
    $audioFile = Get-RawAudioFile $normalized
    if ($null -eq $audioFile) {
        $errors.Add("$($ref.Source) $($ref.Path) references missing raw audio cue '$($ref.CueId)'.")
    } else {
        $issue = Get-RawAudioIssue $audioFile
        if ($issue) {
            $errors.Add("$($ref.Source) $($ref.Path) references invalid raw audio cue '$($ref.CueId)': $issue.")
        }
    }
    if (-not $catalogIds.Contains($normalized)) {
        $warnings.Add("$($ref.Source) $($ref.Path) references cue '$($ref.CueId)' that is not listed in audio_catalog.json.")
    }
}

foreach ($warning in ($warnings | Sort-Object -Unique)) { Write-Warning $warning }
foreach ($errorMessage in ($errors | Sort-Object -Unique)) { Write-Error $errorMessage }

if ($errors.Count -gt 0 -or ($StrictCatalog -and $warnings.Count -gt 0)) {
    exit 1
}

$uniqueRefs = @($references | ForEach-Object { Normalize-CueId $_.CueId } | Where-Object { $_ } | Sort-Object -Unique)
Write-Host "Audio reference validation passed: $($uniqueRefs.Count) referenced cue(s), $($warnings.Count) catalog warning(s)."
