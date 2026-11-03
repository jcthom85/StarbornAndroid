package com.example.starborn

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.starborn.feature.combat.ui.components.EnemyStatusLabel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EnemyCardReadabilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enemyNameAndExactHpRemainLegibleAtNormalAndLargeTextSizes() {
        var scale by mutableStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                com.example.starborn.ui.theme.StarbornTheme {
                    Row(Modifier.width(350.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Siren Skimmer", "The Iron Warden", "Echo Borer").forEach { name ->
                            EnemyStatusLabel(name, 425, 1200, 40, 100, 0, .5f, true, false, false,
                                Color.Cyan, 90.dp)
                        }
                    }
                }
            }
        }
        for (fontScale in listOf(1f, 1.3f)) {
            compose.runOnIdle { scale = fontScale }
            for (name in listOf("Siren Skimmer", "The Iron Warden", "Echo Borer")) {
                val title = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("enemy-label-name-$name", useUnmergedTree = true)
                    .assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(title) }
                assertTrue(title.isNotEmpty())
                assertFalse("$name must not be shortened", title.single().hasVisualOverflow)
                assertTrue(title.single().layoutInput.style.fontSize.value in 11f..13f)
                val hp = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("enemy-label-hp-$name", useUnmergedTree = true)
                    .assertTextEquals("HP 425 / 1200").assertIsDisplayed()
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(hp) }
                assertFalse(hp.single().hasVisualOverflow)
                assertEquals(11f, hp.single().layoutInput.style.fontSize.value, 0f)
            }
            val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            val folder = java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "enemy-card-readability").apply { mkdirs() }
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            java.io.File(folder, "cards-$fontScale.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
