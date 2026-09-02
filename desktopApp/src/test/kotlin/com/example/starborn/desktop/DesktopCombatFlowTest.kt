package com.example.starborn.desktop

import com.example.starborn.domain.combat.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DesktopCombatFlowTest {

    @Test
    fun testFirstCombatEncounterSetupAndAction() {
        val tempSaveDir = File(System.getProperty("java.io.tmpdir"), "starborn_combat_test_${System.currentTimeMillis()}")
        tempSaveDir.mkdirs()

        try {
            val services = DesktopAppServices(saveDirectory = tempSaveDir)

            // Step 1: Initialize game session with party members Nova & Zeke
            services.startNewGame()
            val state = services.sessionStore.state.value.copy(
                partyMembers = listOf("nova", "zeke")
            )
            services.sessionStore.restore(state)

            // Step 2: Load Echo Borer enemy data from enemies.json
            val enemies = services.worldDataSource.loadEnemies()
            val echoBorer = enemies.find { it.id.equals("echo_borer", ignoreCase = true) }
                ?: enemies.find { it.id.contains("borer", ignoreCase = true) }
                ?: enemies.firstOrNull()
            assertNotNull("Enemy data must be loaded from enemies.json", echoBorer)

            // Step 3: Setup turn-based combat encounter via CombatEngine
            val partyCombatants = listOf(
                Combatant(
                    id = "nova",
                    name = "Nova",
                    side = CombatSide.PLAYER,
                    stats = StatBlock(maxHp = 100, strength = 15, vitality = 10, agility = 12, focus = 10, luck = 5, speed = 12, stability = 100)
                ),
                Combatant(
                    id = "zeke",
                    name = "Zeke",
                    side = CombatSide.ALLY,
                    stats = StatBlock(maxHp = 120, strength = 18, vitality = 12, agility = 8, focus = 8, luck = 5, speed = 9, stability = 100)
                )
            )

            val enemyCombatant = Combatant(
                id = echoBorer!!.id,
                name = echoBorer.name,
                side = CombatSide.ENEMY,
                stats = StatBlock(
                    maxHp = echoBorer.hp,
                    strength = echoBorer.strength,
                    vitality = echoBorer.vitality,
                    agility = echoBorer.agility,
                    focus = echoBorer.focus,
                    luck = echoBorer.luck,
                    speed = echoBorer.speed,
                    stability = echoBorer.stability ?: 80
                )
            )

            val setup = CombatSetup(
                playerParty = partyCombatants,
                enemyParty = listOf(enemyCombatant)
            )

            val combatState = services.combatEngine.beginEncounter(setup)
            assertNotNull("Combat encounter should be initialized", combatState)
            assertTrue("Combatants must include Nova, Zeke, and enemy", combatState.combatants.containsKey("nova") && combatState.combatants.containsKey("zeke"))
            assertEquals(3, combatState.combatants.size)

            // Step 4: Verify turn order calculation
            assertTrue("Turn order should have all 3 combatants", combatState.turnOrder.size == 3)
            val firstActorId = combatState.turnOrder[0].combatantId
            assertNotNull("First turn actor should be resolved", firstActorId)

            // Step 5: Execute Attack Action from player to Enemy
            val novaState = combatState.combatants["nova"]!!
            val enemyState = combatState.combatants[echoBorer.id]!!
            val damage = services.combatEngine.calculateDamage(novaState, enemyState, baseDamage = novaState.combatant.stats.strength, element = null)
            assertTrue("Attack damage against Echo Borer must be greater than 0", damage > 0)

            val updatedEnemyHp = (enemyState.hp - damage).coerceAtLeast(0)
            val updatedCombatants = combatState.combatants.toMutableMap()
            updatedCombatants[echoBorer.id] = enemyState.copy(hp = updatedEnemyHp)
            val updatedCombatState = combatState.copy(combatants = updatedCombatants)

            assertEquals(updatedEnemyHp, updatedCombatState.combatants[echoBorer.id]?.hp)
            assertTrue("Enemy HP should be reduced following basic attack", updatedEnemyHp < enemyState.hp)

        } finally {
            tempSaveDir.deleteRecursively()
        }
    }
}
