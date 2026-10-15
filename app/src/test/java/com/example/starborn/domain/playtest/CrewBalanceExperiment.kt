package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.leveling.CrewBuildPrototype as Crew
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.combat.viewmodel.CombatController
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Assert.*
import org.mockito.kotlin.*
import java.io.File
import kotlin.math.min

/** Paired seeded experiments through the real ATB controller. Outcomes are observations, not assertions of victory. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CrewBalanceExperiment(private val reader: AssetJsonReader, private val dispatcher: TestDispatcher) {
    private val tempo = System.getenv("CREW_BALANCE_TEMPO") ?: "shipping"
    private val targeted = System.getenv("CREW_BALANCE_TARGETED") == "1"
    private val coverPolicy = System.getenv("CREW_BALANCE_COVER_POLICY") ?: "reactive"
    private val assets = WorldAssetDataSource(reader)
    private val output = File(System.getenv("CREW_BALANCE_REPORT")?.also {
        require(it.matches(Regex("[a-z0-9_-]+")))
    }?.let { "build/reports/crew-balance-experiments/$it" } ?: "build/reports/crew-balance").apply { mkdirs() }

    /** Explicit rule injection lets the same controller reproduce the original and candidate tempos. */
    private fun encounterTempo(): CrewEncounterTempo {
        require(tempo in setOf("shipping", "baseline", "measured", "moderate", "strong"))
        require(coverPolicy in setOf("reactive", "skip", "proactive"))
        return when (tempo) {
            "baseline" -> CrewEncounterTempo(20, 18, 18, 24)
            "measured" -> CrewEncounterTempo(12, 12, 12, 16)
            "strong" -> CrewEncounterTempo(2, 0, 0, 0)
            else -> CrewEncounterTempo()
        }
    }
    private val catalogItems = reader.readList<Item>("items.json").associateBy { it.id }
    private val catalog = object : ItemCatalog {
        override fun load() = Unit
        override fun findItem(idOrAlias: String) = catalogItems[idOrAlias]
    }
    private val themes = mock<ThemeRepository> {
        on { getTheme(any()) } doReturn null
        on { getStyle(any()) } doReturn null
    }
    private data class Scene(val checkpoint: String, val room: String, val enemies: List<String>)
    private val scenes = listOf(
        Scene("early", "foundry_waste_intake", listOf("slag_golem", "magma_drone")),
        Scene("early", "foundry_conveyor_belt", listOf("welder_bot", "magma_drone")),
        Scene("early", "foundry_titan_dock", listOf("titan_walker_boss")),
        Scene("middle", "orbital_executive_dock", listOf("orbital_fighter", "orbital_fighter")),
        Scene("middle", "orbital_grand_concourse", listOf("compliance_officer")),
        Scene("middle", "deep_tear", listOf("compliance_avatar")),
        Scene("late", "source_zeke_review_loop", listOf("manager_projection")),
        Scene("late", "source_memory_fragments", listOf("source_shadow", "distorted_sentinel", "glitch_hound")),
        Scene("late", "source_center", listOf("ascended_vale"))
    )
    private val builds = linkedMapOf(
        "protection" to listOf("Saboteur", "Protector", "Field Medic", "Precision"),
        "break" to listOf("Saboteur", "Disruptor", "Controller", "Tactical"),
        "precision" to listOf("Improviser", "Disruptor", "Field Medic", "Precision"),
        "unspent" to emptyList()
    )

    fun run(prepared: Map<String, GameSessionState>, preparationLog: List<String>) {
        check(!CombatPlaytestBridge.isOpMode)
        val seeds = (System.getenv("CREW_BALANCE_SEEDS") ?: "3,17,29,41,53,67,79,97,101,113,127,139,151,163,179,191").split(',').map { it.trim().toInt() }
        val checkpoints = listOf("early", "middle", "late").associateWith { requireNotNull(assets.loadCrewCheckpoint(it)) }
        val adapter = MoshiProvider.instance.adapter(GameSessionState::class.java).indent("  ")
        prepared.forEach { (id, snapshot) ->
            assertTrue(snapshot.pendingBattleJson.isBlank())
            assertNull(snapshot.battleCheckpoint)
            assertEquals(checkpoints.getValue(id).playerAp + if (id == "early") 2 else 0, snapshot.playerAp)
            File(output, "prepared-$id.json").writeText(adapter.toJson(snapshot))
        }
        File(output, "preparation.txt").writeText("Public movement, equip, sale, purchase, meal consumption and rest from packaged middle checkpoint.\n" + preparationLog.joinToString("\n"))
        File(output, "parameters.txt").writeText("tempo=$tempo targeted=$targeted coverPolicy=$coverPolicy\n")
        val selectedScenes = if (targeted) scenes.filter { it.room in setOf("orbital_executive_dock", "source_memory_fragments") } else scenes
        val rows = mutableListOf<Map<String, Any>>()
        if (!targeted) for (scene in selectedScenes) for (build in builds.keys) for (seed in seeds) {
            rows += fight(requireNotNull(checkpoints[scene.checkpoint]), scene, build, seed, "tactical", "earned")
        }
        // Paired policy sensitivity after identical public rest/supply preparation.
        for (scene in selectedScenes) for (build in builds.keys)
            for (seed in seeds) for (policy in listOf("tactical", "aggressive")) {
                rows += fight(prepared.getValue(scene.checkpoint), scene, build, seed, policy, "prepared")
            }
        // Reusing exactly the same inputs must reproduce the entire observation, not just win/loss.
        val repeat = fight(prepared.getValue("middle"), scenes[3], "protection", seeds.first(), "tactical", "prepared", writeTrace = false)
        assertEquals(rows.single { it["checkpoint"] == "middle" && it["room"] == scenes[3].room && it["build"] == "protection" && it["seed"] == seeds.first() && it["policy"] == "tactical" && it["start"] == "prepared" }, repeat)
        File(output, "results.json").writeText(MoshiProvider.instance.adapter(List::class.java).indent("  ").toJson(rows))
        val headers = listOf("checkpoint", "start", "room", "build", "policy", "seed", "outcome", "playerActions", "enemyActions", "knockouts", "itemsUsed", "incomingDamage", "healing", "combatMs", "unusedAp")
        File(output, "results.csv").writeText(headers.joinToString(",") + "\n" + rows.joinToString("\n") { row -> headers.joinToString(",") { row.getValue(it).toString() } } + "\n")
        writeSummary(rows, seeds)
        val chains = mutableListOf<Map<String, Any>>()
        if (!targeted) for (build in builds.keys) for (seed in seeds) for (policy in listOf("tactical", "aggressive")) {
            var carry = allocate(prepared.getValue("middle"), build)
            for ((index, scene) in scenes.filter { it.checkpoint == "middle" }.withIndex()) {
                val row = fight(carry, scene, build, seed + index, policy, "attrition", allocateBuild = false) { carry = it }
                chains += row
                if (row["outcome"] != "victory") break
            }
        }
        File(output, "attrition.json").writeText(MoshiProvider.instance.adapter(List::class.java).indent("  ").toJson(chains))
        assertTrue(chains.none { it["outcome"] == "timeout" || it["rejectedCommands"] != 0 })
        assertTrue("Unresolved bounded battles; inspect results before interpreting balance", rows.none { it["outcome"] == "timeout" })
        assertTrue("Rejected player commands invalidate a comparison", rows.all { it["rejectedCommands"] == 0 })
    }

    private fun allocate(snapshot: GameSessionState, build: String): GameSessionState {
        val store = GameSessionStore().apply { restore(snapshot) }
        if (build == "unspent") return store.state.value
        val branches = builds.getValue(build)
        val selected = Crew.trees.mapIndexed { index, tree -> tree.branches.getValue(branches[index]) }
        val capped = "ms_w4_mq20_complete" in snapshot.completedMilestones
        selected.forEachIndexed { index, path ->
            val rank = if (!capped) 3 else if (snapshot.playerAp == 36 && index == (if (build == "protection") 0 else if (build == "break") 3 else 2)) 3 else 4
            path.take(rank).forEach { assertTrue("$build purchase ${it.id}", store.purchasePrototypeNode(it.id)) }
        }
        if (snapshot.playerAp in setOf(26, 28)) {
            (0 until snapshot.playerAp - 24).forEach { index ->
                val other = Crew.trees[index].branches.filterKeys { it != branches[index] }.values.single().first()
                assertTrue(store.purchasePrototypeNode(other.id))
            }
        } else if (snapshot.playerAp == 44) {
            val other = Crew.zeke.branches.filterKeys { it != branches[1] }.values.single()
            other.take(2).forEach { assertTrue(store.purchasePrototypeNode(it.id)) }
            val novaOther = Crew.trees[0].branches.filterKeys { it != branches[0] }.values.single().first()
            assertTrue(store.purchasePrototypeNode(novaOther.id))
        }
        assertEquals("Equal AP investment for $build", 0, store.state.value.playerAp)
        assertEquals(snapshot.playerAp, store.state.value.purchasedSkillNodes.values.sum())
        return store.state.value
    }

    private fun fight(snapshot: GameSessionState, scene: Scene, build: String, seed: Int, policy: String,
                      start: String, writeTrace: Boolean = true, allocateBuild: Boolean = true,
                      onComplete: (GameSessionState) -> Unit = {}): Map<String, Any> {
        val initial = (if (allocateBuild) allocate(snapshot, build) else snapshot).copy(roomId = scene.room, pendingBattleJson = "", battleCheckpoint = null)
        val session = GameSessionStore().apply { restore(initial) }
        val inventory = InventoryService(catalog).apply { loadItems(); restore(initial.inventory) }
        val registry = StatusRegistry(assets.loadStatuses())
        val vm = CombatController(assets, CombatEngine(statusRegistry = registry, random = kotlin.random.Random(seed)), registry,
            session, inventory, catalog, LevelingManager(requireNotNull(assets.loadLevelingData())),
            requireNotNull(assets.loadProgressionData()), AudioRouter(AudioBindings()), themes,
            EnvironmentThemeManager(themes), EncounterCoordinator(), scene.enemies,
            tutorialsEnabled = false, elapsedRealtime = { dispatcher.scheduler.currentTime }, random = SeededCombatRandom(seed),
            crewEncounterTempo = encounterTempo())
        val beginning = requireNotNull(vm.combatState)
        val started = dispatcher.scheduler.currentTime
        val knockouts = mutableSetOf<String>()
        val broken = mutableMapOf<String, Int>()
        var previous = beginning
        var commands = 0
        var rejected = 0
        try {
            vm.onScreenReady()
            for (tick in 0 until 6000) {
                dispatcher.scheduler.advanceTimeBy(50)
                dispatcher.scheduler.runCurrent()
                if (vm.lungeActorId.value != null) vm.onLungeFinished(vm.lungeToken.value)
                if (vm.missLungeActorId.value != null) vm.onMissLungeFinished(vm.missLungeToken.value)
                val current = requireNotNull(vm.combatState)
                current.combatants.forEach { (id, actor) ->
                    if (id in initial.partyMembers && !actor.isAlive && previous.combatants[id]?.isAlive == true) knockouts += id
                    if (actor.breakTurns > 0 && previous.combatants[id]?.breakTurns == 0) broken[id] = broken.getOrDefault(id, 0) + 1
                }
                previous = current
                if (current.outcome != null || commands >= 200) break
                initial.partyMembers.forEach(vm::selectReadyPlayer)
                val actorId = vm.awaitingAction.value ?: continue
                val logSize = current.log.size
                command(vm, current, actorId, policy)
                dispatcher.scheduler.runCurrent()
                commands++
                // Basics can commit after the lunge callback; allow that before measuring rejection.
                if (vm.lungeActorId.value != null) vm.onLungeFinished(vm.lungeToken.value)
                if (vm.missLungeActorId.value != null) vm.onMissLungeFinished(vm.missLungeToken.value)
                if (vm.combatState!!.log.drop(logSize).none { it is CombatLogEntry.ActionQueued && it.actorId == actorId } &&
                    vm.combatState!!.log.drop(logSize).none { it is CombatLogEntry.TurnSkipped && it.actorId == actorId }) rejected++
            }
            val end = requireNotNull(vm.combatState)
            val actions = end.log.filterIsInstance<CombatLogEntry.ActionQueued>()
            val skills = actions.mapNotNull { it.action as? CombatAction.SkillUse }
            val items = actions.mapNotNull { it.action as? CombatAction.ItemUse }
            val row = linkedMapOf<String, Any>(
                "checkpoint" to scene.checkpoint, "start" to start, "room" to scene.room, "build" to build,
                "policy" to policy, "seed" to seed,
                "outcome" to when (end.outcome) { is CombatOutcome.Victory -> "victory"; is CombatOutcome.Defeat -> "defeat"; else -> "timeout" },
                "playerActions" to actions.count { it.actorId in initial.partyMembers },
                "enemyActions" to actions.count { it.actorId !in initial.partyMembers },
                "knockouts" to knockouts.size, "knockedOutCrew" to knockouts.sorted(),
                "itemsUsed" to items.size, "itemUses" to items.groupingBy { it.itemId }.eachCount(),
                "incomingDamage" to end.log.filterIsInstance<CombatLogEntry.Damage>().filter { it.targetId in initial.partyMembers }.sumOf { it.amount },
                "healing" to end.log.filterIsInstance<CombatLogEntry.Heal>().filter { it.targetId in initial.partyMembers }.sumOf { it.amount },
                "combatMs" to dispatcher.scheduler.currentTime - started, "unusedAp" to initial.playerAp,
                "skillsUsed" to skills.filter { it.actorId in initial.partyMembers }.groupingBy { it.skillId }.eachCount(),
                "purchases" to initial.purchasedSkillNodes, "startHp" to beginning.combatants.filterKeys { it in initial.partyMembers }.mapValues { it.value.hp },
                "endHp" to end.combatants.filterKeys { it in initial.partyMembers }.mapValues { it.value.hp },
                "observedBreaks" to broken, "commands" to commands, "rejectedCommands" to rejected)
            if (writeTrace) {
                val name = "${scene.checkpoint}-$start-${scene.room}-$build-$seed-$policy"
                File(output, "$name.txt").writeText("$row\n\nInitial combatants:\n${beginning.combatants}\n\n${end.log.joinToString("\n")}\n")
            }
            onComplete(session.state.value.copy(inventory = inventory.snapshot()))
            return row
        } finally { vm.close(); dispatcher.scheduler.runCurrent() }
    }

    private fun command(vm: CombatController, state: CombatState, actorId: String, policy: String) {
        val allies = state.combatants.values.filter { it.combatant.side == CombatSide.PLAYER && it.isAlive }
        val enemies = state.combatants.values.filter { it.combatant.side == CombatSide.ENEMY && it.isAlive }
        val actor = state.combatants.getValue(actorId)
        val wounded = allies.minBy { it.hp.toDouble() / it.combatant.stats.maxHp }
        val ratio = wounded.hp.toDouble() / wounded.combatant.stats.maxHp
        val available = vm.skillsForPlayer(actorId).filter { vm.canUseSkill(actorId, it) }
        fun cast(id: String, targets: List<String>): Boolean {
            val skill = available.firstOrNull { it.id == id } ?: return false
            vm.useSkill(skill, targets)
            return true
        }
        val groupMissing = allies.sumOf { it.combatant.stats.maxHp - it.hp }
        if (actorId == "nova" && allies.count { it.hp < it.combatant.stats.maxHp * .75 } >= 2 &&
            groupMissing >= (if (policy == "tactical") 90 else 150)) {
            val link = available.firstOrNull { it.id == "nova_link" }
            if (link != null) { vm.useSkill(link); return }
        }
        val healThreshold = if (policy == "tactical") .65 else .30
        if (ratio < healThreshold && actorId == "orion") {
            if (cast(Crew.TREATMENT_SKILL, listOf(wounded.combatant.id))) return
            if (cast(Crew.PATCH_SKILL, listOf(wounded.combatant.id))) return
            if (wounded.combatant.id == actorId && actor.statusEffects.none { it.id == "regen" } &&
                cast("orion_nano_repair", listOf(actorId))) return
        }
        // Spend the smallest sufficient carried restoration; inventory is never replenished in battle.
        if (ratio < (if (policy == "tactical") .40 else .25)) {
            val missing = wounded.combatant.stats.maxHp - wounded.hp
            val item = vm.inventory.value.filter { it.quantity > 0 && (it.item.effect?.restoreHp ?: 0) > 0 }
                .minByOrNull { val heal = it.item.effect!!.restoreHp!!; if (heal >= missing) heal - missing else 10000 - heal }
            if (item != null) { vm.useItem(item, wounded.combatant.id); return }
        }
        if (coverPolicy != "skip" && policy == "tactical" && actorId == "zeke" &&
            ratio < (if (coverPolicy == "proactive") .85 else .55) && wounded.statusEffects.none { it.id == "shield" }) {
            if (cast(Crew.COVER_SKILL, listOf(wounded.combatant.id))) return
        }
        if (policy == "tactical" && actorId == "orion" && enemies.count { it.statusEffects.none { effect -> effect.id == "weak" } } >= 2 && enemies.sumOf { it.hp } > 100) {
            val pulse = available.firstOrNull { it.id == "orion_disruption_pulse" }
            if (pulse != null) { vm.useSkill(pulse); return }
        }
        val target = enemies.minBy { it.hp }
        fun score(skill: Skill): Double {
            val tags = skill.combatTags.orEmpty()
            if (skill.type in setOf("support", "heal", "buff") || tags.any { it in setOf("heal", "buff", "support") } || skill.basePower <= 0) return -1.0
            val count = if (skill.targeting == "all_enemies" || "aoe" in tags) enemies.size else 1
            var score = skill.basePower.toDouble() * count
            if (skill.id == "gh0st_headshot") {
                score *= TalentEffects.hpMultiplier(actor, target, skill.id, true)
                if (Crew.STEADY_AIM in actor.combatant.talents) score *= 1.05
            }
            if (policy == "tactical") {
                score *= TalentEffects.stabilityMultiplier(actor, skill.id, target, "physical" in tags)
                if (target.breakTurns > 0) score *= 1.15
                if (target.statusEffects.none { it.id == "weak" } &&
                    ("weak" in skill.statusApplications.orEmpty() || skill.id == "orion_prism_lance" && Crew.SAFE_EXPOSURE in actor.combatant.talents)) score += 25
                if (skill.id == "zeke_shatter_blow" && Crew.OPENING in actor.combatant.talents) score += 15
                if ("stun" in skill.statusApplications.orEmpty() && target.statusEffects.none { it.id == "stun" }) score += 20 * count
            }
            // Do not spend a whole-party attack to finish one almost-dead target.
            if (count == 1) score = min(score, target.hp.toDouble() + 25)
            return score
        }
        val choice = available.maxByOrNull(::score)?.takeIf { score(it) > 0 }
        if (choice == null) vm.playerAttack(target.combatant.id)
        else vm.useSkill(choice, if (vm.targetRequirementFor(choice) == TargetRequirement.ENEMY) listOf(target.combatant.id) else null)
    }

    private fun writeSummary(rows: List<Map<String, Any>>, seeds: List<Int>) {
        val text = StringBuilder("# Crew build automated comparison\n\nSeeds: $seeds. ${rows.size} independent fights plus one exact deterministic replay.\n\n")
        text.append("All runs use actual controller ATB, cooldowns, status and AI rules; OP mode off. Each comparison restores the same earned save. Builds spend equal AP; unspent is a core-ability control. Scene placement is an explicit encounter fixture, not a traversal or full-campaign claim.\n\n")
        text.append("Prepared starts use public rest (early), shopping/equip/meal/rest (middle), or a carried meal and rest (late). See preparation.txt and prepared snapshots. Other starts preserve saved wounds/supplies. No gear/HP/AP grants. The separate attrition.json carries HP, inventory, earned rewards and meal duration across three middle encounters without rest or replenishment. Scene placement is still a fixture. No optimal-policy claim.\n\n")
        text.append("| Checkpoint/start | Encounter | Policy | Build | Wins | Mean crew actions | Mean KOs | Mean items |\n|---|---|---|---|---:|---:|---:|---:|\n")
        rows.groupBy { listOf(it["checkpoint"], it["start"], it["room"], it["policy"], it["build"]) }.forEach { (key, group) ->
            fun mean(field: String) = "%.2f".format(java.util.Locale.US, group.map { (it[field] as Number).toDouble() }.average())
            text.append("| ${key[0]}/${key[1]} | ${key[2]} | ${key[3]} | ${key[4]} | ${group.count { it["outcome"] == "victory" }}/${group.size} | ${mean("playerActions")} | ${mean("knockouts")} | ${mean("itemsUsed")} |\n")
        }
        text.append("\n## Purchased active usage\n\n")
        val activeIds = Crew.nodes.values.filter { it.effect?.type == "active" }.mapNotNull { it.effect?.subtype }
        activeIds.forEach { id ->
            val eligible = rows.filter { row -> @Suppress("UNCHECKED_CAST") val paid = row["purchases"] as Map<String, Int>; id in Crew.activeSkills(paid) }
            val used = eligible.count { row -> @Suppress("UNCHECKED_CAST") val uses = row["skillsUsed"] as Map<String, Int>; uses.getOrDefault(id, 0) > 0 }
            text.append("- $id: used in $used/${eligible.size} eligible fights. Absence may reflect policy or cooldown opportunities, not a nonworking ability.\n")
        }
        File(output, "summary.md").writeText(text.toString())
    }
}

