# Overall plan checkpoint — September 23, 2026

**Read this first when resuming.** This supersedes older next-step lists in the campaign/Foundry documents. The world balance program is temporarily paused for BurgQuest booth preparation. BurgQuest build 1.3.54 is released to Google Play internal testing; see [the guided sampler record](BURGQUEST_GUIDED_SAMPLER.md). Actual-device/newcomer checks remain. The world-by-world position and combat resume point below are unchanged.

## Released versus local

- Released in 1.3.54: [BurgQuest guided sampler](BURGQUEST_GUIDED_SAMPLER.md), including crew homecoming, direct combat-to-Astra routing and the booth-only title menu. Human/booth checks remain; world/finale resume points below are unchanged.

- Last confirmed release: **1.3.54 (138)**, commit `2a613ba`, pushed to `feature/multiplatform-port`. Google Play accepted the bundle and confirmed it committed/live on internal testing on September 23, 2026.
- This release includes the guided sampler and earlier combat formation spacing, responsive 1-4 crew layouts, compact demo controls, Astra workbench repair, Avatar's post-barrage recovery, tests and campaign/finale diagnostics. New Game, Load Game and Debug Scenarios are temporarily hidden; restore their grouped title flags after BurgQuest. Story Opening starts the campaign opening in an isolated demo session.
- Finale stagger candidates are **test-only**. Production Reality Break still applies stagger. No every-other-cast candidate has been implemented yet.
- Release verification: **552 unit tests passed**, plus a 12-test broad emulator pass, 5-test targeted polish pass and 2-test compact/large-text pass, all offline. Earlier unchanged BurgQuest fixture simulations won 40/40; see the guided sampler/prep records for limitations and operator checks.
- Preserve unrelated untracked booth audio, generation scripts, `desktop.ini` and local evidence. Do not stage the entire workspace indiscriminately.

## World-by-world position

| World | Completed evidence/work | Remaining work |
| --- | --- | --- |
| 1 — Mines | Main-story events/rewards/save checks; selected Mining Depths consecutive-fight tests; Siren defense-loop regression | Wider builds and optional encounters; player-facing full-route playthrough |
| 2 — Sector 9 | Main-story/events/save checks; selected Undercity consecutive-fight tests; Ruin Guardian gate checked | Wider encounter/build coverage and sustained route resources |
| 3 — Spire | Story, rewards, transitions and save checks; contributes to earned Foundry checkpoint | Comparable encounter balance matrix and actual route combat endurance |
| 4 — Foundry | Largest combat matrix; status/AI fixes; Android smoke checks; latest 720-fight matrix: 627 wins, 93 defeats, zero timeouts; selected attrition checks pass; prepared Titan party wins 5/5 | Full arrival-to-Titan run carrying actual HP, supplies, purchases, rewards and rests; verify affordable builds; make Drone target priority understandable |
| 5 — Avatar | Expanded baseline: 34/70 wins. Compared recovery and target restriction. Selected barrage-only recovery implemented: 55/70 wins; deterministic repeats; recovery cue verified on emulator | Wider builds/route coverage; early casualties remain possible. This specific implementation is complete locally, not awaiting another baseline replay |
| 6 — Finale | Story/ending/save checks; Link fixed; opening/recovery cue implemented; standard policy 70/70 wins. Slow runs investigated: finite losses or delayed wins, no deadlock found in those samples | Next: test a less frequent finale-only stagger, then decide production behavior. Continue broader route/tactic coverage |

“Titan wins 5/5” means the **player party defeated Titan in all five tested seeds**. It does not certify the BurgQuest Titan fixture, which currently uses a different debug loadout.

Scripted campaign traversal applies production events/rewards but assumes battle victories. It does not prove route survival or human win rates. Fresh-fight level-9 Foundry fixtures do not replace earned arrival level 7/pre-Titan level 8 resource validation. No world is claimed to have exhaustive manual QA.

## Exact combat resume point after BurgQuest

1. **Finale stagger frequency.** Added `FINALE_PACING` and phase-two-only `FINALE_NO_STAGGER` diagnostics. Removing Reality Break's stagger in the test fixture gave:

   | Policy, 70 seeds | Baseline wins / defeats / timeouts | No-stagger candidate | Mean capped phase-two duration |
   | --- | --- | --- | --- |
   | Standard | 70 / 0 / 0 | 70 / 0 / 0 | 63.55s → 47.75s |
   | Control | 61 / 5 / 4 | 68 / 0 / 2 | 155.75s → 111.29s |
   | No Venom | 66 / 1 / 3 | 69 / 0 / 1 | 166.21s → 116.95s |

   Next proposed comparison: stagger on **every other Reality Break**, scoped to the finale. Preserve damage, HP, cooldowns, recovery and two-phase carryover. Keep the four-minute acceptance cap; label extended diagnostic outcomes separately. Candidate timeouts (control 32/37, no-Venom 63) have not yet been extended. No production choice approved from removal alone.
2. **Foundry full-route resources and target-priority clarity.** Individual fights are extensively tested; actual route affordability/endurance remains the major gap.
3. Expand Worlds 1–3 and 5 encounter/build coverage, then player-facing routes, optional content and save/resume checks across all worlds. Avoid repeating completed matrices unless a relevant change warrants it.

Evidence and detail: [Avatar implementation](AVATAR_RECOVERY_IMPLEMENTATION.md), [Avatar candidate comparison](AVATAR_CANDIDATE_COMPARISON.md), [finale pacing](FINALE_PACING_DIAGNOSTICS.md), [finale stagger comparison](FINALE_STAGGER_COMPARISON.md), [earlier diagnostics and Foundry matrix](REMAINING_BOSS_DIAGNOSTICS.md).

## Immediate detour: BurgQuest prep

Event September 25–27. The [readiness review](BURGQUEST_READINESS_REVIEW.md) is the historical pre-implementation assessment; current results are in the [prep record](BURGQUEST_PREP_IMPLEMENTATION.md).

1. Done locally: four-person Tactical encounter, deterministic booth loadouts and limited supplies, exact Tactical/Titan simulation (40/40 victories), briefings and Titan recovery guidance.
2. Done locally: Astra route guidance, usable stocked workbench, all six cabinet launches verified.
3. Done locally: isolated demo saves, fresh retry, choose-another/title exits, terminal arcade cleanup, guide pause, and end-to-end offline visitor flow.
4. Done locally: public BurgQuest labels and honest Story-preview description; BurgQuest Demo and Debug Scenarios retained, New Game hidden.
5. Remaining operator check: time Tactical -> Astra with a newcomer on the actual booth device; check audio, readability, brightness and charging. Automated combat timing is not a certified 5–10 minute human visit.
6. Prepare a release when authorized; no new version/commit/upload has occurred since 1.3.51.

The user authorized the focused prep pass after this checkpoint was recorded. Follow the current [prep implementation/verification record](BURGQUEST_PREP_IMPLEMENTATION.md) rather than restarting the review. Return to the combat resume point above after BurgQuest preparation; no new release is implied by this checkpoint.
