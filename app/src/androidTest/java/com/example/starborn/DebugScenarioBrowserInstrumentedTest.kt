package com.example.starborn

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import com.example.starborn.feature.mainmenu.ui.DebugScenarioDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DebugScenarioBrowserInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun testInstructionsAreReviewableBeforeLaunch() {
        var launched: String? = null
        compose.setContent {
            MaterialTheme {
                DebugScenarioDialog(onLaunch = { launched = it.id }, onDismiss = {}, testSession = true)
            }
        }
        compose.onNodeWithTag("scenario-search").performTextInput("recipe_cryo_exact")
        compose.onNodeWithTag("scenario-recipe_cryo_exact").performClick()
        compose.onNodeWithText("Steps").assertIsDisplayed()
        compose.onNodeWithText("Launch / Reset").performClick()
        compose.runOnIdle { assertEquals("recipe_cryo_exact", launched) }
    }
}
