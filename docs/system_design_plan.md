# Kotlin Retrofit Plan: Tinkering, Cooking, Fishing

## Goals
- Treat the Python JSON files as canonical and have the Android build ingest the same schemas (`recipes_tinkering.json`, `recipes_cooking.json`, `recipes_fishing.json` in `Starborn_Python/data`).
- Reintroduce the UX/mechanics the legacy screens implemented on top of those files while staying within the existing Kotlin architecture (asset readers, services, Compose routes).
- Leave clear seams for telemetry/tests so that future feature work can verify parity against the Python behaviours.

---

## Tinkering

### Current Kotlin Behaviour
- `CraftingAssetDataSource` already streams the Android asset copy of `recipes_tinkering.json` into `TinkeringRecipe` models (`app/src/main/java/com/example/starborn/data/assets/CraftingAssetDataSource.kt:5-15`).
- `CraftingViewModel` simply exposes the full recipe list and a `craft(id)` wrapper (`app/src/main/java/com/example/starborn/feature/crafting/CraftingViewModel.kt:17-52`), and the Compose screen renders each recipe with a single `Craft` button (`app/src/main/java/com/example/starborn/feature/crafting/ui/TinkeringScreen.kt:36-167`).
- The Python UI filtered by `learned_schematics`, supported recipe-book browsing, slot auto-fill, and scrapping/auto-fill workflows (`Starborn_Python/ui/tinkering_screen.py:767-863` and `Starborn_Python/crafting_manager.py:91-157`), none of which exist in Kotlin.

### Design Updates
1. **Data filtering + learned schematics**
   - Keep `CraftingService.tinkeringRecipes` as the canonical full list but have the view model surface two streams: `learnedRecipes` (those whose `id` is in `GameSessionStore.state.learnedSchematics`, already tracked in `CraftingService.isSchematicLearned` at `app/src/main/java/com/example/starborn/domain/crafting/CraftingService.kt:34-45`) and `lockedRecipes`.
   - Store the filter state (`learnedOnly`, `showAll`) inside a new `TinkeringUiState` so the Compose layer can match the Python recipe book toggle.

2. **Bench state + auto-fill**
   - Introduce a `TinkeringBenchState` data holder containing `selectedBase`, `selectedComponents`, `activeRecipeId`, and `missingItems`. Populate it from the JSON definitions exactly like `_auto_fill_from_recipe` and `_auto_fill_from_best` in `Starborn_Python/ui/tinkering_screen.py:820-863`.
   - View-model emits bench mutations: `selectBase(itemId)`, `selectComponent(slot, itemId)`, `autoFill(recipeId)`, `autoFillBest()`. Each mutation inspects the player inventory through `InventoryService` to decide whether the `Craft` CTA should be enabled.

3. **Scrapping workflow**
   - Mirror `CraftingManager.scrap_item` (`Starborn_Python/crafting_manager.py:91-157`) by adding a `ScrapService` (or extending `CraftingService`) that looks up `scrap_yield` definitions from `items.json` (or a pared-down asset file) and mutates the `InventoryService`.
   - Compose needs an affordance next to each eligible inventory item to trigger `viewModel.scrap(itemId)`; success returns a message and inventory delta so that the bench UI refreshes automatically.

4. **Compose UI**
   - Replace the flat list in `TinkeringScreen` with a two-pane layout: left column shows the base/components slots plus a `Craft` button (populated via `TinkeringBenchState`), right column hosts the recipe list filtered to learned schematics with `Auto-Fill` chips. The existing snackbar host stays for messaging.
   - Add a modal “Schematics Book” composable that lists learned recipes and invokes `autoFill(recipeId)` when tapped, matching the `MenuPopup` flow in the Python screen.

5. **Telemetry/tests**
   - Extend the instrumentation test that already covers navigation (`app/src/androidTest/java/com/example/starborn/NarrativeSystemsInstrumentedTest.kt:162-184`) to assert that unknown recipes stay hidden until a schematic is learned and that scrapping yields items listed in the JSON table.

---

## Cooking

### Current Kotlin Behaviour
- The JSON is already identical to Python aside from removing the duplicate entry (`app/src/main/assets/recipes_cooking.json` vs. `Starborn_Python/data/recipes_cooking.json`).
- `CookingViewModel` faithfully re-implements the timing bar minigame (`app/src/main/java/com/example/starborn/feature/crafting/CookingViewModel.kt:66-258`), but the Compose UI immediately lists every recipe with a `Cook` button and bypasses the ingredient slot/themed overlay that the Python `CookingScreen` used (`Starborn_Python/ui/cooking_screen.py:32-190`).

### Design Updates
1. **Ingredient slots & station context**
   - Add a `CookingStationState` containing `slotItems` (up to 3, `CookingScreen.MAX_SLOTS`), `selectedRecipeId`, and `previewDescription`. Populate it from the JSON by allowing the player to tap an ingredient slot and pick from inventory (mirrors `_choose_ingredient` flows in the Python screen).
   - The `Cook` CTA stays disabled until the current slot combination matches a known recipe and `CraftingService.canCraft(recipe)` returns true.

2. **Recipe book modal**
   - Recreate the `Recipe Book` button from `Starborn_Python/ui/cooking_screen.py:150-190`. In Compose, this is a bottom sheet / dialog listing all recipes pulled from the JSON, grouped by learned status if needed. Selecting a recipe auto-fills slots and immediately enables the minigame button if ingredients exist.

3. **Theme + overlay**
   - Instead of rendering cooking inside a bare Scaffold, wrap the content in a `StationSurface` composable that can accept colors/textures provided by the `ExplorationRoom` (matching the theme helpers in the Python implementation). This allows accessibility toggles (`highContrastMode`) plus room-specific palettes.

4. **Minigame integration**
   - The minigame already matches the Python `TimingBar` logic, but it should now launch only after the user sets up slots (not per recipe row). Hook `viewModel.startMinigame(currentSelection)` to read the selected ingredients before consuming inventory, then reuse the existing overlay component for the timing bar (`app/src/main/java/com/example/starborn/feature/crafting/ui/CraftingUiComponents.kt:36-118`).

5. **Data fidelity**
   - Keep using the Python JSON as-is. If we need per-recipe metadata (difficulty, cues) that only existed in the old Kivy widgets, extend the JSON with optional fields rather than duplicating hard-coded values in Kotlin.

6. **Validation**
   - Unit-test that slot permutations map back to recipes (e.g., selecting `Raw Fish` + `Herb` twice resolves to `fish_stew`) and that minigame outcomes issue the same cues defined in the JSON.

7. **Implementation status**
   - The Kotlin cooking station now mirrors the planned workflow: `CookingWorkspaceState` tracks the staged recipe and ingredient slots, the Recipe Book dialog feeds that state, and the Compose workspace card only enables “Cook” when the selected recipe is craftable (`app/src/main/java/com/example/starborn/feature/crafting/CookingViewModel.kt:25-320`, `app/src/main/java/com/example/starborn/feature/crafting/ui/CookingScreen.kt:1-220`). The timing minigame launches via `cookWorkspaceRecipe`, so ingredients are consumed only after the bench is prepared.

---

## Fishing

### Current Kotlin Behaviour
- Android currently ships a placeholder `recipes_fishing.json` with a single rod/lure/zone (`app/src/main/assets/recipes_fishing.json:1-42`) and bespoke models (`app/src/main/java/com/example/starborn/domain/fishing/FishingModels.kt:6-56`).
- `FishingService` rolls catches from that simplified schema (`app/src/main/java/com/example/starborn/domain/fishing/FishingService.kt:9-88`) and the Compose UI provides a linear rod/lure picker plus a horizontal timing bar (`app/src/main/java/com/example/starborn/feature/fishing/ui/FishingScreen.kt:40-220`).
- The Python build expects the richer schema in `Starborn_Python/data/recipes_fishing.json` and uses `FishingManager` to adjust rarity odds (`Starborn_Python/data/fishing_manager.py:1-90`) plus the vertical `ReelMinigame` widget (`Starborn_Python/ui/fishing_minigame.py:1-170`).

### Design Updates
1. **Data model overhaul**
   - Replace the Android asset file with the Python `recipes_fishing.json` (meta + rods + lures + `zones` map + `minigame_rules` + `victory_screen`).
   - Update `FishingModels` to match that schema: `FishingData(meta, rods, lures, zones: Map<String, List<ZoneCatch>>, minigameRules: Map<String, MinigameRule>, victoryScreen: VictoryScreenConfig)`. Zones now include `rarity` strings per catch instead of `min/max_quantity`.

2. **Service parity**
   - Port `FishingManager.get_catch` weighting into Kotlin: convert each zone entry into `(itemId, weight)` pairs, multiply weights by a rarity factor derived from rod power and lure bonus exactly like `_rarity_factor` (`Starborn_Python/data/fishing_manager.py:47-70`), and roll via a cumulative sum.
   - Move difficulty lookup into a helper that picks a minigame rule set from `minigame_rules` and the current rod tier (Python chooses easy/medium/hard in `_start_reel_minigame`, `Starborn_Python/ui/fishing_screen.py:324-354`).
   - Produce a `FishingResult` that also contains `rarity` and `flavor_text` so the Compose victory overlay can mimic the Python popup (`Starborn_Python/ui/fishing_screen.py:431-475`).

3. **ViewModel flow**
   - Expand `FishingUiState` with explicit phases: `Setup` (rod/lure selection), `Waiting` (bite chance timer), `Minigame` (reel loop), `Result`. The wait/bite animation mirrors `_open_setup_popup` → `_wait_popup` → `_bite_cue` in `Starborn_Python/ui/fishing_screen.py:120-320`.
   - When a bite is triggered, create a Compose equivalent of `ReelMinigame`: a vertical widget that simulates fish physics (see `Starborn_Python/ui/fishing_minigame.py:1-130`). The existing horizontal timing meter can be retired once the new widget lands.
   - After finishing, show a themed result surface that reads `victory_screen.contents` to decide what to render (item name, rarity text, quantity, flavor text).

4. **UI theming + controls**
   - Use the same `MenuOverlay` aesthetic as the other stations: Compose can implement it as a `Dialog`/`Surface` pair with blurred backdrops, while still respecting `highContrastMode`.
   - Rod/lure pickers become scrollable grids with inline power/rarity labels so they read like the Python setup popup.

5. **Testing + tooling**
   - Unit-test the rarity weighting vs. a seeded RNG to confirm that epic items become more likely with high rod power/lure bonus.
   - Add screenshot/UI tests to ensure the result overlay varies according to `victory_screen.show_on_success` vs. failure.

---

## Next Steps
1. Copy the Python JSON files into `app/src/main/assets` (fishing is the only file that differs today) and wire them to the existing `AssetJsonReader`.
2. Implement the Tinkering bench/recipe book state machine and scrap workflow, then gate recipes on learned schematics.
3. Upgrade the Cooking screen to use slot selection + recipe book and launch the existing timing minigame from that workspace.
4. Respec Fishing data/service/view model/UI to the original schema and Reel minigame.
5. Backfill unit/UI tests that cover the new behaviours described above.
