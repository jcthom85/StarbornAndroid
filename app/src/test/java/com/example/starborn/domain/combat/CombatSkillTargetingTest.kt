package com.example.starborn.domain.combat

import com.example.starborn.domain.model.StatusDefinition
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class CombatSkillTargetingTest {

    private val shieldDefinition = StatusDefinition(
        id = "shield",
        target = "ally",
        defaultDuration = 2
    )

    private val poisonDefinition = StatusDefinition(
        id = "poison",
        target = "enemy",
        defaultDuration = 3
    )

    private val statusRegistry = StatusRegistry(listOf(shieldDefinition, poisonDefinition))
    private val engine = CombatEngine(statusRegistry = statusRegistry)

    private val player = Combatant(
        id = "player",
        name = "Player",
        side = CombatSide.PLAYER,
        stats = StatBlock(
            maxHp = 100,
            strength = 18,
            vitality = 12,
            agility = 8,
            focus = 6,
            luck = 5,
            speed = 10
        )
    )

    private val enemy = Combatant(
        id = "enemy",
        name = "Enemy",
        side = CombatSide.ENEMY,
        stats = StatBlock(
            maxHp = 80,
            strength = 10,
            vitality = 8,
            agility = 6,
            focus = 4,
            luck = 3,
            speed = 9
        )
    )

    private fun baseState(): CombatState {
        return engine.beginEncounter(
            CombatSetup(
                playerParty = listOf(player),
                enemyParty = listOf(enemy)
            )
        )
    }

    @Test
    fun `ally targeted status applies to the player when no explicit target provided`() {
        val supportSkill = com.example.starborn.domain.model.Skill(
            id = "skill_shield",
            name = "Smoke Screen",
            character = "player",
            type = "active",
            basePower = 0,
            cooldown = 0,
            description = "Apply shield to self.",
            combatTags = listOf("support"),
            statusApplications = listOf("shield")
        )

        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { if (it == supportSkill.id) supportSkill else null }
        )

        val initial = baseState()
        val result = processor.execute(
            initial,
            CombatAction.SkillUse(
                actorId = player.id,
                skillId = supportSkill.id,
                targetIds = emptyList()
            )
        ) { CombatReward() }

        val playerStatuses = result.combatants.getValue(player.id).statusEffects
        assertTrue(playerStatuses.any { it.id == "shield" })
    }

    @Test
    fun `enemy targeted status applies to selected enemy`() {
        val poisonSkill = com.example.starborn.domain.model.Skill(
            id = "skill_poison",
            name = "Toxic Blast",
            character = "player",
            type = "active",
            basePower = 0,
            cooldown = 0,
            description = "Poison a foe.",
            combatTags = listOf("dmg"),
            statusApplications = listOf("poison")
        )

        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { if (it == poisonSkill.id) poisonSkill else null }
        )

        val initial = baseState()
        val result = processor.execute(
            initial,
            CombatAction.SkillUse(
                actorId = player.id,
                skillId = poisonSkill.id,
                targetIds = listOf(enemy.id)
            )
        ) { CombatReward() }

        val enemyStatuses = result.combatants.getValue(enemy.id).statusEffects
        assertTrue(enemyStatuses.any { it.id == "poison" })
        val playerStatuses = result.combatants.getValue(player.id).statusEffects
        assertEquals(0, playerStatuses.size)
    }
}
