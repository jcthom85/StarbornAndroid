package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.StatusDefinition
import com.squareup.moshi.Types
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class PartThreeBalanceTest {
    private fun registry(): StatusRegistry {
        val root = listOf(File("app/src/main/assets"), File("src/main/assets")).first { it.isDirectory }
        val type = Types.newParameterizedType(List::class.java, StatusDefinition::class.java)
        return StatusRegistry(MoshiProvider.instance.adapter<List<StatusDefinition>>(type)
            .fromJson(File(root, "statuses.json").readText())!!)
    }
    private fun actor(id: String, side: CombatSide, hp: Int) = Combatant(id, id, side,
        StatBlock(maxHp = hp, strength = 10, vitality = 4, agility = 6, focus = 5, luck = 2, speed = 10))

    @Test fun skimmerOpticsDisruptionIsBriefWithoutChangingBaselineAccuracyOrBlind() {
        val registry = registry()
        val engine = CombatEngine(statusRegistry = registry)
        val player = actor("nova", CombatSide.PLAYER, 120)
        val enemy = actor("skimmer", CombatSide.ENEMY, 1000)
        val initial = engine.beginEncounter(CombatSetup(listOf(player), listOf(enemy)))
        fun hits(status: String?): Int {
            val processor = CombatActionProcessor(engine, registry, skillLookup = { null }, random = SeededCombatRandom(42))
            val source = if (status == null) initial else initial.copy(combatants = initial.combatants +
                (player.id to initial.combatants.getValue(player.id).copy(statusEffects = listOf(StatusEffect(status, 1)))))
            return (0 until 2000).count {
                processor.execute(source, CombatAction.BasicAttack(player.id, enemy.id)) { CombatReward() }
                    .combatants.getValue(enemy.id).hp < 1000
            }
        }
        val baseline = hits(null)
        val oldBlind = hits("blind")
        val optics = hits("dazzled")
        println("Seed 42, 2000 attacks: baseline=$baseline, Blind=$oldBlind, Optics Static=$optics")
        assertTrue("Normal hit rate stays near 90%: $baseline/2000", baseline in 1700..1900)
        assertTrue("Optics disruption remains a mild penalty: $optics/2000", optics in 1500..1750)
        assertTrue("Blind remains stronger; no global accuracy nerf", optics > oldBlind + 500)
        assertEquals(1, registry().definition("dazzled")?.defaultDuration)
    }

    @Test fun corrosiveRoundsNoLongerScalesItsDamageWithBossHealth() {
        val registry = registry()
        val engine = CombatEngine(statusRegistry = registry)
        fun damage(hp: Int, status: String): Int {
            var state = engine.beginEncounter(CombatSetup(listOf(actor("nova", CombatSide.PLAYER, 120)),
                listOf(actor("warden", CombatSide.ENEMY, hp))))
            state = state.copy(combatants = state.combatants + ("warden" to state.combatants.getValue("warden")
                .copy(statusEffects = listOf(StatusEffect(status, 3)))))
            repeat(3) { state = engine.tickEndOfTurn(state, actorId = "warden") }
            return hp - state.combatants.getValue("warden").hp
        }
        assertEquals(75, damage(250, "erosion"))
        assertEquals(24, damage(250, "corrosion"))
        assertEquals(24, damage(1000, "corrosion"))
    }
}
