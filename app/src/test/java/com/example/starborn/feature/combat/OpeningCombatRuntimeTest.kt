package com.example.starborn.feature.combat

import androidx.lifecycle.viewModelScope
import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.WorldAssetDataSource
import com.example.starborn.data.repository.ThemeRepository
import com.example.starborn.domain.audio.AudioBindings
import com.example.starborn.domain.audio.AudioRouter
import com.example.starborn.domain.combat.*
import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.leveling.LevelingManager
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.theme.EnvironmentThemeManager
import com.example.starborn.feature.combat.viewmodel.CombatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Attainable opening checkpoint: solo Nova, starting skills, no optional gear
 * equipped. This is not a campaign-earned reward snapshot or a balance audit.
 * Only presentation services are mocked; assets, mapping, ATB, commands and AI
 * are production code. Random outcomes are deliberately not win-rate assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OpeningCombatRuntimeTest {
    private val dispatcher = StandardTestDispatcher()
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun `unavailable skill explanations match availability gates`() {
        val vm = createCombat()
        try {
            val skill = vm.skillsForPlayer("nova").single()
            assertNull(vm.skillUnavailableReason("nova", skill))
            assertTrue(vm.canUseSkill("nova", skill))
            assertEquals("Character unavailable", vm.skillUnavailableReason("missing", skill))
            val exhausted = skill.copy(usesPerBattle = 0)
            assertEquals("Battle use limit reached", vm.skillUnavailableReason("nova", exhausted))
            assertFalse(vm.canUseSkill("nova", exhausted))
            val conditional = skill.copy(conditions = listOf("hp_below_0"))
            assertEquals("Requirements not met - see details", vm.skillUnavailableReason("nova", conditional))
            assertFalse(vm.canUseSkill("nova", conditional))
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test fun `opening fixture uses solo Nova and only her starting skill`() {
        val vm = createCombat()
        try {
            val state = requireNotNull(vm.combatState)
            val party = state.combatants.values.filter { it.combatant.side == CombatSide.PLAYER }
            assertEquals(listOf("nova"), party.map { it.combatant.id })
            assertEquals(setOf("nova_arc_tether"), vm.skillsForPlayer("nova").map { it.id }.toSet())
            val nova = assets.loadCharacters().single { it.id == "nova" }
            assertEquals(nova.strength, party.single().combatant.stats.strength)
            assertEquals(CombatFormulas.maxHp(nova.hp, nova.vitality), party.single().hp)
            assertTrue(vm.skillsForPlayer("zeke").isEmpty())
            assertEquals(listOf("faulted_loader"), vm.enemies.map { it.id })
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `production ATB lets the loader act without player input`() {
        val vm = createCombat()
        try {
            val initial = requireNotNull(vm.combatState)
            val initialHp = initial.combatants.getValue("nova").hp
            // A finite virtual-time window prevents an idle ATB loop hanging CI.
            repeat(240) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                if (requireNotNull(vm.combatState).combatants.getValue("nova").hp < initialHp) return
            }
            fail("The loader never damaged Nova during 60 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `starting skill command is accepted by production combat runtime`() {
        val vm = createCombat()
        try {
            val skill = vm.skillsForPlayer("nova").single()
            repeat(80) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                vm.selectReadyPlayer("nova")
                if (vm.awaitingAction.value == "nova") {
                    assertTrue(vm.canUseSkill("nova", skill))
                    val before = vm.combatState
                    vm.useSkill(skill, listOf("faulted_loader"))
                    dispatcher.scheduler.runCurrent()
                    assertNotEquals("A legal starting skill must change combat state", before, vm.combatState)
                    return
                }
            }
            fail("Nova never became selectable during 20 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `combat rewards preserve a level earned under the previous curve`() {
        val session = GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_1", roomId = "workshop_yard",
                playerId = "nova", partyMembers = listOf("nova"), playerLevel = 12,
                partyMemberLevels = mapOf("nova" to 12), playerXp = 4900,
                partyMemberXp = mapOf("nova" to 4900)))
        }
        val vm = createCombat(session)
        try {
            val method = CombatViewModel::class.java.getDeclaredMethod("applyVictoryRewards", CombatReward::class.java)
            method.isAccessible = true
            method.invoke(vm, CombatReward(xp = 10))
            assertEquals(12, session.state.value.playerLevel)
            assertEquals(12, session.state.value.partyMemberLevels["nova"])
            assertEquals(4910, session.state.value.playerXp)
        } finally { vm.viewModelScope.cancel() }
    }

    // Estimated World 4 main-route checkpoint, not a campaign-earned equipment
    // snapshot. Keep optional gear absent and exclude unearned level-12 skills.
    private fun world4Session(): GameSessionStore {
        val party = listOf("nova", "zeke", "orion", "gh0st")
        return GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_4", playerId = "nova",
                partyMembers = party, playerLevel = 9, playerXp = 11670,
                partyMemberLevels = party.associateWith { 9 },
                partyMemberXp = party.associateWith { 11670 },
                unlockedSkills = setOf("nova_smoke_bomb", "nova_plasma_burst",
                    "zeke_bulwark_stance", "zeke_overload_fists",
                    "orion_nano_repair", "orion_disruption_pulse",
                    "gh0st_venom_edge", "gh0st_system_crash")))
        }
    }

    @Test fun `world four party exposes earned skill tiers against Titan Walker`() {
        val session = world4Session()
        val vm = createCombat(session, listOf("titan_walker_boss"))
        try {
            val party = requireNotNull(vm.combatState).combatants.values
                .filter { it.combatant.side == CombatSide.PLAYER }
            assertEquals(session.state.value.partyMembers.toSet(), party.map { it.combatant.id }.toSet())
            session.state.value.partyMembers.forEach { id ->
                val expected = session.state.value.unlockedSkills.filter { it.startsWith("${id}_") }.toSet() +
                    assets.loadCharacters().single { it.id == id }.skills
                assertEquals("Earned skills for $id", expected, vm.skillsForPlayer(id).map { it.id }.toSet())
            }
            assertEquals(listOf("titan_walker_boss"), vm.enemies.map { it.id })
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `world four Orion can execute his level nine command through production ATB`() {
        val vm = createCombat(world4Session(), listOf("titan_walker_boss"))
        try {
            val skill = vm.skillsForPlayer("orion").single { it.id == "orion_disruption_pulse" }
            repeat(80) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                vm.selectReadyPlayer("orion")
                if (vm.awaitingAction.value == "orion") {
                    assertTrue(vm.canUseSkill("orion", skill))
                    val before = vm.combatState
                    vm.useSkill(skill, listOf("titan_walker_boss"))
                    dispatcher.scheduler.runCurrent()
                    assertNotEquals(before, vm.combatState)
                    return
                }
            }
            fail("Orion never became selectable during 20 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `wounded authored support enemy heals through production ATB and targeting`() {
        val vm = createCombat(enemyIds = listOf("stalker_vine"))
        try {
            dispatcher.scheduler.runCurrent()
            val initial = requireNotNull(vm.combatState)
            val enemy = initial.combatants.getValue("stalker_vine")
            // Seed only the injury, not AI choices, targeting or turn execution.
            // Reflection keeps a test-only mutation hook out of the public API.
            val field = CombatViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
            @Suppress("UNCHECKED_CAST")
            val state = field.get(vm) as MutableStateFlow<CombatState?>
            state.value = initial.copy(combatants = initial.combatants +
                ("stalker_vine" to enemy.copy(hp = 10)))
            repeat(240) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                val healed = requireNotNull(vm.combatState).combatants.getValue("stalker_vine")
                if (healed.hp > 10) {
                    assertTrue("Healing must respect maximum HP", healed.hp <= healed.combatant.stats.maxHp)
                    assertTrue("Heal must enter cooldown", healed.activeCooldowns.getOrDefault("nature_heal", 0) > 0)
                    return
                }
            }
            fail("Wounded Stalker-Vine did not heal during 60 seconds of production ATB")
        } finally { vm.viewModelScope.cancel() }
    }

    private data class SpendingResult(val outcome: String, val medkits: Int, val credits: Int,
                                      val elapsedMs: Long, val remainingHp: Map<String, Int>, val skillsUsed: Int = 0,
                                      val supportUsed: Int = 0, val otherHeals: Int = 0, val medkitsLooted: Int = 0,
                                      val extraSupplies: Map<String, Int> = emptyMap())

    /** Test-only bridge for the campaign's saved checkpoint; no fixture XP or gear grants. */
    fun measureFinalCheckpoint(checkpoint: GameSessionState): List<String> {
        val gear = mapOf("nova" to "nova_flux_liner", "orion" to "heat_liner")
        gear.values.forEach { assertTrue((checkpoint.inventory[it] ?: 0) > 0) }
        assertTrue((checkpoint.inventory["grav_boots"] ?: 0) > 0)
        val equipped = checkpoint.copy(equippedArmors = checkpoint.equippedArmors + gear,
            equippedItems = checkpoint.equippedItems + ("gh0st:accessory" to "grav_boots"))
        fun run(seed: Int): List<SpendingResult> {
            val session = GameSessionStore().apply { restore(equipped) }
            val results = mutableListOf<SpendingResult>()
            for (enemy in listOf("ascended_vale", "ascended_god")) {
                val result = measureSpending(true, seed, session, enemy, skillAware = true,
                    defensive = true, expanded = true, ownedSupplies = true, avatarPolicy = true)
                assertNotEquals("Final encounter must resolve", "timeout", result.outcome)
                results += result
                if (result.outcome != "victory") break
            }
            return results
        }
        return (1..5).map { seed ->
            val result = run(seed)
            assertEquals(result, run(seed))
            "FINAL_CHECKPOINT_COMBAT seed=$seed results=$result"
        }
    }

    fun measureEarnedBossCheckpoint(checkpoint: GameSessionState, enemyId: String): List<String> {
        assertTrue((checkpoint.inventory["nova_flux_liner"] ?: 0) > 0)
        val equipped = checkpoint.copy(equippedArmors = checkpoint.equippedArmors + ("nova" to "nova_flux_liner"))
        fun run(seed: Int, earnedGear: Boolean, allocatedAp: Boolean = false, shopWeapons: Boolean = false): SpendingResult {
            val session = GameSessionStore().apply { restore(equipped) }
            if (earnedGear) {
                val items = reader.readList<Item>("items.json").associateBy { it.id }
                for ((id, slot) in listOf("heat_liner" to "armor", "grav_boots" to "accessory")) {
                    assertTrue("Checkpoint owns $id", (equipped.inventory[id] ?: 0) > 0)
                    assertEquals(slot, items.getValue(id).equipment?.slot)
                }
                session.restore(equipped.copy(
                    equippedArmors = equipped.equippedArmors + ("orion" to "heat_liner"),
                    equippedItems = equipped.equippedItems + ("gh0st:accessory" to "grav_boots")))
                val liner = items.getValue("heat_liner")
                assertTrue("Combat fixture must satisfy picker armor eligibility",
                    com.example.starborn.domain.inventory.GearRules.matchesSlot(liner.equipment, "armor", "orion", liner.type))
                assertEquals(checkpoint.inventory, session.state.value.inventory)
                assertEquals(checkpoint.playerCredits, session.state.value.playerCredits)
            }
            if (allocatedAp) {
                assertEquals(5, session.state.value.playerAp)
                val nodes = listOf("nova_quiet_steps", "nova_night_cloak", "zeke_training_session",
                    "zeke_motivational_speech", "zeke_budgeting")
                for (id in nodes) {
                    val tree = assets.loadSkillTrees().single { it.branches.values.flatten().any { node -> node.id == id } }
                    val node = tree.branches.values.flatten().single { it.id == id }
                    val state = session.state.value
                    assertTrue("Legal Avatar checkpoint purchase $id",
                        com.example.starborn.feature.exploration.skilltree.evaluateNodeStatus(
                            node, tree, state.unlockedSkills, state.completedMilestones, state.playerAp).canPurchase)
                    assertTrue(session.spendAp(node.costAp))
                    session.unlockSkill(id)
                }
                assertEquals(0, session.state.value.playerAp)
                assertEquals(checkpoint.playerCredits, session.state.value.playerCredits)
                assertEquals(checkpoint.inventory, session.state.value.inventory)
            }
            if (shopWeapons) {
                val shop = com.example.starborn.data.assets.ShopAssetDataSource(reader).loadShops().single { it.id == "weapon_shop" }
                val items = reader.readList<Item>("items.json").associateBy { it.id }
                val weapons = mapOf("nova" to "nova_laser_blaster", "zeke" to "zeke_shock_fists",
                    "orion" to "orion_prism_focus", "gh0st" to "gh0st_whisperblade")
                var cost = 0
                weapons.values.forEach { id ->
                    assertTrue(id in shop.sells.items)
                    assertTrue(session.state.value.completedMilestones.containsAll(shop.sells.gates[id]?.milestones.orEmpty()))
                    val item = items.getValue(id)
                    cost += kotlin.math.round((item.buyPrice ?: item.value) * (shop.pricing?.sellMarkup ?: 1.0)).toInt()
                }
                assertEquals(1800, cost)
                val state = session.state.value
                assertTrue(state.playerCredits >= cost)
                session.restore(state.copy(playerCredits = state.playerCredits - cost,
                    inventory = state.inventory + weapons.values.associateWith { (state.inventory[it] ?: 0) + 1 },
                    equippedWeapons = state.equippedWeapons + weapons,
                    unlockedWeapons = state.unlockedWeapons + weapons.values))
                assertEquals(340, session.state.value.playerCredits)
            }
            val result = measureSpending(true, seed, session, enemyId, skillAware = true,
                defensive = true, expanded = true, ownedSupplies = true, avatarPolicy = shopWeapons)
            assertNotEquals("$enemyId must resolve", "timeout", result.outcome)
            assertTrue(result.skillsUsed > 0)
            return result
        }
        val allocated = (1..5).map { seed ->
            val result = run(seed, earnedGear = true, allocatedAp = true)
            assertEquals(result, run(seed, earnedGear = true, allocatedAp = true))
            "AVATAR_ALLOCATED_AP seed=$seed result=$result"
        }
        val purchased = (1..5).map { seed ->
            val result = run(seed, earnedGear = true, allocatedAp = true, shopWeapons = true)
            assertEquals(result, run(seed, earnedGear = true, allocatedAp = true, shopWeapons = true))
            "AVATAR_CLOSEOUT_LOADOUT seed=$seed result=$result"
        }
        return purchased + allocated + (1..5).flatMap { seed -> listOf(false, true).map { gear ->
            val result = run(seed, gear)
            assertEquals(result, run(seed, gear))
            "EARNED_BOSS enemy=$enemyId level=${checkpoint.playerLevel} seed=$seed earnedGear=$gear result=$result"
        } }
    }

    fun measureEarnedTitanCheckpoint(checkpoint: GameSessionState): List<String> {
        assertEquals(8, checkpoint.playerLevel)
        assertTrue((checkpoint.inventory["nova_flux_liner"] ?: 0) > 0)
        val equipped = checkpoint.copy(equippedArmors = checkpoint.equippedArmors + ("nova" to "nova_flux_liner"))
        fun run(seed: Int): SpendingResult {
            val session = GameSessionStore().apply { restore(equipped) }
            val result = measureSpending(true, seed, session, "titan_walker_boss", skillAware = true, defensive = true, expanded = true)
            assertNotEquals("Earned checkpoint must resolve rather than stall", "timeout", result.outcome)
            assertTrue(result.skillsUsed > 0)
            return result
        }
        val bossOnly = (1..5).map { seed ->
            val result = run(seed)
            assertEquals("Earned checkpoint replay seed $seed", result, run(seed))
            "EARNED_LEVEL8_TITAN seed=$seed result=$result"
        }
        fun chain(seed: Int, camp: Boolean, ownedSupplies: Boolean = false, spendEarnedAp: Boolean = false): List<SpendingResult> {
            val session = GameSessionStore().apply { restore(equipped) }
            if (spendEarnedAp) {
                assertEquals(2, session.state.value.playerAp)
                for (nodeId in listOf("nova_quiet_steps", "zeke_training_session")) {
                    val tree = assets.loadSkillTrees().single { it.branches.values.flatten().any { node -> node.id == nodeId } }
                    val node = tree.branches.values.flatten().single { it.id == nodeId }
                    val state = session.state.value
                    val status = com.example.starborn.feature.exploration.skilltree.evaluateNodeStatus(
                        node, tree, state.unlockedSkills, state.completedMilestones, state.playerAp)
                    assertTrue("Earned checkpoint can purchase $nodeId", status.canPurchase)
                    assertTrue(session.spendAp(node.costAp))
                    session.unlockSkill(nodeId)
                }
                assertEquals(0, session.state.value.playerAp)
                assertEquals(equipped.inventory, session.state.value.inventory)
                assertEquals(equipped.playerCredits, session.state.value.playerCredits)
            }
            val results = mutableListOf<SpendingResult>()
            // Constructed stress sequence from the pre-boss snapshot, not a
            // claim that the scripted route navigated or fought this Golem.
            for (enemy in listOf("slag_golem", "titan_walker_boss")) {
                val before = session.state.value
                val result = measureSpending(true, seed, session, enemy, skillAware = true, defensive = true, expanded = true, ownedSupplies = ownedSupplies)
                assertNotEquals("Earned sequence must resolve", "timeout", result.outcome)
                results += result
                if (result.outcome != "victory") break
                assertEquals(before.playerCredits + result.credits, session.state.value.playerCredits)
                assertEquals((before.inventory["medkit"] ?: 0) - result.medkits + result.medkitsLooted,
                    session.state.value.inventory["medkit"] ?: 0)
                assertEquals(result.remainingHp.mapValues { it.value.coerceAtLeast(1) }, session.state.value.partyMemberHp)
                result.extraSupplies.forEach { (id, used) ->
                    assertEquals("Persist consumed $id", (before.inventory[id] ?: 0) - used, session.state.value.inventory[id] ?: 0)
                }
                if (camp && enemy == "slag_golem") {
                    val event = reader.readList<com.example.starborn.domain.model.GameEvent>("events.json")
                        .single { it.id == "w4_camp_forge_alcove" }
                    val catalog = reader.readList<Item>("items.json").associateBy { it.id }
                    val hooks = com.example.starborn.domain.event.EventHooks(
                        onRestParty = {
                            val state = session.state.value
                            session.updatePartyVitals(assets.loadCharacters().filter { it.id in state.partyMembers }
                                .associate { it.id to PartyHealth.maxHp(it, state, catalog::get, assets.loadSkillNodes()) })
                        },
                        onGiveItem = { id, qty -> session.setInventory(session.state.value.inventory +
                            (id to ((session.state.value.inventory[id] ?: 0) + qty))) })
                    val manager = com.example.starborn.domain.event.EventManager(listOf(event), session, hooks)
                    val payload = com.example.starborn.domain.event.EventPayload.Action(event.id)
                    manager.handleTrigger("player_action", payload)
                    val recovered = session.state.value
                    manager.handleTrigger("player_action", payload)
                    assertEquals("Camp must not repeat", recovered, session.state.value)
                }
            }
            return results
        }
        val apComparison = (1..5).flatMap { seed -> listOf(false, true).map { camp ->
            val results = chain(seed, camp, ownedSupplies = true, spendEarnedAp = true)
            assertEquals(results, chain(seed, camp, ownedSupplies = true, spendEarnedAp = true))
            "EARNED_AP_ALLOCATION seed=$seed camp=$camp results=$results"
        } }
        return bossOnly + apComparison + (1..5).flatMap { seed -> listOf(false, true).flatMap { camp -> listOf(false, true).map { owned ->
            val results = chain(seed, camp, owned)
            assertEquals(results, chain(seed, camp, owned))
            "EARNED_LEVEL8_CHAIN seed=$seed camp=$camp ownedSupplies=$owned results=$results"
        } } }
    }

    @Test fun `authored group support reaches living party members without helping enemies or reviving allies`() {
        for ((skillId, caster, statusId) in listOf(
            Triple("zeke_guardian_covenant", "zeke", "shield"),
            Triple("nova_link", "nova", "regen"))) {
            // Isolated targeting scenario: these optional unlocks are supplied,
            // not claimed as earned by the level-nine checkpoint.
            val session = world4Session().apply { unlockSkill(skillId) }
            val vm = createCombat(session, random = SeededCombatRandom(9))
            try {
                val field = CombatViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val flow = field.get(vm) as MutableStateFlow<CombatState?>
                val initial = requireNotNull(flow.value)
                flow.value = initial.copy(combatants = initial.combatants.mapValues { (id, actor) ->
                    when {
                        id == "gh0st" -> actor.copy(hp = 0)
                        actor.combatant.side == CombatSide.PLAYER -> actor.copy(hp = actor.combatant.stats.maxHp / 2)
                        else -> actor
                    }
                })
                val skill = vm.skillsForPlayer(caster).single { it.id == skillId }
                var used = false
                for (tick in 0 until 240) {
                    dispatcher.scheduler.advanceTimeBy(250)
                    dispatcher.scheduler.runCurrent()
                    if (vm.lungeActorId.value != null) vm.onLungeFinished(vm.lungeToken.value)
                    if (vm.missLungeActorId.value != null) vm.onMissLungeFinished(vm.missLungeToken.value)
                    vm.selectReadyPlayer(caster)
                    if (vm.awaitingAction.value != caster) continue
                    val before = requireNotNull(vm.combatState)
                    assertTrue(vm.canUseSkill(caster, skill))
                    vm.useSkill(skill) // No explicit targets: exercise production resolution.
                    val after = requireNotNull(vm.combatState)
                    for (id in listOf("nova", "zeke", "orion")) {
                        assertTrue("$skillId must apply $statusId to $id", after.combatants.getValue(id).statusEffects.any { it.id == statusId })
                        if (skillId == "nova_link") assertTrue(after.combatants.getValue(id).hp > before.combatants.getValue(id).hp)
                    }
                    assertEquals(0, after.combatants.getValue("gh0st").hp)
                    assertTrue(after.combatants.getValue("gh0st").statusEffects.none { it.id == statusId })
                    val enemy = after.combatants.getValue("faulted_loader")
                    assertEquals(before.combatants.getValue("faulted_loader").hp, enemy.hp)
                    assertTrue(enemy.statusEffects.none { it.id == statusId })
                    assertTrue(after.combatants.getValue(caster).activeCooldowns.getOrDefault(skillId, 0) > 0)
                    used = true
                    break
                }
                assertTrue("$caster must receive an ATB action", used)
            } finally { vm.viewModelScope.cancel(); dispatcher.scheduler.runCurrent() }
        }
    }

    private fun earnWorkshopLoftReward(): GameSessionState {
        // Start inside the workshop after the loader gate, not before that battle.
        val rooms = reader.readList<com.example.starborn.domain.model.Room>("rooms.json").associateBy { it.id }
        val floor = rooms.getValue("workshop_floor")
        assertEquals("workshop_loft", floor.connections["west"])
        assertNull("This optional detour must be ungated from the workshop", floor.blockedDirections?.get("west"))
        val loft = rooms.getValue("workshop_loft")
        val action = loft.actions.single { it["action_event"] == "w1_loot_workshop_loft" }
        assertTrue("Loot action must be discoverable in room prose", loft.description.contains(action["name"].toString()))
        val event = reader.readList<com.example.starborn.domain.model.GameEvent>("events.json")
            .single { it.id == action["action_event"] }
        val session = GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_1", roomId = loft.id, playerId = "nova", partyMembers = listOf("nova")))
        }
        fun manager() = com.example.starborn.domain.event.EventManager(listOf(event), session,
            com.example.starborn.domain.event.EventHooks(
                onMilestoneSet = { id -> if (!id.isNullOrBlank()) session.setMilestone(id) },
                onGiveItem = { id, qty -> session.setInventory(session.state.value.inventory +
                    (id to ((session.state.value.inventory[id] ?: 0) + qty))) }))
        val payload = com.example.starborn.domain.event.EventPayload.Action(event.id)
        manager().handleTrigger("player_action", payload)
        assertEquals(1, session.state.value.inventory["precision_sight"])
        assertTrue(session.state.value.completedMilestones.contains(action["requires_milestone_not_set"]))
        val earned = session.state.value
        session.restore(earned)
        manager().handleTrigger("player_action", payload)
        assertEquals("Reloading the event manager must not duplicate the reward", earned, session.state.value)
        return earned
    }

    @Test fun `workshop loft reward survives restore and applies only to its equipped owner`() {
        val earned = earnWorkshopLoftReward()
        // Carry only this earned inventory into a seeded later checkpoint.
        val later = world4Session()
        later.setInventory(earned.inventory)
        val baselineVm = createCombat(later)
        val baseline = try {
            requireNotNull(baselineVm.combatState).combatants.mapValues { it.value.combatant.stats }
        } finally { baselineVm.viewModelScope.cancel() }
        val item = reader.readList<Item>("items.json").single { it.id == "precision_sight" }
        assertEquals("accessory", item.equipment?.slot)
        later.restore(later.state.value.copy(equippedItems = mapOf("nova:accessory" to item.id)))
        val vm = createCombat(later)
        try {
            val actors = requireNotNull(vm.combatState).combatants
            val nova = actors.getValue("nova").combatant.stats
            assertEquals(baseline.getValue("nova").accuracyBonus + 8.0, nova.accuracyBonus, 0.0001)
            assertEquals(baseline.getValue("nova").critBonus + 4.0, nova.critBonus, 0.0001)
            assertEquals("Scoped gear must not improve another party member", baseline.getValue("zeke"), actors.getValue("zeke").combatant.stats)
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `superseded lunges own only one pause and ignore stale completion`() {
        val vm = createCombat()
        try {
            val method = CombatViewModel::class.java.getDeclaredMethod("triggerAttackLunge", String::class.java,
                com.example.starborn.feature.combat.viewmodel.AttackLungeStyle::class.java, Long::class.javaPrimitiveType)
                .apply { isAccessible = true }
            val pauses = CombatViewModel::class.java.getDeclaredField("atbAnimationPauses").apply { isAccessible = true }
            val style = com.example.starborn.feature.combat.viewmodel.AttackLungeStyle.MELEE
            method.invoke(vm, "nova", style, 500L)
            val stale = vm.lungeToken.value
            method.invoke(vm, "faulted_loader", style, 0L)
            assertEquals(1, pauses.getInt(vm))
            vm.onLungeFinished(stale)
            assertEquals(1, pauses.getInt(vm))
            val current = vm.lungeToken.value
            vm.onLungeFinished(current)
            vm.onLungeFinished(current)
            assertEquals(0, pauses.getInt(vm))
            dispatcher.scheduler.advanceTimeBy(600)
            dispatcher.scheduler.runCurrent()
            assertNull(vm.lungeActorId.value)
            assertEquals(0, pauses.getInt(vm))
            val miss = CombatViewModel::class.java.getDeclaredMethod("triggerMissLunge", String::class.java)
                .apply { isAccessible = true }
            method.invoke(vm, "nova", style, 0L)
            miss.invoke(vm, "nova")
            val staleMiss = vm.missLungeToken.value
            miss.invoke(vm, "faulted_loader")
            assertEquals(2, pauses.getInt(vm))
            vm.onMissLungeFinished(staleMiss)
            assertEquals(2, pauses.getInt(vm))
            vm.onMissLungeFinished(vm.missLungeToken.value)
            vm.onMissLungeFinished(vm.missLungeToken.value)
            assertEquals("Duplicate miss completion must not release the attack pause", 1, pauses.getInt(vm))
            vm.onLungeFinished(vm.lungeToken.value)
            assertEquals(0, pauses.getInt(vm))
        } finally { vm.viewModelScope.cancel() }
    }

    @Test fun `budget equipped skill policy carries resources through a constructed encounter sequence`() {
        fun run(seed: Int, defensive: Boolean = false, camp: Boolean = false, expanded: Boolean = false, passives: Boolean = false, accessory: Boolean = false): List<SpendingResult> {
            val session = world4Session()
            val weapons = if (defensive) mapOf("zeke" to "zeke_shock_fists")
                else mapOf("nova" to "nova_laser_blaster", "zeke" to "zeke_shock_fists")
            val armor = if (defensive) mapOf("nova" to "nova_flux_liner") else emptyMap()
            // Retain the opening quest's free armor in the defensive scenario.
            // Assert its authored source rather than charging its shop replacement price.
            val armorReward = reader.readList<com.example.starborn.domain.model.GameEvent>("events.json")
                .single { it.id == "w1_mq01_patch_flux_liner" }.actions
                .single { it.type == "give_item" && it.itemId == "nova_flux_liner" }
            assertEquals(1, armorReward.quantity)
            // 1,070 is a scripted battle-credit baseline, assuming no prior spending.
            // Purchased weapons cost 456 (defensive) or 432 + 456 (offensive).
            // Three medkits cost 180. Unspent money is carried, not spent mid-run.
            // Reward retention is assumed; this is still not a campaign-earned save.
            val purchaseCost = (if (defensive) 456 else 432 + 456) + 3 * 60
            val remainingCredits = 1070 - purchaseCost
            assertEquals(if (defensive) 434 else 2, remainingCredits)
            session.restore(session.state.value.copy(playerCredits = remainingCredits,
                inventory = (weapons.values + armor.values).associateWith { 1 } + ("medkit" to 3),
                equippedWeapons = weapons, equippedArmors = armor, unlockedWeapons = weapons.values.toSet()))
            if (passives) {
                val baselineVm = createCombat(session)
                val baselineStats = try {
                    requireNotNull(baselineVm.combatState).combatants.mapValues { it.value.combatant.stats }
                } finally { baselineVm.viewModelScope.cancel() }
                // Explicit main-route boss rewards before World 4, not per-member AP.
                val bosses = listOf("the_iron_warden", "the_beast", "administrator_boss")
                val earnedAp = bosses.sumOf { id -> assets.loadEnemies().single { it.id == id }.apReward ?: 0 }
                assertEquals(2, earnedAp)
                session.addAp(earnedAp)
                for (nodeId in listOf("nova_quiet_steps", "zeke_training_session")) {
                    val tree = assets.loadSkillTrees().single { tree -> tree.branches.values.flatten().any { it.id == nodeId } }
                    val node = tree.branches.values.flatten().single { it.id == nodeId }
                    val snapshot = session.state.value
                    val status = com.example.starborn.feature.exploration.skilltree.evaluateNodeStatus(
                        node, tree, snapshot.unlockedSkills, emptySet(), snapshot.playerAp)
                    assertTrue("Legal earned-AP purchase: $nodeId", status.canPurchase)
                    assertTrue(session.spendAp(node.costAp))
                    session.unlockSkill(nodeId)
                }
                assertEquals("AP is shared across the party", 0, session.state.value.playerAp)
                val upgradedVm = createCombat(session)
                try {
                    val actors = requireNotNull(upgradedVm.combatState).combatants
                    assertEquals(baselineStats.getValue("nova").speed + 5, actors.getValue("nova").combatant.stats.speed)
                    assertEquals(baselineStats.getValue("zeke").strength + 5, actors.getValue("zeke").combatant.stats.strength)
                } finally { upgradedVm.viewModelScope.cancel() }
            }
            if (accessory) {
                val earned = earnWorkshopLoftReward()
                val before = session.state.value
                val inventory = before.inventory.toMutableMap()
                earned.inventory.forEach { (id, qty) -> inventory[id] = (inventory[id] ?: 0) + qty }
                session.restore(before.copy(inventory = inventory,
                    completedMilestones = before.completedMilestones + earned.completedMilestones,
                    equippedItems = before.equippedItems + ("nova:accessory" to "precision_sight")))
                assertEquals("Optional loot does not cost credits", before.playerCredits, session.state.value.playerCredits)
            }
            val results = mutableListOf<SpendingResult>()
            // Loader is a calibration fight, not an authored World 4 encounter.
            for (enemy in listOf("faulted_loader", "slag_golem", "titan_walker_boss")) {
                val before = session.state.value
                val result = measureSpending(true, seed, session, enemy, true, defensive, expanded)
                assertNotEquals("$enemy defensive=$defensive seed=$seed result=$result", "timeout", result.outcome)
                assertTrue("Policy must execute earned skills", result.skillsUsed > 0)
                results += result
                if (result.outcome != "victory") break
                assertEquals(result.remainingHp.mapValues { it.value.coerceAtLeast(1) }, session.state.value.partyMemberHp)
                assertEquals(before.playerCredits + result.credits, session.state.value.playerCredits)
                if (accessory) {
                    assertEquals(1, session.state.value.inventory["precision_sight"])
                    assertEquals("precision_sight", session.state.value.equippedItems["nova:accessory"])
                }
                assertEquals("Inventory after $enemy seed=$seed passives=$passives result=$result", (before.inventory["medkit"] ?: 0) - result.medkits + result.medkitsLooted,
                    session.state.value.inventory["medkit"] ?: 0)
                if (camp && enemy == "slag_golem") {
                    val event = reader.readList<com.example.starborn.domain.model.GameEvent>("events.json")
                        .single { it.id == "w4_camp_forge_alcove" }
                    val catalog = reader.readList<Item>("items.json").associateBy { it.id }
                    val hooks = com.example.starborn.domain.event.EventHooks(
                        onRestParty = {
                            val snapshot = session.state.value
                            session.updatePartyVitals(assets.loadCharacters().filter { it.id in snapshot.partyMembers }
                                .associate { it.id to PartyHealth.maxHp(it, snapshot, catalog::get, assets.loadSkillNodes()) })
                        },
                        onGiveItem = { id, qty -> session.setInventory(session.state.value.inventory +
                            (id to ((session.state.value.inventory[id] ?: 0) + qty))) })
                    val manager = com.example.starborn.domain.event.EventManager(listOf(event), session, hooks)
                    val payload = com.example.starborn.domain.event.EventPayload.Action(event.id)
                    val suppliesBefore = session.state.value.inventory["painkillers"] ?: 0
                    manager.handleTrigger("player_action", payload)
                    assertEquals(suppliesBefore + 1, session.state.value.inventory["painkillers"])
                    val recovered = session.state.value
                    manager.handleTrigger("player_action", payload)
                    assertEquals("Camp reward must not repeat", recovered, session.state.value)
                }
            }
            return results
        }
        for (seed in 1..5) {
            val passiveResults = run(seed, defensive = true, camp = true, expanded = true, passives = true)
            assertTrue(passiveResults.size >= 2)
            assertEquals(passiveResults, run(seed, defensive = true, camp = true, expanded = true, passives = true))
            println("EARNED_AP_CHAIN seed=$seed results=$passiveResults")
            val accessoryResults = run(seed, defensive = true, camp = true, expanded = true, passives = true, accessory = true)
            assertTrue(accessoryResults.size >= 2)
            assertEquals(accessoryResults, run(seed, defensive = true, camp = true, expanded = true, passives = true, accessory = true))
            println("EARNED_ACCESSORY_CHAIN seed=$seed results=$accessoryResults")
          for (defensive in listOf(false, true)) {
           for (camp in if (defensive) listOf(false, true) else listOf(false)) {
            for (expanded in if (camp) listOf(false, true) else listOf(false)) {
            val results = run(seed, defensive, camp, expanded)
            assertTrue("Calibration victory must exercise the next encounter", results.size >= 2)
            assertEquals(results, run(seed, defensive, camp, expanded))
            if (defensive) assertTrue("Support policy must execute support skills", results.sumOf { it.supportUsed } > 0)
            println("EQUIPPED_CHAIN defensive=$defensive camp=$camp expanded=$expanded seed=$seed results=$results")
            }
           }
          }
        }
    }

    @Test fun `seeded basic attack spending scenarios replay exactly`() {
        for (laterGame in listOf(false, true)) {
            for (seed in 1..5) {
                val first = measureSpending(laterGame, seed)
                assertEquals("Same seed and policy must reproduce spending", first, measureSpending(laterGame, seed))
                assertTrue(first.medkits in 0..3)
                assertNotEquals("Scenario must reach an outcome, not merely replay a stall", "timeout", first.outcome)
                println("SPENDING fixture=${if (laterGame) "world4_ungeared" else "opening"} seed=$seed result=$first replacementCost=${first.medkits * 60}")
            }
        }
    }

    private fun measureSpending(laterGame: Boolean, seed: Int, suppliedSession: GameSessionStore? = null,
                                enemy: String? = null, skillAware: Boolean = false,
                                defensive: Boolean = false, expanded: Boolean = false, ownedSupplies: Boolean = false,
                                avatarPolicy: Boolean = false): SpendingResult {
        val session = suppliedSession ?: if (laterGame) world4Session() else GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_1", roomId = "workshop_yard",
                playerId = "nova", partyMembers = listOf("nova"), unlockedSkills = setOf("nova_arc_tether")))
        }
        // Supplied stock is not claimed as an earned campaign inventory.
        if (suppliedSession == null) session.setInventory(mapOf("medkit" to 3))
        val vm = createCombat(session, listOf(enemy ?: if (laterGame) "titan_walker_boss" else "faulted_loader"), SeededCombatRandom(seed))
        val start = dispatcher.scheduler.currentTime
        var consumed = 0
        var skillsUsed = 0
        var supportUsed = 0
        var otherHeals = 0
        val extraSupplies = mutableMapOf<String, Int>()
        try {
            if (defensive) assertTrue("Nova's armor must affect runtime defense",
                requireNotNull(vm.combatState).combatants.getValue("nova").combatant.stats.flatDamageReduction > 0)
            session.state.value.equippedWeapons.forEach { (id, weapon) ->
                assertEquals(weapon, requireNotNull(vm.combatState).combatants.getValue(id).combatant.weapon?.itemId)
            }
            session.state.value.partyMemberHp.forEach { (id, hp) ->
                val actor = requireNotNull(vm.combatState).combatants.getValue(id)
                assertEquals(hp.coerceIn(1, actor.combatant.stats.maxHp), actor.hp)
            }
            repeat(960) {
                dispatcher.scheduler.advanceTimeBy(250)
                dispatcher.scheduler.runCurrent()
                // Supply the presentation callback after one 250ms tick. The
                // headless harness has no Compose lunge animation to finish it.
                if (vm.lungeActorId.value != null) vm.onLungeFinished(vm.lungeToken.value)
                if (vm.missLungeActorId.value != null) vm.onMissLungeFinished(vm.missLungeToken.value)
                val state = requireNotNull(vm.combatState)
                if (state.outcome != null) {
                    if (enemy == "compliance_avatar" && seed == 1) {
                        println("AVATAR_ACTION_TRACE " + state.log.joinToString("\n"))
                    }
                    val victory = state.outcome as? CombatOutcome.Victory
                    return SpendingResult(if (victory != null) "victory" else "defeat", consumed,
                        victory?.rewards?.credits ?: 0, dispatcher.scheduler.currentTime - start,
                        state.combatants.filterValues { it.combatant.side == CombatSide.PLAYER }.mapValues { it.value.hp }, skillsUsed, supportUsed, otherHeals,
                        victory?.rewards?.drops.orEmpty().filter { it.itemId == "medkit" }.sumOf { it.quantity }, extraSupplies.toMap())
                }
                session.state.value.partyMembers.forEach { vm.selectReadyPlayer(it) }
                if (vm.awaitingAction.value != null) {
                    val wounded = state.combatants.values.filter {
                        it.combatant.side == CombatSide.PLAYER && it.isAlive && it.hp * 100L < it.combatant.stats.maxHp * 40L
                    }.minByOrNull { it.hp.toDouble() / it.combatant.stats.maxHp }
                    val medkit = vm.inventory.value.firstOrNull { it.item.id == "medkit" && it.quantity > 0 }
                        ?: if (ownedSupplies) vm.inventory.value.firstOrNull { it.item.id == "medkit_i" && it.quantity > 0 } else null
                    val activeId = requireNotNull(vm.awaitingAction.value)
                    val active = state.combatants.getValue(activeId)
                    val partyHeal = if (ownedSupplies && state.combatants.values.count {
                        it.combatant.side == CombatSide.PLAYER && it.isAlive && it.combatant.stats.maxHp - it.hp >= 35
                    } >= 2) vm.inventory.value.firstOrNull { it.item.id == "ration_pack" && it.quantity > 0 } else null
                    val selfHeal = if (expanded && active.hp * 100L < active.combatant.stats.maxHp * 70L)
                        vm.inventory.value.firstOrNull { it.quantity > 0 && it.item.id == "painkillers" } else null
                    val supportId = when (activeId) {
                        "nova" -> if (avatarPolicy && state.combatants.values.count {
                            it.combatant.side == CombatSide.PLAYER && it.isAlive && it.hp < it.combatant.stats.maxHp * 0.7
                        } >= 2) "nova_link" else null
                        "zeke" -> "zeke_bulwark_stance"
                        "orion" -> "orion_nano_repair"
                        else -> null
                    }
                    val statusId = if (activeId == "zeke") "guard" else "regen"
                    val support = if (defensive && active.statusEffects.none { it.id == statusId } &&
                        (activeId == "zeke" || active.hp * 100L < active.combatant.stats.maxHp * 80L)) {
                        vm.skillsForPlayer(activeId).firstOrNull { it.id == supportId && vm.canUseSkill(activeId, it) }
                    } else null
                    if (partyHeal != null) {
                        val before = partyHeal.quantity
                        vm.useItem(partyHeal, activeId)
                        val used = before - (vm.inventory.value.firstOrNull { it.item.id == "ration_pack" }?.quantity ?: 0)
                        extraSupplies["ration_pack"] = (extraSupplies["ration_pack"] ?: 0) + used
                    } else if (selfHeal != null && (wounded == null || medkit == null)) {
                        val before = selfHeal.quantity
                        vm.useItem(selfHeal, activeId)
                        otherHeals += before - (vm.inventory.value.firstOrNull { it.item.id == "painkillers" }?.quantity ?: 0)
                    } else if (support != null && (wounded == null || medkit == null)) {
                        if (avatarPolicy && support.id == "nova_link") vm.useSkill(support)
                        else vm.useSkill(support, listOf(activeId))
                        skillsUsed++
                        supportUsed++
                    } else if (wounded != null && medkit != null) {
                        val before = medkit.quantity
                        vm.useItem(medkit, wounded.combatant.id)
                        val after = vm.inventory.value.firstOrNull { it.item.id == medkit.item.id }?.quantity ?: 0
                        if (medkit.item.id == "medkit") consumed += before - after
                        else extraSupplies[medkit.item.id] = (extraSupplies[medkit.item.id] ?: 0) + before - after
                    } else {
                        val target = state.combatants.values.firstOrNull { it.combatant.side == CombatSide.ENEMY && it.isAlive }
                        if (target != null) {
                            val actor = requireNotNull(vm.awaitingAction.value)
                            // Source/shock beat physical here; avoid burn against Foundry
                            // resistance. Only currently unlocked and usable skills qualify.
                            val priorities = if (avatarPolicy) listOf("nova_cryo_vent", "nova_hydraulic_kick",
                                "zeke_overload_fists", "gh0st_venom_edge", "nova_arc_tether", "zeke_shatter_blow",
                                "gh0st_headshot", "orion_prism_lance") else if (expanded) listOf("zeke_overload_fists", "gh0st_system_crash",
                                "nova_arc_tether", "gh0st_venom_edge", "orion_prism_lance", "zeke_shatter_blow", "gh0st_headshot")
                                else listOf("nova_arc_tether", "zeke_shatter_blow", "orion_prism_lance", "gh0st_headshot")
                            val skill = if (skillAware) vm.skillsForPlayer(actor).sortedBy { priorities.indexOf(it.id).let { rank -> if (rank < 0) Int.MAX_VALUE else rank } }.firstOrNull {
                                it.id in priorities &&
                                    vm.canUseSkill(actor, it)
                            } else null
                            if (skill != null) {
                                vm.useSkill(skill, listOf(target.combatant.id))
                                skillsUsed++
                            } else vm.playerAttack(target.combatant.id)
                        }
                    }
                }
            }
            println("TIMEOUT enemy=$enemy pauses=" + CombatViewModel::class.java.getDeclaredField("atbAnimationPauses").apply { isAccessible = true }.get(vm) +
                " lunge=${vm.lungeActorId.value} miss=${vm.missLungeActorId.value} awaiting=${vm.awaitingAction.value} round=${vm.combatState?.round}")
            return SpendingResult("timeout", consumed, 0, dispatcher.scheduler.currentTime - start,
                requireNotNull(vm.combatState).combatants.filterValues { it.combatant.side == CombatSide.PLAYER }.mapValues { it.value.hp }, skillsUsed, supportUsed)
        } finally { vm.viewModelScope.cancel(); dispatcher.scheduler.runCurrent() }
    }

    private fun createCombat(initialSession: GameSessionStore? = null,
                             enemyIds: List<String> = listOf("faulted_loader"),
                             random: CombatRandom = DefaultCombatRandom): CombatViewModel {
        val items = reader.readList<Item>("items.json").associateBy { it.id }
        check(items.isNotEmpty())
        val catalog = object : ItemCatalog {
            override fun load() = Unit
            override fun findItem(idOrAlias: String): Item? = items[idOrAlias]
        }
        val session = initialSession ?: GameSessionStore().apply {
            restore(GameSessionState(worldId = "world_1", roomId = "workshop_yard",
                playerId = "nova", partyMembers = listOf("nova"),
                unlockedSkills = setOf("nova_arc_tether")))
        }
        val registry = StatusRegistry(assets.loadStatuses())
        val themes = mock<ThemeRepository> {
            on { getTheme(any()) } doReturn null
            on { getStyle(any()) } doReturn null
        }
        return CombatViewModel(
            worldAssets = assets, combatEngine = CombatEngine(statusRegistry = registry),
            statusRegistry = registry, sessionStore = session,
            inventoryService = InventoryService(catalog).apply { loadItems(); restore(session.state.value.inventory) }, itemCatalog = catalog,
            levelingManager = LevelingManager(requireNotNull(assets.loadLevelingData())),
            progressionData = requireNotNull(assets.loadProgressionData()),
            audioRouter = AudioRouter(AudioBindings()), themeRepository = themes,
            environmentThemeManager = EnvironmentThemeManager(themes),
            encounterCoordinator = EncounterCoordinator(), enemyIds = enemyIds,
            tutorialsEnabled = false, elapsedRealtime = { dispatcher.scheduler.currentTime }, random = random
        )
    }
}
