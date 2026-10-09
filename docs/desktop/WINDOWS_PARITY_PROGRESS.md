# Windows parity implementation

## Current assessment

See [Desktop versus Android audit](DESKTOP_ANDROID_PARITY_AUDIT.md) for the latest
presentation gaps and prioritized work. The earlier opening check manually
advanced cinematics and therefore did not establish cinematic presentation,
automatic timing or sound parity. Those omissions are addressed and covered by
live-route tests in the new audit.

## Delivered

- Shared JVM gameplay runtime used by Android and desktop, including exploration,
  combat, hub travel, shops, crafting, fishing and all six arcade engines.
- Desktop screens connected to authored events, progression, real inventory,
  equipment, skills, quests, tutorials and station rewards.
- Correct opening sequence, recruited party behavior and New Game Plus gate.
- Protocol saves with legacy JSON migration and retained migration backups,
  synchronized inventory, autosave and asynchronous save dialog operations.
- Desktop keyboard controls, responsive field menus, Starborn colors and
  asynchronous audio lifecycle management.
- Self-contained Windows portable ZIP, MSI and EXE build tasks, packaged game
  assets and a bundled Java runtime.

## Verification

The final focused checks passed seven shared runtime tests and five desktop
tests, including the opening UI route, packaged asset access and save migration.
The broader Android regression selection passed 169 of 170 tests.

The remaining assertion is
`FishingRefinementTest.everyAuthoredRodCanLandEveryBehaviorByReadingTheFish`:
the harmonic carbon rod finishes gentle wobble sooner than the test's six-second
minimum. The fishing fight implementation is unchanged from the original Android
source; no balance change or weakened assertion was made for this port.

The opening UI screenshot is generated at
`desktopApp/build/reports/desktop/screenshots/opening-pod-row.png`.

The final packaged Windows launcher passed its smoke check from the Windows
temporary folder outside the checkout: 1,327 packaged assets, 477 rooms and four
character definitions loaded with bundled Java 21.0.12.1; process exit code zero.
The report is `tmp/desktop-packaged-smoke.txt`.

## Remaining release evaluation

These are private builds. A complete packaged six-world playthrough, combat
presentation review, window and high-DPI checks, and installer upgrade/uninstall
checks remain. The available JavaSound environment had no usable audio Clip
device, so audible playback requires a Windows machine with working audio.

See [Windows build instructions](WINDOWS_BUILD.md) for packages, controls and
save locations. No claim of complete manual campaign validation is made.
