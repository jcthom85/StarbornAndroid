# Narrative hierarchy and independent exploration HUD

The left and right sides are layout regions around the portrait artwork. They no longer have matching full-height enclosing cards.

## Narrative

- A content-sized card with a warm leading accent, 28sp semibold title, and 17sp/26sp prose is the primary reading element.
- It sits toward the artwork, has a maximum width of 400dp, and uses 22dp internal padding.
- Long descriptions scroll inside a card capped at 60% of available height. The title stays visible.
- Contextual actions, services, loot, and special exits have a separate content-sized group below it. Feedback follows outside the narrative card; threats use warm red.
- Optional room-text typography parameters preserve existing defaults for other callers.

## HUD

- The minimap is an independent card at the top right with a 140dp map area.
- Objective and party cards are separate, separated by 20dp of exposed reflected background. They share a scrollable region beneath the anchored minimap.
- The right modules are limited to 300dp width and align to the outer right margin. Party portraits are 36dp and health bars are 4dp tall.
- Menu is a compact outlined button anchored at the bottom; optional hub return sits above it.
- Missing modules reserve no empty card. Darkness hides the minimap and makes the remaining modules move upward.
- Narrow windows retain Room/Status toggles, separate content surfaces, and bottom navigation.

Gameplay callbacks, action deduplication, image geometry, darkness rules, keyboard shortcuts, focus restoration, and blocking overlays are preserved.

Follow-up: locked directional controls are disabled and keyboard movement filters blocked routes before calling travel, matching Android's navigation UI. This avoids triggering the generic blocked-lock cinematic. The bunk rest action is available as “Rest here” inside Menu instead of a standalone “bunk” button beneath the narrative.

Map/quest follow-up: both desktop room maps now render higher world Y values above lower values, matching Android. The quest HUD reads the authored title and main summary from the quest repository; Journal displays the main summary, full description, and flavor from `quests.json`. Tutorial task/stage text is no longer substituted for this narrative writing. Quest stages, task completion, and save data remain intact.

## Verification

The selected regressions cover four aspect ratios, card height limits, separated minimap/objective bounds, stationary minimap during party scrolling, stationary title during narrative scrolling, long and short descriptions, fallback actions, locked controls, darkness, opening progression, dialogue, cinematics, and portrait backdrops.

All 14 selected checks passed with no failures or skips.

The rebuilt application passed its packaged smoke check from outside the repository: 1,327 assets, 477 rooms, 4 characters, bundled Java 21.0.12.1, exit code 0.

The normal portable-folder update encountered Windows file locks from two running copies. The complete updated application is also available at `desktopApp/build/distributions/Starborn-HUD-preview/Starborn/Starborn.exe`. Updating the usual folder requires closing its running copies and rerunning `packagePortableWindows`.

Screenshots are in `desktopApp/build/reports/desktop/screenshots/three-panel/` and the live opening screenshots in its parent folder. Local playtesting is still needed for native monitor DPI and audible output.
