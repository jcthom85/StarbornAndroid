package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.inventory.ItemUseResult
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Skill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1CombatFixesTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    private val registry = StatusRegistry(assets.loadStatuses())
    private val engine = CombatEngine(statusRegistry = registry, random = kotlin.random.Random(7))
    private val itemsById = reader.readList<Item>("items.json").associateBy { it.id }
    private val skillsById = assets.loadSkills().associateBy { it.id }

    private val deterministicRng = object : CombatRandom {
        override fun nextDouble(from: Double, until: Double) = 50.0.coerceIn(from, until - 0.0001)
        override fun nextInt(from: Int, until: Int) = 0
    }

    private fun makeCombatant(
        id: String,
        side: CombatSide,
        resistances: ResistanceProfile = ResistanceProfile(),
        passiveId: String? = null,
        weapon: CombatWeapon? = null
    ) = Combatant(
        id = id,
        name = id,
        side = side,
        stats = StatBlock(
            maxHp = 1000,
            strength = 40,
            vitality = 20,
            agility = 20,
            focus = 30,
            luck = 10,
            speed = 15,
            stability = 120
        ),
        resistances = resistances,
        weapon = weapon,
        accessoryPassiveId = passiveId,
        accessoryName = passiveId
    )

    private fun processor() = CombatActionProcessor(
        engine = engine,
        statusRegistry = registry,
        skillLookup = skillsById::get,
        itemLookup = itemsById::get,
        consumeItem = { itemId ->
            val item = itemsById[itemId]
            val dmg = item?.effect?.damage ?: 0
            if (item != null && dmg > 0) ItemUseResult.Damage(item, dmg) else null
        },
        forcePhysicalHit = { _, _ -> true },
        random = deterministicRng
    )

    @Test
    fun exposedStatusesAmplifySkillDamageAcrossWorlds2To4() {
        val attacker = makeCombatant("nova", CombatSide.PLAYER)
        val target = makeCombatant("boss", CombatSide.ENEMY)
        val baseState = engine.beginEncounter(CombatSetup(listOf(attacker), listOf(target)))
        val baselineDmg = processor().execute(
            baseState,
            CombatAction.SkillUse("nova", "nova_blast_wave", listOf("boss"))
        ) { CombatReward() }.log.filterIsInstance<CombatLogEntry.Damage>().single().amount

        listOf("exposed", "sector9_exposed", "spire_exposed", "radiators_exposed").forEach { statusId ->
            val exposedState = baseState.copy(
                combatants = baseState.combatants + (
                    "boss" to baseState.combatants.getValue("boss").copy(
                        statusEffects = listOf(StatusEffect(statusId, remainingTurns = 2))
                    )
                )
            )
            val exposedDmg = processor().execute(
                exposedState,
                CombatAction.SkillUse("nova", "nova_blast_wave", listOf("boss"))
            ) { CombatReward() }.log.filterIsInstance<CombatLogEntry.Damage>().single().amount
            assertTrue("Expected $statusId ($exposedDmg) > baseline ($baselineDmg)", exposedDmg > baselineDmg)
        }
    }

    @Test
    fun defendResolvesGuardMitigationAndTriggersDefensiveAccessoryPassives() {
        val defender = makeCombatant(
            "zeke",
            CombatSide.PLAYER,
            passiveId = AccessoryPassives.SHIELD_REFLECT_STABILITY
        )
        val enemy = makeCombatant("enemy", CombatSide.ENEMY)
        val unguardedState = engine.beginEncounter(CombatSetup(listOf(defender), listOf(enemy)))
        val guardedState = processor().execute(unguardedState, CombatAction.Defend("zeke")) { CombatReward() }

        val unguardedDmg = processor().execute(
            unguardedState,
            CombatAction.BasicAttack("enemy", "zeke")
        ) { CombatReward() }.log.filterIsInstance<CombatLogEntry.Damage>().single().amount

        val afterGuardedHit = processor().execute(
            guardedState,
            CombatAction.BasicAttack("enemy", "zeke")
        ) { CombatReward() }
        val guardedDmg = afterGuardedHit.log.filterIsInstance<CombatLogEntry.Damage>().last().amount

        assertTrue("Expected Guard to halve incoming damage before flat bonus ($guardedDmg < $unguardedDmg)", guardedDmg <= unguardedDmg / 2)
        assertTrue(
            "Expected Reactive Plating stability reflect while defending",
            afterGuardedHit.combatants.getValue("enemy").stability < 120
        )
    }

    @Test
    fun zeroPowerUtilitySkillsPreservePlayerMomentum() {
        val player = makeCombatant("nova", CombatSide.PLAYER)
        val enemy = makeCombatant("enemy", CombatSide.ENEMY)
        var state = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        state = state.copy(
            combatants = state.combatants + ("nova" to state.combatants.getValue("nova").copy(momentum = 3))
        )

        val afterScan = processor().execute(
            state,
            CombatAction.SkillUse("nova", "source_art_scan", listOf("enemy"))
        ) { CombatReward() }
        assertEquals(3, afterScan.combatants.getValue("nova").momentum)
    }

    @Test
    fun fireElectricCryoSunbeamAndGrenadesResolveToExpectedElements() {
        assertEquals(10.0, itemsById.getValue("relic_mod_biolum_focus").equipment?.critRate ?: 0.0, 0.001)

        val electricWeapon = CombatWeapon(
            itemId = "test_elec",
            name = "Electric Gun",
            weaponType = "pistol",
            minDamage = 10,
            maxDamage = 10,
            attack = WeaponAttack.SingleTarget(1.0, element = "electric")
        )
        val cryoWeapon = CombatWeapon(
            itemId = "test_cryo",
            name = "Cryo Gun",
            weaponType = "pistol",
            minDamage = 10,
            maxDamage = 10,
            attack = WeaponAttack.SingleTarget(1.0, element = "cryo")
        )
        val shockWeakRobot = makeCombatant("robot", CombatSide.ENEMY, resistances = ResistanceProfile(shock = -100, freeze = -100, burn = -100))

        val elecState = engine.beginEncounter(CombatSetup(listOf(makeCombatant("nova", CombatSide.PLAYER, weapon = electricWeapon)), listOf(shockWeakRobot)))
        val elecHit = processor().execute(elecState, CombatAction.BasicAttack("nova", "robot")) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().single()
        assertEquals("shock", elecHit.element)
        assertTrue(elecHit.isWeakness)

        val cryoState = engine.beginEncounter(CombatSetup(listOf(makeCombatant("nova", CombatSide.PLAYER, weapon = cryoWeapon)), listOf(shockWeakRobot)))
        val cryoHit = processor().execute(cryoState, CombatAction.BasicAttack("nova", "robot")) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().single()
        assertEquals("freeze", cryoHit.element)
        assertTrue(cryoHit.isWeakness)

        val orionState = engine.beginEncounter(CombatSetup(listOf(makeCombatant("orion", CombatSide.PLAYER)), listOf(shockWeakRobot)))
        val sunbeamHit = processor().execute(orionState, CombatAction.SkillUse("orion", "orion_sunbeam", listOf("robot"))) { CombatReward() }
            .log.filterIsInstance<CombatLogEntry.Damage>().single()
        assertEquals("burn", sunbeamHit.element)
        assertTrue(sunbeamHit.isWeakness)

        // Pulse grenade shatters barriers and deals Shock damage
        val shieldedRobot = elecState.copy(
            combatants = elecState.combatants + (
                "robot" to elecState.combatants.getValue("robot").copy(
                    statusEffects = listOf(StatusEffect("invulnerable", 2))
                )
            )
        )
        val afterPulse = processor().execute(
            shieldedRobot,
            CombatAction.ItemUse("nova", "pulse_grenade", "robot")
        ) { CombatReward() }
        val pulseHit = afterPulse.log.filterIsInstance<CombatLogEntry.Damage>().single()
        assertEquals("shock", pulseHit.element)
        assertTrue(pulseHit.amount > 0)
        assertFalse(afterPulse.combatants.getValue("robot").statusEffects.any { it.id == "invulnerable" })
    }
}
