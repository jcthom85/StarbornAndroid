# Full-crew automated balance pass

Follow-up: [the tempo tuning pass](CREW_TEMPO_TUNING_2026-10-15.md) implements and verifies a prototype-only adjustment to the two encounter spikes. Results below retain the original rules and remain historical baseline evidence.

October 15, 2026. **The prototype works, but these results do not establish balanced party builds.** Prepared fights expose two encounter spikes, and paid specialization is not consistently better than retaining the core abilities alone. No production gameplay numbers were changed in this pass.

## Executed comparison

The shared ATB combat controller completed **1,728 independent fights**, **242 fights across 128 attrition sequences**, and **one exact deterministic replay**. All resolved without timeouts or rejected player commands. Sixteen seeds were paired across allocations. The full Android-hosted regression suite passes **681 tests**, with zero failures, errors or skips. Desktop/device suites were not rerun because this pass changes test infrastructure and documentation only.

[Compact retained evidence](evidence/crew-balance-2026-10-15.json) contains per-group results, paired differences from the control, sequence completion, source hashes and raw-output hashes. Full traces, starting combat stats, individual skill/item usage, purchased nodes, outcomes, HP, observed Break transitions and virtual fight duration are generated under `app/build/reports/crew-balance/`. Virtual duration includes presentation pauses and is not a human reaction-time measurement.

| Allocation | Nova | Zeke | Orion | Gh0st |
|---|---|---|---|---|
| Protection | Saboteur | Protector | Field Medic | Precision |
| Break/setup | Saboteur | Disruptor | Controller | Tactical |
| Precision | Improviser | Disruptor | Field Medic | Precision |
| Unspent control | Core abilities only | Core abilities only | Core abilities only | Core abilities only |

The three purchased allocations spend equal AP within each starting state. The control retains all earned core abilities, including Field Patch, and leaves AP unspent. At 26 AP, each owner buys three ranks plus two cross-branch first ranks across the party. At 36 AP, three paths reach rank four and one reaches rank three; that partial path differs by allocation. At 44 AP, all four selected paths are complete with four AP spent across secondary paths. Exact receipts are in the raw rows. This compares whole allocations, not the isolated value of an individual node.

Nine authored encounters span Foundry, Void and Source. Each independent fight restores the same gear, XP, HP, inventory and meal state for its cohort. Enemy formation and room placement are explicit fixtures; this does not prove traversal or the gear/levels a player will have on reaching later bosses.

Saved-state cohorts preserve campaign wear. Prepared cohorts use public gameplay actions:

- Early: land on the Foundry shelf, travel to the nearby refuge and rest. Landing completes the quest and legitimately increases AP from **26 to 28**; the prepared comparisons spend 28, preserving that award.
- Middle: equip the owned Heat Liner on Orion; sell seven power cells and four composite plates; buy six Medkit+ and one Glimmer Tonic; consume the meal and rest. Credits fall from 1,152 to 121. No free supplies are added.
- Late: consume one carried Glimmer Tonic and rest at the campfire. AP remains 44.

AP purchases happen after preparation. A Vitality purchase raises maximum HP without supplying free healing. Attrition sequences carry actual remaining HP, inventory, awarded XP/AP and meal duration from fighters to officer to Avatar, with no rest or replenishment. They stop at defeat. This is a deliberately compressed encounter sequence, not the continuous authored route.

## Findings

**The Orbital Fighter pair is a persistent pressure point.** All allocations lose all 16 saved-state attempts before preparation. After preparation, results remain mixed:

| Allocation | Tactical wins | Aggressive wins | Tactical mean knockouts | Tactical mean items |
|---|---:|---:|---:|---:|
| Protection | 6/16 | 8/16 | 2.69 | 3.81 |
| Break/setup | 10/16 | 10/16 | 2.00 | 2.00 |
| Precision | 5/16 | 7/16 | 2.81 | 1.56 |
| Unspent | 4/16 | 8/16 | 3.19 | 1.75 |

The inspected prepared Break fixture has fighter Speed 46 against crew Speed 16–23, even with the meal. Protection spends more consumables under the tactical policy without a consistent win advantage. This warrants investigation of action pressure and the cost of spending a turn on protection; it does not establish the optimal policy or an exact nerf size. Sixteen samples per cell are directional evidence, not a claim of statistical superiority.

**The late three-enemy formation is another spike.** Against Source Shadow, Distorted Sentinel and Glitch Hound, all wounded saved-state cohorts lose 16/16. With a meal and rest, tactical builds win only 6–7/16; aggressive builds win 9–12/16. In contrast, every prepared allocation, including the unspent control, wins 16/16 against the tested single-core Source boss. Preparation, enemy count and action economy have a larger visible effect here than some AP choices. These are checkpoint encounter comparisons, not a claim that the full finale is easy.

**Paid value is uneven and policy-sensitive.** Break/setup has the strongest observed fighter results, but aggressive protection matches the unspent control's outcomes seed-for-seed. Prepared Foundry encounters and several single-enemy fights are won by every allocation. In the attrition sequence, tactical completion is 9/16 for Break, 6/16 protection, 5/16 precision and 3/16 unspent; aggressive completion is 9/16, 7/16, 6/16 and 7/16 respectively. A blanket AP-cost adjustment would obscure these differences.

**All purchased active abilities are exercised.** Across eligible independent fights: Breaker Bolt 643/864, Scatter Charge 206/432, Cover 216/1008, Field Treatment 397/864, Disarm 124/432. Eligibility means purchased, not necessarily a surviving owner with a useful casting opportunity. These counts therefore flag where to inspect traces; low use alone does not prove a weak or broken ability. Passive and capstone correctness has separate focused tests; this pass does not show that every passive meaningfully affected a player's decision.

## Policy and interpretation

Both policies use legal available skills, shared cooldowns and carried items. They focus low-HP enemies and estimate offensive value using power, target count and relevant talent modifiers. Tactical heals earlier, uses Cover, adds Weak/setup and Stun value, and supports group recovery. Aggressive heals later and emphasizes damage. Both can use Link when enough crew are wounded. They are deterministic heuristics, with no lookahead, resistance-learning strategy or claim of optimal play.

An initial 480-fight screening used a weaker policy that omitted group healing and Disruption Pulse. Its compact outputs are kept under `policy-v1/` in the generated report directory. Findings above use only the revised policy. Do not merge the two samples or tune around the initial driver's omissions.

Next work should isolate protective-action value and the two group-encounter spikes. Use paired changes with the same saves and seeds, changing one perk, policy rule or encounter parameter at a time. Then manually play the promising allocations to assess clarity, satisfying choices and story fit. Production migration and final numerical tuning remain pending.

## Reproduce

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*LegalRouteCampaignRunnerTest.crewBuildsAcrossSeedsProduceComparableCombatEvidence' --console=plain
python scripts/summarize_crew_balance.py
```

`CREW_BALANCE_SEEDS` optionally selects comma-separated seeds. The default sixteen are recorded in the evidence JSON. The summarizer validates paired samples, AP accounting, resolved outcomes and command acceptance; it does not manufacture passes. Full regression: `.\gradlew.bat :app:testDebugUnitTest --console=plain`. Execution logs: `tmp/crew-balance-run-v2.log` and `tmp/crew-balance-regression.log`.
