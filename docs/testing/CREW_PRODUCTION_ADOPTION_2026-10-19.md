# Starborn crew progression adoption

October 19, 2026. The owner approved proceeding with the recommended crew-system adoption. This implements the existing two-path/four-purchase structure; it does not establish final human balance or presentation acceptance.

## Production behavior

- Android and desktop New Game use crew progression version 1. The persisted field keeps its existing name, `progressionPrototypeVersion`, to preserve save compatibility; decoding defaults remain 0 to identify old saves.
- Normal player save loads migrate legacy builds. The shared bootstrap also migrates a legacy completed game before starting NG+.
- Version 1 exposes exactly the four playable crew trees and 32 purchases. Legacy asset trees remain available to historical fixtures and compatibility tests, but are not the version-1 purchase catalog.
- Existing prototype saves keep their purchases, AP and award claims. Unknown future versions are not downgraded.
- Core abilities remain earned separately from AP purchases. Free per-character resets remain available outside pending battles.
- Existing ability values and the measured encounter tempo profile are retained. No blanket protection/healing/damage buffs are justified by the existing evidence.

## Legacy compensation

The legacy system did not record paid purchases. The adopted design-packet policy grants a **one-time respec credit**, not an exact refund, using frozen historical costs for recorded legacy unlocks. It intentionally includes ambiguous free grants; the maximum catalog credit is 224 AP. This is compatibility compensation, not the fresh-game budget.

Migration preserves unused AP, learned combat abilities, schematics, story progress, inventory and currency. It removes only retired non-combat tree flags. A historical ability sharing a tree ID remains learned without also retaining the old tree's accidental passive.

New quest AP is backfilled only for completed quests. Orion/Gh0st recruitment AP requires roster or completed recruitment evidence; temporary party separation does not erase proven recruitment. Existing boss AP is never backfilled. Field Patch is retained/granted for proven Orion recruitment without adding him back to a temporarily separated party.

The version marker and award claims make repeated migration a no-op. Battle rollback snapshots migrate independently so saving during a fight cannot persist an unpaid/unmigrated checkpoint. NG+ keeps the adopted build and compensation, resets campaign award claims, and never repeats the legacy credit.

## Budget and build choices

Each path costs 10 AP, both paths for one character cost 20, and the entire crew costs 80. The subsequent World 1 route review established that `w1_sq03` is required certification for mine access, despite its side-quest ID. Including its 2 AP, the core campaign budget floor is **8/16/26/36/44/50** across Worlds 1–6 before the finale. The five remaining AP-paying side quests add 10; their optionality beyond World 1 was not re-audited in this pass. This corrects the earlier classification of 6/14/24/34/42/48 plus 12 "optional" AP; no in-game award changed. Fourth nodes require World 4 completion. The budget regression buys concrete allocations through public purchase rules at each milestone; it does not pretend arithmetic proves route reachability.

## Verification scope

New tests cover the frozen compatibility table, preservation of core skills, evidence-based backfill, repeated migration, existing prototype receipts, unknown future versions, AP overflow, pending-battle rollback, manual/auto/quick saves, corrupt-primary backup recovery, NG+, fresh defaults and affordable campaign allocations. Desktop adapter tests cover every player slot including auto/quick saves.

A separate production-crew controller route starts without seeded crafting materials and attempts all six worlds, real combat, world-boundary reloads and credits. The older prepared/historical route remains explicitly version 0. This controller route uses authored puzzle answers and skips cinematics; it does not replace Android controls, subjective discovery or a human campaign playthrough.

Fresh-seed protection, Break/setup, precision and unspent comparisons use the existing real-ATB experiment. Experiment environment variables are Gradle test inputs so changing seeds or parameters cannot silently reuse an up-to-date result. Reports must name the seed list and distinguish prepared encounter matrices from continuous routes.

### Execution results

The subsequent [pre-World 1 Android pass](PRE_WORLD1_READINESS_2026-10-19.md) passed four isolated device checks plus two repeated UI checks at doubled text size, including a copied historical save. This adds Android UI/service evidence; it does not resolve the continuous World 6 route failure below.

- Focused progression/combat checks: **37 tests passed**, including migration, AP pacing, crew talents and encounter tempo.
- Full Android-hosted unit suite: **699 tests, 698 passed, one failed** on the first production campaign attempt. The failure was defeat at World 5's orbital executive dock. Investigation found the controller route's crew healing policy was gated to prototype runs, so the production driver ignored available Field Patch/Field Treatment. Enabling that policy cleared World 5 but lost at World 6's Endless War Echo. Spending 10 remaining earned AP on Nova's Saboteur path through the public menu then cleared the Echo and Silent Shore Wraith, but **the final targeted rerun still failed at `source_memory_fragments`**. Combat numbers, encounters, rewards and available supplies were not changed to make this pass. The complete suite was not rerun after these driver-only adjustments; its other 698 results are from the preceding run.
- Targeted desktop checks: **6 tests passed** across `DesktopCrewBuildTest`, `DesktopCrewProgressionMigrationTest` and `DesktopNovaPrototypeTest`. These cover public tree purchase/reset behavior, combat integration, save/load and migration.
- Fresh encounter comparison: **30 seeds (211–240), 3,240 independent fights and 680 attrition fights**, with zero rejected commands and zero unresolved fights. See [retained aggregate evidence](evidence/crew-adoption-2026-10-19.json). This report captures source hashes at experiment execution; subsequent campaign-driver edits do not rerun or alter that experiment.

The late tactical executive-dock results were 29/30 victories for Break/setup, 29/30 precision, 28/30 protection and 27/30 unspent. Source-memory results were 29/30, 28/30, 26/30 and 27/30 respectively. Protection's value remains uneven; these samples do not establish statistical superiority or justify automatic buffs.

Local execution logs are `tmp/crew-adoption-focused.log`, `tmp/crew-adoption-regression.log` and `tmp/crew-adoption-route.log`. The full-suite log retains the original failure even after a targeted rerun replaces the Gradle XML results.

### Open campaign acceptance gate

`LegalRouteCampaignRunnerTest.productionCrewCampaignEarnsItsOpeningSuppliesAndReachesCredits` remains a failing regression. The final run used combat seed 17, preserved world-boundary save/load parity through World 5, and reached World 6's `source_memory_fragments` encounter (`source_shadow`, `distorted_sentinel`, `glitch_hound`). By that fight the route had no healing consumables left. It retained the starter mining pistol on Nova and starting weapons on the other crew, with Orion wearing `heat_liner`. This is a failed continuous route with a specific equipment/resource policy, not proof that the encounter is impossible or that every crew build fails. Prepared checkpoint victories cannot substitute for this result.

Before accepting the rollout as campaign-ready, inspect the route's equipment opportunities and earned purchases, recovery use and action priorities against the actual player experience. Reproduce from an earned checkpoint with a human. Then make an evidence-backed route or balance correction and rerun this test through both final bosses, credits and final save/reload. Do not seed additional money/items, change the seed to obtain a pass, weaken the victory assertion or report the production campaign as green. The test currently purchases a mostly protective/support allocation in World 5 and Nova's Saboteur path in World 6; it does not validate other allocations through the full campaign.

## Remaining human acceptance

Play protection, Break/setup and precision from the same earned checkpoint. Record AP allocation, actions, consumables, knockouts, encounter duration and whether a purchase changes a decision. In particular, judge whether Cover is worth an ATB action, whether stability bonuses are visible, and whether all four characters receive satisfying upgrade opportunities. Review the post-World-4 capstone timing and the first AP tutorial on a physical phone.

Automated victories do not establish that these choices are enjoyable. Final ability tuning remains open; make targeted changes from the recorded comparisons and human feedback rather than enlarging the trees by default.
