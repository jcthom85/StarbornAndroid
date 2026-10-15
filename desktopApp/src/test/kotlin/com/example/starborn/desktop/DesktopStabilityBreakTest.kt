package com.example.starborn.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.example.starborn.desktop.ui.DesktopCombatCue
import com.example.starborn.desktop.ui.DesktopCombatHit
import com.example.starborn.desktop.ui.DesktopCueKind
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.runtime.*
import com.example.starborn.desktop.ui.*
import com.example.starborn.domain.combat.*
import com.example.starborn.feature.combat.viewmodel.AttackLungeStyle
import java.nio.file.Files

class DesktopStabilityBreakTest {
    @get:Rule val compose = createComposeRule()

    @Test fun breakBurstAndBadgeRenderAtMidAnimation() {
        compose.mainClock.autoAdvance = false
        val cue = DesktopCombatCue(1L, "enemy", "STABILITY BREAK!", Color(0xFF00E5FF), kind = DesktopCueKind.BREAK)
        compose.setContent {
            Box(Modifier.size(300.dp)) {
                DesktopCombatHit(cue, false, Modifier.size(300.dp))
            }
        }
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithText("SHIELD BREAK!").assertExists()
        val file = File("build/reports/combat-clarity/stability-break.png").apply { parentFile.mkdirs() }
        ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
    }

    @Test fun mobileBrokenFieldIsVisibleWhileBrokenAndDisappearsOnRecoveryOrDeath() {
        val directory = Files.createTempDirectory("starborn-break-parity-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            val enemy = services.worldDataSource.loadEnemies().first { it.id == "faulted_loader" }
            var actor by mutableStateOf(CombatantState(Combatant("enemy", enemy.name, CombatSide.ENEMY,
                StatBlock(200, 10, 10, 10, 10, 10, 10, stability = 200)), hp = 150, stability = 50, breakTurns = 2))
            compose.mainClock.autoAdvance = false
            compose.setContent { DesktopStarbornTheme(services) {
                DesktopBattleFormation(listOf(actor), services, Modifier.size(600.dp, 500.dp),
                    enemySide = true, sprite = { enemy.sprite.firstOrNull() ?: enemy.portrait }, selectedId = null,
                    validTargets = emptySet(), targeting = false, meters = emptyMap(), intents = emptyMap(),
                    feedback = emptyMap(), cues = emptyList(), lungeStyle = AttackLungeStyle.MELEE,
                    lungeActor = null, lungeToken = 0, missActor = null, missToken = 0, onSelect = {})
            } }
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("combat-broken-field-enemy", useUnmergedTree = true).assertExists()
            compose.onNodeWithContentDescription("Broken: takes 25% more direct damage", useUnmergedTree = true).assertExists()
            val file = File("build/reports/combat-clarity/stability-broken-field.png").apply { requireNotNull(parentFile).mkdirs() }
            ImageIO.write(compose.onRoot().captureToImage().asAwtImage(), "png", file)
            compose.runOnIdle { actor = actor.copy(breakTurns = 0, stability = 200) }
            compose.mainClock.advanceTimeBy(100)
            compose.onNodeWithTag("combat-broken-field-enemy", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithContentDescription("Stability (Stagger)", useUnmergedTree = true).assertExists()
            compose.runOnIdle { actor = actor.copy(breakTurns = 2, hp = 0) }
            compose.mainClock.advanceTimeBy(100)
            compose.onNodeWithTag("combat-broken-field-enemy", useUnmergedTree = true).assertDoesNotExist()
        } finally { services.close(); directory.deleteRecursively() }
    }
}
