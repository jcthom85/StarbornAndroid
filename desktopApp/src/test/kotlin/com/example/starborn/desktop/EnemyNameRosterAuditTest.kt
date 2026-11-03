package com.example.starborn.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.Enemy
import com.example.starborn.ui.menu.ReadableEnemyName
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class EnemyNameRosterAuditTest {
    @get:Rule val compose = createComposeRule()
    @Test fun auditWholeRosterAndPreserveFullTextInTheNarrowestLabels() {
        val enemies = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance).readList<Enemy>("enemies.json")
        val widths = mutableMapOf<String, List<Int>>()
        var fontScale by mutableStateOf(1f)
        compose.setContent {
            val measurer = rememberTextMeasurer()
            val style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
            enemies.forEach { enemy -> widths[enemy.name] = listOf(13, 12, 11).map { size ->
                measurer.measure(enemy.name, style.copy(fontSize = size.sp), softWrap = false).size.width
            } }
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
            Column(Modifier.width(82.dp).height(600.dp).verticalScroll(rememberScrollState())) {
                enemies.map { it.name }.forEach { name ->
                    ReadableEnemyName(name, Modifier.testTag(name))
                }
            }
            }
        }
        compose.waitForIdle()
        assertEquals(enemies.size, widths.size)
        for (scale in listOf(1f, 1.3f, 2f)) {
        compose.runOnIdle { fontScale = scale }
        compose.waitForIdle()
        enemies.map { it.name }.forEach { name ->
            val layout = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag(name).assertTextEquals(name)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
            assertFalse("Full name must remain visible: $name", layout.single().hasVisualOverflow)
            assertTrue(layout.single().layoutInput.style.fontSize.value >= 11f)
        }
        }
        val out = File("build/reports/enemy-name-audit").apply { mkdirs() }
        File(out, "roster.csv").writeText("Name,13sp width,12sp width,11sp width,82dp three-column,96dp three-column,144dp two-column,224dp single\n" +
            enemies.joinToString("\n") { enemy ->
                val measured = widths.getValue(enemy.name)
                enemy.name + "," + measured.joinToString(",") + "," + listOf(82,96,144,224).joinToString(",") { available ->
                    val fit = listOf(13,12,11).zip(measured).firstOrNull { it.second <= available }
                    fit?.let { "${it.first}sp" } ?: "wrap: naming discussion"
                }
            } + "\n")
    }
}
