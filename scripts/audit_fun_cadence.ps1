param(
    [string]$AssetsPath = (Join-Path $PSScriptRoot "..\app\src\main\assets")
)

$ErrorActionPreference = "Stop"
$quests = Get-Content (Join-Path $AssetsPath "quests.json") -Raw | ConvertFrom-Json
$rooms = Get-Content (Join-Path $AssetsPath "rooms.json") -Raw | ConvertFrom-Json

Write-Output "QUEST DEPTH"
$quests | ForEach-Object {
    [pscustomobject]@{
        World = ($_.id -split "_")[0]
        Kind = if ($_.id -match "_mq") { "main" } else { "side" }
        Tasks = @($_.stages | ForEach-Object { $_.tasks } | Where-Object { $_ }).Count
    }
} | Group-Object World, Kind | ForEach-Object {
    [pscustomobject]@{
        Group = $_.Name
        Quests = $_.Count
        AverageTasks = [math]::Round((($_.Group | Measure-Object Tasks -Average).Average), 1)
        OneTaskQuests = @($_.Group | Where-Object Tasks -le 1).Count
    }
} | Format-Table -AutoSize

Write-Output "ROOM REACTIVITY"
$rooms | ForEach-Object {
    $world = if ($_.background_image -match "world_([1-6])") { "w$($Matches[1])" } else { "other" }
    [pscustomobject]@{
        World = $world
        Actions = if ($_.actions) { @($_.actions).Count } else { 0 }
        Generic = if ($_.actions) { @($_.actions | Where-Object type -eq "generic").Count } else { 0 }
        Reactive = if (($_.PSObject.Properties.Name -contains "description_variants") -and $_.description_variants) { 1 } else { 0 }
        EnemyRoom = if ($_.enemies -and @($_.enemies).Count -gt 0) { 1 } else { 0 }
    }
} | Group-Object World | ForEach-Object {
    [pscustomobject]@{
        World = $_.Name
        Rooms = $_.Count
        Actions = ($_.Group | Measure-Object Actions -Sum).Sum
        GenericActions = ($_.Group | Measure-Object Generic -Sum).Sum
        ReactiveRooms = ($_.Group | Measure-Object Reactive -Sum).Sum
        EnemyRooms = ($_.Group | Measure-Object EnemyRoom -Sum).Sum
    }
} | Sort-Object World | Format-Table -AutoSize
