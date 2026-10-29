# World 1 functional reference for Worlds 2–6

Assessment date: October 29, 2026. Gameplay/content baseline: commit `e44e40d9` on `main` (Release 1.3.100 plus the Part 3 changes). This is an assessment, not a gameplay change or release sign-off.

World 1 is the functional reference because it has received three parts of human playtesting and several implementation passes. It is **not certified defect-free**. Reuse the contracts below, not every implementation detail or historical recommendation. Passing host tests does not certify phone usability, balance, music, or emotional pacing.

Companion: [focused regression checklist](WORLD_1_REGRESSION_CHECKLIST.md). Retain the [existing full phone acceptance checklist](../playtest/WORLD1_ACCEPTANCE_CHECKLIST.md) and [test/evidence workflow](../testing/WORLD1_PLAYTEST.md).

## Evidence and confidence

- **C — code/content verified:** inspected an executable path or authored data. Does not establish device behavior.
- **T — tested in this assessment:** relevant host/controller/Compose tests passed at the baseline. Coverage is limited to their assertions and fixtures.
- **R — runtime acceptance required:** timing, touch, audio, readability, difficulty, or lifecycle behavior must be observed on a recorded build/device.
- **D — discrepancy/deferred:** source differs from a historical note, a proposal was explicitly excluded, or evidence is insufficient. Do not mark closed from prose alone.

Fresh verification: `:app:testDebugUnitTest` selected ten classes/method groups (100 test cases) and `:desktopApp:test` selected five classes (18 cases): **118 passed, zero failures/errors**. Includes three production `LegalRouteCampaignRunnerTest` cases: earned World 1 handoff/save parity; Warden-before-Jed recovery; completionist ten-quest/cart route. Also includes `Hub1CriticalFlowTest`, `DataIntegrityTest`, `EventManagerTest`, `DialogueServiceTest`, `UIPromptManagerTest`, `PartThreeBalanceTest`, inventory boundaries, audio routing, merchants, rest, quest presentation, and radio/consumed-item presentation. Invocation/output: `/tmp/starborn-w1-standards-audit.log`; generated reports: `app/build/reports/tests/testDebugUnitTest/`, `desktopApp/build/reports/tests/test/`. Temporary logs/reports are local evidence, not permanent proof for a future checkout. No fresh device, Android instrumentation, Maestro, or Windows run was performed for this assessment.

The earlier 112-case Part 3 run is historical evidence for that change set, not a full-suite result. Earlier October 19/23 device results likewise do not certify the October 29 content. The user's completed three playtest parts are observation sources; no post-fix signed phone run was supplied here.

## Actual scope and progression spine

`hub_nodes.json` assigns World 1 ten nodes and 82 distinct rooms across `hub_1_homestead` and `hub_2_logistics`: Pit (10), Workshop (8), Med Bay (7), Trade Row (9), Transit Checkpoint/admin_gate (6), Admin Concourse (8), Server Room (7), Deep Mine (13), Echo Chamber (6), Launch Bay (8). World 2 arrival is included only as World 1's handoff boundary. Node ownership, dynamic NPC presence, action visibility, and event-injected encounters matter more than room-title prefixes.

| Beat | Actual contract and evidence | What later worlds should inherit |
|---|---|---|
| `w1_mq01` Wake Up Call | Bunk light → safety fault/door → Jed/starter equipment → loader → crafted Functional Cryo-Inductor → grounded suit → deliberate governor bypass → live test. `quests.json`, `events.json`, `Hub1CriticalFlowTest`. | Teach an action before requiring it; readiness and completion are different states. Do not complete a quest merely for reaching a room or crafting an intermediate item. |
| `w1_mq02` Shift Clearance | Scanner rejection precedes Zeke's paperwork override and temporary badge. | Make denial legible, provide a reachable resolution, and ensure the reward actually opens its promised gate. |
| Certification within `w1_mq03` | `w1_sq03` Heavy Lifting is required Guard Break training despite its side-quest ID. Hydraulic Kick/shield drill and return to Boggs establish mine authorization. Loader/cargo practice and the mine riot-post detour are optional. | Determine optionality from live gates and task flags, never `sq` naming. Mandatory teaching must have a guaranteed acquisition path. |
| `w1_mq03` The Echo | Authorized mine descent, power restoration, threshold, clue-based three-stage calibration; cinematic completion consumes the Inductor, grants Fork/Blast Wave, changes room state and enables escape. | Connect puzzle clues, observable state, consumption, rewards, and the next route; persist interrupted completion safely. |
| `w1_mq04` Red Alert | Lockdown broadcast → Zeke comms → security gauntlet → Jed's bypass/Chime handoff. `w1_mq04_complete` starts/tracks `w1_mq05`. | Escalation should follow a player-caused change; a character's location, sacrifice, and reward need causal setup. |
| `w1_mq05` The Launch | Warden encounter → Zeke's Chime splice/authorization → navigation console → launch/crash callback → completion and World 2 arrival. | Departure is a transactional handoff: finish the old objective, carry the roster/resources, set the new location and start/track the new objective. |

Five authored side quests: Scavenger's Stash (Scrapper/cache/return), System Flush (Doc/ventilation/return), Heavy Lifting (required certification), Protocol Override (thawed console/key recovery), Lost Shift (powered-mine shunt/datapad/letter). Creative subjects are local; delivery, gating, journal coherence, and exactly-once rewards are reusable.

## Reusable functional contracts

### Navigation and transitions — NAV

1. Use one authoritative room/node/hub/world state. `ExplorationController.travel`, direction evaluation, `visibleConnections`, room states, and node-exit resolution govern both UI and attempts. Locked routes explain the missing requirement; successful gates remain correct after revisiting/loading.
2. Arrival updates description/variants, background, NPCs, visible actions, encounter parties, exits, map discovery and room audio together. Room transition animation must not grant extra movement or replay arrival rewards.
3. Darkness restricts actionable visibility and navigation; escape/backtracking remains intentionally defined. The bunk's light/conduit gate is a local teaching example, not a rule to trap all rooms.
4. Leaving an unfinished puzzle and returning must preserve meaningful progress. The first authorized Elevator Lobby descent intentionally reveals the map before entry; later crossings use the continuous route. Copy that *purpose*, not its room-ID special case.
5. Show navigation affordances consistently while conversations own input; disable activation rather than removing layout. Android currently retains direction arrows/overworld gateway during dialogue. Verify input and visual parity separately on desktop.
6. Map and minimap use the shared `MapPresentation.kt` room states. Minimap: position/routes, gold current room, diamond exit rooms, small red threats and blocked-route bars. Services and destination detail belong to the full map. This supersedes older “prioritize shop icons on minimap” proposals.

### Quests and event execution — QST

1. `quests.json` describes stages/tasks; `events.json` and dialogue triggers drive the actual transitions. `QuestRuntimeManager` persists stage/tasks and journal state; it is not safe to infer live reward delivery solely from a quest's `rewards` array.
2. Identify the reward owner at every grant. Event `give_item`/`give_xp` actions, dialogue triggers, encounter payouts, and AP claims can all participate. A descriptive reward list must not cause a second payout.
3. One-shot events check `completedEvents`; repeatable events declare intent and gates. `EventManager` reports completion when actions execute, not a universal rollback transaction. Compound actions require explicit atomicity/recovery review.
4. Cinematics with completion actions persist `pendingEventCinematics`; recovery resumes eligible callbacks instead of replaying earlier grants. Verify skips, normal finish, load, and force-stop boundaries; a missing/ambiguous recovery mapping is not automatically discarded.
5. Alternate action order is a supported contract where intended: reading the loader diagnostic is not required after defeating it; early Warden victory must recover via Jed without another boss/reward.
6. Tracking and journal objectives must describe the current reachable step. Required training must not be documented as optional simply because its identifier says side quest.

### NPCs, dialogue, shops, and services — NPC

1. Conditioned active-quest lines precede generic conversation/old offers in `DialogueService.startDialogue`. Looping default greetings remain eligible; `scripted_only` radio calls enter by `startDialogueAt` and cannot hijack normal Zeke conversation.
2. Every reachable choice state needs a valid terminal path. Dialogue with available choices cannot advance past them by ordinary tapping. Test all relevant milestone states, repeat visits and gift depletion. **Charlie currently violates this rule**: every available option loops and none leaves.
3. Dialogue line/choice triggers execute on advance/selection. Verify leaving does not skip a required trigger and returning does not duplicate a gift. Save IDs and aliases used as references must survive display-name edits.
4. World 1 merchant pilot: Shopkeep in Market Strip and Scrapper in his stall use `trade_after_dialogue` plus a shop interaction. Run authored quest dialogue to completion first, then offer Trade/topics/Leave. Standalone shop actions elsewhere retain their own gates; do not blindly migrate every merchant.
5. Pam manages the Mess Hall; Rourke cooks in Ration Sink. Pam's biscuit gift remains keyed by the legacy item ID. Character occupations and personal details are local creative decisions; distinguish them from trade/rest/cook mechanics.
6. Rest and Cook are separate explicit controls below prose, with no duplicate clickable bed/cooker names. Preserve rest events, item rewards, HP recovery, cooking discovery and darkness gates. Cargo Lift's service recess is a local justification for recovery during an urgent escape.

### Tutorials and popup ownership — TUT/UI

1. Teach the next actionable mechanic, after the player can use it. Support settings that disable tutorials without stalling event callbacks or quest progression.
2. Guided encounters may seed teaching HP and restrict valid actions. `CombatTutorialTracker` uses loader and shield-drill eligibility; the guided shield target gets at least 240 HP. These are instructional fixtures, not general enemy-stat baselines.
3. `CombatController` delays explanatory feedback by action delay plus 1.1 seconds; reserved tutorial layout avoids the header/action area. Battle end clears pending explanations. Actual overlap and reveal timing still require phone regression.
4. `ExplorationNoticePolicy`, `UIPromptManager`, and shared `QuestPresentationState` coordinate several channels—not one universal queue. Active scene/modal ownership hides/defer notices; narration precedes announcements, then prompt presentation. Ordinary tutorials have priority 300, milestones 200, items 100, after-reward tutorials 50; consumed-item feedback has 350. Active reward groups stay coherent. Do not turn the historical priority sentence into a literal global ordering.
5. Dismiss by expected prompt ID where available so a stale input cannot remove the next notice. Timers must count visible time; hidden notices must not silently expire. Tests cover several cases, not every combination of modal state.
6. Item acquisition remains individual per type/group; consumption says ITEM USED, not ITEM ACQUIRED. Quest detail/summary has persistent queued ownership; progress banners support swipe and X. Passive credit/progress feedback must not obscure dialogue or become an extra acknowledgement chain.
7. “No Continue label” applies to passive/tap-dismissable narrative cards and recruitment hints. Keep explicit confirmation/Cancel/Continue controls for real decisions, inventory targets, respec, and other modal choices. Validate screen-reader and keyboard equivalents.

### Combat and feedback — CBT

1. Reuse shared action validation, legal target selection, cooldowns, ATB, Guard/Break, status durations, affinities and skill effects. Pausing Inspect/details or opening another modal must not leak combat turns or change the selected target unexpectedly.
2. Teaching progression is basic attack/Momentum/weakness on the loader, Hydraulic Kick/Guard Break in certification, then mixed enemy roles and a boss combining earlier mechanics. The encounter identities and damage numbers are creative/balance choices, not cross-world requirements.
3. Registered World 1 static enemies include Faulted Loader, Echo-Borer, Siren Skimmer, Acoustic Bulwark, Resonance Buoy, Dominion Dampener and Iron Warden. Event/patrol encounters must also be audited; an `enemies` array alone is not a complete encounter census.
4. Preserve truthful effect units. `AbilityPowerLabel` distinguishes attack-scaling percentages, base HP healing, Utility and overcharge. Raw power is not flat damage: the processor includes scaling stats, weapon roll, focus potency and downstream modifiers.
5. Critical feedback needs a readable lifetime, layout and target association. Guard Break uses 1.8-second animation and delayed fade; floating statuses should not create accidental modal popups. Inspect provides deliberate details/turn counts and unknown affinities.
6. Investigate mechanics before tuning global hit rates/boss HP. Sonic Shriek now applies one-turn Optics Static (0.9 accuracy multiplier), preserving ordinary Blind (0.5). Seed 42 yielded 1815/2000 hits normally, 886 under Blind and 1622 under Optics Static in the representative fixture. This does not establish win rate for the actual mixed encounter.
7. Corrosive Rounds retains 25% proc and +3 Strength but now applies fixed Corrosion (8 acid/target turn ×3), not Erosion (10% max HP/turn). Against the 250-HP fixture, three ticks are 24 versus 75 HP. Warden remains authored at 250 HP with his shield/burst/drones/gas/overclock abilities. Compare real earned builds with/without the mod before final balance acceptance.

### Inventory, rewards, builds, persistence — ECO

1. `Inventory.kt` publishes immutable entry snapshots on stack increments/partial removal. The dispenser remains repeatable by design; one coffee per tap must immediately agree across authoritative inventory, preview, travel and reload. Do not “fix” stale UI by changing a repeatable reward into a one-off.
2. Protect quest/key/unsellable items in shop logic. Require explicit consumption and recoverability for progression items. Inductor consumption occurs in the relic-sync cinematic callback; Chime authorization uses `take_required_item` before the splice milestone. Keep these separate from arbitrary discard/sell paths.
3. XP, credits, AP, story ability unlocks and equipment all have distinct consumers. Core abilities are earned separately from AP builds; respec refunds purchase receipts, not free story grants.
4. Production crew progression uses versioned `CrewBuildPrototype`/`NovaBuildPrototype` despite their historical names. AP is a shared pool. Current quest award table grants **2 AP** for `w1_mq01`, `w1_sq03`, `w1_mq03`, `w1_mq05`, with persistent claims; migration/backfill has separate rules. The old Part 1 1-AP suggestion is not the current contract.
5. Preserve inventory, gear, roster, claims, quests, room flags, pending cinematics and battle recovery across slots/quick/auto saves. Test legacy migration separately from a fresh-game economy; migration compensation is not earned income.
6. Repeated recruitment adds/announces only a genuinely new member in `EventManager`. Zeke joins during Chime authorization and must persist through World 2; a repeated `recruit:zeke` trigger is safe only while that membership invariant holds.

### Narrative pacing, audio, and world completion — NAR

1. Local actions produce visible consequences: dangerous work → bureaucratic threat → chosen complicity → revelation → pursuit → cost → escape. Give the player a reachable next goal at each transition. Dialogue, room variants, quest text and event order should agree.
2. The three scripted Zeke comms calls establish route, gate, and Jed's bypass position. His radio portrait/name distinguish remote speech from physical presence. Do not use generic narration for every spoken line.
3. Jed's wrench/bypass, Nova's grounding strip, the Fork and Chime are World 1 creative choices. Later worlds need similarly coherent cause/effect, not duplicated characters, sacrifices or calibration puzzles.
4. Combat music restarts per encounter while exploration resumes according to routing/session rules. Cinematic music owns its layer; the launch scene now has one opening music cue and no dialogue-triggered crash-track restart. **Duplicated launch/impact SFX dispatch remains a source-level concern** (see findings).
5. `w1_mq05_use_nav_console` requires active launch quest/stage and the splice milestone. Completion callback marks launch survival, awards XP, completes/milestones World 1, warps to `sector9_crash_site`, starts/tracks `w2_mq01`. C/T prove this path under test assumptions; R must establish uninterrupted/skip/reload presentation and no repeated recruitment/rewards.

## Verification of documented fixes

IDs below are prefixed by part to prevent collisions. P1/P2 reports preserve proposals alongside later implementation sections; later behavior may supersede early recommendations. P3 notes are appended to the walkthrough rather than maintained as a separate original triage file.

| Findings | Baseline evidence and assessment |
|---|---|
| P1 CB-01/02; P2 IC-01/02/03 | C: `CombatTutorialTracker`, `CombatController`, `CombatScreen` reserve guidance, delay explanations, seed survivable teaching HP. Existing tests cover flow; current assessment did not rerun the entire tutorial-controller/instrumentation suite. R: actual header/animation separation and follow-up attack with multiple builds. |
| P1 CB-03 | C: `onTinkeringCrafted` closes menu before dispatching craft event, which starts Jed dialogue. **D: no explicit 1.2-second delay in this path**, despite the old checklist's claim. R: watch real modal disposal and dialogue reveal. |
| P1 CB-04/05; IC-03/05/08; P2 IC-08/09 | C: AP guidance, category-formatted tutorial titles, highlighted UI, Abilities terminology, respec confirmation/refund and field-use restrictions exist. AP guidance may wait behind rewards. R: fresh-player teaching order, font scaling and accessibility; the word “skill” remains an internal model term. |
| P1 PB-CHECK-01/02 | C: shop protections and persisted room/task gates. T: integrity and interrupted-puzzle progression checks. R: inventory sale/target cancellation and save/load at the actual UI boundary. |
| P1 IC-01/04 | D: intro timing was deferred; rest now emits a sound cue, so “no rest sound” is not current source behavior. R: perceived pacing and audible feedback, not just dispatch. |
| P1 IC-02/06/07/09/10 | C: hidden inline bunk possessions, explicit Rest, separate notice owners, grouped rewards, important quest cards and currency paths. T: prompt/quest/merchant/rest checks. D: current presentation supersedes the initial “three clickable bunk nouns” and simple priority ladder. R: complete simultaneous reward/quest/tutorial bursts. |
| P1 IC-11/12/13/14 | C: diagnostic state gates, explicit bypass and live test, ventilation-specific actions, open puzzle departures, retained Jed's Shutter name. T: main/optional routes. R: puzzle variety/clarity; not every physical action gained animation/sound. |
| P1 IC-15/16 | C: Film 02 acquisition is linked to checkpoint content; AP source table is 2 for Wake Up Call. D: campaign-wide ten-tape pacing not established; 1-AP proposal differs from production. Do not silently retune to satisfy historical prose. |
| P1 IC-17/18/19/20; P2 IC-07 | C: Inspect/unknown affinity/status presentation, snack feedback, decorative floating badges and revised combat feedback. R: dense enemy layouts, touch-through selection, long strings and enlarged fonts. |
| P1 IC-21/22/23; P2 IC-06 | C: shared map glyphs, hub menu/details, first-mod tutorial path, merchant pilot and quest-sale guards. T: route/merchant coverage. D: minimap service-icon proposal is superseded by route-only minimap; merchant pilot is not a global automatic migration. R: discoverability, Quick Save, navigation and device parity. |
| P2 CB-01 | C: immutable inventory entry updates. T: inventory snapshot/stack boundaries. R: five actual dispenser taps and live UI/load parity. Original transcript cannot establish where every coffee came from. |
| P2 CB-02/03 | C: restart-capable audio routing; Lost Shift generic start message removed while quest start/tracking/rewards remain. T: audio routing and Lost Shift flow. R: actual player positions/mixer and duplicate notification absence. |
| P2 IC-04/05 | C: completed-quest grouping and Boggs certification-aware fallback; required authorization remains. T: relevant progression gates. R: journal usability with large history and whether routing language is clear to a new player. |
| P2 IC-10 | C: visible generic cinematic hints removed in intended surfaces. D: tactile lever animation/audio was explicitly excluded. R: tap/reveal/advance and keyboard/screen-reader behavior. |
| P3 CB-01/02/03/04/05 | C: idempotent recruitment; service-recess rest/message; Guard Break timing; Consumables labels; cinematic music ownership/single launch music cue. T: event, rest, balance and route checks, plus historical Guard Break preview. R: audio and boss encounter pacing; duplicate SFX finding remains open. |
| P3 IC-01/02/03/06/11/12 | C: consumed-item category/priority, scripted radio entry points/portrait, ordinary tap narration, Jed setup, rounded announcement without hint, stable disabled Android navigation. T: script isolation, consumption assertions, comms/used-item rendering and route continuity. R: phone input, layout, skip/background interactions, emotional continuity. |
| P3 IC-04/10 | C/T: scoped Optics Static and fixed Corrosion changes; seeded attacks and actual status ticks. R: multiple earned builds/seeds, with/without mod, Warden difficulty relative to prior fights. No global accuracy or boss-stat retune was made. |
| P3 IC-05/07/08/09 | C: rail spacing/fades/chevron, swipe/X, labelled Details, power units. T: quest swipe and power-label tests existed in Part 3 run; this assessment reran quest presentation but not every Android UI assertion. R: actual finger drags, clipped rows and ability detail readability. |

## Open findings and limitations

| ID | Finding | Disposition before using as a later-world standard |
|---|---|---|
| W1-STD-01 | `python3 scripts/world1_checkpoints.py` reports source hashes changed. Earned checkpoints are stale after Part 3 content edits. | Do not use stale focused fixtures as latest-build evidence. Regenerate/import through the documented earned-route workflow in a separate authorized implementation step; this audit did not modify fixtures. |
| W1-STD-02 | Charlie's five authored, state-gated trust/play/cuddle choices all return to `charlie_default_talk`; no terminal/Leave choice. User reports being unable to exit. | Confirmed content-level missing escape path; runtime report supplied by user. Charlie is outside the World 1 node roster, but this exposes a reusable dialogue contract failure. **Not fixed by this assessment.** Audit later-world dialogue graphs/state-gated exits before extending the pattern. |
| W1-STD-03 | Part 1 proposed 1 AP; production `questAwards` and crew-adoption documents specify 2. | Documentation/decision drift, not proof of a payout bug. Use production budget and earned receipts; seek design reconciliation before economy changes. |
| W1-STD-04 | `w1_mq05_use_nav_console` emits launch SFX before the cinematic, whose first step also emits it; callback impact SFX repeats the final scene step's impact cue. | C: duplicate dispatch sources. R: whether playback overlaps/restarts/clips. Music fix does not establish this is resolved. Do not use duplicate sound ownership as the gold standard. |
| W1-STD-05 | P1 claims 1.2-second workshop handoff pause; source closes menu then dispatches dialogue with no fixed pause there. | C: implementation differs. R: no-overlap and sufficient pause on device. Prefer state/animation completion over copying a magic delay. |
| W1-STD-06 | `statuses.json` contains `ramp_up` for Erosion, but `StatusTick`/`applyStatusTick` do not model/use it. | C: ignored authoring field. Actual tested ticks are fixed max-HP fractions, not ramping. Do not promise ramping behavior in later descriptions without a consumer/test. |
| W1-STD-07 | Mobile and desktop have different navigation/overlay/layout implementations despite shared state. | C: shared functions do not guarantee visual/input parity. Require per-platform acceptance; latest Pixel install/launch is not a completed post-fix playthrough. |
| W1-STD-08 | Historical reports contain “implemented,” deferred proposals, stale unchecked boxes, and older earned walkthrough snapshots. | Preserve history and use this evidence classification; do not read a checkbox or old route log as current sign-off. |

## Applying this reference to Worlds 2–6

For each world, produce (1) a quest/node/encounter/departure map, (2) reward and AP ownership ledger, (3) NPC dialogue entry/exit audit by state, (4) tutorial/notification ownership table, (5) persisted-state and interruption matrix, (6) normal-route and optional-route evidence, (7) device acceptance and open defects. Use stable IDs such as `W2-NAV-01` and name each invariant from this document.

Score each contract as C/T/R/D, not an aggregate “polished” score. Record intended exceptions (e.g. deliberate party separation, a point of no return, a unique puzzle) and their recovery/communication rules. A later world need not reproduce World 1's pacing, number of quests, merchant placement, boss HP, AP schedule, red strobes, three-stage puzzle, or sacrifice. It must preserve truthful feedback, reachable progression, clear choices, coherent consequences, durable state, and one owner per effect/reward.

Priority: progression/save/reward defects first; inaccessible dialogue exits and input conflicts next; presentation and balance after reproducible evidence. No future inter-world cinematic project is implied by this standard.

## Source map

Paths are relative to repository root; identifiers/functions above are search anchors.

- Content: `app/src/main/assets/{rooms,hub_nodes,hubs,quests,events,dialogue,npcs,items,skills,statuses,tuning_puzzles,cinematics,tutorial_scripts,audio_catalog}.json`.
- Exploration: `gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt`; `.../presentation/{RoomPresentation,RoomObjectPresentation,ExplorationNoticePolicy}.kt`; `.../viewmodel/helpers/{MapStateBuilder,MapServiceResolver}.kt`.
- State: `gameRuntime/src/main/kotlin/com/example/starborn/domain/{event/EventManager,quest/QuestRuntimeManager,dialogue/DialogueService,session/GameSessionStore,session/OpeningNarrativeMigration,inventory/Inventory,leveling/NovaBuildPrototype,leveling/CrewBuildPrototype,prompt/UIPromptManager,audio/AudioRouter}.kt`.
- Combat: `gameRuntime/src/main/kotlin/com/example/starborn/domain/combat/{CombatEngine,CombatActionProcessor,CombatFormulas}.kt`; `.../feature/combat/viewmodel/{CombatController,helpers/CombatTutorialTracker}.kt`; `.../feature/combat/presentation/AbilityPowerLabel.kt`.
- UI: `app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt`; `.../ui/dialogs/SkillsDialog.kt`; `.../feature/combat/ui/{CombatScreen,components/CombatFxCanvas,components/EnemyInspection}.kt`; `.../ui/overlay/QuestBannerOverlay.kt`; `shared/src/androidJvmMain/kotlin/com/example/starborn/{ui/map/MapPresentation,ui/events/QuestPresentationState}.kt`; matching `desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/` components.
- Observations/history: [Part 1](../playtest/reports/starborn_world1_part1_playtest.md), [Part 2](../playtest/reports/starborn_world1_part2_playtest.md), [walkthrough and Part 3 addenda](../playtest/reports/WORLD_1_PLAYTHROUGH_AUDIT.md), [crew production adoption](../testing/CREW_PRODUCTION_ADOPTION_2026-10-19.md), [merchant standard](../desktop/NPC_OVERWORLD_PRESENTATION.md), [map/rest standard](../desktop/MAP_ROOM_MOVEMENT_POLISH.md).
