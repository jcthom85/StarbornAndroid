package com.example.starborn.feature.fishing.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.AndroidAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.FishingAssetDataSource
import com.example.starborn.domain.fishing.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.fishing.viewmodel.*
import com.example.starborn.ui.theme.StarbornTheme
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FishingPortraitInstrumentedTest {
    @get:Rule val rule = createComposeRule()
    @Test fun firstCatchUsesSwipeAndReelControlsAndSecuresInventoryOnce() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val reader = AssetJsonReader(AndroidAssetProvider(context), MoshiProvider.instance)
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        val inventory = InventoryService(object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        }).apply { loadItems(); addItem("wooden_rod"); addItem("basic_lure") }
        val service = FishingService(FishingAssetDataSource(reader), inventory,
            object : Random() { override fun nextBits(bitCount: Int) = 0 }, GameSessionStore())
        val vm = FishingViewModel(service, "sector9_stream", Random(1))
        val deliveries = AtomicInteger()
        try {
            rule.setContent { StarbornTheme { FishingScreen(vm, {}, {}, highContrastMode = false,
                largeTouchTargets = false, disableHaptics = true, onCatchResult = { deliveries.incrementAndGet() }) } }
            rule.onNodeWithText("Cast Line").assertIsDisplayed().performClick()
            rule.waitUntil(10_000) { vm.uiState.value.fishingState == FishingState.HOOKSET }
            rule.onNodeWithText("Set hook").assertIsDisplayed()
            rule.onNodeWithTag("fishing-portrait-scene").performTouchInput { swipeUp() }
            rule.waitUntil(5_000) { vm.uiState.value.fishingState == FishingState.READY }
            rule.onNodeWithText("I'm ready — start reeling").assertIsDisplayed().performClick()
            val screenshot = File(context.getExternalFilesDir(null), "fishing-portrait-android.png")
            screenshot.outputStream().use { rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
            val deadline = System.currentTimeMillis() + 60_000
            var held = false
            while (vm.uiState.value.fishingState == FishingState.REELING && System.currentTimeMillis() < deadline) {
                val reel = requireNotNull(vm.uiState.value.reelState)
                val shouldHold = reel.phase !in setOf(FishingFightPhase.WARNING, FishingFightPhase.SURGE) && reel.tension < .65f
                if (shouldHold != held) {
                    rule.onNodeWithTag("fishing-reel-control").performTouchInput { if (shouldHold) down(center) else up() }
                    held = shouldHold
                }
                Thread.sleep(120)
            }
            rule.waitUntil(5_000) { vm.uiState.value.fishingState == FishingState.RESULT }
            if (held) rule.onRoot().performTouchInput { up() }
            rule.onNodeWithText("Back on shore").assertIsDisplayed()
            assertTrue(requireNotNull(vm.uiState.value.lastCatchResult).secured)
            assertTrue(inventory.snapshot().getOrDefault("raw_glowfish", 0) > 0)
            rule.waitUntil(5_000) { deliveries.get() == 1 }
            val secured = inventory.snapshot()
            rule.onNodeWithText("Cast again").assertIsDisplayed().performClick()
            rule.waitUntil(5_000) { vm.uiState.value.fishingState == FishingState.SETUP }
            assertEquals(secured, inventory.snapshot())
            assertEquals(1, deliveries.get())
        } finally { vm.runtime.close() }
    }
}
