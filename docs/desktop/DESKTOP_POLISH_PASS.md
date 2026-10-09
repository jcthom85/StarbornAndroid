# Desktop reliability and usability pass

## Changes

- Combat explicitly requests keyboard focus and restores it after command dialogs.
- Added abilities/items shortcuts, cycling ready party members, and keyboard
  target selection and confirmation. Outcome and command dialogs block battlefield shortcuts.
- Added combatant damage/heal feedback, break and defeat labels, clearer readiness
  and enemy intention text, and borders identifying the selected actor or target.
- Command controls scroll horizontally in narrow windows; corrected combat copy.
- Exploration blocks movement during level-up, skill-tree, quest detail and
  special milestone/demo dialogs. Added the missing milestone gallery and
  BurgQuest disembark explanation.
- Removed a nonfunctional dialogue dismiss affordance. Enter advances dialogue;
  number keys choose responses.
- Reduced room-exit panel and action widths and exposed a quest objective.
- Quick-save writes, slot metadata reads and deletion run on the IO dispatcher.
- Arcade held keys release when the application loses focus or the screen closes.

## Checks performed

- Shared legal campaign route passed Worlds 1–6 to `ms_game_complete`, including
  save/reload checkpoints. This harness validates shared runtime progression;
  it is not a manual desktop campaign playthrough.
- Seven selected desktop checks passed: opening UI, three migration checks,
  authored catalog/combat setup, combat engine flow and combat visual capture.
- Reviewed the combat screenshot at the test window size. Other sizes and DPI
  settings still need interactive evaluation.
- Combat UI keyboard check passed: Tab selects a ready actor, 1 opens Attack,
  arrows change the target, and Enter confirms and closes targeting.
- Rebuilt launcher passed its smoke check outside the repository with all 1,327
  assets and 477 rooms available through bundled Java.
- Replaced a stale test's fixed count of 26 recipes with exact authored-catalog
  equality and output-item validation; the current catalog has 27 recipes.

## Still pending

Manual packaged campaign testing, full combat animation and battlefield design,
high-DPI and resize coverage, audible playback, and installer lifecycle checks.
The earlier fishing-duration regression remains separate from this pass.
