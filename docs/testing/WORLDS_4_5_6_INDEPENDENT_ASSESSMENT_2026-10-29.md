# Worlds 4–6: independent functional readiness assessment

**Subsequent implementation update:** the shared production dialogue-condition defect has been repaired and regression-tested; see [the pre-playtest fix record](WORLD2_PREPLAYTEST_DIALOGUE_FIX_2026-10-29.md). The findings below describe the assessed baseline; other identified issues remain outstanding.

October 29, 2026 · Source baseline `ae32b9d1` · Assessment only; no gameplay changes.

## Where we stand

**All three worlds have implemented main campaigns, optional quests, rewards and completion paths. They are not empty outlines. None is ready for an unrestricted regression sign-off.** A fresh earned campaign run reaches credits, but specific off-route interactions fail contracts established by World 1.

| World | Assessment | First work before serious playtesting |
|---|---|---|
| **4 — The Foundry** | Substantial industrial exploration, Gh0st development, machinery investigation and ship-upgrade arc. Core ordered route works under the test harness; optional NPC interaction and Crucible correctness need repair. | Fix Rylan/Cameron conversation exits; Crucible victory matching and retry lifecycle; protect early Titan credit; reconcile narrated combat trials with actual play. |
| **5 — The Void** | Complete Ring infiltration, Firewall, Elara/Anchor and Soloist/Tear progression. Most immediate concern is arrival difficulty coupled to permissive access, plus Charles's conversation trap. | Fix Charles exit; settle docking access versus preparation; test premature Firewall/Avatar victories and optional rewards. |
| **6 — The Source** | Full nightmare, memory-bridge, ascent, two-form boss and ending/credits structure exists. Finale works in the ordered earned route; physical staging is much more permissive than its story. | Prevent irreversible early encounter clearance from losing objectives; gate the epilogue appropriately; verify ending interruption/recovery and nightmare difficulty. |

Use [World 1's functional standards](../standards/WORLD_1_FUNCTIONAL_STANDARDS.md), not a quota for cinematics, dialogue lines, boss HP or minigames. World 4's machinery, World 5's surveillance and World 6's memory choices deserve their own structures. This extends the [independent Worlds 2–3 assessment](WORLD_2_AND_3_INDEPENDENT_ASSESSMENT_2026-10-29.md); its shared dialogue-condition defect still applies here.

## Evidence and limits

**C** = source/content inspection; **T** = executed host evidence; **R** = device acceptance required. A source-level failure chain is distinguished from a reproduced phone failure.

Inspected the live flat catalogs for rooms/nodes/hubs, quests/tasks, events, dialogue and NPCs, encounters/enemies, items/rewards, tuning, themes and completion scenes; the relevant exploration, event, dialogue and inventory handling; existing story references and public campaign reports; and the route/test implementations. The runtime loader's flat catalogs take precedence over stale nested world outlines.

Fresh validation: **29 tests passed**, zero failures/errors, across Hub4CriticalFlowTest (2), Hub5CriticalFlowTest (2), Hub6CriticalFlowTest (2), CampaignCatalogIntegrityTest (8), CampaignProvisionSourceTest (3), CampaignEventIntegrationTest (5), CinematicSkipInvariantTest (5), CampaignXpCurveTest (1), and the production-crew earned campaign test (1). The historical `Hub` test names do not map directly to world numbers: Hub4 covers Spire, Hub5 Foundry's first half, and Hub6 its second half. World 5/6 evidence additionally comes from campaign tests, not from those names alone.

The fresh earned route uses real combat, public controller navigation/actions and boundary save/reload; its assistance report is empty. It reaches all 30 main quest completions and credits. However, its dialogue evaluator is a test implementation with more supported condition tokens than production. It also calls some dialogue by speaker directly, uses seed 17, authored puzzle answers, virtual timing and animation callbacks. **It proves this ordered controller route under those assumptions, not arbitrary NPC access, all optional quests, all combat seeds or touch/audio quality.**

Separate probes used the actual dialogue service with both compiled production predicates, and the actual event manager with the live event catalog. They reproduce the dialogue loops and Crucible matching/receipt problems below. Dialogue trigger handlers were inert; event spawn/message hooks were inert while real milestone/event receipt state was recorded. These are not real arena fights or phone sessions.

[Evidence artifact](evidence/WORLDS_4_5_6_INDEPENDENT_ASSESSMENT_2026-10-29.json) includes source hashes, test results, exact probe output, graph paths, content counts and the fresh earned combat trace. No gameplay/content edits, device install, commit or push occurred.

## Actual implementation inventory

| Scope | W4 | W5 | W6 |
|---|---:|---:|---:|
| Rooms / nodes / hubs | 74 / 10 / 2 | 81 / 9 / 2 | 65 / 10 / 2 |
| Main quests / side quests | 5 / 6 | 5 / 5 | 5 / 5 |
| Room actions | 198 | 198 | 150 |
| Rooms with description variants | 19 | 18 | 11 |
| Rooms with static enemy presence | 33 | 40 | 17 |
| Main quest XP, excluding combat | 4,600 | 6,200 | 9,600 |
| Side quest XP, excluding combat/favors | 2,300 | 2,300 | 3,350 |
| Room/node/hub visual references checked | 86 | 92 | 77 |
| Missing referenced visual files | 0 | 0 | 0 |

File existence is not visual or asset-delivery certification. W4's Crucible also spawns encounters through events. Counts describe scope; they are not completeness percentages.

## 1. Critical blockers

### C1. More conversation traps: Rylan, Cameron and Charles

**C/T.** World 4's `forge_hand_rylan_default_talk` and `pipefitter_cameron_default_talk`, and World 5's `analyst_charles_default_talk`, have choice menus without a terminal exit. Their available reply/favor branches return to the same menu. Fresh probes with both actual production predicates enter these roots; every initially available option returns to its root rather than ending.

`DialogueSession.advance()` does not advance while choices exist. The Android overlay offers choice/advance callbacks; its exploration Back handling has no active-dialogue dismissal branch. These are the same class of conversation trap that prompted the Charlie repair. A tester can encounter them through ordinary visible NPCs, so they are blockers for ordinary exploration, even though the main route test skips them.

Required: an unconditional, terminal conversation exit for each, then repeat/topic/favor tests through actual interaction controls. Do not remove their quest/shop functionality to make them dismissible.

### C2. Production dialogue condition parity remains broken

**C/T.** Both production `isDialogueConditionMet` implementations omit authored `quest_task_done`, `quest_task_not_done`, `milestone_set` and `quest_not_completed`; unknown tokens return true. This is documented with broader consequences in the W2/3 assessment. Late-world dialogue uses these tokens extensively.

Fresh example: Maintenance Bot still selects `maintenance_bot_w5_sq22_intro` after Solar Maintenance is completed, because `quest_not_completed` is ignored. Event conditions can still reject the triggered action, so this observation is wrong dialogue selection, not evidence that this particular reward duplicates. Similar task-gated reminders/reveals require retesting after the shared repair.

Repair once with actual production-predicate coverage, then regress W1 quest NPCs and all later-world entry selection. Do not patch around it by rewriting every world's conditions or trusting the route harness's richer evaluator.

## 2. Required pre-playtest fixes

### P1. World 4 Crucible awards all round victories after round one

**C/T, reproduced using the real event manager.** `w4_crucible_victory_round_1/2/3` have room/enemy filters but no round-state prerequisites. `EventManager.matchesOutcome` treats the enemy list as **any matching enemy**, not an exact party match. Round one includes Magma Drones and a Welder Bot; the other round filters also include those types.

Dispatching only the first round's authored victory produces all three milestones:

`ms_crucible_round1_cleared`, `ms_crucible_round2_cleared`, `ms_crucible_round3_cleared`.

It also records all three victory event receipts. Brutus's claim dialogue can consequently offer the final award without rounds two and three. This is an optional-content integrity blocker, separate from main-story completion. Fix local round identity/state checks; a broad change to shared enemy-filter semantics could break intentional alternative victories elsewhere.

### P2. Crucible retry lifecycle is one-shot

**C/T.** The three spawn events omit `repeatable`, so their successful dispatch records a permanent completion receipt. The probe confirms round one is eligible before first start and ineligible immediately afterward. No corresponding arena retry/reconciliation path was found in the inspected events.

This creates a defeat/retreat/re-entry recovery risk even after P1 is fixed. Exercise actual defeat, retreat and process death; make unfinished rounds restartable without duplicating cleared-round rewards. The host probe establishes event eligibility/receipts, not how every battle-recovery UI behaves.

### P3. Early victories can consume future quest encounters

**C, high-priority runtime reproduction required.** Static victories persist encounter clearance independently of whether a quest credited them. Several required victory events require a later active quest or earlier task. The room graph exposes the encounters beforehand.

| World | Concrete early-order risk |
|---|---|
| W4 | From Conveyor Belt, the authored connections lead through Conditioning Chamber and Power Core to Titan Dock. MQ18 is already sufficient to reach this connected hub, but Titan credit requires active MQ20. |
| W5 | Dock → Concourse → Security Hub → Service Shaft → Server Farm → Anchor → Throne → Tear has no authored blocked-direction gates on those links. Firewall victory requires MQ23 and gamma complete; Avatar victory requires MQ25 and witnessed Soloist. There are multiple Firewall/Avatar sources, so one early victory need not doom the save, but clearing every usable source early remains a risk. |
| W6 | Campfire is physically connected through Echo Mines, bridge and stair to Memory Fragments, Spire Arena and Center. Shadow-wave credit requires MQ29 plus stair completion; guard credit requires the shadow wave; Vale credit requires MQ30 plus confrontation; God credit requires Vale defeated. Killing them outside that order risks losing the future objective. |

The controller has a specific W1 early-Warden recovery precedent. An equivalent recovery for these late-world conditions was not found. Static graph evidence does not alone certify every touch traversal; reproduce the earliest legal visit/victory and reload. Protect progress through deliberate encounter timing/access or prior-victory reconciliation. Preserve intentional exploration and alternate enemy sources.

### P4. World 6 epilogue geography is accessible before the ending

**C.** `source_center.connections.east` leads directly to `source_new_world`, without a blocked-direction condition. Its connected street/diner/repair-crew branch is also present, including the Astra shuttle action. `source_board_astra` has controller handling and does not require `ms_game_complete`.

The credits-replay event correctly requires game completion; this is **not** an unrestricted credits trigger. It is a physical/narrative staging leak: a player can reach the supposedly new-world aftermath before completing the final confrontation. Tie access to the intended ending state and test both pre-ending refusal and post-ending free exploration/boarding.

### P5. Promised combat trials complete through messages instead of trials

**C.** W4's `conditioning records` action fires `w4_mq18_defeat_prototypes`, directly setting the prototype-defeat task. Overclocked's `test chamber terminal` similarly fires `w4_sq20_survive_waves`, directly setting its survive-waves task and milestone. Neither event spawns a fight or runs a three-wave hazard interaction.

There are real static Phantom encounters and a real separate Crucible, but these events do not require those victories. The test's use of visible actions proves progression, not the promised combat. Choose intentionally between a records/calibration task with truthful wording and an actual trial. New combat is not automatically the right solution.

### P6. World 5 docking gate and preparation must be resolved together

**C/T.** The sealed-airlock narrative does not match physical access to Concourse, Customs Lounge and beyond before fighter clearance, docking and cipher objectives. The fresh earned controller route uses this access to equip, trade, eat and rest before returning to the dock fight.

That fight is an actual **pair** of Orbital Fighters. The existing World 5 report records costly preparation and failed lighter policies. The fresh earned trace confirms a successful prepared route; it does not prove an ordinary arrival loadout is adequate. Do not simply lock the airlock and remove the recovery/shop access on which the proven route relies. Settle where safe preparation belongs, then test low-resource arrivals and affordability before changing traversal.

### P7. Favor quantity checks and optional step ordering

**C.** Cameron's favor option checks possession of Composite Plate but takes **two**; Charles's checks Circuit Board but takes **two**. Bare `item:` conditions establish at least one. The reward sequences need a quantity-aware precondition and atomic payment check. Otherwise insufficient inputs can expose a turn-in, with reward/payment behavior depending on trigger handling; exact partial-payment outcomes need a focused mutation test.

Several optional tasks also have stricter room-action gating than their event definitions: e.g. W4 salvage versus stopping intake, and W5 Director logs/purge steps versus preparatory evidence. Audit the **visible route plus event guards** together. Do not label every direct-event shortcut a player exploit, because action-level prerequisites can already prevent it. Tests should ensure completing a quest does not merely auto-fill work the player never performed.

## World 4: functional and creative assessment

The authored route is clear: landing and heat crossing → vent controls/airlock → Project Phantom truth → Cooling Springs regroup → Assembly/matrix → Anvil machinery → Titan confrontation → engine/arrays theft → high-orbit departure. Most transitions use task-gated object actions, not required companion NPC conversations.

| System | What exists / what remains |
|---|---|
| Main quests | MQ16 Into the Fire, MQ17 Ghost in the Machine, MQ18 The Assembly, MQ19 The Anvil, MQ20 Meltdown all have event-owned progression/rewards. The pulse board → grease marks → override paddle → breath pistons → cradle chain is real multi-location investigation. P5 affects the supposed prototype wave. |
| NPCs and narrative | Rylan's shop/favor, Cameron's stories/favor, Brutus's arena are physically present. Main Gh0st/Orion/Rylos beats also have dialogue definitions, but companion/Rylos room presence does not automatically expose them; corresponding object actions often show narration instead. Verify intended emotional delivery, especially Phantom revelation and Rylos. Missing dialogue access is not a missing main route when a visible action supplies it. |
| Optional quests | Sabotage (three crates), Lost Worker (ping/shelter/safe route), Scrap Heap (intake/salvage/bypass), Quality Control (reprogram then release/worker-defense choice), Overclocked and Crucible. These are implemented routes, with P1/P2/P5 correctness gaps. Both Quality Control endings deserve exclusive, persistent reward checks. |
| Combat | Foundry enemies and Titan are present. Fresh Titan run wins with no item use or KOs under its earned build and rest policy. This supports viability, not an ideal difficulty curve; inspect other Foundry fights and P3. |
| Rewards/progression | Heat Liner, Gh0st override key/Phase Counter, Anvil/Construct, engine and arrays; optional equipment/skills and arena armor. W4 main quest XP is 4,600. Verify usable skill ownership, item deltas, AP receipts and once-only awards. Some items described as mods are implemented as accessories; clarify player-facing slot/effect expectations rather than assume the equipment is absent. |
| Navigation/rest | Cooling Springs and forge rest spots support the arc. Slag-bridge toggle gates an optional smelter branch. Hub transition through regroup is intentional; no direct starting-room path to Titan is required. Check toggle cycles, return travel and local map clarity on device. |
| World handoff | Escape requires both theft tasks, completes MQ20, starts/tracks MQ21 and warps to Executive Dock. Earned test rejects launch before theft and verifies item possession. It does not establish touch confirmation or uninterrupted audiovisual pacing. |

W4's specificity is worth preserving. The need is order safety and truthful interactions, not an additional cinematic quota.

## World 5: functional and creative assessment

| System | What exists / what remains |
|---|---|
| Main quests | MQ21 Docking Procedure: approach/fighter pair/dock/cipher. MQ22 Zero G: Solarium/security/shaft/mainframe/Thorne. MQ23 Core Approach: maze and alpha/beta/gamma plus actual Firewall victory. MQ24 Anchor: chamber/Elara/relic and Stasis. MQ25 Critical Mass: throne/Soloist/Avatar/Tear. All have event-owned progression and the earned ordered route completes them. |
| Optional quests | Employee of the Month (redactions/logs/publication, Corporate Insight), Solar Maintenance (false sun/mirrors/gardens, Sunbeam), Vacuum Seal (pressure/breaches/dock, Mag-Boots), Ghost in the Shell (purge/recovery/guardian ownership versus crew covenant), Admin Privileges (keycard/armory, Void Clip). Verify prerequisites, exact awarded skills and exclusive branch outcomes. |
| NPCs | Charles, Maintenance Bot, Thorne, Elara and Vale have room presence. C1 prevents safe Charles interaction; C2 affects task-based dialogue selection. Elara's music trigger and direct inspection route should be checked together for consistent scene/audio delivery. |
| Combat/economy | Forty rooms have static enemies; some are optional repeated patrol types. Fresh ordered victories establish a viable prepared path through fighters, Firewall and Avatar. They do not establish no-grind affordability after different purchases/side branches, or a smooth arrival spike. Record item spending and recovery across the whole ring. |
| Puzzles and equipment | Zero-G clamp physically gates the optional avionics lab; optional Graviton tuning exists. Main firewall is an authored action sequence plus combat, not an absent mandatory tuning game. Grav Boots, Anchor/Stasis and side rewards must agree with actual effects and equipment slots. |
| Navigation/content density | Physical main access is permissive (P3/P6). Six groups of **identical full room descriptions span 32 rooms** in repeated side branches. Action labels/messages repeat as well. This is measurable prose repetition, unlike calling all `_scale_` IDs filler. Assess whether detours earn their walking/combat time before rewriting or deleting rooms. |
| Completion | Tear event requires Avatar victory, completes MQ25, unlocks Source and starts/tracks MQ26 at Campfire. Test loss/retreat/reload near Avatar and deliberate return before crossing. |

The Ring has a strong executive-paradise/control-system identity, but it shows the greatest verified repetition of the three worlds. That is a pacing/content-quality concern, below progression correctness in priority.

## World 6: functional and creative assessment

| System | What exists / what remains |
|---|---|
| Nightmares | MQ26 credits Manager Projection, Endless War Echo and Silent Shore Wraith victories; song-fire requires all three before Key/reassembly. Alternate single-enemy side-branch victories count. The earned route deliberately uses the single Echo, not the paired Kill Suite. This is legal content; paired encounter balance remains unproven. |
| Memory progression | MQ27 mines/patrol/elevator, MQ28 four crew anchors/bridge/promise/threshold, MQ29 three refusals/stair/shadow party/guard party/Center. These are specific emotional and prerequisite structures, not a requirement to reproduce W1's tutorials. P3 exposes encounters before their intended stages. |
| Optional payoffs | Jed's Echo → Legacy; HR Record → Unshackled with personal exit/worker-broadcast variants; Elara's Song → Source Balance; Aethel Grave → Ancestral Grace; Final Scavenge → Starborn equipment. All have multi-step events and task locations. Verify learned abilities are usable and final branches are exclusive. |
| Combat/resources | Fresh earned trace wins nightmares, full three-enemy Memory Fragments, full two-enemy Spire Arena and both Ascended forms. It consumes ten Medkit Plus portions across the first five fights plus rations, while the final forms each use a ration. This is one build/seed/policy, not a broad balance verdict. Final boss HP alone cannot establish difficulty. |
| Final fight staging | Vale is at Center; God form is a separate encounter at `source_center_node_scale_03`. Story prose presents transformation, while the player must travel along the north branch to fight it. Check that this is understandable and emotionally continuous. No claim that God form is missing: the earned run defeats it. |
| Tuning/ending | `source_harmonic_focus` is an actual slider puzzle. Controller success-event eligibility blocks opening/submitting before the authored victory requirements. Final Note cinematic callback gives XP/completes MQ30/sets game completion/warps New World; epilogue callback sets credits seen and untracks. Fresh earned route reaches credits and boundary save parity. |
| Postgame | New World/diner/community/repair branch, replay photo and Astra shuttle exist. P4 leaks this space early. After proper completion, check replay, repeated boarding, return location and saved game/credits flags without re-awarding finale rewards. |

The ending exists and has more functional implementation than a superficial dialogue count would suggest. Human acceptance must establish that its separate boss rooms, tuning, music and cinematic callbacks feel like one climax.

## 3. Safe self-contained improvement candidates

No fixes were implemented in this pass. Suitable small repair groups are:

- Add terminal exits to the three identified NPC menus and test topic/favor return paths.
- Add local Crucible round-state guards and retry coverage, preserving shared victory-filter semantics used by alternative encounters elsewhere.
- Add tests that defeat required encounters before their quest/task eligibility, reload, then attempt the normal objective. Ordered campaign success is insufficient.
- Validate quantity-aware favor payment, reward-once behavior and visible optional-step prerequisites.
- Add a completion gate to New World's intended entry while preserving postgame boarding/replay.

Broader combat redesign, shared parser changes, map redesign and narrative rewrites should be separate deliberate work. Production condition parity is shared repair work already identified, not a new W4-specific patch.

## 4. Focused manual regression checklist

Use earned preceding-world saves and separate snapshots for destructive order tests. Record build, save, inventory/AP, active task and exact interaction. Run normal paths and these deviations.

**World 4**

1. Rylan/Cameron: every topic, favor acceptance, insufficient/full payment, completion, unconditional exit and re-entry; commerce remains usable.
2. Landing/slag/vent/airlock in normal order; toggle optional bridge twice, leave/reload and revisit smelter.
3. Phantom terminal before/after quest start; Cooling Springs regroup, party state and exact reward/skill once.
4. Assembly records versus actual enemy victories; agree on intended trial before rating it. Complete all Anvil clues out of order and ensure clear feedback.
5. Visit Titan during MQ18/19 on a disposable save, win early and reload; prove MQ20 can still finish. Test confrontation omitted, defeat, retreat and theft-before-victory.
6. Crucible: first victory must clear only round one; fail/retreat/reload each unfinished round; final armor/XP/credits once only.
7. Complete all six sides and both Quality Control branches on separate saves. Launch early, then legitimately; confirm W5 party/inventory/journal.

**World 5**

1. Arrive with ordinary low supplies and a second richer loadout. Determine safe recovery/shop access, fighter-pair difficulty and affordability without relying on foreknowledge.
2. Charles topics/favor/exit; Maintenance Bot before and after Solar Maintenance. Thorne/Elara/Vale dialogue must follow real task state.
3. Test sealed-airlock narrative against every physical exit. Before changing gates, ensure intended preparation remains reachable.
4. Inspect maze/firewalls early; defeat all reachable Constructs before gamma on an isolated save, then test recovery. Repeat early Avatar versus Soloist ordering.
5. Complete all five sides in different orders; test the two guardian ownership outcomes, payment/skill/equipment effects and repeat awards.
6. Toggle zero-G clamp repeatedly, optional vault/tuning, return shortcuts and map readability.
7. Tear before victory, after loss/retreat, after proper victory; reload during handoff to W6.

**World 6**

1. Nightmare order permutations, retreat/loss/reload, legal alternate Echo fight and paired Kill Suite. Check party recovery and supply sustainability.
2. Each companion side story: prerequisites, branch choice, ability unlock, journal and repeat award. HR revocation versus later 'deletion' prose should remain consistent with the chosen outcome.
3. Attempt bridge and stair early, test missing anchors/refusals and wrong-order enemy victories. No permanent loss of required credit.
4. Try reaching Center and New World before their intended stages. New World should not spoil the finale or offer a premature shuttle.
5. Confront/fight Vale, locate God form naturally, retreat/reload between forms; puzzle blocked before both victories, usable afterward.
6. Fail tuning once, leave/reopen, solve. Background/kill/relaunch during Final Note, world warp, epilogue and credits. No stranded completed task with missing callback, repeated rewards or lost game-complete flag.
7. Postgame save/load, replay photo, Astra boarding/disembarking and return-travel state.

Across all three: physical NPC/action row clarity, minimap, navigation during dialogue, notification/reward ordering, combat readability, audible music layering and process-death recovery still require Pixel acceptance.

## 5. Later polish and narrative continuity

Prioritize World 5's proven room-description repetition, main-character speech delivered as object narration, accessory-versus-mod terminology, reward effect wording, optional shortcut reuse and ambience transitions. W4's pipe-gallery `industrial` environment lacks the dedicated Foundry theme mapping; the normal Foundry/Void/Source environments do have themes. Treat fallback styling as localized polish, not an unrenderable world.

Rylan's worker acknowledgement condition references `ms_w4_worker_traced`, for which no setter was found; the actual worker-ping chain uses other milestones. Verify/align this optional callback. W6 has duplicate identical milestone-set actions in some side rewards; sets are idempotent, so this is cleanup rather than a duplicate payout proof.

W3 departure speaks of slipping the Shield while W4 arrival distinguishes an internal curtain from the still-unreached outer Shield. Review player-facing continuity across the boundary so the two-layer explanation is clear. This assessment does not prescribe new lore. Preserve the Foundry → high-orbit hardware → Ring → Tear → Source causal arc.

## Repair order and planning estimate

1. Shared dialogue condition parity with W1 regressions, plus the three terminal exits.
2. Local Crucible round identity/retries and early-victory recovery tests across late worlds.
3. Epilogue access and World 5 arrival/preparation decision; reconcile trial/reward claims.
4. Earned main/optional controller routes, then focused phone sessions and balance/presentation follow-up.

Planning allowance, not measured implementation time: **roughly 2–4 engineering days** for exits, Crucible, staging/recovery and their targeted regressions, excluding the already identified shared parser repair. Allow **another 1–3 days** if real combat trials or a substantial arrival-balance pass are chosen, plus at least one dedicated playtest session per world and follow-up time determined by results. Room prose expansion is separate polish work. The biggest uncertainties are early-victory reproduction/recovery scope, Crucible defeat lifecycle and the intended W5 preparation route.

The evidence supports targeted stabilization before broad expansion. It does not support a reliable percentage-complete score or declaring any of these worlds regression-certified.
