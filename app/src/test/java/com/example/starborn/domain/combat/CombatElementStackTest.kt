package com.example.starborn.domain.combat

import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.model.StatusTick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

class CombatElementStackTest {

    private val statusRegistry = StatusRegistry(
        listOf(
            StatusDefinition(
                id = "burn",
                tick = StatusTick(mode = "damage", amount = 4, element = "fire")
            )
        )
    )

    private val engine = CombatEngine(statusRegistry = statusRegistry)

    private val player = Combatant(
        id = "player",
        name = "Player",
        side = CombatSide.PLAYER,
        stats = StatBlock(120, 18, 12, 10, 10, 5, 12)
    )

    private val enemyAlpha = Combatant(
        id = "enemy_a",
        name = "Alpha",
        side = CombatSide.ENEMY,
        stats = StatBlock(90, 12, 10, 10, 8, 6, 8)
    )

    private val enemyBeta = Combatant(
        id = "enemy_b",
        name = "Beta",
        side = CombatSide.ENEMY,
        stats = StatBlock(75, 10, 10, 8, 8, 5, 7)
    )

    @Test
    fun `element stacks trigger fire burst on threshold`() {
        var state = startCombat(enemyAlpha, enemyBeta)
        repeat(ElementalStackRules.STACK_THRESHOLD - 1) {
            state = engine.applyDamage(state, "player", "enemy_a", amount = 10, element = "fire")
        }
        val afterSecond = state.combatants.getValue("enemy_a").elementStacks["fire"]
        assertEquals(2, afterSecond)

        val betaBefore = state.combatants.getValue("enemy_b").hp
        state = engine.applyDamage(state, "player", "enemy_a", amount = 10, element = "fire")

        val alphaStacks = state.combatants.getValue("enemy_a").elementStacks["fire"]
        assertNull(alphaStacks)
        val expectedSplash = max(1, enemyAlpha.stats.maxHp / 10)
        val betaAfter = state.combatants.getValue("enemy_b").hp
        assertEquals(betaBefore - expectedSplash, betaAfter)

        val burstEntry = state.log.filterIsInstance<CombatLogEntry.ElementBurst>().lastOrNull()
        assertTrue(burstEntry != null && burstEntry.element == "fire" && burstEntry.targetId == "enemy_a")
    }

    @Test
    fun `status damage does not fuel stacks`() {
        var state = startCombat(enemyAlpha)
        state = engine.applyStatus(
            state = state,
            targetId = "enemy_a",
            statusId = "burn",
            duration = 1,
            stacks = 1
        )
        state = engine.tickEndOfTurn(state)
        val stacks = state.combatants.getValue("enemy_a").elementStacks["fire"]
        assertNull(stacks)
    }

    private fun startCombat(vararg enemies: Combatant): CombatState {
        val setup = CombatSetup(
            playerParty = listOf(player),
            enemyParty = enemies.toList()
        )
        return engine.beginEncounter(setup)
    }
}
