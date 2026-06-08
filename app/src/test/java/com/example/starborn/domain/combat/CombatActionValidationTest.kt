package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.StatusDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatActionValidationTest {

    private val stunDefinition = StatusDefinition(
        id = "stun",
        target = "enemy",
        defaultDuration = 1
    )
    private val statusRegistry = StatusRegistry(listOf(stunDefinition))
    private val engine = CombatEngine(statusRegistry = statusRegistry)

    private val player = Combatant(
        id = "player",
        name = "Player",
        side = CombatSide.PLAYER,
        stats = StatBlock(
            maxHp = 100,
            strength = 18,
            vitality = 12,
            agility = 10,
            focus = 10,
            luck = 10,
            speed = 10
        ),
        weapon = CombatWeapon(
            itemId = "basic_sword",
            name = "Basic Sword",
            weaponType = "sword",
            attack = WeaponAttack.SingleTarget()
        )
    )

    private val enemy = Combatant(
        id = "enemy",
        name = "Enemy",
        side = CombatSide.ENEMY,
        stats = StatBlock(
            maxHp = 100,
            strength = 10,
            vitality = 10,
            agility = 10,
            focus = 10,
            luck = 10,
            speed = 10
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
    fun `skill on cooldown is blocked`() {
        val skill = Skill(
            id = "test_skill",
            name = "Test Skill",
            character = "player",
            type = "active",
            basePower = 50,
            cooldown = 3,
            description = "Deals damage"
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { if (it == skill.id) skill else null }
        )

        val state = baseState()
        val actorState = state.combatants.getValue(player.id)
        val cooldownState = state.copy(
            combatants = state.combatants + (player.id to actorState.copy(
                activeCooldowns = mapOf(skill.id to 2)
            ))
        )

        val result = processor.execute(
            cooldownState,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }

        assertEquals(cooldownState, result)
    }

    @Test
    fun `skill condition hp_below_50 is validated`() {
        val skill = Skill(
            id = "desperate_strike",
            name = "Desperate Strike",
            character = "player",
            type = "active",
            basePower = 80,
            description = "Only usable below 50% HP",
            conditions = listOf("hp_below_50")
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { if (it == skill.id) skill else null }
        )

        val state = baseState()
        val resultHighHp = processor.execute(
            state,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertEquals(state, resultHighHp)

        val actorState = state.combatants.getValue(player.id)
        val lowHpState = state.copy(
            combatants = state.combatants + (player.id to actorState.copy(hp = 40))
        )
        val resultLowHp = processor.execute(
            lowHpState,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }

        assertTrue(resultLowHp.combatants.getValue(enemy.id).hp < 100)
    }

    @Test
    fun `skill condition weapon type equipped is validated`() {
        val skill = Skill(
            id = "slash",
            name = "Slash",
            character = "player",
            type = "active",
            basePower = 50,
            description = "Requires sword",
            conditions = listOf("sword_equipped")
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { if (it == skill.id) skill else null }
        )

        val state = baseState()
        
        val resultWithSword = processor.execute(
            state,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertTrue(resultWithSword.combatants.getValue(enemy.id).hp < 100)

        val actorState = state.combatants.getValue(player.id)
        val unarmedActor = actorState.copy(
            combatant = actorState.combatant.copy(
                weapon = CombatWeapon(
                    itemId = "basic_glove",
                    name = "Basic Glove",
                    weaponType = "glove",
                    attack = WeaponAttack.SingleTarget()
                )
            )
        )
        val otherWeaponState = state.copy(
            combatants = state.combatants + (player.id to unarmedActor)
        )

        val resultWithGlove = processor.execute(
            otherWeaponState,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertEquals(otherWeaponState, resultWithGlove)
    }

    @Test
    fun `skill condition target_stunned is validated`() {
        val skill = Skill(
            id = "exploit",
            name = "Exploit",
            character = "player",
            type = "active",
            basePower = 100,
            description = "Requires stunned target",
            conditions = listOf("target_stunned")
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { if (it == skill.id) skill else null }
        )

        val state = baseState()
        
        val resultNotStunned = processor.execute(
            state,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertEquals(state, resultNotStunned)

        val enemyState = state.combatants.getValue(enemy.id)
        val stunnedEnemyState = state.copy(
            combatants = state.combatants + (enemy.id to enemyState.copy(
                statusEffects = listOf(StatusEffect("stun", 1))
            ))
        )
        val resultStunned = processor.execute(
            stunnedEnemyState,
            CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertTrue(resultStunned.combatants.getValue(enemy.id).hp < 100)
    }

    @Test
    fun `snack on cooldown is blocked`() {
        val snacks = mapOf(
            "heal_snack" to Item(
                id = "heal_snack",
                name = "Heal Snack",
                type = "snack",
                effect = ItemEffect(
                    restoreHp = 20,
                    target = "self",
                    cooldown = 3
                )
            )
        )
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = statusRegistry,
            skillLookup = { null },
            itemLookup = { id -> snacks[id] }
        )

        val state = baseState()
        val actorState = state.combatants.getValue(player.id)
        val cooldownState = state.copy(
            combatants = state.combatants + (player.id to actorState.copy(
                snackCooldown = 2
            ))
        )

        val result = processor.execute(
            cooldownState,
            CombatAction.SnackUse(actorId = player.id, snackItemId = "heal_snack")
        ) { CombatReward() }

        assertEquals(cooldownState, result)
    }
}
