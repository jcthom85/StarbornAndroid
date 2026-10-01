# Cooking and craftable snacks

## Implemented rules

- 15 meal recipes and three recipes for existing equipped snacks: Mineral Trail Mix, Pulse Citrus, and Comet Gummies.
- Meals cooked at a station become inventory portions. Eating outside combat applies immediate healing to the item's stated target and a three-battle party Well-Fed bonus. Eating another meal requires replacement confirmation.
- Healing meals used in combat apply healing and their authored temporary buffs to the stated targets, without starting Well-Fed or applying a serving perk.
- Equipped snacks retain their existing passive equipment effects and reusable cooldown abilities. They cannot be consumed through inventory or the combat item list. Snack recipes produce one item, have no masterwork roll, and cannot be crafted while already owned.
- Ordinary buff consumables are used in combat and do not overwrite Well-Fed.
- Nova adds 3 Focus; Zeke adds 25 Max HP and 3 Stability; Gh0st adds 2 Speed and 10 percentage points of resistance to enemy-inflicted statuses; Orion adds 4 percentage points of Crit chance.
- Accuracy, evasion, and crit use combat's percentage-point convention. Status resistance now has a separate combat stat rather than elemental affinity.
- Victories and successful retreats spend one meal battle. Defeat follows existing recovery/checkpoint behavior.
- Healing uses PartyHealth, including equipment and passive vitality. Healing cannot lower existing HP.
- Spiced Ration Press produces two portions. Meals have one 15% extra-portion roll per batch unit; supported batch sizes are 1–5. Snacks are crafted individually.
- Regional recipes unlock at their associated cooking stations and remain unlocked through persisted room state. Existing owned foods remain usable without rediscovery.
- Cooking includes search, a Can cook filter, meal/snack categories, current meal information, serving perks, complete effect/cost previews, high contrast, and large targets.
- The Sandy Cove campfire and separate Source Campfire cookware open cooking. Existing Source story and rest interactions remain available.
- Food and crafted snack resale bases are explicitly bounded against their recipe materials. Purchase prices and snack abilities are unchanged.
- Saved meal data has a rules version. Legacy fractional crit bonuses migrate to percentage points once; current bonuses round-trip without conversion. Existing serving bonuses remain until the saved meal expires.

## Manual playtest

1. Cook a basic meal, confirm ingredient consumption and portion yield, then eat from Items. Verify healing scope, serving perk, and the party bonus.
2. Try replacing an active meal. Cancel and confirm that inventory and bonuses remain unchanged; accept and verify a fresh three-battle bonus.
3. Use Spicy Ramen or Glowfish Broth in combat. Check that both healing and the temporary stat effect apply to the intended ally. Well-Fed should not be replaced or refreshed.
4. Check meals with accuracy, evasion, and crit effects against their previews. Compare Orion's perk with the other companions.
5. With equipment or Zeke raising maximum HP, heal near the ceiling. Healing must respect the shared maximum and never reduce current HP.
6. Win a battle, flee a battle, and save/reload. Each completed victory or retreat spends one charge; the third expires Well-Fed. Check defeat recovery separately.
7. Attempt inventory consumption of an equipped snack and a stimulant. No item should be consumed and no meal overwritten. Use the snack through its combat ability, observe its cooldown, and confirm the item remains owned.
8. Discover each snack recipe, craft it, equip it, and use its ability. An owned snack should display as already owned and prevent unnecessary recrafting.
9. Discover the Stream Falls, ridge, Spire roof kitchen, Foundry break station, Orbital galley, and Source cookware. Check recipe unlocks and their save/reload persistence. Existing owned meals must still work before rediscovery.
10. Cook Spiced Ration Press at batch sizes 1 and 3: base yields are 2 and 6, with possible extra portions. Confirm scaled ingredient costs.
11. Check all pages with enlarged fonts, narrow phone width, high contrast, and large targets. The entire page should scroll and labels should wrap.
12. Check fishing catches' cooking uses and shared ingredients with tinkering. Confirm the three snack recipes provide tactical alternatives worth their material costs.

## Verification

On October 1, 2026, the full Android unit test suite compiled and passed: 622 tests, zero failures, errors, or skips. Focused system runs also passed (120 and 80 tests, with overlap). Nine new CookingRefinementTest cases cover meal replacement, snack boundaries, chef bonuses, equipment-aware healing, discovery, batch yields, combat food effects, hostile status resistance, and protected salvage.

The six initial full-suite failures were stale dialogue/quest flow, wording, and navigation harness assumptions. Tests now execute the authored puzzles and departure choice; the campaign runner traverses Worlds 1 through 6 with save/reload checkpoints.

Device playtesting remains outstanding, including layout, readability, recipe discovery feedback, and game feel. Logs: build/full-release-regression.log and build/full-release-regression-summary.txt.
