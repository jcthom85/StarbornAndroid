# Starborn improvement pass

Approved scope: correctness, realistic campaign testing, combat/economy, opening/UI,
exploration/narrative consistency, and presentation/release verification.

## Phase 1 — correctness (completed 2026-09-08)

- Implemented: remove three blocking Compose lint findings.
- Implemented: extend the XP curve through level 12 (4000 and 4900 cumulative XP,
  continuing the existing +100 increase in each level's XP requirement).
- Implemented: remove level-three unlocks already granted as starting skills.
- Implemented: reject malformed saves, recover the newest readable backup, preserve
  unrecoverable files, and remove recovery copies when a slot is deliberately cleared.
- Implemented: slot inspection/loading no longer imports bundled checkpoints or
  overwrites slot 2 when it differs from the launch-bay sample. Explicit sample reset
  remains available. Empty slots remain empty.
- Unrecoverable manual slots report an error without changing their bytes.
  Unrecoverable DataStore saves retain a `.corrupt` copy before resetting, allowing
  a new save to be written. Recovery detects malformed protobuf; it cannot detect
  every logically incorrect but structurally valid save.
- Verification: content/asset validators passed; all 296 JVM tests passed.
  Final lint rerun passed after the save-path changes (zero errors).
  Existing warnings remain. No device installation or fresh device playthrough in
  this phase. Next implementation phase: trustworthy campaign testing.

## Phase 2 — trustworthy testing (in progress)

- Derive combat fixtures from attainable checkpoint party/skills/gear.
- Exercise production enemy behavior and support abilities.
- Implemented: separate `CampaignCatalogIntegrityTest` from
  `CampaignEventIntegrationTest`; neither is presented as an autonomous playthrough.
- Implemented: one continuous event/dialogue route through all 30 main quests,
  including MQ13 and formerly skipped opening quests in later worlds. Recreate
  the harness from a real protobuf save between every world; assert whole-session
  equality, all five main quests per world, and ending milestones.
- Implemented: fail on unknown dialogue conditions, missing dialogue, unchosen
  options, nonexistent room/enemy IDs, and stalled dialogue loops.
- Fixed: eligible active-quest dialogue now precedes new quest offers, preventing
  Zeke's unaccepted World 2 optional quest from hiding World 3 main-quest dialogue.
  Eligible entries within the same group keep author order; regression covered.
- Implemented: label the old combat report as synthetic in Markdown and JSON,
  and remove unsupported win-rate/encounter-length acceptance thresholds.
- Limits: the campaign harness supplies navigation, craft success, cinematic
  completion and battle victories. It uses EventManager and DialogueService,
  with simplified quest hooks, not QuestRuntimeManager or the exploration UI.
  It does not prove route/action reachability, crafting, rewards, or combat balance.
- Still pending: attainable combat checkpoints, production enemy-AI/support/ATB
  simulations, and integration with production quest runtime/reward handling.
  Do not use the synthetic report to justify enemy buffs.
- Added opening combat runtime coverage: attainable solo Nova with her starting
  skill and no optional equipment; authored assets and production combat mapping,
  ATB, skill command and loader AI. Presentation dependencies are mocked and
  tutorials are disabled. No exact damage, victory or win-rate claims.
- Added isolated production AI decision tests for Core Repair: wounded self-heal
  applied by the real action processor, cooldown exclusion, authored Jammed
  blocking, and no wasted repair at full HP. These use explicit scenario stats
  and a SELF targeting callback, not a campaign-earned checkpoint.
- Remaining combat work is broader checkpoint/loadout coverage, general support
  targeting, reproducible encounter policies and campaign-earned reward snapshots.
- Combat-test follow-up verification (2026-09-08): all 299 JVM tests passed,
  zero failures/errors/skips. This follow-up changes only tests and this backlog;
  no gameplay tuning or assets changed, and lint/device checks were not rerun.
- Verification (2026-09-08): `scripts/verify_world1.ps1 -SkipMaestro` passed
  asset/integrity validators, all 292 JVM tests (zero failures/skips), and Android
  lint. The lower test count reflects consolidating six isolated world tests into
  one continuous route, plus a new dialogue-priority regression. No device/UI
  playthrough was performed in this pass.

## Phase 3 — combat and rewards

- Evaluate encounter decisions and cooldown cycles with corrected fixtures.
- Audit main-path and optional-content XP/credits, shop prices and recipe costs.
- Tune individual encounters and rewards using measured results.

## Phase 4 — opening and interface

- Group opening guidance around getting ready, repairing the cutter and its bypass.
- Review current screens; compact loadouts and unavailable mod slots.
- Keep enemy information visible during commands; improve truncated labels.
- Reduce repeated tutorials and progress interruptions.

## Phase 5 — exploration and narrative

- Extend action discoverability checks across all worlds.
- Migrate to explicit action references compatibly.
- Review route discovery cadence, Erosion terminology and generic dialogue.

## Phase 6 — presentation and release

- Verify available device/emulator layouts, accessibility and lifecycle behavior.
- Check audio transitions, asset loading and release build behavior.
- Profile representative exploration and combat scenes.

External-player feedback remains separate from automated verification.
