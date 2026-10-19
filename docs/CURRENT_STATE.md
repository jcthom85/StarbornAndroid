# Starborn implementation snapshot

October 19 World 1 update: [the World 1 review](testing/WORLD1_REVIEW_2026-10-19.md) records opening and escape fixes, early-Warden save recovery, all ten quests completed through a production controller route, 71 passing focused JVM tests and a successful debug APK build. Required certification makes the observed World 1 budget 8 AP. Human phone, audiovisual and broader encounter acceptance remain open.

October 19 progression update: [crew production adoption](testing/CREW_PRODUCTION_ADOPTION_2026-10-19.md) supersedes the legacy-default and migration-pending statements below. The 32-node crew structure now integrates with normal New Game and legacy save migration. Campaign acceptance remains open: the continuous production route currently loses at World 6's memory-fragments encounter after exhausting healing supplies. Final human balance and physical-device acceptance also remain open. Historical test totals below do not certify this change.

Date: October 14, 2026. Source review of the current working tree, which contains ongoing changes. This is not a release manifest, a claim of complete correctness, or a full device playthrough. Start with [the index](README.md).

## Baseline

The owner confirms that most design documents predate the game. Current code, catalogs, and live routes take precedence for implementation facts. Correct conflicting documentation first. Demonstrate a defect or choose a redesign before altering working logic.

Source-reconciled world references are available for [Sector 9](story/world_2_the_wilds/00_world_story.md) and [Spire](story/world_3_the_spire/README.md). Their archive snapshot records the catalog hashes and reference IDs used; they are documentation checks, not new gameplay acceptance runs.

| Area | Inspected implementation | Sources |
|---|---|---|
| Platforms | Android portrait RPG and Windows desktop port | `app/`, `desktopApp/`, `settings.gradle.kts` |
| Shared rules | Gameplay controllers and domains in a JVM runtime | `gameRuntime/`; platform adapters remain separate |
| Campaign catalog | Six campaign worlds plus Astra; 477 room records, 30 main quests, 32 side quests at this snapshot | `worlds.json`, `rooms.json`, `quests.json`; record counts include special content and are not playtime |
| Exploration | Tappable prose, room variants, explicit action links, gated movement and authored special exits | `RoomPresentation.kt`, `ExplorationController.kt`, catalogs |
| Enemy movement | Controller-managed movement and persisted party state | `EnemyMovementManager.kt`, `enemy_movement.json` |
| Combat | ATB, cooldowns, elemental affinities, stability/Break, statuses, equipment and carried resources | Combat controller, engine, processor and catalogs |
| Progression | XP thresholds, level-based skill unlocks, AP trees, story unlocks and gear | `leveling_data.json`, `progression.json`, `skill_trees/`; effect support must be checked |
| Activities | Tinkering, cooking/meals, equipped snack abilities, fishing and six arcade engines | Crafting/fishing/arcade controllers and catalogs |
| Astra | Five-room hub with rest, navigation, crew entries, records, recreation and gated simulations | [Current functionality](story/hub_ship_astra/01_functionality.md) |
| Saves | Shared session/protobuf state, platform repositories, backups/migrations, pending-scene and battle-checkpoint handling | `domain/session/` and live platform services |

## Evidence already available

[The completed public campaign record](desktop/PUBLIC_CAMPAIGN_WORLD_6.md) documents all 30 main quests through public controller actions and real combat, with zero assisted actions, synthetic victories, or position shortcuts. It uses a known seed, authored answers, virtual time, disabled tutorials, and a documented starter-component fixture. It establishes a specific route and save/reload evidence, not broad balance or human input quality.

[Campaign systems](desktop/CAMPAIGN_SYSTEMS_ROUTE_AUDIT.md) and [food/fishing](desktop/FOOD_FISHING_ECONOMY_AUDIT.md) records contain scoped availability and economy evidence. Read follow-up notices before using earlier counts. [Desktop readiness](desktop/PC_READINESS_PASS.md) records packaging, input, and audio-decode work with native acceptance limits.

The [October 14 regression review](testing/REGRESSION_REVIEW_2026-10-14.md) investigated the earlier evaluation failures and an additional outdated common-room expectation. It fixed hidden completed objectives in the desktop journal and aligned tests with current labels, layouts, routes and room-description selection. Final automated verification passed 652 Android-hosted tests and 147 desktop tests, with zero failures, errors or skips. These are working-tree fixture results, not device or release acceptance.

## Open verification and decisions

October 15 tuning follow-up: [two prototype encounter spikes now use lower base enemy Speed](testing/CREW_TEMPO_TUNING_2026-10-15.md). Paired fresh-seed prepared wins improved from 118/256 to 231/256; other encounters and normal campaign mode retain their rules. Support buffs were screened and rejected as inconsistent. The full Android-hosted suite passes 683 tests, five focused desktop checks pass, and a debug APK is built. Manual interest/clarity and final AP balance remain open.

October 15 automated balance follow-up: [the full-crew comparison](testing/CREW_BALANCE_PASS_2026-10-15.md) completes 1,728 independent fights and 242 attrition fights across sixteen seeds. Orbital Fighters and the Source three-enemy formation remain pressure points; paid builds do not consistently outperform core abilities alone. No gameplay values were tuned. The Android-hosted suite now passes 681 tests.

[The progression and ability plan](design/PROGRESSION_AND_ABILITY_PLAN.md) now has an [opt-in Nova prototype](testing/NOVA_PROTOTYPE_IMPLEMENTATION.md): two executable four-node paths, earned AP, purchase receipts and isolated playtest sessions. Effective HP presentation and NG+ skill/AP/schematic carryover are implemented. Other crew paths, legacy-tree migration and broad balance remain pending.

October 15 follow-up: the [full-crew prototype](testing/CREW_PROTOTYPE_IMPLEMENTATION.md) implements all 32 nodes, free owner-specific resets and campaign-derived 26/36/44-AP checkpoints. This supersedes the Nova-only scope above. Party-wide balance and production migration remain pending. First-purchase guidance is present; desktop Break visuals share the mobile renderer.

- Verify all player-facing skill-tree promises against supported runtime effects. A tree definition or unlocked flag is insufficient evidence.
- Verify effective HP and NG+ carryover on physical devices and existing saves; automated menu/combat and save/reload checks now cover the corrected behavior.
- Finish human campaign playthroughs, varied earned builds, route/resource pressure, optional choices, and recovery after failure.
- Measure physical-phone startup, frame time, memory, heat/battery, large text, touch, process death and audio interruptions. Separately verify packaged desktop audio, focus, display/DPI and installer behavior.
- Rewrite heavy Source prose and prototype distinct puzzle interactions under the agreed direction. Proposed replacements are not implemented features.

## Release facts

Local Gradle versions, old upload notes, and generated installers can differ. Confirm the specific artifact/commit and release track for release work. This source-only documentation pass does not establish which build is currently on Google Play and does not publish or sign a release.
