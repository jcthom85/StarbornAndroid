package com.example.starborn.feature.exploration.presentation

import com.example.starborn.domain.model.*
import com.example.starborn.feature.exploration.viewmodel.ActionHintUi
import java.util.Locale

sealed interface InlineActionTarget {
    data class Room(val action: RoomAction) : InlineActionTarget
    data class Npc(val name: String) : InlineActionTarget
    data class Enemy(val id: String, val label: String) : InlineActionTarget
}

data class InlineActionSegment(
    val id: String,
    val target: InlineActionTarget,
    val start: Int,
    val end: Int,
    val locked: Boolean,
    val inspected: Boolean = false
)

data class InlineActionPlan(
    val description: String,
    val segments: List<InlineActionSegment>
)

fun buildInlineActionPlan(
    description: String?,
    actions: List<RoomAction>,
    hints: Map<String, ActionHintUi>,
    room: Room?,
    inspectionState: Map<String, Boolean> = emptyMap()
): InlineActionPlan? {
    if (description.isNullOrBlank()) return null
    val travelActions = actions.filterIsInstance<TravelAction>()
    val sourceDescription = if (travelActions.isEmpty()) {
        description
    } else {
        description.trimEnd() + "\n\nPaths: " + travelActions.joinToString(" | ") { action ->
            "[action:${action.name}|${action.name}]"
        }
    }
    val segments = mutableListOf<InlineActionSegment>()
    val occupied = mutableListOf<IntRange>()

    // Explicit action references use the authored name, with an optional display label.
    // Only actions supplied by the caller (already filtered for visibility) can resolve.
    val markerPattern = Regex("""\[(npc|action):([^\]]+)]""", RegexOption.IGNORE_CASE)
    val explicitActions = mutableSetOf<String>()
    val parsedDescription = buildString {
        var cursor = 0
        markerPattern.findAll(sourceDescription).forEach { match ->
            append(sourceDescription, cursor, match.range.first)
            val isAction = match.groupValues[1].equals("action", ignoreCase = true)
            val body = match.groupValues[2]
            val reference = body.substringBefore('|').trim()
            val label = if (isAction) body.substringAfter('|', reference).trim() else body.trim()
            val start = length
            append(label)
            val end = length
            if (label.isNotBlank()) {
                val range = start until end
                occupied += range
                if (isAction) {
                    val action = actions.filter { it.isInlineDescriptionAction() }
                        .singleOrNull { it.name.equals(reference, ignoreCase = true) }
                    if (action != null) {
                        val key = action.actionKey()
                        explicitActions += key
                        segments += InlineActionSegment(
                            id = "$key:marker:$start",
                            target = InlineActionTarget.Room(action),
                            start = start,
                            end = end,
                            locked = hints[key]?.locked == true
                        )
                    }
                } else segments += InlineActionSegment(
                    id = "npc-marker:$label:$start",
                    target = InlineActionTarget.Npc(label),
                    start = start,
                    end = end,
                    locked = false
                )
            }
            cursor = match.range.last + 1
        }
        append(sourceDescription, cursor, sourceDescription.length)
    }
    val lower = parsedDescription.lowercase(Locale.getDefault())

    fun variantsFor(label: String): List<String> {
        if (label.isBlank()) return emptyList()
        return buildList {
            add(label)
            val normalizedUnderscore = label.replace('_', ' ')
            if (normalizedUnderscore != label) add(normalizedUnderscore)
            val normalizedDash = label.replace('-', ' ')
            if (normalizedDash != label) add(normalizedDash)
            val normalizedApostrophe = label.replace('’', '\'')
            if (normalizedApostrophe != label) add(normalizedApostrophe)
        }.distinctBy { it.lowercase(Locale.getDefault()) }
            .sortedByDescending { it.length }
    }

    fun findRange(label: String): IntRange? {
        val variants = variantsFor(label)
        if (variants.isEmpty()) return null
        for (variant in variants) {
            val needle = variant.lowercase(Locale.getDefault())
            var searchIndex = 0
            while (searchIndex <= lower.length - needle.length) {
                val index = lower.indexOf(needle, searchIndex)
                if (index < 0) break
                val end = index + needle.length
                val startOk = index == 0 || !lower[index - 1].isLetterOrDigit()
                val endOk = end == lower.length || !lower[end].isLetterOrDigit()
                val rangeCandidate = index until end
                if (startOk && endOk && occupied.none { rangesOverlap(it, rangeCandidate) }) {
                    return rangeCandidate
                }
                searchIndex = index + 1
            }
        }
        return null
    }

    // Match whole object names before shorter names such as "monitor" can claim their text.
    actions.filter { it.isInlineDescriptionAction() }.sortedByDescending { it.name.length }.forEach { action ->
        if (action.actionKey() in explicitActions) return@forEach
        val baseName = action.name
        if (baseName.isBlank()) return@forEach
        val range = findRange(baseName) ?: return@forEach
        occupied += range
        val key = action.actionKey()
        val locked = hints[key]?.locked == true
        segments += InlineActionSegment(
            id = key,
            target = InlineActionTarget.Room(action),
            start = range.first,
            end = range.last + 1,
            locked = locked
        )
    }

    room?.enemies.orEmpty()
        .filter { it.isNotBlank() }
        .forEach { enemyId ->
            val label = enemyId
            val range = findRange(label) ?: return@forEach
            occupied += range
            segments += InlineActionSegment(
                id = "enemy:$enemyId",
                target = InlineActionTarget.Enemy(enemyId, label),
                start = range.first,
                end = range.last + 1,
                locked = false
            )
        }

    // Keep cleaned marker text even when its action is hidden or unresolved.
    if (segments.isEmpty() && parsedDescription == sourceDescription) return null
    segments.sortBy { it.start }
    val decorated = segments.map { segment ->
        val action = (segment.target as? InlineActionTarget.Room)?.action
        segment.copy(inspected = action?.let { inspectionReceiptKey(it, description) }
            ?.let { inspectionState[it] == true } == true)
    }
    return InlineActionPlan(description = parsedDescription, segments = decorated)
}

fun resolveRoomDescription(
    room: Room?,
    roomState: Map<String, Boolean>,
    completedMilestones: Set<String>,
    isRoomDark: Boolean
): String? {
    if (room == null) return null
    if (isRoomDark) {
        return room.descriptionDark?.takeIf { it.isNotBlank() }
            ?: "It's too dark to make out the room."
    }
    val variant = room.descriptionVariants.firstOrNull { variant ->
        variant.description.isNotBlank() &&
            variant.requiresState.all { (key, expected) -> roomState[key] == expected } &&
            variant.forbiddenState.none { (key, forbidden) -> roomState[key] == forbidden } &&
            variant.requiresMilestones.all { it in completedMilestones } &&
            variant.forbiddenMilestones.none { it in completedMilestones }
    }
    return variant?.description ?: room.description
}

fun resolveRoomBackground(
    room: Room?,
    roomState: Map<String, Boolean>,
    completedMilestones: Set<String>
): String? {
    room ?: return null
    val variant = room.descriptionVariants.firstOrNull { variant ->
        !variant.backgroundImage.isNullOrBlank() &&
            variant.requiresState.all { (key, expected) -> roomState[key] == expected } &&
            variant.forbiddenState.none { (key, forbidden) -> roomState[key] == forbidden } &&
            variant.requiresMilestones.all { it in completedMilestones } &&
            variant.forbiddenMilestones.none { it in completedMilestones }
    }
    return variant?.backgroundImage ?: room.backgroundImage
}

// Rest/cooking are service controls, including explicit markers; never duplicate them in prose.
private fun RoomAction.isInlineDescriptionAction(): Boolean =
    this !is NpcAction && this !is EnemyAction && this !is RestStopAction && !hideInline

private fun rangesOverlap(a: IntRange, b: IntRange): Boolean =
    a.first < b.last && b.first < a.last


fun resolveRoomDarkness(ui: com.example.starborn.feature.exploration.viewmodel.ExplorationUiState): Boolean {
    val room = ui.currentRoom
    var dark = when {
        ui.roomState["dark"] == true -> true
        ui.roomState["dark"] == false -> false
        ui.roomState["light_on"] == false -> true
        ui.roomState["light_on"] == true -> false
        else -> room?.dark == true
    }
    if (room?.id !in ui.darkCapableRooms || (room?.id in ui.generatorLitRooms && ui.mineGeneratorOnline)) dark = false
    return dark
}

/** Only passive inspections get read receipts. Functional controls retain their normal appearance. */
fun inspectionReceiptKey(action: RoomAction, description: String?): String? {
    val generic = action as? GenericAction ?: return null
    if (!generic.actionEvent.isNullOrBlank() || generic.type.lowercase() !in setOf("generic", "inspect")) return null
    val content = "${generic.name}\n${generic.conditionUnmetMessage}\n${description.orEmpty()}"
    val digest = java.security.MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
    return "inspection_read_" + digest.joinToString("") { "%02x".format(it) }
}
