package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.leveling.CrewBuildPrototype
import com.example.starborn.domain.leveling.NovaBuildPrototype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class Phase2BCombatAndProgressionTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private val registry = StatusRegistry(assets.loadStatuses())
    private val skills = assets.loadSkills(CrewBuildPrototype.VERSION).associateBy { it.id }

    @Test
    fun vantageGrantsOnePointFiveStabilityAndCooldownRefundOncePerAction() {
        val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(11))
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = registry,
            skillLookup = skills::get,
            forcePhysicalHit = { _, _ -> true }
        )
        val nova = Combatant(
            id = "nova",
            name = "Nova",
            side = CombatSide.PLAYER,
            stats = StatBlock(500, 40, 10, 20, 20, 0, 15, stability = 100)
        )
        val neutralBoss = Combatant(
            id = "neutral_boss",
            name = "Neutral Boss",
            side = CombatSide.ENEMY,
            stats = StatBlock(2000, 20, 10, 10, 10, 0, 10, stability = 1000),
            resistances = ResistanceProfile(shock = 0)
        )
        val vantageBoss = Combatant(
            id = "vantage_boss",
            name = "Vantage Boss",
            side = CombatSide.ENEMY,
            stats = StatBlock(2000, 20, 10, 10, 10, 0, 10, stability = 1000),
            resistances = ResistanceProfile(shock = -50)
        )

        val neutralStart = engine.beginEncounter(CombatSetup(listOf(nova), listOf(neutralBoss))).let { s ->
            s.copy(combatants = s.combatants + ("nova" to s.combatants.getValue("nova").copy(
                activeCooldowns = mapOf("nova_blast_wave" to 3),
                snackCooldown = 2
            )))
        }
        val vantageStart = engine.beginEncounter(CombatSetup(listOf(nova), listOf(vantageBoss))).let { s ->
            s.copy(combatants = s.combatants + ("nova" to s.combatants.getValue("nova").copy(
                activeCooldowns = mapOf("nova_blast_wave" to 3),
                snackCooldown = 2
            )))
        }

        val neutralAfter = processor.execute(
            neutralStart,
            CombatAction.SkillUse("nova", "nova_arc_tether", listOf("neutral_boss"))
        ) { CombatReward() }
        val vantageAfter = processor.execute(
            vantageStart,
            CombatAction.SkillUse("nova", "nova_arc_tether", listOf("vantage_boss"))
        ) { CombatReward() }

        val neutralDamage = neutralAfter.log.filterIsInstance<CombatLogEntry.Damage>().last()
        val vantageDamage = vantageAfter.log.filterIsInstance<CombatLogEntry.Damage>().last()
        assertFalse(vantageDamage.isWeakness)
        assertTrue(vantageDamage.isVantage)
        assertEquals(neutralDamage.amount * 1.5, vantageDamage.amount.toDouble(), 1.0)

        val neutralStabLost = 1000 - neutralAfter.combatants.getValue("neutral_boss").stability
        val vantageStabLost = 1000 - vantageAfter.combatants.getValue("vantage_boss").stability
        // VANTAGE multiplies both base elemental damage (1.5x) and stability damage (1.5x -> ~2.25x vs neutral)
        assertEquals((vantageDamage.amount * 1.5).roundToInt(), vantageStabLost)
        assertTrue(vantageStabLost > neutralStabLost * 2)

        // End-of-turn ticks -1, plus VANTAGE WeaknessReward refunds an extra -1 -> 3 becomes 1 (vs 2 on neutral)
        assertEquals(2, neutralAfter.combatants.getValue("nova").activeCooldowns["nova_blast_wave"])
        assertEquals(1, vantageAfter.combatants.getValue("nova").activeCooldowns["nova_blast_wave"])
        assertTrue(vantageAfter.log.any { it is CombatLogEntry.WeaknessReward && it.actorId == "nova" })
    }

    @Test
    fun offensiveSkillsRollNaturalCritsConsumeTargetLockAndRespectBlindAccuracy() {
        val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(7))
        // Roll 12.0 hits (12 <= 90) and crits when critChance >= 12% (5% base + 25% target_lock = 30%)
        val critRng = object : CombatRandom {
            override fun nextDouble(from: Double, until: Double): Double = 12.0.coerceIn(from, until - 0.001)
            override fun nextInt(from: Int, until: Int): Int = from
        }
        val processor = CombatActionProcessor(
            engine = engine,
            statusRegistry = registry,
            skillLookup = skills::get,
            random = critRng
        )
        val nova = Combatant("nova", "Nova", CombatSide.PLAYER, StatBlock(500, 40, 10, 20, 0, 0, 15))
        val target = Combatant("drone", "Drone", CombatSide.ENEMY, StatBlock(1000, 10, 5, 5, 0, 0, 10))
        val base = engine.beginEncounter(CombatSetup(listOf(nova), listOf(target)))
        val locked = base.copy(
            combatants = base.combatants + ("drone" to base.combatants.getValue("drone").copy(
                statusEffects = listOf(StatusEffect("target_lock", remainingTurns = 2, stacks = 1))
            ))
        )

        val uncritResult = processor.execute(base, CombatAction.SkillUse("nova", "nova_arc_tether", listOf("drone"))) { CombatReward() }
        val critResult = processor.execute(locked, CombatAction.SkillUse("nova", "nova_arc_tether", listOf("drone"))) { CombatReward() }

        val uncritEntry = uncritResult.log.filterIsInstance<CombatLogEntry.Damage>().last()
        val critEntry = critResult.log.filterIsInstance<CombatLogEntry.Damage>().last()
        assertFalse(uncritEntry.critical)
        assertTrue(critEntry.critical)
        assertEquals(uncritEntry.amount * 2, critEntry.amount)
        assertNull(critResult.combatants.getValue("drone").statusEffects.firstOrNull { it.id == "target_lock" })

        // Blinded caster (0.5x accuracy -> ~47.5% - 5.75% = ~41.75% hit chance) misses on a 70.0 roll
        val blindMissRng = object : CombatRandom {
            override fun nextDouble(from: Double, until: Double): Double = 70.0.coerceIn(from, until - 0.001)
            override fun nextInt(from: Int, until: Int): Int = from
        }
        val blindProcessor = CombatActionProcessor(engine = engine, statusRegistry = registry, skillLookup = skills::get, random = blindMissRng)
        val blinded = base.copy(
            combatants = base.combatants + ("nova" to base.combatants.getValue("nova").copy(
                statusEffects = listOf(StatusEffect("blind", remainingTurns = 2, stacks = 1))
            ))
        )
        val missResult = blindProcessor.execute(blinded, CombatAction.SkillUse("nova", "nova_arc_tether", listOf("drone"))) { CombatReward() }
        val missEntry = missResult.log.filterIsInstance<CombatLogEntry.Damage>().last()
        assertEquals("miss", missEntry.element)
        assertEquals(0, missEntry.amount)
    }

    @Test
    fun percentHpBossDotIsCappedAt120AndShortNukeCooldownsAreThreeTurns() {
        val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(3))
        val mob = Combatant("mob", "Mob", CombatSide.ENEMY, StatBlock(600, 10, 5, 5, 0, 0, 10))
        val boss = Combatant("boss", "Boss", CombatSide.ENEMY, StatBlock(2600, 30, 10, 10, 0, 0, 10))
        val player = Combatant("gh0st", "Gh0st", CombatSide.PLAYER, StatBlock(500, 40, 10, 30, 10, 0, 20))

        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(mob, boss)))
        state = engine.applyStatus(state, "mob", "erosion", duration = 3, stacks = 1)
        state = engine.applyStatus(state, "boss", "erosion", duration = 3, stacks = 1)

        val afterMobTick = engine.tickEndOfTurn(state, "mob")
        val afterBossTick = engine.tickEndOfTurn(afterMobTick, "boss")

        assertEquals(600 - 60, afterBossTick.combatants.getValue("mob").hp)
        assertEquals(2600 - 120, afterBossTick.combatants.getValue("boss").hp)

        assertEquals(3, skills.getValue("nova_rend_round").cooldown)
        assertEquals(3, skills.getValue("zeke_overload_fists").cooldown)
        assertEquals(3, skills.getValue("gh0st_venom_edge").cooldown)

        assertEquals(3, NovaBuildPrototype.nodes.getValue(NovaBuildPrototype.STEPS).effect?.value)
        assertEquals(5, CrewBuildPrototype.nodes.getValue(CrewBuildPrototype.REINFORCED_HARNESS).effect?.value)
        assertEquals(3, CrewBuildPrototype.nodes.getValue(CrewBuildPrototype.TRAINING_SESSION).effect?.value)
        assertEquals(3, CrewBuildPrototype.nodes.getValue(CrewBuildPrototype.FIELD_RESILIENCE).effect?.value)
        assertEquals(3, CrewBuildPrototype.nodes.getValue(CrewBuildPrototype.CLEAR_FOCUS).effect?.value)
        assertEquals(3, CrewBuildPrototype.nodes.getValue(CrewBuildPrototype.QUICK_POSITION).effect?.value)
    }
}
