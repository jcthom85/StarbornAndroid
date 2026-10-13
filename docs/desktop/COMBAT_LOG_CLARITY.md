# Combat log clarity

Android and desktop now share readable status messages. Generated buff IDs no longer appear as labels, and buff amounts are no longer mistaken for stack counts.

- `buff_accuracy x8` becomes `Accuracy +8% (4 turns for this character)` in the desktop tactical feed.
- Weapon crit bonuses explicitly say `Weapon crit chance`.
- Negative modifiers keep their sign and percentage units.
- Actual statuses retain authored display names and show genuine stack counts.
- Shared combat history identifies the affected character; expiry messages use readable effect names.
- Android floating labels show the effect amount without the long duration clause. Full durations remain in history and effect details.

Buff application log entries capture the effective duration when applied, including a refreshed buff that retains a longer existing duration. Presentation does not infer historical duration from later combat state.

Validation: focused Android formatting, snack contract, status ticking, and combat viewmodel tests; desktop combat UI and log rollover tests. No combat balance or turn timing changes.
