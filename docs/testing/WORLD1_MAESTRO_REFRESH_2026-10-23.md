# World 1 Maestro refresh — October 23–24, 2026

**Emulator acceptance passed: all eight final runs succeeded.** Smoke, recovery, focused cases and the continuous fresh campaign each passed twice consecutively on the same final build with unchanged automation. The queue finished October 24 at 10:05 a.m. EDT. Windows execution, human phone acceptance and the broader host/narrative gates remain open.

Use the [maintained setup/run guide](WORLD1_PLAYTEST.md) and [phone checklist](../playtest/WORLD1_ACCEPTANCE_CHECKLIST.md). The [verification ledger](evidence/world1-maestro-refresh-2026-10-23/verification.json) records results and limitations; retained [device metadata/JUnit reports](evidence/world1-maestro-refresh-2026-10-23/device/) make the recorded successes and failures reviewable.

## Changes implemented

- Four supported entrypoints replace treating the historical flow directory as accepted coverage: smoke, continuous campaign, focused World 1 cases, and opening recovery. Historical flows remain explicitly outside the supported dependency graph.
- Mac shell and Windows PowerShell wrappers share a pinned Maestro runner with device/app preflight, isolated `.playtest` targeting, managed artifacts, timeouts, and strict rejection of empty/failing/skipped reports. New metadata hashes the flow/configuration tree and runner; source changes during a run prevent accepting it. Device-disconnect log collection is bounded.
- Ten World 1 checkpoints come from the production completionist controller route with normal combat and earned resources. Source/catalog and snapshot hashes prevent stale imports. Fixtures disclose earlier optional rewards, fixed-seed decisions and accelerated presentation; other-world synthetic setups remain unverified.
- Checkpoint restoration now applies the same opening/crew migrations and equipment normalization as ordinary save loading. Android regression checks include inventory, milestones, unlocked skills, equipment and campaign-save isolation. This caught missing Hydraulic Kick after completed certification.
- Scenario descriptions, starts and procedures match the earned checkpoints. The browser searches scenario IDs and exposes a review step before Launch / Reset. Recipe boundaries use coherent post-opening story state with synthetic exact/missing ingredient stock; they do not prove first-time ability unlocks.
- Stable IDs cover title/scenario controls, rooms/actions, dialogue and presentation, quests, save slots, combat controls and living targets. Actor HP and selection state support normal combat decisions. Modern Tinker uses its actual base/component picker and Synthesize controls.
- Screen initialization preserves the prompt/tutorial managers' current state. A queued AP tutorial previously became invisible while still blocking MENU; the regression reproduced expected tutorial / actual null before the fix.
- Combat uses normal enabled abilities, owned healing supplies and live target selection. It prioritizes support drones and keeps the command menu open to preserve the game's normal ATB pause. Guided loader/certification steps remain explicit. Debug victory, OP Mode and resource grants are not route actions.
- Room control lookups fail without broad gameplay swipes, which can travel. Mine descent has its explicit nested-prose scroll. The route uses the lift rest before the security fight, then Jed/checkpoint recovery supplies; the rest link is not exposed after Jed's handoff. Ration Pack meal replacement is handled explicitly.

## Coverage and current evidence

A passing chapter or isolated diagnostic is not a clean named-suite pass. The fixed-seed controller route supplies separate production-behavior evidence with accelerated presentation and authored puzzle answers.

| Coverage | Maintained flow | Current device evidence / remaining gate |
| --- | --- | --- |
| MQ01 Wake Up Call | `world1/01_opening.yaml` | Two consecutive clean fresh campaign passes |
| MQ02 Shift Clearance | `world1/02_clearance.yaml` | Two consecutive clean fresh campaign passes |
| SQ03 / certification | `world1/03_certification.yaml`, `certification.yaml` | Both fresh campaigns and both focused runs passed |
| MQ03 The Echo | `world1/04_echo.yaml` | Full mine route and Fork puzzle passed in both fresh campaigns |
| MQ04 Red Alert | `world1/05_escape.yaml` | Normal security combat and Jed handoff passed in both fresh campaigns |
| MQ05 The Launch | `world1/06_launch.yaml` | Both fresh campaigns passed checkpoint/Warden combat, pod and World 2 arrival |
| SQ01 Scavenger's Stash | `world1/stash.yaml` | Both final focused runs passed |
| SQ02 System Flush | `world1/flush.yaml` | Both final focused runs passed, including combat |
| SQ04 Protocol Override | `world1/protocol.yaml` | Both final focused runs passed |
| SQ05 Lost Shift | `world1/lost_shift.yaml` | Both final focused runs passed |
| Cart vault | `world1/cart.yaml` | Both final focused runs passed entry/revisit |
| Exact/missing recipe | `world1/craft_exact.yaml`, `craft_missing.yaml` | Both final focused runs and Android refusal/consumption/save parity passed |
| Menus/map | `world1/menus.yaml` | Both final focused runs passed; phone gestures, targets/cancel and larger text pending |
| Opening recovery | `world1/recovery.yaml` | Two consecutive passes on the frozen final APK; reports retained |
| Audio, pacing, balance, alternate order, later lifecycle | [Phone checklist](../playtest/WORLD1_ACCEPTANCE_CHECKLIST.md) | Human acceptance pending; controller assertions and asset checks do not certify these |

## Verification record

- Nine runner/image safeguard tests pass, covering report rejection, device selection, environment isolation, bounded disconnect logging and PNG filter reconstruction. The portable sprite check passes all 24 combat sprites.
- Structure/reference validation passes across all maintained and historical YAML, with the supported graph checked separately. Current checkpoint source/snapshot hashes and strict authored selectors pass. The validator catches scenario/NPC/action drift, real tutorial continuation labels, authored ability names and counter-tune solution changes.
- The full host rerun ran **717 tests: 715 passed, two failed, zero errors/skips**. Failures remain `crewPrototypeProducesEarnedCheckpoints` (older crew-checkpoint milestone comparison) and `productionCrewCampaignEarnsItsOpeningSuppliesAndReachesCredits` (World 6 source-memory-fragments defeat). They are not reclassified as passing. See [host/lint output](evidence/world1-maestro-refresh-2026-10-23/full-host-and-lint.txt).
- All 22 exploration tests, including the startup-prompt regression, pass. [Regression XML](evidence/world1-maestro-refresh-2026-10-23/exploration-prompt-regression.xml) retains that focused result. The earlier 27-case World 1 host selection also passed.
- Three focused Android cases pass: browser review/launch, ten checkpoint launches with save/load skill/equipment/inventory/milestone parity and campaign-save isolation, and exact/missing recipe refusal/consumption/save parity. See [current instrumentation](evidence/world1-maestro-refresh-2026-10-23/android-instrumentation.txt) and the [pre-fix skill mismatch](evidence/world1-maestro-refresh-2026-10-23/checkpoint-skills-before.txt).
- A broader rebuilt-fixture Android case stops at the unrelated `campaign_w4_crucible` fixture before reaching its recipe checks. It remains a recorded failure; the shared recipe checks also have a directly runnable passing case.
- The final rerun, including checkpoint migration and combat HP/selection/tag changes, reports zero lint errors, 363 warnings and nine hints.
- World 1 art/audio/content validation passes 82 rooms across 10 nodes, with seven inline-action warnings. The retained compatibility-only launch rail is verified unreachable; it is not treated as a playable shortcut. Stale test expectations for duplicate Flux Liners were corrected to one earned suit.
- Global narrative validation still reports **126 word-limit errors and 54 warnings**. Obsolete opening literal checks were reconciled to authored facts; no word limit was relaxed. Full verification remains failing because of the host and narrative gates.

## Final device acceptance

All eight final runs passed, with no retry or source change within this queue:

| Suite | First clean pass | Consecutive repeat |
| --- | --- | --- |
| smoke | `20261024-073156-e802fc41` (3.3 min) | `20261024-073514-5ec01fb4` (3.3 min) |
| recovery | `20261024-073835-9d277f69` (1.8 min) | `20261024-074024-93dd3d2e` (1.8 min) |
| focused | `20261024-074213-df6094b8` (21.3 min) | `20261024-080335-ca5ce83f` (23.1 min) |
| campaign | `20261024-082639-b0d031fd` (49.6 min) | `20261024-091614-116bf968` (49.2 min) |

The campaigns each start at normal New Game and finish at the World 2 crash site with actual combat and earned resources. Focused runs use their declared earned checkpoints or recipe stock fixtures. These results establish emulator automation acceptance, not human balance or presentation acceptance.

The [queue receipt](evidence/world1-maestro-refresh-2026-10-23/queue.json), [tested manifest](evidence/world1-maestro-refresh-2026-10-23/tested-suites.json) and [execution-file hashes](evidence/world1-maestro-refresh-2026-10-23/tested-flow-files.json) preserve the inputs. All eight reports record APK `9bd3d139d1c37947adb7162b3c2b0fc4cd3b3de6463b4f303554dd69a398d519`, flow-tree `a99cc9c4ee28a4e6872e8dcea42d4bab11f3bbab010f1dcf95efe727bf300bc2` and runner `cdfd1d37de2c3ca5a33f6912f95d5b09ff8367fc3185b02356094bf08bd0f69e`. The suite manifest's status/evidence metadata was updated only after completion; its entrypoints, every YAML file and toolchain configuration remain unchanged. The recorded flow-tree hash refers to the preserved tested manifest.

Earlier failed diagnostics remain in the ledger. They exposed obsolete rest lookups, missing checkpoint skill migration, meal replacement, combat target/menu timing and an undismissed rest overlay. Interrupted continuations and isolated passes were not counted toward this acceptance matrix.

Windows execution and a recorded phone playthrough remain unverified. Phone acceptance must cover alternate loader/Warden order, later save/background/restart boundaries, wrong puzzle choices, reward/reload parity, audiovisual presentation and normal-resource balance. The two host failures and global narrative errors recorded above still prevent claiming full verification is green.
