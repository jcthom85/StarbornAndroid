package com.example.starborn.domain.combat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SupportAbilityTest {

    private val statusRegistry = StatusRegistry()
    private val engine = CombatEngine(statusRegistry = statusRegistry)
    private val processor = CombatActionProcessor(
        engine = engine,
        statusRegistry = statusRegistry,
        skillLookup = { null }
    )

    @Test
    fun `Nova Cheap Shot applies accuracy down to enemy and evasion up to self`() {
        val nova = player("nova")
        val enemy = enemy("enemy")
        val state = engine.beginEncounter(CombatSetup(playerParty = listOf(nova), enemyParty = listOf(enemy)))

        val result = processor.execute(
            state = state,
            action = CombatAction.SupportAbility(actorId = nova.id, targetId = enemy.id)
        ) { CombatReward() }

        val enemyAccuracy = result.combatants.getValue(enemy.id).buffs.firstOrNull { it.effect.stat == "accuracy" }
        assertNotNull(enemyAccuracy)
        assertEquals(-12, enemyAccuracy!!.effect.value)

        val novaEvasion = result.combatants.getValue(nova.id).buffs.firstOrNull { it.effect.stat == "evasion" }
        assertNotNull(novaEvasion)
        assertEquals(5, novaEvasion!!.effect.value)
    }

    @Test
    fun `Zeke Synergy Pitch applies a small barrier buff to an ally`() {
        val zeke = player("zeke")
        val nova = player("nova")
        val enemy = enemy("enemy")
        val state = engine.beginEncounter(CombatSetup(playerParty = listOf(zeke, nova), enemyParty = listOf(enemy)))

        val result = processor.execute(
            state = state,
            action = CombatAction.SupportAbility(actorId = zeke.id, targetId = nova.id)
        ) { CombatReward() }

        val defenseBuff = result.combatants.getValue(nova.id).buffs.firstOrNull { it.effect.stat == "defense" }
        assertNotNull(defenseBuff)
        assertEquals(6, defenseBuff!!.effect.value)
    }

    @Test
    fun `Orion Stasis Stitch heals an ally and grants a small ward buff`() {
        val orion = player("orion")
        val nova = player("nova")
        val enemy = enemy("enemy")
        val base = engine.beginEncounter(CombatSetup(playerParty = listOf(orion, nova), enemyParty = listOf(enemy)))
        val wounded = base.copy(
            combatants = base.combatants + (nova.id to base.combatants.getValue(nova.id).copy(hp = 60))
        )

        val result = processor.execute(
            state = wounded,
            action = CombatAction.SupportAbility(actorId = orion.id, targetId = nova.id)
        ) { CombatReward() }

        val healedHp = result.combatants.getValue(nova.id).hp
        assertEquals(68, healedHp)
        val defenseBuff = result.combatants.getValue(nova.id).buffs.firstOrNull { it.effect.stat == "defense" }
        assertNotNull(defenseBuff)
        assertEquals(5, defenseBuff!!.effect.value)
    }

    @Test
    fun `Gh0st Target Lock marks an enemy and the mark is consumed on hit`() {
        val gh0st = player("gh0st")
        val nova = player(
            "nova",
            weapon = CombatWeapon(
                itemId = "nova_rocket_launcher",
                name = "Rocket Launcher",
                weaponType = "gun",
                attack = WeaponAttack.ChargedSplash(
                    chargeTurns = 0,
                    powerMultiplier = 1.0,
                    splashMultiplier = 0.0
                )
            )
        )
        val enemy = enemy("enemy")
        val base = engine.beginEncounter(CombatSetup(playerParty = listOf(gh0st, nova), enemyParty = listOf(enemy)))

        val marked = processor.execute(
            state = base,
            action = CombatAction.SupportAbility(actorId = gh0st.id, targetId = enemy.id)
        ) { CombatReward() }

        val applied = marked.combatants.getValue(enemy.id).statusEffects.firstOrNull { it.id == "target_lock" }
        assertNotNull(applied)
        assertEquals(2, applied!!.stacks)

        val afterHit = processor.execute(
            state = marked,
            action = CombatAction.BasicAttack(actorId = nova.id, targetId = enemy.id)
        ) { CombatReward() }

        val remaining = afterHit.combatants.getValue(enemy.id).statusEffects.firstOrNull { it.id == "target_lock" }
        assertNotNull(remaining)
        assertEquals(1, remaining!!.stacks)
    }

    private fun player(id: String, weapon: CombatWeapon? = null): Combatant {
        return Combatant(
            id = id,
            name = id,
            side = CombatSide.PLAYER,
            stats = StatBlock(
                maxHp = 100,
                strength = 18,
                vitality = 10,
                agility = 10,
                focus = 10,
                luck = 5,
                speed = 10
            ),
            weapon = weapon
        )
    }

    private fun enemy(id: String): Combatant {
        return Combatant(
            id = id,
            name = id,
            side = CombatSide.ENEMY,
            stats = StatBlock(
                maxHp = 100,
                strength = 8,
                vitality = 6,
                agility = 5,
                focus = 3,
                luck = 3,
                speed = 5
            )
        )
    }
}

