# Exploration interactions and combat return ? October 15, 2026

## Player behavior

Room objects are linked in the description. Pure inspections show their authored text; services offer an explicit activity button (Rest, Open workbench, Browse shop, Fish) before starting. The Actions chooser also exposes room objects, including ones absent from the prose. NPC portraits, loot controls and directional exits remain visible. The desktop shortcut legend is hidden; keyboard bindings and Controls remain available.

Mess Hall tables are scenery, with recovery at Nova?s bunk. Inspection labels use sentence capitalization rather than raw lowercase names.

## Combat return repair

Both platforms now deliver the complete combat result to one shared victory handler. Static formations are removed and their cleared state is saved there; moving parties retain their own persisted identities. Desktop previously missed Android?s separate static-enemy cleanup callback. That split callback has been removed.

Saves with the Loader victory flag/event recover the Scrap Yard clearance on load, without another battle or reward. Older unrelated kills without any recorded clearance cannot be inferred safely.

A blocked exit now opens one specific route-blocked prompt. The generic blocked cinematic no longer also starts. Android and desktop both render the shared prompt.

## Validation

All 684 app-hosted unit tests pass, including the real-combat legal campaign route through Worlds 1?6 and its save/reload checkpoints. All 17 focused desktop checks pass (ExplorationReturn, ExplorationPanels, PcInteraction and OpeningAcceptance). The clean Mess Hall panel was also visually inspected. The debug APK builds; this pass did not install or restart the emulator. Regression cases cover Loader victory and north exit, save/reload, old Loader save recovery, other static formations, retreat, moving-party identity, activity cancellation and confirmation, and the hidden shortcut legend.


Commands and evidence:

- `:app:testDebugUnitTest`: 684 tests, zero failures/skips. XML: `app/build/test-results/testDebugUnitTest/`.
- `:desktopApp:test --tests '*DesktopExplorationReturnTest' --tests '*DesktopExplorationPanelsTest' --tests '*DesktopPcInteractionTest' --tests '*DesktopOpeningAcceptanceTest' -I tmp/game-evaluation.init.gradle`: 17 tests, zero failures/skips. XML: `tmp/game-evaluation-results/desktopApp/test/xml/`.
- `:app:assembleDebug`: debug APK at `app/build/outputs/apk/debug/app-debug.apk`.
- Screenshot: `desktopApp/build/reports/desktop/screenshots/mess-hall-clean.png`. The panel fixture omits the background artwork.

The desktop return regressions submit typed combat-result payloads to the shared handler. The legal-route suite exercises actual battles; desktop victory UI handoff is additionally verified by compilation and its use of the combat controller?s immutable encounter identity. Mobile visual acceptance remains manual.
