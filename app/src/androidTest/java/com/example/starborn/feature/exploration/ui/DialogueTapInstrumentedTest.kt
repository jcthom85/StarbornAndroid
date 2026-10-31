package com.example.starborn.feature.exploration.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.feature.exploration.viewmodel.DialogueChoiceUi
import com.example.starborn.feature.exploration.viewmodel.DialogueUi
import com.example.starborn.ui.theme.StarbornTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

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
        rule.onNodeWithText("1. Tell me about the signal").assertIsDisplayed()
        rule.runOnIdle { assertEquals(0, choices); assertEquals(0, advances) }
        rule.onNodeWithText("1. Tell me about the signal").performClick()
        rule.runOnIdle { assertEquals(1, choices); assertEquals(0, advances) }
    }
}
