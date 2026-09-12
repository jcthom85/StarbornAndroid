# Starborn improvement pass

Approved scope: correctness, realistic campaign testing, combat/economy, opening/UI,
exploration/narrative consistency, and presentation/release verification.

## Current phase status (2026-09-11)

- Release preparation 2026-09-12: 1.3.29 (113) packages all World 5 and current
  World 6 batches below, superseding their outside-bundle-112 notes. User authorized
  commit, push and Internal Testing upload. Signed bundle, release lint and 385 JVM
  tests passed; 354 strict selectors and diff checks passed. rooms.json changes
  verified description-only. Audit remains 58 candidates, including 23 World 6.
  IDE device cache excluded. Upload outcome is reported separately after publishing.

- World 6 Center/Gh0st batch: repaired nine references across eight descriptions.
  Center tuning maps to source_harmonic_focus / w6_mq30_tune_world; eligibility
  requires active MQ30, defeat_god_form done and tune_world unfinished, independent
  of the completion description milestone. Post-finale prose references Vale
  historically. Added 1,280 event-state cases plus restored-state replay checks
  and phantom dogtag reference coverage. Full JVM suite passed; 354 strict
  selectors and diff check passed. Audit now 58 overall / 23 World 6 candidates.
  These are event/prose tests, not tuning-dialog device or finale balance acceptance.
  Changes remain uncommitted, outside bundle 112. Next: memory rooms and the
  remaining World 6 references, then Astra.

- World 6 first batch: repaired seven references across Jed's Echo Bench,
  Manager Patrol Route (both descriptions), Locker Row Loop and White Shore.
  Added 192 event-state cases for independent legacy milestones, prerequisite
  tasks and quest retirement, with restored-state replay checks and inspect
  reference coverage. Full JVM suite passed; 354 strict selectors and diff
  check passed. Audit now 67 overall / 32 World 6 candidates. Prose-only asset
  edits remain uncommitted and outside bundle 112. Center finale references
  remain unclassified pending tuning eligibility and post-finale review;
  continue World 6 with those and the Gh0st/memory rooms next.

- World 5 final static-candidate batch: repaired 30 references in ten descriptions
  across Server Farm, Service Shaft, Solarium, Surveillance Pit and Zero-G Junction.
  Added runtime coverage of independent description/event milestones, retired
  tasks/quests and restored-state replay protection, including film cache and
  guardian choice events. Full JVM suite passed; 354 strict selectors and diff
  check passed. World 5 now has zero static candidates; this is not device or
  route-discovery sign-off. Overall audit: 74 candidates (World 1: 1, World 2: 16,
  World 3: 5, World 6: 39, Astra: 10, unmapped: 3). All World 5 batches remain
  uncommitted and outside Play bundle 112. Asset edits are description prose only.
  Next: World 6 discoverability, then Astra and final residual classification.

- World 5 concourse/mirror/security batch: restored 14 references across six
  descriptions in Grand Concourse, Mirror Walk and Security Hub. Covered 640
  event-state cases across six independent milestones and completed/incomplete
  side quests, with action milestone prerequisites checked separately from
  event eligibility. Replay checks restore through migration before retrying.
  Full JVM suite passed; 354 strict selectors and diff check passed. Audit now
  104 overall / 30 World 5 candidates. Prose-only asset changes remain
  uncommitted and outside bundle 112. Next: Server Farm, Service Shaft, Solarium,
  Surveillance Pit and Zero-G Junction (remaining World 5 candidates).

- World 5 dock/gallery batch: repaired 17 missing references across five
  descriptions in orbital_executive_dock and orbital_airlock_gallery. Tests
  cover 1,536 event-state combinations spanning five independent milestones,
  main-quest prerequisites/task retirement and side-quest completion, plus
  generic/inspect references and replay checks. Sealing's event conditions alone
  permit repetition, but EventManager completed-event history prevents replay;
  the regression verifies this safeguard. All asset edits are prose-only.
  Full JVM suite passed (381 tests), 354 strict selectors and diff check passed.
  Audit now 118 overall / 44 World 5 candidates. Uncommitted, outside bundle 112.
  Next: Grand Concourse, Mirror Walk and Security Hub, then remaining orbital rooms.

- World 5 deep-station batch: repaired 17 references in nine descriptions across
  Mainframe Nave, SysAdmin Nest, Tear and Throne Room. Inspect actions have no
  state gates; quest actions use tasks independently of description milestones.
  Runtime tests cover every description-milestone combination with prerequisite,
  retired-task and side-quest-completed states (192 event attempts, 48 usable
  states plus replay checks). Migration/restoration preserves these partial
  states. Full JVM suite passed; strict selectors passed (354), diff check passed.
  Audit now 135 overall / 61 World 5 candidates. Asset edits are prose-only;
  changes remain uncommitted, outside bundle 112. Next: orbital_airlock_gallery
  and orbital_executive_dock, then remaining orbital rooms.

- World 5 firewall batch: classified and repaired all four Alpha/Beta/Gamma
  variant omissions. AppServices restores/imports through
  migrateOpeningNarrativeState then GameSessionStore.restore; these preserve
  World 5 tasks and milestones independently. EventManager checks active quest,
  prerequisite task and unfinished node task, not the description milestone.
  Thus partial saves can execute a node even with its down milestone present.
  Restored exact action names in the four variants without changing gates or
  events. Regression reproduced the missing reference before the fix; all 96
  combinations of active quest, prerequisite task, node task, node milestone
  and MQ23 milestone now pass, including replay prevention after execution.
  Full JVM suite and 354 strict selectors passed; audit is 152 overall / 78
  World 5 candidates. Changes remain uncommitted and outside bundle 112.
  Next: deep_mainframe_nave, deep_sysadmin_nest, deep_tear and deep_throne_room.

- Release 1.3.28 (112): commit `e43c7e5` pushed; Google Play accepted and
  committed bundle 112 to Internal Testing. Includes World 1/3/4 discoverability
  work; 377 JVM tests, 354 selectors, release bundle and lint passed.
- World 5 first bounded batch after 112: repaired four description references
  (graviton well in all three Anchor Chamber descriptions; scratched route in
  Thaw Gallery). Only prose changed in rooms.json. Added runtime coverage for
  all eight independent combinations of Elara found, MQ24 complete and graviton
  solved, including action retirement after solving. Puzzle asset resolves to
  w5_graviton_matrix_tune. Full JVM suite passed; 354 strict selectors and diff
  check passed. Audit now 156 overall candidates, 82 in World 5.
  At that checkpoint Firewall Alpha/Beta/Gamma omissions remained unclassified: event retirement uses
  quest-task completion while descriptions use milestones; inspect persistence
  reconciliation and partial states before changing those variants. Next continue
  World 5, then World 6/Astra. This batch is uncommitted and outside bundle 112.

- Release 1.3.27 (111): commit `7229b72` pushed; Google Play accepted bundle
  111 and committed the completed Internal Testing release. Not production.
  User authorized commit, push and Internal
  Testing upload of the ridge compatibility repair before a bounded World 1
  pass. Includes description selection changes and 2,592 migrated / 108 raw
  state combinations. No gameplay gates changed; IDE cache excluded.
  The ridge repair is now included in the uploaded build.
  Release checks passed: 369 JVM tests, 354 strict selectors, signed bundle,
  release lint (349 warnings, 11 hints, no errors) and diff check. Build used
  the previously verified 8 GB heap / two-worker invocation.

- Release 1.3.26 (110): commit `3ed88bf` pushed to
  `origin/feature/multiplatform-port`; Google Play accepted bundle 110 and
  committed a completed release on Internal Testing. Not production.
  User authorized version increment, commit,
  push and Internal Testing upload before continuing Phase 5. Includes current
  Phase 4 large-text UI fixes, device flows and instrumentation coverage, World 2
  prose repairs and compatibility tests. Physical-device acceptance and ridge
  compatibility repair remain open at this release checkpoint. Subsequent ridge
  changes are not part of uploaded bundle 110.
  Verification: 367 JVM tests, 354 strict selectors and diff checks passed;
  signed release bundle and release lint passed. Bundle signing exhausted the
  default 4 GB heap; rerun succeeded with command-line 8 GB heap and two workers
  (no project configuration change). Bundle SHA-256:
  `51d5dc3fce78d93f8efc3a7dfa008ad209e2b3585b29f7231e7059c6052fb547`.

- Release 1.3.25 (109): commit `92594a4` pushed to
  `origin/feature/multiplatform-port`. Google Play publisher confirmed bundle
  upload and committed a completed release for version 109 on the internal
  testing track. This was not a production-track rollout or phase sign-off.
  Release verification: 353 JVM tests passed; signed bundle and release lint
  passed (350 warnings, 11 hints, no errors). Unrelated Android Studio device
  cache change excluded from commit.
  Post-release follow-up: added a targeted runtime test for both Jammed and
  Silenced using shipped status definitions. It verifies skill blocking,
  status-message priority over cooldown, cooldown remaining after status
  removal, and availability after both blockers are removed. Targeted test
  passed (1 test, no failures/errors); subsequent full JVM suite passed
  (354 tests). Strict selector validation passes (350), diff check passes.
  This seeds status state, not enemy
  application or timed expiration, and does not verify rendered UI.

  Android UI follow-up: `AbilityRowInstrumentedTest` passed both tests on the
  Android 17 Medium Phone emulator at 320 dp width, with font scales 1.0 and
  2.0. The production row displays the status explanation, disables Use
  without invoking its callback, leaves Details actionable, and enables Use
  after the supplied block is removed. `AbilityRow` is now internal for test
  access; gameplay behavior is unchanged. This is isolated component coverage,
  not enemy status application/expiration or a full-dialog visual audit.
  Espresso (androidTest only) updated from 3.5.1 to 3.7.0 after the older
  runner failed on Android 17 with InputManager.getInstance NoSuchMethodException.
  The AndroidX release notes document the corresponding fix:
  https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0

### Remaining closeout checks

- Phase 4: one remaining acceptance group: physical-device verification on a
  build containing the current UI fixes. Crowded ability-list scrolling and
  level-2/max Overcharge large-text checks now pass end to end on the emulator.
  Opening journal,
  starter loadout and single-ability Loader list/details passed the full-screen
  2.0-font-scale emulator checks. Normal-size
  cooldown, level-2/max Overcharge, long names, opening journal and detail
  scrolling checks are already verified; do not restart those audits.
  Status-blocked ability-row behavior is now covered on the emulator at normal
  and doubled text size; status application/expiration in a real encounter and
  physical-device acceptance are not established by that isolated test.
- Phases 2-3: finale balance (Vale/God), campaign route/shop affordability,
  renewable-resource economy and player/device acceptance remain open.
- Phase 5: three work areas remain: all-world discoverability triage (started;
  audit coverage added and 74 World 2 interaction prose fixes), compatible explicit
  action references, and route-discovery/narrative review. Initial static scan:
  465 rooms, 909 inline actions, 436 missing-name candidates; now 343 after
  74 interaction fixes (96 description references across 55 rooms) and the
  post-110 ridge compatibility descriptions. The extra variants add static
  candidate pairs even though runtime coverage improves.
  Candidates are description/action pairs, NOT confirmed bugs
  or unique actions. Conditional reachability still needs review.
- Phase 6: accessibility, lifecycle/save/audio, performance and release-device
  verification remain mostly pending. Uploading a bundle does not close these.

### Verification history

- World 4 seventh batch: restored the final 49 static description/action pairs
  across Titan Dock and waste intake (ten descriptions). Later dock prose names
  Rylos through his insignia rather than asserting he survives his defeat.
  Intake prose retains separate quest controls, terminal, salvage and film-cache
  references. Added both rooms to the milestone/event matrix; milestone_not_set
  conditions are now explicitly checked for blocked dispatch as well as positive
  cases. Prerequisite quest tasks are seeded independently of description flags;
  actual combat, film reward delivery, routes and save-file I/O are not simulated.
  All pending room changes verified prose-only, no action/gate/reward/route edits.
  Verification: 377 JVM tests passed, zero failures/errors; 354 strict selectors
  and diff check passed. Audit: 160 overall, zero World 4; audit logic unchanged.
  This closes World 4 static discoverability triage, not device/narrative sign-off.
  Next: World 5 (86 candidates), World 6 (39), Astra (10), debug/unmapped (3).
  Earlier classified omissions remain: World 1 (1), World 2 (16), World 3 (5).
  Explicit references and route/terminology review still follow. Changes remain
  uncommitted and outside uploaded build 111; no install, version bump or release.

- World 4 sixth bounded batch: restored eight references across slag landing,
  slag river and stepping stones: compliance bulletin, crossing, crate alpha
  and flare rhythm. Expanded the selected-description/milestone/event matrix to
  these rooms. Authored positive quest prerequisites are seeded; description
  milestones remain independent of unfinished quest tasks. The compliance
  bulletin still requires active MQ16 and its own unread milestone; later
  description milestones alone do not retire it in partial states.
  All pending room edits verified prose-only. Audit: 209 overall, 49 World 4
  (down from 217/57). No suppression, progression change, commit, version change,
  push, install or upload. Next: Titan Dock and waste-intake candidates.
  Verification: 377 JVM tests passed, zero failures/errors; 354 strict selectors
  and diff check passed. Changes remain uncommitted and outside build 111.

- World 4 fifth bounded batch: restored 19 references in power-core and service-
  airlock base/variant descriptions. These retain engine mount, arrays, airlock
  door, crate gamma and engineer valve references across independent milestones.
  Extended the milestone matrix/event-dispatch regression to both rooms. Event
  conditions use quest/task completion separately from the description milestones;
  partial states can still execute these actions. Positive prerequisite tasks
  are seeded; rendered UI, real save import and earned routes remain unproven.
  All pending room edits verified prose-only. Audit: 217 overall, 57 World 4
  (down from 236/76), without suppression. No version change, commit, push,
  install or upload. Remaining World 4 candidates are the next batch.
  Verification: 377 JVM tests passed with zero failures/errors, 354 strict
  selectors passed, and diff check passed. Changes remain uncommitted, not in 111.

- World 4 fourth bounded batch: restored nine references across forge control
  alcove, bellows walk, obsidian overlook, coolant pipes and reject bay. Base and
  milestone-selected descriptions now retain the reviewed inspection names.
  Runtime regression checks selected prose/visibility and dispatches hazard-map,
  overclock-profile and rejection-code events with required milestones seeded.
  Reject-bay spared-units milestone alone does not retire reading rejection codes;
  the event independently checks SQ19 completion and codes-read milestone.
  Inspection-only actions have no event to dispatch. No route/device or save-file
  I/O proof is claimed. All pending rooms.json edits verified prose-only.
  Audit: 236 overall, 76 World 4 (down from 245/85), without suppression.
  Next: power-core variants and service-airlock candidates, then remaining
  World 4 rooms. No version change, commit, push, install or upload.
  Verification: 377 JVM tests passed, zero failures/errors; 354 strict selectors
  and diff check passed. Follow-up changes remain uncommitted, outside build 111.

- World 4 third bounded batch: restored 23 references across Cooling Springs,
  engine service ring, escape catwalk and forge/anvil descriptions. Added the
  three worker-tablet omissions; expanded Cooling Springs coverage beyond the
  earlier base-only test. Selected-description tests enumerate independent
  description/action milestones and dispatch shipped generic events with their
  positive quest prerequisites seeded. Completed tasks remain absent to model
  partial saves. Tuning references are checked for visibility/prose only; puzzle
  completion, rendered UI and save-file I/O are not established by these tests.
  All pending rooms.json changes verified prose-only against release 111.
  Audit: 245 overall, 85 World 4 (down from 268/108). No candidates suppressed.
  Verification: 376 JVM tests passed, zero failures/errors; 354 strict selectors
  and diff check passed. Changes remain uncommitted and are not in build 111.
  No commit, push, version change, install or upload. Next: remaining World 4
  rooms, then Worlds 5-6, Astra and debug rooms; explicit references and
  route/terminology review still pending.

- World 4 second bounded batch: restored 16 prose references in cooling springs
  and decontamination rooms across base and milestone-selected descriptions.
  Worker-tablet references remained unresolved in three descriptions; the
  regression excluded unmet milestone prerequisites from its base-state check.
  That check alone did not prove variant coverage or event usability. The third
  batch above closes this gap. The previously reported count of 36 was incorrect.
  No action definitions, event conditions, IDs, rewards or routes changed.
  Verification: 375 JVM tests passed, 354 strict selectors passed, diff check
  passed. Audit: 268 overall, 108 World 4. Remaining World 4 candidates are
  still batched; no commit, upload or version change.

- World 4 bounded batch: restored 16 prose references in the conveyor,
  conditioning and belt-timing rooms. These are state-independent interactions;
  event conditions remain unchanged. Added a 64-state milestone regression for
  the reviewed rooms and confirmed executable events for gated conveyor actions.
  Audit moved from 300 to 284 candidates overall; World 4 moved from 140 to 124.
  Verification: 374 JVM tests passed, 354 strict selectors passed, and diff check
  passed. The remaining 124 World 4 candidates need batched triage; no bulk
  suppression or release action was performed. Changes remain uncommitted and
  are not in Internal Testing build 111.

- World 3 discoverability pass after 111: restored 35 description/action pairs
  across 13 rooms. Added missing side-quest objects, caches, tuning console,
  roof controls and post-Lens Scan references; completion variants retain
  unrelated interactions and partial-state objectives. All changes are prose-only;
  action definitions, conditions, IDs, rewards and connections are unchanged.
  Reviewed shipped event conditions: side quests have their own quest/milestone
  gates, not blanket main-quest retirement. Quest tasks and milestones can be
  independently persisted; completion milestones alone do not prove task retirement.
  The five remaining World 3 candidates are intentional pre-MQ14 Scan omissions:
  Archive base/prism-solved, Prism base/prism-solved and drone base. All three
  Scan events require ms_w3_mq14_complete; its first-priority variants now name
  them. Runtime event tests prove blocked-before/executable-after behavior.
  Added a bounded milestone-combination regression over all 13 reviewed rooms,
  including independent action retirement/requirement and description milestones.
  It checks visibility and selected prose, not event usability for every action.
  Event usability beyond the three Scan events was reviewed from authored
  conditions, not established through complete route/device execution. No rendered
  interaction, puzzle completion or save-file I/O proof is claimed.
  Verification: 373 JVM tests passed, zero failures/errors; 354 strict selectors
  and diff check passed. Audit: 300 overall, 5 World 3; no suppression added.
  World 3's 40 candidates are classified (35 fixed, 5 gated omissions).
  Next: World 4's 140 candidates, then World 5 (86), World 6 (39), Astra (10)
  and debug/unmapped (3); explicit references and route/terminology review remain.
  Changes remain uncommitted and are not in uploaded 1.3.27 (111). No install,
  version increment, commit, push or upload performed in this pass.

- World 1 discoverability repair after 111: resolved the eight remaining pairs
  with eight prose-only edits in seven rooms. Added bunk conduit references in
  base/dark prose; tactile storage-panel/basement-chest references and backup
  terminal standby glow; retained loader diagnostic, toxic-blockage and mainframe
  terminal names in completion variants. Completion language remains historical
  rather than instructing the player to repeat completed work.
  Production `buildActionHints` only derives locks for toggle actions, not these
  generic actions; generic selection/dispatch has no blanket darkness lock.
  Runtime tests migrate seeded sessions, verify narrative visibility, dispatch
  shipped events and require selected prose to name the executable action in all
  eight cases. The backup terminal is repeatable, so execution is observed through
  EventHooks rather than the one-shot completedEvents set. Room-only completion
  flags can coexist with unfinished quest tasks after migration; the loader,
  blockage and server events execute in those partial states.
  Tests do not establish rendered UI, actual save-file import or earned routes.
  World 1's nine static pairs are now classified: eight fixed, mine-shunt base
  omission intentional and covered by its power/quest-state regression. Static
  audit is 335 overall / 1 World 1; no audit suppression. Next: World 3 (40),
  then World 4 (140), World 5 (86), World 6 (39), Astra (10), and debug/unmapped (3).
  Explicit references and route/terminology review still follow discoverability.
  All new room changes verified prose-only against release 111. No progression
  gates, events, rewards, routes, version, commit, push, install or upload changed.
  Verification: 371 JVM tests passed with zero failures/errors, 354 strict Maestro
  selectors passed, and diff check passed. Changes remain uncommitted, not in 111.

- Post-111 bounded World 1 pass: added a four-combination runtime selector
  regression for mine_shunt's crew datapad. Power absent hides it; power present
  selects prose naming it; SQ05 completion retires it. This classifies the base
  omission without editing prose or suppressing the audit. No asset changes.
  Eight World 1 candidates remain: bunk base conduit, four dark descriptions,
  and medbay/server/loader partial or completed states. Code inspection found
  darkness hides NPCs/items/service shortcuts, but inline actions are built from
  selected dark prose and generic dispatch has no blanket darkness gate. Do not
  classify dark omissions as unusable solely because the room is dark; action
  hints and event conditions still require targeted checks. World 1 remains open.
  Follow-up test and this release-outcome record are not committed or in 111.
  Verification: 370 JVM tests passed, zero failures/errors; 354 strict selectors
  and diff check passed. Audit unchanged at 343 candidates, 9 World 1 (one now
  classified). Paused at this bounded checkpoint to conserve the stated budget.

- Post-release Phase 5 ridge repair (after bundle 110): added two descriptions
  for unconfronted-hunter compatibility states and moved the Beast description
  ahead of later completion flags, guarded by Beast-not-defeated. Normal
  cumulative progression keeps its existing prose. The combined hunter/drill
  variant only names both objectives when both visibility gates permit them.
  No action definitions, event IDs, progression gates, rewards, routes or
  migration logic changed. This is description-selection work, not prose-only
  editing: description variants and their state guards changed intentionally.
  Regression first failed against release 110's missing reference, then passed
  after the repair. Added production-event coverage across 2,592 migrated flag,
  quest/task and milestone combinations, plus 108 raw flag/milestone combinations
  checking all visible ridge names and compatibility variants' retired objectives.
  Absent and explicit-false flags are both covered. Event hooks connect room/task
  updates to the session; combat and cinematics are not played on a device.
  World 2's current 16 static candidates are classified: 13 ridge pairs are
  covered by state/priority tests, 2 emitter pairs have blocked source-only events,
  and the post-launch console event is blocked by quest completion. Audit logic
  remains unchanged; none were suppressed to manufacture a zero count.
  World 1 triage started (9 candidates): mine_shunt's base crew-datapad omission
  is paired with a power-milestone variant that names it when shown. Still to
  verify: dark-room omissions (bunk conduit, storage panel, backup terminal,
  basement chest), bunk base compatibility, and post-completion/partial states
  for medbay blockage, server terminal and loader diagnostic strip. Do not
  infer event usability from their room flags alone. Then Worlds 3-6 and Astra,
  explicit action references, and route-discovery/terminology review.
  These follow-up changes are uncommitted and not in uploaded bundle 110.
  Verification: full offline JVM suite passed (369 tests, zero failures/errors),
  strict selectors passed (354), diff check passed. Static audit: 343 candidates,
  16 World 2. No follow-up install, commit, push or upload.

- Phase 5 compatibility investigation (2026-09-11): added four runtime tests
  covering ridge quest-complete versus milestone-only restoration, independently
  migrated ridge tasks, all four milestone combinations for each emitter, and
  launch without console inspection. `GameSessionPersistence` reads milestones,
  tasks and room flags independently; AppServices applies
  `migrateOpeningNarrativeState` on startup, slot/quick/autosave load and legacy
  import. The migration reconstructs ridge flags from corresponding tasks or
  completed MQ04, not from the MQ04 milestone alone. Tests exercise that
  production migration and shipped EventManager events, not save-file I/O.
  Findings superseding the ambiguity in earlier checkpoints:
  - Emitter source-only omissions (2 candidates): events explicitly require the
    source milestone to be absent. The action can remain nominally visible but
    cannot execute. Do not add prose advertising it. Retirement-only states hide
    the action; with neither milestone present the event executes; with both
    present the action is retired normally.
    Migration does not reconcile these milestone pairs.
  - Post-launch console omission (1 candidate): shipped launch can complete MQ05
    without inspection. Console remains nominally visible, but its event requires
    active MQ05 and is blocked after completion. Do not add post-launch console
    prose. Migration backfills bridge installation, not console inspection.
  - Ridge (10 candidates): normal cumulative progression remains covered.
    Completed-quest legacy states repair all three flags. Milestone-only states
    do not: Confront stalker is visible with MQ03 complete, absent from selected
    prose, and actually executable if MQ04 is active with its task unfinished.
    This is a confirmed compatibility discoverability defect, not a safe omission.
    Independent task migration can also leave noncumulative flags. These require
    a targeted description/state strategy; do not mark World 2 resolved yet.
  No prose, action gates, migration logic or other production files changed in
  this investigation. Next: cover the full noncumulative ridge state matrix and
  repair its usable-action omissions, then World 1, Worlds 3-6 and Astra. Explicit
  action references and route/terminology review remain subsequent work.
  Verification: full offline JVM suite passed (367 tests, zero failures/errors),
  strict Maestro selectors passed (354), and diff check passed. Static audit
  remains 340 candidates overall / 13 World 2; no suppression was added.
  No APK install, version change, commit, push or upload. IDE cache untouched.

- Phase 5 tenth batch (9% usage checkpoint): added
  `WorldTwoDiscoverabilityStateTest` using production `narrativeActionVisible`
  and `resolveRoomDescription`, not a duplicated static matcher. Tests cover
  cumulative ridge states: Confront stalker -> Face the Beast -> Anchor Drill ->
  Shield inspection only, with every visible action named in selected prose.
  A second test dispatches shipped emitter events and verifies they select their
  completed descriptions while retiring the respective actions. Ridge states
  are seeded, not earned through combat; no rendered UI or save-file I/O proof.
  No game assets or production code changed this batch. Static totals unchanged:
  340 candidates overall, 13 in World 2. Do not suppress these solely to reach zero.
  Classification checkpoint: normal cumulative ridge progression and authored
  paired-emitter event paths are covered; compatibility states remain unresolved.
  In particular, MQ04-complete with missing ridge flags can leave Confront stalker
  visible but absent from the milestone-only description. A source-only emitter
  milestone likewise does not independently hide its action. Post-launch hangar
  text omits dark console while visibility only checks the inspection milestone;
  launch does not directly require that milestone. These require targeted
  compatibility decisions/tests, not wholesale insertion of retired objectives.
  Next: narrowly resolve completion/partial-state visibility, then proceed to
  other worlds. Phase 5 and World 2 remain open; Phase 4 still needs phone acceptance.
  Verification: full JVM suite passed (363 tests, no failures/errors), strict
  Maestro selector validation passed (354), and diff whitespace check passed.
  No install, commit or upload; retained a bounded checkpoint for remaining usage.

- Phase 5 ninth batch (usage-conserving checkpoint): restored eight conditional
  references across seven rooms: locker, muddy clearing, cargo panel, still
  basins, central relief, stasis mural overview, bridge relic and launch cradle.
  Expanded the conditional catalog regression for all eight. No action/event
  definitions, gates, rewards or routes changed. Audit: 340 candidate pairs;
  World 2: 13. No device install, commit or upload.
  Verification: 361 JVM tests passed (no failures/errors), strict Maestro selector
  validation passed (354), and diff whitespace check passed. No device proof.
  Remaining World 2 classification:
  - Two emitter variant[0] omissions match shipped events that set both the
    source milestone selecting that variant and the action-retirement milestone.
    Safe for that authored event path; independently seeded/legacy flags are
    not proven equivalent. Do not re-expose completed emitter work blindly.
  - Ten canopy-ridge candidates depend on show/hide state gates and variant
    priority. Several are intentional, but milestone-only/legacy-state combinations
    still need explicit review before declaring this group fully classified.
  - One hangar post-launch dark-console omission needs the same legacy/route
    review. Launch checks reboot completion, not the console-inspected milestone
    directly; do not assume the two are identical.
  Next checkpoint: resolve these 13 candidates with state/compatibility evidence,
  then move to the remaining worlds. World 2 and Phase 5 are not yet signed off.

- Phase 5 eighth batch: restored nineteen missing references across crash-site,
  canopy, canopy ridge and stasis chamber states. Wreckage, glow-water and Shield
  inspection persist; pod examination no longer strands moss scanning, fern
  scanning retains spore/scout references, and stasis objectives retain unrelated
  pod/root/vent references. Expanded catalog checks to those conditional states
  and added ridge stage/gate regression coverage. Did not reinsert Confront stalker,
  Face the Beast or Anchor Drill into stages where their authored gates hide them.
  The static audit does not model these state gates or variant priority: the 21
  remaining World 2 candidates include intentional omissions, not just fixes to do.
  All 48 changed rooms remain prose-only against HEAD. Audit total: 348 candidates.
  Verification: 361 JVM tests passed, strict Maestro selector validation passed
  (354), and diff whitespace check passed. No rendered interaction proof claimed.
  No device install, commit or upload. Next: remaining conditional inspections
  (pod interior, brush/drop/pools, echo hall, observation, emitters and hangar),
  then classify the ridge's remaining state-dependent candidates explicitly.

- Phase 5 seventh batch: restored fifteen action names in twelve facility base
  descriptions, plus bus bar array references in both breaker-deck variants.
  Expanded the reviewed-action regression to 57 actions and conditional coverage
  to both breaker completion milestones. Inspected the shipped breaker event:
  it sets both source-breakers-starved and breakers-overloaded; its existing
  action retirement gate is unchanged. The campaign integration suite already
  exercises the breaker action during World 2's departure sequence.
  Verified all 44 changed rooms against HEAD: only description text differs;
  actions, gates, rewards, routes and events are unchanged. Audit: 367 candidate
  pairs, World 2: 40. These are review candidates, not confirmed bugs or UI proof.
  Verification: full JVM suite passed (360 tests, no failures/errors), strict
  Maestro selector validation passed (354), and diff whitespace check passed.
  No device install, commit or upload. Next: multi-state crash-site, canopy and
  stasis descriptions and the remaining conditional inspection references.

- Phase 5 sixth batch: restored eight missing description references across
  beach pools and grotto. Weed scanning no longer removes the separate tideglass
  gathering reference; cache inspection retains the separate prism and arches.
  Anemones are named in both pools descriptions. Corrected the grotto's false
  "salvaged" claim: the cache event only reports inert crystals and sets its
  inspection milestone; the prism awards the actual relic mod separately.
  Expanded conditional-prose checks and added shipped-event regressions for
  scan-then-gather and inspect-then-prism-success, including payout replay checks.
  These seed quest prerequisites/dispatch puzzle success, not route or slider UI
  verification. No gameplay event, reward, gate or connection changes.
  Audit: 384 candidate pairs, World 2: 57. No device install, commit or upload.
  Verification: 360 JVM tests passed, strict Maestro selector validation passed
  (354), and diff whitespace check passed. All 32 changed rooms remain prose-only
  against HEAD. Next: remaining World 2 facility base descriptions, then the
  multi-stage crash-site/canopy/stasis descriptions.

- Phase 5 fifth batch: restored thirteen description references for seven
  interactions across six rooms. Canopy, crystals, moss, stump and trunk
  inspections now remain named after their unrelated local objectives; both
  World 2 film cache names are exposed, including the cave's harvested variant.
  Resolved the deferred film-cache concern: GameEvent defaults to nonrepeatable,
  and EventManager skips completed events. A regression using shipped room/event
  definitions verifies each film is granted exactly once, including replay after
  in-memory session restoration (not save-file I/O or rendered UI verification).
  Added conditional-prose regression coverage for five completed-objective states.
  All thirty changed rooms remain prose-only against HEAD; no action definitions,
  gates, rewards, routes or events changed. Audit: 392 candidate pairs, World 2: 65.
  Full JVM suite passed (358 tests, no failures/errors); strict Maestro selector
  validation passed (354); diff whitespace check passed. No device verification,
  install, commit or upload. Next: beach pools/grotto conditional actions, then
  remaining World 2 candidates. Beach gathering uses trigger action
  `w2_sq03_gather_tideglass` on event ID `w2_sq03_gather`; do not mistake the
  different event ID for a missing trigger.

- Phase 5 fourth batch: restored nineteen missing action names across thirteen
  stream, wilds and ridge base descriptions, including the pre-harvest blue-frond
  cluster. Expanded the catalog regression to thirty reviewed actions; use a
  list of pairs so multiple actions in one room are all tested. Verified all
  24 changed rooms against HEAD: only description text differs, with action
  definitions, events, gates, rewards and connections unchanged.
  Audit: 405 candidate pairs, World 2: 78; other world counts unchanged.
  Ridge Plateau's film cache remains deferred: its event has no authored
  conditions, so check runtime repeat-collection handling before exposing it.
  Narrative review also needs to check the existing Thermal Cutter shortcut/cache
  inspection messages, which describe cutting but have no action event or item gate.
  Verification: full JVM suite passed (356 tests, no failures/errors), strict
  Maestro selector validation passed (354), and diff whitespace check passed.
  No device verification, install, commit or upload this batch.

- Phase 5 third batch: named warm sand and the resonant pillar in their base
  descriptions, and wind-sculpted ridges in both base and harvested dunes prose.
  Added a harvested-state catalog regression that preserves the grazer's
  retirement gate and absence from harvested prose while retaining ridge discovery.
  All eleven changed rooms remain prose-only: action definitions, milestone
  gates, rewards and routes are unchanged. Audit: 424 candidate pairs, including
  97 in World 2. Full JVM suite passed (356 tests, no failures/errors); strict
  Maestro selector validation passed (354); diff whitespace check passed.
  Static checks only; no device verification, install, commit or upload.

- Phase 5 second batch: reviewed five sole/base-description omissions in World
  2 and named the existing basalt cliffs, rusted hull, mineral shelter,
  maintenance spores and shaft shortcut interactions in their room prose.
  No action IDs, gates, rewards, routes or dialogue changed. Expanded the
  reviewed-room catalog regression to all eight repaired references.
  Audit now reports 428 candidate pairs (World 2: 101); other world counts
  unchanged. This is static discoverability coverage, not device or route proof.
  Full JVM suite passed; strict Maestro selector validation passed (354), and
  diff whitespace check passed. No build install, commit or upload this batch.

- Phase 5 first pass: added `scripts/audit_action_discoverability.ps1` covering
  all rooms and grouping candidates by node/hub world mapping. It checks base,
  dark and conditional description text, mirrors literal name variants, excludes
  service/arcade controls, and recognizes direct milestone contradictions.
  It does not simulate state, variant priority, NPC overlap, or route reachability;
  intentional dark-state/retired-action omissions can remain candidates.
  Unlike the World 1 validator, it does not silently acknowledge known omissions.
  `-SummaryOnly` gives bounded output; `-Strict` exits 1 on candidates (verified).
  Three World 2 actions absent from their sole descriptions now appear in prose:
  service map (sector9_conduit_junction), Aethel extinction log
  (sector9_archive_vault), research terminal (sector9_archive_reading_room).
  No action names/IDs, gates, rewards, routes or dialogue changed. Added a
  catalog regression for their action definitions and matching description text.
  Remaining scan: W1 9, W2 106, W3 40, W4 140, W5 86, W6 39, Astra 10,
  unmapped 3 = 433 candidate pairs. Do not interpret these as bug counts.
  Next: prioritize base-description omissions in World 2, then conditional
  cases and explicit-reference compatibility. No device verification claimed.
  Verification: full JVM suite passed (355 tests); strict Maestro prose selector
  validation passed (354); diff whitespace check passed. No commit or upload.

- Emulator Phase 4 closeout: installed the rebuilt debug APK with stacked
  AbilityDetailLine label/value presentation and ran
  `phase4_large_text_combat.yaml` at system font scale 2.0. All required commands
  passed, including level-2/max Overcharge, Arc Tether cooldown/traits scrolling,
  Plasma Burst name/control access and Details, and return scrolling to Smoke
  Bomb. Screenshots reviewed: complete readable cooldown suffix, Plasma Burst
  name/summary/buttons and Smoke Bomb at the top; Back/Close remain reachable.
  Earlier name failures were resolved by a 70% scroll visibility threshold;
  the saved failure hierarchy already contained visible Smoke Bomb bounds.
  Screenshot review remains required, not replaced by relaxed semantics.
  Font scale restored to 1.0. This closes the scoped emulator group, not every
  skill/device or combat balance. Physical-device acceptance remains open;
  only emulator-5554 was connected. The uploaded internal 109 bundle predates
  these uncommitted UI fixes. No new production changes/build needed in this
  verification pass; prior build/JVM results remain 354 passing tests.

- Crowded/Overcharge large-text follow-up: saved Maestro results from
  2026-09-11_115317 confirm level-2 and maximum Overcharge flows plus max detail
  scrolling passed at system font scale 2.0. Screenshots show the complete max
  banner, Level 3 detail heading, and reachable cooldown/traits/Back control.
  Visual review found the detail value column splitting the Overcharge suffix
  mid-word. AbilityDetailLine now stacks label/value at font scales >= 1.5;
  assembleDebug and testDebugUnitTest passed, but this new change has NOT yet
  been installed or visually rechecked. Prior emulator screenshots predate it.
  The full crowded-list flow failed: reaching the Plasma Burst Details button
  did not leave its name in view. New checkpoint helpers separate name/control
  checks, but retry 2026-09-11_121859 still reported Smoke Bomb absent while its
  failure screenshot shows it clearly. Investigate accessibility selectors
  before claiming full-list scroll coverage; no successful combined rerun.
  Font scale restored to 1.0 by finally blocks. Strict selector validation
  passed (354). No commit, push or upload. Phase 4 still has the emulator
  detail/list closeout and physical-device acceptance groups open.

- Large-text follow-up (Android 17 Medium Phone emulator, system font scale
  2.0): fixed four presentation issues found by screenshot review, not just
  semantic assertions. Debug scenario headers now scroll with their results
  and category filters scroll horizontally; ability controls stack below the
  copy at font scales >= 1.5; quest badges wrap and stage titles are no longer
  one-line ellipsized; inventory category tabs scroll horizontally at large
  text sizes instead of breaking words. Gameplay rules and content are unchanged.
  `AbilityRowInstrumentedTest` now uses 270 dp width and asserts the large-text
  controls sit below the explanation: both tests passed using direct adb
  instrumentation. Debug build and 354 JVM tests pass; strict Maestro selector
  validation passes (352). The combined `phase4_large_text.yaml` preserves app
  data, replaces unsaved debug sessions, and requires external font setup/restore.
  Final-build Loader list/details flow passed at 2.0; screenshots show the
  intact ability name, full summary, and scrolled cooldown/traits/Back control.
  Final-build opening journal flow also passed: screenshot review confirms
  the complete TRACKED badge and scroll access to the full final cutter
  objective and reward list. Stage guidance wraps and can be scrolled; the
  current-stage screenshot is a scrolled excerpt, not the whole section at once.
  Final-build starter loadout flow passed and screenshots confirm readable
  empty/equipped tiles, selectors, locked mod controls and story-gate copy.
  Both Mining Pistol and Flux Liner were equipped in the unsaved debug session.
  Combined flow exited 0; its optional third Continue was absent (warning only).
  Font scale was restored to its original 1.0 and verified afterward. The debug
  game remains installed. No app-data clear, save operation, commit or upload
  was performed in this pass. Existing unrelated IDE device cache edit retained.
  This is a single-ability tutorial checkpoint, not a crowded multi-ability or
  maximum-Overcharge large-text audit. Physical-device acceptance remains open.

- Maximum-charge detail scrolling verified with checkpoint helper
  `phase4_overcharge_detail_scroll.yaml`: all commands passed. First cooldown
  visibility match did not scroll enough visually, but scrolling to Combat
  traits exposed the full cooldown suffix, applied Shock and traits with the
  Back to Abilities control still accessible. Screenshots reviewed; current
  emulator max-charge detail scrolling gap closed, not a large-font sign-off.
  Bounded shipped-catalog review narrowed remaining disabled-state scope:
  no skills have uses_per_battle; active player conditions are only
  bonus_if_target_staggered / bonus_if_target_stunned, which do not block use.
  HP-gated entries are passive or enemy skills. Battle-limit and requirements
  messages retain synthetic coverage in OpeningCombatRuntimeTest; their device
  display is deferred until applicable active content exists, not called tested.
  Jammed and Silenced both have block_skills and remain the applicable status
  UI check. Phase 4 remaining: status-blocked ability presentation and
  large-text/physical-device verification. Other phases unchanged.
  No production changes or build/JVM rerun in this pass.

- Phase 4 maximum-charge banner verified: `phase4_max_overcharge.yaml`
  passed end to end, including the level-2 flow and a third basic attack.
  Screenshots show readable MAX OVERCHARGE and Level 3 banners (+80%, refund,
  critical-hit text) and boosted list preview. The detail cooldown suffix is
  below the initial viewport; scrolling that full section remains part of
  layout/accessibility verification, not claimed visually complete here.
  No overcharged ability was consumed. Non-cooldown disabled reasons and
  large-text/physical-device checks remain; other phases unchanged.
  Fixed static selector false positives by expanding the actual cooldown
  formatter template for singular/plural labels, rather than allowing stale
  strings. Strict validation now passes (350 selectors); diff check passes.
  No production changes/build/JVM rerun in this automation-only pass.

- Phase 4 level-2 Overcharge presentation verified: `phase4_overcharge.yaml`
  passed end to end after two Nova basic attacks in the First Combat debug
  checkpoint. Screenshots show the +50% banner, boosted Arc Tether power and
  one-turn cooldown refund; list/detail cooldown labels match and are readable.
  Detail content extends below the viewport and remains a scrolling layout;
  this pass only inspected the visible Overcharge and cooldown section.
  No production fix/rebuild needed. Not a maximum-charge or charge-consumption
  check. Remaining combat UI coverage: max-charge presentation and non-cooldown
  disabled reasons; large-text/physical-device group also remains. Phases 2-3
  acceptance gaps and mostly-pending Phases 5-6 unchanged.
  Verification: Maestro passed; diff check passed. Strict prose-selector check
  reports two false positives for dynamically composed "1 turn after use" in
  the new flow; not claimed clean. Existing JVM tests cover Momentum rules and
  cooldown label calculation; suite was not rerun for this automation-only pass.

- Phase 4 opening-guidance presentation check closed at current emulator
  settings. `phase4_opening_guidance.yaml` passed end to end, including menu
  hints, Journal -> Wake Up Call, grouped prepare/repair/test overview,
  current-stage instructions, and scrolling to the final cutter objective.
  Reviewed all four checkpoint views: copy wraps readably and final objectives
  are accessible. No production fix needed. This uses the Gear & Inventory
  debug checkpoint, whose earlier tasks remain unchecked despite placing Nova
  at the workshop; it does not establish earned progression or route correctness.
  No tracking toggle/save/data reset. Strict selector validation passes (343),
  whitespace check passes; no production edits, so JVM/build were not rerun.
  Phase 4 remaining groups: (1) Overcharge and non-cooldown disabled reasons,
  (2) large-text and physical-device verification. Phases 2-3 acceptance gaps
  and mostly-pending Phases 5-6 remain unchanged. No whole-phase sign-off.

- Phase 4 ability-name truncation fixed: ability rows allow names to wrap.
  Rebuilt/installed APK and ran `phase4_nonweak_cooldown.yaml` end to end:
  every required command passed, including the two-turn disabled-parent and
  matching detail-label assertions. One-turn conditional branch was skipped.
  Screenshots confirm Smoke Bomb and Plasma Burst display full names on two
  lines; Arc Tether countdown, explanation and buttons remain readable. Taller
  rows reduce the number visible at once; this is not a large-font/full-list audit.
  Long-name finding at current emulator settings and combined-flow rerun are
  closed. Verification: 353 JVM tests, assembleDebug, 339-selector validation
  and whitespace checks pass. Removed the now-unused TextOverflow import after
  that build. Remaining Phase 4: Overcharge/other disabled reasons, opening quest
  guidance, large-font and physical-device checks. Phases 2-3 acceptance gaps
  and mostly-pending Phases 5-6 unchanged; no whole-phase sign-off.

- Phase 4 nonzero-cooldown UI checkpoint verified on emulator-5554:
  Arc Tether against Shock-resistant Echo-Borers displayed "Ready in 2 turns"
  and "On cooldown"; Maestro confirmed the parent button containing 2 is
  disabled and the detail label matches. Both screenshots reviewed, readable.
  Added `phase4_nonweak_cooldown.yaml` and `phase4_assert_arc_cooldown.yaml`.
  Navigation needed a cinematic wait and a skill-specific Use selector (the
  first ability is Smoke Bomb). The original one-turn expectation was not
  met; this is presentation coverage, NOT turn-timing verification. Final
  assertion helper passed from the reached screen; the revised combined flow
  still needs one end-to-end rerun. One-turn branch remains unexecuted.
  New layout finding: some longer ability names truncate in the full-inventory
  list. Remaining Phase 4: Overcharge/other disabled reasons, opening guidance,
  long-name and large-font layouts, physical-device acceptance, and combined
  cooldown-flow rerun. Phase 2-3 balance/economy acceptance gaps and mostly
  pending Phases 5-6 unchanged. No production edits or rebuild in this pass.

- Phase 4 post-Arc-Tether inspection exposed another tutorial copy mismatch:
  success text claimed Arc Tether was on cooldown, but its weakness refund plus
  the normal turn-end reduction clears the two-turn cooldown. The emulator
  showed Use enabled at Nova's next turn; production cooldown code confirms
  both reductions. Corrected the success text to explain weakness refunds
  without claiming the skill is unavailable. No balance rules changed.
  Added `phase4_loader_cooldown_refund.yaml`: reuses the label check, executes
  Arc Tether, checks success copy and next-turn list/detail labels, and requires
  Use enabled with no "On cooldown" message. Bounded portrait-tap retries
  handle the visible-but-not-yet-ready interval. This scenario does NOT cover
  the disabled-cooldown state; use a non-weakness encounter for that check.
  Verification: 353 JVM tests pass, assembleDebug passes, APK installed with
  data retained; strict selector validation passes (339 selectors), diff check
  passes. Final Maestro rerun passed all commands after resuming the interrupted
  session. Reviewed success-copy and post-refund list/detail screenshots: text
  fits, Use is enabled, and the list/detail cooldown labels agree. This bounded
  tutorial-copy/refund regression is closed; nonzero cooldown is not verified.
  Phase 4 remaining: nonzero cooldown and other disabled reasons, Overcharge,
  opening quest guidance, and large-font/physical-device layouts. Phases 2-3
  acceptance gaps and mostly-pending Phases 5-6 are unchanged.

- Phase 4 Maestro recheck passed on emulator-5554 with the rebuilt APK.
  Added `playtests/maestro/phase4_loader_labels.yaml`, preserving app data and
  selecting the Gear & Inventory scenario without keyboard input. The first
  attempt was interrupted by Android handwriting-keyboard onboarding; the
  scrolling-based rerun passed every command. Reviewed all three screenshots:
  corrected Loader briefing is readable, Arc Tether's full summary wraps onto
  two lines including "2 turns after use", and detail targeting/cooldown fit.
  These two presentation-fix rechecks are closed. No combat action was used.
  Remaining Phase 4: additional cooldown/disabled/Overcharge states, opening
  quest guidance, and large-font/physical-device checks. Phases 2-3 retain their
  campaign affordability/economy, finale balance and player/device acceptance
  gaps; Phases 5-6 remain mostly pending. No whole-phase sign-off.
  Verification: focused Maestro flow passed; strict selector validation passed
  (338 prose selectors); diff whitespace check passed. No production code changed
  in this pass, so the previously passing JVM suite was not rerun.

- Corrected gear scenario visually verified on the connected emulator: arrived
  at Scrap Yard (workshop_yard), inventory and gear tutorials displayed readable
  text and dismissed with Continue, Flux Liner and Mining Pistol appeared in
  selectors and equipped successfully. Equipped tiles show names, locked mod
  controls and story-gate text. This validates the debug UI path at current
  settings, not campaign acquisition, unlocked mods or large-font layouts.
  Entered Faulted Loader combat: Arc Tether detail target selection and cooldown
  are readable. Found list summary truncating before cooldown and loader briefing
  incorrectly asking for Attack while tutorial requires Arc Tether. Removed the
  summary's one-line truncation and corrected loader-specific briefing copy.
  These two final presentation fixes needed visual recheck (now passed above).
  Phase 4 remaining: that recheck, additional cooldown/disabled-action states,
  opening quest guidance, and physical-device/large-font checks. No phase sign-off.
  Verification: full JVM suite and assembleDebug pass after presentation fixes;
  whitespace checks pass, emulator crash buffer empty. New APK was subsequently
  installed and visually verified with Maestro as recorded above.

- Phase 4 emulator visual pass started (Medium Phone API 37.1, debug 1.3.24/108).
  Installed current debug APK without clearing data. Title screen, inventory
  supplies, empty Nova loadout and empty armor selector render and respond.
  Empty weapon/armor mod hints fit without clipping at the observed settings.
  This is not large-font, physical-device or complete loadout acceptance.
  Found stale Gear & Inventory debug bootstrap: nonexistent pit_landing room,
  task ID used as stage ID, obsolete item IDs and missing unlocks. It fell back
  to Nova's Bunk and could not exercise equipment. Updated only that bootstrap
  to workshop_yard/report_to_jed, current starter item IDs, required unlocks and
  the tutorial milestone; updated its description. Saved slots were not touched.
  Remaining: rerun corrected scenario, equipped loadout and tutorial interaction,
  opening guidance and combat-label visual checks. No Phase 4 sign-off yet.
  Verification after bootstrap repair: full JVM suite and assembleDebug pass;
  diff whitespace checks pass. Updated APK installed successfully. The corrected
  scenario has not yet been relaunched/visually verified. No crash log entries
  were returned during the inspected pre-repair UI session.

- Phase 4 bounded tutorial-trigger review performed. QuestAssetDataSource loads
  quests.json, which has no task tutorial IDs; stage-wide references found in
  quests_base.json are not the shipped quest source. Shipped events reference
  seven scripts: movement, menu_save, scene_market_locator, source_art_unlock,
  world2_debuffs, link_unlock and rest_recovery. Only world2_debuffs and
  link_unlock have delayed second steps; their guidance is not tied to acting
  on a departed room/menu. Bag and gear entry scripts are single-step. No new
  context-cancellation rule is justified by these active scripts. Archived save
  data and future script additions are not covered by this finding.
  Added shipped-asset regression for both delayed event scripts, checking first
  step preservation for slow readers, second-step arrival for fast readers and
  completion only after the final dismissal. No production changes this pass.
  This closes the bounded code-level trigger review, not player pacing acceptance.
  Phase 4 remaining: on-device opening/tutorial, loadout and combat-label checks,
  plus concrete fixes from those checks. Phases 2-3 risks and 5-6 remain unchanged.
  Verification: all 353 JVM tests pass, zero failures/errors/skips; whitespace
  checks pass. Lint and visual/device checks not rerun this pass.

- Phase 4 delayed system hints: SystemTutorialCoordinator now rechecks the
  tutorial setting after its delay. Disabling hints during that wait suppresses
  the popup and invokes the event continuation without marking a tutorial seen
  or complete. Tests also retain the enabled path's wait-for-dismissal behavior.
  Contextual pacing is NOT fully signed off: room/menu changes during multi-step
  scripts and stage-wide tutorial scheduling still need a bounded trigger review.
  Remaining Phase 4 checks: (1) that trigger review, (2) on-device opening,
  inventory/loadout and combat-label review, including text wrapping and tutorial
  timing. Fix concrete findings, then assess Phase 4 acceptance; do not reopen
  unlimited cosmetic iterations. Phase 2-3 acceptance risks remain unchanged.
  Verification: all 352 JVM tests pass, zero failures/errors/skips; whitespace
  checks pass. Lint and device checks were not rerun this pass.

- Phase 4 ability explanation pass: disabled rows show the first blocking reason
  from the same checks used by canUseSkill (status, cooldown, battle limit or
  conditions); tutorial restrictions have a separate instruction. Target details
  now explicitly describe target selection, removing tag-based guesses about
  self/group effect recipients. This does not change combat eligibility or
  targeting. Tests cover availability/reason consistency and all selection labels.
  The initial cooldown/target-selection/disabled-reason implementation pass is
  done; per-effect targeting descriptions are not claimed audited. Phase 4 still
  needs contextual tutorial pacing and visual/device verification, including
  wrapping of these new labels. Phases 2-3 acceptance gaps and Phases 5-6 remain.
  Verification: all 350 JVM tests pass, zero failures/errors/skips; whitespace
  checks pass. Lint and device checks were not rerun for this pass.

- Phase 4 combat cooldown labels: ability list and detail panel now share a
  formatter distinguishing current lockout ("Ready in N turns") from cooldown
  after use. Both preview the engine's one-turn Overcharge refund at two or
  more Momentum, including zero cooldown. Active cooldown is not discounted
  by current Momentum. Three regressions cover these cases and singular labels.
  Combat rules are unchanged. Target descriptions and disabled-action reasons
  still need review; contextual tutorial pacing and device/visual checks remain.
  Verification: all 348 JVM tests pass with zero failures/errors/skips; diff
  whitespace checks pass. Lint and device checks were not rerun for this pass.

- Phase 4 tutorial-disable behavior: turning tutorials off now cancels scheduled
  hints and discards active/queued tutorial prompts. Reward/item prompts are
  preserved, and cancellation does not invoke completion callbacks or mark
  skipped steps complete. Regression coverage checks both an active tutorial
  and an active reward, delayed hints, and eligibility to show a skipped hint
  again. This is a targeted interruption fix; contextual timing/pacing and
  device verification remain open. Combat-label review is still pending.
  Verification: full JVM suite passes (345 tests, zero failures/errors/skips);
  diff whitespace checks pass. Lint and device checks not rerun this pass.

- Phase 4 tutorial queue fix: marking a tutorial key or script complete now
  removes its obsolete queued prompts without firing unseen dismissal callbacks.
  Explicit complete-and-dismiss removes queued steps before promoting the next
  prompt; unrelated prompts remain intact. Two regressions cover script matching,
  callback suppression, preserving an unrelated active prompt and repeat blocking.
  Verification: full JVM suite passes; diff whitespace checks pass. Device and
  lint checks not rerun for this pass. This closes the stale-queue defect, not
  the entire tutorial pacing review. Phase 4 still needs pacing review,
  combat-label clarity and visual/device verification of opening/loadout changes.
  Phases 2-3 retain the acceptance risks below; Phases 5-6 remain mostly pending.

- Generic armor eligibility fixed: shared GearRules now accepts generic armor
  for party members while retaining character-specific restrictions. Both armor
  picker and equip handler use that rule; unlock requirements remain. Catalog
  regressions cover Heat Liner, Flux Liner ownership, and wrong-slot rejection.
  Avatar fixture now asserts the same eligibility rule. This resolves the
  compatibility mismatch below, not full device/shop or finale acceptance.
  Verification: all 341 JVM tests pass. Lint/device checks not rerun. Phase 4
  remains open for tutorial interruptions, combat labels and visual verification,
  plus any findings from review of the implemented guidance/loadout changes.

- Phase 4 loadout presentation: empty weapon/armor tiles replace two unusable
  mod chips with an instruction to equip the base item, and use a smaller minimum
  height. Equipped item names can wrap to two lines. Equipped mod controls and
  story gates remain unchanged. Pure presentation regressions cover hint rules;
  Compose/device visual verification remains outstanding.
  Verification: all 339 JVM tests pass; diff whitespace checks pass. Lint and
  device checks were not rerun for this UI pass.

- Acceptance caveat found during UI inspection: GearRules.matchesSlot requires
  character-specific armor types, while Heat Liner is generic armor. Earlier
  seeded combat comparisons installed it directly on Orion, bypassing picker
  eligibility. Those results are not proof that this armor allocation is UI-
  attainable. Do not silently treat the prior owned-slot assertion as full gear
  compatibility validation; retain this issue for the loadout/acceptance review.

- Phase 4 started: opening quest overview now groups preparation, cutter repair,
  and bypass testing; workshop-stage description covers its full task sequence.
  Text only; IDs, rewards and task order unchanged. Four campaign tests and both
  asset checks pass. Remaining: tutorial interruptions, loadout presentation,
  combat-label clarity and visual/device verification. This is an initial copy
  improvement, not completion of the opening-guidance workstream.

### Bounded Phase 2-3 closeout (supersedes open-ended variant work)

1. Avatar investigation completed with limits: one four-weapon catalog-priced
   build costs 1,800 of 2,140 credits (340 left), retains earned armor and spends
   five AP legally. Cryo Vent/Hydraulic Kick priorities and conditional Link
   recovery win seeds 1-4, lose seed 5, repeated identically. No boss nerf justified
   from this result. Shop navigation/prior spending and player win rates unproven.
2. Next: one World 6 earned-checkpoint boss pass.
   Pass performed: disk-restored checkpoint has 29,800 XP, level 12, 9 AP and
   2,640 credits. Five repeated runs with retained defensive gear lose to Vale
   in 1.5-2.5 seconds; God phase is never reached. Credits/AP remain unspent.
   Record as an unresolved finale balance risk, not readiness or full coverage.
   No additional build search authorized by the bounded checklist; carry this
   evidence into the economy/acceptance decision.
3. Economy decision recorded: retain general prices/rewards/XP thresholds;
   evidence supports targeted fixes, not a global economy rebalance. Avatar's
   1,800-credit build leaves 340 only under the no-prior-spending baseline.
   Full-route affordability and finale balance remain explicitly unapproved.
4. Then: final tests/validators/lint and list outstanding device/player checks.

All 337 tests pass after Avatar closeout; no production changes this pass.
Three of four closeout items remain. This is not a percentage of the whole game.
Update: the World 6 diagnostic pass is now performed. Two checklist activities
remain (economy decision, final verification), but the finale risk is unresolved
and must stay visible in any Phase 2-3 handoff. Full suite: 337 tests pass.
Latest: economy decision is recorded in ECONOMY_AUDIT.md. Only final verification
remains on the bounded checklist; unresolved balance/access evidence remains
outside that checklist and prevents claiming full Phase 2-3 acceptance.

Final bounded verification: 337 JVM tests pass, asset integrity and World 1
validators pass, Android lintDebug passes, and diff whitespace checks pass.
The four closeout activities are performed. Phases 2-3 are NOT fully accepted:
finale readiness/second-phase coverage, continuous spending/shop access and
renewable-input economy evidence remain unresolved. Device/player verification
was not performed. Move implementation focus to Phase 4 opening/interface while
keeping these explicit acceptance risks visible; do not present this as release
approval or silently expand the closeout into more build variants.

- Avatar AP allocation checked: spend saved five AP legally on Nova Quiet Steps/
  Night Cloak and Zeke Training Session/Motivational Speech/Budgeting, retaining
  earned gear and supplies. All five repeated runs still lose (40-55.5 seconds).
  Inventory/credits unchanged by purchases; all 337 JVM tests pass. This closes
  one legal AP comparison, not optimal allocation or balance acceptance. Next
  prioritize shop-equipped offense and policy suitability; broader coverage and
  affordability sign-off remain open.

- Avatar trace exposed/fixed party-restoration targeting: an explicit selected
  actor narrowed Ration Pack to one recipient. Party-support items now retain
  authored ally scope. Regression covers one consumption, living allies, max-HP
  clamp, no revival and no enemy healing. All 337 JVM tests and both asset checks
  pass. Earlier ration measurements are historical. Avatar earned-gear runs still
  lose; AP/shop allocation and broader affordability work remain outstanding.

- Avatar earned-gear comparison: equip owned Heat Liner on Orion and Grav-Boots
  on Gh0st, retaining Nova's Flux Liner. Ownership/slot and unchanged inventory/
  credits checked; all five repeated seeds still lose (35.5-52 virtual seconds).
  All 336 tests pass. Avatar's advertised Purge Mode has no named implementation
  in searched combat code; catalog uses three shared attacks. Review description
  intent, action/damage evidence and legal AP/shop options before balance tuning.
  No remaining phase package is closed by this comparison.

- World 5 baseline added: reward-linked pre-Avatar checkpoint survives disk
  round trip at level 10, 17,930 XP, 5 AP, 2,140 credits. Five repeated production
  combat runs lose using the World 4 policy and only retained Flux Liner equipped.
  These are measured defeats, not suite failures (336 tests pass). Next audit
  Avatar counterplay and an attainable loadout before tuning; unspent resources
  and policy mismatch prevent a balance conclusion. Broader coverage advances,
  but all three remaining Phase 2-3 packages are still open.

- Earned AP allocation comparison completed: spend the saved two shared AP on
  Quiet Steps/Training Session through prerequisite/tier/balance checks. No AP
  grants, inventory additions or credit spending. With owned supplies, all five
  seeds clear Golem/Titan both with and without camp. Supply efficiency is mixed,
  so no optimal-build claim. Remaining: shop/resource-allocation decisions,
  broader encounter coverage, and final affordability verification.

- Owned-supply comparison: saved checkpoint's five Medkit I and ten Ration Packs
  can now participate in a separate policy. Party rations are used when at least
  two living members lack 35 HP; Medkit I follows ordinary medkit exhaustion.
  Per-item consumption is reported and conserved after victories. Five paired
  seeds clear both fights in four cases without camp, all five with camp. No
  purchases/AP upgrades or gameplay changes; full suite 336 passing. Remaining:
  legal resource allocation, broader encounters, and affordability/sign-off.

- Saved-checkpoint attrition comparison: added Golem-to-Titan sequences with and
  without the real one-time camp event. HP, medkit inventory and credits carry
  through victories. Five paired/repeated seeds: three clear both without camp,
  four with camp; seed 3 loses to Golem. All 336 JVM tests pass. This completes
  the first constructed cumulative sequence, not full-route spending. Policy
  still leaves AP, credits and some owned supplies unused. Broader encounters
  and affordability sign-off remain open; Phases 4-6 mostly pending.

- Saved level-eight checkpoint now feeds production Titan combat directly,
  retaining earned inventory/unlocks and equipping one retained Flux Liner.
  Five seeds, each replayed, all win using 0/1/1/1/1 medkits. No purchased gear,
  extra XP or AP purchases. All 336 JVM tests pass. This closes the first saved
  campaign-to-boss bridge; prior route attrition/spending remains unmodeled.
  Remaining packages: extend checkpoint realism to cumulative spending, broaden
  encounter coverage, then affordability decisions and final verification.

- Pre-Titan correction: reward-linked route reaches 10,450 XP, level 8, 2 AP,
  1,070 credits before Titan, with disk-restored state and earned-tier checks.
  Earlier level-nine combat fixtures are not established by this route (550 XP
  short); they also omit some story unlocks. Next run combat from this actual
  snapshot with legal equipment/spending before balance decisions. Other optional
  or unsimulated battles may add XP; no global level-cap/difficulty change made.

- Production battle-reward checkpoint: separate World 1-3 route now generates
  and pays supplied victories through CombatViewModel, retaining real protobuf
  saves between worlds. Result: 7,050 XP, level 7, 2 shared AP, 1,070 credits.
  Event-only ledger stays separate. This confirms credit/AP provenance, not
  battle success or spending. Next carry progression to the World 4 pre-boss
  point, then incorporate purchases/consumption. All three major packages remain
  open; this advances checkpoint realism rather than closing Phase 2 or 3.
  Verification: all 336 JVM tests pass; diff whitespace checks pass. No production
  changes this pass; validators, lint and device checks were not rerun.

- Event-earned checkpoint follow-up: report now includes restored inventory/AP
  after all six worlds. A continuous World 1-3 test retains and equips earned
  Flux Liner with a real protobuf disk round trip, without buying replacement.
  The route grants two liners (starter kit plus patch action); retained unchanged
  pending opening-intent review. Event-only checkpoint is level 7, 5,105 XP,
  zero credits/AP: battle rewards/costs still need integration before this is a
  full earned combat checkpoint. Three major Phase 2-3 packages remain open.

- Player group-support coverage: production ATB/command tests now execute
  Guardian Covenant and Link without explicit targets. Assert living-party
  shield/regen coverage, Link healing, no enemy benefit, no fallen-ally revival,
  and caster cooldown. Optional unlocks and injuries are deliberately seeded;
  this is behavior coverage, not earned-route or enemy group-support coverage.
  Remaining major packages: campaign-earned checkpoints, broader representative
  encounters/support, and affordability decisions with final verification.

- Optional accessory chain completed: the real workshop reward now feeds a
  separate five-seed consecutive combat comparison. All seeds win Golem/Titan;
  medkit spending is unchanged from the two-AP baseline. Full suite: 333 passing.
  This closes this item's constructed comparison, not campaign-earned snapshots
  or broader balance acceptance. Next prioritize those gaps and group support
  rather than adding more World 4 accessory variants.

- Optional equipment follow-up: Precision Sight now has a local ungated-route,
  real loot-event/replay, and owner-scoped combat-stat regression. The earned
  item is carried into a seeded World 4 party. Full navigation, disk persistence
  for this scenario, and optional-gear encounter comparisons remain separate.

- Two-AP comparison: pre-World-4 main-route boss rewards total two shared AP.
  Added legally checked Quiet Steps/Training Session purchases and runtime stat
  assertions, with five repeated seeded chains. Fixed missing desktop skill-tree
  directory enumeration and victory inventory persistence on empty loot rolls.
  Older combat measurements predate loaded passive definitions; use the latest
  comparison in COMBAT_SPENDING_BASELINE.md. Optional route/gear and broader
  campaign-earned checkpoint coverage remain open.
  Verification: all 332 JVM tests and both asset checks pass. All five two-AP
  chains clear Golem and Titan using 2-3 medkits total plus the camp item.

- Earned-equipment follow-up: corrected the defensive fixture to retain its free
  opening Flux Liner, with a source-grant assertion. Purchases now total 636 and
  leave 434 credits; no extra supplies or stats were added. Recorded optional
  pre-World-4 equipment sources and AP purchase constraints. Remaining: earned AP
  ledger, legal chosen passives, and route/slot-verified optional gear comparisons.

- Titan audit: fixed Vent Heat targeting the opponent and wired ignored status
  defense multipliers into physical vitality-defense calculation. Titan now
  takes an eligible cooling turn after either heavy skill; dedicated self-status
  and regression tests added. Found free opening Flux Liner reward, so defensive
  fixture budget was conservative by 432 credits. Full earned gear/passives
  reconciliation remains before balance sign-off. All 332 JVM tests and both
  asset checks pass. Expanded defensive/camp seeds 2-5 now defeat Titan;
  seed 1 still loses to Golem. Earlier Titan-loss notes below are historical.

- Expanded-offense comparison: level-earned offensive skills and the camp's
  self-heal item now participate in the defensive test policy. Golem wins in
  four of five seeds (versus two previously); all surviving paths still lose to
  Titan. Full suite: 330 passing. Next narrow the audit to earned loadouts/passives
  and Titan counterplay instead of assuming a global economy shortage.

- First-use camp comparison implemented: real Forge camp event restores HP via
  PartyHealth and grants its one-time supply; immediate replay changes nothing.
  Two defensive seeds reach Titan with full HP but still lose under the limited
  medkit/skill policy. All 330 JVM tests pass. Next is broader earned skill/item
  use and a representative winning loadout, not yet enemy/price tuning.

- Recovery follow-up: fixed rest using raw HP instead of the combat ceiling by
  sharing PartyHealth between exploration and combat. Found first-use-only camps
  beside the World 4 boss route. Golem loot cannot fully repay three medkits even
  when both drops sell at the best dealer. No reward/price or camp-repeatability
  change made; first-use recovery must be included in the next scenario.

- Latest pass: defensive equipped/support policy exposed and fixed overlapping
  animation pause leakage, with stale/duplicate callback regressions. Two of five
  defensive seeds now complete Slag Golem before entering Titan with depleted
  resources; no price/stat tuning. See `COMBAT_SPENDING_BASELINE.md` for limits.

This summary supersedes historical pending notes below; the dated entries remain
an implementation log, not a percentage-complete tracker.

- Phase 1: original correctness acceptance completed. New regressions still get
  fixed when discovered; completion is not a claim that no bugs remain.
- Phase 2: campaign event/save coverage and seeded runtime fixtures implemented.
  Remaining: more attainable equipped checkpoints, group-support scenarios,
  and campaign-earned multi-encounter coverage beyond seeded inventory.
- Phase 3: payout reconciliation, XP progression, shop safety, missing materials
  and shop-funded resale loops addressed. Three remaining work packages:
  (1) representative resource-spending measurements, (2) balance/affordability
  decisions and justified adjustments, (3) rerun representative scenarios and
  validators after tuning. No balance acceptance is claimed from limited policies.
- Phase 4: mostly pending; opening guidance/tutorial pacing, loadout presentation,
  and combat information/label clarity, followed by visual/device verification.
- Phase 5: mostly pending; all-world action discoverability, compatible explicit
  action references, and route/narrative terminology consistency.
- Phase 6: mostly pending; device/accessibility/lifecycle checks, audio/assets,
  performance profiling and release-build verification. A prior Play upload does
  not establish these checks. External-player feedback remains separate.

Do not translate test counts into percentage completion or a reliable time estimate.

## Latest economy pass (2026-09-08)

- Combat-spending baseline: injected default-preserving CombatRandom into the
  production view model and AI; added five-seed, twice-replayed opening and
  World 4 scenarios with real ATB/items/attacks and animation completion callbacks.
  Timeouts fail. Opening consumed zero medkits; ungeared/basic-only Titan party
  lost all five scenarios and used three medkits in four. No balance change made.
  See `COMBAT_SPENDING_BASELINE.md`; next is equipped, skill-aware multi-encounter
  testing, not treating this deliberately limited policy as realistic affordability.

- Craft/resale follow-up: closed shop-funded Armor Plating and Source Resin
  loops with targeted resale-value overrides, leaving purchase prices/stats and
  recipe costs unchanged. Added chained-crafting expected-yield checks and a
  production shop regression for separate buy/resale pricing.
- Scripted battle reward baseline totals 2,640 credits; World 1 contributes 330.
  This excludes additional encounters and loot sales, so it is not an actual
  player budget. Broader affordability and combat-consumable measurements remain.

- Material-source follow-up: defined Pure Iron, Composite Plate and Astral Thread
  and added one-time World 4/5/6 room-entry caches supplying four each. Removed
  the six missing-ingredient test exceptions. Added event-to-craft coverage for
  both affected recipes per material, including state-restore replay protection.
  Other ingredients are seeded in this focused test; supplies are finite and
  sellable, not a renewable source. See `ECONOMY_AUDIT.md` for locations and limits.

- Fixed shop stock validation, alias gates, live transaction checks, overflow
  guards and immediate session inventory synchronization. Removed the mechanic's
  unreachable basic-vest milestone gate; prices were not changed.
- Fixed duplicate-alias ingredient consumption and invalid/oversized cooking
  batches. Added transaction and catalog regressions.
- Direct cross-shop resale checks found no profitable loops across ten shops.
- Six recipes reference three undefined/unobtainable materials; explicitly
  tracked as unresolved test debt, not silently treated as valid crafting paths.
- See `ECONOMY_AUDIT.md` for price ranges, affected recipes and verification limits.
  Phase 3 remains open for material sources, craft/resale loops and affordability.

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
