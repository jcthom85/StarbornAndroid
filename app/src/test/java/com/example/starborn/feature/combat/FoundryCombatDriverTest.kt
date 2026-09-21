package com.example.starborn.feature.combat

import androidx.lifecycle.viewModelScope
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.*
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.inventory.*
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.*
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import com.example.starborn.feature.combat.viewmodel.TargetRequirement
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*
import java.io.File

/** Real runtime, explicit low-gear level-9 fixture; not a campaign-earned balance baseline. */
@OptIn(ExperimentalCoroutinesApi::class)
class FoundryCombatDriverTest {
    private val dispatcher = StandardTestDispatcher()
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    @Before fun setup() = Dispatchers.setMain(dispatcher)
    @After fun cleanup() = Dispatchers.resetMain()

    @Test fun completeFoundryFightsAndExportEvidence() {
        val parties = listOf(listOf("magma_drone"), listOf("slag_golem"), listOf("welder_bot"),
            listOf("welder_bot", "magma_drone"), listOf("slag_golem", "welder_bot"),
            listOf("slag_golem", "magma_drone"))
        val output = File("build/reports/foundry-driver").apply { mkdirs() }
        val summaries = mutableListOf<String>()
        val seeds = (System.getenv("FOUNDRY_SEEDS") ?: "41").split(',').map { it.trim().toInt() }
        val loadouts = (System.getenv("FOUNDRY_LOADOUTS") ?: "weak,purchased").split(',')
        val fixtureXp = (System.getenv("FOUNDRY_XP") ?: "11000").toInt()
        // Keep unlocks/stats intact while holding offensive choices to the lower-level set.
        val skillPolicy = System.getenv("FOUNDRY_SKILL_POLICY") ?: "all"
        val isolatedSkills = mapOf("overload" to "zeke_overload_fists",
            "disruption" to "orion_disruption_pulse", "crash" to "gh0st_system_crash")
        require(skillPolicy in setOf("all", "pre9") + isolatedSkills.keys)
        val excludedSkills = if (skillPolicy != "all") requireNotNull(assets.loadProgressionData())
            .levelUpSkills.values.flatMap { tiers -> tiers.filterKeys { it.toInt() >= 9 }.values }.toSet() -
                setOfNotNull(isolatedSkills[skillPolicy])
            else emptySet()
        val policies = (System.getenv("FOUNDRY_POLICIES") ?: "first,support,pressure,defensive_support,defensive_pressure").split(',')
        require(policies.all { it in setOf("first", "support", "pressure", "defensive_support", "defensive_pressure") })
        for (seed in seeds) for (loadout in loadouts)
        for ((index, enemies) in parties.withIndex()) for (policy in policies) {
            val party = listOf("nova", "zeke", "orion", "gh0st")
            val session = GameSessionStore().apply { restore(FoundryLoadouts.session(loadout, assets, fixtureXp)) }
            val items = reader.readList<Item>("items.json").associateBy { it.id }
            val catalog = object : ItemCatalog {
                override fun load() = Unit
                override fun findItem(idOrAlias: String) = items[idOrAlias]
            }
            val registry = StatusRegistry(assets.loadStatuses())
            val themes = mock<ThemeRepository> {
                on { getTheme(any()) } doReturn null
                on { getStyle(any()) } doReturn null
            }
            val vm = CombatViewModel(worldAssets = assets, combatEngine = CombatEngine(statusRegistry = registry),
                statusRegistry = registry, sessionStore = session,
                inventoryService = InventoryService(catalog).apply { loadItems(); restore(session.state.value.inventory) }, itemCatalog = catalog,
                levelingManager = LevelingManager(requireNotNull(assets.loadLevelingData())),
                progressionData = requireNotNull(assets.loadProgressionData()),
                audioRouter = AudioRouter(AudioBindings()), themeRepository = themes,
                environmentThemeManager = EnvironmentThemeManager(themes), encounterCoordinator = EncounterCoordinator(),
                enemyIds = enemies, tutorialsEnabled = false,
                elapsedRealtime = { dispatcher.scheduler.currentTime }, random = SeededCombatRandom(seed))
            val name = "$seed-$loadout-$index-$policy" + (if (fixtureXp == 11000) "" else "-xp$fixtureXp") +
                (if (skillPolicy == "all") "" else "-$skillPolicy")
            val start = dispatcher.scheduler.currentTime
            var opportunities = 0
            var exploited = 0
            var commands = 0
            try {
                File(output, "$name-fixture.txt").writeText("seed=$seed policy=$policy\n${session.state.value}\n" +
                    requireNotNull(vm.combatState).combatants.values.joinToString("\n") { it.combatant.toString() })
                for (tick in 0 until 4800) {
                    dispatcher.scheduler.advanceTimeBy(250)
                    dispatcher.scheduler.runCurrent()
                    // A headless presentation acknowledges completed animations through production callbacks.
                    if (vm.lungeActorId.value != null) vm.onLungeFinished(vm.lungeToken.value)
                    if (vm.missLungeActorId.value != null) vm.onMissLungeFinished(vm.missLungeToken.value)
                    val state = requireNotNull(vm.combatState)
                    if (state.outcome != null || state.log.count { it is CombatLogEntry.ActionQueued } >= 200) break
                    party.forEach(vm::selectReadyPlayer)
                    val actor = vm.awaitingAction.value ?: continue
                    val wounded = state.combatants.values.filter { it.combatant.side == CombatSide.PLAYER && it.isAlive &&
                        it.hp.toDouble() / it.combatant.stats.maxHp < 0.4 }.minByOrNull { it.hp.toDouble() / it.combatant.stats.maxHp }
                    val medkit = vm.inventory.value.firstOrNull { it.item.id == "medkit" && it.quantity > 0 }
                    if (wounded != null && medkit != null) {
                        vm.useItem(medkit, wounded.combatant.id)
                        commands++
                        continue
                    }
                    // Match the UI's automatic support targeting; do not grant ally targeting
                    // to a self-targeted skill such as Nano Repair.
                    if (policy.startsWith("defensive_")) {
                        val self = state.combatants.getValue(actor)
                        val supportId = when {
                            actor == "orion" && self.hp < self.combatant.stats.maxHp * 0.75 &&
                                self.statusEffects.none { it.id == "regen" } -> "orion_nano_repair"
                            actor == "nova" && self.statusEffects.none { it.id == "shield" } -> "nova_smoke_bomb"
                            actor == "zeke" && self.statusEffects.none { it.id == "guard" || it.id == "braced" } -> "zeke_bulwark_stance"
                            else -> null
                        }
                        val support = vm.skillsForPlayer(actor).firstOrNull { it.id == supportId && vm.canUseSkill(actor, it) }
                        if (support != null) {
                            vm.useSkill(support)
                            commands++
                            continue
                        }
                    }
                    val living = state.combatants.values.filter { it.combatant.side == CombatSide.ENEMY && it.isAlive }
                    val exposed = living.firstOrNull { it.statusEffects.any { effect -> effect.id == "radiators_exposed" } }
                    if (exposed != null) opportunities++
                    val target = when (policy) {
                        "support", "defensive_support" -> living.firstOrNull { it.combatant.id.startsWith("welder_bot") } ?: exposed
                        "pressure", "defensive_pressure" -> living.firstOrNull { it.combatant.id.startsWith("magma_drone") } ?: exposed
                        else -> null
                    } ?: living.firstOrNull() ?: continue
                    if (target == exposed) exploited++
                    val priorities = listOf("zeke_overload_fists", "orion_disruption_pulse", "gh0st_system_crash",
                        "gh0st_venom_edge", "nova_arc_tether", "zeke_shatter_blow", "orion_prism_lance", "gh0st_headshot")
                    val skill = vm.skillsForPlayer(actor).filter { it.id in priorities && it.id !in excludedSkills && vm.canUseSkill(actor, it) }
                        .minByOrNull { priorities.indexOf(it.id) }
                    if (skill != null) {
                        val targets = if (vm.targetRequirementFor(skill) == TargetRequirement.ENEMY)
                            listOf(target.combatant.id) else null
                        vm.useSkill(skill, targets)
                    } else vm.playerAttack(target.combatant.id)
                    commands++
                }
                val end = requireNotNull(vm.combatState)
                File(output, "$name-trace.txt").writeText(end.log.joinToString("\n"))
                val skills = end.log.filterIsInstance<CombatLogEntry.ActionQueued>()
                    .mapNotNull { it.action as? CombatAction.SkillUse }
                val healing = end.log.filterIsInstance<CombatLogEntry.Heal>().filter { it.sourceId.startsWith("welder_bot") }.sumOf { it.amount }
                val incoming = end.log.filterIsInstance<CombatLogEntry.Damage>().filter { it.targetId in party }.sumOf { it.amount }
                summaries += "$name enemies=$enemies outcome=${end.outcome} ms=${dispatcher.scheduler.currentTime-start} " +
                    "commands=$commands actions=${end.log.count { it is CombatLogEntry.ActionQueued }} incoming=$incoming " +
                    "repairUses=${skills.count { it.skillId == "field_weld" }} repairAmount=$healing " +
                    "cooling=${skills.count { it.skillId == "slag_cooldown" }} opportunities=$opportunities exploited=$exploited " +
                    "medkits=${(if (loadout.removeSuffix("_ap") in setOf("weak", "purchased")) 3 else 0) - (vm.inventory.value.firstOrNull { it.item.id == "medkit" }?.quantity ?: 0)} " +
                    "supportUses=${skills.count { it.skillId in setOf("orion_nano_repair", "nova_smoke_bomb", "zeke_bulwark_stance") }} " +
                    "hp=${end.combatants.filterKeys { it in party }.mapValues { it.value.hp }}"
                File(output, "summary.txt").writeText(summaries.joinToString("\n"))
                assertNotNull("$name timed out; see $output", end.outcome)
                assertTrue("$name exceeded finite repairs", skills.count { it.skillId == "field_weld" } <= 2)
                assertTrue("$name never issued player commands", commands > 0)
            } finally { vm.viewModelScope.cancel(); dispatcher.scheduler.runCurrent() }
        }
    }
}
