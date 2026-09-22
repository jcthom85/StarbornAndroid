# Campaign validation status — 2026-09-22

This is the current coverage summary, not a claim of complete game QA. All **547 unit tests pass**. Automated main-story/event checks cover Worlds 1–6, including reward delivery, recruitment, journal/save restoration and final completion. Scripted victories used in progression traversal do not prove those battles are balanced; separate production-combat runs supply that evidence.

Latest follow-up: `REMAINING_BOSS_DIAGNOSTICS.md` closes the timeout investigation (all extended runs terminate; some win slowly, others lose), reproduces Avatar's burst-driven loss and rejects an unsuccessful early-healing policy. The current Foundry mixed-party refresh completes 720 fights with 627 wins and no timeouts; selected consecutive-fight gates pass. Full-route resource validation and broader build fairness are still open. No further production tuning was applied.

| World | Verified coverage | Still needed |
| --- | --- | --- |
| 1 | Story/event checks; five-seed Mining Depths attrition; Siren defense-loop regression | Broader builds, optional encounters and player-facing route playthrough |
| 2 | Story/event checks; five-seed Undercity attrition; Ruin Guardian no longer blocks that gate | More builds, encounter combinations and sustained resource checks |
| 3 | Story/event, reward, transition and save checks; feeds the scripted Foundry arrival checkpoint | Comparable encounter-level balance matrix and sustained route combat |
| 4 — Foundry | Largest encounter matrix, status/AI corrections, Android smoke checks; scripted arrival level 7 (7,050 XP, 2 AP, 1,070 credits), pre-Titan level 8 (10,450 XP); Titan wins 5/5 current seeds; camp/attrition fixtures pass | Rerun mixed-party matrix on the final mechanics; validate actual route attrition and affordable build choices without treating scripted victories or fresh-fight stock as player-earned survival |
| 5 | Story/event/save checks; pre-Avatar level 10 checkpoint and legal gear/AP/shop preparation; current prepared Avatar build wins 4/5 | Investigate the losing seed and test more builds/encounter parties; passing measurement tests do not imply every sampled fight wins |
| 6 | Story/ending/save checks; Link targeting fixed; final-boss opening/recovery cycle; 35/35 standard-policy finale wins; real menu-to-heal emulator check | Broader tactics still have timeouts; polish/readability and complete human-style route verification |

## Erosion isolation

Same scripted checkpoint preparation, supplies, support logic and seed set (1–35), deterministic repeats. Offensive choices only vary. These are fixed-policy sensitivity comparisons, not estimated player win rates.

| Policy | Wins | Defeats | Timeouts |
| --- | ---: | ---: | ---: |
| Standard | 35 | 0 | 0 |
| Standard minus Venom Edge | 32 | 1 | 2 |
| Direct damage | 18 | 5 | 12 |
| Direct damage plus Venom Edge | 34 | 0 | 1 |
| Control-first | 31 | 2 | 2 |

Venom Edge has a large marginal benefit to the direct policy, but a strong non-Venom strategy exists in the standard-minus-Venom comparison. This does **not** establish that erosion alone causes the gap: removing/adding the skill also changes immediate damage, cooldown choices, action order and random draw consumption. Next compare Venom Edge with only its erosion application disabled in a clearly labelled test fixture, or quantify damage by source from traces, before changing global DOT values. Do not nerf erosion merely because the direct policy performs worse.

The recovery banner now explains **“Recovery window — attack or heal”**, and Link's summary states **“Heals party + Regen”**. No damage, HP, cooldown or status-duration values were changed during this isolation/readability pass. BurgQuest Demo is unchanged.

The rebuilt Android presentation test passes (1/1, 10.446 seconds). Both text assertions and the real Link menu-to-party-heal interaction pass; screenshots were visually reviewed at `test-results/finale-presentation/clarified-ui/`. The recovery subtitle is fully visible and Link's summary wraps within its card without clipping.

## Recommended order

1. Close the remaining finale tactic timeouts and examine World 5's losing seed, without weakening acceptance gates.
2. Refresh Foundry's mixed-party and route-resource checks on the stabilized mechanics.
3. Expand Worlds 1–3 and 5 encounter coverage, then run player-facing routes, optional content and save/resume checks across all six worlds.
4. Review and authorize a release separately. Current passing tests are not blanket balance or content sign-off.

Evidence: `test-results/finale-presentation/standard-no-venom.xml`, `direct-venom.xml`, `direct.xml`, `control.xml`, and the current campaign integration XML. Alternative tactic runs deliberately record defeats/timeouts instead of requiring every seed to win. Use `--rerun` when changing environment-selected policies. Older generated “100% complete” audit headlines refer to their scripted scope, not all encounter balance or full manual playtesting.
