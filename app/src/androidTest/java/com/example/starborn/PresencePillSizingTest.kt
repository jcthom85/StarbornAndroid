package com.example.starborn

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.feature.exploration.ui.QuickMenuAction
import com.example.starborn.feature.exploration.ui.RoomEntitySection
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Measures the actual Android presence rail, including its unbounded horizontal scroll. */
class PresencePillSizingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pillsFitContentAndRemainScrollable() = checkSizing(1f)
    @Test fun largerTextDoesNotCollapsePillLabels() = checkSizing(1.4f)

    private fun checkSizing(fontScale: Float) {
        val expected = mutableMapOf<String, Float>()
        var selected = ""
        var talkHeight = 0f
        val labels = listOf("Rest", "Cook", "Shops", "Equipment and Supplies")
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                com.example.starborn.ui.theme.StarbornTheme {
                    val density = LocalDensity.current
                    val measurer = rememberTextMeasurer()
                    val service = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    val bold = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    val talkStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    SideEffect {
                        talkHeight = measurer.measure(AnnotatedString("Talk"), talkStyle).size.height / density.density
                        labels.forEach { expected["$it action"] = measurer.measure(AnnotatedString(it), service).size.width / density.density + 58f }
                        for (name in listOf("Penny", "Scout with a particularly long name"))
                            expected["Talk to $name"] = measurer.measure(AnnotatedString(name), bold).size.width / density.density + 58f
                        for (name in listOf("Ore", "Functional Cryo Inductor"))
                            expected["Pick up $name"] = measurer.measure(AnnotatedString(name), bold).size.width / density.density + 58f
                    }
                    Box(Modifier.width(320.dp)) {
                        RoomEntitySection(
                            npcs = listOf("penny", "scout"),
                            npcPresenceNames = mapOf("penny" to "Penny", "scout" to "Scout with a particularly long name"),
                            npcPortraitPaths = emptyMap(),
                            groundItems = mapOf("ore" to 1, "inductor" to 1),
                            serviceActions = labels.map { QuickMenuAction(iconRes = R.drawable.inventory_icon, label = it,
                                roomAction = RestStopAction(it)) },
                            itemDisplayName = { if (it == "ore") "Ore" else "Functional Cryo Inductor" },
                            itemDetailLabel = { null }, itemIsEquipment = { false },
                            accentColor = Color.Cyan, borderColor = Color.Gray, isDark = false,
                            onNpcClick = { selected = it }, onCollectItem = { selected = it },
                            onCollectAll = {}, onAction = { selected = it.name })
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("Talk").assertCountEquals(2)
        compose.onAllNodesWithText("Talk").fetchSemanticsNodes().indices.forEach { i ->
            compose.onAllNodesWithText("Talk")[i].assertHeightIsAtLeast((talkHeight - 1f).dp)
        }
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val folder = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "presence-pill-sizing").apply { mkdirs() }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        java.io.File(folder, "pills-$fontScale.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        for ((description, width) in expected) {
            val node = compose.onNodeWithContentDescription(description)
            node.assertWidthIsAtLeast((maxOf(width, 100f) - 1).dp)
            // Avoid fixing truncation by giving every pill a generous fixed width.
            val bounds = node.getUnclippedBoundsInRoot()
            assertEquals("$description must fit its content with a useful minimum", maxOf(width, 100f), (bounds.right - bounds.left).value, 1f)
        }
        compose.onNodeWithContentDescription("Equipment and Supplies action").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("Equipment and Supplies", selected) }
        compose.onNodeWithContentDescription("Talk to Scout with a particularly long name").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("scout", selected) }
        compose.onNodeWithContentDescription("Pick up Functional Cryo Inductor").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("inductor", selected) }
    }

    @Test fun shortNamesKeepUsefulWidthAndOnlyRealMerchantsAdvertiseTrading() {
        var talkedTo = ""
        compose.setContent { com.example.starborn.ui.theme.StarbornTheme {
            Box(Modifier.width(320.dp)) {
                RoomEntitySection(npcs = listOf("jed", "mechanic_mira", "scrapper"),
                    npcPresenceNames = mapOf("jed" to "Jed", "mechanic_mira" to "Mira", "scrapper" to "Scrapper"),
                    npcPortraitPaths = emptyMap(), npcTradeKeys = setOf("scrapper"), groundItems = emptyMap(),
                    itemDisplayName = { it }, itemDetailLabel = { null }, itemIsEquipment = { false },
                    accentColor = Color.Cyan, borderColor = Color.Gray, isDark = false,
                    onNpcClick = { talkedTo = it }, onCollectItem = {}, onCollectAll = {}, onAction = {})
            }
        } }
        compose.onNodeWithContentDescription("Talk to Jed").assertWidthIsAtLeast(100.dp)
        compose.onNodeWithContentDescription("Talk to Mira").assertWidthIsAtLeast(100.dp).performClick()
        compose.runOnIdle { assertEquals("mechanic_mira", talkedTo) }
        compose.onAllNodesWithText("Talk").assertCountEquals(2)
        compose.onAllNodesWithText("Talk • 🛒 Shop").assertCountEquals(1)
    }
}
