# Finale stagger comparison — 2026-09-22

Test-only change: remove `stagger` from Reality Break for phase two (`ascended_god`) via the fixture's asset-source spy. Damage, HP, cooldowns, recovery windows, phase-one mechanics, inventory carryover and player policy remain fixed. This tests removal of this specific stagger source, not a global status change or a fractional-frequency implementation.

Each condition uses seeds 1–70, each repeated twice deterministically. Original seeds 1–35 plus fresh 36–70 are included. All six campaign integration runs passed. The four-minute cap is unchanged.

| Policy | Baseline wins / defeats / timeouts | Candidate wins / defeats / timeouts | Mean phase-two time, baseline → candidate |
| --- | --- | --- | --- |
| Standard | 70 / 0 / 0 | 70 / 0 / 0 | 63.55s → 47.75s |
| Control-first | 61 / 5 / 4 | 68 / 0 / 2 | 155.75s → 111.29s |
| Standard without Venom | 66 / 1 / 3 | 69 / 0 / 1 | 166.21s → 116.95s |

Means include losses and cap timeouts at 240 seconds, so they are observed capped durations, not estimates of eventual completion times. These results describe one automated policy per row, not human win rates. Changing actions also changes downstream random draws; paired seeds do not fix every subsequent roll.

## Interpretation

Reality Break's repeated party stagger materially affects pacing in this harness. Removing it cuts roughly 44–49 seconds from the slower policies' capped means, while preserving their damage/health settings. It also shortens the already successful standard policy by about sixteen seconds; this is a real difficulty/pacing tradeoff, not a universally cost-free improvement.

The candidate still times out on control seeds 32 and 37, and no-Venom seed 63. These differ from baseline timeout seeds (control 21, 27, 53, 68; no-Venom 9, 27, 51), showing why validation cannot focus only on previously failing seeds. Candidate timeouts have not yet been replayed under the longer diagnostic budget, so no claim is made about their eventual outcomes.

Recommendation: retain these measurements as the causal baseline and test a less frequent finale-only stagger before selecting production behavior. A one-time opening stagger or every-other Reality Break could preserve the move's identity, but each requires its own explicitly scoped implementation and comparison. Do not raise the time cap or change the global stagger status. Production finale assets remain unchanged in this pass.

## Reproduction

Evidence: `test-results/finale-stagger-comparison/`, files `control-baseline.xml`, `control-candidate.xml`, `standard_no_venom-baseline.xml`, `standard_no_venom-candidate.xml`, `standard-baseline.xml`, `standard-candidate.xml`.

Set `FINALE_PACING=1`. Candidate only: `FINALE_NO_STAGGER=1`. For control/no-Venom set `FINALE_TACTIC` to `control` or `standard_no_venom` and `FINALE_DIAGNOSTIC_SEEDS` to comma-separated 1–70. For standard omit the tactic and set `FINALE_HOLDOUT_SEEDS` to 6–70 (1–5 always run). Use `:app:testDebugUnitTest --rerun --tests '*CampaignEventIntegrationTest'`. Omit `FINALE_TRACE` to retain only the original acceptance budget. No release, commit or upload occurred.
