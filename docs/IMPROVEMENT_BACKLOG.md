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
  completion and battle victories. It now uses EventManager, DialogueService and
  QuestRuntimeManager with the exploration quest callbacks and a test scheduler.
  It does not run the exploration UI.
  It does not prove route/action reachability, crafting, rewards, or combat balance.
- Still pending: attainable combat checkpoints, production enemy-AI/support/ATB
  simulations, and integration with production reward handling.
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
- Production quest follow-up (2026-09-08): all 30 main quests pass with real stage
  advancement and task completion. Every world reload also verifies the hydrated
  session, completed journal, active/completed quest sets and stage map. Runtime
  coroutine scopes are cancelled after each world and on assertion failure.
- Added generated `reports/campaign/event-checkpoints.md` recording cumulative
  event XP/credits, party and skill unlocks. Before the payout fix the route paid 480 event XP
  by World 1 and no further event XP through World 6; credits remain zero in this
  limited ledger. Battle earnings and optional routes are excluded. Quest catalog
  rewards are displayed by the UI but are not automatically paid by the quest
  manager. Audit advertised versus actual quest payouts before choosing late-game
  XP/loadout assumptions; do not silently add catalog payouts to fixtures.
- Fixed missing advertised XP on all 25 World 2–6 main quests: insert `give_xp`
  into their existing one-time completion events (including cinematic completion
  callbacks). This pays 1,250 / 2,600 / 4,600 / 6,200 / 9,600 XP per world.
  Existing item grants remain unchanged. Already-completed events in old saves
  are not replayed or retroactively compensated.
- Campaign coverage asserts each world's actual XP delta equals its catalog
  main-quest XP total. XP-paying player actions are replayed immediately and on
  a freshly loaded save to check against duplicate awards. The resulting event
  ledger ends at 24,730 cumulative XP, including the existing 480 from World 1.
- Remaining reward audit: World 1 and optional quests, item reward discrepancies,
  and how quest XP flows into levels/party progression. The restored advertised
  XP substantially changes progression; remeasure level timing before tuning
  encounter difficulty in Phase 3.
- Payout-fix verification: full JVM suite and asset/integrity validators passed.
  Lint and device testing were not repeated for this asset/test change.
- Verification: both CampaignEventIntegrationTest cases passed after this follow-up.
  No production code or asset changes were needed; full suite/device checks were
  not repeated for this test-harness-only change.
- Combat-test follow-up verification (2026-09-08): all 299 JVM tests passed,
  zero failures/errors/skips. This follow-up changes only tests and this backlog;
  no gameplay tuning or assets changed, and lint/device checks were not rerun.
- Verification (2026-09-08): `scripts/verify_world1.ps1 -SkipMaestro` passed
  asset/integrity validators, all 292 JVM tests (zero failures/skips), and Android
  lint. The lower test count reflects consolidating six isolated world tests into
  one continuous route, plus a new dialogue-priority regression. No device/UI
  playthrough was performed in this pass.

## Phase 3 — combat and rewards

- Companion progression: exploration/quest XP now awards the full amount to
  each current party member, matching battle awards. Recruitment already seeds
  the lead's XP and level. All recipients update levels and earned skills on the
  award; saved XP differences are retained, with no retroactive compensation.
  Snapshot-based fallbacks prevent missing companion data receiving XP twice.
  Duplicate roster entries are deduplicated; absent companions receive no award.
- Companion verification: all 308 JVM tests passed, including recruitment/resume,
  missing-XP fallback and full-campaign party parity assertions. Device and lint
  checks were not repeated in this focused pass.

- XP curve tuned using catalog main-quest payouts and scripted battle XP; see
  `docs/XP_CURVE_TUNING.md` for inputs and limits. Level 6/9/12 thresholds are now
  3,000/11,000/26,000 XP, targeting Worlds 2/4/6 for the lead player with combat.
  Existing saves retain earned levels after combat and exploration rewards.
  Full suite: 306 tests passed, including saved-level compatibility and curve
  pacing regressions. No device playthrough or lint rerun in this pass.
  Companion XP distribution and item rewards still need attention.

- XP payout audit (2026-09-08): restored missing XP for World 1 main quests and
  optional quests across all worlds, including alternate completion events and
  the two dialogue completion paths. Existing paid w1_sq01/w1_sq02 remain intact.
- QuestPayoutIntegrityTest checks all 60 catalog quests against their authored
  completion payouts. It isolates payout actions from entry conditions, executes
  them through EventManager, and verifies one-time event replay protection.
  It does not prove every optional route or dialogue is reachable/replay-safe.
- Full JVM suite (300 tests) and asset/integrity validators passed. No device or
  lint rerun in this asset/test pass. Item payout reconciliation remains pending.
- Progression finding: event XP updates the player XP balance immediately, but
  combat victory applies levels and skill unlocks. Quest XP is awarded to the
  lead player; combat XP is awarded to each participant. Audit this asymmetry
  before selecting later-game party fixtures.
- Fixed immediate exploration progression: both `give_xp` and reward-bundle XP
  now use ExplorationXpAwarder to update the lead player's level and earned
  level skills immediately. Positive awards also recover missing earned unlocks
  from older saves. Nonpositive awards are no-ops; existing levels never decrease.
  The existing lead-only quest XP recipient rule remains unchanged.
- Verification: all 304 JVM tests passed, including four focused progression
  regressions and the continuous six-world campaign/save-load check using the
  same awarder. The generated ledger now records actual lead levels (6, 9, 12,
  12, 12, 12 on this scripted route). Device/UI verification was not performed.
- With restored payouts, the scripted campaign ledger reaches 5,105 XP after
  World 3, exceeding the 4,900 XP level-12 threshold. Main quests alone reach
  4,525 XP after World 3 and exceed the cap in World 4. No XP curve changes made;
  level timing and party XP distribution need an explicit tuning pass.

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

## Support runtime regression (2026-09-08)

- Added a wounded Stalker-Vine scenario using production enemy assets, ATB,
  AI scoring, targeting and skill execution. Only its initial injury is seeded;
  this is not a campaign-earned checkpoint or a complete encounter playthrough.
- The regression exposed Nature Heal falling through to enemy targeting because
  its positive healing power was treated as damage. Added explicit self-targeting
  for this authored self-heal, consistent with its skill description.
- The check requires actual enemy HP recovery, maximum-HP clamping and an active
  heal cooldown. Group support, encounter win rates and device verification remain
  open; this does not complete the entire combat-testing phase.
- Verification: all 313 JVM tests passed after the fix. Lint and device checks
  were not rerun in this focused pass.

## Later-game combat fixture (2026-09-08)

- Added a four-character, level-9 World 4 fixture against Titan Walker using
  production combat mapping, ATB and command handling. All members expose their
  starting and level-6/9 skills, without level-12 unlocks.
- Verified Orion's level-9 command becomes selectable and changes combat state.
- The checkpoint is seeded from the estimated main-route XP baseline, without
  optional gear; it is not a campaign-earned inventory snapshot, boss victory,
  support-AI encounter test or balance measurement.
- Full JVM suite passed. No gameplay assets changed; device testing remains open.
- Next focused step: support-AI behavior in a representative encounter, followed
  by encounter outcome measurements and shop/recipe economy checks.

## Item payout audit (2026-09-08)

- Reconciled advertised item quantities against event and dialogue payout actions;
  no missing item grants were found, so no item reward assets were changed.
- World 4's escape rewards are collected in the prerequisite engine/array theft
  events, not granted again at completion. Corporate Espionage's private ending
  intentionally substitutes Phase Rounds for the ledger. Story extras remain.
- Added catalog-wide isolated item-payout and replay checks. These flatten
  branches and do not establish route reachability or actual dialogue replay safety.
- Both Corporate Espionage endings also run with their real event conditions:
  either ending prevents both further payouts after an in-memory state restore.
  This focused test is not a disk persistence or device test.
- Verification: all 310 JVM tests passed with no failures or skips;
  `git diff --check` passed. Device, lint and asset validators were not rerun
  in this test/documentation-only item audit.
- Next: representative later-game combat fixtures and support-AI encounter
  coverage, then shop/recipe economy tuning. Phases 4–6 remain outstanding.
