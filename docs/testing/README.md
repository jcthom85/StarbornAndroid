# Game test suite rebuild

October 19: [World 1 review](WORLD1_REVIEW_2026-10-19.md) records 71 passing focused host tests, all ten quests and alternate-order escape recovery, plus a successful Android build. Human phone and audiovisual acceptance remain open.

October 19: [crew production adoption](CREW_PRODUCTION_ADOPTION_2026-10-19.md) supersedes the prototype-only and migration-pending statements below. Normal New Game and player-save migration now use crew progression. The continuous production campaign still has an open World 6 acceptance failure; earlier test totals do not certify this rollout.

[Android exploration navigation](ANDROID_EXPLORATION_NAVIGATION_2026-10-15.md) centers Menu, removes the duplicate exit, reserves room-interaction space, and adds system Back exits from eligible rooms.

[Popup alignment and desktop backdrop repair](POPUP_ALIGNMENT_AND_BACKDROP_FIX_2026-10-15.md) implements the acknowledgement rules and prevents sharp room artwork from leaking into the side atmosphere during travel.

[Popup dismissal audit](POPUP_DISMISSAL_AUDIT_2026-10-15.md) inventories both platforms, identifies alignment candidates, and records tutorial body-tap fixes.

[Exploration interactions and combat return](EXPLORATION_INTERACTIONS_AND_COMBAT_RETURN_2026-10-15.md) records the object interaction rules, shared victory repair and single blocked-route prompt.

[Prototype encounter tempo tuning](CREW_TEMPO_TUNING_2026-10-15.md) selects a scoped adjustment after fresh-seed comparisons; 683 Android-hosted tests and five focused desktop checks pass. Human build-interest and AP balance remain pending.

[Full-crew automated balance pass, October 15](CREW_BALANCE_PASS_2026-10-15.md): 1,728 independent fights plus attrition sequences; encounter spikes and uneven paid value remain under investigation.

The [full-crew prototype](CREW_PROTOTYPE_IMPLEMENTATION.md) now provides all 32 executable nodes, owner-specific resets and 26/36/44-AP campaign-derived checkpoints. Balance and production migration remain pending.

For the October 14 review of catalog and desktop regression failures, see [the regression review](REGRESSION_REVIEW_2026-10-14.md). Its automated results are separate from the in-game acceptance backlog below.

The additional `nova_build_prototype` scenario starts an isolated, earned-AP opening with Nova's new paths. See the [prototype implementation record](NOVA_PROTOTYPE_IMPLEMENTATION.md) for launch instructions, automated evidence and balance limits.

October 15 adds first-AP/first-purchase guidance and contextual perk cues. The same record describes desktop's shared mobile Break visuals and their scoped UI checks.

## Current implementation

The replacement suite is under construction. There are now 38 entries: the ten below, six fishing locations, two provisioning workbenches, and stocked/zero-credit variants for all ten shops.

| ID | Starting point | Scope |
| --- | --- | --- |
| `campaign_w1_mq01` | Normal new game | Opening through Wake Up Call and next objective |
| `recipe_cryo_exact` | Workshop with exact recipe resources | Crafting success, repeat refusal, persistence |
| `recipe_cryo_missing` | Same setup without scrap | Refusal without consumption, persistence |
| `recovery_opening` | Normal new game | Opening actions and manual save/reload |
| `arcade_deep_mine_asteroid_drill` | Installed in Astra common room | Deep Mine gameplay, rewards and persistence |
| `arcade_canopy_hopper` | Installed in Astra common room | Canopy Hopper gameplay, rewards and persistence |
| `arcade_spire_infiltrator` | Installed in Astra common room | Spire Infiltrator gameplay, rewards and persistence |
| `arcade_slag_catcher` | Installed in Astra common room | Slag Catcher gameplay, rewards and persistence |
| `arcade_orbital_defense` | Installed in Astra common room | Orbital Defense gameplay, rewards and persistence |
| `arcade_harmonic_pulse` | Installed in Astra common room | Harmonic Pulse gameplay, rewards and persistence |

These definitions live in `DebugTestRegistry.kt`; fresh setup lives in `DebugFixtureBuilder.kt`.
Additional system definitions and setup live in `DebugSystemScenarios.kt` and `DebugSystemFixtureBuilder.kt`. The menu uses `DebugTestRegistry.allScenarios`.

Fishing workbenches use each zone's authored room/action and all current rods/lures. Cooking stocks three batches of every recipe and the four playable chefs; tinkering stocks current recipes/tools and learns all schematics. Shop workbenches open the actual shop screen directly and return to Astra for saving; they do not prove regional shop access or unlock gated merchandise.
The opening uses the production New Game path. Recipe boundaries specify prerequisites from the current opening quest and recipe, with no full-inventory grants or legacy checkpoint chaining. Arcade workbenches install only their selected cabinet and explicitly do not prove discovery, repair, or campaign access.

## How to use

1. Install the rebuilt debug APK. Open **Debug Scenarios** from the title screen.
2. Select an entry to review starting conditions, steps, pass criteria, and limitations.
3. **Launch test** opens a separate Test Session. Its session file, autosave, quicksave, numbered slots, and save backups use `files/debug-test-session/datastore`.
4. For recovery tests, load the test slot rather than launching the scenario again. After relaunching the app, choose **Resume Test Saves**, then **Load Test Save**.
5. To restart a fixture, return to the test title screen and launch it again. **Exit Test Session** returns to the previous activity.

Normal campaign saves continue using `files/datastore`. Audio/accessibility/tutorial settings are currently shared; record them for a run and enable tutorials when testing their triggers. Launching another fixture replaces the current test session and may update its autosave; preserve useful test checkpoints in numbered test slots.

The legacy filter keeps old shortcuts available during replacement. They have not earned acceptance in the new suite. Their removal is gated on replacement gameplay evidence and caller migration. Release builds no longer expose the scenario menu.

## Coverage and evidence

- [Inventory summary](debug-coverage.md): asset counts and every quest's planned scenario ID.
- [Replacement catalog](debug-replacement-catalog.md): planned route/system families and explicit system behavior cases.
- `debug-coverage.json`: individual asset, stage, task, action, exit, and behavior rows with content digests.
- `debug-legacy-migration.json`: visible/hidden old launchers and shared setup groups.
- [Run record template](run-template.md): copy for each gameplay run; never overwrite old evidence with regenerated inventory.

Regenerate inventory with `python scripts/debug_coverage.py`; validate it with `python scripts/debug_coverage.py --check`.
Generated assignments are a backlog, not proof of runtime reachability or successful gameplay. Duplicate asset IDs are retained as separate occurrences and explicitly flagged.

## Remaining work

1. Review actual routes, prerequisite state, legacy callers, and asset reachability; split broad system/route families into practical variants.
2. Complete the opening gameplay acceptance, then build campaign fixtures through all six worlds, Astra, and all side quests.
3. Implement the remaining combat/system/recovery workbenches, including all arcade games and NG+.
4. Execute continuous campaign, interruption, presentation/audio, and physical-device acceptance.
5. Remove superseded entries and unused setup code after their replacements pass.

The initial framework tests do not establish full-game acceptance, balance, a playable completed opening, or process-death recovery.
