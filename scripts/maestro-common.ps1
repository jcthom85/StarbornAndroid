$ErrorActionPreference = 'Stop'

function Get-StarbornMaestroConfig {
    $maestroRepoRoot = Split-Path -Parent $PSScriptRoot
    Get-Content -LiteralPath (Join-Path $maestroRepoRoot 'playtests/maestro/toolchain.json') -Raw | ConvertFrom-Json
}

function Get-StarbornJava {
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) { return (Join-Path $env:JAVA_HOME 'bin/java.exe') }
    $studioJava = 'C:/Program Files/Android/Android Studio/jbr/bin/java.exe'
    if (Test-Path -LiteralPath $studioJava) { return $studioJava }
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($javaCommand) { return $javaCommand.Source }
    throw 'Install Android Studio (including its JBR), or set JAVA_HOME to a Java 17+ installation.'
}

function ConvertTo-StarbornNativeArgument([string]$Value) {
    if ($Value -notmatch '[\s"]' -and $Value.Length -gt 0) { return $Value }
    $quoted = [regex]::Replace($Value, '(\\*)"', '$1$1\"')
    $quoted = [regex]::Replace($quoted, '(\\+)$', '$1$1')
    return '"' + $quoted + '"'
}
