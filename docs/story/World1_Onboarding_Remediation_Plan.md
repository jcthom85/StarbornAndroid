# World 1 Onboarding Remediation Plan

Purpose: make the full World 1 player journey consistently understandable from New Game through the crash into World 2. This plan turns the June 17, 2026 audit into a phased implementation checklist.

## Implementation Status

Last updated: June 18, 2026.

- Phase 1 Critical Path Clarity: Implemented.
  - `w1_mq01` now points to Jed's Bunk before Jed's Workshop.
  - `npc_talk` now triggers at Jed's Bunk.
  - Completing `w1_mq01` starts/tracks `w1_mq02`.
  - Completing `w1_mq02` starts/tracks `w1_mq03`.
- Phase 2 Required Tutorial Coverage: In progress.
  - Implemented and device-verified: early hotspot/action prompt, journal prompt, NPC talk prompt, Logistics commitment warning, mandatory Guard Break training, Source Art unlock prompt.
  - Deferred decisions: save/rest, party basics, deeper combat micro-tutorials.
- Phase 3 Quest Text and Objective Polish: Implemented for the known mismatch.
  - `w1_mq02` keeps internal task id `spoof_liability_form` for save/test stability.
  - Player-facing text now reads: `Let Zeke bury the liability flag under grid-instability paperwork.`
- Phase 4 Maestro and Automated Coverage Repair: Implemented for the World 1 critical path.
  - `smoke_launch.yaml` passes against `com.junewiregames.starborn.prealpha`.
  - `mainquest_wake_up_call.yaml`, `checkpoint_badge_gate.yaml`, `heavy_lifting_training.yaml`, `mainquest_the_echo.yaml`, `mainquest_red_alert.yaml`, and `mainquest_the_launch.yaml` pass standalone on the Pixel 8a.
  - Known tooling note: Maestro 2.6.0 can still intermittently fail `launchApp` immediately after app data is cleared; use ADB preflight during repair runs.
- Phase 5 Documentation Cleanup: Updated for the June 18 critical-path pass.

## Current Assessment

World 1's main story spine is coherent at the event/data level:

1. Wake in Nova's bunk.
2. Talk to Jed and repair the Cryo-Inductor.
3. Reach the Transit Checkpoint.
4. Get denied by Hank and rescued by Zeke's paperwork override.
5. Enter Logistics and receive Bogs' Sector 4 assignment.
6. Complete guard-break training before the Deep Mine.
7. Descend, fight through Sector 4, and sync with the Tuning Fork.
8. Escape the lockdown.
9. Receive Jed's Chime at the cargo lift.
10. Defeat the Warden, splice the Chime, launch, and crash into Sector 9.

The main story spine is now coherent and device-verified through chapter flows. Remaining risk is limited to deferred tutorial/design decisions: save/rest, party basics timing, and deeper combat micro-tutorials.

## Evidence From Audit

- `Hub1CriticalFlowTest` passes the World 1 critical path through the crash handoff.
- `:app:runAssetIntegrity` passes with zero World 1 validator warnings.
- Device coverage now verifies Wake Up Call, Paperwork/Badge Gate, Heavy Lifting, The Echo, Red Alert, and The Launch as standalone chapter flows.
- The MQ02 -> MQ03 handoff now starts/tracks `The Echo` immediately when Zeke grants the Mine Access Badge, so the player has a visible next objective before walking to Logistics.

## Guiding Principles

- The player should always have one clearly tracked main objective.
- The first time the game requires a mechanic, the player must be taught that mechanic before or at the requirement.
- Optional content can reinforce mechanics, but must not be the only place a main-path mechanic is taught.
- Quest text must describe what the player actually does. If an NPC does a task during dialogue, do not phrase the objective as if the player performs a minigame or manual action.
- Prefer one concise tutorial prompt plus immediate practice over long tutorial stacks.

## Phase 1: Critical Path Clarity

Goal: remove ambiguity about where to go next from New Game through entry into Logistics.

### 1. Fix Wake Up Call objective order

Problem: `w1_mq01` currently asks the player to "Reach Jed's Workshop," but the first mandatory Jed conversation happens at `pit_jed_bunk`. This makes the intended early route unclear.

Update `app/src/main/assets/quests.json`:

- Stage `wake_in_the_pit`
  - Keep `check_bunk`.
  - Keep `leave_sleeping_level`.
- Stage `report_to_jed`
  - Add or rename first task to `find_jed`.
  - Text: `Find Jed in his bunk.`
  - Keep `talk_to_jed`.
  - Move `reach_workshop` after `talk_to_jed`.
  - Text: `Reach Jed's Workshop.`
  - Keep `equip_starter_gear`.
  - Keep `use_tinkering_table`.

Update events/dialogue:

- Add an event or dialogue-side task completion for `find_jed` when the player enters `pit_jed_bunk` or starts Jed's first dialogue.
- Ensure `talk_to_jed` is completed only by finishing the Jed intro line, not by opening the tinkering table.
- Keep `equip_starter_gear` tied to Jed's final intro trigger.

Acceptance:

- Fresh game quest detail points the player to Jed's Bunk before it points to Jed's Workshop.
- After Jed's first dialogue, the quest objective points to Jed's Workshop.
- `Hub1CriticalFlowTest.wakeUpCallCompletesFromBunkToJedToCryoInductorRepair` still passes after being updated for the new task.

### 2. Move NPC talk tutorial to the first Jed encounter

Problem: `npc_talk` currently appears at `workshop_floor`, after the player already had to talk to Jed in `pit_jed_bunk`.

Update `app/src/main/assets/events.json` or dialogue trigger wiring:

- Trigger `system_tutorial:npc_talk|Jed's Bunk` when Jed is first visible in `pit_jed_bunk`, before the player must tap him.
- Remove or suppress the redundant `npc_talk` tutorial at `workshop_floor` for players who already completed it.

Acceptance:

- Fresh game teaches NPC interaction before the first required NPC conversation.
- The player does not see the same NPC talk tutorial again at Jed's Workshop.

### 3. Strengthen MQ01 to MQ02 handoff

Problem: `w1_mq02` only starts when the player enters `checkpoint_queue`. If the player finishes `Wake Up Call` and returns to the hub, the next main destination is implied but not guaranteed.

Preferred fix:

- Start `w1_mq02` immediately when `w1_mq01` completes.
- Track `w1_mq02`.
- Set stage `reach_checkpoint`.
- Objective text remains `Approach the Transit Checkpoint.`

Alternative if immediate start is not desired:

- Add a clear post-completion banner and hub hint: `Report to the Transit Checkpoint for mine clearance.`

Acceptance:

- After completing `Wake Up Call`, the player has an active tracked main quest.
- Hub UI makes `Transit Checkpoint` the obvious next node.

### 4. Strengthen MQ02 to MQ03 handoff

Problem: `w1_mq03` only starts when the player enters `admin_lobby`. After Zeke's override, the player receives the Mine Access Badge but may not have a clear tracked next objective.

Preferred fix:

- Start `w1_mq03` immediately when `w1_mq02` completes.
- Track `w1_mq03`.
- Set stage `sector_four_assignment`.
- First active task: `Enter the Logistics Sector.`

Acceptance:

- After Zeke's override, the tracked objective points to Logistics/Concourse.
- Entering `admin_lobby` can still mark `enter_logistics_sector` complete, but should not be responsible for starting the quest.

## Phase 2: Required Tutorial Coverage

Goal: make every required World 1 mechanic taught before it matters.

### 5. Align tutorial documentation with implementation

Problem: `docs/story/Tutorial_Placement_Map.md` lists many required World 1 tutorials that are not currently wired into `tutorial_scripts.json` or event triggers.

Update docs after implementation:

- Mark implemented tutorials with their actual script id or code path.
- Move aspirational tutorials into a backlog section until they exist.
- Remove obsolete references such as duplicate or cut scripts.

Acceptance:

- Every World 1 row marked `Mandatory` in `Tutorial_Placement_Map.md` maps to one of:
  - `app/src/main/assets/tutorial_scripts.json`
  - an event/dialogue `system_tutorial`
  - a named runtime tutorial system in Kotlin
  - a clearly documented implicit UI affordance with a test or screenshot

### 6. Journal tutorial

Problem: `scene_market_journal` exists but is never referenced. The player receives quests and updates, but the journal itself is not taught.

Implement:

- Trigger `scene_market_journal` after `Wake Up Call` starts or after the first quest update, but not before the player has seen movement and an object interaction.
- Do not gate progress on opening the journal unless the UI is stable enough for a one-step practice prompt.

Acceptance:

- Fresh players see one concise journal tutorial in early W1.
- The prompt does not stack on top of the movement/NPC/inventory/tutorial sequence.

### 7. Hotspot and action-menu tutorial

Problem: the plan says highlighted words and action menus are mandatory teaches. The game has tappable actions, but the teach is mostly implicit.

Implement:

- Add a one-time prompt in `pit_nova_bunk` for the first interactable object.
- Suggested text: `Highlighted room details can be inspected. Tap one to see what Nova can do.`
- If the current UI uses direct action buttons instead of a multi-action menu, update the doc language to match the real UI.

Acceptance:

- Before the player must inspect the bunk, the UI teaches that highlighted/actionable room details are interactive.
- The tutorial text matches the actual control scheme.

### 8. Save/rest tutorial

Problem: the placement map says bed/rest should be taught in Nova's Bunk. Current W1 data does not clearly expose a save/rest teach there.

Decision needed:

- If bed/rest is in scope for the current build, add a `Rest` or `Save` action to `pit_nova_bunk` and teach it once.
- If not in scope, downgrade this from mandatory in the tutorial map.

Acceptance if implemented:

- The bunk has a clear action.
- The first use explains exactly what restores or saves.

### 9. Point-of-no-return warning

Problem: the plan calls for a checkpoint warning, but no current event text clearly warns about unfinished errands before leaving Homestead/entering Logistics.

Implement:

- Add a one-time prompt near the Admin Gate/Transit Checkpoint before the player commits to Logistics.
- Text should be specific, not vague:
  - `Entering Logistics advances the main story. Finish Homestead errands first if you want to complete them now.`

Acceptance:

- The warning appears before the transition that changes available NPCs/quests.
- It does not appear repeatedly on every checkpoint visit.

### 10. Hacking/tutorial wording for Paperwork

Problem: MQ02 used to say `Spoof the liability form`, but the player does not actually do a hacking minigame. Zeke performs the override through dialogue.

Choose one:

- Keep it dialogue-driven and rename the task text to `Let Zeke bury the liability flag under grid-instability paperwork.`
- Or build a simple required interaction/minigame and teach it.

Recommended short-term fix:

- Rename the task to match current gameplay.
- Keep hacking tutorial out of World 1 until there is an actual hacking interaction.

Acceptance:

- Quest task text no longer promises an unimplemented minigame.

### 11. Guard-break tutorial remains mandatory

Current state:

- Runtime combat tutorial exists in `CombatViewModel`.
- It triggers in `workshop_dock` during `w1_sq03` stage `guard_break_training`.
- Bogs redirects untrained players into Heavy Lifting before Sector 4.

Keep and harden:

- Ensure Bogs' redirect remains mandatory before Deep Mine shield enemies.
- Keep `ms_w1_guardbreak_trained` as the gate for MQ03 progression.
- Update Maestro flow `heavy_lifting_training.yaml` after UI selector cleanup.

Acceptance:

- A player cannot reach the Deep Mine shield lesson without either completing or intentionally skipping guard-break training.
- If skipping is supported, the game records that choice and does not block main progression silently.

### 12. Source Art acquisition tutorial

Status: implemented. The Tuning Fork relic sync now grants `tuning_fork`, unlocks `nova_blast_wave`, and shows the `source_art_unlock` tutorial before the `Red Alert` handoff.

Implemented prompt:

- `Source Art unlocked: Blast Wave. Relic techniques are powerful skills with longer cooldowns.`

Acceptance:

- `Hub1CriticalFlowTest` asserts `nova_blast_wave` is unlocked after `touch_relic`.
- `mainquest_the_echo.yaml` verifies the Source Art tutorial appears after the Tuning Fork item popup and before `Red Alert`.

### 13. Snack, cooldown, weakness, and status tutorials

Current state:

- Cooldowns and snack cooldown labels exist in combat UI.
- Weakness reward exists in combat logic.
- Snacks exist as item type and combat command.
- No clear World 1 tutorial prompt teaches these explicitly.

Recommended sequencing:

- Do not add all of these as popups in early World 1.
- Add one contextual prompt for cooldown the first time a player opens Skills in combat.
- Add one contextual toast for first weakness hit if not already visible enough.
- Add a snack prompt only after the player has a snack equipped and enters combat.
- Teach status effects via actual enemy/status encounter only if main W1 requires the player to react to it.

Acceptance:

- No combat tutorial stack appears before the player has control.
- Each tutorial fires only when the mechanic is visible and usable.

### 14. Party basics and Zeke

Problem: the tutorial map says party basics should be taught when Zeke joins, but W1 ends immediately after launch. If Zeke is not controllable until World 2, teaching party basics in W1 is premature.

Decision:

- If there is no Nova+Zeke combat before the crash, move party basics to World 2.
- If a post-Warden/pre-launch fight is added, teach party basics there.

Recommended:

- Move party basics to `w2_mq01` or the first World 2 combat with Zeke.

Acceptance:

- The tutorial appears when the player can actually use party mechanics.

## Phase 3: Quest Text and Objective Polish

Goal: make every objective read like an actionable next step.

Review all W1 main quest task text:

- Avoid tasks that complete automatically without visible player action unless the task is phrased as a story beat.
- Make destination objectives name the destination exactly as shown in UI.
- Ensure every task has either:
  - a visible NPC/action label,
  - a destination room/node label,
  - or a direct narrative popup that explains the next move.

Specific edits:

- `Reach Jed's Workshop` should not appear before Jed sends the player there.
- `Spoof the liability form` has been renamed in player-facing text; internal id remains stable.
- `Survive the Warden's arrival` may be okay as a story beat, but confirm it does not appear as an unresolved actionable task for too long.
- `Splice the Chime into the pod core` is good because Zeke dialogue and pod-core room support it.

## Phase 4: Maestro and Automated Coverage Repair

Goal: restore UI confidence after the data/content changes.

### 15. Repair stale Maestro flows

Flows that failed in the focused audit:

- `mainquest_wake_up_call.yaml`
- `checkpoint_badge_gate.yaml`
- `mainquest_the_echo.yaml`
- `mainquest_red_alert.yaml`
- `mainquest_the_launch.yaml`

Current repair status:

- `smoke_launch.yaml`: passes.
- `mainquest_wake_up_call.yaml`: passes from fresh New Game through bunk inspection, hotspot/journal/NPC/inventory/tutorial prompts, Jed intro, Jed's Workshop, tinkering, Functional Cryo-Inductor craft, `Wake Up Call` completion, and `Paperwork` handoff.
- `checkpoint_badge_gate.yaml`: passes through Hank denial, Zeke override, Mine Access Badge grant, immediate `The Echo` handoff, gate unlock, and Bogs' Heavy Lifting redirect for an untrained Nova.
- `heavy_lifting_training.yaml`: passes through Bogs' setup, inline loader/cargo interactions, Guard Break tutorial gating, Hydraulic Kick usage, Acoustic Bulwark victory, spoils, level-up, reward item popups, and quest completion.
- `mainquest_the_echo.yaml`: passes through Bogs' Sector 4 assignment, Deep Mine route, generator restore, cleared encounter suppression, Tuning Fork cinematic, relic item reward, and `Red Alert` handoff.
- `mainquest_red_alert.yaml`: passes through Zeke comms, escape route, cleared encounter suppression, Jed sacrifice, Ghost Signal Cell reward, and `The Launch` handoff.
- `mainquest_the_launch.yaml`: passes through Zeke/Chime splice, navigation-console launch, Planetary Impact cinematic, `A Strange Coast` handoff, and Crash Site return.

Failure pattern:

- stale labels such as `Inspect Bunk`
- changed debug checkpoint visibility
- changed NPC/action labels
- potentially changed current app state after launch

Fix approach:

1. Run each flow individually.
2. Capture screenshot on first failure.
3. Update selectors to current visible text/content descriptions.
4. Prefer stable content descriptions where text is intentionally variable.
5. Avoid coordinate taps except where no semantic selector exists.

Acceptance:

- The chapter flows pass individually.
- Focused W1 suite:
  - `mainquest_wake_up_call.yaml`
  - `checkpoint_badge_gate.yaml`
  - `heavy_lifting_training.yaml`
  - `mainquest_the_echo.yaml`
  - `mainquest_red_alert.yaml`
  - `mainquest_the_launch.yaml`

### 16. Add targeted unit tests for handoff changes

Update or add tests in `Hub1CriticalFlowTest`:

- Completing `w1_mq01` starts/tracks `w1_mq02`.
- Completing `w1_mq02` starts/tracks `w1_mq03`.
- First Jed route completes `find_jed` before `talk_to_jed`.
- `npc_talk` tutorial is requested before first Jed conversation.

Acceptance:

- `.\gradlew.bat :app:testDebugUnitTest --tests "com.example.starborn.domain.dialogue.Hub1CriticalFlowTest"`
- `.\gradlew.bat :app:runAssetIntegrity`

## Phase 5: Documentation Cleanup

After implementation, update:

- `docs/story/Tutorial_Placement_Map.md`
- `docs/story/World1_Vertical_Slice_Status.md`
- `playtests/maestro/README.md`

Acceptance:

- Tutorial map no longer lists unimplemented mandatory teaches as if they are live.
- World 1 status doc names known remaining gaps honestly.
- Maestro README descriptions match current flow coverage.

## Proposed Implementation Order

1. Fix `w1_mq01` task order and first Jed tutorial.
2. Add MQ01->MQ02 and MQ02->MQ03 automatic handoffs.
3. Rename MQ02 spoof task or build the actual interaction.
4. Add journal and hotspot tutorials.
5. Add point-of-no-return warning.
6. Decide party-basics placement.
7. Update docs to match implementation.
8. Repair Maestro flows.
9. Run validation suite.

## Minimum Done Criteria

The remediation is complete when:

- A fresh player always has a clear tracked main objective from New Game through World 2 crash site.
- The first required NPC interaction is taught before the player must do it.
- The first required object/hotspot interaction is taught before the player must do it.
- The first required combat puzzle, guard break, is taught before Deep Mine and Warden.
- The tutorial placement map matches shipped behavior.
- `runAssetIntegrity` passes.
- `Hub1CriticalFlowTest` passes.
- The focused W1 Maestro suite passes or has documented, non-gameplay blockers.
