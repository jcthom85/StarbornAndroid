package com.example.starborn.feature.exploration.ui

import androidx.compose.foundation.layout.*
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.feature.exploration.ui.hud.RoomDescriptionPanel
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import com.example.starborn.ui.theme.StarbornTheme
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class DialogueTapInstrumentedTest {
    @get:Rule val rule = createComposeRule()

    @Test fun tappingReservedChoiceAreaRevealsWithoutAccidentallySelectingAResponse() {
        var choices = 0
        var advances = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { StarbornTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DialogueOverlay(DialogueUi(DialogueLine("tap-regression", "Zeke", "Read the signal carefully. ".repeat(24)), null, null),
                listOf(DialogueChoiceUi("reply", "Tell me about the signal")),
                onAdvance = { advances++ }, onChoice = { choices++ }, onPlayVoice = {})
        } } }
        rule.onNodeWithTag("dialogue-advance").performTouchInput { click(Offset(center.x, bottom - 35f)) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Tell me about the signal").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, choices); assertEquals(0, advances) }
        rule.onNodeWithText("Tell me about the signal").performTouchInput { click() }
        rule.runOnIdle { assertEquals(1, choices); assertEquals(0, advances) }
    }

    @Test fun persistentLeaveIsTouchableWhileTextIsRevealingAtLargeFont() {
        var dismissed = 0
        var chosen = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { StarbornTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.4f)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(340.dp).heightIn(max = 540.dp)) {
                        DialogueOverlay(DialogueUi(DialogueLine("leave-revealing", "Evelyn", "The orchids are flowering. ".repeat(30)),
                            "images/npcs/elara.webp", null, canDismiss = true),
                            List(5) { DialogueChoiceUi("topic_$it", "Ask about another part of the garden") },
                            onAdvance = {}, onChoice = { chosen++ }, onPlayVoice = {}, onDismiss = { dismissed++ })
                    }
                }
            }
        } }
        rule.onNodeWithTag("dialogue-leave").assertIsDisplayed().performTouchInput { click() }
        rule.runOnIdle { assertEquals(1, dismissed); assertEquals(0, chosen) }
    }

    @Test fun androidBackLeavesWithoutSelectingOrAdvancingDialogue() {
        var dispatcher: OnBackPressedDispatcher? = null
        var dismissed = 0
        var progressed = 0
        rule.mainClock.autoAdvance = false
        rule.setContent { StarbornTheme {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            DialogueOverlay(DialogueUi(DialogueLine("back-regression", "Robert", "A long conversation. ".repeat(30)),
                null, null, canDismiss = true), listOf(DialogueChoiceUi("topic", "Ask a question")),
                onAdvance = { progressed++ }, onChoice = { progressed++ }, onPlayVoice = {}, onDismiss = { dismissed++ })
        } }
        rule.runOnIdle { requireNotNull(dispatcher).onBackPressed() }
        rule.runOnIdle { assertEquals(1, dismissed); assertEquals(0, progressed) }
    }

    @Test fun actualEvelynAndRobertGoodbyeChoicesReceiveTouchEvents() {
        val type = Types.newParameterizedType(List::class.java, DialogueLine::class.java)
        val lines = InstrumentationRegistry.getInstrumentation().targetContext.assets.open("dialogue.json").bufferedReader().use {
            requireNotNull(MoshiProvider.instance.adapter<List<DialogueLine>>(type).fromJson(it.readText()))
        }.associateBy { it.id }
        var who by mutableStateOf("evelyn")
        val chosen = mutableListOf<String>()
        rule.mainClock.autoAdvance = false
        rule.setContent { StarbornTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val line = lines.getValue("${who}_default_talk")
            DialogueOverlay(DialogueUi(line, null, null, canDismiss = true),
                line.options.orEmpty().map { DialogueChoiceUi(it.id, it.text) },
                onAdvance = {}, onChoice = { chosen += it }, onPlayVoice = {}, onDismiss = {})
        } } }
        for (npc in listOf("evelyn", "robert")) {
            rule.runOnIdle { who = npc }
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithTag("dialogue-advance").performTouchInput { click(center) }
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithText("I'll let you get back to it. Take care.").assertIsDisplayed().performTouchInput { click() }
            rule.runOnIdle { assertEquals("${npc}_leave", chosen.last()) }
        }
    }

    @Test fun overflowCueWaitsForRevealedContentAndDisappearsAtTheEnd() {
        rule.mainClock.autoAdvance = false
        rule.setContent { StarbornTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DialogueOverlay(DialogueUi(DialogueLine("cue-regression", "Evelyn", "A line of dialogue.\n".repeat(24)), null, null, canDismiss = true),
                List(8) { DialogueChoiceUi("topic_$it", "Tell me about garden section $it") },
                onAdvance = {}, onChoice = {}, onPlayVoice = {}, onDismiss = {})
        } } }
        rule.onNodeWithTag("dialogue-more-text").assertDoesNotExist()
        rule.onNodeWithTag("dialogue-more-responses").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithTag("dialogue-more-text").assertDoesNotExist()
        rule.mainClock.advanceTimeBy(7000)
        rule.onNodeWithTag("dialogue-more-text").assertIsDisplayed()
        rule.onNodeWithTag("dialogue-more-responses").assertDoesNotExist()
        rule.onNodeWithTag("dialogue-advance").performTouchInput { click(Offset(20f, 200f)) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithTag("dialogue-more-text").assertIsDisplayed()
        rule.onNodeWithTag("dialogue-more-responses").assertIsDisplayed()
        capture("dialogue-overflow.png")
        repeat(12) {
            if (rule.onAllNodesWithTag("dialogue-more-text").fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithTag("dialogue-more-text").performTouchInput { click() }
                rule.mainClock.advanceTimeBy(600)
            }
        }
        rule.onNodeWithTag("dialogue-more-text").assertDoesNotExist()
        rule.onNodeWithTag("dialogue-leave").assertIsDisplayed()
    }

    @Test fun shortDescriptionsHaveNoCueAndLongDescriptionsCanBeScrolledToTheEnd() {
        var long by mutableStateOf(false)
        rule.mainClock.autoAdvance = false
        rule.setContent { StarbornTheme { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            RoomDescriptionPanel(null, if (long) "Rain drums against the glass.\n".repeat(24) else "A quiet room.", null,
                isDark = false, textColor = Color.White, onAction = {}, onNpcClick = {}, onEnemyClick = {},
                borderColor = Color.Cyan, accentColor = Color.Cyan, modifier = Modifier.width(330.dp).heightIn(max = 220.dp))
        } } }
        rule.onNodeWithTag("room-description-more").assertDoesNotExist()
        rule.runOnIdle { long = true }
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag("room-description-more").assertIsDisplayed()
        capture("room-description-overflow.png")
        repeat(16) {
            if (rule.onAllNodesWithTag("room-description-more").fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithTag("room-description-more").performTouchInput { click() }
                rule.mainClock.advanceTimeBy(600)
            }
        }
        rule.onNodeWithTag("room-description-more").assertDoesNotExist()
    }

    private fun capture(name: String) {
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, name)
        file.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
