# Remaining boss diagnostics — 2026-09-22

No production code, balance assets or main-menu behavior changed in this pass. Test-only diagnostics record longer outcomes separately; the four-minute acceptance cap is unchanged.

## World 5: Avatar seed 1

The prepared shop/AP checkpoint still loses at **47.75 seconds**, with the boss at **310/780 HP**. The trace shows repeated attacks on Orion, then a **122-damage critical basic attack against Nova**. The party consumes one ration, but retains nine rations, one medkit and five Medkit I at defeat. This is burst pressure/action availability, not exhausted inventory or a deadlock. Erosion already contributes 234 of the 470 damage dealt to Avatar; lack of erosion is not this failure's explanation.

A separate, test-only early-triage policy prioritizes single-target medkits below 65% HP instead of the normal 40% threshold and group-heal ordering. Across the five prepared-build seeds it wins **1/5**, versus **4/5** under the default policy. Seed 1 still loses, at 70.75 seconds. More proactive healing consumes attack/control opportunities and does not establish a fix. Keep this diagnostic out of default acceptance.

Recommendation: examine Avatar's speed, critical burst and repeated-target pressure together. Test a bounded recovery/telegraph or repeated-target restriction before a broad HP/damage nerf, and compare all five seeds plus new seeds. Do not infer from one seed that every loss is unacceptable; establish the intended difficulty and viable responses.

## World 6: timeouts versus actual stalls

Every previously recorded alternate-policy timeout was replayed with its original four-minute result retained, then repeated under a separately labelled sixteen-minute diagnostic budget. All extended runs terminated deterministically; no combat deadlock was found.

| Policy | Four-minute timeouts examined | Extended outcome |
| --- | ---: | --- |
| Direct damage | 12 | 9 wins, 3 defeats |
| Control-first | 2 | Both win: seed 21 at 260.50s; seed 27 at 251.00s |
| Standard without Venom Edge | 2 | Both lose: seed 9 at 293.25s; seed 27 at 271.75s |
| Direct plus Venom Edge | 1 | Seed 19 loses at 262.75s |

The ordinary no-Venom defeat (seed 21) also reproduces. In direct-plus-Venom seed 19, Gh0st falls during phase one, reducing the team's phase-two readiness despite access to Venom Edge. Non-Venom extended defeats leave the boss at 31 HP (seed 9) and 134 HP (seed 27): these are finite attrition losses, not infinite invulnerability. The control-first timeouts instead expose an acceptance-duration mismatch for a slower but viable strategy.

Recommendation: separate **runtime liveness**, **fight pacing** and **victory rates** in future acceptance. Retain the current cap while reviewing whether roughly four-minute control wins are acceptable. Do not raise the cap merely to turn the report green, and do not globally nerf erosion based on these comparisons. If pacing must improve, investigate reduced player downtime and clarity of offensive response windows before additional blanket damage changes.

## Test-scope correction

`FINALE_TACTIC` previously also affected the prepared Avatar policy because both used the same helper flag. It is now explicitly limited to `ascended_vale` and `ascended_god`. Earlier finale outcomes are unaffected: the final checkpoint is built by the separate scripted campaign, not by the Avatar diagnostic's mutated session. Use `avatar-and-direct-venom.xml` for the fresh default Avatar trace, not the Avatar traces in the earlier no-Venom/control diagnostic files.

`FINALE_DIAGNOSTIC_SEEDS` restricts only optional tactic diagnostics. `FINALE_TRACE=1` prints starting/ending combat inventory and state and runs a separate longer diagnostic for timeouts. `AVATAR_EARLY_TRIAGE=1` enables the comparison above. Defaults and original victory/time-limit assertions remain unchanged. Use `--rerun` when changing these environment switches; clear them before the default suite.

Evidence: `test-results/remaining-boss-diagnostics/` contains `no-venom.xml`, `control.xml`, `direct.xml`, `avatar-and-direct-venom.xml` and `avatar-early-triage.xml`. Reports echo some rows; count unique policy/seed results.

## Foundry refresh

Current mechanics, seeds 41–70, level-9 starter/purchased two-AP fixtures, support-first/drone-first, six enemy parties: **720 terminal fights, 627 victories, 93 defeats, zero timeouts**. All three World 1/2/4 consecutive-fight tests pass. Evidence: `test-results/foundry-current-30-seed/` contains only the current summary's fixtures/traces plus the attrition test XML.

Mixed-party wins out of 30, support-first / drone-first:

| Party | Starter + 2 AP | Purchased + 2 AP |
| --- | --- | --- |
| Welder + Drone | 23 / 29 | 17 / 22 |
| Golem + Welder | 30 / 30 | 28 / 26 |
| Golem + Drone | 4 / 30 | 0 / 28 |

All single-enemy groups win 30/30 in both builds/policies. Golem + Drone remains highly target-order sensitive; the evidence supports teaching target priority, not declaring the pair universally fair from its best policy. These fresh-fight fixtures are not campaign-earned arrival inventories or proof of full-route affordability. The consecutive-fight tests are selected scenarios, not exhaustive route traversal. Full player-driven route attrition remains open.

No release, version bump, commit or upload was performed. BurgQuest Demo is untouched.
