$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'explicit_action_references.ps1')
if ((ConvertTo-RenderedReferenceText 'Read [action:crew datapad|the tablet] with [npc:Jed].') -cne 'Read the tablet with Jed.') { throw 'Rendered labels differ' }
if ((ConvertTo-RenderedReferenceText '[action:crew datapad]') -cne 'crew datapad') { throw 'Default label differs' }
if (-not (Test-ExplicitActionReference '[ACTION:crew datapad|tablet]' 'crew datapad')) { throw 'Reference not recognized' }
if (Test-ExplicitActionReference '[action:other|crew datapad]' 'crew datapad') { throw 'Display label mistaken for target' }
Write-Output 'Explicit reference helper checks passed (4).'
$action = [pscustomobject]@{ name='crew datapad'; type='generic' }
foreach ($bad in @('[action:crew datapad', '[action:]', '[action:crew datapad|]', '[action:crew datapad|a|b]', '[action:crew datapad|[npc:Jed]]', '[action:missing]', '[action:crew datapad|   ]')) {
    if (@(Get-ExplicitReferenceErrors $bad @($action)).Count -eq 0) { throw "Accepted invalid marker: $bad" }
}
if (@(Get-ExplicitReferenceErrors '[action:crew datapad|tablet]' @($action)).Count) { throw 'Rejected valid marker' }
if (@(Get-ExplicitReferenceErrors '[action:crew datapad]' @($action, $action)).Count -eq 0) { throw 'Accepted duplicate target' }
foreach ($type in @('shop','tinkering','firstaid','first_aid','rest_stop','reststop','rest','fish','fishing','arcade_discovery')) {
    if (@(Get-ExplicitReferenceErrors '[action:crew datapad]' @([pscustomobject]@{name='crew datapad';type=$type})).Count -eq 0) { throw "Accepted service $type" }
}
Write-Output 'Explicit reference validation checks passed (19).'
