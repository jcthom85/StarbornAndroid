# Player choices review — November 17, 2026

This is the first pass in the approved sequence: player abilities and progression, equipment/economy, encounters and enemy skills, then puzzle/exploration experience. It reviews current production code rather than adopting historical proposals. It does not declare the game balanced or replace an earned campaign playthrough.

## Decision

Keep the four crew identities, the compact two-path trees, free out-of-combat resets, and the authored encounter framework. Correct behavior and targeting discrepancies before tuning power, AP costs or enemy stats. Then compare choices with equipment and consumables actually available at each campaign checkpoint.

The review includes 43 asset-defined crew skills (including legacy records), five abilities supplied by production trees, all 32 production purchases, level-up unlocks, story grants and the shared combat controller. See [the full ability and purchase inventory](ABILITY_INVENTORY.md), [runtime observations](runtime_snapshot.json), and [acquisition references](acquisition_inventory.json). The accompanying runtime export uses a deliberately supplied diagnostic party; it is not evidence that all those abilities can be acquired together.

## Confirmed repair in this pass

**Cover, Field Patch and Field Treatment could not request an ally through the normal combat controls.** All three declare `single_ally`, but the shared controller had no matching target category. It returned “Self” and no target-selection request. The action processor already supported a selected living ally, so lower-level support tests did not catch the UI/controller gap.

Added the missing target category, “One ally” label and ally-selection request. Android and desktop already consume that request. Direct calls without a selected target retain a self fallback. AI target enumeration understands the new category. No ability power, cooldown or AP cost changed.

The regression first failed with `zeke_cover expected:<ALLY> but was:<NONE>`. It exercises all three abilities through the production controller, checks the ally-selection contract, selects another party member, verifies the effect does not spill to other characters or enemies, and checks cooldown commitment. The fixture supplies party progress and purchases; it is not an earned-route test.

## Findings to resolve before numerical balance

| Priority | Finding | Evidence and next decision |
| --- | --- | --- |
| P1 | Ally-selection gap | Reproduced and repaired above. Recheck both device interfaces when installing the next build. |
| P1 | Generic friendly statuses can affect the entire party despite a “Self” target label | `statusesForSkill` resolves `ally` status definitions against all friendly combatants, independently of explicit action targets. Inspect Intercept, Bulwark Stance, Harden and Nano Repair together. Preserve intentional party skills such as Guardian Covenant and Link; do not fix this with a blanket targeting change. |
| P1 | Source-art targeting and effect classification are inconsistent | Scan has support/debuff tags plus Discord; Construct has support/damage tags plus Guard. The controller and processor interpret those mixed declarations differently. Specify each relic's actual combat action separately from its exploration power before changing its numbers. Include Tune World and NG+ behavior. |
| P1 | Nova passive descriptions differ from their consumers | Corporate Insight/Legacy promise accuracy and critical chance. Current consumers add direct damage through `TalentEffects.hpMultiplier` and critical chance through `critChance`; `accuracyRating` has no corresponding passive bonus. Decide and implement one consistent contract. Preserve the shared 25% talent-damage cap in comparisons. |
| P2 | AP budget documentation/test excludes an optional elite reward | Boardroom Bailiff currently awards 4 AP. The old test asserts every AP-awarding enemy is a boss. The required-route arithmetic still passes, and the five side-quest awards still total 10. Account for the optional elite separately; do not erase its reward or relax the test merely to make it green. Review whether 4 AP is intentional and prove its encounter cannot repay repeatedly. |
| P2 | Ability catalog contains legacy-only or ungranted records | Keep acquisition evidence separate from catalog presence. Field Patch is an exception: code grants it for Orion even though there is no event grant. Signal Jammer can survive legacy migration. Do not remove retained abilities from old saves as an audit cleanup. |
| P2 | Existing public combat fixtures can exercise legacy progression | The existing `world4Session` helper does not explicitly opt into crew version 1. New review fixtures explicitly set the production version. Historical passing tests must not be presented as coverage of every current crew purchase. |

The runtime snapshot records displayed target scope and isolated HP/status consequences to make these discrepancies reviewable. Static source findings and controlled probes are not blind-playtest conclusions.

## Character assessment

### Nova

**Keep:** the early sequence of Arc Tether, Cryo Vent and Hydraulic Kick gives her a clear disruption/setup role. Saboteur and Improviser are understandable directions. Source relics give story progression a gameplay identity.

**Review:** her growing collection of damage, control, party healing and protection risks making her the default answer to everything. Compare paid Breaker Bolt with free Hydraulic Kick and Arc Tether: the extra stability pressure must create an attainable interruption or Break breakpoint. Compare Scatter Charge with later Plasma Burst and Blast Wave; shorter cooldown and Physical damage can preserve a role, but availability alone does not prove one.

**Tree assessment:** Grounded Wire has a concrete purpose if its extra pressure changes a Break timing. Controlled Vent needs comparison with other setup actions. Breaker Bolt needs a breakpoint test. Exposed Seam rewards team setup but shares a 25% damage cap with other talents/passives. Quiet Steps is a small passive and needs a visible tempo benefit. Scatter Charge provides an early multi-target option. Hot Start is clear but adds another first-hit bonus. Keep Moving is likely encounter-dependent: kills against escorts may matter more than a solo boss. Test its defensive value after the kill rather than counting every proc as useful.

**Priority comparison:** the same earned Nova kit with no AP, 6 AP Saboteur, and 6 AP Improviser against a guarded single target and a small squad. Include elemental weapon conversion: a nominally Physical skill can inherit a weapon element in the current processor.

### Zeke

**Keep:** Shatter Blow combines damage with reducing enemy offense, while his tree offers protection or stability pressure. This is a good basis for an active frontline character.

**Review:** Cover must compete with attacking the threatening enemy, basic Guard, items, Orion's recovery and existing party protection. Last Line bypasses cooldown once for a severely injured ally but neither heals nor grants a free action; its emergency value needs an actual survival test. Clarify Bulwark Stance's intended recipients before evaluating Protector.

**Tree assessment:** Cover now has selectable ally access. Reinforced Harness is modest permanent survivability. Steady Cover's stability recovery matters only where allies face a real Break threat. Last Line needs a scenario where spending the action saves someone. Training Session is a small general offensive step. Fault Line is a measurable Shatter Blow breakpoint improvement. Opening encourages another character to follow up. Controlled Demolition reinforces the Discord plan but can be redundant when the enemy would already break.

**Priority comparison:** protect a specifically threatened ally, interrupt the attacker, and use a recovery item from the same state. Measure prevented knockouts and action cost, not just nominal Shield duration.

### Orion

**Keep:** Acid pressure plus recovery establishes a useful support/controller identity immediately on recruitment. Healing based on maximum HP gives a different scaling model from Focus-based Field Patch.

**Review:** Field Treatment, Field Patch, Nano Repair and Nova's Link need clearly different reasons to be selected. At fixed test stats, Field Patch is `round(18 × (1 + Focus/100))`, while Treatment is 20% maximum HP (25% with Triage below 35% HP). Treatment can scale well on high-HP allies, but its cooldown and purchase chain still matter. Compare the whole encounter, not heal numbers in isolation. Resolve Nano Repair's actual recipients first.

**Tree assessment:** Field Resilience adds a small survivability step. Field Treatment adds a distinct scalable heal. Triage improves emergencies but costs another 3 AP for five percentage points. Aftercare adds two 5%-maximum-HP ticks and competes with Regen for one recovery-over-time slot. Clear Focus is broad but small. Prism Lens needs a visible Break breakpoint. Safe Exposure duplicates a widely available debuff, with a deliberately short duration and no refresh of existing Discord. Steady Field extends new Discord applications, so its value depends on target action timing and other crew applications.

**Priority comparison:** short burst damage, sustained attrition and recovery after a party attack; measure useful healing, overheal, resources spent, damage prevented and turns diverted from offense.

### Gh0st

**Keep:** opening shots, finishing pressure and selective disruption form a strong precision identity. His recruitment timing makes World 3 the first meaningful place to teach his kit.

**Review:** Headshot scales from Strength, while Venom Edge and Disarm use Agility. Equipment and tree guidance must make that split understandable. Compare paid Disarm with Shatter Blow and Phase Counter, including cooldown, scaling, damage and the team's existing Discord coverage. Intercept and Harden need their true defensive scope settled before we call either redundant.

**Tree assessment:** Scope favors healthy targets; Steady Aim applies only to Headshot; Execute rewards finishing with single-target skills. Cool Head rewards a basic-attack/Headshot sequence and shares the talent damage cap. Quick Position is a small tempo step. Surgical Strike improves basic-attack stability. Disarm adds damage plus Discord but costs 6 AP along its path. Recenter is a situational safety talent: it stops one Stun/Stagger skip, not Freeze, and needs relevant late enemies to earn its slot.

**Priority comparison:** opening burst, sustained single-target pressure, finishing a damaged enemy, and interrupting a preparation. Use actual Strength/Agility equipment options rather than identical invented stats for balance conclusions.

## Unlock timing and budgets

- Each character has level-up skills at levels **6, 9 and 12**. World placement must come from earned route checkpoints, not an assumed level-per-world table.
- Each production tree has two paths costing **1 + 2 + 3 + 4 AP**. Each final purchase requires completion of World 4. Total crew cost is 80 AP.
- The existing required-route arithmetic is **8 / 16 / 26 / 36 / 44 / 50 AP** across Worlds 1–6 before finale battle rewards. It is an award/purchase contract, not new continuous-route proof.
- Five optional quest awards add 10 AP. The optional Boardroom Bailiff adds another 4 AP in the current enemy catalog; this is missing from the older shorthand budget.
- Do not add more tree nodes yet. First make current purchases useful, understandable and distinct from story/level rewards.
- Do not automatically pull all capstones earlier. Compare meaningful use opportunities in Worlds 5–6 and whether earlier nodes leave a satisfying choice before World 4's completion gate.

## Equipment and snack dependencies for the next pass

Physical base-gear ownership is already implemented: `GameSessionStore.setBaseEquipment` checks inventory counts through `BaseEquipmentRules.canEquip`, and fresh games opt into physical inventory. The old November 11 finding about shared armor is not evidence that the defect remains. Recheck transfer, selling/crafting reservations and old-save migration with focused tests before further changes.

Weapons influence both basic attacks and the generic skill-damage formula. Skills can inherit non-Physical weapon elements. Mods and weapon choices therefore belong in ability comparisons. Snacks provide passive bonuses and reusable actions; receiving one is not proof its activation is worth a turn. Include AP-free builds, realistic crafted builds and ordinary exploration rewards. Never use assisted playtest saves as balance evidence without recording their provenance.

## Next execution order

1. Finish the explicit effect/target contracts for self protection, party protection, Scan, Construct and Nova's conditional passives. Reproduce mismatches with focused regression tests. The ally-selection repair is the first completed item.
2. Reconcile AP income, grant routes and level-up timing with current rewards, documenting legacy-only skills separately.
3. Compare representative earned early/middle/late states. Start with Warden/Canopy/Beast, then Administrator/Titan, then Avatar/finale. Use current authored formations, multiple seeds and at least two plausible action policies; older prepared encounter arrays may omit current components or escorts.
4. Tune only demonstrated weak or dominant choices, together with equipment/snack opportunities. Record the player decision each change is supposed to improve.
5. Carry that into the full equipment/economy pass, then enemy/encounter tuning, then puzzle/exploration experience. Fix blockers immediately wherever discovered.

Acceptance: a normal route remains viable without grinding; each crew member has useful decisions; upgrades and purchases alter choices or outcomes; no mandatory dominant build; descriptions and target controls tell the truth; migration and save/reload preserve ownership and purchases. Automated victories do not establish enjoyment.


## Controlled probe results

The runtime snapshot records the baseline before the Scan correction.

These observations use real runtime code with all talents supplied, equal artificial stats (1,000 maximum HP, 500 current HP, 20 Focus) and one test enemy. They demonstrate effect/target behavior, not campaign strength or earned availability.

- **Scan:** the baseline displayed Self and applied Discord to Nova. The correction declares a single enemy target and describes the combat effect.
- **Construct:** displayed All allies; restores 156 HP to each crew member and applies party Guard. There is no offensive choice in this observed action.
- **Intercept/Bulwark Stance/Harden:** the displayed scope is Self, but friendly statuses spread across the living party.
- **Nano Repair:** displayed Self; applies party Regen.
- **Guardian Covenant:** correctly grants party Shield/Braced, but its displayed scope is Self. Preserve the party effect and correct the declaration/presentation.
- **Tune World:** displayed All enemies; the probe applies enemy Stun but records no HP damage or healing. Review this endgame/NG+ action separately from ordinary campaign unlocks.
- Seven asset skills have no fresh grant in the inspected acquisition routes: Signal Jammer, Pulse Grenade, Scrap Shot, Cheap Shot, Synergy Pitch, Target Lock and Slide Kick. Signal Jammer has explicit legacy-retention handling. Their presence in old saves is not automatically a defect.

## Verification and stopping checkpoint

Portrait work was committed/pushed first as `8965f879`. This follow-up changes only shared single-ally targeting, corresponding AI target enumeration, focused tests and audit records. No production skill power, rewards, AP prices, equipment or enemy definitions changed. No installation or version increment.

- Initial focused run: **29 checks, 28 passed**. The failure is the boss-only AP assertion described above; required-route AP arithmetic passed.
- Support/migration/targeting run passed before the repair; it did not cover the missing production ally-selection request.
- Reproduction: the new production ally-selection test failed before repair, expecting ALLY and receiving NONE.
- Final focused run: **38 checks, 36 passed**. All three selected controller checks, all 13 crew-talent checks and all nine Nova-talent checks passed. Eleven of 13 enemy-support checks passed.
- Two enemy-support expectations remain unresolved against current authored boss data: Avatar expects the old `missile_barrage → avatar_recalibration` recovery map, and the finale expects a `gathering_silence` opening rather than the currently selected `source_great_silence`. The first directly checks unchanged enemy data; neither test was weakened to make the run green. Review them with the authored encounter contracts before changing behavior.
- No full suite, fresh campaign run, phone UI check or desktop UI playtest was performed in this pass. Android/shared production code compiled as part of the host run. Both interfaces already consume the shared ally-selection request.

The Scan follow-up passed three focused controller tests. With two enemies present, Scan applied Discord only to the selected enemy and recorded its Shock weakness. The tests also verified that single-ally selection and existing party support still work.

## Support ability follow-up

- Bulwark Stance, Intercept and Harden now apply their defensive effects only to the caster, as their descriptions indicate. This removes accidental party-wide Guard and makes the opportunity cost of those actions worth testing in the next balance pass.
- Nano Repair grants Regen to all living allies. Guardian Covenant grants Shield and Braced to all living allies. Their target labels now match their existing effects.
- Construct's description and target label now reflect its existing combat behavior. It restores HP and grants Guard to all living allies.

Corporate Insight and Legacy now describe the direct damage and basic attack critical chance bonuses they already grant at their respective HP thresholds.

Three focused controller tests passed. They checked all seven self and party support abilities, Scan, and purchased single-ally support. The baseline findings and runtime exports above capture the behavior before these corrections. Next, reconcile the AP budget and compare protection, healing and setup with earned builds.

## AP and earned combat follow-up

The required route budgets remain **8 / 16 / 26 / 36 / 44 / 50 AP** at the six checkpoints before finale rewards. The five optional quest awards add 10 AP and the Boardroom Bailiff adds 4 AP, for up to **64 AP before the finale**. Ascended Vale and Ascended God pay another 5 and 8 AP respectively, for **77 AP after both victories** against 80 AP in the trees. The reward contract now checks the exact six combat AP sources. The earned Ring completionist route verified the Bailiff's 4 AP, immediate save and reload, and continued absence on revisiting Boardroom Grave after a rest.

The World 2 comparison used existing campaign-earned main and completionist arrivals, seeds 17, 41 and 73, and the current Beast encounter. Both arrivals produced identical metrics for these policies. The main arrival was level 5 with 10 AP available, and the completionist arrival was level 6 with 12. Each policy used emergency restoration below 45% HP when available.

| Beast policy | Wins across both routes | Mean crew actions | Mean incoming HP damage | Mean consumables used |
| --- | ---: | ---: | ---: | ---: |
| Basic attacks | 6/6 | 45.3 | 215.7 | 2.0 |
| Guard when marked, otherwise basic attacks | 6/6 | 54.0 | 133.3 | 0.3 |
| Field Patch and timed Shatter Blow, otherwise basic attacks | 6/6 | 38.7 | 147.0 | 0.0 |

Guard reduced incoming damage and took more actions. The combined crew support policy cut actions and eliminated consumable use in all six runs. It used Field Patch 3–4 times per run and Shatter Blow 2–4 times. Shatter Blow also deals damage, so the combined result cannot isolate Discord or Field Patch. All policies won. Keep both abilities and test their separate contributions against later enemies before adjusting power.

The Warden's reactive tether strategy won in all three seeds with no recorded incoming damage and averaged 10.3 crew actions versus 19.0 for direct attacks. Both Canopy target orders won in all three seeds. These are tests of deterministic action policies. They help identify useful decisions and leave open how clearly the game teaches those decisions.

The earned Foundry completionist route and both Source finale variants also passed with no assisted actions.

## Titan and Silence comparisons

Replayed each boss from an earned checkpoint with three seeds (17, 41, 73) and four policies. Each replay restored the same combat inputs, including HP, items, gear and unlocks. The Silence checkpoint carries forward the actual state after Vale. The baseline already uses skills and emergency healing. Each variant adds one priority to that strategy: Guard when marked, Orion's Field Patch below 80% HP, or an available interrupt tool aimed at the source of an enemy preparation. All 24 replays won with zero consumables spent and zero knockouts.

| Added priority | Titan mean actions / incoming damage | Silence mean actions / incoming damage |
| --- | ---: | ---: |
| Baseline | 68.7 / 38.3 | 44.7 / 48.3 |
| Guard when marked | 78.0 / 28.7 | 48.3 / 23.7 |
| Earlier Field Patch | 72.7 / 38.3 | 44.3 / 49.3 |
| Interrupt the preparation | 69.3 / 4.7 | 45.3 / 7.3 |

Prioritizing interruption gave the biggest damage reduction with little change in action count. Keep that choice and check whether players can identify the source and the moves available to stop it. Guard reduced average damage but sometimes prolonged exposure enough to take more damage than baseline on the same seed. It belongs as a fallback when interruption is unavailable. Earlier Field Patch recovered HP in several runs but did not prevent a knockout or save an item in this sample. The tested route already handled both bosses comfortably, so these runs give no reason to buff Field Patch or raise skill costs. They also leave open how these choices perform for players with weaker gear or different unlocks.

The focused route test passed and writes the per-seed outcomes and action counts to `app/build/reports/player-choices/earned-boss-choices.txt`. The next player choices review can compare unlock timing and equipment across the six worlds using these results as a reference.

## Unlock timing and equipment on earned routes

Two fresh campaigns using production crew rules reached the credits without combat assistance. The main route recorded 26 encounter entries and the side-content route recorded 38. The scripts followed public quest and combat actions and only used gear, AP and supplies available on those routes. They model two specific play styles. For example, both scripts left Nova's Mining Pistol equipped even after earning the Laser Blaster. Owned gear and equipped gear are recorded separately.

| Before encounter | Main route level / unspent AP | Side-content route level / unspent AP | Options already available |
| --- | ---: | ---: | --- |
| W1 Iron Warden | 4 / 6 | 4 / 6 | Arc Tether, Cryo Vent and Hydraulic Kick. Both can afford the 6 AP path to Breaker Bolt. |
| W2 Source Beast | 5 / 10 | 6 / 12 | Orion has Field Patch and Zeke has Shatter Blow. Both own the Laser Blaster. The side route also equipped Brawler Rig and owns Ion Poppers and Stability Gyro. |
| W3 Administrator | 7 / 22 | 8 / 26 | Both have four crew members, Link and Scan. Side content added Intercept, Splitter Edge, Rapid Capacitor and several equipped accessories or mods. |
| W4 Titan | 8 / 33 | 10 / 39 | Both have Arc Tether, Hydraulic Kick and Construct. System Crash is present only on the side route. Both own a Heat Liner. The side route also has Driftweave Cloak and Crucible Forge-Plate. |
| W5 Avatar | 10 / 18 | 12 / 26 | Both have System Crash and Stasis, and entered with 22 AP already spent. Both own Quake Knuckles and have Heat Liner on Orion. |
| W6 Silence | 12 / 23 | 12 / 33 | Both have purchased Breaker Bolt and Exposed Seam, with 32 AP spent overall. Both own Sunder Seeds. The side route also has Starborn Mod equipped. |

The main route has a usable interruption option from World 1. System Crash arrived at the Orbital Fighter on the main route and at Magma Drone on the side route. Keep its level 9 unlock so the extra combat can earn an earlier tactical option. Prioritize making Titan's interrupt source and Arc Tether's role clear. Breaker Bolt costs 6 AP along its path with no world milestone prerequisite. The test script chose to buy it in World 6, although both routes could afford it by the Warden. Capstones were closed at Titan and open at Avatar, with World 5 and World 6 left to use them. These runs establish that the timing gives room to use them. They only purchased Exposed Seam among the capstones, so the other capstones still need individual value comparisons.

The older equipment audit's crafting gap is partly addressed. Tinkering now offers two armor recipes in addition to mods. The side route crafted and equipped Brawler Rig and Driftweave Cloak. Ingredient checks at the sampled pre-boss inventories found Armor Plating Mk I available across both routes and Thermite Core on the side route from Titan onward. These checks measure materials on hand after earlier spending and do not count potential shop purchases or additional detours. The main route mostly used starter weapons and only equipped Heat Liner among the major gear rewards. The clearest equipment opportunity is helping players identify useful upgrades they already own, especially the Laser Blaster and later armor, before adding another tier.

The main route reached the finale with Sunder Seeds as its first recorded owned snack. The side route carried Ion Poppers from the Beast checkpoint. Starbar Crunch is already an authored room pickup, but neither route collected it. Treat snack discovery as the first issue. The early snack comparison found that opening with Ion Poppers' Accuracy action could erase the time saved by the equipped upgrade. For further comparisons, activate snacks in response to an actual threat or unmet hit requirement and count the turn spent. Do not assume every owned snack needs to be an attractive opener.

The route trace writes `app/build/reports/player-choices/earned-progression.json`. The physical-copy and legacy migration check passed, along with crafting and snack contract tests.

## Earned gear comparison

Replayed the Beast, Titan, Avatar and Silence from the main route with seeds 17, 41 and 73. The paired runs used the same skills, combat decisions, inventory and saved HP. The upgrade run equipped only items the crew already owned, through the normal equipment controls. This means Laser Blaster for the Beast, Laser Blaster and Orion's Heat Liner for Titan, and Laser Blaster and Quake Knuckles for Avatar and Silence. The Avatar and Silence baseline already wore Heat Liner. Armor can change maximum HP and move the same saved HP across a healing threshold.

| Fight | Mean actions, baseline / upgraded | Mean incoming damage, baseline / upgraded | Items used across 3 runs, baseline / upgraded |
| --- | ---: | ---: | ---: |
| Beast | 24.0 / 22.0 | 0.0 / 0.0 | 0 / 0 |
| Titan | 68.7 / 73.3 | 38.3 / 51.7 | 0 / 3 |
| Avatar | 32.0 / 32.3 | 48.0 / 25.0 | 0 / 1 |
| Silence | 44.7 / 44.3 | 48.3 / 48.3 | 0 / 0 |

All 24 replays won with zero knockouts. Laser Blaster earned its keep against the Beast and the Avatar upgrade pair reduced damage, but the later results varied. Titan changed two pieces together, so the extra actions and item use do not establish that either piece is worse by itself. These are results for one strategy and one route. Keep the current gear values. In the encounter pass, prioritize clearer interrupt cues and pressure that creates a reason to adjust gear or tactics. Per-seed results are in `app/build/reports/player-choices/earned-gear.txt`.

Starbar Crunch moved from Pod Row's floor to the shift ration tin at Ration Sink. The opening route tests verified the new pickup and equip flow on both loader branches, including ownership and the opened tin after save and reload.

## Phase 2 kickoff: combat challenges

Titan and Silence already marked the threatened crew portraits, highlighted the charging component and let players select it from the persistent warning. The visible warning only named the attack and who it threatened. It now names the source and presents targeting it as an action when one attack is preparing. Multiple warnings retain the compact layout. Android gives the warning priority over transient combat messages in the same fixed space, and desktop reserves enough space for both lines.

The tactical distinction is between stopping this attack and removing future attacks. A Break on the charging component cancels the current preparation. Destroying it also denies future attacks from that component. Titan has four such systems, while Silence's Voice and Memory Anchors only power their specific commands. Great Silence comes from the body. The World 4 and World 6 walkthroughs now explain how to select the source and when Guard is the fallback.

The 24 earned boss replays above establish the payoff for prioritizing interruption. They do not measure how often players notice the new cue or prove that eliminating every component beats focusing the body. The broader encounter pass should check how often other worlds teach and reuse these choices, and compare where optional fights give a meaningful reward.

Verification passed for both boss runtime checks and all three focused Android emulator tests. These covered Titan's source selection at normal and large text sizes, Silence's anchor selection and warning removal, and two incoming warnings in the existing message space. Desktop compiled.

### Six-world encounter review

I compared authored room formations with the fights the two fresh routes actually entered. The counts below cover non-debug, room-listed formations grouped by their world background. Scripted spawns and moving patrols can add encounters outside that count. Route counts are observed entries and can include those spawns or patrols. The side-content route is separate from the Ring combat sweep, which visits more of World 5.

| World | Room formations / unique compositions | Main / side-route entries | Tactics and optional payoff |
| --- | ---: | ---: | --- |
| 1 · The Pit | 10 / 10 | 6 / 7 | The Loader gives Nova an early elemental target. The Bulwark certifies Hydraulic Kick's Guard Break, and the Warden gives Arc Tether a timing payoff. Preserve the transition between those lessons. The Warden's Laser Blaster is the first major gear payoff. |
| 2 · Sector 9 | 10 / 9 | 3 / 4 | Canopy teaches choosing between support and damage threats. Both opening targets won in three seeds. The pair pays Stability Gyro, and Hound Nest pays Ion Poppers and Beast Meat with a guarded supply pack behind it. The Beast lets Field Patch and Shatter Blow matter without requiring the optional fights. |
| 3 · Spire | 8 / 8 | 3 / 9 | Single and paired Crawlers build into rooftop pressure. Optional Relay squads give an earlier chance to interrupt support before the Administrator. Service Dumbwaiter has a distinctive Gh0st weapon plus a Circuit Board, and Fuel Bridge supplies Wiring. Leave the exit between rooftop fights available. |
| 4 · Foundry | 24 / 22 | 2 / 6 | Slag River teaches a marked attack followed by recovery. Welder repair and coolant squads let players practice interrupting support before Titan. Coolant Spillway's iron has a specific arcade repair use, and the Crucible pays Forge-Plate. This world has the biggest gap between authored fights and the two routes' participation. Check what motivates a player to enter a third Foundry fight on a blind run before adding harder gates. |
| 5 · Ring | 25 / 24 | 5 / 5 | The dock teaches alternating preparations, Gamma tests elemental response, and the Avatar applies the linked-source lesson. Surveillance Pit and Firewall Alpha repeat the same Screener/Turret composition. The optional sweep cleared both without spending items or taking a knockout in the tested seed. Alpha's fight can stay optional alongside the required node work. The Bailiff remains a stronger optional incentive at 4 AP and three Power Cells. |
| 6 · Source | 15 / 15 | 7 / 7 | Rescues teach distinct responses, then the arena combines support and disruption before the linked finale. The counts stay equal because the optional quest route can bypass the branch fights. Some nightmare enemies have no item drops, and the Memory Leak and Nightmare Guard also appear separately before the required pair. Gauge whether the rematches feel like foreshadowing or repetition in a blind run. The required pair pays Sunder Seeds for the finale. |

The review found limited identical formation repetition. The more likely issue is repetition of the same response across different groups, particularly in the Foundry and Ring. Keep the authorship and current difficulty values for now. The recommended playtest targets are the Foundry's first optional repair fight, whether Ring players choose to clear both Screener/Turret fights, and whether Source players understand the rematches. Clearing an automated route establishes viability for that build and strategy. It cannot tell us how often players notice the optional fights or what they find repetitive.

The reward clues now appear in existing room interactions. The Hound Nest pack shows wiring and weave through a tear, the Service Dumbwaiter tray reveals the guard's blade and drone's circuit board, the Coolant Spillway steam reveals the iron, and Boardroom Grave's chairs point to the charging cells. The Spire and Ring walkthroughs now identify the matching gear or AP payouts and give an interrupt cue for Firewall Alpha. No reward amounts or combat gates changed.

## Phase 3: puzzles and exploration

The first pass checked the six walkthroughs against the current puzzle definitions and looked for places where player-facing clues, controls, and outcomes disagreed.

| World | What works | What to examine in a blind playtest |
| --- | --- | --- |
| 1 | The minecart has visible route consequences. The Fork teaches one control at a time through a waveform that works without audio. | Whether players can explain a blocked cart route from its feedback. |
| 2 | The prism shows the beam, fringe, and receiver, so the player can predict what each adjustment will do. | Whether nearby routes and rewards make the grotto a satisfying optional discovery. |
| 3 | The Lens and the optional Chrono Coils have separate objectives and rewards. | Whether the heist's setup and containment controls feel like distinct problems. The coil readouts now give direction on both platforms. |
| 4 | The physical Anvil route uses machinery in the world. Overclocked now provides a separate, optional simulation about matching safeguards to machine conditions. | Whether players understand why each safeguard works. The main machinery route still deserves a check for too many sequential taps. |
| 5 | The bulkhead simulation explains the pressure dependencies in the dock and gives a safe retry. | Whether players can derive the sequence from the dock itself. The Graviton Well now gives directional readings, but remains a three-control tuning puzzle. |
| 6 | Memory Stair's refusals are character choices, and the Choir Lock gives the finale an interactive resolution. | Whether the choices feel emotionally distinct and the lock's feedback sustains the scene instead of interrupting it. |

The Forge's test chamber previously completed its calibration on selection. It now pairs three recorded machine conditions with three safeguards. The local log and panel state explain the relationship. Incorrect simulations flag the affected beats, retain the choices, and can be rerun immediately. Successful calibration uses the existing quest task and reward path. Completed calibrations remain inspectable.

The generic numeric panels now display Too low, Too high, or In range on each control. Desktop previously displayed the exact targets, while Android only confirmed a correct value. Their feedback now follows the same rule. The desktop calibration panel also removes a decorative waveform that did not reflect the actual targets. The specialized World 1 waveform and World 2 optics controls remain the reference for a future pass that gives more of the later devices their own physical feedback.

### Puzzle variety follow-up

The required Foundry override now uses the timing tower and underpass clues to make two physical decisions. The crusher animation reflects the chosen beat, and the carriage preview moves toward the selected stop. A rejected setting explains the obstruction. The successful override clears the lane to the Bellows Walk pistons through the existing quest milestone, including for saves that already parked the belt.

At the finale, Nova enters the open beat in a score shared by Orion, Gh0st and Zeke. The preview shows whose voice she would cover and when she can enter cleanly. The release into the Final Note still uses the existing victory, quest and cinematic checks.

The Graviton Well now shows the path of a test mass. The counter-field moves its path sideways, and the magnetic brake changes its stopping distance. The projection explains misses and the controls stay available for an immediate retry. Chrono Coils and Magma Conduit retain numeric controls. Both are optional and separated by puzzles with distinct mechanics.

To reproduce the runtime snapshot, run the `OpeningCombatRuntimeTest` method named `export production player choices for the current design review`; it writes `app/build/reports/player-choices/current-runtime.json`. The saved snapshot and source hashes describe this checkpoint. Regenerate after any further ability or targeting changes.
