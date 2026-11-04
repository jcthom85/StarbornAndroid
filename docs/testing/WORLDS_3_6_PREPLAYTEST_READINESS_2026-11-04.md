# Worlds 3 to 6 readiness before playtesting

**Implementation followup:** the approved stabilization changes and regression results are recorded in [the repair record](WORLDS_3_6_STABILIZATION_2026-11-04.md). The findings below describe the assessed baseline.

November 4, 2026. Baseline `e9bcfd06`, version 1.3.106 build 190. Assessment only: no gameplay/content fixes, device installation, commit or push.

**A targeted stabilization pass is needed before ordinary playtesting. The later worlds contain substantial campaigns, not missing outlines, but World 3 still has an immediate main-quest access blocker and all four have unsafe progression edge cases.** Fix World 3 first so its playtest can proceed; repair each later world's blockers before its own session. A wholesale rewrite of Worlds 3–6 is not justified.

This reassessment uses the [World 1 functional contracts](../standards/WORLD_1_FUNCTIONAL_STANDARDS.md) plus the actual World 2 refinements: visible/reachable NPC progression, explicit interaction availability, persistent state across reloads, exactly-once rewards, honest feedback and deliberate world handoffs. It supersedes older readiness conclusions where fresh observations below differ. World-specific creative structures should remain distinct.

## What was checked

Read the current runtime catalogs and the controller/event/dialogue paths, earlier assessments, relevant walkthroughs and production campaign tests. Fresh verification includes the complete Android host suite: **785 tests, zero failures/errors/skips**. Its production crew/campaign route reaches completion with real combat and boundary saves. However, the route helper can call an NPC by name without requiring its pill to be visible; some tests also exercise author-script events directly. Those passing tests do not establish ordinary interaction accessibility or alternate exploration order.

A separate desktop observation probe uses the actual shared exploration controller, production dialogue predicate, event manager and protocol loader. It records NPC visibility, conversation graph exits, early encounter clearance, public movement, reward execution and arena receipts. The probe completed successfully **as an observation exercise**; it records confirmed defects rather than asserting they are fixed.

Early-kill probes inject victory results through `onCombatVictory` rather than playing fights. The Administrator probe uses the actual Administrator/Aero Drone party, not the separate roof guard party. Subsequent objective acceptance and save/load test whether clearance and missing credit recover. Public-path probes separately use actual `travel` calls from seeded earlier quest states. These are controller reproductions, not a phone playthrough or difficulty certification.

Evidence: [runtime observations](evidence/worlds_3_6_readiness_2026-11-04/runtime-observations.json), [probe source](evidence/worlds_3_6_readiness_2026-11-04/RuntimeReadinessProbe.kt.txt), [source hashes and verification](evidence/worlds_3_6_readiness_2026-11-04/verification.json), [content inventory](evidence/worlds_3_6_readiness_2026-11-04/content-inventory.json). The temporary executable probe was removed from the test source tree afterward; it is preserved as evidence, not a misleading permanent passing regression.

## World readiness

| World | Current substance | Before its playtest |
| --- | --- | --- |
| 3 — The Spire | 70 rooms, 10 nodes, 5 main and 6 side quests. Implemented safehouse/heist/infiltration/Lens/roof/departure progression. | Fix Zeke access, early Administrator credit and Neon Fix recovery. Resolve false reward/trial promises and refresh the guide. |
| 4 — The Foundry | 74 rooms, 10 nodes, 5 main and 6 side quests. Machinery investigation, Gh0st history, Anvil, Titan and ship-upgrade handoff exist. | Fix two NPC exits, Crucible state/retry, early Titan credit and insufficient-quantity favor payment. Decide what the narrated trials actually require. |
| 5 — The Void | 81 rooms, 9 nodes, 5 main and 5 side quests. Ring infiltration, Firewall, Anchor and departure sequence exist. | Fix Charles exit/payment and early encounter credit. Preserve access to safe preparation while fixing progression. Repeated side rooms merit a separate quality pass. |
| 6 — The Source | 65 rooms, 10 nodes, 5 main and 5 side quests. Nightmares, crew anchors, ascent, both final forms, tuning, ending and postgame exist. | Protect out-of-order victories and prevent access to the epilogue before the ending. Check final-boss transition clarity and ending recovery. |

Counts describe authored scope, not completion percentages. Room inventory is based on hub-node ownership, not prefixes alone. Referenced images in the checked room/node/hub and placed-NPC/static-enemy catalogs exist: 94 references in W3, 91 in W4, 104 in W5 and 85 in W6, with no missing files found. This does not certify visual quality or every scripted asset/audio cue.

## Confirmed blockers and progression risks

### B1 — World 3 safehouse hides the required Zeke interaction

**Controller reproduced.** At `spire_zekes_apartment` with Homecoming active, entering credits `find_apartment`, but the NPC list is empty, no required conversation starts and the available actions are only workstation/planning table. Zeke's sole presence rule requires `ms_w3_mq11_complete`; his safehouse conversation is needed to produce that completion.

This is an ordinary main-route blocker, not an optional branch issue. Make the NPC or equivalent standard dialogue accessible before completion. Add a regression that uses the visible NPC interaction rather than directly calling `onNpcInteraction("Zeke")` with an invisible character.

### B2 — Rylan, Cameron and Charles still lack terminal conversation exits

**Actual production entry and available graph inspected at runtime.** Rylan at Lava Tube Bypass, Cameron at Pipe Gallery and Charles at Customs Lounge are visible. Their default menus have no reachable terminal path in the tested ordinary state; topic/favor branches return to their menus. The production dialogue service refuses ordinary advancement while choices exist.

These are the same failure class as Charlie's earlier trap. Add an unconditional terminal option while retaining their favors and shops. Test fresh, favor-pending, incomplete-material and favor-completed states. Charles is particularly easy to encounter while preparing for World 5's arrival fight.

### B3 — Early victories can permanently remove later quest targets

**Controller outcome plus save/load reproduced, with separate public movement checks.**

| Case | Public early access | Result after early victory, quest acceptance and reload |
| --- | --- | --- |
| W3 Administrator | Archive Vault → Drone Test Alcove → Landing Pad Roof while MQ14 is active; no block in the probe. | The Administrator party remains cleared; `defeat_administrator` is not credited under MQ15. Its only authored static source is the roof. |
| W4 Titan | Conveyor Belt → Conditioning Chamber → Power Core → Titan Dock during MQ18; no block. | Titan remains cleared; the actual MQ20 task `defeat_titan_walker` remains missing. |
| W5 Firewall | Dock → Concourse → Security Hub → Service Shaft → Server Farm → Mainframe → Alpha/Beta/Gamma while MQ21 is active; no block. | Clearing both authored Construct sources before eligibility leaves `defeat_construct` uncredited after MQ23/gamma credit and reload. One early kill alone still leaves an alternate source; exhausting both is the demonstrated failure. |
| W6 Vale | Campfire → Echo route → memory bridge/stair → Spire → Center is traversable during MQ26. | Early Vale clearance remains persisted but `defeat_vale` is missing after MQ30/confrontation eligibility and reload. |

W5's Avatar and W6's intermediate memory waves/second final form have similar task-dependent victory handlers and permissive placement. Those additional failure chains are **source-level risks**, not separately reproduced kills in this pass.

Choose a local solution appropriate to each beat: stage the encounter until eligible, gate access where narratively justified, or reconcile a saved legitimate victory into later objective credit. Recovery must not repeat drops/XP/AP. Preserve alternative victory sources and exploration deliberately; do not replace shared “any matching enemy” filters globally.

### B4 — World 4 Crucible round identity and retries are broken

**Real event-manager reproduction.** Round-one victory with Magma Drones/Welder Bot writes **all three round-cleared milestones** because the victory filters overlap and do not require the correct current round. Their receipts are consumed together. Separately, the first round-start event becomes ineligible immediately after its first execution because it is not repeatable.

The optional arena can therefore award advancement without its later fights and fail recovery after loss/retreat. Add explicit round ownership/eligibility and a retry lifecycle; test full wins, defeat, retreat, re-entry and reload. Do not change shared victory matching just to repair this arena.

### B5 — World 6 lets the player enter the epilogue early

**Public movement reproduced.** With MQ26 active and no ending milestone, the controller can travel from Campfire through the main route to Center and east into `source_new_world`. This is an early leak of the aftermath, not an unrestricted credits trigger: the credits replay itself has a completion guard.

Tie epilogue geography and related boarding to the actual ending state. Preserve free postgame exploration after completion.

## Required correctness and content work

1. **World 3 Neon Fix is not reliably completable.** `neon_sign_core` has a 50 percent drop from Sentinel Mk. I, with one authored room source and no guaranteed grant, shop or recipe found. Static encounter clearance persists. A no-drop victory can strand the side quest. This is source-verified; the assessment did not reroll a phone combat. Supply a guaranteed/recoverable route, including when the enemy is killed before quest acceptance.
2. **Cameron's favor pays with too few materials.** Actual controller interaction with one Composite Plate and one Hydraulic Fluid exposes the turn-in and grants the wrench/completion milestone although two plates are required. Charles's analogous two-board condition is also deficient in source, but that payment was not separately executed here. Quantity-aware eligibility and atomic payment should accompany the conversation fixes.
3. **Neon Fix promises a permanent discount that has no identified pricing consumer.** The event sets `ms_w3_streetwise_unlocked`; references found are the event, milestone and room prose, not a live pricing implementation. Implement the intended effect or correct the promise before economy testing.
4. **W3 Prototype Testing and two W4 trials overstate what the interaction does.** W3's rack event sets `test_weapon` and narrates destroyed drones. W4's `conditioning records` sets `defeat_prototypes` and narrates a wave falling; the Overclocked terminal sets `survive_waves`. These actions do not run the promised fight/wave interaction. Real static enemies elsewhere do not make those objective grants a played trial. Decide between honest records/calibration tasks and real trials; do not add combat merely to meet a quota.
5. **W5 arrival access and preparation are linked.** The proven campaign policy goes to Customs, equips/trades/eats/rests, then returns for the two-Fighter screen. The sealed-airlock story does not match this physical access. Preserve a safe preparation route while preventing premature objective encounters. Locking everything immediately could remove the demonstrated recovery path.
6. **Refresh the World 3 guide before relying on it as an acceptance script.** It still describes flight through the outer Shield into orbit toward World 4 and blanket permanent side-quest lockouts. Current Foundry progression requires obtaining the hardware for high orbit there, and Astra return travel exists. The game data and current controller must govern the guide, not its “authoritative” heading. This pass does not certify the other later-world walkthroughs either.

## Largest quality gap

**World 5's optional room branches show the strongest measurable repetition.** Six groups of identical base descriptions span **32 of its 81 rooms**. Examples across Dock/Concourse/Solarium repeat the same service-alcove/maintenance-mark prose and inspection replies. Some “manual toggle” interactions only repeat generic maintenance-label flavor. Distinct world-specific text does exist elsewhere, including Elara's cable/hymn branch.

That warrants a focused side-branch pass for distinct purpose, clue/reward value and walking/combat time. It does **not** establish that World 5's main story is absent or justify deleting every room with a `_scale_` identifier. W3, W4 and W6 have no identical full base-description groups in this comparison; uniqueness alone is not a quality certification.

Other human evaluation priorities: W3 Lens warning versus actual object interaction; W4 Gh0st/Rylos emotional delivery through short object messages versus available scripted dialogue; W5 arrival difficulty and resource margin; W6 moving through several rooms between the two final forms. These are pacing/presentation questions, not proof of missing endings.

## What the World 1 and 2 work already carries forward

Shared restoration replaces future room/quest caches when loading earlier saves; ground loot now persists; the dialogue predicate understands task gates; read-state behavior, party overflow cues, tinkering structure, rest behavior, enemy full-name fallback and prompt handling are shared improvements. The old production dialogue-token bug is **fixed**, not a current blocker.

Local content does not automatically inherit those fixes: missing terminal dialogue options, impossible NPC presence rules, one-shot arena starts, early-clear objective guards and unimplemented narrated tasks remain authored problems. World 2's Orion identity reveal, ball interaction and first Astra boarding are creative decisions, not mandatory templates for every later world.

## Recommended order

1. **Before World 3:** fix safehouse access, Administrator order safety and guaranteed Neon Fix recovery. Resolve the discount/trial wording and update the guide. Run an earned route through visible controls, plus early-roof and no-drop side-quest cases.
2. **Before World 4:** NPC exits/quantities, Crucible sequencing/retry and Titan order safety. Decide trial behavior with the existing content; a broad narrative rewrite is unnecessary.
3. **Before World 5:** Charles and order safety, then an arrival/resource check. Rework repetitive optional branches separately rather than holding World 3 behind that polish.
4. **Before World 6:** prevent out-of-order finale/memory clearance and early epilogue access. Check ending skip/reload/credits flags and the physical transition between final forms on the device.

A small adversarial regression group should accompany those fixes: valid visible NPC access; unconditional exits; early kill/accept/reload; arena wrong-round/retreat/retry; undersupplied favor payment; and pre/post-ending travel. Keep the earned campaign route too, but stop using its success as evidence that all NPCs are visible.

No gameplay/content changes were made in this assessment. No device regression run was performed. Existing unrelated workspace files were preserved.
