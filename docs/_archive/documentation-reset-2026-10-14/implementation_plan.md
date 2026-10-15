# Implementation Plan: Tinkering & Cooking Systems Upgrade

This plan upgrades the Tinkering and Cooking systems in *Starborn* by combining Option A (adding non-consumed tool requirements, such as a portable stove, to recipes) and Option B (expanding late-game gear mods and integrating cooking with the Resonance Fishing system).

---

## User Review Required

> [!IMPORTANT]
> **Tool Consumability:**
> In this implementation, tools (e.g., `portable_stove`) are required in the player's inventory to craft/cook, but they are **not consumed** when the recipe is executed. Only the raw ingredients (e.g., `noodles`, `beast_meat`) will be consumed.
> 
> **Backwards Compatibility:**
> The `TinkeringRecipe` constructor is updated with a default parameter for `tools` (`emptyList()`), ensuring that existing tests and Moshi parsers do not break.

---

## Proposed Changes

### 1. Domain Models & Parsing

#### [MODIFY] [CraftingModels.kt](file:///C:/users/jctho/StudioProjects/StarbornAndroid/app/src/main/java/com/example/starborn/domain/model/CraftingModels.kt)
*   Add a `tools` parameter to the `TinkeringRecipe` data class to declare non-consumed tool requirements:
    ```kotlin
    data class TinkeringRecipe(
        val id: String,
        val name: String,
        val description: String? = null,
        val category: String = "gear",
        val method: String? = null,
        val base: String? = null,
        val components: List<String> = emptyList(),
        val ingredients: Map<String, Int> = emptyMap(),
        val result: String,
        @Json(name = "result_quantity")
        val resultQuantity: Int = 1,
        @Json(name = "success_message")
        val successMessage: String? = null,
        val tools: List<String> = emptyList() // New field with default value for compatibility
    )
    ```

---

### 2. Crafting Service Logic

#### [MODIFY] [CraftingService.kt](file:///C:/users/jctho/StudioProjects/StarbornAndroid/app/src/main/java/com/example/starborn/domain/crafting/CraftingService.kt)
*   Update `canCraft(recipe: TinkeringRecipe)` to verify that the player possesses at least one of each required tool listed in the recipe's `tools` list:
    ```kotlin
    val hasIngredients = requirementCounts.all { (id, needed) -> (inventoryCounts[id] ?: 0) >= needed }
    if (!hasIngredients) return false

    val hasTools = recipe.tools.all { tool ->
        val normalizedTool = normalizeToken(tool)
        (inventoryCounts[normalizedTool] ?: 0) >= 1
    }
    return hasTools
    ```
*   Update `craftTinkering(recipeId: String)` to return a clear error message `CraftingOutcome.Failure("Missing components or tools")` if the `canCraft` check fails.

---

### 3. Game Assets: Items, Cooking & Late-game Mods

#### [MODIFY] [items.json](file:///C:/users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/items.json)
*   **[NEW]** Add the `portable_stove` utility item:
    ```json
    {
      "id": "portable_stove",
      "name": "Portable Stove",
      "description": "A compact thermal burner for cooking meals in the field. Reusable.",
      "type": "utility",
      "value": 150,
      "buy_price": 300,
      "rarity": "uncommon"
    }
    ```
*   **[NEW]** Add new late-game gear mods: `photon_core_mod` (World 4), `nanite_plating_mod` (World 5), `resonance_capacitor_mod` (World 6).
*   **[NEW]** Add new cooked fish dishes: `resonance_carp_stew`, `chime_minnow_broth`.

#### [MODIFY] [recipes_tinkering.json](file:///C:/users/jctho/StudioProjects/StarbornAndroid/app/src/main/assets/recipes_tinkering.json)
*   Update all cooking recipes (`provision_spicy_ramen`, `provision_ration_soup`, `provision_glowfish_broth`) to require the `portable_stove` in `tools`.
*   **[NEW]** Add recipes for the new late-game gear mods and new fish dishes.

---

## Verification Plan

### Automated Tests
Run unit tests to verify that:
1.  Recipes requiring a tool cannot be crafted if the tool is missing.
2.  Recipes requiring a tool can be crafted if the tool is present.
3.  The tool itself is **not consumed** during the crafting process.
4.  Existing recipes without tools continue to craft correctly.

Execute tests using:
```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.starborn.domain.crafting.CraftingServiceTest"
```

### Static Asset Verification
Verify all assets compile and reference IDs correctly:
```powershell
.\gradlew.bat :app:runAssetIntegrity
```
