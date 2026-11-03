package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.*
import com.example.starborn.feature.exploration.presentation.*
import org.junit.Test
import org.junit.Assert.*
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.migrateOpeningNarrativeState
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import java.io.File

/** Conservative content inventory: all authored actions against each description, before visibility filtering. */
class DesktopActionReachabilityAuditTest {
    @Test fun usableFunctionalObjectsHaveSharedDescriptionLinksAcrossAuthoredVariants() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val events = reader.readList<GameEvent>("events.json")
        val failures = linkedSetOf<String>()
        var checked = 0
        for (room in reader.readList<Room>("rooms.json")) for (raw in room.actions) {
            val type = raw["type"] as? String ?: "generic"
            val name = raw["name"] as? String ?: continue
            val eventId = raw["action_event"] as? String
            if (type in setOf("shop", "rest_stop", "tinkering") || raw["hide_inline"] == true) continue
            if (type in setOf("generic", "inspect") && eventId.isNullOrBlank()) continue
            val matching = events.filter { it.trigger.type == "player_action" && it.trigger.action == eventId }
            val probes = if (matching.isEmpty()) listOf<GameEvent?>(null) else matching
            for (variant in listOf<RoomDescriptionVariant?>(null) + room.descriptionVariants) for (event in probes) {
                val conditions = event?.conditions.orEmpty()
                val milestones = (variant?.requiresMilestones.orEmpty() + conditions.filter { it.type in setOf("milestone", "milestone_set") }.mapNotNull { it.milestone } +
                    listOfNotNull(raw["requires_milestone"] as? String, raw["show_when_milestone"] as? String) +
                    (raw["requires_milestones"] as? List<*>)?.filterIsInstance<String>().orEmpty()).toSet()
                // Model the authored prerequisite of Orion's briefing: alignment has already woken him.
                // The separate legacy-state tests cover incomplete/corrupt milestone combinations.
                val canonicalMilestones = if ("ms_w2_orion_briefed" in milestones) milestones + "ms_w2_stasis_awake" else milestones
                val completedFromVariant = variant?.requiresMilestones.orEmpty().mapNotNull {
                    Regex("ms_(w[1-6]_[ms]q[0-9]+)_complete").matchEntire(it)?.groupValues?.get(1)
                }.toSet() + if ("ms_w1_mq01_cutter_surge" in variant?.requiresMilestones.orEmpty()) setOf("w1_mq01") else emptySet()
                val flags = room.state.mapNotNull { (k,v) -> (v as? Boolean)?.let { k to it } }.toMap() +
                    variant?.requiresState.orEmpty() + variant?.forbiddenState.orEmpty().mapValues { !it.value } +
                    listOfNotNull((raw["show_when_state"] as? String)?.let { it to true }, (raw["hide_when_state"] as? String)?.let { it to false }).toMap()
                val active = conditions.filter { it.type == "quest_active" }.mapNotNull { it.questId }.toSet() + listOfNotNull(raw["show_when_quest_active"] as? String)
                val tasks = conditions.filter { it.type == "quest_task_done" }.groupBy { requireNotNull(it.questId) }.mapValues { (_,c) -> c.mapNotNull { it.taskId }.toSet() }
                val state = GameSessionState(roomId = room.id, activeQuests = active - completedFromVariant,
                    completedQuests = conditions.filter { it.type == "quest_completed" }.mapNotNull { it.questId }.toSet() + completedFromVariant,
                    completedMilestones = canonicalMilestones, questTasksCompleted = tasks, roomStates = mapOf(room.id to flags),
                    inventory = conditions.filter { it.type == "item" }.associate { requireNotNull(it.itemId ?: it.item) to (it.quantity ?: 1) }).migrateOpeningNarrativeState()
                val currentFlags = state.roomStates[room.id].orEmpty()
                if (!narrativeActionVisible(raw, room.state.mapValues { it.value as? Boolean ?: false }, currentFlags, state.completedMilestones)) continue
                if (event != null) {
                    val store = GameSessionStore().apply { restore(state) }
                    if (!EventManager(listOf(event), store).canHandleTrigger("player_action", EventPayload.Action(requireNotNull(eventId)))) continue
                }
                val prose = resolveRoomDescription(room, currentFlags, state.completedMilestones, false)
                val actions = room.actions.filter { narrativeActionVisible(it, room.state.mapValues { e -> e.value as? Boolean ?: false }, currentFlags, state.completedMilestones) }
                    .filter { (it["show_when_quest_active"] as? String)?.let { q -> q in active } != false }
                    .mapNotNull { a -> (a["name"] as? String)?.let { GenericAction(it, a["type"] as? String ?: "generic", hideInline = a["hide_inline"] as? Boolean ?: false) } }
                val plan = buildInlineActionPlan(prose, actions, emptyMap(), room)
                checked++
                if (plan?.segments.orEmpty().none { (it.target as? InlineActionTarget.Room)?.action?.name == name }) failures += "${room.id}: $name (milestones=${state.completedMilestones})"
            }
        }
        File("build/reports/desktop-functional-link-check.txt").apply { parentFile.mkdirs(); writeText("Checked $checked condition-satisfying probes.\n" + failures.joinToString("\n")) }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun inventoryActionsMissingFromRoomProse() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rows = mutableListOf<String>()
        val rooms = reader.readList<Room>("rooms.json")
        for (room in rooms) {
            val actions = room.actions.mapNotNull { raw ->
                val name = raw["name"] as? String ?: return@mapNotNull null
                val type = raw["type"] as? String ?: "generic"
                if (type in setOf("rest_stop", "rest", "camp", "cookfire")) RestStopAction(name)
                else GenericAction(name, type, actionEvent = raw["action_event"] as? String,
                    hideInline = raw["hide_inline"] as? Boolean ?: false)
            }
            val descriptions = listOfNotNull(room.description, room.descriptionDark) + room.descriptionVariants.map { it.description }
            descriptions.distinct().forEachIndexed { variant, prose ->
                val plan = buildInlineActionPlan(prose, actions, emptyMap(), room)
                val linked = plan?.segments.orEmpty().mapNotNull { (it.target as? InlineActionTarget.Room)?.action?.name }.toSet()
                actions.filterNot { it is RestStopAction || it.name in linked }.forEach { action ->
                    val raw = room.actions.first { it["name"] == action.name }
                    val gated = raw.keys.any { it.startsWith("requires_") || it.startsWith("hide_when") || it.startsWith("show_when") }
                    rows += "${room.id}\t$variant\t${(action as GenericAction).type}\t${action.name}\t${action.actionEvent.orEmpty()}\tgated=$gated\thideInline=${action.hideInline}"
                }
            }
        }
        File("build/reports/desktop-action-reachability.tsv").apply { parentFile.mkdirs(); writeText(rows.distinct().joinToString("\n") + "\n") }
        println("Rooms=${rooms.size}; conservative unmatched room/description/action combinations=${rows.size}")
    }
}
