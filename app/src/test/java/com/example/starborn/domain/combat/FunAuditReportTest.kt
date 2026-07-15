package com.example.starborn.domain.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.domain.model.Enemy
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Player
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.Skill
import com.example.starborn.domain.model.StatusDefinition
import com.squareup.moshi.Types
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.ceil

class FunAuditReportTest {
    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val assets = File(root, "app/src/main/assets")
    private val reports = File(root, "reports/fun-audit")
    private val moshi = MoshiProvider.instance

    @Test
    fun `generate deterministic combat and skill decision audits`() {
        reports.mkdirs()
        val events = readList<GameEvent>("events.json")
        val rooms = readList<Room>("rooms.json")
        val enemies = readList<Enemy>("enemies.json")
        val players = readList<Player>("characters.json")
        val skills = readList<Skill>("skills.json")
        val statuses = readList<StatusDefinition>("statuses.json")

        val combat = combatAudit(events, rooms, enemies, players, skills, statuses)
        File(reports, "combat-variety.json").writeText(combat.toString(2))
        File(reports, "combat-variety.md").writeText(combatMarkdown(combat))

        val skill = skillAudit(skills, players.map { it.id }.toSet())
        File(reports, "skill-decisions.json").writeText(skill.toString(2))
        File(reports, "skill-decisions.md").writeText(skillMarkdown(skill))

        assertEquals(99, skills.size)
        assertTrue(skills.any { it.id == "nova_link" })
        assertEquals(25, combat.getJSONArray("encounters").length())
        assertEquals(0, combat.getJSONArray("unresolved").length())
    }

    private fun combatAudit(
        events: List<GameEvent>,
        rooms: List<Room>,
        enemies: List<Enemy>,
        players: List<Player>,
        skills: List<Skill>,
        statuses: List<StatusDefinition>
    ): JSONObject {
        val roomById = rooms.associateBy { it.id }
        val enemyById = enemies.associateBy { it.id }
        val skillById = skills.associateBy { it.id }
        val overrides = mapOf(
            "w3_mq11_clear_landing" to EncounterOverride("spire_vent_output", listOf("aero_drone"), "Event omits encounter identity; uses the authored Spire security baseline.")
        )
        val required = events.filter { event ->
            event.trigger.type == "encounter_victory" && event.conditions.any {
                it.type == "quest_active" && it.questId?.contains("_mq") == true
            }
        }
        val unresolved = JSONArray()
        val results = JSONArray()

        required.forEach { event ->
            val questId = event.conditions.first { it.type == "quest_active" && it.questId?.contains("_mq") == true }.questId!!
            val override = overrides[event.id]
            val roomId = override?.roomId ?: event.trigger.room ?: event.trigger.roomId
            val enemyIds = override?.enemyIds
                ?: event.trigger.enemies.orEmpty().ifEmpty { roomId?.let(roomById::get)?.enemies.orEmpty() }
            val definitions = enemyIds.mapNotNull(enemyById::get)
            if (enemyIds.isEmpty() || definitions.size != enemyIds.size) {
                unresolved.put(JSONObject().put("event_id", event.id).put("quest_id", questId).put("enemy_ids", JSONArray(enemyIds)))
                return@forEach
            }
            val world = questId.drop(1).substringBefore("_").toIntOrNull()?.coerceIn(1, 6) ?: 1
            val policies = JSONObject()
            Policy.entries.forEach { policy ->
                val runs = (0 until 20).map { seed ->
                    simulate(world, definitions, players, skills, statuses, skillById, policy, seed)
                }
                policies.put(policy.id, policySummary(runs))
            }
            val tactical = policies.getJSONObject(Policy.TACTICAL.id)
            val greedy = policies.getJSONObject(Policy.GREEDY.id)
            val dominantShare = tactical.getDouble("dominant_action_share")
            val maxStreak = tactical.getInt("max_repeated_streak")
            val phaseChange = definitions.any { it.id == "ascended_god" || it.tags.any { tag -> tag.equals("multi_phase", true) } }
            val tacticalQuestion = tacticalQuestion(definitions)
            val flags = mutableListOf<String>()
            if (tactical.getDouble("win_rate") < 0.8) flags += "baseline_defeat_risk"
            if (tactical.getDouble("win_rate") >= 0.8 && tactical.getDouble("median_rounds") > 6.0 && !phaseChange) flags += "over_six_rounds_without_phase"
            if (dominantShare > 0.60) flags += "dominant_action"
            if (maxStreak >= 3) flags += "repeated_action_streak"
            if (tacticalQuestion == "weakness exploitation" && tactical.getDouble("median_rounds") >= greedy.getDouble("median_rounds")) flags += "no_tactical_speed_advantage"
            if (tacticalQuestion == "raw damage") flags += "no_distinct_tactical_question"

            results.put(
                JSONObject()
                    .put("event_id", event.id)
                    .put("quest_id", questId)
                    .put("world", world)
                    .put("room_id", roomId)
                    .put("enemy_ids", JSONArray(enemyIds))
                    .put("metadata_override", override?.note)
                    .put("phase_change", phaseChange)
                    .put("tactical_question", tacticalQuestion)
                    .put("policies", policies)
                    .put("flags", JSONArray(flags))
            )
        }
        return JSONObject()
            .put("schema", 1)
            .put("required_encounter_rule", "encounter_victory conditioned on an active main quest")
            .put("seeds_per_policy", 20)
            .put("encounters", results)
            .put("unresolved", unresolved)
    }

    private fun simulate(
        world: Int,
        enemyDefinitions: List<Enemy>,
        players: List<Player>,
        skills: List<Skill>,
        statuses: List<StatusDefinition>,
        skillById: Map<String, Skill>,
        policy: Policy,
        seed: Int
    ): RunResult {
        val partySize = when (world) { 1 -> 2; 2 -> 3; else -> 4 }
        val party = players.take(partySize).map { player -> playerCombatant(player, world, skills) }
        val enemyParty = enemyDefinitions.mapIndexed { index, enemy -> enemyCombatant(enemy, index) }
        val registry = StatusRegistry(statuses)
        val engine = CombatEngine(statusRegistry = registry)
        val processor = CombatActionProcessor(engine, registry, skillById::get, forcePhysicalHit = { _, _ -> true }, random = SeededCombatRandom(seed))
        var state = engine.beginEncounter(CombatSetup(party, enemyParty))
        val actions = mutableListOf<String>()
        var guard = 0
        while (state.outcome == null && guard++ < 800) {
            val actor = state.activeCombatant ?: break
            val enemiesAlive = state.combatants.values.filter { it.isAlive && it.combatant.side == CombatSide.ENEMY }
            val playersAlive = state.combatants.values.filter { it.isAlive && it.combatant.side != CombatSide.ENEMY }
            val target = if (actor.combatant.side == CombatSide.ENEMY) playersAlive.firstOrNull() else enemiesAlive.minByOrNull { it.hp }
            if (target == null) {
                state = engine.resolveOutcome(state) { CombatReward() }
                continue
            }
            val action = if (actor.combatant.side == CombatSide.ENEMY) {
                CombatAction.BasicAttack(actor.combatant.id, target.combatant.id)
            } else {
                selectPlayerAction(actor, target, state, policy, skillById)
            }
            val before = state
            var after = processor.execute(state, action) { CombatReward() }
            if (after === before || after == before) {
                after = processor.execute(state, CombatAction.BasicAttack(actor.combatant.id, target.combatant.id)) { CombatReward() }
            }
            if (actor.combatant.side != CombatSide.ENEMY) actions += "${actor.combatant.id}:${actionKey(action)}"
            state = after
        }
        val victory = state.outcome is CombatOutcome.Victory
        return RunResult(state.round.coerceAtMost(99), victory, actions)
    }

    private fun selectPlayerAction(
        actor: CombatantState,
        target: CombatantState,
        state: CombatState,
        policy: Policy,
        skills: Map<String, Skill>
    ): CombatAction {
        if (policy == Policy.BASIC) return CombatAction.BasicAttack(actor.combatant.id, target.combatant.id)
        val available = actor.combatant.skills.mapNotNull(skills::get)
            .filter {
                it.basePower > 0 &&
                    actor.activeCooldowns.getOrDefault(it.id, 0) == 0 &&
                    conditionsMet(it, target)
            }
        val waitingPayoffStatuses = if (policy == Policy.TACTICAL) {
            state.combatants.values
                .filter { it.isAlive && it.combatant.side == actor.combatant.side }
                .flatMap { ally -> ally.combatant.skills.mapNotNull(skills::get) }
                .flatMap { it.conditions.orEmpty() }
                .mapNotNull { condition ->
                    when (condition.lowercase()) {
                        "target_stunned", "bonus_if_target_stunned" -> "stun"
                        "target_staggered", "bonus_if_target_staggered" -> "stagger"
                        else -> null
                    }
                }
                .toSet()
        } else emptySet()
        val selected = available.maxByOrNull { skill: Skill ->
            val multiplier = if (policy == Policy.TACTICAL) affinity(target.combatant.resistances, skill) else 1.0
            val setupBonus = if (skill.statusApplications.orEmpty().any { it.lowercase() in waitingPayoffStatuses }) 90.0 else 0.0
            skill.basePower * multiplier + if (policy == Policy.TACTICAL) skill.statusApplications.orEmpty().size * 12.0 + setupBonus else 0.0
        }
        return selected?.let { CombatAction.SkillUse(actor.combatant.id, it.id, listOf(target.combatant.id)) }
            ?: CombatAction.BasicAttack(actor.combatant.id, target.combatant.id)
    }

    private fun conditionsMet(skill: Skill, target: CombatantState): Boolean = skill.conditions.orEmpty().all { condition ->
        when (condition.lowercase()) {
            "target_stunned" -> target.statusEffects.any { it.id.equals("stun", true) || it.id.equals("stagger", true) }
            "target_staggered" -> target.statusEffects.any { it.id.equals("stagger", true) }
            "bonus_if_target_stunned", "bonus_if_target_staggered" -> true
            else -> true
        }
    }

    private fun playerCombatant(player: Player, world: Int, skills: List<Skill>): Combatant {
        val growth = (world - 1) * 3
        val owned = skills.filter { it.character == player.id && it.type != "passive" }.map { it.id }
        return Combatant(
            id = player.id,
            name = player.name,
            side = CombatSide.PLAYER,
            stats = StatBlock(
                maxHp = CombatFormulas.maxHp(player.hp + world * 35, player.vitality + growth),
                strength = player.strength + growth,
                vitality = player.vitality + growth,
                agility = player.agility + growth,
                focus = player.focus + growth,
                luck = player.luck + growth,
                speed = CombatFormulas.speed(world * 2, player.agility + growth).toInt(),
                stability = 100
            ),
            skills = owned
        )
    }

    private fun enemyCombatant(enemy: Enemy, index: Int): Combatant {
        val profile = ElementalAffinityRules.applyOverrides(ElementalAffinityRules.fromTags(enemy.tags), enemy.resistances)
        val hp = CombatFormulas.maxHp(enemy.hp, enemy.vitality)
        return Combatant(
            id = if (index == 0) enemy.id else "${enemy.id}#$index",
            name = enemy.name,
            side = CombatSide.ENEMY,
            stats = StatBlock(hp, enemy.strength, enemy.vitality, enemy.agility, enemy.focus, enemy.luck, CombatFormulas.speed(enemy.speed, enemy.agility).toInt(), enemy.stability ?: CombatFormulas.stabilityForTier(hp, enemy.tier)),
            resistances = profile,
            skills = enemy.abilities,
            brokenTurns = enemy.brokenTurns ?: 1
        )
    }

    private fun policySummary(runs: List<RunResult>): JSONObject {
        val rounds = runs.map { it.rounds }.sorted()
        val allActions = runs.flatMap { it.actions }
        val counts = allActions.groupingBy { it }.eachCount()
        val dominant = allActions.groupBy { it.substringBefore(':') }
            .values
            .flatMap { actorActions ->
                actorActions.groupingBy { it }.eachCount().entries.map { entry -> entry to actorActions.size }
            }
            .maxByOrNull { (entry, total) -> entry.value.toDouble() / total }
        return JSONObject()
            .put("median_rounds", median(rounds))
            .put("worst_rounds", rounds.maxOrNull() ?: 0)
            .put("win_rate", runs.count { it.victory }.toDouble() / runs.size)
            .put("dominant_action", dominant?.first?.key)
            .put("dominant_action_share", dominant?.let { (entry, total) -> entry.value.toDouble() / total } ?: 0.0)
            .put("max_repeated_streak", runs.maxOfOrNull { run ->
                run.actions.groupBy { it.substringBefore(':') }.values.maxOfOrNull(::longestStreak) ?: 0
            } ?: 0)
            .put("action_counts", JSONObject(counts))
    }

    private fun skillAudit(skills: List<Skill>, playerIds: Set<String>): JSONObject {
        val findings = JSONArray()
        skills.groupBy { it.character }.values.forEach { owned ->
            owned.groupBy(::signature).values.filter { it.size > 1 }.forEach { group ->
                findings.put(finding("exact_duplicate", group.map { it.id }, "Identical owner, power, cooldown, tags, statuses, scaling, limits, and conditions."))
            }
            if (owned.firstOrNull()?.character in playerIds) {
                owned.forEach { weaker ->
                    owned.filter { it.id != weaker.id && role(it) == role(weaker) }.firstOrNull { stronger -> dominates(stronger, weaker) }?.let { stronger ->
                        findings.put(finding("potentially_dominated", listOf(weaker.id, stronger.id), "${stronger.id} has no weaker power/cooldown/status package."))
                    }
                }
            }
        }
        skills.filter { it.basePower >= 90 && it.cooldown <= 1 }.forEach {
            findings.put(finding("rotation_risk", listOf(it.id), "High base power on a zero/one-turn cooldown."))
        }
        val produced = skills.flatMap { skill -> skill.statusApplications.orEmpty().map { it.lowercase() to skill.id } }.groupBy({ it.first }, { it.second })
        val consumers = skills.flatMap { skill -> skill.conditions.orEmpty().mapNotNull { condition ->
            produced.keys.firstOrNull { condition.lowercase().contains(it) }?.let { it to skill.id }
        }}.groupBy({ it.first }, { it.second })
        if (consumers.isEmpty()) {
            findings.put(finding("no_conditional_payoff_skills", emptyList(), "No skill condition consumes a status produced by another skill; setup payoff currently comes from systemic status and weakness rules."))
        }
        consumers.filterKeys { it !in produced }.forEach { (status, payoffSkills) ->
            findings.put(finding("payoff_without_setup", payoffSkills.distinct(), "Condition '$status' has no skill producer."))
        }
        return JSONObject()
            .put("schema", 1)
            .put("skill_count", skills.size)
            .put("player_skill_count", skills.count { it.character in playerIds })
            .put("enemy_skill_count", skills.count { it.character !in playerIds })
            .put("findings", findings)
            .put("compatibility", JSONObject().apply {
                produced.toSortedMap().forEach { (status, producers) -> put(status, JSONObject().put("producers", JSONArray(producers.distinct())).put("consumers", JSONArray(consumers[status].orEmpty().distinct()))) }
            })
    }

    private fun signature(skill: Skill) = listOf(skill.basePower, skill.cooldown, skill.combatTags.orEmpty().sorted(), skill.statusApplications.orEmpty().sorted(), skill.scaling, skill.usesPerBattle, skill.conditions.orEmpty().sorted()).joinToString("|")
    private fun role(skill: Skill) = if (skill.basePower > 0) "damage:${element(skill)}" else "support:${skill.combatTags.orEmpty().sorted()}"
    private fun dominates(a: Skill, b: Skill): Boolean {
        val limitOk = a.usesPerBattle == null || (b.usesPerBattle != null && a.usesPerBattle >= b.usesPerBattle)
        val conditionsOk = a.conditions.orEmpty().size <= b.conditions.orEmpty().size
        return a.basePower >= b.basePower && a.cooldown <= b.cooldown && limitOk && conditionsOk && a.statusApplications.orEmpty().containsAll(b.statusApplications.orEmpty()) && (a.basePower > b.basePower || a.cooldown < b.cooldown || a.statusApplications.orEmpty().size > b.statusApplications.orEmpty().size)
    }

    private fun tacticalQuestion(enemies: List<Enemy>): String = when {
        enemies.size > 1 -> "target priority"
        enemies.any { it.tier.equals("boss", true) } -> "boss pressure and break timing"
        enemies.any { enemy -> enemy.resistances.let { listOf(it.burn, it.freeze, it.shock, it.acid, it.source, it.physical).any { value -> value != null && value < 0 } } } -> "weakness exploitation"
        enemies.any { (it.stability ?: 0) > 0 || it.tags.any { tag -> tag.contains("shield", true) } } -> "guard break and stagger"
        enemies.any { it.abilities.size > 1 } -> "status and cooldown response"
        else -> "raw damage"
    }

    private fun affinity(profile: ResistanceProfile, skill: Skill): Double {
        val value = when (element(skill)) { "burn" -> profile.burn; "freeze" -> profile.freeze; "shock" -> profile.shock; "acid" -> profile.acid; "source" -> profile.source; else -> profile.physical }
        return ElementalAffinityRules.tierForValue(value).multiplier
    }
    private fun element(skill: Skill): String = skill.combatTags.orEmpty().map { it.lowercase() }.firstOrNull { it in setOf("burn", "fire", "freeze", "ice", "shock", "acid", "source", "physical") }?.let { if (it == "fire") "burn" else if (it == "ice") "freeze" else it } ?: "physical"
    private fun actionKey(action: CombatAction): String = when (action) { is CombatAction.SkillUse -> action.skillId; is CombatAction.BasicAttack -> "basic_attack"; is CombatAction.Defend -> "defend"; else -> action::class.simpleName ?: "action" }
    private fun longestStreak(actions: List<String>): Int { var best = 0; var run = 0; var previous: String? = null; actions.forEach { if (it == previous) run++ else { previous = it; run = 1 }; best = maxOf(best, run) }; return best }
    private fun median(values: List<Int>): Double = if (values.isEmpty()) 0.0 else if (values.size % 2 == 1) values[values.size / 2].toDouble() else (values[values.size / 2 - 1] + values[values.size / 2]) / 2.0
    private fun finding(type: String, skillIds: List<String>, reason: String) = JSONObject().put("type", type).put("skill_ids", JSONArray(skillIds)).put("reason", reason)

    private fun combatMarkdown(report: JSONObject): String = buildString {
        appendLine("# Combat Variety Audit")
        appendLine()
        appendLine("Required encounters: ${report.getJSONArray("encounters").length()}; fixed seeds per policy: ${report.getInt("seeds_per_policy")}.")
        appendLine()
        appendLine("| World | Quest | Encounter | Tactical median | Greedy median | Tactical question | Flags |")
        appendLine("|---|---|---|---:|---:|---|---|")
        report.getJSONArray("encounters").forEachObject { item ->
            val policies = item.getJSONObject("policies")
            appendLine("| ${item.getInt("world")} | ${item.getString("quest_id")} | ${item.getJSONArray("enemy_ids").join(", ")} | ${policies.getJSONObject("tactical").getDouble("median_rounds")} | ${policies.getJSONObject("greedy").getDouble("median_rounds")} | ${item.getString("tactical_question")} | ${item.getJSONArray("flags").join(", ")} |")
        }
    }

    private fun skillMarkdown(report: JSONObject): String = buildString {
        appendLine("# Skill Decision Audit")
        appendLine()
        appendLine("Catalog: ${report.getInt("skill_count")} skills (${report.getInt("player_skill_count")} player, ${report.getInt("enemy_skill_count")} enemy).")
        appendLine()
        appendLine("| Finding | Skills | Reason |")
        appendLine("|---|---|---|")
        report.getJSONArray("findings").forEachObject { item -> appendLine("| ${item.getString("type")} | ${item.getJSONArray("skill_ids").join(", ")} | ${item.getString("reason")} |") }
    }

    private inline fun <reified T> readList(name: String): List<T> {
        val type = Types.newParameterizedType(List::class.java, T::class.java)
        return requireNotNull(moshi.adapter<List<T>>(type).fromJson(File(assets, name).readText()))
    }

    private fun JSONArray.forEachObject(block: (JSONObject) -> Unit) { for (index in 0 until length()) block(getJSONObject(index)) }

    private data class EncounterOverride(val roomId: String, val enemyIds: List<String>, val note: String)
    private data class RunResult(val rounds: Int, val victory: Boolean, val actions: List<String>)
    private enum class Policy(val id: String) { BASIC("basic"), GREEDY("greedy"), TACTICAL("tactical") }
}
