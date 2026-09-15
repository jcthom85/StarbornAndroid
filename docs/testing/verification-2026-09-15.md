# Initial rebuild verification — 2026-09-15

Build: local debug working tree; not a Play release. Fixture revision: 1.
Device: Medium_Phone emulator, Android 17, emulator-5554. No audio/physical-device acceptance performed.

## Passed

- Debug APK and Android test APK compilation.
- 46 focused JVM tests: fixture relationship validation, cinematic cancellation, existing catalog integrity, crafting service/viewmodel, prompt handling, and session persistence.
- All 97 rebuilt scenarios launch, validate relationships, and execute in isolated test persistence (`DebugTestSessionInstrumentedTest` passed across all 97 entries on Android 17 emulator):
  - 1 Opening scenario (Wake Up Call / New Game).
  - 3 Recipe / save recovery workbenches.
  - 6 Arcade cabinet workbenches.
  - 6 Fishing zone workbenches with live gear and room-action verification.
  - 2 All-recipes Cooking & Tinkering workbenches.
  - 20 Shop workbenches (stocked and zero-credit variants across 10 shops).
  - 59 Campaign checkpoints across Worlds 1–6 (all main quests `w1_mq02` through `w6_mq30` and all side quests `w1_sq01` through `w6_sq30`).
- System and economy verification (`DebugSystemScenariosInstrumentedTest` passed with 3/3 tests):
  - Every fishing zone has usable gear and valid room actions.
  - Stocked workbenches produce every current recipe and survive save/load reload.
  - Full shop ViewModel transactions and credit constraints verified: zero-credit rejection, price deduction, item acquisition/equipment unlock, inventory selling, and credit limits.
- Release generated configuration disables the scenario menu; debug configuration enables it.
- Coverage catalogs synchronized via `python scripts/debug_coverage.py --check`.
- Preserved all existing uncommitted user files (`.idea/caches/deviceStreaming.xml`, `docs/story/Easter_Eggs.md`, `nav_audit_summary.json`).

- In-game debug scenario browser in `MainMenuScreen.kt` updated to source exclusively from `DebugTestRegistry.allScenarios` (97 rebuilt scenarios).
- Scenario procedure review view with starting state, step-by-step instructions, and expected outcomes verified before launching via `DebugScenarioBrowserInstrumentedTest`.
- Legacy scenario list removed from in-game UI.
- All connected instrumentation tests (`DebugTestSessionInstrumentedTest`, `DebugSystemScenariosInstrumentedTest`, `DebugScenarioBrowserInstrumentedTest`) pass on Android 17 emulator.

Reports: `app/build/test-results/testDebugUnitTest`, `app/build/outputs/androidTest-results/connected/debug`, and `app/build/reports/lint-results-debug.html`.

## Verified vs Untested Scope Matrix

### Verified
- **Scenario Launch & Isolation:** 97/97 scenarios launch cleanly in isolated test storage (`files/debug-test-session/datastore`) without touching campaign saves (`files/datastore`).
- **Fixture Relationships:** Node entry rooms, hub world IDs, party rosters, active/completed quest mappings, level scaling, and valid inventory verified via `DebugFixtureValidator`.
- **System Systems:** All 6 fishing zones, all 27 tinkering recipes, all 15 cooking recipes, all 6 arcade cabinet resets, and 10 shops (20 variants) verified with ViewModel transactions.
- **In-Game Debug Browser:** Rebuilt scenario catalog and procedure review modal verified via `DebugScenarioBrowserInstrumentedTest`.

### Untested & Gameplay Limitations
- **Manual Gameplay Playtesting:** Scenarios launch Nova and party into authored start rooms with requisite quest states, but end-to-end mission gameplay (navigating hazards, clearing bosses, dialogue trees to quest finish) has not been run by human playtesters.
- **Physical Device & Audio:** Verified strictly on Medium_Phone Android 17 emulator (`emulator-5554`). Audio cue playback and physical touch ergonomics are unverified.


