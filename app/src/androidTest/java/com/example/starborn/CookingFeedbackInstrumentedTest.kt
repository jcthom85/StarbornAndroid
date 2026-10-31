package com.example.starborn

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.AndroidAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.domain.crafting.CraftingService
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.*
import com.example.starborn.feature.crafting.ui.CookingScreen
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CookingFeedbackInstrumentedTest {
    @get:Rule val rule = createComposeRule()

    @Test fun cookingFarDownTheListShowsTheStandardResultWithoutScrollingBack() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reader = AssetJsonReader(AndroidAssetProvider(context), MoshiProvider.instance)
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        val source = CraftingAssetDataSource(reader)
        val meals = source.loadCookingRecipes().filter { it.category == "meal" }.sortedBy { it.name }
        val inventory = InventoryService(object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        }).apply {
            loadItems()
            meals.flatMap { it.ingredients.keys }.distinct().forEach { addItem(it, 100) }
        }
        val session = GameSessionStore().apply { restore(GameSessionState(
            playerId = "nova", partyMembers = listOf("nova"),
            roomStates = meals.mapNotNull { it.discoveryRoom }.associateWith { mapOf("cooking_discovered" to true) }
        )) }
        val service = CraftingService(source, inventory, session)
        val recipe = meals.last()
        val before = inventory.snapshot()[recipe.result] ?: 0
        rule.setContent { StarbornTheme { CookingScreen(service, inventory, "Campfire", {}) } }
        rule.onNodeWithTag("cooking-recipes").performScrollToNode(hasTestTag("cook-${recipe.id}"))
        rule.onNodeWithTag("cook-${recipe.id}").performClick()
        rule.onNodeWithText("At the cookfire").assertIsDisplayed()
        rule.onNodeWithText("Done").assertIsDisplayed().performClick()
        rule.onNodeWithText("At the cookfire").assertDoesNotExist()
        assertTrue((inventory.snapshot()[recipe.result] ?: 0) > before)
    }
}
