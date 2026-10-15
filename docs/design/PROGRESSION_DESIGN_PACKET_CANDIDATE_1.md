# Progression design packet - candidate 1

October 14 design, October 15 implementation update. **All four crew trees now have an opt-in executable prototype.** See the [full-crew record](../testing/CREW_PROTOTYPE_IMPLEMENTATION.md) for scope and verification limits. Production migration remains proposed. Numerical effects are starting candidates, not established balance. The final static-review paragraph records the original documentation-only pass; its source hashes are historical.

Refined after owner review: Gh0st's recovery is self-developed, Orion's healing uses grounded field-care language, and the protection/healing/Headshot contracts below replace the initial stacking and cooldown proposals. Node counts, AP costs and the campaign budget are unchanged.

## Design recommendation

Keep useful starter, level and story skills. AP buys specialization around them. Replace the sprawling overlapping trees with two paths per character, four purchases per path, costing **1, 2, 3 and 4 AP** in order. One complete path costs 10 AP; both cost 20; all four characters cost 80.

There are no global row-investment thresholds and no cross-character prerequisites. Each purchase requires the preceding node in its own path. Fourth nodes also require completion of World 4 (`ms_w4_mq20_complete`); the lower three remain available earlier. Both paths can eventually be purchased, so a choice is an allocation tradeoff rather than a permanent class lock. Free development respec supports experimentation.

Core abilities keep their actual acquisition sources. The audit traces 35 of the 43 crew skill records to starter, level, or explicit event/dialogue grants; eight need further runtime/cinematic tracing. Of the 15 matching tree/skill IDs, 14 already have a non-AP grant: Headshot is a starter, eleven are level skills, and Cryo Vent/Hydraulic Kick are quest rewards. Remove those duplicate AP unlocks while preserving learned skills. Signal Jammer needs an explicit earned-gadget acquisition contract.

Basic recovery must not require the healer specialization. Propose granting Orion **Field Patch at recruitment**, retaining the existing `orion_stasis_stitch` skill ID. It heals one living ally for `round(18 * (1 + effective Focus * .01))`, cooldown 3, plus Shield for one target turn. This is deliberately changed acquisition, display name and tuning, not a claim about current play. The stronger Field Medic path trades AP for emergency and sustained recovery.

## Effect rules shared by all paths

Every node below needs an executable consumer and a test that buys it publicly and demonstrates its consequence. The [machine-readable candidate](PROGRESSION_BUILD_CANDIDATE_1.json) contains exact contracts, all 100 old-node dispositions, all 43 skill acquisition entries, and the budget allocations.

All new actives spend a normal ATB action, start ready each battle, and have no additional consumable cost. Cooldowns count resolved personal turns, including skipped turns, with the existing cast-turn offset tested explicitly. Status durations count recipient turn-end ticks; refreshing a status never creates an additional simultaneous copy. New active damage uses the existing base-power/scaling/weapon/Focus formula. A direct-damage bonus multiplies resolved pre-defense HP damage once, before affinity/mitigation; it does not amplify damage-over-time or stability damage. Stability bonuses act only on the independently resolved stability amount.

Within an action, add talent HP-damage bonuses and cap their combined contribution at +25%; independently cap talent stability bonuses at +50%. These caps cover all crew talent contributions, including Opening. Existing critical/affinity/meal/status rules are separate and must be included in balance tests. Chance bonuses are percentage points and obey the runtime cap. A target's HP threshold is evaluated immediately before the relevant hit. Once-per-action and once-per-battle limits persist through battle checkpoint recovery; canceled/invalid actions consume neither an action nor a trigger.

Damage elements must not implicitly apply a same-named status. Cryo Vent applies Brittle, not Frozen; later changes to actual freeze/control skills require their own contracts. No proposed talent extends hard-control duration or bypasses boss immunity. The boss action-skipping rate is nevertheless measured because base skills and weakness rewards can still interact.

These rules are a proposed typed-effects contract. The existing runtime does not yet implement all these hooks. If an effect cannot be delivered cleanly, replace it in the reviewed design; never ship its description with a missing handler.

## Character builds

Each numbered purchase corresponds to its 1/2/3/4 AP cost. Source arts and core level skills remain available to both directions. The example descriptions intentionally name the exact effect rather than relying on thematic ability names.

### Nova

Saboteur improves setup and stability pressure, paying an action to prepare a target. Improviser gains faster pressure and repeatable group damage, but no new hard control. Brittle/Weak cooperation and opening damage are different opportunities; neither branch receives a universal damage multiplier.

| Path | AP | Purchase | Proposed consequence |
|---|---:|---|---|
| Saboteur | 1 | Grounded Wire | Arc Tether deals 20% more stability damage. Its HP damage, Shock and cooldown are unchanged. |
| Saboteur | 2 | Controlled Vent | Cryo Vent base power increases from 55 to 65. It applies Brittle for 2 target turns and does not apply Frozen. |
| Saboteur | 3 | Breaker Bolt | New active: one enemy; physical; base power 100; agility scaling; cooldown 3 personal turns; 50% more stability damage than its normal resolved hit; no status application. |
| Saboteur | 4 | Exposed Seam | Nova direct hits deal 15% more HP damage to a target with Weak or Brittle. Having both does not double this bonus; no damage-over-time bonus. |
| Improviser | 1 | Quiet Steps | Permanent +1 Speed, added to the base Speed input once. Does not add Agility or a percentage speed multiplier. |
| Improviser | 2 | Scatter Charge | New active: all living enemies; physical; base power 60; agility scaling; cooldown 2 personal turns; no status or guard-breaking effect. |
| Improviser | 3 | Hot Start | Nova direct hits deal 15% more HP damage to a target above 75% HP when that hit is evaluated. No damage-over-time bonus. |
| Improviser | 4 | Keep Moving | After Nova personally defeats at least one enemy with a damaging action, apply Shield to Nova for 1 personal turn. At most once per action and twice per battle; damage-over-time kills do not trigger it. |

### Zeke

Protector spends Zeke actions reducing danger to a chosen ally. Disruptor spends them creating breaks and a consumable opening for someone else. Cover does not secretly redirect attacks, and mitigation must be tested with Guard, Shield, gear and meals.

| Path | AP | Purchase | Proposed consequence |
|---|---:|---|---|
| Protector | 1 | Cover | New active: one living ally, including Zeke; cooldown 3 personal turns; applies Shield for 2 target turns (25% less incoming damage under the reviewed Shield rule); no heal, damage or interception. |
| Protector | 2 | Reinforced Harness | Permanent +1 Vitality. Derived maximum HP and defense change through their shared formulas; no duplicate flat HP bonus. |
| Protector | 3 | Steady Cover | Cover additionally restores 20 stability to its target, capped at that target current maximum. Does not heal HP, remove an existing skip effect or grant a turn. |
| Protector | 4 | Last Line | Once per battle, Zeke may cast Cover while it is on cooldown if the selected living ally is below 30% HP at commitment. Spend a normal ATB action, apply ordinary Cover and Steady Cover if learned, and restart Cover's full 3-turn cooldown. Consume the emergency charge only for a valid cast that bypassed an active cooldown. No extra Guard, stronger Shield, heal, free action or stacked Shield copy. |
| Disruptor | 1 | Training Session | Permanent +1 Strength; derived Attack changes through the shared formula. |
| Disruptor | 2 | Fault Line | Shatter Blow deals 20% more stability damage. Its HP damage, Weak duration and cooldown are unchanged. |
| Disruptor | 3 | Opening | When Shatter Blow successfully applies Weak, also mark that enemy for 2 target turns. The next direct damaging action from another crew member consumes the mark for +10% HP damage against that enemy. One mark per target; reapplication refreshes but does not stack; damage-over-time and Zeke own actions do not consume it. |
| Disruptor | 4 | Controlled Demolition | Zeke physical direct hits against an enemy with Weak deal 15% more stability damage. No HP damage bonus, immunity bypass or extra skipped turn. |

### Orion

Field Medic responds to actual HP pressure and can waste value through overhealing. Controller reduces incoming threat through Weak and stability pressure, but does not get longer Freeze or immunity bypass. Both retain the proposed basic Field Patch recovery.

Orion's care uses available medical equipment and practiced treatment of wounds and damaged protective gear. Proposed presentation: a portable medical applicator, a fast field patch and careful follow-up. These are flavor treatments for the reviewed HP/Shield effects, not extra consumable costs. Show recipient-appropriate care for Gh0st without rewriting his identity, replacing consciousness or inventing regeneration of destroyed bodies. Do not claim the Aethel invented this equipment: [the lore](../story/Lore_Bible.md) establishes survivors and operators, not a universal Aethel nanotechnology origin. Existing Nano Repair terminology comes from the production skill catalog and remains a separate ability contract; it does not make every Orion heal a nanite or Source power. Field Patch deliberately avoids implying access to the later Anchor's stasis ability.

| Path | AP | Purchase | Proposed consequence |
|---|---:|---|---|
| Field Medic | 1 | Field Resilience | Permanent +1 Vitality; maximum HP and defense use the shared formulas. |
| Field Medic | 2 | Field Treatment | New active: one living ally; cooldown 4 personal turns; restore 20% of target derived maximum HP, rounded down with minimum 1 and capped at missing HP. No Focus multiplier, resurrection or baseline recovery-over-time effect. |
| Field Medic | 3 | Triage | Field Treatment restores an additional 5% of target maximum HP when the target is below 35% HP before the cast, for 25% total. Calculate the combined amount once, round down once, and emit one direct-heal event per target. |
| Field Medic | 4 | Aftercare | Field Treatment additionally applies Aftercare for 2 target turns: each turn-end restores floor(cast-time target maximum HP / 20), minimum 1, capped at missing HP. Aftercare and Regen share one recovery-over-time slot. Existing Regen has priority and retains its potency and remaining duration; this cast neither extends it nor queues Aftercare. Recasting Aftercare refreshes its own duration to 2 without stacking ticks; a later Regen replaces it. No resurrection, Focus multiplier or post-battle ticking. |
| Controller | 1 | Clear Focus | Permanent +1 Focus, using the shared potency/accuracy/critical/resistance formulas exactly once. |
| Controller | 2 | Prism Lens | Prism Lance deals 20% more stability damage. Its HP damage, element and cooldown are unchanged. |
| Controller | 3 | Safe Exposure | Prism Lance applies Weak for 1 target turn only if the target did not have Weak before the action and is not immune. An existing Weak is neither extended nor replaced by this talent. |
| Controller | 4 | Steady Field | Weak newly applied by Orion lasts 1 additional target turn, capped at 3. Does not change potency, extend existing effects retroactively, affect Frozen/Stun/Stagger or bypass immunity. |

### Gh0st

Precision rewards target selection and finishers. Tactical trades burst for stability pressure, Weak and a personal safeguard against lost turns. Recenter protects one turn per battle; it does not counterattack or protect the whole crew.

Recenter is a recovery routine Gh0st develops and chooses to practice: regaining his footing, checking his movement and returning to his own chosen task. The World 4 capstone gate places it after the Foundry confrontation. Its automatic combat trigger represents a practiced skill, not a Dominion command, an Elara memory, Nova's control over him or a Source Lock activating. [His arc](../story/Character_Arcs.md) and [the retained story](../story/Starborn_Master_Story.md) distinguish an opportunity to choose from an order to protect. Keep any supporting scene brief and optional; no mandatory speech or new plot beat is required.

| Path | AP | Purchase | Proposed consequence |
|---|---:|---|---|
| Precision | 1 | Scope | Headshot deals 10% more HP damage against a target at or above 75% HP when the hit is evaluated. |
| Precision | 2 | Steady Aim | Headshot gains 5 percentage points of critical chance, subject to the normal critical cap. No other action gains this bonus. |
| Precision | 3 | Execute | Gh0st single-target direct damaging skills deal 15% more HP damage to targets at or below 30% HP before the hit. No instant kill, boss exemption or damage-over-time bonus. |
| Precision | 4 | Cool Head | Landing a basic attack gives Gh0st one steady-shot charge. His next valid Headshot consumes it for +15% direct HP damage, even if that Headshot misses. Charges do not stack and expire at battle end; invalid or canceled actions consume none. Headshot keeps base cooldown 2, with no talent cooldown refund, ATB grant or critical-hit recharge. The shared +25% talent-damage cap includes Scope and Execute. |
| Tactical | 1 | Quick Position | Permanent +1 Agility; Speed/evasion use their shared formulas. No immediate ATB grant. |
| Tactical | 2 | Surgical Strike | Gh0st basic attack deals 20% more stability damage, with no HP damage or extra status bonus. |
| Tactical | 3 | Disarm | New active: one enemy; physical; base power 60; agility scaling; cooldown 3 personal turns; applies Weak for 2 target turns, respecting immunity. No Stun, silence or instant kill. |
| Tactical | 4 | Recenter | Once per battle, prevent a scheduled-turn skip caused by Stun or Stagger on Gh0st, removing those two effects from him. Frozen and other unhandled skip effects remain; if any would still skip this turn, do not activate or consume Recenter. Other statuses remain; no bonus action, counterattack, damage or team-wide immunity. |

## Story and balance refinements

These replace the earlier draft contracts. They keep every path's cost and prerequisite sequence intact. Draft node IDs change to `gh0st_recenter_v2`, `orion_field_treatment_v2` and `orion_aftercare_v2`; no deployed save uses these proposed IDs. The existing `orion_stasis_stitch` skill ID is preserved under its proposed Field Patch display name.

| Pressure point | Refined decision | Intended tradeoff |
|---|---|---|
| Emergency protection | Last Line permits one timely, ordinary Cover cast while that skill is cooling down. It does not add Guard or increase mitigation. | Spend Zeke's next normal action and the one battle charge now, or save it for a later emergency. The cast resets the full cooldown. |
| Direct healing plus recovery | Field Treatment heals 20%, or 25% below the Triage threshold, with cooldown 4. Aftercare adds two 5% ticks in one non-stacking recovery slot. | At maximum HP 120, normal direct healing is 24; emergency healing is 30; follow-up adds at most 6 + 6. The emergency per-cast total is 42 HP before overhealing, rather than the first draft's 56. Recovery still takes time, the target must survive, and stronger core Regen takes priority without gaining extra duration. |
| Repeated Headshots | Cool Head keeps Headshot's base cooldown at 2. A landed basic attack prepares one boosted Headshot instead of permanently increasing cast frequency. | Choose the opening Headshot immediately or spend an action preparing it. Repeated basics cannot bank multiple charges, and the total talent damage bonus remains capped at 25%. |

The refinement removes the new sources of mitigation multiplication and permanent cooldown acceleration; it does not globally retune existing Guard/Shield or weakness cooldown rewards. Those existing interactions remain part of the balance matrix. Recenter's story treatment also limits its scope: ordinary disruption can be overcome through practiced recovery, but actual stasis remains a threat.

Focused implementation checks are recorded in the JSON candidate: ready versus on-cooldown Cover; invalid or healed targets at commitment; recovery rounding and overhealing; Regen priority at one remaining turn; prepared-shot consumption on hits/misses; Scope/Execute/allied-bonus caps; mixed Frozen and Stun; and checkpoint recovery without renewed battle charges. The illustrative HP and cap arithmetic is checked separately from gameplay. These are contracts and test cases to implement, not claims of completed behavior tests.

## Proposed AP curve and earned builds

These are **new candidate rewards**, not observed current earnings. Preserve the five existing boss AP amounts. Add small one-time quest/recruitment awards, with no AP from ordinary repeatable encounters. Every award needs a persisted claim tied to its actual completion source; stage replay, dialogue replay and save/reload must not grant it twice. In NG+, a new campaign can earn its own awards while previous purchases and unused AP carry over.

| Checkpoint | New AP during world, including retained boss awards | Main-route AP total | Example total invested: Nova / Zeke / Orion / Gh0st | Banked AP |
|---|---:|---:|---|---:|
| World 1 departure | 6 | 6 | 3 / 3 / 0 / 0 | 0 |
| World 2 departure | 8 | 14 | 3 / 3 / 3 / 3 | 2 |
| World 3 departure | 10 | 24 | 6 / 6 / 6 / 6 | 0 |
| World 4 departure | 10 | 34 | 10 / 10 / 6 / 6 | 2 |
| World 5 departure | 8 | 42 | 10 / 10 / 10 / 10 | 2 |
| Before the first finale form | 6 | 48 | 13 / 11 / 11 / 11 | 2 |

The example is affordable by arithmetic and follows node prerequisites; it is not a completed gameplay run. A 3-AP investment buys the first two nodes, 6 buys three, and 10 completes a path after the World 4 gate. At 13 AP a character has a full path plus the first two nodes of the other. The sample completes one path per crew member by World 5 and adds cross-branch choices before the finale. The two early 10-AP allocations can go to any two crew, rather than always Nova and Zeke.

Proposed award placements:

- **World 1:** 2 each at completion of `w1_mq01`, `w1_mq03` and `w1_mq05`. The first points follow the starter repair; Zeke can spend by departure.
- **World 2:** 2 at Orion recruitment, 2 at Gh0st recruitment, 4 at `w2_mq05`. Award recruitment AP once from the actual recruitment callback, independently of later cinematic replay. Verify that both recruitment rewards arrive before the intended next purchases.
- **World 3:** 2 each at `w3_mq11`, `w3_mq13`, `w3_mq14` and `w3_mq15`, plus the existing Administrator 2.
- **World 4:** 2 at `w4_mq16`, 2 at `w4_mq18`, 3 at `w4_mq19`, plus the existing Titan 3. The final row becomes available only after `w4_mq20` completion, not merely spending enough AP before Titan.
- **World 5:** 2 at `w5_mq22`, 2 at `w5_mq24`, plus the existing Avatar 4.
- **World 6 before finale:** 2 each at `w6_mq26`, `w6_mq28` and `w6_mq29`. Vale's 5 and God's 8 are excluded from the preparation budget; they yield 61 main-route AP after the finale. Do not rely on a menu opportunity between forms.

Propose an additional 2 AP for one reviewed optional quest per world: `w1_sq03`, `w2_sq01`, `w3_sq12`, `w4_sq19`, `w5_sq22`, `w6_sq26`. These keep their existing rewards. They supply up to 12 extra AP, yielding at most 60 before the finale and 73 afterward. Their exact completion timing and availability must be validated; the main-route examples need none of them.

A first campaign cannot buy all 80 AP of nodes even with these optional awards. That preserves meaningful allocation without requiring NG+ for a complete specialization. NG+ can eventually complete both directions. Ordinary players must never need to replay a unique boss or farm a repeatable battle to finish their chosen path.

## Builds and situations that should feel different

| Party approach | Picks to emphasize | Intended strength | Cost or weakness to preserve |
|---|---|---|---|
| Prepared crew | Saboteur, Protector, Field Medic, Precision | Reliable setup, safe recovery, focused kills | Setup and protection spend actions that could deal damage; recovery can overheal. |
| Fast clearance | Improviser, Disruptor, Controller, Precision | Remove groups and exploit short openings | Less AP-funded emergency recovery; wrong target order wastes the opening. |
| Recovery under pressure | Saboteur, Protector, Field Medic, Tactical | Rebuild stability, survive pressure and avoid one lost turn | Lower burst; defensive casts still cost turns and cannot sustain a permanent immunity loop. |
| Hybrid | First two nodes in both directions, then deepen one | Earlier access to tools and flexibility | Delayed third/fourth-node payoff; hybrids must not strictly dominate specialists. |

A single durable boss should favor reliable setup/precision; groups should reward Scatter Charge and kill-triggered Shield; high pressure should reward protection/triage; debuff-immune enemies should favor raw stability damage over Weak-dependent bonuses. Encounter-specific advantage is intentional. No branch may become useless for an entire world, and no mandatory boss should require a particular AP path.

## Existing abilities and saves

The JSON packet gives every old node a proposed disposition. All 100 old tree records leave the AP catalog. Learned combat skills survive; the 85 non-catalog tree entries are replaced through a reviewed respec, not silently translated into unrelated effects. New node IDs and a purchase ledger identify owner, acquisition origin and AP actually paid. Story/level unlocks count for ability access but never for AP investment.

For the 43 crew skills, complete effect and acquisition tracing before release. Missing explicit starter/level/event grants are screening findings, not proof that an ability is unreachable. The seven non-Signal-Jammer entries without a mapped grant are Pulse Grenade, Scrap Shot, Cheap Shot, Synergy Pitch, Stasis Stitch, Target Lock and Slide Kick. The Stasis Stitch ID receives the deliberate Field Patch core acquisition above; the other six need a retain/retire decision after runtime/cinematic tracing. They do not become new AP purchases just to fill a tree.

Headshot also requires a description contract: its current power is 95, while prose promises 150% ATK and piercing. The inspected combat files contain no piercing-tag handler. Recommend keeping its current damage tuning for the initial baseline and describing a precise physical shot without unsupported armor-bypass or multiplier claims. If armor penetration is desired, specify and measure it as an explicit mechanic first. Likewise, verify earned passives such as Legacy and Source Balance rather than assuming a recorded unlock produces their promised consequence.

Recommended migration policy for review:

1. Preserve learned combat skills and permanent schematics; preserve unused AP. Clear only the old tree effects that the approved migration replaces. A same-ID active skill is not also a permanent passive, so Smoke Bomb loses its accidental automatic passive while retaining its active evasion effect.
2. Since old saves lack paid-purchase history, offer a generous **one-time legacy respec credit** equal to the old catalog costs of all recorded legacy tree unlocks. This intentionally includes ambiguous free grants; call it compensation, not an exact paid-AP refund. Its catalog upper bound is 224 AP for all 100 recorded entries, not a normal-route earning target. Keep fresh-run balance evidence separate from compensated legacy saves. This policy needs owner review.
3. Backfill newly introduced quest/recruitment AP only where the old save proves the source completed. Do not backfill existing boss AP a second time. Record new claims before resuming play, and backfill the proposed Field Patch core grant for an already recruited Orion, preserving the same skill ID.
4. Persist migration version, claims and purchase origin in manual/auto/quick/backup states and battle checkpoints. A second load or migration pass must change nothing. NG+ retains those permanent investments while resetting campaign award claims for the new run; compensation is never awarded again.

The one-time credit is an alternative to pretending the old paid amount is knowable. If the owner prefers exact refunds, implementation must first establish a reliable historical purchase source. Do not infer paid amounts from the old unlock set and present them as exact.

## Balance review before shipping

First make the contracts executable, including explicit element/status separation, per-skill targeting/duration, capped modifiers and battle-limited triggers. Reject unsupported effects at both catalog validation and public purchase boundaries. Agree a complete skill contract before enabling its acquisition.

Then compare equal earned states, equipment, items, rests and seed lists. The initial matrix uses the four party approaches above, a conservative core-skills baseline, and an intentionally concentrated-AP build. Run representative groups/elites/bosses across all six worlds and consecutive routes. Use 30 matched seeds as a screen; expand close or unstable comparisons to 100. These counts are diagnostics, not statistical certification or human win-rate predictions.

Proposed screening targets, to agree before execution:

- Every planned main-route purchase path is affordable at its checkpoint without optional AP. Both recruitment milestones create a useful first purchase within World 2.
- Each character direction has an earned party configuration that wins at least 27/30 ordinary encounters and 21/30 representative bosses with ordinary route supplies and a documented competent policy. Record failures and policy limitations; do not tune from a single seed or synthetic maximum-level party.
- No tested branch/party approach should dominate wins, duration and consumable use across every encounter family. Specialists should beat hybrids in their niche; hybrids should have a useful wider range.
- New talents must not create a repeatable boss action lock or a heal/control loop whose costs disappear. Measure scheduled turns, actually executed turns, skipped-turn uptime, cooldown refunds and recovery triggers. Review skipped-turn uptime above 50% even if it comes from the existing kit; that is an investigation threshold, not an automatic global nerf.
- Compare pre-finale affordability and pressure separately from post-victory AP. Preserve the four-minute combat acceptance cap and report longer diagnostics separately.
- Public purchase -> effect -> save/reload -> effect -> NG+ must retain the agreed build. Test immunities, zero targets, invalid/canceled actions, simultaneous status effects, actual meal/gear HP changes, large text and both platform renderers.

Human testing comes after these gates: comprehension of purchases, useful feedback, touch/mouse targeting, then sustained route/resource pressure and enjoyment. Until those checks pass, this packet is **a concrete, varied candidate with checked arithmetic**, not a claim of finished balance.

## Next implementation slice

Review the proposed scope and compensation policy, resolve the remaining core-skill contracts, and pilot Nova's two paths plus derived HP/purchase-history infrastructure. Prove public purchases against earned World 1/2 states and one later saved build before expanding to the other crew. The new budget is validated through public campaign routes before becoming production rewards. No production ability, reward, save schema or HP logic was changed to create this packet.

Static verification passed: 32 unique proposed nodes, two four-node paths per crew, 80 AP total cost, exact coverage of all 100 old nodes and 43 crew skill records, six affordable checkpoint examples, matching JSON/Markdown effect contracts, and resolving source/plan references. The current baseline source hashes still match. No game suite was rerun for this documentation-only candidate; the last passing gameplay test record remains the regression review linked from the overarching plan.

