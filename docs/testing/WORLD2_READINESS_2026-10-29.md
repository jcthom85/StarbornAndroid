# World 2 readiness assessment — October 29, 2026

## Decision for tonight

**Ready for an exploratory playtest on the updated build, with the limitations below. Not regression-certified or balance-approved.** The production main route and all ten quests now pass with earned resources, public navigation/actions, real crafting, actual selected combat, and disk save/load. The assessment found functional defects that the older zero-warning narrative audit missed; the bounded fixes are listed here.

Use an earned World 1 arrival save for the primary run. Stabilize Zeke before spending the recovered Medkit. Keep separate saves before Orion's awakening, the Beast, the Anchor Drill and departure. Finish local quests before confirming departure. Use the [focused checklist](../playtest/WORLD_2_FOCUSED_CHECKLIST.md) for observations and results.

There is **one open required correction for truthful item feedback**: Rapid Capacitor claims faster skill cooldowns while its authored equipment bonuses are Speed/Agility. This does not prevent chapter completion. The shared item's text/mechanics can affect later worlds, so this assessment leaves that decision to the owner. Do not count acquiring it as evidence that cooldown reduction works.

The assessment built a new debug APK but **did not install it on the Pixel**. The earlier Charlie/checkpoint installation predates these World 2 and bunk-fade changes. Update the phone before judging these fixes. No World 3 assessment, quest traversal or content changes were performed; its landing room and initial quest are checked only as World 2's departure boundary.

## Baseline, scope and evidence rules

Base commit: `cb4c2cf3` on `main`, with existing uncommitted Charlie/checkpoint work preserved. Results apply to this additional uncommitted follow-up, not the commit alone. Release display version remains 1.3.100 (184); identify the build with its APK hash. See the [per-class results, content hashes and route records](evidence/WORLD2_READINESS_2026-10-29.json).

Reference: [World 1 functional standards](../standards/WORLD_1_FUNCTIONAL_STANDARDS.md). **C** = code/content inspected; **T** = fresh host/controller test; **R** = phone/runtime acceptance needed; **D** = discrepancy or deferred decision. An authored condition or a passing synthetic-event harness does not certify touch input, narrative pacing, actual audio or balance.

Sources examined: current rooms and all their variants/actions, node membership/transitions, quests/tasks, related events and dialogue graphs, NPC/shop definitions, items/recipes, tuning puzzles, tutorials, cinematics, progression/AP claims, enemy/ability/status definitions, discovery/migration/save systems, and exploration/combat controllers. Historical context: `WORLD2_REFINEMENT_PASS.md`, World 2 story references, completionist walkthrough, playthrough audit, prior navigation/playtest notes and public campaign reports. Historical “completed”/zero-warning claims were not treated as acceptance.

`WorldAssetDataSource` reads `worlds.json`, `hubs.json`, `hub_nodes.json` and `rooms.json`. The older nested `assets/worlds/world_2.json` describes a different sky/flight structure and is not this loader's authoritative room graph.

## Actual content and route

World 2 has **93 distinct rooms, ten nodes, two hubs, 164 room actions, 60 description variants, nine authored containers, and 13 rooms with direct enemy presence**. The Source Beast is an additional action-spawned encounter. All 105 referenced room backgrounds, node icons and hub backgrounds exist in the source asset roots. File existence is C, not rendered-image quality or release asset-delivery acceptance.

| Hub / node | Rooms | Role and functional boundary |
|---|---:|---|
| Jungle Ruins / Crash Site | 7 | Injured Zeke, Chime/supplies, treatment, stream; Charlie and later salvage return. |
| Jungle Ruins / Razor-Vine Path | 11 | Hostile flora, patrol signatures/cache, optional heated-cutter loop. |
| Jungle Ruins / Tideglass Beach | 18 | Coast/caves, David/Penny, Kaelen, Megan, fishing, Prism and drainage-vault detours. Entry is Mist Pools, not the credited Tide Pools room. |
| Jungle Ruins / Canopy Ridge | 8 | Scout route to temple first; later hunter/Beast/drill. Earlier scouting does not start the boss. |
| Jungle Ruins / Temple Gate | 6 | Sentinel-3, two optional crystals, Chime-authenticated entrance. |
| Sector 9 Ruins / Hall of Echoes | 17 | Stasis clues, archive/chorus, optional guardian/conduit route and loot. |
| Sector 9 Ruins / Stasis Chamber | 7 | Four clue preparations, three ring decisions, six-beat awakening, separate Bridge recovery, vent branch. |
| Sector 9 Ruins / Source Gate | 7 | Four feed preparations, two routing decisions, recovery/cooking station, hangar barrier. |
| Sector 9 Ruins / Hangar Bay | 6 | Inspect, salvage shortcut, install/power Bridge, confirm departure; grounded planning inspections. |
| Sector 9 Ruins / The Sky | 6 | Walkable flight-plan projections while Astra remains docked. Not a second launch or crossing of the outer Shield. |

| Quest | Reachable sequence and reward owner | Evidence / limits |
|---|---|---|
| MQ01 A Strange Coast | Zeke conversation → pod console grants Chime, Medkit, ration and fishing kit → Pod Interior treatment consumes Medkit → Stream Bank entry grants **150 XP**, completes and starts/tracks MQ02. | C/T earned route. R early Medkit use, arrival popup order and physical navigation. Zeke is already a party member from World 1. |
| MQ02 The Signal | Accepted vine/borer victory → gate visit → stone lock → **200 XP**, open/warp into Sanctuary, start/track MQ03. | C/T; actual route retreats from Stalker-Vine and wins Glow-Moss Cavern's Echo-Borer. This accepted alternative is preserved. R whether “clear canopy” wording communicates it. |
| MQ03 Sleeping Giant | Hall murals, pod, overview, coolant → pressure/memory/return decisions → full Orion conversation/recruitment → separate cradle grants Bridge and **250 XP**, starts MQ04. | C/T; wrong-answer and early-recovery guards tested. R reveal/awakening pacing, optional quest discovery and lifecycle at final dialogue. |
| MQ04 The Hunter | Ridge → confrontation → repeatable Beast attempt → victory recruits Gh0st → Anchor Drill completion unlocks Link, gives **300 XP**, completes/starts MQ05. | C/T actual Beast combat; defeat/retreat/retry event guards and pending-scene disk recovery tested separately. R actual failure UI and drill playback. |
| MQ05 Liftoff | Horn/cup/gauge/breakers → two route decisions → inspect Astra → Pod Fragment conduits/return → consume Bridge/install/authenticate → departure choice → cinematic completion gives **350 XP** and hands off. | C/T main and completionist routes; cancel/resume/exactly-once callback tests. R normal/skip/background/force-stop presentation and physical cross-hub refresh. |
| SQ01 Botanist | Accept from Zeke → five distinct scans → **return to Zeke** → dialogue pays **100 XP + two painkillers**; completion owns **2 AP** through progression claims. | C/T fresh production payout and no-repeat assertion. Previously auto-completed on scan five without the dialogue payout; fixed for unfinished/fresh quests. |
| SQ02 Lost Patrol | Alpha starts quest → beta/gamma → Burned Glade cache grants damaged cutter, schematic, wire/scrap and Corrosive Rounds → learn/craft cutter → **100 XP**. | C/T real ingredient consumption/craft. Long loop works before cutter; shortcut is optional. Cache need not create an inventory transceiver: task/state records recovery. |
| SQ03 Tideglass Day | Orion offer → re-enter Tide Pools → gather supplied herbs/meat/schematic → learn/craft Source Resin → **150 XP + two painkillers**. | C/T. Earlier Beast resin or cooking a meal does not substitute for the quest's craft. R whether this distinction is clear. |
| SQ04 Ancient Echoes | Orion offer → west/east/north crystals → relief/matrix socket decisions → **150 XP + Focus Conduit**, calibration-record access. | C/T decisions and production completionist route. Revisiting crystals after acceptance remains supported; ordinary Orion conversation is a hint, not success. |
| SQ05 Stolen Tech | Gh0st offer → gantry scan-mask decision → hatch bypass → transmitter case gives core/schematic/wire/scrap → learn/craft Rapid Capacitor → **150 XP**. | C/T reachable NPC/quest route and craft. Premade toolbox capacitor is separate loot, not quest completion. R stealth presentation and modifier truthfulness. |

### Reward and progression ownership

- Main-quest direct XP: **1,250**. Five side quests: **650**. Quest reward metadata describes these awards; events/dialogue deliver them. No second generic reward payout was added.
- World 2 AP additions: **2 on Orion recruitment, 2 on Gh0st recruitment, 4 on Liftoff, optional 2 on Botanist**, using persistent claims. Warden/Beast XP is separate from quest XP; Beast currently grants 500 XP/250 credits, no authored AP.
- On the seeded earned test routes, Nova ends at **3,340 XP / 16 AP** for main-only and **3,990 XP / 18 AP** with all five side quests; both end at level 6 with Nova/Zeke/Orion/Gh0st. These are fixture observations, not guaranteed arrival targets. Optional enemies/gifts, purchases, AP spending, gear, random drops and player routing change totals.
- Unique progression items are protected: Chime/Bridge, damaged cutter, repaired cutter and transmitter core have quest/key/unsellable protections. Their schematics and supplied ingredients support actual crafting. Ordinary herbs/wire/scrap remain spendable and are sold by Sentinel-3; blueprint learning is a separate player action.
- Local extras remain separate from the ten journal quests: Charlie trust/gift (prior fix), Penny bandana/50 XP, Kaelen introduction supplies/favor/patrol acknowledgement, Megan's material favor/scanner, Prism reward, drainage vault, caches, films, fishing and arcade-part discovery. Do not include these in the 650 XP side-quest ledger.

## Findings by requested category

### 1. Critical blockers

| ID | Finding | Status |
|---|---|---|
| W2-NPC-01 | David, Penny and Megan each had choice menus whose every path looped back; no conversation exit. Talking could trap exploration input like Charlie. | **Fixed, C/T.** Unconditional terminal choices added; leaving grants nothing. Penny's gift/revisits tested. R actual phone dismissal and navigation. |

**No unresolved hard progression blocker was reproduced on the two earned routes after this pass.** That is bounded evidence, not proof that every old save or action order is safe.

### 2. Required pre-playtest fixes

| ID | Finding | Status |
|---|---|---|
| W2-QST-01 | Botanist auto-completed when its fifth task was scanned. `quest_active:w2_sq01` then excluded its reward dialogue; production completionist XP was 100 short and the stabilizers were missing. | **Fixed, C/T.** Required `report_to_zeke` task and final-dialogue trigger; actual XP/item changes and no duplicate XP asserted. Pre-existing completed saves are a separate limitation below. |
| W2-NPC-02 | Gh0st had no room NPC presence anywhere in World 2. Authored Stolen Tech existed but had no normal NPC rail entry point. Direct test calls masked this. | **Fixed, C/T.** Visible at Canopy Ridge after Beast victory until departure; earned optional route asserts visibility and accepts/crafts/completes Stolen Tech. |
| W2-NPC-03 | Zeke vanished from Crash Site at stream completion with no nearby replacement; later idle lines also preceded his unaccepted Botanist offer. Stasis placement disappeared at MQ03 completion, making later turn-in routing worse. | **Fixed, C/T.** Stream Bank presence during MQ02, Stasis presence through the remaining chapter, and existing World 2 idle lines moved after the offer. Production route verifies access. Main-quest hints still deliberately precede optional offers while eligible. |
| W2-NAV-01 | Temple Gate and Source Gate narrated sealed barriers but had no directional block; east travel crossed without solving them. Quest action conditions alone did not enforce physical navigation. | **Fixed, C/T.** Local state requirements block east until resolution. Solve events set state; reward-free entry reconciliation restores it for already-solved saves. Actual controller attempts before solving and old-save state reconciliation tested. R lock glyphs, return crossings and phone reload. |
| W2-ECO-01 | Kaelen's favor offered a two-herb exchange using a one-herb dialogue condition; failed ordinary removal did not abort later reward actions. | **Fixed, C/T.** Local event validates both quantities, uses required removals, owns one payout and reports missing herbs. Test checks no partial loss/no reward with one herb, correct consumption/150 XP/200 credits/one sight with two, and no repeat payout. A generic stock-check line now precedes the validated outcome. |
| W2-NPC-05 | Orion's post-gate success line remained eligible for all of Liftoff, permanently preceding unaccepted optional offers after Hunter. | **Fixed, C/T.** It retires after Astra inspection; a new late-state test accepts both quests while Liftoff remains active. Earlier gate guidance retains priority. |
| W2-ECO-02 | Rapid Capacitor says it speeds skill cooldowns; its equipment data provides `speed:5, agility:3`, with no authored cooldown modifier. | **Open, C/D.** Correct the promise or approve a mechanical change before treating that reward as trustworthy cooldown feedback. Shared item/mechanics can affect later worlds, so left unchanged. Chapter remains playable; record this known mismatch tonight. |

### 3. Safe self-contained improvements

| ID | Finding / change | Status |
|---|---|---|
| W2-NPC-04 | Kaelen's follow-up entry was referenced by his favor replies and lacked a `_default_talk` suffix, so normal selection excluded it after the first meeting. Its hazard reminder also traversed first-meeting supplies again. | **Fixed, C/T.** Follow-up entry now uses the established default-talk naming contract; all current local references updated. Reminder reuses the existing advice in a terminal, reward-free reply. Repeated visits yield one initial ration/antidote total. No saved dialogue cursor migration is introduced. |
| W2-NAR-01 | Astra departure's opening step and completion callback both emitted `sfx_w2_astra_launch`, causing a second launch cue at the handoff. | **Fixed, C.** Scene retains cue ownership; callback handles state/rewards only. Callback/route tests pass; perceived sound needs R. |
| W2-QA-01 | Existing narrative audit seeded position and synthesized victories; it could report zero issues without exercising UI access or reward delivery. | **Improved, T.** New bounded production main/completionist tests stop at the departure boundary, assert key NPC presence/physical gates, learn schematics, consume materials, verify Botanist rewards and save parity. Old audit retained as historical evidence. |

### 4. Issues to verify during playtesting

| ID | Check and source evidence | Remaining risk |
|---|---|---|
| W2-ECO-03 | Rescue uses ordinary consumable `medkit`, given once at pod inspection and required for treatment. David sells replacement Medkits; his coast route is physically reachable. | Spending/selling the last kit first can leave rescue action unavailable and send the player on an unexplained detour. No automatic reservation/reissue was invented. Stabilize first for the primary run; test depletion/replacement on a separate save. If recovery is confusing or unaffordable, choose a dedicated quest-kit/reservation/recovery design. |
| W2-QST-02 | A previously completed Botanist save cannot reach the new active-quest turn-in. Existing saves have no reliable receipt distinguishing a paid completion from the old automatic unpaid completion. | No speculative XP/AP/item backfill. Preserve an affected save; decide restitution separately. In-progress five-scan saves should now need the report task and deserve a phone check. |
| W2-QST-03 | Confront event starts Gh0st dialogue and immediately credits confrontation/room state; last dialogue line also credits the task. | Quitting mid-conversation may skip the rest of the confrontation on return. Verify and preserve the save. A deferred completion/replay change should be based on the observed boundary; no shared dialogue refactor made here. |
| W2-QST-04 | Source Gate, stasis, mural and vent puzzles have wrong/leave/retry paths; host tests verify representative errors and some disk resume. | Test every decision, leaving, wrong-then-right, save/load and no premature reward; slider controls reset on cancel by current design. |
| W2-NAV-02 | Ten connected nodes, cross-hub transitions, new physical gates, thermal-cutter shortcuts and sluice drain all use shared state. | Map/glyph/room arrival parity, discovery and returns; do not infer them from reciprocal JSON connections. Closing sluice must not strand a player below; test toggle/save/return behavior. |
| W2-CBT-01 | Nine direct enemy definitions: Echo-Borer, Skimmer, Shard-Hound, Spore-Spitter, Razor-Vine, Stalker-Vine, Ruin-Guardian, Sentinel Orb and Beast. | Fresh route fights Borer/Beast and retreats Vine; it does not win every optional encounter. Evaluate healing vine, shielded guardian, Orb Blind and the other wildlife with earned builds. No global accuracy/status/boss retuning performed. |
| W2-CBT-02 | Beast: 520 HP, stability 240, STR 14; weak to Burn/Shock; claw/roar/stomp. Nova/Zeke/Orion fight; Gh0st joins after victory. | Seed 17 earns victory using three recovery items with no knockout; this is one policy/seed. Test several builds, with/without Corrosive Rounds, retreat/defeat/retry and readability. Compare difficulty with World 1, rather than copying Warden's HP. |
| W2-TUT-01 | Debuff tutorial after stream; Link tutorial after completed drill. Link's description promises party healing/Regen, not damage sharing. Core crew/AP progression is shared. | Teaching timing, disable-tutorial path on phone, ally targets, healing/Regen feedback, skill availability, new-member gear and AP offers. No compulsory Erosion-survival system should be inferred from old prose. |
| W2-UI-01 | Current shared notice owners, consumption category, power units, quest banners, minimap, explicit Rest/Cook services are inherited. | Radio/dialogue/reward/quest/recruitment bursts, enlarged text, NPC rail scrolling, merchant exits, swipe dismissal, no accidental double clicks. Merchant pilot was not blindly extended to every World 2 shop. |
| W2-NAR-02 | Full awakening precedes separate Bridge recovery; hunter precedes Link drill; departure warns and follows a low airlane under the Shield. | Character voice, six-beat pacing, Gh0st's voluntary choice, grounding explanation and preview-room clarity. Launch/crash audio, normal finish/skip and reload with roster/rewards intact. |
| W2-SAVE-01 | Pending drill/departure scene callbacks, old premature hunter flags, real disk round trips and whole-route save parity have host coverage. | Android background/force-stop, incomplete dialogue, scene resume and battle recovery on actual device; Windows/desktop interaction still separate. |

### 5. Later polish

- **W2-ART-01:** Charlie and Penny use Orion's portrait; vines use Skimmer art, Ruin-Guardian uses Bulwark art, Orb uses Buoy art. Assets exist, but visual identity is uneven and may confuse target recognition. Record severe confusion tonight; dedicated art is a separate task.
- **W2-TEXT-01:** “Bridge Echo” in MQ03, Tideglass's “skewers” stage wording, “Gh0st?s” recipe hint, an old mining-drill compatibility description, abstract “Objective completed” fallbacks, and some inspect text that claims physical changes despite being inspection only. Do a bounded copy pass after behavioral observations.
- **W2-TEXT-02:** Poison Hollow's “every breath drains guard rapidly” and other Erosion/guard language imply mechanics the current World 2 events do not establish. Historical persistent-Erosion survival plans are retired; fix misleading copy rather than inventing a survival system.
- **W2-TEXT-03:** Hangar prose places a Bridge on the dais before installation while the separate recovered item is carried; post-install descriptions could reflect state more precisely. Sandy Cove describes resting but exposes Cook, while explicit Rest exists at other camps. Clarity issues, not evidence that every room needs a recovery button.
- **W2-QA-02:** Older completion plans claim 91 rooms, older scripts use retired puzzle answers, and the generated narrative audit contains fallback World 1 Zeke text. Those documents/test transcripts cannot certify current visible NPC routes. Prefer this report/current walkthrough; migrate test tooling deliberately.
- **W2-PACE-01:** Six projection rooms add exploration without combat or a second launch. Their length, acoustic vocabulary density, repeated clue/choice form and coast backtracking need human judgment before creative changes. Preserve World 2's exploration/recovery identity.

## What actually changed

Production files changed in this assessment:

1. `dialogue.json`: David/Penny/Megan exits; Kaelen follow-up/default entry, reward-free hazard reminder and event-routed favor check; World 2 Zeke idle ordering; retirement of Orion's gate-success line after Astra inspection; Botanist report trigger.
2. `rooms.json`: Zeke Stream Bank/Stasis visibility, post-Beast Gh0st presence, eastward Temple/Source gate requirements.
3. `events.json`: gate state on resolution and reward-free legacy reconciliation; guarded Kaelen exchange/missing-herb feedback; removed duplicate departure callback SFX.
4. `quests.json`: Botanist's required return/report task.
5. `ExplorationController.kt`: **separately requested World 1 fix**, described below. No general dialogue/quest/combat architecture refactor.

Tests: `World2RefinementTest` adds local exit/reward/legacy gate tests; `LegalRouteCampaignRunnerTest` adds bounded earned main/completionist World 2 routes and the opening-fade test. Documentation/checklist/evidence added. World 1's ten earned checkpoint files/manifest were regenerated from its production completionist route after the content changes, so freshness validation still passes.

Earlier uncommitted Charlie exit, its test, World 1 checklist work and escape automation change predate this assessment; they were preserved, not represented as new findings/fixes. Unrelated untracked files were untouched. No commit, push or phone install was performed in this assessment.

### Separate requested World 1 fix: bunk-light fade

The fade-shortening hook still listened for the retired `ms_lights_out_complete`; the current light event sets `ms_w1_mq01_fault_visible`. The controller now handles the current milestone and immediately clears the initial fade/black screen, cancels any remaining 2.2-second presentation pause and consumes its callback once. Natural fade completion retains its ordinary timing. Subsequent unrelated fades are not shortened by an already-lit bunk.

The production test taps the actual bunk-light action both during the fade and after visual completion but during its pending pause. Without advancing virtual time, it asserts the light narration, cleared black/fade state and west-door block. A late renderer completion cannot repeat state changes. Both earned opening routes also pass. R: visibly tap during the fade on Pixel; assert no delayed reappearance/second popup and then open the door normally.

## Verification performed and limitations

**Final focused batch: 110 tests, zero failures/errors/skips.** Counts: World2Refinement 16; WorldTwoDiscoverability 10; DataIntegrity 30; DialogueService 4; EventManager 15; OpeningNarrativeMigration 5; QuestRuntimeManager 7; UIPromptManager 12; InventoryAndConsumableBoundary 5; LegalRouteCampaignRunner selection 6 (two openings, bunk fade, World 1 completionist, World 2 main, World 2 completionist). Appropriate focused groups were rerun as dialogue, presence/gates, exchanges and Botanist changed.

The two earned World 2 tests use production crew rules, seed 17, supplied puzzle answers, accelerated presentation and explicit scene skips. Both stop at `spire_sewers_landing` with `w3_mq11` active/tracked, full roster, installed Bridge and no pending event scene. These establish chapter handoff, not World 3 readiness. Whole-route disk slot parity passes; detailed scene-resume tests use event hooks and separately serialized state.

Combat evidence: actual World 2 Borer victory, Stalker-Vine retreat, Beast victory; no assisted movement/actions/victories. Optional room enemies can be left alive during exploration; completing ten quests does not prove all 13 enemy rooms are balanced. The completionist driver takes the long patrol loop before owning the cutter instead of forcing the blocked shortcut.

Built APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `5908e30d6dd13aea7fb45edc9381ce48881dd02a7ef6f39a2278c3119bb4bbe0`. Not installed in this assessment.

Additional checks: ten refreshed World 1 checkpoint hashes pass; Maestro structure passes (132 flows, 301 references), **not device execution**; debug APK assembly and desktop Kotlin compilation pass; scoped whitespace check passes. No fresh Maestro, Android instrumentation, phone acceptance, Windows run or desktop UI suite was performed here.

A broader `ExplorationViewModelTest` attempt had two failures: `openingInventoryTabAfterJedQueuesBagTutorial` and `laterDefersEquipmentTeachingWithoutCompletingOrRepeatingTheOffer`. Both reproduced with the unchanged HEAD controller, before the bunk fix, and concern mocked World 1 tutorial fixtures. They were not silently counted as passes or repaired outside scope. Baseline diagnostic: `/tmp/starborn-baseline-tutorial-tests.log`; passing focused run: `/tmp/starborn-w2-final-regression.log`. Generated reports live under `app/build/reports/`; durable summary/content hashes are linked above.

## Remaining work and planning estimate

These are planning ranges based on the source inventory and tests, **not measured human completion times**:

| Work | Estimate / dependency |
|---|---|
| Start a controlled exploratory session | No further reproduced main-route blocker remains. Update Pixel to this build, preserve earned saves, and record build/settings. Allow 10–15 minutes setup. |
| Focused critical checks | About 60–90 minutes if using well-positioned saves: NPC exits/offers, Botanist payout, gates, awakening/drill/departure recovery. A fresh arrival requires additional travel. |
| Fresh main-route human playtest | Budget roughly 2–3 hours for reading, exploration, puzzles and actual combat; first-time navigation may extend this. |
| Five side quests and meaningful coastal/archive extras | Another roughly 1.5–3 hours, depending on exploration/fishing and already completed steps. All 93 rooms are not a short smoke pass. Split across sessions if needed. |
| Rapid Capacitor wording/mechanic decision | Copy-only correction likely under an hour including scoped checks, **after owner approval for a shared item**. Mechanical cooldown changes require a separate design/test estimate. |
| Existing unpaid Botanist save restitution | Roughly 1–3 hours to inspect affected saves and choose a safe receipt/migration policy; no evidence supports indiscriminate backfill. |
| Post-playtest bug pass | Reserve half a day to a day for triage, bounded corrections and reruns; actual work depends on reproducible findings. Balance changes require several earned builds/seeds before estimating. |
| Art/large narrative/automation overhaul | Not estimated as prerequisite work. Those are separate scopes; the current assessment does not authorize them. |

Recommended order tonight: earned crash arrival → opening/recovery and new NPC exits → Sanctuary/Orion → Botanist actual turn-in → Beast/drill → remaining optional quests → Source Gate/salvage → cancel then confirm departure. Preserve a pre-departure save. Use the checklist to distinguish PASS, FAIL, NOT RUN and BLOCKED; report a misleading promise even if it does not stop progression.
