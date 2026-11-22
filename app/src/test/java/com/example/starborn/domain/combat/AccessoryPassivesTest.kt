package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.TinkeringRecipe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessoryPassivesTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    private val registry = StatusRegistry(assets.loadStatuses())
    private val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(23))

    private val deterministicRandom = object : CombatRandom {
        override fun nextDouble(from: Double, until: Double) = 50.0.coerceIn(from, until - 0.0001)
        override fun nextInt(from: Int, until: Int) = from
    }

    private val testSkills: Map<String, Skill> = mapOf(
        "test_strike" to Skill(
            id = "test_strike",
            name = "Test Strike",
            character = "nova",
            type = "physical",
            basePower = 100,
            cooldown = 0,
            description = "Single target strike",
            targeting = "single_enemy"
        ),
        "test_debuff" to Skill(
            id = "test_debuff",
            name = "Test Debuff",
            character = "orion",
            type = "tech",
            basePower = 60,
            cooldown = 0,
            description = "Applies burn",
            statusApplications = listOf("burn"),
            targeting = "single_enemy"
        ),
        "test_heal" to Skill(
            id = "test_heal",
            name = "Test Heal",
            character = "orion",
            type = "heal",
            basePower = 120,
            cooldown = 0,
            description = "Heals ally",
            targeting = "single_ally"
        ),
        "test_heavy" to Skill(
            id = "test_heavy",
            name = "Test Heavy",
            character = "nova",
            type = "physical",
            basePower = 140,
            cooldown = 3,
            description = "Heavy strike",
            targeting = "single_enemy"
        ),
        "enemy_hit" to Skill(
            id = "enemy_hit",
            name = "Enemy Hit",
            character = "enemy",
            type = "physical",
            basePower = 80,
            cooldown = 0,
            description = "Enemy attack",
            targeting = "single_enemy"
        )
    )

    private fun processor(rng: CombatRandom = deterministicRandom) = CombatActionProcessor(
        engine = engine,
        statusRegistry = registry,
        skillLookup = testSkills::get,
        forcePhysicalHit = { _, _ -> true },
        random = rng,
        explicitStatusSkillIds = setOf("test_debuff"),
        prototypeMode = true
    )

    private fun combatant(
        id: String,
        side: CombatSide,
        maxHp: Int = 1000,
        attack: Int = 80,
        defense: Int = 0,
        stability: Int = 100,
        passiveId: String? = null,
        accessoryName: String? = null
    ) = Combatant(
        id = id,
        name = id,
        side = side,
        stats = StatBlock(
            maxHp = maxHp,
            strength = attack,
            vitality = 20,
            agility = 20,
            focus = 40,
            luck = 10,
            speed = 10,
            stability = stability,
            flatDamageReduction = defense,
            statusResistance = 0
        ),
        accessoryPassiveId = passiveId,
        accessoryName = accessoryName
    )

    @Test
    fun allAccessoriesInItemsJsonHaveValidPassiveIdAndText() {
        val validIds = setOf(
            AccessoryPassives.ATB_HEADSTART_20,
            AccessoryPassives.ATB_HEADSTART_EVASION,
            AccessoryPassives.OPENING_CRIT_12,
            AccessoryPassives.GUARD_STABILITY_RESTORE,
            AccessoryPassives.BREAK_BONUS_STABILITY,
            AccessoryPassives.BROKEN_PUNISHER,
            AccessoryPassives.BREAK_MOMENTUM_GAIN,
            AccessoryPassives.GUARD_SHIELD_MOMENTUM,
            AccessoryPassives.OPENING_SHIELD_2,
            AccessoryPassives.SHIELD_REFLECT_STABILITY,
            AccessoryPassives.HEAL_CLEANSE_1,
            AccessoryPassives.SUPPORT_STABILITY_BOOST,
            AccessoryPassives.LOW_HP_REGEN,
            AccessoryPassives.PACK_GUARDIAN_CLUTCH,
            AccessoryPassives.SNACK_COOLDOWN_MINUS_1,
            AccessoryPassives.STATUS_HUNTER_BONUS,
            AccessoryPassives.DEFENSE_PIERCE_25,
            AccessoryPassives.LEGENDARY_RETRO_OVERCLOCK,
            AccessoryPassives.LEGENDARY_CADENCE_CROWN,
            AccessoryPassives.LEGENDARY_STARBORN_SURGE
        )
        val accessories = reader.readList<Item>("items.json").filter { it.type.equals("accessory", ignoreCase = true) }
        assertEquals(45, accessories.size)
        accessories.forEach { item ->
            val eq = item.equipment
            assertNotNull("Accessory ${item.id} missing equipment block", eq)
            assertTrue(
                "Accessory ${item.id} has invalid passiveId=${eq?.passiveId}",
                eq?.passiveId in validIds
            )
            assertTrue(
                "Accessory ${item.id} missing passiveText",
                !eq?.passiveText.isNullOrBlank()
            )
        }
    }

    @Test
    fun allFivePreviouslyUnplacedAccessoriesHaveTinkeringRecipes() {
        val recipes = reader.readList<TinkeringRecipe>("recipes_tinkering.json")
        val results = recipes.map { it.result }.toSet()
        listOf("gadget_belt", "holo_charm", "shadow_coil", "reactive_plating", "brutal_charm").forEach { id ->
            assertTrue("Expected tinkering recipe for $id", id in results)
        }
    }

    @Test
    fun battleStartPassivesApplyShieldEvasionMomentumAndAtbHeadstart() {
        val p1 = combatant("p1", CombatSide.PLAYER, passiveId = AccessoryPassives.OPENING_SHIELD_2, accessoryName = "Holo Charm")
        val p2 = combatant("p2", CombatSide.PLAYER, passiveId = AccessoryPassives.ATB_HEADSTART_EVASION, accessoryName = "Grav-Boots")
        val p3 = combatant("p3", CombatSide.PLAYER, passiveId = AccessoryPassives.LEGENDARY_RETRO_OVERCLOCK, accessoryName = "Retro Overclock Core")
        val enemy = combatant("e1", CombatSide.ENEMY)

        val state = engine.beginEncounter(CombatSetup(listOf(p1, p2, p3), listOf(enemy)))

        assertEquals(2, state.combatants.getValue("p1").statusEffects.firstOrNull { it.id == "shield" }?.remainingTurns)
        assertTrue(state.combatants.getValue("p2").buffs.any { it.effect.stat == "evasion" && it.effect.value == 10 })
        assertEquals(1, state.combatants.getValue("p3").momentum)
        assertEquals(0.20f, AccessoryPassives.initialAtbBonus(AccessoryPassives.ATB_HEADSTART_20), 0.001f)
        assertEquals(0.25f, AccessoryPassives.initialAtbBonus(AccessoryPassives.ATB_HEADSTART_EVASION), 0.001f)
        assertTrue(state.log.filterIsInstance<CombatLogEntry.PassiveTriggered>().size >= 3)
    }

    @Test
    fun guardPassivesRestoreStabilityCleanseDebuffAndGrantBoundedMomentum() {
        val p1 = combatant("p1", CombatSide.PLAYER, passiveId = AccessoryPassives.GUARD_STABILITY_RESTORE, accessoryName = "Stability Gyro")
        val p2 = combatant("p2", CombatSide.PLAYER, passiveId = AccessoryPassives.GUARD_SHIELD_MOMENTUM, accessoryName = "Kinetic Bracelet")
        val enemy = combatant("e1", CombatSide.ENEMY)
        var state = engine.beginEncounter(CombatSetup(listOf(p1, p2), listOf(enemy)))
        state = state.copy(
            combatants = state.combatants + (
                "p1" to state.combatants.getValue("p1").copy(
                    stability = 50,
                    statusEffects = listOf(StatusEffect("burn", 2))
                )
            )
        )

        val afterP1 = processor().execute(state, CombatAction.Defend("p1")) { CombatReward() }
        assertEquals(70, afterP1.combatants.getValue("p1").stability)
        assertNull(afterP1.combatants.getValue("p1").statusEffects.firstOrNull { it.id == "burn" })

        val afterP2First = processor().execute(afterP1, CombatAction.Defend("p2")) { CombatReward() }
        assertEquals(1, afterP2First.combatants.getValue("p2").momentum)
        // Second defend in the same round must NOT double-trigger momentum
        val afterP2Second = processor().execute(afterP2First, CombatAction.Defend("p2")) { CombatReward() }
        assertEquals(1, afterP2Second.combatants.getValue("p2").momentum)
    }

    @Test
    fun brokenPunisherAndDefensePierceIncreaseDamageAndBreakBonusIncreasesStabilityDamage() {
        val basePlayer = combatant("p1", CombatSide.PLAYER)
        val punisher = combatant("p1", CombatSide.PLAYER, passiveId = AccessoryPassives.BROKEN_PUNISHER, accessoryName = "Brutal Charm")
        val piercer = combatant("p1", CombatSide.PLAYER, passiveId = AccessoryPassives.DEFENSE_PIERCE_25, accessoryName = "Phase Rounds")
        val breaker = combatant("p1", CombatSide.PLAYER, passiveId = AccessoryPassives.BREAK_BONUS_STABILITY, accessoryName = "Miner's Gyro Mod")
        val enemy = combatant("e1", CombatSide.ENEMY, defense = 80, stability = 500)

        val sBase = engine.beginEncounter(CombatSetup(listOf(basePlayer), listOf(enemy)))
        val sBaseBroken = sBase.copy(combatants = sBase.combatants + ("e1" to sBase.combatants.getValue("e1").copy(breakTurns = 1)))
        val sPunish = engine.beginEncounter(CombatSetup(listOf(punisher), listOf(enemy))).let { st ->
            st.copy(combatants = st.combatants + ("e1" to st.combatants.getValue("e1").copy(breakTurns = 1)))
        }
        val sPierce = engine.beginEncounter(CombatSetup(listOf(piercer), listOf(enemy)))
        val sBreak = engine.beginEncounter(CombatSetup(listOf(breaker), listOf(enemy)))

        val baseNormalDmg = processor().execute(sBase, CombatAction.SkillUse("p1", "test_strike", listOf("e1"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().last().amount
        val pierceDmg = processor().execute(sPierce, CombatAction.SkillUse("p1", "test_strike", listOf("e1"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().last().amount
        assertTrue("Expected defense pierce ($pierceDmg) > baseline ($baseNormalDmg)", pierceDmg > baseNormalDmg)

        // Also verify piercing Shield & Physical Resistance when flatDefense == 0 (matching real enemies.json)
        val shieldedEnemy = Combatant(
            id = "e_res",
            name = "Armored Drone",
            side = CombatSide.ENEMY,
            stats = StatBlock(1000, 40, 20, 20, 20, 10, 10, stability = 200, flatDamageReduction = 0),
            resistances = ResistanceProfile(physical = 50)
        )
        val sBaseRes = engine.beginEncounter(CombatSetup(listOf(basePlayer), listOf(shieldedEnemy))).let { st ->
            st.copy(combatants = st.combatants + ("e_res" to st.combatants.getValue("e_res").copy(statusEffects = listOf(StatusEffect("shield", 2)))))
        }
        val sPierceRes = engine.beginEncounter(CombatSetup(listOf(piercer), listOf(shieldedEnemy))).let { st ->
            st.copy(combatants = st.combatants + ("e_res" to st.combatants.getValue("e_res").copy(statusEffects = listOf(StatusEffect("shield", 2)))))
        }
        val baseResDmg = processor().execute(sBaseRes, CombatAction.SkillUse("p1", "test_strike", listOf("e_res"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().last().amount
        val pierceResDmg = processor().execute(sPierceRes, CombatAction.SkillUse("p1", "test_strike", listOf("e_res"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().last().amount
        assertTrue("Expected pierce vs Shield+Resist ($pierceResDmg) > baseline ($baseResDmg)", pierceResDmg > baseResDmg)

        val baseBrokenDmg = processor().execute(sBaseBroken, CombatAction.SkillUse("p1", "test_strike", listOf("e1"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().last().amount
        val punishDmg = processor().execute(sPunish, CombatAction.SkillUse("p1", "test_strike", listOf("e1"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().last().amount
        assertTrue("Expected broken punisher ($punishDmg) > broken baseline ($baseBrokenDmg)", punishDmg > baseBrokenDmg)

        val baseStabLeft = processor().execute(sBase, CombatAction.SkillUse("p1", "test_strike", listOf("e1"))) { CombatReward() }
            .combatants.getValue("e1").stability
        val breakStabLeft = processor().execute(sBreak, CombatAction.SkillUse("p1", "test_strike", listOf("e1"))) { CombatReward() }
            .combatants.getValue("e1").stability
        assertTrue("Expected break bonus to deal more stability damage ($breakStabLeft < $baseStabLeft)", breakStabLeft < baseStabLeft)
    }

    @Test
    fun healCleanseSupportStabilityAndLowHpRegenWork() {
        val healer = combatant("h1", CombatSide.PLAYER, passiveId = AccessoryPassives.HEAL_CLEANSE_1, accessoryName = "Field Bio-Scanner")
        val booster = combatant("h2", CombatSide.PLAYER, passiveId = AccessoryPassives.SUPPORT_STABILITY_BOOST, accessoryName = "Focus Conduit")
        val ally = combatant("a1", CombatSide.PLAYER, passiveId = AccessoryPassives.LOW_HP_REGEN, accessoryName = "Vital Signet")
        val enemy = combatant("e1", CombatSide.ENEMY)

        var state = engine.beginEncounter(CombatSetup(listOf(healer, booster, ally), listOf(enemy)))
        state = state.copy(
            combatants = state.combatants + (
                "a1" to state.combatants.getValue("a1").copy(
                    hp = 400,
                    stability = 50,
                    statusEffects = listOf(StatusEffect("burn", 2))
                )
            )
        )

        val afterCleanse = processor().execute(state, CombatAction.SkillUse("h1", "test_heal", listOf("a1"))) { CombatReward() }
        assertNull(afterCleanse.combatants.getValue("a1").statusEffects.firstOrNull { it.id == "burn" })

        val afterBoost = processor().execute(afterCleanse, CombatAction.SkillUse("h2", "test_heal", listOf("a1"))) { CombatReward() }
        assertEquals(70, afterBoost.combatants.getValue("a1").stability)

        // Set ally below 60% HP and tick end-of-turn for LOW_HP_REGEN
        val lowState = afterBoost.copy(
            combatants = afterBoost.combatants + ("a1" to afterBoost.combatants.getValue("a1").copy(hp = 500))
        )
        val afterTurnEnd = engine.tickEndOfTurn(lowState, "a1")
        assertEquals(560, afterTurnEnd.combatants.getValue("a1").hp)
    }

    @Test
    fun shieldReflectAndPackGuardianClutchReactToEnemyHits() {
        val tank = combatant("t1", CombatSide.PLAYER, passiveId = AccessoryPassives.SHIELD_REFLECT_STABILITY, accessoryName = "Reactive Plating")
        val guardian = combatant("g1", CombatSide.PLAYER, passiveId = AccessoryPassives.PACK_GUARDIAN_CLUTCH, accessoryName = "Pack-Guardian Bandana")
        val enemy = combatant("e1", CombatSide.ENEMY, attack = 80, stability = 100)

        var state = engine.beginEncounter(CombatSetup(listOf(tank, guardian), listOf(enemy)))
        state = state.copy(
            combatants = state.combatants + (
                "t1" to state.combatants.getValue("t1").copy(
                    hp = 400,
                    statusEffects = listOf(StatusEffect("shield", 2))
                )
            )
        )

        val afterHit = processor().execute(state, CombatAction.SkillUse("e1", "enemy_hit", listOf("t1"))) { CombatReward() }
        assertEquals(82, afterHit.combatants.getValue("e1").stability)
        assertTrue(afterHit.combatants.getValue("t1").hp < 350)
        assertNotNull(afterHit.combatants.getValue("g1").statusEffects.firstOrNull { it.id == "shield" })
        assertEquals(1, afterHit.combatants.getValue("g1").momentum)

        // Verify boss-scale stability reflect (15% of 260 = 39, capped at 40)
        val boss = combatant("boss", CombatSide.ENEMY, attack = 80, stability = 260)
        var bossState = engine.beginEncounter(CombatSetup(listOf(tank), listOf(boss)))
        bossState = bossState.copy(
            combatants = bossState.combatants + ("t1" to bossState.combatants.getValue("t1").copy(statusEffects = listOf(StatusEffect("shield", 2))))
        )
        val afterBossHit = processor().execute(bossState, CombatAction.SkillUse("boss", "enemy_hit", listOf("t1"))) { CombatReward() }
        assertEquals(221, afterBossHit.combatants.getValue("boss").stability)
    }

    @Test
    fun snackCooldownReductionAndLegendaryCadenceAndSurgeWork() {
        val snacker = combatant("s1", CombatSide.PLAYER, passiveId = AccessoryPassives.SNACK_COOLDOWN_MINUS_1, accessoryName = "Gadget Belt")
        val crown = combatant("c1", CombatSide.PLAYER, passiveId = AccessoryPassives.LEGENDARY_CADENCE_CROWN, accessoryName = "Crown of the Primordial Cadence")
        val surge = combatant("u1", CombatSide.PLAYER, passiveId = AccessoryPassives.LEGENDARY_STARBORN_SURGE, accessoryName = "Starborn")
        val enemy = combatant("e1", CombatSide.ENEMY, stability = 500)

        var state = engine.beginEncounter(CombatSetup(listOf(snacker, crown, surge), listOf(enemy)))
        assertEquals(1, AccessoryPassives.snackCooldownReduction(state.combatants.getValue("s1")))

        // Give crown 3 momentum + an existing cooldown on another skill, then spend 3 momentum on test_heavy
        state = state.copy(
            combatants = state.combatants + mapOf(
                "c1" to state.combatants.getValue("c1").copy(
                    momentum = 3,
                    activeCooldowns = mapOf("other_skill" to 3)
                ),
                "u1" to state.combatants.getValue("u1").copy(
                    momentum = 2,
                    stability = 60
                )
            )
        )

        val afterCrown = processor().execute(state, CombatAction.SkillUse("c1", "test_heavy", listOf("e1"))) { CombatReward() }
        // Normal end-of-turn ticks cooldown 3 -> 2, and Crown shaves an extra 1 turn -> 1
        assertEquals(1, afterCrown.combatants.getValue("c1").activeCooldowns["other_skill"])

        // Basic attack by u1 raises momentum 2 -> 3, triggering Starborn Surge (Shield 1t + 15 Stability)
        val afterSurge = processor().execute(afterCrown, CombatAction.BasicAttack("u1", "e1")) { CombatReward() }
        assertEquals(3, afterSurge.combatants.getValue("u1").momentum)
        assertEquals(75, afterSurge.combatants.getValue("u1").stability)
        assertNotNull(afterSurge.combatants.getValue("u1").statusEffects.firstOrNull { it.id == "shield" })
    }
}
