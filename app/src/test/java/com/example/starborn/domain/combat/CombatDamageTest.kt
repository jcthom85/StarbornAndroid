package com.example.starborn.domain.combat

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.max

class CombatDamageTest {

    private val engine = CombatEngine()

    private val player = Combatant(
        id = "player",
        name = "Player",
        side = CombatSide.PLAYER,
        stats = StatBlock(
            maxHp = 100,
            strength = 20,
            vitality = 10,
            agility = 10,
            focus = 10,
            luck = 5,
            speed = 12
        )
    )

    private val enemyNeutral = Combatant(
        id = "enemy_neutral",
        name = "Neutral",
        side = CombatSide.ENEMY,
        stats = StatBlock(70, 8, 6, 6, 6, 4, 8),
        resistances = ResistanceProfile(physical = 0)
    )

    private val enemyResistant = Combatant(
        id = "enemy_resistant",
        name = "Resistant",
        side = CombatSide.ENEMY,
        stats = StatBlock(70, 8, 6, 6, 6, 4, 8),
        resistances = ResistanceProfile(physical = 50, fire = 25)
    )

    private val enemyVulnerable = Combatant(
        id = "enemy_vulnerable",
        name = "Vulnerable",
        side = CombatSide.ENEMY,
        stats = StatBlock(70, 8, 6, 6, 6, 4, 8),
        resistances = ResistanceProfile(physical = -20, fire = -50)
    )

    private fun scenario(engine: CombatEngine, defender: Combatant): CombatState {
        val setup = CombatSetup(
            playerParty = listOf(player),
            enemyParty = listOf(defender)
        )
        return engine.beginEncounter(setup)
    }

    @Test
    fun `physical damage applies resistance`() {
        val base = scenario(engine, enemyNeutral)
        val resist = scenario(engine, enemyResistant)
        val weak = scenario(engine, enemyVulnerable)

        val baseState = engine.queueAction(
            base,
            CombatAction.BasicAttack("player", "enemy_neutral")
        )
        val resistState = engine.queueAction(
            resist,
            CombatAction.BasicAttack("player", "enemy_resistant")
        )
        val weakState = engine.queueAction(
            weak,
            CombatAction.BasicAttack("player", "enemy_vulnerable")
        )

        val neutralDamage = baseState.calculateDamage("player", "enemy_neutral")
        val resistantDamage = resistState.calculateDamage("player", "enemy_resistant")
        val weakDamage = weakState.calculateDamage("player", "enemy_vulnerable")

        assertEquals(20, neutralDamage)
        assertEquals(10, resistantDamage)
        assertEquals(24, weakDamage)
    }

    @Test
    fun `element damage uses specific resistance`() {
        val resist = scenario(engine, enemyResistant)
        val weak = scenario(engine, enemyVulnerable)

        val resistState = engine.queueAction(
            resist,
            CombatAction.SkillUse("player", "skill_fire", listOf("enemy_resistant"))
        )
        val weakState = engine.queueAction(
            weak,
            CombatAction.SkillUse("player", "skill_fire", listOf("enemy_vulnerable"))
        )

        val baseDamage = 20
        val resistant = resistState.calculateDamage("player", "enemy_resistant", baseDamage, element = "fire")
        val vulnerable = weakState.calculateDamage("player", "enemy_vulnerable", baseDamage, element = "fire")

        assertEquals(15, resistant)
        assertEquals(30, vulnerable)
    }

    private fun CombatState.calculateDamage(
        attackerId: String,
        targetId: String,
        baseDamage: Int = this.combatants.getValue(attackerId).combatant.stats.strength,
        element: String? = null
    ): Int {
        val attacker = combatants.getValue(attackerId)
        val target = combatants.getValue(targetId)
        return engine.calculateDamage(
            attackerState = attacker,
            targetState = target,
            baseDamage = max(baseDamage, 1),
            element = element
        )
    }
}
