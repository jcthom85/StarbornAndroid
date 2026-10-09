# Three-area desktop combat

- Portrait battlefield preserves the room image aspect ratio with reflected blur on each side.
- Enemy formation is above the party; party uses two columns, original combat art, compact blue readiness/red HP bars, and circular selection rings. Enemy labels and readiness/HP/stability bars are above their sprites.
- Sprite-bound feedback, lunges, recoil, target eligibility and shared combat actions remain connected. Timed prompts are bounded to the central battlefield.
- Right command area uses the room combat theme accent and selected character portrait/HP/readiness/momentum/status/charge/buff information. Skills/items are inline, with descriptions, cooldowns, availability and Back/Escape.
- Left area contains location/encounter heading, announcements, focused enemy information, tutorial guidance and four recent battle events. Full log retains its pause behavior.
- Smaller windows use toggled Battle/Commands overlays; battlefield ratio remains fixed.
- Fixed an existing focus request made before the combat UI was attached.

Validation: Kotlin compilation succeeded; offscreen renders reviewed for one enemy, two enemies/full party, Iron Warden, narrow window, inline skills/items, and attack targeting. Screenshot runner exercised character clicks, command keyboard shortcuts and Escape cancellation. Native audio, timed prompt/victory/defeat/tutorial playthrough and larger composite boss formations remain to be checked.

Screenshots: desktopApp/build/reports/desktop/screenshots/combat-three.
