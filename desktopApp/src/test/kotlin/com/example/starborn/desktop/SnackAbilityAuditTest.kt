package com.example.starborn.desktop

import com.example.starborn.domain.combat.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.*
import com.example.starborn.feature.combat.viewmodel.CombatController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** Production enemy AI and speed calculation; idealized immediate player decisions, no animation delays. */
@OptIn(ExperimentalCoroutinesApi::class)
class SnackAbilityAuditTest {
    private data class Variant(val id: String, val snack: String? = null, val policy: String = "passive",
        val duration: Int? = null, val crit: Int? = null, val speed: Int? = null, val weaponOnly: Boolean = false)
    private data class Measurement(val won: Boolean, val actions: Int, val medkits: Int, val hp: Int, val seconds: Double, val activations: Int)
    private val variants = listOf(Variant("none"),
        Variant("legacy_comet_ready", "comet_gummies", "ready", duration = 2, crit = 10),
        Variant("legacy_comet_weapon_ready", "comet_gummies", "ready", duration = 2, crit = 10, weaponOnly = true),
        Variant("legacy_pulse_opening", "pulse_citrus", "opening", duration = 2, speed = 8),
        Variant("legacy_pulse_ready", "pulse_citrus", "ready", duration = 2, speed = 8)) + listOf("mineral_trail_mix", "pulse_citrus", "comet_gummies").flatMap { snack ->
        listOf("passive", "ready", "opening", "situational").map { Variant("${snack}_$it", snack, it) }
    } + listOf(
        Variant("comet_30_4_opening", "comet_gummies", "opening", duration = 4, crit = 30),
        Variant("comet_50_4_opening", "comet_gummies", "opening", duration = 4, crit = 50),
        Variant("comet_30_6_opening", "comet_gummies", "opening", duration = 6, crit = 30),
        Variant("pulse_16_3_opening", "pulse_citrus", "opening", duration = 3, speed = 16),
        Variant("pulse_24_3_opening", "pulse_citrus", "opening", duration = 3, speed = 24),
        Variant("comet_weapon_passive", "comet_gummies", weaponOnly = true),
        Variant("comet_weapon_opening", "comet_gummies", "opening", weaponOnly = true),
        Variant("comet_weapon_ready", "comet_gummies", "ready", weaponOnly = true),
        Variant("comet_weapon_30_4_opening", "comet_gummies", "opening", duration = 4, crit = 30, weaponOnly = true),
        Variant("comet_weapon_30_4_ready", "comet_gummies", "ready", duration = 4, crit = 30, weaponOnly = true))

    @Test fun auditSnackTimingAndCandidateEffects() {
        CombatPlaytestBridge.reset()
        val directory = Files.createTempDirectory("starborn-food-audit-").toFile()
        val services = DesktopAppServices(directory, RecordingAudioDriver())
        try {
            val lines = mutableListOf("encounter,variant,wins,seeds,mean_actions,mean_medkits,mean_remaining_party_hp,mean_atb_seconds,mean_activations")
            for ((enemyId, party, level, room) in listOf(
                listOf("siren_skimmer", "nova,zeke", "5", "sector9_stream_pools"),
                listOf("siren_skimmer+siren_skimmer", "nova,zeke", "5", "sector9_stream_pools"),
                listOf("the_beast", "nova,zeke,orion", "12", "sector9_canopy_ridge"))) {
                for (variant in variants) {
                    val catalog = object : ItemCatalog {
                        override fun load() {}
                        override fun findItem(idOrAlias: String): Item? = services.itemRepository.findItem(idOrAlias)?.let {
                            if (it.id != variant.snack) it else it.copy(effect = it.effect?.let { effect ->
                                fun buff(b: com.example.starborn.domain.model.BuffEffect) = b.copy(
                                    duration = variant.duration ?: b.duration,
                                    value = if (b.stat == "crit_rate") variant.crit ?: b.value else if (b.stat == "speed") variant.speed ?: b.value else b.value)
                                effect.copy(singleBuff = effect.singleBuff?.let(::buff), buffs = effect.buffs?.map(::buff))
                            })
                        }
                    }
                    val members = party.split(',')
                    val session = GameSessionState(worldId = "world_2", roomId = room, playerId = "nova", playerLevel = level.toInt(),
                        partyMembers = members, partyMemberLevels = members.associateWith { level.toInt() },
                        equippedWeapons = GameBootstrap.defaultWeapons.filterKeys { it in members },
                        equippedArmors = GameBootstrap.defaultArmors.filterKeys { it in members },
                        equippedItems = variant.snack?.let { mapOf("nova:snack" to it) }.orEmpty(),
                        inventory = variant.snack?.let { mapOf(it to 1) }.orEmpty())
                    services.sessionStore.restore(session)
                    val engine = CombatEngine(statusRegistry = services.statusRegistry, random = kotlin.random.Random(0))
                    val runtime = CombatController(services.worldDataSource, engine, services.statusRegistry, services.sessionStore,
                        services.inventoryService, catalog, services.levelingManager, services.progressionData, services.audioRouter,
                        services.themeRepository, services.environmentThemeManager, services.encounterCoordinator, enemyId.split("+"),
                        tutorialsEnabled = false, parentScope = TestScope())
                    val template = requireNotNull(runtime.state.value)
                    runtime.close()
                    fun trial(seed: Int): Measurement {
                        val aiRuntime = CombatController(services.worldDataSource, engine, services.statusRegistry, services.sessionStore,
                            services.inventoryService, catalog, services.levelingManager, services.progressionData, services.audioRouter,
                            services.themeRepository, services.environmentThemeManager, services.encounterCoordinator, enemyId.split("+"),
                            tutorialsEnabled = false, random = SeededCombatRandom(seed), parentScope = TestScope())
                        try { return simulate(template, variant, seed, catalog, services, aiRuntime) }
                        finally { aiRuntime.close() }
                    }
                    val results = (0..49).map(::trial)
                    assertEquals(results.first(), trial(0))
                    assertTrue("Audit must reach an outcome", results.all { it.actions < 500 && it.seconds < 600 })
                    fun average(selector: (Measurement) -> Int) = "%.2f".format(java.util.Locale.US, results.map(selector).average())
                    lines += "$enemyId,${variant.id},${results.count { it.won }},50,${average { it.actions }},${average { it.medkits }},${average { it.hp }},${"%.2f".format(java.util.Locale.US, results.map { it.seconds }.average())},${average { it.activations }}"
                }
            }
            File("build/reports/snack-ability-audit.csv").apply { parentFile.mkdirs(); writeText(lines.joinToString("\n") + "\n") }
        } finally { services.close(); directory.deleteRecursively(); CombatPlaytestBridge.reset() }
    }

    private fun simulate(template: CombatState, variant: Variant, seed: Int, catalog: ItemCatalog, services: DesktopAppServices, runtime: CombatController): Measurement {
        val inventory = InventoryService(catalog).apply { loadItems(); addItem("medkit_i", 3) }
        val engine = CombatEngine(statusRegistry = services.statusRegistry, random = kotlin.random.Random(seed))
        val processor = CombatActionProcessor(engine, services.statusRegistry, services.skillDefinitions::get,
            consumeItem = inventory::useItem, itemLookup = catalog::findItem, random = SeededCombatRandom(seed))
        var state = template
        var actions = 0
        var activations = 0
        val select = CombatController::class.java.getDeclaredMethod("selectEnemyAction", CombatState::class.java, CombatantState::class.java).apply { isAccessible = true }
        val record = CombatController::class.java.getDeclaredMethod("recordEnemyActionUse", CombatAction::class.java).apply { isAccessible = true }
        val speed = CombatController::class.java.getDeclaredMethod("effectiveSpeed", CombatantState::class.java).apply { isAccessible = true }
        val rng = kotlin.random.Random(seed)
        val meters = template.combatants.mapValues { (_, c) -> if (c.combatant.side == CombatSide.ENEMY) rng.nextDouble(.2,.6) else rng.nextDouble(.4,.8) }.toMutableMap()
        var elapsed = 0.0
        val ready = java.util.ArrayDeque<String>()
        while (state.outcome == null && actions < 500) {
            while (ready.isEmpty() && elapsed < 600) {
                elapsed += .05
                for (slot in state.turnOrder) {
                    val c = requireNotNull(state.combatants[slot.combatantId])
                    if (!c.isAlive) { meters[slot.combatantId] = 0.0; continue }
                    val id = slot.combatantId
                    if (id in ready) continue
                    val gain = .05 * (speed.invoke(runtime, c) as Int) / 90 / c.breakTurns.coerceAtLeast(1)
                    meters[id] = ((meters[id] ?: 0.0) + gain).coerceAtMost(1.0)
                    if (meters.getValue(id) >= .999) ready.add(id)
                }
            }
            if (ready.isEmpty()) break
            val actorId = ready.removeFirst()
            val actor = requireNotNull(state.combatants[actorId])
            if (!actor.isAlive) continue
            val recovered = if (actor.breakTurns > 0) actor.copy(breakTurns = 0) else actor
            state = state.copy(activeTurnIndex = state.turnOrder.indexOfFirst { it.combatantId == actorId },
                combatants = state.combatants + (actorId to recovered))
            val friendly = actor.combatant.side != CombatSide.ENEMY
            val opponents = state.combatants.values.filter { it.isAlive && (it.combatant.side == CombatSide.ENEMY) == friendly }
            val target = opponents.minWithOrNull(compareBy<CombatantState> { it.hp }.thenBy { it.combatant.id }) ?: break
            val allies = state.combatants.values.filter { it.isAlive && it.combatant.side != CombatSide.ENEMY }
            val injured = allies.minByOrNull { it.hp.toDouble() / it.combatant.stats.maxHp }
            val skill = if (variant.weaponOnly) null else actor.combatant.skills.mapNotNull(services.skillDefinitions::get)
                .filter { it.type == "active" && it.basePower > 0 && actor.activeCooldowns.getOrDefault(it.id, 0) == 0 &&
                    it.conditions.isNullOrEmpty() && it.usesPerBattle == null }
                .maxByOrNull { it.basePower }
            val action: CombatAction = if (friendly) {
                when {
                    injured != null && injured.hp.toDouble() / injured.combatant.stats.maxHp < .40 && inventory.hasItem("medkit_i") ->
                        CombatAction.ItemUse(actorId, "medkit_i", injured.combatant.id)
                    actorId == "nova" && variant.snack != null && actor.snackCooldown == 0 &&
                        actor.buffs.isEmpty() && when (variant.policy) {
                            "ready" -> true
                            "opening" -> activations == 0 && opponents.sumOf { it.hp } > 100
                            "situational" -> opponents.sumOf { it.hp } > 100 && when (variant.snack) {
                                "mineral_trail_mix" -> actor.hp < actor.combatant.stats.maxHp * .65
                                "comet_gummies" -> actor.statusEffects.any { services.statusRegistry.definition(it.id)?.accuracyMultiplier?.let { value -> value < 1.0 } == true } ||
                                    opponents.any { it.statusEffects.any { status -> status.id in setOf("invulnerable", "shield", "guard", "defend") } }
                                "pulse_citrus" -> opponents.any { it.statusEffects.any { status -> status.id in setOf("invulnerable", "shield", "guard", "defend") } }
                                else -> false
                            }
                            else -> false
                        } -> CombatAction.SnackUse(actorId, variant.snack)
                    skill != null -> CombatAction.SkillUse(actorId, skill.id,
                        if (skill.targeting in setOf("all_enemies", "all_opponents")) opponents.map { it.combatant.id } else listOf(target.combatant.id))
                    else -> CombatAction.BasicAttack(actorId, target.combatant.id)
                }
            } else select.invoke(runtime, state, recovered) as CombatAction
            var next = processor.execute(state, action) { CombatReward() }
            if (next == state) next = processor.execute(state, CombatAction.BasicAttack(actor.combatant.id, target.combatant.id)) { CombatReward() }
            assertNotEquals("Combat policy must advance", state, next)
            if (action is CombatAction.SnackUse && next.log.drop(state.log.size).any { it is CombatLogEntry.StatusApplied }) activations++
            if (!friendly) record.invoke(runtime, action)
            meters[actorId] = 0.0
            state = next
            actions++
        }
        return Measurement(state.outcome is CombatOutcome.Victory, actions, 3 - (inventory.snapshot()["medkit_i"] ?: 0),
            state.combatants.values.filter { it.combatant.side != CombatSide.ENEMY }.sumOf { it.hp }, elapsed, activations)
    }
}
