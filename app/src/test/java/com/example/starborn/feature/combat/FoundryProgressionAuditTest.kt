package com.example.starborn.feature.combat

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.*
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.event.*
import com.example.starborn.domain.leveling.*
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Authored-source and isolated delivery checks, NOT a traversed campaign route. */
class FoundryProgressionAuditTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val assets = WorldAssetDataSource(reader)
    @Test fun lowerXpFixtureDoesNotGrantLevelNineSkills() {
        val state = FoundryLoadouts.session("weak_ap", assets, 6430)
        assertEquals(7, state.playerLevel)
        assertTrue("orion_nano_repair" in state.unlockedSkills)
        assertFalse("orion_disruption_pulse" in state.unlockedSkills)
        assertFalse("zeke_overload_fists" in state.unlockedSkills)
        assertTrue("nova_quiet_steps" in state.unlockedSkills)
    }
    private fun flatten(actions: List<EventAction>): List<EventAction> = actions.flatMap {
        listOf(it) + flatten(it.onComplete.orEmpty()) + flatten(it.`do`.orEmpty()) + flatten(it.elseDo.orEmpty())
    }

    @Test fun mainQuestXpListingsReconcileWithCallbacksAndDialogue() {
        val quests = reader.readList<Quest>("quests.json").filter { it.id.matches(Regex("w[123]_mq\\d+")) }
        val events = reader.readList<GameEvent>("events.json")
        val dialogue = reader.readList<DialogueLine>("dialogue.json")
        val report = mutableListOf("Authored XP source reconciliation; NOT a route-earned ledger.")
        for (quest in quests) {
            val sources = events.filter { it.id.startsWith(quest.id + "_") }.mapNotNull { event ->
                val xp = flatten(event.actions).filter { it.type == "give_xp" }.sumOf { it.xp ?: 0 }
                if (xp > 0) event.id to xp else null
            } + dialogue.mapNotNull { line ->
                val actions = DialogueTriggerParser.parse(line.trigger)
                if (actions.none { it.type == "complete_quest" && (it.questId ?: it.completeQuest) == quest.id }) null
                else actions.filter { it.type == "give_xp" }.sumOf { it.xp ?: 0 }.takeIf { it > 0 }?.let { line.id to it }
            }
            val listed = quest.rewards.filter { it.type == "xp" }.sumOf { it.amount ?: 0 }
            assertEquals(quest.id, listed, sources.sumOf { it.second })
            report += "${quest.id}: listed=$listed sources=$sources"
        }
        assertEquals(15, quests.size)
        assertEquals(4525, quests.flatMap { it.rewards }.filter { it.type == "xp" }.sumOf { it.amount ?: 0 })
        File("build/reports/foundry-progression").apply { mkdirs() }
            .resolve("xp-sources.txt").writeText(report.joinToString("\n"))
    }

    @Test fun authoredCutterCallbackAwardsXpOnlyAfterCompletionAndOnlyOnce() {
        val event = reader.readList<GameEvent>("events.json").single { it.id == "w1_mq01_cutter_surge" }
        val store = GameSessionStore().apply {
            restore(GameSessionState(playerId = "nova", partyMembers = listOf("nova"),
                activeQuests = setOf("w1_mq01"), inventory = mapOf("functional_cryo_inductor" to 1),
                questTasksCompleted = mapOf("w1_mq01" to setOf("use_tinkering_table", "patch_flux_liner", "confirm_governor_bypass"))))
        }
        val awarder = ExplorationXpAwarder(store, LevelingManager(requireNotNull(assets.loadLevelingData())), requireNotNull(assets.loadProgressionData()))
        var finish: (() -> Unit)? = null
        val manager = EventManager(listOf(event), store, EventHooks(
            onPlayCinematic = { _, done -> finish = done }, onGiveXp = awarder::award))
        manager.handleTrigger("player_action", EventPayload.Action("w1_mq01_cutter_surge"))
        assertNotNull(finish)
        assertEquals(0, store.state.value.playerXp)
        finish!!()
        assertEquals(50, store.state.value.playerXp)
        manager.handleTrigger("player_action", EventPayload.Action("w1_mq01_cutter_surge"))
        assertEquals(50, store.state.value.playerXp)
    }

    @Test fun newlyRecruitedMemberInheritsLeaderProgressAndEarnsSubsequentXp() {
        val store = GameSessionStore().apply { restore(GameSessionState(playerId = "nova",
            playerXp = 4525, playerLevel = 6, partyMembers = listOf("nova"))) }
        store.addPartyMember("gh0st")
        assertEquals(4525, store.state.value.partyMemberXp["gh0st"])
        assertEquals(6, store.state.value.partyMemberLevels["gh0st"])
        ExplorationXpAwarder(store, LevelingManager(requireNotNull(assets.loadLevelingData())), requireNotNull(assets.loadProgressionData())).award(100)
        assertEquals(4625, store.state.value.partyMemberXp["gh0st"])
        assertEquals(4625, store.state.value.playerXp)
        assertTrue("gh0st_venom_edge" in store.state.value.unlockedSkills)
        assertEquals(0, store.state.value.playerAp)
    }
}
