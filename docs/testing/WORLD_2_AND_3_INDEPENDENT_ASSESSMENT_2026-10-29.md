# Independent World 2 and World 3 readiness assessment

**Subsequent implementation update:** the shared production dialogue-condition defect has been repaired and regression-tested; see [the pre-playtest fix record](WORLD2_PREPLAYTEST_DIALOGUE_FIX_2026-10-29.md). The findings below describe the assessed baseline; other identified issues remain outstanding.

Assessment date: October 29, 2026. Source baseline: `ae32b9d1` (`main`). No gameplay, content, APK, or published website changes were made for this assessment.

## Verdict

**World 2 has a coherent, substantially implemented campaign, but its production dialogue gates prevent a clean readiness endorsement. World 3 has a substantial authored heist campaign, but it is not ready for an ordinary uninterrupted playthrough.** Its first main quest has a circular NPC visibility requirement. Its optional completion route also depends on an unreliable, nonrenewable drop. Repair and test these specific failures before expanding or rewriting either world.

This supersedes the readiness confidence in [the earlier World 2 report](WORLD2_READINESS_2026-10-29.md). That report's individual content fixes and successful test observations remain evidence, but its route harness evaluates conditions more completely than the production app. My earlier readiness conclusion gave those tests too much weight.

World 1 supplies [functional contracts](../standards/WORLD_1_FUNCTIONAL_STANDARDS.md), not a required number of cinematics, dialogues, rooms, or fights. Its own [regression checklist](../standards/WORLD_1_REGRESSION_CHECKLIST.md) remains relevant; it is not a certification of every shared system.

## Scope and evidence

Inspected the runtime flat asset catalogs, room graphs and presence rules, main and side quest stages/tasks, event conditions/actions, dialogue entry selection and chains, encounters and drops, item/reward definitions, crafting and tuning definitions, progression, discovery, navigation, combat victory persistence, and departure boundaries. Compared existing story references, walkthroughs, readiness notes, and relevant tests. The older nested world files are not the authoritative graph loaded by `WorldAssetDataSource`.

Evidence labels below: **C** means source inspection; **T** means executable host evidence; **R** means phone acceptance still needed. C/T findings are not claims of a reproduced phone session. No phone playthrough or fresh combat-balance simulation was performed in this assessment. World 4 was inspected only at the World 3 handoff boundary.

Seven selected test classes passed: **72 tests, zero failures/errors**: DataIntegrityTest (30), World2RefinementTest (16), WorldTwoDiscoverabilityStateTest (10), Hub3CriticalFlowTest (2), DialogueTriggerHandlingTest (2), AudioRouterTest (7), OpeningNarrativeMigrationTest (5). These establish their tested contracts, not end-to-end UI readiness.

Separate probes loaded the actual dialogue catalog and `DialogueService`, then selected dialogue using both compiled production predicates (Android and shared/desktop). Both produced the same incorrect selections. [Captured results and source hashes](evidence/WORLD_2_AND_3_INDEPENDENT_ASSESSMENT_2026-10-29.json) preserve the baseline and limits. Probe trigger handlers were intentionally inert: selected lines were executed, their reward mutations were inspected in source rather than exercised.

## 1. Critical blockers

### B1. Production dialogue conditions do not implement authored task/evidence gates — both worlds

**C/T.** `gameRuntime/.../dialogue/DialogueTriggerLogic.kt:isDialogueConditionMet` and the Android-local predicate in `app/.../di/AppServices.kt` omit `quest_task_done`, `quest_task_not_done`, `milestone_set`, and `quest_not_completed`. Unknown tokens return true. Supported `quest_not_started`, `milestone`, and `milestone_not_set` should not be confused with these missing tokens.

Observed selections using each real predicate:

| Scenario | Actual selected line | Consequence |
|---|---|---|
| W2 crash introduction already finished | `zeke_w2_crash_1` | Completed introductory task gate does not retire the opening. |
| W2 Botanist active, zero scans | `zeke_w2_sq01_turnin_1` | Turn-in is offered before its collection work; its authored trigger awards completion/rewards. |
| W2 Astra inspected, Orion available | `orion_w2_mq05_gate_1` | Earlier gate hint still takes precedence over optional offers. |
| W3 Vance before evidence or investigation | `director_vance_default_talk` | Evidence gate passes; its confession trigger can grant the case outcome prematurely. |

An additional Orion probe selected the awakening line before alignment; this proves the predicate failure, **not** that the room's presence rules let a player physically meet Orion at that moment.

The existing `LegalRouteCampaignRunnerTest` supplies its own evaluator with these missing implementations. Other focused dialogue harnesses likewise do not establish production predicate parity. Their successful authored routes cannot disprove B1.

Required response: implement the authored condition vocabulary consistently, test actual production entry selection and completion, and check both platforms. This touches shared infrastructure and should be treated as a deliberately approved fix, with World 1 regression coverage; no such fix was made here. Unknown-token behavior also needs an explicit validation policy so future omissions cannot silently grant progress.

### B2. World 3 Homecoming cannot expose the conversation needed to complete it

**C; phone reproduction pending.** `spire_zekes_apartment` has no base NPCs. Its only Zeke presence rule requires `ms_w3_mq11_complete`. Yet the safehouse dialogue beginning `zeke_w3_mq11_safehouse_intro` ends by firing `w3_mq11_establish_shield`, which sets that very completion milestone and starts MQ12.

Entering the apartment records `find_apartment` and a message; it does not start this conversation. Its only room actions are the later workstation and planning table. `ExplorationController.visibleNpcsForRoom` applies the presence requirement literally. The ordinary interaction route therefore cannot offer Zeke before the quest that needs him is complete.

`Hub3CriticalFlowTest` directly calls `harness.dialogue.startDialogue("Zeke")`, bypassing NPC visibility. Its passing main-quest test does not exercise this dependency. Make Zeke reachable at the correct safehouse stage and test the visible NPC interaction, then normal stage advancement and post-quest presence.

## 2. Required pre-playtest fixes

| ID | Finding and evidence | Required outcome |
|---|---|---|
| P1 | **W3 Neon Fix: C.** `neon_sign_core` has a 50% drop on `sentinel_mki`; that enemy has one authored W3 room source, the Private Booth branch. No alternative grant/shop/recipe was found. Static victories persist encounter clearance. | Make required optional-quest completion deterministic or provide a reliable recovery source. Verify both drop outcomes and killing the enemy before accepting the quest. This is a completionist blocker, not a main-story blocker. |
| P2 | **W3 early boss defeat: C, targeted reproduction required.** Physical connections lead Archive → Drone Test Alcove → Landing Pad Roof without a quest gate on those links. Administrator is statically present, while `w3_mq15_defeat_administrator` credits only active MQ15. Victories persist clearance independently. | Verify an early roof visit/victory. Prevent losing the future quest credit, either through appropriate access/encounter timing or reliable prior-victory reconciliation. No W3-specific reconciliation was found in inspected paths. Treat as a probable progression softlock until disproved; not a reproduced phone failure. |
| P3 | **W2 Rapid Capacitor: C.** Cooldown-reduction promise does not match the item's speed/agility stats and no corresponding cooldown implementation was identified. | Resolve the promise versus actual effect before asking a tester to validate that reward. Check other-world ownership before changing its shared definition. |
| P4 | **W3 Neon Fix discount: C.** Completion sets `ms_w3_streetwise_unlocked` and updates room prose, but no runtime pricing consumer of that milestone was found. | Verify whether the promised permanent discount has an actual pricing path; implement the intended reward or correct the promise. A changed description is not a functional discount. |
| P5 | **W3 Prototype Testing: C.** The prototype-rack sequence records progress through events/messages rather than an actual spawned prototype encounter; the reward is an accessory, while wording presents it as a mod. | Decide whether this is an inspection/calibration task or a combat trial and make the existing task/reward presentation truthful. An additional fight is not automatically necessary. |
| P6 | **W3 first combat credit: C.** `w3_mq11_clear_landing` accepts any encounter victory while active, without room/enemy filtering. | Bind the objective to its intended encounter(s), or explicitly make the objective any suitable sewer victory. Check progression after retreating and fighting a side-branch crawler. |

B1/B2 are the first repair group. P1/P2 follow before a serious World 3 completion test. P3–P6 are smaller correctness decisions, not reasons to redesign the world.

## Functional assessment: World 2

The live catalog contains **93 rooms, ten nodes, two hubs, five main quests, five side quests, 164 room actions, and 60 description variants across 41 rooms**. Thirteen rooms have direct enemy presence; the Beast is additionally event-spawned. These counts describe scope, not quality quotas.

| System | Established implementation | Assessment and remaining acceptance |
|---|---|---|
| Arrival and onboarding | Crash-site dialogue, Chime and supplies, treatment consuming a Medkit, stream transition to MQ02. Zeke carries over from W1. | Coherent survival opening. B1 invalidates dialogue sequencing confidence. R: consume/sell early supplies, journal guidance and popup order. |
| Rooms and transitions | Coast/caves, canopy, Temple, Hall, Stasis, Source routing and Hangar; optional branches and return salvage. Temple/Source state gates have reconciliation paths. | Functional variety and clear broad escalation. R: physical exits, minimap/discovery, shortcuts and reload at gates. Source Gate's node has separated local branches; this is a map usability question, not proof of disconnected campaign geography. |
| Quest progression | Main awards 150/200/250/300/350 XP. Task and milestone events lead from rescue through signal, awakening, Beast, Bridge installation and departure. | Authored causal chain is substantial. Production dialogue gates must agree with it. Inspect journal after each state-changing interaction, not only final completion. |
| NPCs and optional quests | Charlie, David, Penny, Kaelen, Megan, Sentinel, Orion and companions have authored roles; five side routes include Botanist and crafting/exploration. | Previous exit/repeat fixes exist in source. B1 still permits early Botanist turn-in and shadows Orion offers. NPC presence, commerce, quest offers and terminal exits require combined UI checks. |
| Puzzles/tutorials | Clue-driven Stasis ring choices, Source feed/routing choices, optional bioluminescent tuning; crafting, fishing and later Link progression. | Their different presentation is legitimate. Main puzzles do not need the optional Prism tuning UI merely to resemble W1. R: wrong choices, leaving mid-sequence, hints, retries and unlock discoverability. |
| Combat | Flora/ruin enemies plus the Source Beast; victory/retreat/defeat hooks and drill aftermath. | Existing seeded Beast test demonstrates one viable route, not balanced difficulty. R: hit rate, status strength, healing pressure, progression-dependent builds and boss recovery. Zero credit/loot on some enemies is not inherently defective if XP is intended. |
| Economy and rewards | Five main quests total 1,250 quest XP; five sides total 650 quest XP, with separate items/AP. Bridge and Chime have authored departure use. | Check inventory deltas, once-only rewards and P3. Ordinary emergency Medkit can be spent before treatment: verify the replacement purchase route and affordability rather than assume an irrecoverable lock. |
| Narrative and completion | Injury → exploration → Orion → hunter/Gh0st → repaired Astra. The Sky rooms are docked planning projections. Confirm/cancel departure and pending cinematic callback exist. | This is its own functional structure. Check whether confrontation credit remains coherent if dialogue is interrupted, and whether drill/launch scenes resume correctly after process death. No additional cinematic quota is justified. |

Prior W2 local fixes inspected include unconditional conversation exits, Kaelen repeat/reward guards, companion guidance/presence adjustments, physical gate reconciliation, Botanist's report step, and departure sound handling. These are **previously committed changes**, not work performed in this assessment. Their production behavior still depends on the paths described above.

## Functional assessment: World 3

The live catalog contains **70 rooms, ten nodes, two hubs, five main quests and six side quests**. Twenty rooms have description variants. Thirty-three room IDs contain `_scale_`; none of those is a one-action empty filler room by that criterion. Room names or file IDs alone cannot establish filler quality. Eight rooms have enemy presence; the landing roof uses authored encounter parties.

| Main quest | Actual functional route | Assessment |
|---|---|---|
| MQ11 Homecoming | Sewer victory → vent → apartment → safehouse conversation/shield; 400 XP. | B2 stops the normal route. P6 makes the first objective broader than its prose. |
| MQ12 The Plan | Jax intel, patrol mapping, security records, copied badges, disguises, blueprint workstation, assembled planning table; 450 XP. | Meaningful multi-location preparation and prerequisite checks. Security-kiosk data gathering is labelled as guard interrogation in the task vocabulary: align player-facing expectations. Test all prep orders and earlier inspections. |
| MQ13 Social Engineering | Registry/cover preparation, Laundry sensor work, service-route entry; 500 XP. | Authored infiltration sequence exists. Test whether physical navigation can bypass expected staging and whether reminders follow actual task state after B1. |
| MQ14 The Lens | Archive entry, containment/shutter/tether clues, resolved prism route and Lens acquisition; 550 XP and source-art scan unlock. | Investigation exists without a separate tuning minigame. Archive has no Zeke NPC presence despite an authored Zeke Lens-warning dialogue: verify how the player receives this beat. Direct Lens action exists, so missing warning is not itself a progression blocker. |
| MQ15 Burn Notice | Roof access, Administrator victory, Shield-gap scan, Astra launch; 700 XP and W4 handoff. | Quest/event chain exists; P2 threatens order safety. Launch directly completes and warps, unlike W2's confirmation/cinematic flow. Assess clarity and accidental activation; do not assume every world needs identical departure presentation. |

Main quest awards total **2,600 quest XP**, excluding enemies and sides. This is not evidence of correct level balance by itself.

| Side quest | Implemented route / reward | Assessment |
|---|---|---|
| Neon Fix | Sign/core installation through Jax; 200 XP and promised shop benefit. | P1 deterministic supply; P4 real discount. Check optional dialogue priority during main Jax guidance. |
| Cold Case | Erased memorial, security case records, relay; 200 XP and Gh0st intercept unlock. | Object-led discovery is a valid alternative to a standing quest-giver NPC. Verify the unlocked capability is usable and explained. |
| Night Market Run | Power trace/reroute, lanterns and registry; 200 XP and Neon Band. | Has authored exploration steps. Test action order, Mika visibility and once-only award. |
| Corporate Espionage | Concierge mirror, safe and disclosure/dead-drop branch; 250 XP with distinct ledger/accessory outcomes. | A real optional choice. Test exclusive outcomes, replay and branch persistence; do not flatten it into one W1-style turn-in. |
| Prototype Testing | Targeting audit, prototype-rack step and roof telemetry; 300 XP and Phase Rounds. | P5 presentation/mechanics mismatch. `phase_rounds` has one item definition; a second quest awarding the same accessory is not a duplicate-ID bug. |
| Murder at the Skyline Salon | Barrier starts case; glass, terminal and rig evidence; Vance confession; 450 XP and two items. | Substantial evidence structure undermined by B1, not an absence of evidence design. Test all evidence orders and attempted early accusation through the actual NPC UI. |

W3 supports urban exploration, disguises, records, source-art investigation and branch rewards. It does not need extra cinematics or artificial enemy variety before these mechanics can be tested. Administrator difficulty cannot be inferred from 420 HP alone: party state, stability, summon abilities, encounter partners and statuses matter.

## 3. Safe self-contained improvements

These are candidates, **not implemented changes**:

- Add content validation for unknown dialogue condition tokens and direct production-predicate tests. This is diagnostic/test work; changing shared semantics belongs to B1's repair group.
- Add focused visibility/access tests for each required quest conversation and tests for required-item supply after an encounter has been cleared.
- Correct verified task/reward wording discrepancies after choosing the intended mechanics; retain W3's investigation and infiltration structure.
- Inspect the one-shot shortcut events on W3 branch termini. Reusable-looking ladders/returns should not silently stop working after their event receipt is recorded; normal backtracking still exists, so this is not currently classified as a main-route blocker.
- Add localized environment theme entries where appropriate. W2's `swamp` theme is absent; W3's city-themed secret space needs the same check. Existing fallback behavior is not proof that a room is unrenderable.

## 4. Focused playtest verification

Run after B1/B2 and the relevant route-safety fixes. Keep both a fresh earned W1 exit save and a snapshot before each irreversible boundary. Do not use manually granted quest milestones as the only acceptance route.

### Shared checks

1. Start, advance, choose, dismiss and reopen NPC conversations from their actual visible interaction controls. Confirm every conversation ends, navigation remains usable and quest/shop interactions coexist.
2. Check task journal, tracked quest, inventory, XP/AP and party membership before and after each award. Repeat interaction and reload; no skipped prerequisites or duplicate payouts.
3. Background, kill and relaunch during dialogue, queued rewards, a puzzle, combat return and departure. Confirm one coherent outcome and usable recovery.
4. Test with touch input on Pixel: crowded action/NPC rows, map/minimap, banners, abilities/details, reward readability and audio overlap. Host tests do not establish these results.

### World 2 acceptance route

- Crash: finish intro, reopen Zeke, inspect console once, treat injury; test early Medkit consumption on a separate save.
- Reach stream and Temple by normal exits; check wrong/early stone-lock actions and alternate accepted combat victory.
- Stasis: inspect all clues, deliberately make one wrong decision, leave/reload, awaken Orion fully, recover Bridge separately.
- Offer/accept every side quest, especially Orion after Astra inspection. Attempt Botanist turn-in at zero/four/five scans and confirm exactly one report reward.
- Repeat Charlie/Kaelen/vendor conversations; complete favor/crafting/fishing branches and verify reward effects against descriptions.
- Beast: retreat or lose once, retry, win; confirm Gh0st recruitment, drill/Link and party state once only.
- Route power, inspect ship, return for salvage, install/consume correct items. Cancel launch once, then confirm; check saved transition and W3 arrival.

### World 3 acceptance route

- Enter from an earned W2 departure. Complete landing encounter and reach apartment; confirm Zeke is visible **before** MQ11 completion and MQ12 starts exactly once.
- Complete heist preparation in differing orders. Inspect workstation/planning table early; all missing steps remain discoverable and NPC guidance updates.
- Visit Upper City/roof as early as permitted. Attempt early Administrator victory on an isolated save; eventual MQ15 must remain completable.
- Complete registry/Laundry infiltration; follow Archive clues with one wrong/early action. Confirm Lens award, warning delivery, scan unlock and alarm state.
- Attempt Vance confession before evidence, after each evidence item and after full investigation. No premature outcome or repeated rewards.
- Kill the Neon Core enemy before quest acceptance and exercise the no-drop case; Neon Fix must still have a reliable completion path.
- Finish all six side quests, both espionage branches on separate saves, real discount behavior and prototype reward/effect.
- Fight rooftop parties with ordinary earned equipment. Record turns, misses, healing, summons, stability/guard breaks and KOs; compare to earlier W3 encounters instead of tuning from HP alone.
- Scan Shield gap only when eligible; launch once. Confirm W3 journal closure, W4 quest/room/party/inventory continuity and available return-travel behavior. This checks the boundary, not W4 readiness.

## 5. Later polish and creative decisions

Phone review should decide ambience continuity, placeholder/shared portraits, readable branch navigation, puzzle feedback, repeat dialogue variety, room density and narrative pacing. Missing explicit per-room audio bindings do not prove silence: hub/environment fallbacks exist. A shop's direct action bypassing greeting dialogue does not prove commerce is broken. Main puzzles do not automatically need the optional tuning presentation.

W3's Lens-warning placement and MQ12 task wording deserve continuity attention. W2's optional offer visibility deserves repair before adding dialogue. Preserve W2's survival/awakening arc and W3's urban heist/investigation identity. Defer future inter-world cinematic expansion as previously requested.

## Repair order and work remaining

1. Repair production condition parity; add production entry-selection regressions and run W1 quest-NPC checks.
2. Repair W3 safehouse visibility, deterministic Neon Core recovery and early-Administrator credit/access. Add tests that use ordinary room/NPC access rather than injecting final events.
3. Resolve reward/task truthfulness and verify optional route ordering.
4. Run earned-route host/controller checks, then focused Pixel regression and balance/presentation observation.

Rough engineering planning allowance: **1–2 working days** for the first two repair groups and targeted regressions if no further dependency emerges; **another half to one day** for reward/wording/optional-route cleanup; **one or two dedicated playtest sessions per world**, followed by fixes sized from actual findings. These are estimates from identified scope, not measured implementation time or a completion percentage. B1's shared reach and P2's reproduction are the largest uncertainties. A full new narrative/combat expansion is neither included nor currently justified.

## Changes actually made

Only this assessment and its evidence artifact were added. No gameplay/content edits, implementation fixes, commit, push, site publication or device installation were performed. The supplied Gemini assessment was preserved unchanged.
