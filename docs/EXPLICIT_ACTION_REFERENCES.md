# Explicit room action references

Room descriptions may use `[action:authored name|display text]` or
`[action:authored name]`. The name resolves case-insensitively to exactly one
currently supplied inline action. This format changes no save fields or action keys.

Example: `Read [action:crew datapad|the battered tablet].`

The renderer strips the marker and links the display text. Existing action lock
hints still apply. Hidden, unknown, duplicate-name or non-inline service targets
render as plain display text; their span is reserved so another action cannot
accidentally claim it. Explicit references take priority over automatic matching
for that action. Multiple explicit references may target the same action.

Existing plain descriptions retain literal name matching. Existing `[npc:Name]`
markers remain supported. Avoid `|` or `]` inside names/labels; escaping and nested
markers are not supported. Resolve duplicate authored names before linking them.

The debug enemy-party room pilots a custom `layout manifest` label; Astra cargo
bay pilots `repair bench` targeting `repair workbench`. Strict validation rejects
malformed/nested markers, empty labels, ambiguous targets and service actions. Run
`validate_explicit_action_references.ps1 -Strict` to check authored targets and
`test_explicit_action_references.ps1` for shared text-handling checks. Maestro
validation uses rendered marker labels; the static audit recognizes explicit
targets and excludes reserved marker spans from automatic matching. Device
verification of wrapped links, accessibility labels and locked interactions
passed in isolated Compose tests on the Android 17 Medium Phone emulator at 2x
font scale and 240 dp width. Full room-flow and physical-phone acceptance remain
open. The Astra cargo pilot also passes a production-ViewModel integration test:
the rendered link dispatches the authored workbench inspection and creates its
narration prompt. Entry navigation and full-screen dismissal are not covered.
Run instrumentation with a fresh Gradle process (`--no-daemon`)
and an awake/unlocked emulator; an older restricted daemon caused UTP access errors.
