# Finale pacing diagnostics — 2026-09-22

Production balance unchanged in this pass. The existing earned two-phase checkpoint and phase carryover remain intact. Added opt-in `FINALE_PACING=1` runtime sampling; the original four-minute cap stays unchanged. Longer diagnostic runs retain their separate labels.

## Phase-two measurements

| Policy / seed | Outcome with extended budget | Duration | Animation-pending samples | Player stagger skips | Boss recovery actions |
| --- | --- | ---: | ---: | ---: | ---: |
| Standard / 21 | Win within normal cap | 57s | 2.50s | 4 | 5 |
| Standard / 27 | Win within normal cap | 93.25s | 4.00s | 8 | 6 |
| Control / 21 | Win after original timeout | 260.50s | 7.50s | 19 | 23 |
| Control / 27 | Win after original timeout | 251s | 8.50s | 19 | 20 |
| No Venom / 9 | Defeat after original timeout | 293.25s | 7.25s | 21 | 24 |
| No Venom / 27 | Defeat after original timeout | 271.75s | 7.50s | 22 | 21 |

The sampled pending-lunge portion is approximately 2.5–3.4% of the slow fights. Shortening that portion alone cannot bring these fights reliably under four minutes. The real emulator may have different animation timing: the headless harness acknowledges lunges on a 250ms tick, and these figures are not a UI performance benchmark.

Player stagger skips are distinct from enemy stun/freeze skips. Control seed 21 has six enemy stun skips and one freeze skip; control seed 27 has one stun skip and eight freeze skips. Repeated player loss of actions, plus low offensive output and casualties, is a more useful investigation direction than simply speeding animations. Longer fights also naturally accumulate more stagger events; this observation does not by itself prove stagger is the primary cause.

## What the samples do and do not mean

Buckets are exclusive, sampled at the start of each 250ms interval: pending attack animation, pending player input, then other runtime. No player-input-pending interval appeared because the policy responds immediately. `otherRuntime` includes ATB charging, status handling and other resolution; it must not be relabeled as pure idle time. Exact wait-versus-resolution attribution remains open. Action and skip counts come from the production combat log.

Each comparison ran twice deterministically, including the extended slow outcomes. Campaign integration tests passed. Evidence is in `test-results/finale-pacing/{standard,control,no-venom}.xml`. No additional full suite was required for this opt-in observation-only change.

## Next bounded comparison

Test a finale-specific reduction in stagger frequency while keeping damage, HP, cooldowns and recovery windows fixed. Compare standard, control and no-Venom across original and fresh seeds; report survival, casualties, skipped turns and duration. Do not modify the global stagger status or increase the acceptance cap merely to pass. Preserve two-phase carryover and the current loss outcomes as the baseline. Select an actual production change only after this causal comparison.
