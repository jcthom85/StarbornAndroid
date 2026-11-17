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

These observations use real runtime code with all talents supplied, equal artificial stats (1,000 maximum HP, 500 current HP, 20 Focus) and one test enemy. They demonstrate effect/target behavior, not campaign strength or earned availability.

- **Scan:** displayed Self; applies Discord to Nova herself. This is the first remaining targeting defect to repair.
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

Resume with **Scan's self-targeting and the explicit self/party support contracts**, preserving intended party abilities. Then resolve passive wording/behavior and AP-budget evidence before numerical balance experiments. This is a completed baseline review and one verified repair; the remaining balance/design pass is open.

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
