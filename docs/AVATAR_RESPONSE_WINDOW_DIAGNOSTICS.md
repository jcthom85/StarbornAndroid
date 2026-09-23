# Avatar response-window diagnostics

2026-09-22. Production assets/code unchanged; experiments modify a spied asset source inside the purchased Avatar test fixture only.

## Results

Each of 35 seeds ran twice deterministically under each condition. Equipment, AP, purchases, supplies and player policy were held constant.

| Condition | Victories | Defeats | Timeouts | Character deaths | Mean duration |
| --- | ---: | ---: | ---: | ---: | ---: |
| Current production | 18 | 17 | 0 | 78 | 66.46s |
| Recovery after each major special | 34 | 1 | 0 | 7 | 51.59s |

Recovery uses the existing harmless `gathering_silence` skill after `missile_barrage`, `seismic_stomp` and `roar_of_the_source`. It adds no opening pause or numeric stat changes. This borrowed skill is an experimental placeholder, not proposed Avatar naming/presentation. Changing action selection also changes subsequent random draws, so identical seed labels do not imply identical later critical/target rolls.

## Baseline observations

Only 10 of 78 death observations contain a critical damage event; criticals alone do not explain the losses. Every defeated party loses its first member within 44.75 seconds. Four winning seeds also suffer casualties, so an early death is not proof of inevitable defeat.

- Seed 1: Orion dies at 19.25s to Seismic Stomp. Nova dies at 22.75s to a 122-damage critical basic attack from full 120 HP, with her prior ATB meter at 0.968. Single-target healing cannot prevent that full-health hit. All healing supplies remain at that moment.
- Seed 16: Gh0st dies at 11.25s from Missile Barrage at 46 HP; all healing stock remains and no party member has a full prior ATB meter. In the recovery experiment, this early death still occurs at 11s before a recovery can help; the party eventually loses at 121.25s.
- Seed 32: Gh0st dies at 26.75s. The remaining party persists until 159.5s; Nova later falls to a 124-damage critical at 135s. Two rations remain at defeat. This is an early casualty followed by prolonged attrition, not simply running out of all supplies.

The trace samples every 250ms around production action processing. It records new action/damage/status log entries, HP before/after, ATB meters before/after, awaiting actor and live combat inventory. These are sampled observations, not frame-exact reaction timings. Deaths are observed alive-to-dead transitions; a critical flag on the same tick is reported as an association rather than automatically assigned causality.

## Recommendation and remaining comparisons

Response windows merit further testing: preserving party actions can improve both survival and pacing. Do not promote the all-special recovery configuration directly from this 34/35 result. Compare a narrower window after Missile Barrage alone and a separately implemented repeated-target restriction; use fresh seeds before choosing production behavior. Inspect the initial basic-attack/special sequence in seed 16 and whether players have an understandable defensive response before its first casualty. A global critical or HP change is not established by this evidence.

Evidence: `test-results/avatar-trace-35/campaign.xml` (baseline) and `recovery.xml` (candidate). Both CampaignEventIntegrationTest runs passed. Baseline outcomes reproduce the previous 35-seed report. Gradle test success indicates checks and measurements completed, not universal victory.

Reproduce with `AVATAR_BASELINE_SEEDS=1,2,...,35`, `AVATAR_TRACE=1`, optional `AVATAR_RECOVERY=1`, and `:app:testDebugUnitTest --rerun --tests '*CampaignEventIntegrationTest'`. Omit diagnostic flags for default coverage. No commit, upload or release in this pass.
