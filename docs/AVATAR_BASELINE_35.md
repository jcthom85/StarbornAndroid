# Avatar baseline: 35 seeds

2026-09-22, following release 1.3.51 (135).

The prepared campaign-earned Avatar fixture wins **18/35**, loses **17/35**, and has **zero timeouts**. Each seed ran twice with identical results. Original seeds 1–5 retain four victories; seeds 6–35 produce fourteen victories and sixteen defeats. The earlier 4/5 result overstated reliability for this fixed player policy.

Defeat seeds: 1, 7, 10, 11, 13, 14, 16, 17, 21, 23, 24, 25, 26, 27, 32, 33, 34.

The fixture uses the existing legal armor/AP/shop preparation and unchanged supplies and decisions. This is a deterministic policy comparison, not a measured human win rate. Production code and assets are unchanged. All CampaignEventIntegrationTest checks passed; passing here means deterministic measurement and campaign integration succeeded, not that every simulated fight was won.

Evidence: `test-results/avatar-baseline-35/campaign.xml`. Rows report elapsed time, remaining party HP, skill/support use and consumed supplies. Seed 32 lasts 159.5 seconds and consumes eight rations and five Medkit I; it should be investigated separately from the fast seed-1 collapse. Do not assume all seventeen losses share the seed-1 burst cause.

Reproduce with `AVATAR_BASELINE_SEEDS=1,2,...,35` and Gradle `:app:testDebugUnitTest --rerun --tests '*CampaignEventIntegrationTest'`. The environment variable changes only purchased Avatar fixture seeds; default coverage remains 1–5. Gradle requires `--rerun` when changing diagnostic environment variables.

Next: capture lethal-hit timing, critical flags, target sequences, player readiness and remaining supplies across losses before comparing recovery windows against a separate repeated-target restriction. The baseline alone does not establish which adjustment will help.
