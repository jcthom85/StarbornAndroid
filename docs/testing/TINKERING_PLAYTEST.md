# Tinkering refinement playtest

## Implementation

- The exploration field menu has updated Workbench, Schematics, and Scrap pages. The classic menu layout remains available through its existing setting.
- The modern pages use shared menu typography, flexible card heights, full-width actions, large-target sizing, and high-contrast text.
- The workbench offers recipe base items and components compatible with the selected base. Ordinary inventory clutter is excluded.
- Result previews show actual item benefits, mod stats, yield, consumed material quantities, reusable tools, and quantities available after reserving equipped items.
- Simple recipes remain discoverable. Source Resin, Thermal Cutter Repair, and Rapid Capacitor require their awarded schematics. Found schematics can be learned directly in the tinkering menu or through Inventory.
- Craft/scrap feedback remains visible until dismissed. Sounds follow successful operations.
- Power Lens Mk. I (+2 Focus), Ergonomic Grip (+2 Accuracy), Insulated Lining (+1 Defense, +1 Vitality), and Armor Plating Mk. I (+2 Defense) are equippable mods. Armor Plating remains an input to Nanite Plating.
- Salvage returns are explicit per item. Quest/unsellable items and equipped gear cannot be scrapped. Equipped copies are reserved when crafting.
- The two component slots retain their positions when one is cleared.

## Manual checks

1. Start the World 1 workshop briefing and open Tinkering through the exploration menu. Follow the three-step Cryo-Inductor guide. Confirm the recipe consumes one damaged inductor and one Scrap Metal, grants the functional inductor, unlocks Cryo Vent, and advances the quest.
2. Before the live cutter test, check Scrap: the functional inductor must be absent. Finish the cutter test normally.
3. Build each early mod. Check its preview stats, find it in the gear picker, equip it, and check the party/combat benefit. Equipped copies must not be eligible for scrapping or consuming as crafting inputs.
4. For Armor Plating, unequip it before upgrading to Nanite Plating. Check that the available requirement count updates and that the upgrade consumes the correct quantities.
5. Choose a base with one component, then one with two components. Clear Component 1 while Component 2 is filled; Component 2 must stay in its original socket. After a successful build, the next selection must start at the base socket.
6. Check the Magnetic Salvage Lure preview: two Scrap Metal and one Wiring Bundle are consumed; the Basic Lure is a reusable tool and remains in inventory.
7. In World 2, obtain each awarded schematic. Before learning it, its advanced recipe cannot be built. Learn it from the menu's Found Schematics card, then build normally. Confirm quest progression and save/reload persistence.
8. Confirm repaired projector/grinder/display, arcade cores, and the Thermal Cutter are absent from Scrap. The Thermal Cutter must still open its intended vine routes.
9. Scrap an unequipped ordinary mod. Confirm the dialog and inventory agree on exactly one consumed item and the listed salvage, rather than a full recipe refund. A failed action must leave materials intact.
10. Search learned schematics, view discovery clues, build directly, and load a recipe into the workbench. Read success/failure feedback and dismiss it.
11. Check the narrowest supported phone size, increased system font size, high contrast, and large targets. Names, requirements, tutorial instructions, and action labels should wrap without clipping. Verify scrolling across all three pages.
12. Toggle the classic field menu and confirm its existing bench remains usable with the same recipe and salvage rules.

## Verification status

On October 1, 2026, the full Android unit test suite compiled and passed: 622 tests, zero failures, errors, or skips. Focused system runs also passed (120 and 80 tests, with overlap). Nine new CookingRefinementTest cases cover meal replacement, snack boundaries, chef bonuses, equipment-aware healing, discovery, batch yields, combat food effects, hostile status resistance, and protected salvage.

The six initial full-suite failures were stale dialogue/quest flow, wording, and navigation harness assumptions. Tests now execute the authored puzzles and departure choice; the campaign runner traverses Worlds 1 through 6 with save/reload checkpoints.

Device playtesting remains outstanding, including layout, readability, recipe discovery feedback, and game feel. Logs: build/full-release-regression.log and build/full-release-regression-summary.txt.
