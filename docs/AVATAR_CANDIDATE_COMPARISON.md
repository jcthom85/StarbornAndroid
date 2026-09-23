# Avatar candidate comparison — 2026-09-22

Implementation follow-up: the selected barrage recovery is now authored in production assets as `avatar_recalibration`. The table below is historical pre-implementation evidence. Default runs now exercise the selected fix; do not rerun the old no-repeat flag against these assets as an isolated target-only comparison, because production already includes recovery. Use the preserved XML for those original comparisons.

Test-only comparison on the prepared campaign-earned fixture. Each seed runs twice with identical results. Seeds 1–35 were previously examined; 36–70 are fresh for this comparison. Production balance remains unchanged.

| Candidate | Wins, seeds 1–35 | Wins, seeds 36–70 | Total wins | Character deaths | Mean fight duration |
| --- | ---: | ---: | ---: | ---: | ---: |
| Current game | 18 | 16 | 34/70 | 161 | 65.30s |
| Recovery after Missile Barrage | 29 | 26 | 55/70 | 79 | 61.38s |
| No consecutive single-target attacks on same living player | 23 | 27 | 50/70 | 86 | 73.95s |

All fights terminate within the existing four-minute cap. Means include both wins and losses; they are not victory-only timing estimates. These are results for one automated player policy, not human win-rate estimates. The one-win difference on fresh seeds between the candidates is not compelling evidence of superiority.

## Mechanisms and validation

The barrage candidate borrows the harmless `gathering_silence` action after Missile Barrage only. It does not add an opening pause or modify stats. It is narrower than the earlier recovery-after-every-special experiment (34/35 original-seed wins).

The target candidate preserves the selected action but redirects a repeated single target to the living alternative with highest current HP. It permits repetition when only one player survives and leaves self-targets and multi-target actions alone. Consequently this tests both a repeat prohibition and a specific replacement-target rule. It is not a clean measurement of only a repeat penalty. The previous target comes from logged actions; a preliminary harness incorrectly counted choices on frozen/skipped turns and was corrected before final measurement. Final traces contain 1,044 observed Avatar single-target attacks and zero prohibited repeats while alternatives survive.

Both candidates use the same gear, supplies, purchases, player policy and seeds. Changed action/target choices affect subsequent random draws and player responses; seed matching does not hold later damage rolls identical. Candidates are never enabled together.

## Recommendation

Prefer the Missile Barrage recovery window for a production proposal: it improves overall survival, halves casualties in this sample, and preserves shorter pacing while giving the player an identifiable opening. The target limiter is also viable but takes longer and adds hidden targeting behavior; fresh-seed victory counts alone do not distinguish them convincingly.

Before implementation, specify Avatar-specific recovery text/visuals and verify that the opening is visible on the emulator. Keep the unresolved opening-pressure case in acceptance; a recovery window does not guarantee protection before its trigger. Do not combine both candidates or promote the broader all-special recovery solely to maximize wins. Production implementation remains a separate decision.

## Reproduction

Evidence: `test-results/avatar-candidates-70/{baseline,barrage,no-repeat}.xml`. Campaign integration checks and deterministic repeats pass for all three final runs.

Set `AVATAR_BASELINE_SEEDS` to comma-separated 1 through 70 and `AVATAR_TRACE=1`. For barrage set `AVATAR_RECOVERY=barrage`; for target switching set `AVATAR_NO_REPEAT=1`; for baseline omit both. Run `:app:testDebugUnitTest --rerun --tests '*CampaignEventIntegrationTest'`. Defaults retain the original five seeds. `AVATAR_RECOVERY=1` retains the older broad candidate for reproduction.
