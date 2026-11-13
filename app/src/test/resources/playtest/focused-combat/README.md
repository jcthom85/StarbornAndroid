# Earned combat review checkpoints

Prepared November 13, 2026. These JSON snapshots are inputs for FocusedCombatCheckpointExportTest, which exports a separate protobuf bundle for numbered slots 11–14 using GameSessionPersistence and verifies exact save/reload parity.

- warden.json, hound-pack.json and plant-pair.json: captured before explicit engagement by LegalRouteCampaignRunnerTest.authoredWorld2CompletionistRouteFromEarnedLaunchReachesWorld3AndReloads. The route starts at the previously recorded earned World 1 launch checkpoint and uses public travel, interactions and real combat.
- beast.json: identical state to ../world2-beast-main.json, recorded before Face the Beast on the public main route. No Gh0st in the party and no Beast victory/Anchor completion.

The exporter grants no XP, gear, healing, milestones or quest tasks. It writes only its build/report output folder, never the user's campaign storage. All encounter sources are still uncleared. Keep fixtures in sync with a newly verified public capture if progression or encounter contracts change.

The exported bundle was added only to unused slots on the emulator after backing up and hashing the existing numbered saves. See docs/playtest/FOCUSED_COMBAT_PLAYTEST_2026-11-13.md for the user-facing slot map.
