# Inventory & Crafting Port Plan

## Source Systems (Python)
- `items.json` (core item definitions), `shops.json`, `crafting_manager.py`, `ui/bag_screen.py`, `ui/shop_screen.py`, `ui/tinkering_screen.py`, `ui/inventory` widgets.
- Inventory logic resides in `game.py` (Bag, Item usage), `game_objects.Bag`, and interactions via events (actions like `give_item`, `set_room_state`).
- Crafting managers cover recipes, station unlocks, UI flows, and reward distribution.

## Kotlin Architecture
1. **Data Models**
   - `Item`, `Equipment`, `RewardItem`, `Recipe` (tinkering), `ShopInventory` with Moshi adapters.
   - Map Python structures to Kotlin data classes; normalize categories (weapon, armor, consumable, key item, crafting material).
   - **EquipSlot Enum:** WEAPON, ARMOR, ACCESSORY, SNACK.
   - **Equipment Model:** Include `sockets: List<ModItem>` (Max 2) to support modding.

2. **Repositories & Services**
   - `ItemRepository`: loads `items.json` into memory, supports lookup by id/aliases.
   - `InventoryService`: holds player inventory state (Flow-backed), handles add/remove/dedupe, stackable vs unique logic.
   - `CraftingService`: consumes recipe definitions, checks requirements, emits crafted items, fires `EventManager` hooks.

3. **Compose UI Strategy**
   - Rebuild `BagScreen` as `feature.inventory.ui.InventoryScreen` with filters/tabs (All, Weapons, Consumables, Key Items).
   - `ShopScreen` as separate feature packages.
   - **Tinkering Screen:** Implemented as a **Main Menu** screen (accessible anywhere).
   - **Equipment Detail:** Visuals must show Mod Sockets as **Locked / Open / Filled**, driven by Story Flags.

4. **Integration Points**
   - Tie into `ExplorationViewModel` events: `give_item`, `grant_reward`, `spawn_item_on_ground`, `unlock_room_search` update inventory or world state.
   - Dialogue/event triggers can open inventory or crafting screens via new navigation routes.
   - **Combat UI:** Replace standard "Item" menu with a single "Snack" button. This button instantly activates the equipped item in the `SNACK_SLOT` (Self-Target only), triggering a cooldown.
   - Combat consumes inventory items (heals, buffs) via shared service and event bus.

5. **Upcoming Tasks**
   - Expand `InventoryScreen` with filters, detail drawer, and navigation from exploration.
   - Implement item usage UI and hook restorative/buff outcomes into player stats once available.
   - Wire crafting screens (tinkering/first aid) using `CraftingService` for validation and item consumption.
   - Define navigation routes from exploration to inventory/crafting screens.

## Risks / Unknowns
- Recipe data files (tinkering) require normalization; verify file structure in Python repo.
- Equipment stats & combat integration (damage ranges, slots) need alignment with future combat overhaul.
- Some Python interactions rely on world state toggles and tooltips; plan incremental UI while event system matures.
