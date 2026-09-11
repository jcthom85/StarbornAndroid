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

    @Test fun `scanning beach weeds does not prevent the separate tideglass quest payout`() {
        val events = reader.readList<GameEvent>("events.json").filter {
            it.trigger.action in setOf("w2_sq01_scan_weeds", "w2_sq03_gather_tideglass")
        }
        assertEquals(2, events.size)
        val store = GameSessionStore().apply {
            startQuest("w2_sq01")
            startQuest("w2_sq03")
            setQuestTaskCompleted("w2_sq03", "visit_beach", true)
        }
        val grants = mutableMapOf<String, Int>()
        val manager = EventManager(events, store, EventHooks(onGiveItem = { item, qty ->
            grants[item] = grants.getOrDefault(item, 0) + qty
        }))
        manager.handleTrigger("player_action", EventPayload.Action("w2_sq01_scan_weeds"))
        assertTrue("ms_w2_flora_weeds_scanned" in store.state.value.completedMilestones)
        assertTrue(grants.isEmpty())
        manager.handleTrigger("player_action", EventPayload.Action("w2_sq03_gather_tideglass"))
        val expected = mapOf("herb" to 1, "beast_meat" to 1, "schematic_source_resin" to 1)
        assertEquals(expected, grants)
        manager.handleTrigger("player_action", EventPayload.Action("w2_sq03_gather_tideglass"))
        assertEquals(expected, grants)
    }

    @Test fun `inspecting grotto cache leaves the separate prism reward unsolved`() {
        val room = reader.readList<Room>("rooms.json").single { it.id == "sector9_beach_grotto" }
        val prism = room.actions.single { it["name"] == "bioluminescent prism" }
        val puzzle = reader.readList<TuningPuzzle>("tuning_puzzles.json").single { it.id == prism["puzzle_id"] }
        val events = reader.readList<GameEvent>("events.json")
        val store = GameSessionStore()
        val grants = mutableMapOf<String, Int>()
        val manager = EventManager(events.filter {
            it.trigger.action in setOf("w2_sq03_gather_relic", puzzle.successEvent)
        }, store, EventHooks(onGiveItem = { item, qty ->
            grants[item] = grants.getOrDefault(item, 0) + qty
        }))
        manager.handleTrigger("player_action", EventPayload.Action("w2_sq03_gather_relic"))
        assertTrue("ms_w2_grotto_relic_gathered" in store.state.value.completedMilestones)
        val solved = prism["requires_milestone_not_set"] as String
        assertFalse(solved in store.state.value.completedMilestones)
        assertTrue(grants.isEmpty())
        // Exercise the authored success event, not the rendered slider puzzle.
        val success = requireNotNull(puzzle.successEvent)
        manager.handleTrigger("player_action", EventPayload.Action(success))
        assertTrue(solved in store.state.value.completedMilestones)
        assertEquals(mapOf("relic_mod_biolum_focus" to 1), grants)
        manager.handleTrigger("player_action", EventPayload.Action(success))
        assertEquals(mapOf("relic_mod_biolum_focus" to 1), grants)
    }

    @Test fun `world two film caches grant once including after session restore`() {
        val events = reader.readList<GameEvent>("events.json")
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val caches = listOf(
            Triple("sector9_stream_cave_depths", "waterproof cargo case", "vhs_tape_03"),
            Triple("sector9_ridge_plateau", "ridge overlook cache", "vhs_tape_04")
        )
        caches.forEach { (roomId, actionName, itemId) ->
            val action = rooms.getValue(roomId).actions.single { it["name"] == actionName }
            val eventId = action["action_event"] as String
            val event = events.single { it.id == eventId }
            assertFalse("$eventId must be one-shot", event.repeatable)
            val store = GameSessionStore()
            fun manager(target: GameSessionStore) = EventManager(listOf(event), target, EventHooks(
                onGiveItem = { item, qty -> target.setInventory(target.state.value.inventory +
                    (item to (target.state.value.inventory.getOrDefault(item, 0) + qty))) }
            ))
            val manager = manager(store)
            manager.handleTrigger("player_action", EventPayload.Action(eventId))
            val awarded = store.state.value
            assertEquals(mapOf(itemId to 1), awarded.inventory)
            assertTrue(eventId in awarded.completedEvents)
            manager.handleTrigger("player_action", EventPayload.Action(eventId))
            assertEquals(awarded.inventory, store.state.value.inventory)
            // In-memory restore verifies replay protection, not save-file I/O or UI.
            val restored = GameSessionStore().apply { restore(awarded) }
            manager(restored).handleTrigger("player_action", EventPayload.Action(eventId))
            assertEquals(awarded.inventory, restored.state.value.inventory)
        }
    }

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
