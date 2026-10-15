package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.DesktopCombatScreen
import com.example.starborn.desktop.ui.DesktopStarbornTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class DesktopCombatFormationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun partySizesAndBossCompanionsHaveReviewedSnapshots() {
        val directory = Files.createTempDirectory("starborn-formation-matrix-").toFile()
        val services = DesktopAppServices(directory)
        val scenario = mutableStateOf(0)
        val cases = (1..4).map { it to listOf("echo_borer", "resonance_buoy", "faulted_loader") } +
            listOf(4 to listOf("the_iron_warden"), 4 to listOf("the_iron_warden", "resonance_buoy", "resonance_buoy"))
        try {
            services.startNewGame()
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            services.sessionStore.setPartyMembers(listOf("nova"))
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme(services) {
                androidx.compose.runtime.key(scenario.value) {
                    Box(Modifier.size(1024.dp, 768.dp)) {
                        DesktopCombatScreen(services, cases[scenario.value].second, {}, {}, {})
                    }
                }
            } }
            cases.forEachIndexed { index, (count, _) ->
                compose.runOnIdle {
                    services.sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st").take(count))
                    scenario.value = index
                }
                compose.mainClock.advanceTimeBy(100)
                compose.waitForIdle()
                val file = File("build/reports/combat-clarity/matrix-$index-party-$count.png").apply { parentFile.mkdirs() }
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
                val figures = compose.onAllNodes(SemanticsMatcher("figures") { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith("combat-figure-") }).fetchSemanticsNodes()
                assertTrue(figures.size == count + cases[index].second.size)
                compose.onNodeWithTag("combat-recent-actions").assertIsDisplayed()
                if (index == 5) {
                    val boss = compose.onNodeWithContentDescription("The Iron Warden", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                    assertTrue("Boss should retain visual presence beside companions", boss.height > 200f)
                }
            }
        } finally { services.close(); directory.deleteRecursively() }
    }

    @Test fun sixEnemiesAndFullPartyFitWideAndCompactBattlefields() {
        val directory = Files.createTempDirectory("starborn-formation-").toFile()
        val services = DesktopAppServices(directory)
        val compact = mutableStateOf(false)
        try {
            services.startNewGame()
            services.sessionStore.setPartyMembers(listOf("nova", "zeke", "orion", "gh0st"))
            runBlocking { services.userSettingsStore.setTutorialsEnabled(false) }
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme(services) {
                Box(Modifier.size(if (compact.value) 800.dp else 1024.dp, if (compact.value) 600.dp else 768.dp)) {
                    DesktopCombatScreen(services, listOf("faulted_loader", "resonance_buoy", "echo_borer", "resonance_buoy", "echo_borer", "faulted_loader"), {}, {}, {})
                }
            } }
            for (small in listOf(false, true)) {
                compose.runOnIdle { compact.value = small }
                compose.mainClock.advanceTimeBy(100)
                val figures = compose.onAllNodes(SemanticsMatcher("combat figures") { it.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.TestTag) { "" }.startsWith("combat-figure-") }).fetchSemanticsNodes()
                assertTrue("All ten combatants should remain present", figures.size == 10)
                val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
                figures.forEach { figure ->
                    val bounds = figure.boundsInRoot
                    assertTrue("Combatant must fit battlefield: $bounds", bounds.top >= root.top && bounds.bottom <= root.bottom && bounds.left >= root.left && bounds.right <= root.right)
                }
                figures.forEachIndexed { index, first -> figures.drop(index + 1).forEach { second ->
                    assertTrue("Combatant slots overlap", !first.boundsInRoot.overlaps(second.boundsInRoot))
                } }
                val file = File("build/reports/combat-clarity/formation-${if (small) "compact" else "wide"}.png").apply { parentFile.mkdirs() }
                ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
            }
        } finally { services.close(); directory.deleteRecursively() }
    }
}
