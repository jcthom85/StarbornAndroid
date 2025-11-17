package com.example.starborn.domain.combat

import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.domain.model.StatusTick
import org.junit.Assert.assertEquals
import org.junit.Test

class CombatStatusTickTest {

    private val statusRegistry = StatusRegistry(listOf(
        StatusDefinition(
            id = "poison",
            tick = StatusTick(mode = "damage", perMaxHpDivisor = 10, min = 1, element = "poison"),
            defaultDuration = 3
        ),
        StatusDefinition(
            id = "regen",
            tick = StatusTick(mode = "heal", perMaxHpDivisor = 12, min = 1),
            defaultDuration = 3
        )
    ))

    private val engine = CombatEngine(statusRegistry = statusRegistry)

    private val player = Combatant(
        id = "player",
        name = "Player",
        side = CombatSide.PLAYER,
        stats = StatBlock(
            maxHp = 100,
            maxRp = 10,
            strength = 20,
            vitality = 10,
            agility = 8,
            focus = 6,
            luck = 5,
            speed = 12
        )
    )

    private val enemy = Combatant(
        id = "enemy",
        name = "Enemy",
        side = CombatSide.ENEMY,
        stats = StatBlock(
            maxHp = 80,
            maxRp = 5,
            strength = 12,
            vitality = 8,
            agility = 6,
            focus = 4,
            luck = 3,
            speed = 10
        )
    )

    private fun baseState(): CombatState {
        return engine.beginEncounter(
            CombatSetup(playerParty = listOf(player), enemyParty = listOf(enemy))
        )
    }

    @Test
    fun `poison status deals damage over time`() {
        val initial = baseState()
        val poisoned = initial.copy(
            combatants = initial.combatants + (
                "player" to initial.combatants.getValue("player").copy(
                    statusEffects = listOf(StatusEffect(id = "poison", remainingTurns = 2))
                )
            )
        )

        val ticked = engine.tickEndOfTurn(poisoned)
        val updated = ticked.combatants.getValue("player")
        // poison deals 10% of max HP (100) = 10 damage
        assertEquals(90, updated.hp)
        // duration should decrement
        assertEquals(1, updated.statusEffects.first().remainingTurns)
    }

    @Test
    fun `regen status restores health over time`() {
        val initial = baseState()
        val damaged = initial.copy(
            combatants = initial.combatants + (
                "player" to initial.combatants.getValue("player").copy(
                    hp = 60,
                    statusEffects = listOf(StatusEffect(id = "regen", remainingTurns = 2))
                )
            )
        )

        val ticked = engine.tickEndOfTurn(damaged)
        val updated = ticked.combatants.getValue("player")
        // regen restores roughly 1/12th (8) but cannot exceed max hp
        assertEquals(68, updated.hp)
        assertEquals(1, updated.statusEffects.first().remainingTurns)
    }
}
