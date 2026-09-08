package com.example.starborn.domain.playtest

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.dialogue.DialogueTriggerParser
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.*
import com.example.starborn.domain.session.GameSessionStore
import org.junit.Assert.*
import org.junit.Test

class QuestPayoutIntegrityTest {
    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)

    private fun flatten(actions: List<EventAction>): List<EventAction> = actions.flatMap {
        listOf(it) + flatten(it.onComplete.orEmpty()) + flatten(it.`do`.orEmpty()) + flatten(it.elseDo.orEmpty())
    }

    @Test fun `advertised item rewards match completion or prerequisite grants`() {
        val quests = reader.readList<Quest>("quests.json").associateBy { it.id }
        val events = reader.readList<GameEvent>("events.json").associateBy { it.id }
        val paths = events.values.map { it.id to it.actions } +
            reader.readList<DialogueLine>("dialogue.json").mapNotNull { line ->
                line.trigger?.let { line.id to DialogueTriggerParser.parse(it) }
            }
        val covered = mutableSetOf<String>()
        paths.forEach { (id, actions) ->
            val flat = flatten(actions)
            val quest = flat.firstOrNull { it.type == "complete_quest" }
                ?.let { quests[it.completeQuest ?: it.questId] } ?: return@forEach
            val promised = quest.rewards.filter { it.type == "item" }
            if (promised.isEmpty()) return@forEach
            val prerequisiteActions = if (quest.id == "w4_mq20") {
                listOf("w4_mq20_steal_engine", "w4_mq20_steal_arrays")
                    .flatMap { flatten(events.getValue(it).actions) }
            } else emptyList()
            val grants = mutableMapOf<String, Int>()
            val store = GameSessionStore()
            val hooks = EventHooks(onGiveItem = { item, quantity ->
                grants[item] = grants.getOrDefault(item, 0) + quantity
            }, onReward = { reward -> reward.items.forEach {
                grants[it.itemId] = grants.getOrDefault(it.itemId, 0) + (it.quantity ?: 1)
            } })
            // This checks isolated payout quantities, not route reachability or
            // dialogue replay. The ending test below retains real conditions.
            val payoutActions = (prerequisiteActions + flat).filter {
                it.type in setOf("give_item", "give_reward", "grant_reward", "complete_quest")
            }
            val event = GameEvent(id = id, trigger = EventTrigger(type = "player_action", action = "claim"), actions = payoutActions)
            val manager = EventManager(listOf(event), store, hooks)
            manager.handleTrigger("player_action", EventPayload.Action("claim"))
            // This explicitly authored alternative trades the ledger for ammunition.
            val expected = if (id == "w3_sq14_weaponize_ledger") mapOf("phase_rounds" to 1)
                else promised.associate { requireNotNull(it.itemId) to (it.quantity ?: 1) }
            expected.forEach { (item, quantity) ->
                assertEquals("${quest.id} via $id must grant $item exactly once", quantity, grants[item])
            }
            if (id == "w3_sq14_weaponize_ledger") assertFalse(grants.containsKey("encrypted_ledger"))
            val firstGrants = grants.toMap()
            manager.handleTrigger("player_action", EventPayload.Action("claim"))
            assertEquals("Replaying payout actions must not duplicate items: $id", firstGrants, grants)
            covered += quest.id
        }
        assertEquals(quests.values.filter { q -> q.rewards.any { it.type == "item" } }.map { it.id }.toSet(), covered)
    }

    @Test fun `corporate espionage endings are mutually exclusive after restoring state`() {
        val events = reader.readList<GameEvent>("events.json")
        val endings = listOf("w3_sq14_leak_ledger", "w3_sq14_weaponize_ledger")
        endings.forEach { chosen ->
            val store = GameSessionStore().apply {
                startQuest("w3_sq14")
                setMilestone("ms_w3_ledger_stolen")
            }
            fun manager(target: GameSessionStore) = EventManager(events, target, EventHooks(
                onGiveItem = { item, qty -> target.setInventory(target.state.value.inventory +
                    (item to (target.state.value.inventory.getOrDefault(item, 0) + qty))) },
                onGiveXp = target::addXp
            ))
            manager(store).handleTrigger("player_action", EventPayload.Action(chosen))
            val awarded = store.state.value
            assertTrue("w3_sq14" in awarded.completedQuests)
            assertEquals(250, awarded.playerXp)
            val item = if (chosen.endsWith("leak_ledger")) "encrypted_ledger" else "phase_rounds"
            assertEquals(mapOf(item to 1), awarded.inventory)
            val restored = GameSessionStore().apply { restore(awarded) }
            endings.forEach { manager(restored).handleTrigger("player_action", EventPayload.Action(it)) }
            assertEquals(awarded.inventory, restored.state.value.inventory)
            assertEquals(awarded.playerXp, restored.state.value.playerXp)
        }
    }

    @Test fun `every quest completion path pays its advertised XP`() {
        val quests = reader.readList<Quest>("quests.json").associateBy { it.id }
        val paths = reader.readList<GameEvent>("events.json").map { it.id to it.actions } +
            reader.readList<DialogueLine>("dialogue.json").mapNotNull { line ->
                line.trigger?.let { line.id to DialogueTriggerParser.parse(it) }
            }
        val covered = mutableSetOf<String>()
        paths.forEach { (id, actions) ->
            val flat = flatten(actions)
            val completions = flat.filter { it.type == "complete_quest" }
                .mapNotNull { quests[it.completeQuest ?: it.questId] }
            if (completions.isEmpty()) return@forEach
            assertEquals("Split multi-quest payout fixture: $id", 1, completions.size)
            val quest = completions.single()
            val expected = quest.rewards.filter { it.type == "xp" }.sumOf { it.amount ?: 0 }
            val store = GameSessionStore()
            // Isolate payout/completion actions from navigation and conditional
            // branches. Campaign integration separately executes the main routes.
            val payoutActions = flat.filter { it.type in setOf("give_xp", "give_reward", "grant_reward", "complete_quest") }
            val event = GameEvent(id = id, trigger = EventTrigger(type = "player_action", action = "claim"), actions = payoutActions)
            val hooks = EventHooks(onGiveXp = store::addXp, onReward = { store.addXp(it.xp ?: 0) },
                onPlayCinematic = { _, done -> done() })
            val manager = EventManager(listOf(event), store, hooks)
            manager.handleTrigger("player_action", EventPayload.Action("claim"))
            assertEquals("XP payout for ${quest.id} via $id", expected, store.state.value.playerXp)
            assertTrue("Completion must execute for $id", quest.id in store.state.value.completedQuests)
            manager.handleTrigger("player_action", EventPayload.Action("claim"))
            assertEquals("Event replay must not pay again: $id", expected, store.state.value.playerXp)
            covered += quest.id
        }
        assertEquals("Every catalog quest needs a checked completion path", quests.keys, covered)
    }
}
