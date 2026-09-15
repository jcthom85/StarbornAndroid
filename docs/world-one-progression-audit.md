# World 1 progression audit — 2026-09-14

## Repair follow-up

The findings below describe the original audit. The local implementation now separates repeatable console feedback from guarded one-shot launch, adds a required-consumption Chime event plus missing-item dialogue, protects both Cryo-Inductor forms from sale, and migrates prematurely consumed launch events and missing opening parts. Pending/completed launches are preserved. Save/autosave/quicksave/import now restore the migrated inventory instead of overwriting it with the old inventory. Completed-Fork migration consumes a spare coil only once.

Regression tests now assert the corrected behavior rather than characterize the defects. Desktop clicking was unavailable because the Computer Use native pipe failed after retries; Android instrumentation is used for actual emulator testing. This is not a full six-world playthrough or a Play release.

## Status and scope

First source/runtime audit pass, not a certified end-to-end playthrough. No Android device or emulator was connected (`adb devices -l` returned an empty list), so on-device scenario launch smoke tests are blocked. No production code/assets were changed in this audit pass, and no release was made.

Reviewed the five main quests, their event and dialogue handoffs, launch controls, initial crafting requirements, shop disposal rules, node gates, scenario wiring, and cinematic recovery. Added five executable characterization checks using the authored events and the real EventManager. These deliberately demonstrate existing defects; their passing is NOT evidence that the defects are fixed.

## Findings

### P0 — early navigation-console tap permanently consumes launch

Confirmed by `WorldOneProgressionAuditTest`: with MQ05 at `launch_pod`, tap `use_nav_console` before `ms_w1_chime_spliced`. The locked-message branch executes, and the non-repeatable `w1_mq05_use_nav_console` event is added to completedEvents. Splice the Chime, then retry: no launch cinematic. Restoring that session into a fresh store/manager retains the failure.

The room action is visible and has no prerequisite milestone, so this is an ordinary interaction-order hazard, not just an injected event. References: `rooms.json` / `launch_pod`; `events.json` / `w1_mq05_use_nav_console`; `EventManager.handleTrigger` and `executeConditionalBranch`.

Safest correction: separate repeatable locked feedback from the guarded one-shot launch, or guard the launch event before any actions execute. Keep pending-cinematic recovery and one-time payouts intact. Add narrowly scoped migration for saves where this event is already completed but MQ05 has not completed and no launch cinematic is pending. Merely changing visibility will not repair already affected saves.

### P1 — Chime-splicing success is not conditional on consuming the Chime

Confirmed at the real dialogue-action execution layer. `zeke_w1_mq05_pod_core_4` marks the task done, attempts `take_item:ghost_signal_cell`, then sets `ms_w1_chime_spliced`. A false return from onTakeItem does not stop subsequent actions. The opening dialogue condition checks quest/stage/milestone but not possession of the Chime.

This is a gating-integrity defect, not a demonstrated normal-play missing-Chime route. Safest correction: validate the item at an atomic splice action/event boundary before marking the task or milestone. Avoid globally changing all action-list failure semantics without checking other authored events.

### P1 — required Cryo-Inductor can be sold before the live test

Confirmed exposure, potential softlock requiring device/recovery-route verification. `cryo_inductor` and `functional_cryo_inductor` are positive-value components without unsellable protection. Mechanic's Wares buys components without a blacklist. ShopViewModel's sale path consumes these items without consulting active quest requirements or adding buyback stock.

The cutter test requires possession of `functional_cryo_inductor`, even after the room describes it as installed. Jed's starter kit is one-shot. A second base part exists in `w1_loot_server_cooling`, but that is in Logistics; the normal checkpoint entry requires completion of MQ01. Neither item is explicitly sold in shops. Do not count that later cache as a proven early-game recovery source.

Safest correction: protect the required part while the opening depends on it, or provide a repeatable, bounded replacement through Jed. Consider representing installation as state and removing the loose inventory requirement, but coordinate that with the later Fork scene that consumes the part. Do not make all ordinary scrap permanently unsellable.

### P2 — existing Maestro file is not the playthrough its title suggests

`playtests/maestro/playtest_01_prologue_to_mine_boss.yaml` loads The Echo: Deep Mine, checks the Concourse Lobby and menu tabs, and closes the menu. It does not start in the bunk, perform MQ01/MQ02, fight a boss, or validate launch. It also clears app state. Use only on a disposable installation, not a personal save.

## Main-quest coverage

| Quest | Authored path inspected | Remaining verification |
| --- | --- | --- |
| MQ01 Wake Up Call | Light → door panel → Jed's kit → loader → craft → liner/bypass → cutter cinematic → MQ02 | Actual UI action order, shop-sale recovery, repeat dialogue, leaving/re-entering workshop |
| MQ02 Shift Clearance | Checkpoint entry → Hank/Zeke dialogue → retirement override → Logistics/MQ03 | Fresh-save traversal and out-of-order NPC conversations |
| MQ03 The Echo | Foreman/training → mine encounters → threshold → Fork cinematic → lockdown | Combat-route enforcement and actual slider puzzle input; cinematic continuation was exercised in isolation |
| MQ04 Red Alert | MQ03 completion trigger → echo exit → lift victory → Jed's sacrifice → MQ05 | Live event-bus ordering, retreat/re-entry, save between lift victory and dialogue |
| MQ05 The Launch | Bay entry → Warden victory → Zeke/Chime → console → Sector 9 | Confirmed early-console defect; happy-order event path succeeds; full combat/UI flow still pending |

## Checks run

- Progression reference validator: passed for 104 World 1 events and 80 dialogue lines, zero warnings. Reference validity does not prove usable interaction ordering.
- 25 selected tests passed across scenario catalog, opening migration, quest payouts, narrative visibility, navigation, and the new World 1 audit checks.
- New checks: early console failure + restored-state failure; successful Chime-first launch and one-time XP; missing cutter part refuses without consuming event and permits retry after restoration; pending Fork cinematic resumes and grants once; failed Chime removal still grants splice milestone.
- Session-restore checks here reconstruct the in-memory store/manager. They do not replace on-device process-death or actual disk save/load testing.

## Next actions

1. Fix the confirmed launch softlock, with affected-save recovery and success/retry regression tests.
2. Make Chime splicing resource-safe; resolve Cryo-Inductor protection/recovery.
3. Connect a disposable Android test device/emulator. Smoke-test every repaired/new scenario and then play from a clean World 1 start, including critical save/reload boundaries. Do not infer scenario launch success from catalog checks.
4. Continue the same audit method through Worlds 2–6; none are certified by this report.
