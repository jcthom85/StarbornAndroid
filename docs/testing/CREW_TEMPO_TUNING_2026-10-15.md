# Crew prototype encounter tempo tuning

October 15, 2026. Follow-up to the [initial balance pass](CREW_BALANCE_PASS_2026-10-15.md). The selected change reduces base Speed in **two prototype encounters**. Cover, Treatment, damage, HP, Agility, AP prices and earned progression retain their previous rules. This is a measured first tuning pass, not final party balance or production migration.

## Selected behavior

`CrewEncounterTempo` applies these base Speed values only in progression prototype version 1:

| Encounter / enemy | Old base Speed | New base Speed | Old effective Speed | New effective Speed |
|---|---:|---:|---:|---:|
| Executive Dock / Orbital Fighter | 20 | 8 | 46 | 34 |
| Memory Fragments / Source Shadow | 18 | 8 | 41 | 31 |
| Memory Fragments / Distorted Sentinel | 18 | 8 | 41 | 31 |
| Memory Fragments / Glitch Hound | 24 | 10 | 56 | 42 |

Agility still contributes through the shared Speed formula and retains its accuracy/evasion effects. The enemies remain fast relative to the prepared crew. Other appearances of these enemies, other rooms, ordinary campaign mode and unknown prototype versions keep their authored stats. The combat controller accepts an explicit immutable tempo profile so experiments can reproduce original rules without changing production assets or adding live environment switches.

## Paired experiments

Ten screening variants each ran 256 prepared fights: two encounters × four allocations × two policies × sixteen seeds. These included the original tempo, three speed candidates, skipping Cover, earlier Cover, a third Cover turn, stronger Treatment, and each support buff combined separately with the selected tempo. Two further 256-fight runs compared original versus selected rules on **sixteen disjoint holdout seeds**. Every run resolved without rejected commands or timeouts and included an exact deterministic replay.

The initial tempo candidates used test-only enemy catalog overlays. Temporary source probes changed only Cover duration or Treatment percentage and restored the exact original bytes after each run; their replacement specifications are retained with the experiment evidence. Support changes were not selected. The final controller's explicit tempo injection subsequently reproduced all 256 original baseline observations exactly.

| Prepared holdout comparison | Original wins | Selected wins |
|---|---:|---:|
| Orbital Fighter pair | 68/128 | 115/128 |
| Source three-enemy formation | 50/128 | 116/128 |
| Combined | 118/256 (46.1%) | 231/256 (90.2%) |

Each encounter's denominator pools four allocations and two policies over sixteen paired seeds; these are not 128 independent human players. The selected tempo improved results in all sixteen holdout encounter/build/policy groups. Support policies and individual builds still differ. No optimal-policy or statistical-superiority claim is made.

The stronger speed reduction won every screening fight, including the unspent control, and was rejected. The smaller reduction left protection and some tactical late builds unreliable. The selected middle candidate produced 236/256 screening wins, then 231/256 fresh-seed wins, preserving defeats rather than guaranteeing victory.

Longer Cover helped two original-tempo fighter runs but did not improve wins once tempo was corrected. Stronger Treatment had mixed effects, including regressions. Earlier Cover often traded damage for protection without improving survival; skipping Cover changed some fight details but no screening win/loss outcomes. These results do not justify changing their numbers yet. They also do not prove their current designs are satisfying: paid-build identity and protection's action value remain open manual-play questions.

## Broader verification

The final default matrix completed **1,728 independent fights, 363 fights across 128 attrition sequences, and an exact replay**. The 1,344 individual observations outside the two tuned encounters matched the previous matrix exactly. In the compressed World 5 sequence, tactical completion increased from 6/16 to 14/16 for protection, 9/16 to 14/16 Break, 5/16 to 14/16 precision, and 3/16 to 14/16 unspent. Aggressive completion was 12/16, 15/16, 13/16 and 11/16 respectively. This preserves the original fixture limitations: scene placement is explicit, and the sequence is not an uninterrupted campaign route.

All **683 Android-hosted unit tests pass** with no failures, errors or skips. Both new scope/Speed tests pass. Five focused desktop integration tests pass (all-crew purchase/reset/checkpoints and existing Nova persistence/HP flows). Debug APK assembly succeeds at `app/build/outputs/apk/debug/app-debug.apk`. This pass did not reinstall the emulator, rerun phone instrumentation, or repeat the entire desktop suite. The emulator's current session was left untouched.

Retained evidence:

- [Screening and holdout aggregates](evidence/crew-tuning-experiments-2026-10-15.json), including variant parameters, temporary source-probe specifications and raw hashes.
- [Final full matrix](evidence/crew-tempo-1-full-matrix-2026-10-15.json), including source hashes and attrition results.
- Full traces: `app/build/reports/crew-balance-experiments/` and `app/build/reports/crew-balance/`. The original full outputs were copied to `pre_tuning_full/` before the final matrix ran. Historical evidence JSON from the earlier pass remains unchanged.

## Reproduce and next acceptance

The standard command in the [initial report](CREW_BALANCE_PASS_2026-10-15.md) now uses the selected shipping prototype profile. `CREW_BALANCE_TARGETED=1` limits a run to the two prepared encounters. `CREW_BALANCE_TEMPO=baseline`, `measured`, `moderate` or `strong` explicitly selects experimental profiles; omitting it uses the game's current default. `CREW_BALANCE_REPORT` selects a separate report subdirectory, and `CREW_BALANCE_SEEDS` selects paired seeds. These environment settings are consumed by test infrastructure, not the app.

Next acceptance is manual play with the three allocations: evaluate whether the crew gets readable opportunities to act, whether protection is worth choosing, and whether each build changes decisions. The unspent control remains competitive, so this pass does not approve final AP balance, perk redesign or migration of existing production saves.
