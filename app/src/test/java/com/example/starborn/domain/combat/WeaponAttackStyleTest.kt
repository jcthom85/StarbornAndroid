package com.example.starborn.domain.combat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaponAttackStyleTest {

    private val statusRegistry = StatusRegistry()
    private val engine = CombatEngine(statusRegistry = statusRegistry)
    private val processor = CombatActionProcessor(
        engine = engine,
        statusRegistry = statusRegistry,
        skillLookup = { null }
    )

    @Test
    fun `all-enemies weapon attack processes each enemy`() {
        val attacker = Combatant(
            id = "nova",
            name = "Nova",
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
            weapon = CombatWeapon(
                itemId = "nova_plasma_shotgun",
                name = "Plasma Shotgun",
                weaponType = "gun",
                attack = WeaponAttack.AllEnemies(powerMultiplier = 0.7)
            )
        )
        val enemies = listOf(
            stubEnemy("enemy_a"),
            stubEnemy("enemy_b"),
            stubEnemy("enemy_c")
        )
        val state = engine.beginEncounter(
            CombatSetup(
                playerParty = listOf(attacker),
                enemyParty = enemies
            )
        )

        val result = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(attacker.id, enemies.first().id)
        ) { CombatReward() }

        val damageTargets = result.log
            .filterIsInstance<CombatLogEntry.Damage>()
            .filter { it.sourceId == attacker.id }
            .map { it.targetId }
            .toSet()
        assertEquals(enemies.map { it.id }.toSet(), damageTargets)
    }

    @Test
    fun `charged splash weapon attack requires a charge turn`() {
        val attacker = Combatant(
            id = "nova",
            name = "Nova",
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
            weapon = CombatWeapon(
                itemId = "nova_rocket_launcher",
                name = "Rocket Launcher",
                weaponType = "gun",
                attack = WeaponAttack.ChargedSplash(
                    chargeTurns = 1,
                    powerMultiplier = 1.6,
                    splashMultiplier = 0.5
                )
            )
        )
        val enemies = listOf(
            stubEnemy("enemy_a"),
            stubEnemy("enemy_b"),
            stubEnemy("enemy_c")
        )
        val state = engine.beginEncounter(
            CombatSetup(
                playerParty = listOf(attacker),
                enemyParty = enemies
            )
        )

        val charged = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(attacker.id, enemies.first().id)
        ) { CombatReward() }

        assertNotNull(charged.combatants.getValue(attacker.id).weaponCharge)
        assertTrue(charged.log.filterIsInstance<CombatLogEntry.Damage>().isEmpty())
        assertTrue(
            charged.log.any { entry ->
                entry is CombatLogEntry.TurnSkipped && entry.actorId == attacker.id && entry.reason.contains("charging", ignoreCase = true)
            }
        )

        val beforeSecond = charged.log.size
        val fired = processor.execute(
            state = charged,
            action = CombatAction.BasicAttack(attacker.id, enemies.first().id)
        ) { CombatReward() }

        assertNull(fired.combatants.getValue(attacker.id).weaponCharge)

        val newEntries = fired.log.drop(beforeSecond)
        val damageTargets = newEntries
            .filterIsInstance<CombatLogEntry.Damage>()
            .filter { it.sourceId == attacker.id }
            .map { it.targetId }
            .toSet()
        assertEquals(enemies.map { it.id }.toSet(), damageTargets)
    }

    private fun stubEnemy(id: String): Combatant {
        return Combatant(
            id = id,
            name = id,
            side = CombatSide.ENEMY,
            stats = StatBlock(
                maxHp = 50,
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

