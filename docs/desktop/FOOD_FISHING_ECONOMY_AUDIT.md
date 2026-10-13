# Automated food and fishing economy audit

The subsequent [campaign availability audit](CAMPAIGN_SYSTEMS_ROUTE_AUDIT.md)
checks progression states and unlocked return travel, and fixes a missing
ingredient source.

This follow-up adds reusable evidence tests. It changes no gameplay rules or
authored balance values. The +40 HP Trail Mix trial remains in place.

## Combat method

`FoodAtbBalanceAuditTest` builds combatants with the production controller,
equipment, meal rules and skill catalog. It calls the production enemy selector
and records enemy action history. A deterministic 50 ms scheduling model uses
the controller's effective-speed calculation, ATB scale, initial-meter ranges,
break slowdown and ready-queue ordering. Action resolution uses the real engine
and processor. Seed zero is replayed to check reproducibility.

The player policy targets low-HP enemies, heals the most injured living ally
below 40% HP, and uses available unconditional attack skills. Snack activation
and passive-only variants expose the cost of spending a turn on buffs.

The report contains 1,200 trials: 12 provision variants, two encounter fixtures
and 50 seeds. Fixtures use authored character profiles, default gear and skills;
requested session levels alone do not alter these combat profiles. This is not
a comparison of leveled campaign builds. The scheduler excludes animation time,
human input delay and controller-specific support/boost mechanics. Trials start
at full HP, so immediate meal healing has no benefit here.

Results: `desktopApp/build/reports/food-atb-balance.csv`.

In the boss fixture, baseline wins were 47/50, activated +40 Trail Mix 48/50,
and activated +120 Trail Mix 50/50. Comet Gummies won 47/50 with passive bonuses
alone and 36/50 when repeatedly activated. Pulse Citrus passive bonuses shortened
modeled combat from 81.74 to 74.14 seconds with the same 47/50 wins. These are
policy-specific observations from small matched samples, not global food ranks.

## Recipe acquisition method

`FishingEconomyAuditTest` exercises production catch weighting, encounter
selection, fight timing, catch securing and mastery rewards. It measures 39
recipes with catchable ingredients across starter/upgraded gear and
mastery-disabled/enabled cases: 156 scenarios and 7,800 seeded acquisition runs.

Each run begins with an empty catch inventory and zero mastery progress. It
assumes all zones, selected gear and ingredients without a fishing source are
available. It selects the best zone/lure probability for the next missing
ingredient. An attentive policy reels outside warning/surge phases while keeping
tension below 0.65. Time includes the authored 1.8–3.5 second bite wait and reel
time; it excludes hook reaction, travel, menus, selling and recipe discovery.

The report separates native fish from salvage. Scrap, wiring and other salvage
can also come from exploration; fishing-only repair estimates are optional
acquisition costs, not required campaign grind.

Results: `desktopApp/build/reports/fishing-economy.csv`.

With starter gear and mastery enabled, the fishing portion of Glowfish Broth
averages 30 seconds (90th percentile 45 seconds); Trail Mix averages 32 seconds
(90th percentile 63 seconds). Frequency Tetra Consomme averages 106 seconds
(90th percentile 190 seconds), falling to 62/102 seconds with upgraded gear.
These measurements do not include gathering herbs or other non-catch materials.

## Income method and findings

The income audit runs 1,000 casts for each of six zones, two rods and two mastery
cases: 24,000 casts. It uses the starter lure and values secured catches at the
best eligible authored shop resale price, following shop type/blacklist rules,
unsellable flags, markdowns and integer rounding. Collection unlock rewards are
excluded. Shop accessibility and travel time are not modeled, so this is gross
income potential, not a guaranteed route or net earnings.

Results: `desktopApp/build/reports/fishing-income.csv`.

The starter-rod stream scenario increases from about 48 to 64 credits per fishing
minute with mastery. The best-rod ether-well scenario increases from about 348
to 468. Native-fish copies increase by approximately one third under this clean
catch policy; salvage does not receive the bonus. This identifies late-game
fishing as an economy pressure point to compare against actual shop costs and
legal progression routes before changing prices.

## Decisions

- Retain Trail Mix at +40 HP. The stronger audit supports a smaller useful HP
  cushion; it does not justify reverting to +120 or declaring final balance.
- Retain fishing mastery and catch weights. Early fish-dependent recipe costs
  do not show a clear bottleneck under attentive play.
- Treat snack activation as a policy-sensitive tradeoff. Compare passive and
  activated results rather than assuming every ready ability should be used.
- Next automated extension: campaign-valid gear/recipe discovery routes and
  locally reachable shop income comparisons. Physical input feel and audible
  playback still require hands-on testing.

Reproduce with `gradlew.bat :desktopApp:test --tests '*FoodAtbBalanceAuditTest'
--tests '*FishingEconomyAuditTest'` using the project's configured JDK.

Both audit tests passed in the final verification run. No application code or
assets changed in this follow-up; no new portable package was necessary.
