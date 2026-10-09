# Focused desktop UI verification

For the later cinematic/action-text fixes and broader remaining-gap review, see
[Desktop versus Android audit](DESKTOP_ANDROID_PARITY_AUDIT.md). The dialogue
preview described below used the older desktop component; it is not evidence
for the newly restored live Android-style dialogue renderer.

## Results

The combined run passed ten desktop tests and seven shared-runtime tests.
The updated connected-map and dialogue-choice checks also passed afterwards.
The portable build completed, and its launcher passed from outside the checkout:
1,327 assets, 477 rooms, four character definitions, bundled Java 21.0.12.1,
exit code zero.

## Exercised behavior

- Authored opening actions, quest milestone, exit travel and room fade completion.
- F5 quick-save and manual save/reload inventory contents.
- Inventory search; actual medkit consumption and healing.
- Owned-but-locked equipment restrictions, unlocked weapon equipping and equipped indicator.
- Quest tracking/untracking and menu navigation.
- Connected discovered-room map selection without moving the party or revealing
  unknown room names.
- Combat keyboard actor selection, command dialogs, target selection/confirmation,
  offscreen target scrolling, victory and defeat acknowledgement callbacks.
- Long dialogue, portrait rendering, Continue and response-choice callbacks.
- Save archive opening, legacy migration and packaged asset access.

Compose screenshots were reviewed at the default 1024×768 test surface and with
800×600 content constraints. These are rendered UI checks, not a live OS window
resize or high-DPI test. Combat outcomes use isolated fixtures (including elevated
party level for the victory route); this does not validate campaign balance.

## Fixes from the checks

- Exploration exit panel could shrink to about 60 pixels and wrap direction
  labels vertically. Width now uses viewport dimensions directly.
- Custom dark panels inherited black text. The theme now supplies a light default
  content color and consistent dark card colors.
- Crowded combat formations clipped status panels. Figure height now adapts to
  the available formation height; selected offscreen figures scroll into view.
- The action gauge used redundant interpolation over its frequent timer updates.
  It now renders the authoritative meter directly; HP still animates.
- Equipment offered an enabled equip action before a weapon/armor was unlocked.
  The UI now exposes and enforces that restriction without changing shared rules.
- Test waits now advance the Compose clock for fades and animation-dependent
  presentation. Lazy-list tests scroll to items before accessing them.

## Evidence

- Combined log: `tmp/desktop-ui-release-check.log`
- Follow-up log: `tmp/desktop-connected-map-dialogue.log`
- Packaged check: `tmp/desktop-ui-packaged-check.txt`
- Screenshots: `desktopApp/build/reports/desktop/screenshots/`

## Outstanding live checks

Windows Computer Use failed to connect its native pipe (OS error 2), including
retry and kernel-reset recovery. No live-window inputs were performed. Fullscreen,
focus switching, OS resizing and actual high DPI remain unverified. JavaSound
has no usable Clip device in this environment, so audible playback remains
unverified. Full manual campaign coverage and installer lifecycle checks remain
separate release requirements.
