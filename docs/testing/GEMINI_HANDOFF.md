# Continue the complete debug scenario rebuild

## User objective and authorization

Finish implementing a replacement debug scenario suite for the ENTIRE current Starborn Android game before handing it to the user for full-game playtesting. Scope includes all six worlds, Astra, all main/side quests, bosses, tutorials, combat, progression, economy, equipment, cooking/tinkering, fishing, all arcade games, puzzles, saves/recovery and NG+.

The user approved the master plan and repeatedly authorized implementation. Continue doing the work; do not respond with another plan or ask whether to proceed. They specifically corrected the earlier premature handoff of only the first ten scenarios. Do not call the suite complete because entries launch. Validate as you implement; retire legacy entries only after verified replacement coverage. No release/publishing request exists.

## Read first

1. `docs/debug-scenario-master-plan.md`
2. `docs/testing/README.md`
3. `docs/testing/debug-replacement-catalog.md`
4. `docs/testing/verification-2026-09-15.md` and the continuation results below
5. Current source in `app/src/main/java/com/example/starborn/debug/`

Inspect `git status` and any applicable AGENTS.md. The work is UNCOMMITTED, including earlier Codex work. Preserve it. Unrelated user files include `.idea/caches/deviceStreaming.xml`, `docs/story/Easter_Eggs.md`, and `nav_audit_summary.json`; do not revert/delete or include them accidentally.

## Current implemented state

38 selectable rebuilt entries:

- Opening / Wake Up Call using normal New Game.
- Exact Cryo recipe, missing-scrap recipe, opening manual-save recovery.
- Six installed arcade cabinet workbenches.
- Six fishing zone workbenches, at the actual authored room/action with current rods/lures.
- All-recipes cooking and tinkering workbenches.
- Ten shops, each with stocked and zero-credit variants (20 entries). These open the shop screen directly; their return destination is the Astra staging room, not the shop's campaign location.

`DebugTestRegistry.allScenarios` is the combined registry used by the menu, lookup, and launch tests. Its older `scenarios` property contains only the first ten; avoid accidentally using it for full-suite coverage.

Implementation files:

- `debug/DebugTestRegistry.kt`: shared procedure/fixture model, first ten entries, combined registry.
- `debug/DebugSystemScenarios.kt`: fishing, provisioning and shops.
- `debug/DebugFixtureBuilder.kt`: normal-game start, fresh recipe and arcade setup.
- `debug/DebugSystemFixtureBuilder.kt`: fishing/provisioning/shop setup using current catalogs.
- `debug/DebugFixtureValidator.kt`: basic location, quest/task, inventory, party and pending-state checks. It does NOT yet validate all milestones/skills/gear ownership or prove actionable progression.
- `di/AppServices.kt`: rebuilt dispatch/validation, separate persistence namespace, and legacy switch still present.
- `MainActivity.kt`, `navigation/AppNavigation.kt`, `feature/mainmenu/ui/MainMenuScreen.kt`: separate Test Session activity, instructions dialog, legacy filter, resume-test-saves button, direct shop navigation.
- `domain/cinematic/CinematicCoordinator.kt`: cancellation discards scenes without invoking completion callbacks.

Normal saves: `files/datastore`. Test saves/session/autosave/quicksave/backups: `files/debug-test-session/datastore`. Settings remain shared and are documented. Initial scenario intent is consumed once; restoring/reopening test saves must not reseed them. Release menu flag is disabled; debug enabled. `AppServices.release()` now cancels its runtime/persistence scopes before releasing audio.

Legacy entries and hidden launchers have NOT been removed. They remain behind a migration filter. Some old helpers are public or used by tests; inspect callers before deletion. Some old later-world presets inherit full inventory/unlocks, so wrapping or renaming them is not a fresh, trustworthy campaign fixture.

## Coverage infrastructure and caveats

`python scripts/debug_coverage.py` regenerates inventory, replacement catalog and legacy migration inventory. `--check` checks drift. JSON records individual assets, quest stages/tasks, room actions/exits and behavior requirements, plus source digests. Many assignments are broad planned families, not executable scenarios. Execution status deliberately remains `not_run` unless separately evidenced; a `built` scenario does not mean all associated content passed.

The generator currently identifies implemented IDs by parsing Kotlin; update it when adding new scenario construction patterns. Run records are separate from generated inventory. Do not overwrite human execution evidence on regeneration.

Inventory includes 60 quests, 465 rooms, 64 nodes, 13 hubs (including Astra), 6 tuning puzzles, 27 tinkering recipes, 15 cooking recipes, 6 fishing zones and 10 shops. Duplicate asset IDs `items:phase_rounds` and `cinematics:ollie_intro_scene` are flagged, not fixed. Inspect current catalogs if counts change.

## Verification actually completed

Earlier stage: 46 focused JVM tests passed; lint had zero errors, 349 warnings, 11 hints. Browser instructions and actual activity navigation into the recipe room passed instrumentation. Those results preceded the latest system/shop additions and are not a claim of fresh full-regression coverage.

Latest run: September 15, 2026, 20:00 UTC, Medium_Phone Android 17 emulator (`emulator-5554`): THREE instrumentation tests passed, zero failures/errors:

- `DebugTestSessionInstrumentedTest`: all 38 rebuilt entries launch/validate; selected cabinet reset checks; campaign manual/quick saves remain unchanged by test writes; exact/missing crafting and disk reload; clean opening reset.
- `DebugSystemScenariosInstrumentedTest.everyFishingZoneHasUsableGearAndItsActualRoomAction`: six zones have their actual fishing action and prepare encounters for every available rod/lure combination.
- `DebugSystemScenariosInstrumentedTest.stockedWorkbenchesCanProduceEveryCurrentRecipe`: all 27 tinkering and 15 cooking recipes produce outputs using production services; output inventory survives save/load.

Latest changes compile and debug APK was produced at `app/build/outputs/apk/debug/app-debug.apk`. Reports are in `app/build/outputs/androidTest-results/connected/debug`. Generator check and git diff whitespace check passed.

NOT verified: shop UI navigation/transactions for the newly added 20 entries (launch setup only), full UI use of provisioning/fishing, playing arcade runs, continuous campaign, all quest handoffs, real process death, audible audio or physical-device quality. Do not claim these passed.

## Immediate next work

1. Verify new shop navigation and transactions through the real UI/ViewModel, including zero funds, successful purchase/sale and persistence. Note `arcade_prize_shop` text mentions tokens; inspect actual currency behavior and report any discrepancy as a game issue rather than silently masking it.
2. Build the still-missing campaign fixture specifications and implementations for EVERY main quest and side quest across Worlds 1–6. Resolve prerequisites from current events/dialogue/quests/rooms, not old names. Use appropriate party, skills, equipment and supplies; document synthetic state. Start before the action under test, finish validation after its reward/onward handoff.
3. Complete combat, recruitment/leveling/equipment, tutorial, tuning/environmental puzzle, Astra, discovery/gate, failure/recovery, NG+, and remaining boundary coverage. Stocked workbenches do not prove acquisition or economy.
4. Strengthen fixture relationship validation and meaningful runtime/UI tests as needed. Keep each test's evidence scope explicit.
5. Validate replacement gameplay, then remove corresponding old menu entries, hidden aliases, unused helpers and obsolete setup references. Keep useful behavior regression tests.
6. Update docs/coverage and deliver the full suite with an explicit verified/unverified matrix. Do not ask the user to perform the broad playtest while the suite is still missing most campaign checkpoints.

The existing `LegalRouteCampaignRunnerTest.kt` can inform event sequencing, but its own documentation admits direct room seeding, reflective action triggers and synthesized combat victories. Its initial state also differs from normal New Game. Do not call its saves legal-playthrough/balance-proven fixtures or its success full campaign acceptance.

## Commands/environment

Workspace: `C:\Users\jcthomas\StudioProjects\StarbornAndroid`, PowerShell. Java lives at `C:\Program Files\Android\Android Studio\jbr`. Gradle may require permission for its cache outside the workspace. Emulator was left running; check `adb devices -l` before use. Do not clear the user's app storage or personal saves.

Latest successful validation command:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.starborn.DebugTestSessionInstrumentedTest,com.example.starborn.DebugSystemScenariosInstrumentedTest' --console=plain --warning-mode=none -q
python scripts/debug_coverage.py --check
```

Use appropriate focused tests and build/lint after changes. Preserve the user's worktree. No commit, push, or Play upload was performed.
