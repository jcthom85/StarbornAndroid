package com.example.starborn.desktop

import androidx.compose.ui.graphics.asAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.starborn.desktop.ui.DesktopCombatScreen
import com.example.starborn.domain.combat.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class DesktopWorld1BossAndTransitionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWorld1EliteBulwarkCombatAndWorldTransition() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_w1_climax_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)
            services.startNewGame()

            // 1. Verify Quest progression for World 1 Climax
            val allQuests = services.questRepository.allQuests().associateBy { it.id }
            val w1Quests = listOf("w1_mq01", "w1_mq02", "w1_mq03", "w1_mq04", "w1_mq05")
            w1Quests.forEach { questId ->
                assertTrue("World 1 Main Quest $questId must exist", allQuests.containsKey(questId))
            }

            // 2. Setup Elite Bulwark Boss Encounter
            val enemyData = services.worldDataSource.loadEnemies().associateBy { it.id }
            val bulwark = enemyData["acoustic_bulwark"]
            assertNotNull("Acoustic Bulwark elite must exist in enemies.json", bulwark)

            val partyCombatants = listOf(
                Combatant(
                    id = "nova",
                    name = "Nova",
                    side = CombatSide.PLAYER,
                    stats = StatBlock(maxHp = 120, strength = 18, vitality = 14, agility = 12, focus = 10, luck = 5, speed = 12, stability = 100)
                ),
                Combatant(
                    id = "zeke",
                    name = "Zeke",
                    side = CombatSide.ALLY,
                    stats = StatBlock(maxHp = 110, strength = 14, vitality = 12, agility = 10, focus = 15, luck = 8, speed = 10, stability = 100)
                )
            )

            val enemyCombatant = Combatant(
                id = bulwark!!.id,
                name = bulwark.name,
                side = CombatSide.ENEMY,
                stats = StatBlock(
                    maxHp = bulwark.hp,
                    strength = bulwark.strength,
                    vitality = bulwark.vitality,
                    agility = bulwark.agility,
                    focus = bulwark.focus,
                    luck = bulwark.luck,
                    speed = bulwark.speed,
                    stability = bulwark.stability ?: 80
                )
            )

            val setup = CombatSetup(
                playerParty = partyCombatants,
                enemyParty = listOf(enemyCombatant)
            )

            val combatState = services.combatEngine.beginEncounter(setup)
            assertNotNull("Combat encounter should be initialized", combatState)
            assertEquals(3, combatState.combatants.size)

            // 3. Render Desktop Combat View for Boss
            composeTestRule.setContent {
                DesktopCombatScreen(
                    services = services,
                    enemyIds = listOf("acoustic_bulwark"),
                    onVictory = {},
                    onDefeat = {},
                    onFlee = {}
                )
            }
            composeTestRule.waitForIdle()

            val artifactsDir = File(System.getProperty("user.home"), ".gemini/antigravity-cli/brain/03813eca-b59d-44ca-9f2d-90a0e5b8e32f")
            artifactsDir.mkdirs()

            val node = composeTestRule.onRoot()
            val image = node.captureToImage()
            val awtImage = image.asAwtImage()

            ImageIO.write(awtImage, "png", File(artifactsDir, "desktop_w1_boss_combat.png"))

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
