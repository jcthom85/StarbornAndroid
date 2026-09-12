# Shared authored-text handling for the runtime's non-nested marker syntax.
function Get-ExplicitReferenceErrors([string]$Text, $Actions) {
    $validPattern = '\[action:([^\[\]|]+)(?:\|([^\[\]|]+))?]'
    $remaining = [regex]::Replace($Text, $validPattern, '', 'IgnoreCase')
    if ($remaining -match '(?i)\[action\b') { 'Malformed action marker' }
    foreach ($match in [regex]::Matches($Text, $validPattern, 'IgnoreCase')) {
        $name = $match.Groups[1].Value.Trim()
        $label = if ($match.Groups[2].Success) { $match.Groups[2].Value.Trim() } else { $name }
        $targets = @($Actions | Where-Object { $_.name -ieq $name })
        if (-not $name -or -not $label -or $targets.Count -ne 1) {
            "Invalid or ambiguous target: $($match.Value)"
        } elseif ($targets[0].type -match '^(shop|tinkering|firstaid|first_aid|rest_stop|reststop|rest|fish|fishing|arcade.*)$') {
            "Non-inline target: $name"
        }
    }
}

function ConvertTo-RenderedReferenceText([string]$Text) {
    return [regex]::Replace($Text, '\[(npc|action):([^\]]+)]', {
        param($match)
        $body = $match.Groups[2].Value
        if ($match.Groups[1].Value -ieq 'npc') { return $body.Trim() }
        return ($body -split '\|', 2)[-1].Trim()
    }, [Text.RegularExpressions.RegexOptions]::IgnoreCase)
}

function Test-ExplicitActionReference([string]$Text, [string]$Name) {
    foreach ($match in [regex]::Matches($Text, '\[action:([^\]]+)]', 'IgnoreCase')) {
        if (($match.Groups[1].Value -split '\|', 2)[0].Trim() -ieq $Name) { return $true }
    }
    return $false
}
