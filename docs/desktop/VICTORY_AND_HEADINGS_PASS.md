# Victory and headings presentation pass

## Changes

- Desktop victory presentation follows Android's `VictorySpoilsDialog`: original Orbitron combat names, orange/warm dark surfaces, trophy header, reward tiles, loot, and a separate level-up stage with original portraits and skill chips.
- The battlefield remains visible beneath the overlay. Victory feedback has 900ms before spoils. Shared CombatController already starts victory music; desktop adds no duplicate trigger.
- Reward data is captured for presentation after the shared runtime resolves the encounter. Level-up summaries are consumed once. Dialog actions never grant items, XP, AP, or credits. Only the final stage calls the existing exit callback/fade.
- Enter release advances; Escape does not dismiss. A short stage settling interval blocks double activation, and completion is guarded. The scrim blocks background taps without merging reward/list accessibility semantics.
- The panel is capped at 470dp with scrollable rewards/level-ups; its header and Continue button remain visible in short windows.
- The command waiting prompt is an open, bold 24sp heading with an icon and accent rule. Ready-state accent pulses respect reduced-motion and high-contrast settings.
- Room headings use the existing Oxanium heading font at 36sp ExtraBold/42sp line height, or 30sp/36sp in compact drawers. Titles wrap, retain dark-room concealment rules, and stay fixed above the independently scrolling description.

## Validation

Nine focused UI checks pass. They cover the two-stage sequence, empty/positive rewards, short-window scrolling, keyboard progression, duplicate completion, no inventory changes from presentation, bold/wrapping typography, description scrolling, and authored dark-title visibility. An existing completed-encounter test was updated to current button labels and the new spoils title.

Offscreen screenshots cover spoils, multiple level-ups, empty/high-contrast rewards, a 500x320 viewport, full combat waiting state, Nova's Bunk, Jed's Bunk, and Night Market. Artifacts are under `desktopApp/build/reports/desktop/screenshots/victory-headings`.

The broader older exploration panel test class was also attempted: three checks fail against direction labels/disabled semantics and the old description ScrollBy selector. Those assertions were not changed in this pass. This is not a claim of a clean full test suite.

Native fullscreen playthrough and live audio listening were not performed. Renderer audio device warnings do not affect the presentation screenshots. The regular portable Windows folder was successfully updated after the game closed and released its `icudtl.dat` lock. Delivery is under `desktopApp/build/distributions/Starborn-Windows-portable/Starborn`. No ZIP.
