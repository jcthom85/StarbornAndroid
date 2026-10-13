# Snack ability trials and public procurement routes

The latest [Worlds 1-2 boss pass](PUBLIC_CAMPAIGN_BOSSES.md) strengthens
the setup used by this procurement slice; no synthetic victories remain
before World 3.

The follow-up [combat clarity pass](COMBAT_CLARITY_PASS.md) makes ability costs,
effects and remaining turns clearer on both platforms.

## Implemented trials

| Snack | Previous ability | Current trial |
| --- | --- | --- |
| Pulse Citrus | +8 Speed for 2 personal turns | +16 Speed for 3 personal turns |
| Comet Gummies | +8 accuracy / +10 crit chance for 2 personal turns | +8 accuracy / +30 weapon crit chance for 4 personal turns |

Both abilities retain the five-turn cooldown, their equipped passive bonuses,
recipes, prices and discovery requirements. Trail Mix remains +40 equipped HP
with its existing +6 defense ability for two turns. Using any snack ability
spends an action and does not consume the snack.

Crafting descriptions now state the action cost and tactical purpose. Pulse
Citrus is intended for a longer fight where faster upcoming turns can repay the
setup action. Comet Gummies is intended before basic weapon attacks. The generic
skill-damage path uses momentum and forced-crit rules rather than normal weapon
crit rolls, so a weapon-crit buff is not a universal skill-damage buff. Trail Mix
guidance distinguishes its always-on equipped HP from its defensive activation.

## Automated comparison

`SnackAbilityAuditTest` compares passive, repeated-ready, opening and situational
activation policies, plus explicit legacy-stat controls and candidate effects.
It also tests a weapon-only policy. Three fixtures cover a single skimmer, two
skimmers and the adapted Beast encounter, with 50 matched seeds per variant.
The audit records wins, actions, remaining HP, healing use, modeled ATB time and
activation count in `desktopApp/build/reports/snack-ability-audit.csv`.
The final report contains 27 variants and 4,050 recorded trials. Explicit legacy
controls reproduce the previous ability values after the production trial is
changed.

Opening uses are limited to encounters with sufficient remaining enemy HP;
short encounters can correctly avoid spending a setup turn. Situational uses
look for incoming pressure, impaired accuracy or defensive enemy windows.
Those checks are simple decision policies, not an optimal player model.

The pre-change skill-focused boss policy won 47/50 with passive Comet Gummies,
36/50 with repeated activation and 48/50 with one opening use. Increasing weapon
crit did little for skill-heavy turns. In the two-skimmer weapon policy, the
old repeated ability averaged 38.29 modeled seconds versus 35.37 for passive
use; the +30/four-turn candidate averaged 34.59. The +16/three-turn Pulse opening
candidate averaged 72.69 seconds in the boss fixture versus 74.14 for passive
use. These are trial-selection observations, not a global ranking or final
balance verdict. The regenerated report includes explicit legacy controls.

The scheduler uses production enemy AI, combat resolution and effective-speed
calculation with immediate player decisions. It excludes input and animation
delays, some controller support mechanics and human reactions. Character
profiles use default gear and skills rather than fully leveled campaign builds.

`SnackAbilityContractTest` separately checks that activation retains the full
advertised buff duration, enemy turns do not shorten it, personal turns expire
it, cooldown blocks early reuse, reactivation works and inventory is preserved.
Matched weapon-hit trials verify that the Comet crit stat actually affects
eligible normal weapon attacks.

## Public route

The campaign test first establishes the existing assisted end-of-World-2
checkpoint. The procurement slice then uses public controller methods only:

1. Return through the hub, board the Astra and navigate to World 2.
2. Walk to David's Provisions and buy the Resin Rod base with existing credits.
3. Return on the Astra to the Spire and enter its runoff fishing location.
4. Play the real waiting, hook and reel phases until both upgrade components
   are secured, using starter fishing gear.
5. Auto-fill and craft the Reinforced Resin Rod through the Field Kit.
6. Return to World 2 and sell caught fish, reserving two Glowfish for cooking.
7. Buy peppers and herbs from Sentinel Scraps, visit the ridge cookfire and
   cook Pulse Citrus through the same service operation as the cooking UI.
8. Save/reload and verify the crafted gear, snack and inventory state survive.

The slice asserts that it adds no room-position shortcuts, reflective actions
or scripted victories. Purchases and sales verify exact credit and inventory
changes; the upgrade consumes its base and preserves starter gear. Cooking
discovery comes from selecting the actual public rest-stop action, without
manually setting its flag.

The trace is written to
`app/build/reports/campaign-systems/public-procurement-route.txt`. Simulated time
includes authored fishing and Astra transit delays. Travel and menu operations
run immediately in the harness, so this is not measured human farming time or
a rendered UI playthrough. The initial campaign checkpoint still uses the
existing assisted runner; the public slice does not eliminate those earlier
campaign shortcuts.

The final seeded route obtained components in 41 casts, banked 588 credits from
the catches while reserving cooking ingredients, crafted both the rod upgrade
and Pulse Citrus, and preserved them through save/reload. Its simulated elapsed
time was 385,143 ms, including three 1.4-second Astra transits. This is one
executed procurement sample, not a latency measurement or expected farming rate.

Validation passed 77 selected Android tests, seven desktop audit/flow tests and
28 shared-runtime tests. The refreshed Windows portable package passed from the
temporary directory, including title-music decoding and all 1,328 assets.
