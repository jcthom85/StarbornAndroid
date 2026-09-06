package com.example.starborn.domain.combat

import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.StatusDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaponMomentumAndOverchargeTest {

    private val statusRegistry = StatusRegistry(
        listOf(
            StatusDefinition(
                id = "acid",
                name = "Acid",
                defaultDuration = 3
            ),
            StatusDefinition(
                id = "shock",
                name = "Shock",
                defaultDuration = 2
            )
        )
    )

    private val engine = CombatEngine(statusRegistry = statusRegistry)

    private val testSkills = mutableMapOf<String, Skill>()

    private val processor = CombatActionProcessor(
        engine = engine,
        statusRegistry = statusRegistry,
        skillLookup = { testSkills[it] },
        forcePhysicalHit = { _, _ -> true } // force hits to avoid miss variance in tests
    )

    private fun createPlayer(
        id: String = "nova",
        weapon: CombatWeapon? = null
    ): Combatant {
        return Combatant(
            id = id,
            name = "Nova",
            side = CombatSide.PLAYER,
            stats = StatBlock(
                maxHp = 100,
                strength = 20,
                vitality = 10,
                agility = 10,
                focus = 10,
                luck = 5,
                speed = 10
            ),
            weapon = weapon ?: CombatWeapon(
                itemId = "test_pistol",
                name = "Test Pistol",
                weaponType = "pistol",
                attack = WeaponAttack.SingleTarget(powerMultiplier = 1.0),
                minDamage = 10,
                maxDamage = 10
            )
        )
    }

    private fun createEnemy(id: String = "enemy"): Combatant {
        return Combatant(
            id = id,
            name = "Enemy Target",
            side = CombatSide.ENEMY,
            stats = StatBlock(
                maxHp = 500,
                strength = 10,
                vitality = 10,
                agility = 5,
                focus = 5,
                luck = 0,
                speed = 5
            ),
            resistances = ResistanceProfile(
                physical = 0,
                shock = -100 // WEAKNESS tier
            )
        )
    }

    @Test
    fun `basic attack increments player momentum up to cap of 3`() {
        val player = createPlayer()
        val enemy = createEnemy()
        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))

        assertEquals(0, state.combatants.getValue(player.id).momentum)

        // Strike 1
        state = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(player.id, enemy.id)
        ) { CombatReward() }
        assertEquals(1, state.combatants.getValue(player.id).momentum)

        // Strike 2
        state = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(player.id, enemy.id)
        ) { CombatReward() }
        assertEquals(2, state.combatants.getValue(player.id).momentum)

        // Strike 3
        state = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(player.id, enemy.id)
        ) { CombatReward() }
        assertEquals(3, state.combatants.getValue(player.id).momentum)

        // Strike 4 (should remain capped at 3)
        state = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(player.id, enemy.id)
        ) { CombatReward() }
        assertEquals(3, state.combatants.getValue(player.id).momentum)
    }

    @Test
    fun `enemy attacks do not generate momentum`() {
        val player = createPlayer()
        val enemy = createEnemy()
        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))

        state = processor.execute(
            state = state,
            action = CombatAction.BasicAttack(enemy.id, player.id)
        ) { CombatReward() }

        assertEquals(0, state.combatants.getValue(enemy.id).momentum)
        assertEquals(0, state.combatants.getValue(player.id).momentum)
    }

    @Test
    fun `ability use consumes all momentum and resets to 0`() {
        val player = createPlayer()
        val enemy = createEnemy()
        val skill = Skill(
            id = "strike_skill",
            name = "Heavy Strike",
            character = "nova",
            description = "Test strike",
            type = "damage",
            basePower = 100,
            cooldown = 0
        )
        testSkills[skill.id] = skill

        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        // Build 2 momentum
        state = state.copy(
            combatants = state.combatants + (player.id to state.combatants.getValue(player.id).copy(momentum = 2))
        )
        assertEquals(2, state.combatants.getValue(player.id).momentum)

        // Cast skill
        state = processor.execute(
            state = state,
            action = CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }

        assertEquals(0, state.combatants.getValue(player.id).momentum)
    }

    @Test
    fun `ability damage scales with momentum tier`() {
        val skill = Skill(
            id = "test_damage",
            name = "Test Damage",
            character = "nova",
            description = "Test damage scaling",
            type = "damage",
            basePower = 100,
            scaling = "str"
        )
        testSkills[skill.id] = skill

        fun executeSkillAtMomentum(momentum: Int): Int {
            val player = createPlayer()
            val enemy = createEnemy()
            var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
            state = state.copy(
                combatants = state.combatants + (player.id to state.combatants.getValue(player.id).copy(momentum = momentum))
            )
            val logBefore = state.log.size
            state = processor.execute(
                state = state,
                action = CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
            ) { CombatReward() }
            val damageEntry = state.log.drop(logBefore).filterIsInstance<CombatLogEntry.Damage>().first()
            return damageEntry.amount
        }

        val baseDamage = executeSkillAtMomentum(0)
        val m1Damage = executeSkillAtMomentum(1)
        val m2Damage = executeSkillAtMomentum(2)
        val m3Damage = executeSkillAtMomentum(3)

        // 1 momentum: ~1.25x
        assertTrue("m1 damage ($m1Damage) should be greater than base ($baseDamage)", m1Damage > baseDamage)
        // 2 momentum: ~1.50x
        assertTrue("m2 damage ($m2Damage) should be greater than m1 ($m1Damage)", m2Damage > m1Damage)
        // 3 momentum: ~1.80x * 2.0x (forced crit) = ~3.6x
        assertTrue("m3 damage ($m3Damage) should be much higher due to 1.8x + forced crit", m3Damage >= m2Damage * 2)
    }

    @Test
    fun `cooldown refund applies at 2 or 3 momentum`() {
        val skill = Skill(
            id = "cd_skill",
            name = "Cooldown Skill",
            character = "nova",
            description = "Test CD refund",
            type = "damage",
            basePower = 50,
            cooldown = 3
        )
        testSkills[skill.id] = skill

        val player = createPlayer()
        val enemy = createEnemy()

        // Test with 0 momentum: active cooldown becomes 3 after turn ends (cooldown + 1 - 1 tick = 3)
        var state0 = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        state0 = processor.execute(
            state = state0,
            action = CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertEquals(3, state0.combatants.getValue(player.id).activeCooldowns[skill.id])

        // Test with 2 momentum: effective cooldown is 3 - 1 = 2, so after turn ends it is 2
        var state2 = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        state2 = state2.copy(
            combatants = state2.combatants + (player.id to state2.combatants.getValue(player.id).copy(momentum = 2))
        )
        state2 = processor.execute(
            state = state2,
            action = CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }
        assertEquals(2, state2.combatants.getValue(player.id).activeCooldowns[skill.id])
    }

    @Test
    fun `3 momentum forces critical hit and logs momentumSpent`() {
        val skill = Skill(
            id = "crit_skill",
            name = "Crit Skill",
            character = "nova",
            description = "Test crit",
            type = "damage",
            basePower = 50
        )
        testSkills[skill.id] = skill

        val player = createPlayer()
        val enemy = createEnemy()
        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        state = state.copy(
            combatants = state.combatants + (player.id to state.combatants.getValue(player.id).copy(momentum = 3))
        )
        val beforeLog = state.log.size
        state = processor.execute(
            state = state,
            action = CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }

        val damageEntry = state.log.drop(beforeLog).filterIsInstance<CombatLogEntry.Damage>().first()
        assertTrue("Hit should be critical at 3 momentum", damageEntry.critical)
        assertEquals("momentumSpent should be logged as 3", 3, damageEntry.momentumSpent)
    }

    @Test
    fun `physical skill inherits weapon element and weapon mod status`() {
        val shockWeapon = CombatWeapon(
            itemId = "shock_pistol",
            name = "Shock Pistol",
            weaponType = "pistol",
            attack = WeaponAttack.SingleTarget(element = "shock"),
            statusOnHit = "acid",
            statusChance = 100.0
        )
        val player = createPlayer(weapon = shockWeapon)
        val enemy = createEnemy()

        val skill = Skill(
            id = "scrap_shot",
            name = "Scrap Shot",
            character = "nova",
            description = "Test weapon infusion",
            type = "damage",
            basePower = 60
            // No explicit element tag
        )
        testSkills[skill.id] = skill

        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        val beforeLog = state.log.size
        state = processor.execute(
            state = state,
            action = CombatAction.SkillUse(actorId = player.id, skillId = skill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }

        val damageEntry = state.log.drop(beforeLog).filterIsInstance<CombatLogEntry.Damage>().first()
        assertEquals("Should inherit shock element from weapon", "shock", damageEntry.element)
        assertTrue("Enemy is vulnerable to shock, so should be weakness", damageEntry.isWeakness)

        // Check weapon mod status (acid) was applied to enemy
        val enemyState = state.combatants.getValue(enemy.id)
        assertTrue("Enemy should have acid status applied from weapon mod", enemyState.statusEffects.any { it.id == "acid" })
    }

    @Test
    fun `explicit skill element is not overridden by weapon element`() {
        val shockWeapon = CombatWeapon(
            itemId = "shock_pistol",
            name = "Shock Pistol",
            weaponType = "pistol",
            attack = WeaponAttack.SingleTarget(element = "shock")
        )
        val player = createPlayer(weapon = shockWeapon)
        val enemy = createEnemy()

        val fireSkill = Skill(
            id = "fire_strike",
            name = "Fire Strike",
            character = "nova",
            description = "Test explicit element preservation",
            type = "damage",
            basePower = 60,
            combatTags = listOf("burn") // explicit element
        )
        testSkills[fireSkill.id] = fireSkill

        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        val beforeLog = state.log.size
        state = processor.execute(
            state = state,
            action = CombatAction.SkillUse(actorId = player.id, skillId = fireSkill.id, targetIds = listOf(enemy.id))
        ) { CombatReward() }

        val damageEntry = state.log.drop(beforeLog).filterIsInstance<CombatLogEntry.Damage>().first()
        assertEquals("Should preserve skill's own element", "burn", damageEntry.element)
    }

    @Test
    fun `heal skill scales with momentum and logs momentumSpent`() {
        val healSkill = Skill(
            id = "field_medic",
            name = "Field Medic",
            character = "nova",
            description = "Test heal momentum",
            type = "heal",
            basePower = 40
        )
        testSkills[healSkill.id] = healSkill

        fun executeHeal(momentum: Int): CombatLogEntry.Heal {
            val player = createPlayer()
            val enemy = createEnemy()
            var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
            state = state.copy(
                combatants = state.combatants + (player.id to state.combatants.getValue(player.id).copy(hp = 20, momentum = momentum))
            )
            val beforeLog = state.log.size
            state = processor.execute(
                state = state,
                action = CombatAction.SkillUse(actorId = player.id, skillId = healSkill.id, targetIds = listOf(player.id))
            ) { CombatReward() }
            return state.log.drop(beforeLog).filterIsInstance<CombatLogEntry.Heal>().first()
        }

        val heal0 = executeHeal(0)
        val heal2 = executeHeal(2)

        assertEquals(0, heal0.momentumSpent)
        assertEquals(2, heal2.momentumSpent)
        assertTrue("Heal with 2 momentum should be ~1.5x of base heal", heal2.amount > heal0.amount)
        assertEquals((heal0.amount * 1.5).toInt(), heal2.amount)
    }
}
