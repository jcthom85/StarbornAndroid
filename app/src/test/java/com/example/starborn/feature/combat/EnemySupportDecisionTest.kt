package com.example.starborn.feature.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.domain.combat.*
import com.example.starborn.feature.combat.viewmodel.SkillTargeting
import com.example.starborn.feature.combat.viewmodel.helpers.CombatEnemyAI
import com.example.starborn.feature.combat.viewmodel.helpers.CombatBehavior
import com.example.starborn.feature.combat.viewmodel.helpers.CombatRole
import com.example.starborn.feature.combat.viewmodel.helpers.EnemyBrain
import org.junit.Assert.*
import org.junit.Test

/** Isolated AI decision scenarios, not campaign checkpoints or balance fixtures. */
class EnemySupportDecisionTest {
    private val assets = WorldAssetDataSource(AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance))
    private val repair = assets.loadSkills().single { it.id == "driller_core_repair" }
    private val registry = StatusRegistry(assets.loadStatuses())
    private val stats = StatBlock(200, 8, 6, 5, 8, 4, 5)
    private val core = Combatant("driller_core", "Core", CombatSide.ENEMY, stats, skills = listOf(repair.id))
    private val nova = Combatant("nova", "Nova", CombatSide.PLAYER, stats)

    private fun scenario(cooldown: Int = 0, jammed: Boolean = false): CombatState = CombatState(
        turnOrder = listOf(TurnSlot(core.id, 10), TurnSlot(nova.id, 5)), activeTurnIndex = 0,
        combatants = mapOf(
            core.id to CombatantState(core, hp = 30, stability = 100,
                activeCooldowns = mapOf(repair.id to cooldown),
                statusEffects = if (jammed) listOf(StatusEffect("jammed", 2)) else emptyList()),
            nova.id to CombatantState(nova, hp = 200, stability = 100)
        )
    )

    private fun ai(): CombatEnemyAI = CombatEnemyAI(
        skillById = mapOf(repair.id to repair),
        enemyDefinitions = assets.loadEnemies().associateBy { it.id },
        statusRegistry = registry, aiWeights = CombatAiWeights(),
        isSupportSkill = { it.type == "heal" }, skillStatusDefinitions = { emptyList() },
        // Core Repair's production targeting rule is SELF. This fixture deliberately
        // covers only that skill, not the view model's general targeting resolver.
        determineSkillTargeting = { SkillTargeting.SELF },
        checkSkillConditions = { _, skill, _, _ ->
            check(skill.conditions.isNullOrEmpty()) { "Extend fixture for new repair conditions" }
            true
        },
        enemyBrains = mutableMapOf(core.id to EnemyBrain(CombatBehavior.DEFENSIVE, CombatRole.SUPPORT)),
        enemyActionHistory = mutableMapOf(), enemySkillUsageCounts = mutableMapOf(),
        getPlayerIdList = { listOf(nova.id) }
    )

    @Test fun `party ration ignores single selection heals living allies and consumes once`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val ration = reader.readList<com.example.starborn.domain.model.Item>("items.json").single { it.id == "ration_pack" }
        val zeke = nova.copy(id = "zeke")
        val orion = nova.copy(id = "orion")
        val ghost = nova.copy(id = "gh0st")
        val actors = listOf(nova, zeke, orion, ghost, core)
        val state = CombatState(turnOrder = actors.map { TurnSlot(it.id, 5) }, activeTurnIndex = 0,
            combatants = actors.associate { it.id to CombatantState(it,
                hp = when (it.id) { "gh0st" -> 0; "orion" -> 190; else -> 100 }, stability = 100) })
        var consumed = 0
        val processor = CombatActionProcessor(CombatEngine(statusRegistry = registry), registry, { null },
            consumeItem = { id ->
                assertEquals(ration.id, id)
                consumed++
                com.example.starborn.domain.inventory.ItemUseResult.Restore(ration, requireNotNull(ration.effect?.restoreHp))
            })
        val after = processor.execute(state, CombatAction.ItemUse("nova", ration.id, "nova")) { CombatReward() }
        assertEquals(1, consumed)
        assertEquals(135, after.combatants.getValue("nova").hp)
        assertEquals(135, after.combatants.getValue("zeke").hp)
        assertEquals(200, after.combatants.getValue("orion").hp)
        assertEquals(0, after.combatants.getValue("gh0st").hp)
        assertEquals(100, after.combatants.getValue(core.id).hp)
    }

    @Test fun `wounded support AI repairs itself and processor heals the enemy`() {
        val state = scenario()
        val action = ai().selectEnemyAction(state, state.combatants.getValue(core.id), null) { }
        assertEquals(CombatAction.SkillUse(core.id, repair.id, listOf(core.id)), action)
        val engine = CombatEngine(statusRegistry = registry)
        val processor = CombatActionProcessor(engine, registry, { id -> repair.takeIf { it.id == id } },
            random = SeededCombatRandom(7))
        val after = processor.execute(state, action) { CombatReward() }
        assertTrue(after.combatants.getValue(core.id).hp > state.combatants.getValue(core.id).hp)
        assertEquals(state.combatants.getValue(nova.id).hp, after.combatants.getValue(nova.id).hp)
    }

    @Test fun `repair on cooldown is excluded from enemy decisions`() {
        val state = scenario(cooldown = 2)
        val ai = ai()
        assertFalse(ai.canEnemyUseSkill(core.id, repair, state))
        assertFalse(ai.selectEnemyAction(state, state.combatants.getValue(core.id), null) { } is CombatAction.SkillUse)
    }

    @Test fun `Titan vents after either heavy attack and exposes only itself`() {
        val skills = assets.loadSkills().associateBy { it.id }
        val titan = core.copy(id = "titan_walker_boss", skills = listOf("missile_barrage", "titan_stomp", "vent_exposure"))
        for (previous in listOf("missile_barrage", "titan_stomp")) {
            val state = CombatState(turnOrder = listOf(TurnSlot(titan.id, 10), TurnSlot(nova.id, 5)),
                activeTurnIndex = 0, combatants = mapOf(titan.id to CombatantState(titan, hp = 200, stability = 100),
                    nova.id to CombatantState(nova, hp = 200, stability = 100)))
            val ai = CombatEnemyAI(skills, assets.loadEnemies().associateBy { it.id }, registry, CombatAiWeights(),
                isSupportSkill = { false }, skillStatusDefinitions = { emptyList() },
                determineSkillTargeting = { SkillTargeting.SINGLE_ENEMY }, checkSkillConditions = { _, _, _, _ -> true },
                enemyBrains = mutableMapOf(), enemyActionHistory = mutableMapOf(titan.id to ArrayDeque(listOf(previous))),
                enemySkillUsageCounts = mutableMapOf(), getPlayerIdList = { listOf(nova.id) })
            val action = ai.selectEnemyAction(state, state.combatants.getValue(titan.id), null) {}
            assertEquals(CombatAction.SkillUse(titan.id, "vent_exposure", listOf(titan.id)), action)
            val processor = CombatActionProcessor(CombatEngine(statusRegistry = registry), registry, skills::get,
                random = SeededCombatRandom(1))
            val after = processor.execute(state, action) { CombatReward() }
            assertTrue(after.combatants.getValue(titan.id).statusEffects.any { it.id == "radiators_exposed" && it.remainingTurns > 0 })
            assertTrue(after.combatants.getValue(nova.id).statusEffects.isEmpty())
            assertEquals(200, after.combatants.getValue(nova.id).hp)
        }
    }

    @Test fun `radiator exposure reduces physical defense with identical damage rolls`() {
        val attacker = CombatantState(nova, hp = 200, stability = 100)
        val target = CombatantState(core, hp = 200, stability = 100)
        fun damage(defender: CombatantState): Int {
            val processor = CombatActionProcessor(CombatEngine(statusRegistry = registry), registry, { null },
                random = SeededCombatRandom(31))
            val method = CombatActionProcessor::class.java.getDeclaredMethod("basePhysicalDamage",
                CombatantState::class.java, CombatantState::class.java).apply { isAccessible = true }
            return method.invoke(processor, attacker, defender) as Int
        }
        assertTrue(damage(target.copy(statusEffects = listOf(StatusEffect("radiators_exposed", 1)))) > damage(target))
    }

    @Test fun `support AI does not waste repair at full health`() {
        val wounded = scenario()
        val state = wounded.copy(combatants = wounded.combatants +
            (core.id to wounded.combatants.getValue(core.id).copy(hp = stats.maxHp)))
        assertFalse(ai().selectEnemyAction(state, state.combatants.getValue(core.id), null) { } is CombatAction.SkillUse)
    }

    @Test fun `authored jammed status blocks enemy support skills`() {
        assertEquals(true, registry.definition("jammed")?.blockSkills)
        val state = scenario(jammed = true)
        val ai = ai()
        assertFalse(ai.canEnemyUseSkill(core.id, repair, state))
        assertFalse(ai.selectEnemyAction(state, state.combatants.getValue(core.id), null) { } is CombatAction.SkillUse)
    }
}
