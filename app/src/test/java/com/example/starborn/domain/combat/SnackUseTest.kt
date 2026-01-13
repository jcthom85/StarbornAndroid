package com.example.starborn.domain.combat

import com.example.starborn.domain.model.BuffEffect
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackUseTest {

    private val statusRegistry = StatusRegistry()
    private val engine = CombatEngine(statusRegistry = statusRegistry)

    @Test
    fun `snack restore heals the chosen ally`() {
        val snacks = mapOf(
            "heal_snack" to Item(
                id = "heal_snack",
                name = "Heal Snack",
                type = "snack",
                effect = ItemEffect(
                    restoreHp = 20,
                    target = "ally"
                )
            )
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { null },
            itemLookup = { id -> snacks[id] }
        )

        val orion = player("orion")
        val nova = player("nova")
        val enemy = enemy("enemy")
        val base = engine.beginEncounter(CombatSetup(playerParty = listOf(orion, nova), enemyParty = listOf(enemy)))
        val wounded = base.copy(combatants = base.combatants + (nova.id to base.combatants.getValue(nova.id).copy(hp = 50)))

        val result = processor.execute(
            state = wounded,
            action = CombatAction.SnackUse(actorId = orion.id, snackItemId = "heal_snack", targetId = nova.id)
        ) { CombatReward() }

        assertEquals(70, result.combatants.getValue(nova.id).hp)
    }

    @Test
    fun `snack buff applies a temporary buff to self`() {
        val snacks = mapOf(
            "buff_snack" to Item(
                id = "buff_snack",
                name = "Buff Snack",
                type = "snack",
                effect = ItemEffect(
                    singleBuff = BuffEffect(stat = "defense", value = 6, duration = 2),
                    target = "self"
                )
            )
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { null },
            itemLookup = { id -> snacks[id] }
        )

        val nova = player("nova")
        val enemy = enemy("enemy")
        val state = engine.beginEncounter(CombatSetup(playerParty = listOf(nova), enemyParty = listOf(enemy)))

        val result = processor.execute(
            state = state,
            action = CombatAction.SnackUse(actorId = nova.id, snackItemId = "buff_snack")
        ) { CombatReward() }

        val buff = result.combatants.getValue(nova.id).buffs.firstOrNull { it.effect.stat == "defense" }
        assertNotNull(buff)
        assertEquals(6, buff!!.effect.value)
    }

    @Test
    fun `snack debuff applies a negative buff to an enemy`() {
        val snacks = mapOf(
            "debuff_snack" to Item(
                id = "debuff_snack",
                name = "Debuff Snack",
                type = "snack",
                effect = ItemEffect(
                    singleBuff = BuffEffect(stat = "defense", value = -6, duration = 2),
                    target = "enemy"
                )
            )
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { null },
            itemLookup = { id -> snacks[id] }
        )

        val nova = player("nova")
        val enemy = enemy("enemy")
        val state = engine.beginEncounter(CombatSetup(playerParty = listOf(nova), enemyParty = listOf(enemy)))

        val result = processor.execute(
            state = state,
            action = CombatAction.SnackUse(actorId = nova.id, snackItemId = "debuff_snack", targetId = enemy.id)
        ) { CombatReward() }

        val debuff = result.combatants.getValue(enemy.id).buffs.firstOrNull { it.effect.stat == "defense" }
        assertNotNull(debuff)
        assertEquals(-6, debuff!!.effect.value)
    }

    @Test
    fun `snack damage reduces enemy hp`() {
        val snacks = mapOf(
            "damage_snack" to Item(
                id = "damage_snack",
                name = "Damage Snack",
                type = "snack",
                effect = ItemEffect(
                    damage = 15,
                    target = "enemy"
                )
            )
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { null },
            itemLookup = { id -> snacks[id] }
        )

        val nova = player("nova")
        val enemy = enemy("enemy")
        val state = engine.beginEncounter(CombatSetup(playerParty = listOf(nova), enemyParty = listOf(enemy)))

        val result = processor.execute(
            state = state,
            action = CombatAction.SnackUse(actorId = nova.id, snackItemId = "damage_snack", targetId = enemy.id)
        ) { CombatReward() }

        val updatedHp = result.combatants.getValue(enemy.id).hp
        assertEquals(85, updatedHp)

        val damageEntry = result.log.filterIsInstance<CombatLogEntry.Damage>()
            .firstOrNull { it.sourceId == nova.id && it.targetId == enemy.id && it.amount > 0 }
        assertNotNull(damageEntry)
        assertTrue(damageEntry!!.amount > 0)
    }

    private fun player(id: String): Combatant {
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
            )
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

