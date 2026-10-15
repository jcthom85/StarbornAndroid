package com.example.starborn

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.domain.model.RestStopAction
import com.example.starborn.feature.exploration.presentation.roomObjectOffer
import com.example.starborn.feature.exploration.ui.*
import com.example.starborn.feature.exploration.viewmodel.NarrationPrompt
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RoomPopupStyleInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun blockedRouteUsesInspectionAcknowledgement() {
        var closed=0
        compose.setContent { MaterialTheme {
            InspectionOverlay(NarrationPrompt("The door control panel needs attention.",true),null,{closed++},Modifier.fillMaxSize())
        } }
        compose.onNodeWithText("Continue").assertIsDisplayed()
        compose.onAllNodesWithText("Route blocked").assertCountEquals(0)
        compose.onNodeWithText("The door control panel needs attention.").performTouchInput {click()}
        compose.runOnIdle {assertEquals(1,closed)}
    }
    @Test fun sleepingShelfBodyDoesNotRestAndButtonsRemainExplicit() {
        var rested=0;var closed=0
        val offer=requireNotNull(roomObjectOffer(RestStopAction("sleeping shelf")))
        compose.setContent {MaterialTheme {RoomObjectOfferDialog(offer,null,{rested++},{closed++})}}
        compose.onNodeWithText("Sleeping shelf").assertIsDisplayed()
        compose.onNodeWithText(offer.detail).performTouchInput {click()}
        compose.runOnIdle {assertEquals(0,rested);assertEquals(0,closed)}
        compose.onNodeWithText("Back").performClick()
        compose.runOnIdle {assertEquals(0,rested);assertEquals(1,closed)}
        compose.onNodeWithText("Rest").performClick()
        compose.runOnIdle {assertEquals(1,rested)}
    }
}
