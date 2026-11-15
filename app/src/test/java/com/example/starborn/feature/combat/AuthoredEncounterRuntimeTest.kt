package com.example.starborn.feature.combat

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
import com.example.starborn.feature.combat.viewmodel.CombatController
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*
import java.io.File

/** Synthetic prototype fixtures prove mechanics. The Warden case separately uses an earned arrival. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthoredEncounterRuntimeTest {
    private val dispatcher=StandardTestDispatcher()
    private val reader=AssetJsonReader(DesktopAssetProvider(),MoshiProvider.instance)
    private val assets=WorldAssetDataSource(reader)
    @Before fun setup() { Dispatchers.setMain(dispatcher);CombatPlaytestBridge.reset() }
    @After fun cleanup() { CombatPlaytestBridge.reset();Dispatchers.resetMain() }
    private fun prototypeFixture(): GameSessionState {
        val base=FoundryLoadouts.session("purchased",assets,55000)
        return base.copy(equippedWeapons=base.equippedWeapons+("nova" to "mining_pistol"),
            inventory=base.inventory+("mining_pistol" to 1))
    }
    private fun create(ids:List<String>,state:GameSessionState=prototypeFixture(),seed:Int=41,
        encounter: EncounterDescriptor? = null):Pair<CombatController,GameSessionStore> {
        val store=GameSessionStore().apply { restore(state) }
        val items=reader.readList<Item>("items.json").associateBy { it.id }
        val catalog=object:ItemCatalog { override fun load()=Unit;override fun findItem(idOrAlias:String)=items[idOrAlias] }
        val registry=StatusRegistry(assets.loadStatuses())
        val themes=mock<ThemeRepository> { on { getTheme(any()) } doReturn null;on { getStyle(any()) } doReturn null }
        return CombatController(assets,CombatEngine(statusRegistry=registry),registry,store,
            InventoryService(catalog).apply { loadItems();restore(state.inventory) },catalog,
            LevelingManager(requireNotNull(assets.loadLevelingData())),requireNotNull(assets.loadProgressionData()),
            AudioRouter(AudioBindings()),themes,EnvironmentThemeManager(themes),
            EncounterCoordinator().apply { encounter?.let(::setPendingEncounter) },ids,
            tutorialsEnabled=false,elapsedRealtime={dispatcher.scheduler.currentTime},random=SeededCombatRandom(seed)) to store
    }
    private fun step(vm:CombatController) {
        dispatcher.scheduler.advanceTimeBy(250);dispatcher.scheduler.runCurrent()
        if(vm.lungeActorId.value!=null) vm.onLungeFinished(vm.lungeToken.value)
        if(vm.missLungeActorId.value!=null) vm.onMissLungeFinished(vm.missLungeToken.value)
    }
    private fun until(vm:CombatController,limit:Int=1800,condition:()->Boolean) {
        repeat(limit) { if(condition()) return;step(vm) }
        fail("Timed out; outcome=${vm.combatState?.outcome}; runtime=${vm.combatState?.authored}; meters=${vm.atbMeters.value}")
    }
    private fun novaAttack(vm:CombatController,target:String) {
        until(vm) { vm.selectReadyPlayer("nova");vm.awaitingAction.value=="nova" || vm.combatState?.outcome!=null }
        assertNull(vm.combatState!!.outcome)
        vm.playerAttack(target);step(vm)
    }

    @Test fun silenceKeepsAStageOneKnockoutAndLosesItsVoiceCommandWhenTheAnchorFalls() {
        val arrival = prototypeFixture().let { it.copy(roomId = "source_center",
            partyMemberHp = it.partyMemberHp + ("orion" to 0)) }
        val (vm, _) = create(listOf("ascended_god"), arrival)
        try {
            val initial = requireNotNull(vm.combatState)
            assertEquals(0, initial.combatants.getValue("orion").hp)
            assertEquals(listOf("ascended_god"), vm.encounterEnemyIds)
            assertEquals(0f, vm.atbMeters.value["silence_voice_anchor"])
            until(vm) { vm.combatState!!.authored.preparations["ascended_god"]?.sourceId == "silence_voice_anchor" }
            CombatPlaytestBridge.isOpMode = true
            novaAttack(vm, "silence_voice_anchor")
            assertNull(vm.combatState!!.outcome)
            assertFalse(vm.combatState!!.authored.preparations.containsKey("ascended_god"))
            assertTrue(vm.combatState!!.combatants.getValue("silence_memory_anchor").isAlive)
            novaAttack(vm, "ascended_god")
            val victory = vm.combatState!!.outcome as CombatOutcome.Victory
            assertEquals(1500, victory.rewards.xp)
            assertEquals(8, victory.rewards.ap)
            assertFalse(vm.combatState!!.combatants.getValue("silence_memory_anchor").isAlive)
        } finally { vm.close(); CombatPlaytestBridge.reset() }
    }

    @Test fun fiveTargetsShareOneClockAndDestroyingAChargingWeaponCancelsIt() {
        val ids=listOf("prototype_titan","prototype_missile","prototype_cutter","prototype_anchor","prototype_projector")
        val (vm,store)=create(ids)
        try {
            val initialCells=store.state.value.inventory["power_cell"] ?: 0
            until(vm) { vm.combatState!!.authored.preparations["prototype_titan"]?.sourceId=="prototype_missile" }
            val pending=vm.combatState!!.authored.preparations.getValue("prototype_titan")
            val actions=vm.combatState!!.log.filterIsInstance<CombatLogEntry.ActionQueued>()
            assertFalse(actions.any { it.action is CombatAction.SkillUse && (it.action as CombatAction.SkillUse).skillId==pending.skillId })
            assertTrue(ids.drop(1).all { vm.atbMeters.value[it]==0f })
            vm.setBackgroundPaused(true);val before=vm.combatState;repeat(40) { step(vm) };assertEquals(before,vm.combatState)
            vm.setBackgroundPaused(false)
            CombatPlaytestBridge.isOpMode=true
            novaAttack(vm,"prototype_missile")
            assertFalse(vm.combatState!!.combatants.getValue("prototype_missile").isAlive)
            assertTrue(vm.combatState!!.authored.preparations.isEmpty())
            assertNull(vm.combatState!!.outcome)
            // Real command pipeline, boosted only to end the synthetic mechanics fixture promptly.
            novaAttack(vm,"prototype_titan")
            val reward=(vm.combatState!!.outcome as CombatOutcome.Victory).rewards
            assertEquals(100,reward.xp);assertEquals(25,reward.credits);assertTrue(reward.drops.isEmpty())
            assertEquals(initialCells,store.state.value.inventory["power_cell"] ?: 0)
            assertEquals(ids,vm.encounterEnemyIds)
        } finally { vm.close() }
    }
    @Test fun deploymentAddsRealTargetsRespectsLiveCapAndCoreVictoryPaysOnlyTheParent() {
        val (vm,_)=create(listOf("prototype_administrator"))
        try {
            until(vm) { vm.combatState!!.authored.reservesUsed["prototype_administrator"]==2 }
            assertEquals(3,vm.enemyCombatantIds.size)
            assertEquals(2,vm.combatState!!.authored.reserveOwners.size)
            repeat(300) { step(vm) }
            assertEquals(2,vm.combatState!!.authored.reservesUsed["prototype_administrator"])
            assertNull(vm.combatState!!.outcome)
            CombatPlaytestBridge.isOpMode=true
            val drone=vm.combatState!!.authored.reserveOwners.keys.first()
            novaAttack(vm,drone)
            until(vm) { vm.combatState!!.authored.reservesUsed["prototype_administrator"]==3 }
            assertEquals(4,vm.enemyCombatantIds.size)
            assertEquals(4,vm.enemyCombatantIds.distinct().size)
            novaAttack(vm,"prototype_administrator")
            val reward=(vm.combatState!!.outcome as CombatOutcome.Victory).rewards
            assertEquals(100,reward.xp);assertEquals(25,reward.credits);assertTrue(reward.drops.isEmpty())
            assertEquals(listOf("prototype_administrator"),vm.encounterEnemyIds)
        } finally { vm.close() }
    }

    @Test fun titanPartsCanBeDisabledIndependentlyAndDisarmedChassisKeepsItsWarnedStomp() {
        val boss = "titan_walker_boss"
        val missile = "titan_missile_battery"
        val parts = listOf(missile, "titan_cutting_arm", "titan_anchor_cannon", "titan_shield_projector")
        val (vm, _) = create(listOf(boss))
        try {
            assertEquals(listOf(boss) + parts, vm.enemyCombatantIds)
            until(vm) { vm.combatState!!.authored.preparations[boss]?.sourceId == missile }
            assertTrue(parts.all { vm.atbMeters.value[it] == 0f })
            CombatPlaytestBridge.isOpMode = true
            novaAttack(vm, missile)
            assertFalse(vm.combatState!!.combatants.getValue(missile).isAlive)
            assertTrue(vm.combatState!!.authored.preparations.isEmpty())
            parts.drop(1).forEach { part ->
                novaAttack(vm, part)
                assertNull(vm.combatState!!.outcome)
            }
            until(vm) { vm.combatState!!.authored.preparations[boss]?.skillId == "foundry_titan_stomp" }
            assertEquals(boss, vm.combatState!!.authored.preparations.getValue(boss).sourceId)
            novaAttack(vm, boss)
            val reward = (vm.combatState!!.outcome as CombatOutcome.Victory).rewards
            assertEquals(CombatReward(600, 3, 400, listOf(LootDrop("power_cell", 3))), reward)
            assertEquals(listOf(boss), vm.encounterEnemyIds)
        } finally { vm.close() }
    }

    @Test fun defeatingTitanShutsDownAttachedSystemsInAMixedEncounter() {
        val boss = "titan_walker_boss"
        val (vm, _) = create(listOf("the_iron_warden", boss))
        try {
            CombatPlaytestBridge.isOpMode = true
            novaAttack(vm, boss)
            val battle = vm.combatState!!
            assertTrue(battle.combatants.getValue("the_iron_warden").isAlive)
            assertTrue(vm.enemyCombatantIds.filter { it != "the_iron_warden" }.none {
                battle.combatants.getValue(it).isAlive
            })
            assertNull(battle.outcome)
        } finally { vm.close() }
    }
    @Test fun spireAdministratorDeploysThreeReservesAcrossEscortLossesAndShutsDownSurvivors() {
        val boss = "administrator_boss"
        val (vm, _) = create(listOf(boss, "aero_drone"))
        fun deployed() = vm.combatState!!.authored.reservesUsed[boss] ?: 0
        try {
            until(vm) { deployed() == 1 }
            repeat(150) { step(vm) }
            assertEquals("Starting Aero Drone counts against the escort cap", 1, deployed())
            CombatPlaytestBridge.isOpMode = true
            novaAttack(vm, "aero_drone")
            until(vm) { deployed() == 2 }
            novaAttack(vm, vm.combatState!!.authored.reserveOwners.keys.first())
            until(vm) { deployed() == 3 }
            repeat(150) { step(vm) }
            assertEquals(3, deployed())
            assertEquals(3, vm.combatState!!.authored.reserveOwners.size)
            novaAttack(vm, boss)
            val state = vm.combatState!!
            assertTrue(state.combatants.values.none { it.isAlive && it.combatant.side == CombatSide.ENEMY })
            val reward = (state.outcome as CombatOutcome.Victory).rewards
            assertEquals(490, reward.xp)
            assertEquals(340, reward.credits)
            assertEquals(2, reward.ap)
            assertTrue(reward.drops.any { it.itemId == "power_cell" && it.quantity >= 2 })
            assertEquals(listOf(boss, "aero_drone"), vm.encounterEnemyIds)
        } finally {
            vm.close()
        }
    }

    @Test fun guardIsARealTurnAndRemainsAvailableWithoutAnOptionalSkill() {
        val (vm,_)=create(listOf("prototype_administrator"))
        try {
            until(vm) { vm.selectReadyPlayer("nova");vm.awaitingAction.value=="nova" }
            val before=vm.combatState!!.combatants.getValue("nova")
            vm.guard();step(vm)
            val guarded=vm.combatState!!.combatants.getValue("nova")
            assertTrue(guarded.statusEffects.any { it.id=="defend" })
            assertTrue(guarded.buffs.any { it.effect.stat=="defense" })
            assertEquals(before.hp,guarded.hp)
            assertNull(vm.awaitingAction.value)
        } finally { vm.close() }
    }
    @Test fun earnedSoloWardenArrivalWinsWithDirectAndReactivePoliciesAndKeepsRewards() {
        val root=if(File("src/debug/assets").exists()) File(".") else File("app")
        val checkpoint=MoshiProvider.instance.adapter(GameSessionState::class.java)
            .fromJson(File(root,"src/debug/assets/playtest/world1/world1_w1_mq05.json").readText())!!
        val evidence=File("build/reports/authored-encounters").apply { mkdirs() }
        for(seed in listOf(17,41,73)) for(policy in listOf("direct","react")) {
            val arrival=checkpoint.copy(roomId="launch_bay",completedEvents=checkpoint.completedEvents-
                setOf("w1_mq05_warden_victory","w1_record_warden_victory"))
            val (vm,store)=create(listOf("the_iron_warden"),arrival,seed)
            try {
                val initialCredits=store.state.value.playerCredits
                repeat(2400) {
                    if(vm.combatState!!.outcome!=null) return@repeat
                    step(vm);vm.selectReadyPlayer("nova")
                    if(vm.awaitingAction.value!="nova") return@repeat
                    val nova=vm.combatState!!.combatants.getValue("nova")
                    val heal=vm.inventory.value.firstOrNull { it.quantity>0 && it.item.id in setOf("medkit_i","medkit","ration_pack") }
                    if(nova.hp<nova.combatant.stats.maxHp*.4 && heal!=null) vm.useItem(heal,"nova")
                    else {
                        val skill=vm.skillsForPlayer("nova").firstOrNull { policy=="react" && vm.combatState!!.authored.preparations.isNotEmpty() && it.id=="nova_arc_tether" && vm.canUseSkill("nova",it) }
                        if(skill!=null) vm.useSkill(skill,listOf("the_iron_warden"))
                        else if(policy=="react" && vm.combatState!!.authored.preparations.isNotEmpty()) vm.guard()
                        else vm.playerAttack("the_iron_warden")
                    }
                }
                val result=vm.combatState!!
                File(evidence,"warden-$seed-$policy.txt").writeText(result.log.joinToString("\n")+"\nOutcome: ${result.outcome}\n")
                assertTrue("seed=$seed policy=$policy outcome=${result.outcome}",result.outcome is CombatOutcome.Victory)
                assertEquals(1,result.log.filterIsInstance<CombatLogEntry.TurnSkipped>().count { it.reason == "Emergency Overclock — shield down" })
                assertTrue(result.log.filterIsInstance<CombatLogEntry.TurnSkipped>().any { it.reason.startsWith("Preparing ") })
                assertEquals(initialCredits+225,store.state.value.playerCredits)
                assertEquals(1,store.state.value.inventory["warden_keycard"])
                assertEquals(listOf("nova"),vm.playerParty.map { it.id })
                assertEquals(setOf("the_iron_warden"),result.combatants.values.filter { it.combatant.side==CombatSide.ENEMY }.map { it.combatant.id }.toSet())
            } finally { vm.close() }
        }
    }
    @Test fun earnedBeastArrivalsWinWithBasicAttacksOrGuardAndKeepRewards() {
        val adapter=MoshiProvider.instance.adapter(GameSessionState::class.java)
        val evidence=File("build/reports/world2-authored").apply { mkdirs() }
        for(route in listOf("main","completionist")) for(seed in listOf(17,41,73)) for(policy in listOf("direct","guard")) {
            val arrival=javaClass.getResourceAsStream("/playtest/world2-beast-$route.json")!!.bufferedReader().use {
                adapter.fromJson(it.readText())!!
            }
            val (vm,store)=create(listOf("the_beast"),arrival,seed)
            try {
                val credits=arrival.playerCredits
                val resin=arrival.inventory["source_resin"] ?: 0
                repeat(2400) {
                    if(vm.combatState!!.outcome!=null) return@repeat
                    step(vm)
                    vm.playerParty.forEach { vm.selectReadyPlayer(it.id) }
                    val id=vm.awaitingAction.value ?: return@repeat
                    val state=vm.combatState!!
                    val wounded=state.combatants.values.filter { it.combatant.side==CombatSide.PLAYER && it.isAlive &&
                        it.hp<it.combatant.stats.maxHp*.45 }.minByOrNull { it.hp.toDouble()/it.combatant.stats.maxHp }
                    val healing=vm.inventory.value.filter { it.quantity>0 && it.item.type.equals("consumable",true) &&
                        (it.item.effect?.restoreHp ?: 0)>0 }.maxByOrNull { it.item.effect?.restoreHp ?: 0 }
                    val marked=state.authored.preparations.values.any { id in it.targetIds }
                    when {
                        wounded!=null && healing!=null -> vm.useItem(healing,if(healing.item.effect?.target=="party") null else wounded.combatant.id)
                        policy=="guard" && marked -> vm.guard()
                        else -> vm.playerAttack("the_beast")
                    }
                }
                val result=vm.combatState!!
                File(evidence,"beast-$route-$seed-$policy.txt").writeText(result.log.joinToString("\n")+"\nOutcome: ${result.outcome}\n")
                assertTrue("$route seed=$seed policy=$policy: ${result.outcome}",result.outcome is CombatOutcome.Victory)
                assertEquals(1,result.log.filterIsInstance<CombatLogEntry.TurnSkipped>().count { it.reason=="Frenzy — two strikes, then overextension" })
                assertTrue(result.log.filterIsInstance<CombatLogEntry.TurnSkipped>().any { it.reason.startsWith("Preparing ") })
                assertEquals(credits+250,store.state.value.playerCredits)
                assertEquals(resin+1,store.state.value.inventory["source_resin"])
                assertEquals(setOf("nova","zeke","orion"),vm.playerParty.map { it.id }.toSet())
                assertEquals(setOf("the_beast"),result.combatants.values.filter { it.combatant.side==CombatSide.ENEMY }.map { it.combatant.id }.toSet())
                if(policy=="guard") assertTrue(result.log.filterIsInstance<CombatLogEntry.ActionQueued>().any { it.action is CombatAction.Defend })
            } finally { vm.close() }
        }
    }

    @Test fun earnedCanopyPairSupportsEitherTargetOrderAndPaysTheModOnce() {
        val arrival = javaClass.getResourceAsStream("/playtest/focused-combat/plant-pair.json")!!.bufferedReader().use {
            MoshiProvider.instance.adapter(GameSessionState::class.java).fromJson(it.readText())!!
        }
        val room = assets.loadRooms().single { it.id == "sector9_canopy" }
        val enemies = room.enemyParties.orEmpty().single()
        val descriptor = EncounterDescriptor(room.enemyInstances.orEmpty().map {
            EncounterEnemyInstance(it.enemyId, hp = it.hp, vitality = it.vitality, stability = it.stability,
                extraDrops = it.extraDrops)
        })
        val evidence = File("build/reports/world2-authored").apply { mkdirs() }
        for (seed in listOf(17, 41, 73)) for (first in enemies) {
            val (vm, store) = create(enemies, arrival, seed, descriptor)
            try {
                until(vm) {
                    val state = requireNotNull(vm.combatState)
                    if (state.outcome != null) return@until true
                    vm.playerParty.forEach { vm.selectReadyPlayer(it.id) }
                    if (vm.awaitingAction.value != null) {
                        val target = (listOf(first) + enemies).first { state.combatants[it]?.isAlive == true }
                        vm.playerAttack(target)
                    }
                    false
                }
                val result = requireNotNull(vm.combatState)
                File(evidence, "canopy-$seed-$first.txt").writeText(result.log.joinToString("\n"))
                assertTrue("seed=$seed first=$first outcome=${result.outcome}", result.outcome is CombatOutcome.Victory)
                assertEquals((arrival.inventory["stability_gyro_mod"] ?: 0) + 1,
                    store.state.value.inventory["stability_gyro_mod"])
            } finally { vm.close() }
        }
    }

    @Test fun anUnequippedWeaponDoesNotCarryIntoCombat() {
        val fixture = prototypeFixture()
        val arrival = fixture.copy(
            equippedWeapons = fixture.equippedWeapons - "nova",
            unlockedWeapons = fixture.unlockedWeapons + "mining_pistol"
        )
        val (vm, _) = create(listOf("the_iron_warden"), arrival)
        try {
            assertNull(vm.combatState!!.combatants.getValue("nova").combatant.weapon)
        } finally { vm.close() }
    }

    @Test fun earlyOptionalGearCanBeUsedWithoutChangingTheEarnedCombatRoute() {
        val adapter = MoshiProvider.instance.adapter(GameSessionState::class.java)
        val project = if (File("src/debug/assets").exists()) File(".") else File("app")
        val warden = requireNotNull(adapter.fromJson(File(project,
            "src/debug/assets/playtest/world1/world1_w1_mq05.json").readText())).copy(roomId = "launch_bay")
        fun fixture(name: String) = requireNotNull(javaClass.getResourceAsStream("/playtest/$name.json"))
            .bufferedReader().use { requireNotNull(adapter.fromJson(it.readText())) }
        val canopy = assets.loadRooms().single { it.id == "sector9_canopy" }
        val canopyDescriptor = EncounterDescriptor(canopy.enemyInstances.orEmpty().map {
            EncounterEnemyInstance(it.enemyId, hp = it.hp, vitality = it.vitality, stability = it.stability,
                extraDrops = it.extraDrops)
        })
        val results = mutableListOf("fight,seed,loadout,actions,incomingDamage,healingItems")
        fun compare(
            fight: String, arrival: GameSessionState, enemies: List<String>, snack: String, owner: String,
            buff: String, laser: Boolean, encounter: EncounterDescriptor? = null
        ) {
            for (seed in listOf(17, 41, 73)) for (loadout in listOf("starter", "equipped", "use-once")) {
                val state = if (loadout == "starter") arrival else arrival.copy(
                    inventory = (arrival.inventory + (snack to 1)).let {
                        if (laser) it + ("nova_laser_blaster" to 1) else it
                    },
                    equippedItems = arrival.equippedItems + ("$owner:snack" to snack),
                    equippedWeapons = if (laser) arrival.equippedWeapons + ("nova" to "nova_laser_blaster")
                        else arrival.equippedWeapons
                )
                val (vm, store) = create(enemies, state, seed, encounter)
                try {
                    var used = false
                    until(vm, limit = 2400) {
                        val battle = requireNotNull(vm.combatState)
                        if (battle.outcome != null) return@until true
                        vm.playerParty.forEach { vm.selectReadyPlayer(it.id) }
                        val actor = vm.awaitingAction.value ?: return@until false
                        val wounded = battle.combatants.values.filter {
                            it.combatant.side == CombatSide.PLAYER && it.isAlive && it.hp < it.combatant.stats.maxHp * .45
                        }.minByOrNull { it.hp.toDouble() / it.combatant.stats.maxHp }
                        val heal = vm.inventory.value.filter {
                            it.quantity > 0 && it.item.type.equals("consumable", true) && (it.item.effect?.restoreHp ?: 0) > 0
                        }.maxByOrNull { it.item.effect?.restoreHp ?: 0 }
                        when {
                            wounded != null && heal != null ->
                                vm.useItem(heal, if (heal.item.effect?.target == "party") null else wounded.combatant.id)
                            loadout == "use-once" && actor == owner && !used -> {
                                assertTrue(vm.canUseSnack(actor))
                                vm.useSnack()
                                val after = requireNotNull(vm.combatState).combatants.getValue(actor)
                                assertTrue(after.buffs.any { it.effect.stat == buff })
                                assertTrue(vm.snackCooldownRemaining(actor) > 0)
                                assertFalse(vm.canUseSnack(actor))
                                used = true
                            }
                            else -> vm.playerAttack(enemies.first { battle.combatants[it]?.isAlive == true })
                        }
                        false
                    }
                    val result = requireNotNull(vm.combatState)
                    assertTrue("$fight seed=$seed loadout=$loadout outcome=${result.outcome}", result.outcome is CombatOutcome.Victory)
                    assertEquals(loadout == "use-once", used)
                    if (loadout != "starter") assertEquals(1, store.state.value.inventory[snack])
                    val actions = result.log.filterIsInstance<CombatLogEntry.ActionQueued>()
                        .filter { it.action.actorId in state.partyMembers }
                    val damage = result.log.filterIsInstance<CombatLogEntry.Damage>()
                        .filter { it.targetId in state.partyMembers }.sumOf { it.amount }
                    results += "$fight,$seed,$loadout,${actions.size},$damage,${actions.count { it.action is CombatAction.ItemUse }}"
                } finally { vm.close() }
            }
        }
        compare("Warden", warden, listOf("the_iron_warden"), "starbar_crunch", "nova", "evasion", laser = false)
        compare("Canopy", fixture("focused-combat/plant-pair"), listOf("spore_spitter", "stalker_vine"),
            "starbar_crunch", "nova", "evasion", laser = true, encounter = canopyDescriptor)
        compare("Beast", fixture("world2-beast-main"), listOf("the_beast"),
            "ion_poppers", "zeke", "accuracy", laser = true)
        File("build/reports/world2-readiness/early-optional-gear.csv").apply {
            requireNotNull(parentFile).mkdirs()
            writeText(results.joinToString("\n", postfix = "\n"))
        }
    }

    @Test fun avatarPartnerDeathBreaksPreparationPermanentlyAndOnlyBodyVictoryPays() {
        for (bodyFirst in listOf(false, true)) {
            val (vm, store) = create(listOf("compliance_avatar"))
            try {
                assertEquals(listOf("compliance_avatar"), vm.encounterEnemyIds)
                assertTrue("directive_conductor" in vm.enemyCombatantIds)
                val cells = store.state.value.inventory["power_cell"] ?: 0
                until(vm) { vm.combatState!!.authored.preparations["compliance_avatar"]?.sourceId == "directive_conductor" }
                CombatPlaytestBridge.isOpMode = true
                if (!bodyFirst) {
                    novaAttack(vm, "directive_conductor")
                    val broken = vm.combatState!!
                    assertNull(broken.outcome)
                    assertTrue("compliance_avatar" in broken.authored.brokenLinks)
                    assertFalse("compliance_avatar" in broken.authored.preparations)
                    assertFalse(broken.combatants.getValue("compliance_avatar").statusEffects.any { it.id == "ring_command_ward" })
                    CombatPlaytestBridge.isOpMode = false
                    until(vm) { vm.combatState!!.authored.preparations["compliance_avatar"]?.skillId == "ring_unlinked_fire" }
                    assertEquals("compliance_avatar", vm.combatState!!.authored.preparations.getValue("compliance_avatar").sourceId)
                    CombatPlaytestBridge.isOpMode = true
                }
                novaAttack(vm, "compliance_avatar")
                val victory = vm.combatState!!.outcome as CombatOutcome.Victory
                assertFalse(vm.combatState!!.combatants.getValue("directive_conductor").isAlive)
                assertEquals(750, victory.rewards.xp)
                assertEquals(500, victory.rewards.credits)
                assertEquals(cells + 3, store.state.value.inventory["power_cell"])
            } finally { vm.close(); CombatPlaytestBridge.isOpMode = false }
        }
    }

    @Test fun historicalSideRoomAvatarReceiptCannotCompleteTheMainBossTask() {
        val store = GameSessionStore()
        store.startQuest("w5_mq25")
        store.setQuestTaskCompleted("w5_mq25", "witness_soloist", true)
        store.restore(store.state.value.copy(roomId = "deep_tear", roomStates = mapOf(
            "deep_throne_room_scale_04" to mapOf("encounter_cleared:compliance_avatar" to true))))
        val manager = com.example.starborn.domain.event.EventManager(
            events = reader.readList<com.example.starborn.domain.model.GameEvent>("events.json"), sessionStore = store)
        manager.reconcileRecordedVictories()
        assertFalse("ms_w5_avatar_defeated" in store.state.value.completedMilestones)
        fun victory(ids: List<String>, room: String) = manager.handleTrigger("encounter_victory",
            com.example.starborn.domain.event.EventPayload.EncounterOutcome(ids,
                com.example.starborn.domain.event.EventPayload.EncounterOutcome.Outcome.VICTORY, room))
        victory(listOf("compliance_avatar"), "deep_throne_room_scale_04")
        victory(listOf("directive_conductor"), "deep_tear")
        assertFalse("ms_w5_avatar_defeated" in store.state.value.completedMilestones)
        victory(listOf("compliance_avatar"), "deep_tear")
        assertTrue("ms_w5_avatar_defeated" in store.state.value.completedMilestones)
    }

    @Test fun ringFighterPairStaggersItsOpeningAndConductorHasFiniteWards() {
        val (fighters, _) = create(listOf("orbital_fighter", "orbital_fighter"))
        try {
            assertNotEquals(fighters.combatState!!.authored.cyclePositions["orbital_fighter#1"],
                fighters.combatState!!.authored.cyclePositions["orbital_fighter#2"])
            until(fighters) { fighters.combatState!!.authored.preparations.isNotEmpty() }
            assertTrue(fighters.combatState!!.authored.preparations.values.all { it.skillId == "ring_guided_pass" })
        } finally { fighters.close() }
        val (boss, _) = create(listOf("compliance_avatar"))
        try {
            assertEquals(3, boss.combatState!!.authored.remainingUses["directive_conductor"]?.get("ring_command_ward"))
        } finally { boss.close() }
    }

    @Test fun plantHealingExhaustsItsVisibleReservesAndPackMembersBeginOnDifferentBeats() {
        val (vine,_)=create(listOf("stalker_vine","spore_spitter"))
        try {
            assertEquals(2,vine.combatState!!.authored.remainingUses["stalker_vine"]?.get("sector9_mend"))
            until(vine,2400) { vine.combatState!!.authored.remainingUses["stalker_vine"]?.get("sector9_mend")==0 }
            repeat(400) { step(vine) }
            val casts=vine.combatState!!.log.filterIsInstance<CombatLogEntry.ActionQueued>().mapNotNull { it.action as? CombatAction.SkillUse }
            assertEquals(2,casts.count { it.actorId=="stalker_vine" && it.skillId=="sector9_mend" })
            assertEquals(1,casts.count { it.actorId=="stalker_vine" && it.skillId=="sector9_shelter" })
            assertTrue(casts.filter { it.skillId=="sector9_mend" }.all { it.targetIds.toSet()==setOf("stalker_vine","spore_spitter") })
        } finally { vine.close() }
        for(kind in listOf("shard_hound","moss_borer")) {
            val (pack,_)=create(listOf(kind,kind))
            try {
                assertEquals(0,pack.combatState!!.authored.cyclePositions["$kind#1"])
                assertEquals(2,pack.combatState!!.authored.cyclePositions["$kind#2"])
                until(pack) { pack.combatState!!.log.filterIsInstance<CombatLogEntry.ActionQueued>()
                    .filter { it.actorId.startsWith("$kind#") }.map { it.actorId }.distinct().size==2 }
                val first=pack.combatState!!.log.filterIsInstance<CombatLogEntry.ActionQueued>()
                    .filter { it.actorId.startsWith("$kind#") }.groupBy { it.actorId }.mapValues { it.value.first().action }
                val a=(first.getValue("$kind#1") as CombatAction.SkillUse).skillId
                val b=(first.getValue("$kind#2") as CombatAction.SkillUse).skillId
                assertNotEquals(a,b)
            } finally { pack.close() }
        }
    }

}
