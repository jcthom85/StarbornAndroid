package com.example.starborn

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.feature.exploration.ui.tabs.*
import com.example.starborn.feature.exploration.ui.components.SkillTreeContent
import com.example.starborn.feature.exploration.viewmodel.*
import com.example.starborn.feature.combat.ui.components.CombatTutorialOverlay
import com.example.starborn.feature.combat.viewmodel.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class Part2ConsiderationsUiTest {
    @get:Rule val compose = createComposeRule()
    @Composable private fun LargeText(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) { com.example.starborn.ui.theme.StarbornTheme {
            Surface(Modifier.fillMaxSize(), color = Color(0xFF071018)) {
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(12.dp)) { content() }
            }
        } }
    }

    @Test fun completedQuestGroupsExposeAllEntriesAndCollapseWithoutLosingSelection() {
        val quests = (1..10).map { QuestSummaryUi("w1_sq$it", "Finished quest $it", "", null, null, emptyList(), true, 0, 1) } +
            QuestSummaryUi("w2_sq01", "Forest quest", "", null, null, emptyList(), true, 0, 1)
        var selected: String? = null
        compose.setContent { LargeText { Column(Modifier.fillMaxSize()) {
            CompletedQuestGroups(quests, Color.Cyan, Color.Gray) { selected = it }
        } } }
        compose.onNodeWithText("World 1 (10)").performClick()
        compose.onNodeWithText("Finished quest 10").performClick()
        compose.runOnIdle { assertEquals("w1_sq10", selected) }
        compose.onNodeWithText("World 1 (10)").performClick()
        compose.onNodeWithText("Finished quest 10").assertDoesNotExist()
        capture("completed-quests")
        compose.onNodeWithText("World 2 (1)").performClick()
        compose.onNodeWithText("Forest quest").assertExists()
    }

    @Test fun combatConsumableCannotBeUsedFromFieldDetails() {
        var uses = 0
        val coffee = InventoryPreviewItemUi("black_coffee", "Black Coffee", 5, "consumable", combatOnly = true)
        compose.setContent { LargeText { InventoryItemDetailsContent(coffee, Color.Cyan, Color.Gray, { uses++ }) } }
        compose.onNodeWithText("COMBAT CONSUMABLE").assertExists()
        compose.onNodeWithText("Combat only").assertIsNotEnabled()
        capture("combat-consumable")
        compose.runOnIdle { assertEquals(0, uses) }
    }

    @Test fun respecShowsRefundAndRequiresConfirmation() {
        var resets = 0
        val tree = SkillTreeOverlayUi("nova", "Nova", null, 2, 3, emptyList(), respecRefund = 3)
        compose.setContent { LargeText { SkillTreeContent(tree, Color.Cyan, Color.Gray, onUnlockSkill = {}, onResetBuild = { resets++ }) } }
        compose.onNodeWithText("Respec Character · Refund 3 AP").performClick()
        compose.runOnIdle { assertEquals(0, resets) }
        capture("respec-confirmation")
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, resets) }
        compose.onNodeWithText("Respec Character · Refund 3 AP").performClick()
        compose.onNodeWithText("Respec Character", substring = false).performClick()
        compose.runOnIdle { assertEquals(1, resets) }
    }

    @Test fun guidanceUsesReservedSpaceBetweenHeaderAndBattlefield() {
        compose.setContent { LargeText { Column(Modifier.fillMaxSize()) {
            Text("Security Post", Modifier.fillMaxWidth().testTag("header"))
            CombatTutorialOverlay(CombatTutorialState(CombatTutorialStep.CHOOSE_SKILLS, "trainer"), null, false, {}, {}, Modifier.testTag("guidance"))
            Box(Modifier.weight(1f).fillMaxWidth().testTag("battlefield"))
        } } }
        val header = compose.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
        val guidance = compose.onNodeWithTag("guidance").fetchSemanticsNode().boundsInRoot
        val battlefield = compose.onNodeWithTag("battlefield").fetchSemanticsNode().boundsInRoot
        assertTrue(guidance.top >= header.bottom)
        assertTrue(guidance.bottom <= battlefield.top)
        capture("tutorial-guidance")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val folder = java.io.File(context.getExternalFilesDir(null), "part2-considerations").apply { mkdirs() }
        val bitmap = requireNotNull(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        java.io.File(folder, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

}
