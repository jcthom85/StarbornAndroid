package com.example.starborn.feature.crafting

fun tinkeringAvailability(recipe: TinkeringRecipeUi): String {
    if (!recipe.learned && recipe.requiresSchematic) return "Learn this schematic before building."
    val missing = recipe.ingredients.filter { it.available < it.required }
    return if (missing.isEmpty()) {
        if (recipe.canCraft) "Ready to synthesize" else "Check the workbench requirements."
    } else "Missing: " + missing.joinToString(", ") {
        "${it.label.removePrefix("Tool: ")} ×${it.required - it.available}"
    }
}

/** Keep ownership and assembly notes available in details, rather than above every materials list. */
fun tinkeringEffectSummary(recipe: TinkeringRecipeUi): String = recipe.resultSummary.lineSequence()
    .filterNot { it.startsWith("Owned:") || it.startsWith("Equip in an unlocked") }
    .joinToString("\n")
