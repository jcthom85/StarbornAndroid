param(
    [switch] $Json
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$planPath = Join-Path $repoRoot "playtests\starborn-test-plan.json"
$scenarioPath = Join-Path $repoRoot "app\src\main\java\com\example\starborn\feature\mainmenu\DebugScenario.kt"

if (-not (Test-Path -LiteralPath $planPath)) {
    throw "Missing test plan: $planPath"
}

$plan = Get-Content -Raw -LiteralPath $planPath | ConvertFrom-Json
$errors = New-Object System.Collections.Generic.List[string]
$warnings = New-Object System.Collections.Generic.List[string]

$flowIds = @{}
foreach ($flow in $plan.flows) {
    if ($flowIds.ContainsKey($flow.id)) {
        $errors.Add("Duplicate flow id: $($flow.id)")
    } else {
        $flowIds[$flow.id] = $flow
    }
    $flowPath = Join-Path $repoRoot ($flow.file -replace "/", "\")
    if (-not (Test-Path -LiteralPath $flowPath)) {
        $errors.Add("Registered flow file is missing: $($flow.id) -> $($flow.file)")
    }
    if ([string]::IsNullOrWhiteSpace($flow.qualityQuestion)) {
        $errors.Add("Registered flow lacks qualityQuestion: $($flow.id)")
    }
}

foreach ($suite in $plan.suites) {
    foreach ($flowId in @($suite.flowIds)) {
        if ($flowId -eq "*") { continue }
        if (-not $flowIds.ContainsKey($flowId)) {
            $errors.Add("Suite '$($suite.id)' references unknown flow '$flowId'")
        }
    }
}

$yamlFlows = Get-ChildItem -LiteralPath (Join-Path $repoRoot "playtests\maestro") -Filter "*.yaml" -File |
    ForEach-Object { [System.IO.Path]::GetFileNameWithoutExtension($_.Name) }

foreach ($yamlFlow in $yamlFlows) {
    if (-not $flowIds.ContainsKey($yamlFlow)) {
        $errors.Add("Stable Maestro YAML is not registered: $yamlFlow")
    }
}

foreach ($flowId in $flowIds.Keys) {
    if ($flowId -notin $yamlFlows) {
        $errors.Add("Registered flow has no matching stable Maestro YAML: $flowId")
    }
}

if (Test-Path -LiteralPath $scenarioPath) {
    $scenarioText = Get-Content -Raw -LiteralPath $scenarioPath
    $scenarioIds = @(
        [regex]::Matches($scenarioText, '(?:scenario|hub|hubScenario)\("([^"]+)"') |
            ForEach-Object { $_.Groups[1].Value } |
            Sort-Object -Unique
    )
    $coveredScenarios = @{}
    foreach ($flow in $plan.flows) {
        if (-not [string]::IsNullOrWhiteSpace($flow.debugScenario)) {
            $coveredScenarios[$flow.debugScenario] = $true
        }
    }
    foreach ($checkpoint in $plan.qualityCheckpoints) {
        if ($checkpoint.type -eq "debugScenario") {
            $coveredScenarios[$checkpoint.id] = $true
        }
    }
    foreach ($scenarioId in $scenarioIds) {
        if (-not $coveredScenarios.ContainsKey($scenarioId)) {
            $errors.Add("Debug scenario lacks flow or quality checkpoint coverage: $scenarioId")
        }
    }
} else {
    $warnings.Add("DebugScenario.kt not found; skipped debug scenario coverage.")
}

$result = [PSCustomObject]@{
    ok = $errors.Count -eq 0
    errors = @($errors)
    warnings = @($warnings)
    registeredFlows = $flowIds.Count
    stableMaestroFlows = @($yamlFlows).Count
}

if ($Json) {
    $result | ConvertTo-Json -Depth 5
} else {
    Write-Host "Starborn test coverage"
    Write-Host "Registered flows: $($result.registeredFlows)"
    Write-Host "Stable Maestro YAML flows: $($result.stableMaestroFlows)"
    foreach ($warning in $warnings) { Write-Warning $warning }
    foreach ($errorItem in $errors) { Write-Error $errorItem -ErrorAction Continue }
}

if ($errors.Count -gt 0) {
    exit 1
}

