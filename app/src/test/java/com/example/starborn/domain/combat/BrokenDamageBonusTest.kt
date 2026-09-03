package com.example.starborn.domain.combat

import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokenDamageBonusTest {

    @Test
    fun `direct attack deals 25 percent more damage to an already broken target`() {
        val normal = executeAttack(breakTurns = 0, stability = 500, seed = 29)
        val broken = executeAttack(breakTurns = 2, stability = 500, seed = 29)

        assertEquals((normal.amount * 1.25).roundToInt(), broken.amount)
        assertFalse(normal.isBrokenBonus)
        assertTrue(broken.isBrokenBonus)
    }

    @Test
    fun `hit that creates break does not receive the damage bonus`() {
        val breakingHit = executeAttack(breakTurns = 0, stability = 1, seed = 41)

        assertFalse(breakingHit.isBrokenBonus)
    }

    private fun executeAttack(breakTurns: Int, stability: Int, seed: Int): CombatLogEntry.Damage {
        val statusRegistry = StatusRegistry()
        val engine = CombatEngine(statusRegistry = statusRegistry)
        val attacker = combatant("nova", CombatSide.PLAYER, strength = 20, vitality = 4)
        val target = combatant("target", CombatSide.ENEMY, strength = 4, vitality = 0)
        val initial = engine.beginEncounter(CombatSetup(listOf(attacker), listOf(target)))
        val targetState = initial.combatants.getValue(target.id).copy(
            stability = stability,
            breakTurns = breakTurns
        )
        val state = initial.copy(combatants = initial.combatants + (target.id to targetState))
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { null },
            forcePhysicalHit = { _, _ -> true },
            random = SeededCombatRandom(seed)
        )

        return processor.execute(
            state = state,
            action = CombatAction.BasicAttack(attacker.id, target.id)
        ) { CombatReward() }.log.filterIsInstance<CombatLogEntry.Damage>().last()
    }

    private fun combatant(id: String, side: CombatSide, strength: Int, vitality: Int): Combatant {
        return Combatant(
            id = id,
            name = id,
            side = side,
            stats = StatBlock(
                maxHp = 500,
                strength = strength,
                vitality = vitality,
                agility = 8,
                focus = 4,
                luck = 0,
                speed = 8,
                stability = 500
            )
        )
    }
}
