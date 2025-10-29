# Item Effect Handling & Crafting Integration

## Current Status
- `ItemEffect` includes restorative, damage, schematic, and buff definitions mirroring `items.json`.
- `InventoryService` exposes `useItem(id)` returning an `ItemUseResult`; items are consumed from inventory and events fire through `ExplorationViewModel`.
- Schematic use triggers `CraftingService.learnSchematic`, while consumables and buffs surface status messages/events for UI integration.

## Remaining Work
1. **Effect Application**
   - Hook restorative and buff results into combat/player stats once those systems are ported.
   - Apply damage effects and targeted usage when enemy/player contexts exist.
2. **Crafting Progress**
   - Persist learned schematics and expose availability filtering in crafting UI.
3. **UI Enhancements**
   - Inventory detail view to display item effects and enable “Use” actions directly from the list.
