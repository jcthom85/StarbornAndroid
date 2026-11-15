package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.inventory.FieldItemRules
import com.example.starborn.domain.inventory.InventoryEntry
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.ItemEffect
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.StatusDefinition
import com.example.starborn.feature.combat.presentation.CombatItemPresentation
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
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
    fun `authored recovery items heal and cleanse only the selected ally`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        val inventory = InventoryService(object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String) = items[idOrAlias]
        })
        val processor = CombatActionProcessor(engine, statusRegistry, { null },
            consumeItem = inventory::useItem, itemLookup = items::get)
        val ally = player.copy(id = "ally", side = CombatSide.ALLY)
        val initial = engine.beginEncounter(CombatSetup(listOf(player, ally), listOf(enemy)))
        val cases = listOf(
            Triple("herbal_poultice", 45, setOf("weak")),
            Triple("antidote_flask", 0, setOf("corrosion", "poison", "erosion")),
            Triple("burn_salve", 35, setOf("meltdown"))
        )
        for ((id, hp, cleared) in cases) {
            val item = items.getValue(id)
            assertTrue(CombatItemPresentation.isUsable(InventoryEntry(item, 2)))
            assertEquals(TargetRequirement.ALLY, CombatItemPresentation.targetRequirement(InventoryEntry(item, 2)))
            assertEquals(hp == 0, FieldItemRules.isCombatOnly(item, preparedMeal = false))
            inventory.restore(mapOf(id to 2))
            val action = CombatAction.ItemUse(player.id, id, ally.id)
            assertEquals(initial, processor.execute(initial, action) { CombatReward() })
            assertEquals(2, inventory.snapshot()[id])

            val allStatuses = (cleared + "bleed").map { StatusEffect(it, 3) }
            val injured = initial.combatants.getValue(ally.id).copy(hp = 40, statusEffects = allStatuses)
            val active = initial.copy(combatants = initial.combatants + (ally.id to injured))
            assertEquals(active, processor.execute(active, action.copy(targetId = enemy.id)) { CombatReward() })
            assertEquals(2, inventory.snapshot()[id])

            val result = processor.execute(active, action) { CombatReward() }
            val restored = result.combatants.getValue(ally.id)
            assertEquals(40 + hp, restored.hp)
            assertEquals(listOf("bleed"), restored.statusEffects.map { it.id })
            assertEquals(cleared, result.log.filterIsInstance<CombatLogEntry.StatusExpired>()
                .filter { it.cleansed && it.targetId == ally.id }.map { it.statusId }.toSet())
            assertEquals(1, inventory.snapshot()[id])
        }
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
