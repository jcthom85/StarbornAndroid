package com.example.starborn.domain.playtest.audit

import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionState
import java.io.File

/**
 * Captures an event stream during an automated campaign playthrough and
 * performs deterministic quality, design, and narrative audits.
 */
class PlaythroughNarrativeAuditor(
    val worldId: String,
    val worldTitle: String,
    private val allRooms: Map<String, Room>,
    private val allQuests: Map<String, Quest>,
    private val allEvents: Map<String, GameEvent>,
    private val allDialogue: Map<String, DialogueLine>
) {

    enum class IssueSeverity {
        BLOCKER,
        WARNING,
        POLISH_NOTE
    }

    data class AuditIssue(
        val severity: IssueSeverity,
        val category: String, // e.g., "Narrative Continuity", "Confusing Text", "Room/Prose Mismatch", "State Discrepancy"
        val locationOrContext: String,
        val description: String,
        val suggestion: String? = null
    )

    data class TraceEntry(
        val stepIndex: Int,
        val stepType: String, // "NAVIGATION", "ACTION", "DIALOGUE", "COMBAT", "CRAFTING"
        val target: String,
        val details: String,
        val activeQuest: String?,
        val activeStage: String?,
        val party: List<String>,
        val currentRoom: String
    )

    private val traceLog = mutableListOf<TraceEntry>()
    private val issues = mutableListOf<AuditIssue>()
    private var stepCounter = 0

    fun recordNavigation(fromRoom: String, toRoom: String, session: GameSessionState) {
        stepCounter++
        val roomObj = allRooms[toRoom]
        val details = buildString {
            append("Entered room: ${roomObj?.title ?: toRoom}")
            if (roomObj?.description != null) {
                append(" | Prose snippet: \"${roomObj.description.take(120)}...\"")
            }
        }
        traceLog += TraceEntry(
            stepIndex = stepCounter,
            stepType = "NAVIGATION",
            target = toRoom,
            details = details,
            activeQuest = session.trackedQuestId ?: session.activeQuests.firstOrNull(),
            activeStage = session.trackedQuestId?.let { session.questStageById[it] },
            party = session.partyMembers,
            currentRoom = toRoom
        )

        // Audit check: room existence & description quality
        if (roomObj == null) {
            recordIssue(
                IssueSeverity.BLOCKER,
                "Room/Prose Mismatch",
                toRoom,
                "Navigated to non-existent room '$toRoom' in room catalog."
            )
        } else {
            val desc = roomObj.description.orEmpty()
            if (desc.isBlank()) {
                recordIssue(
                    IssueSeverity.WARNING,
                    "Confusing Text",
                    toRoom,
                    "Room '${roomObj.title}' ($toRoom) has an empty or blank description."
                )
            }
            if (desc.contains("TODO", ignoreCase = true) || desc.contains("TEMP", ignoreCase = true)) {
                recordIssue(
                    IssueSeverity.WARNING,
                    "Confusing Text",
                    toRoom,
                    "Room '${roomObj.title}' contains developer placeholder text ('TODO'/'TEMP')."
                )
            }
        }
    }

    fun recordAction(actionId: String, payload: String?, session: GameSessionState) {
        stepCounter++
        val eventObj = allEvents[actionId]
        val desc = eventObj?.description ?: "Action triggered: $actionId (payload: $payload)"
        traceLog += TraceEntry(
            stepIndex = stepCounter,
            stepType = "ACTION",
            target = actionId,
            details = desc,
            activeQuest = session.trackedQuestId ?: session.activeQuests.firstOrNull(),
            activeStage = session.trackedQuestId?.let { session.questStageById[it] },
            party = session.partyMembers,
            currentRoom = session.roomId ?: "unknown"
        )
    }

    fun recordDialogueExchange(speaker: String, text: String, choiceSelected: String?, session: GameSessionState) {
        stepCounter++
        val details = buildString {
            append("$speaker: \"$text\"")
            if (choiceSelected != null) {
                append(" -> [Player Chose: $choiceSelected]")
            }
        }
        traceLog += TraceEntry(
            stepIndex = stepCounter,
            stepType = "DIALOGUE",
            target = speaker,
            details = details,
            activeQuest = session.trackedQuestId ?: session.activeQuests.firstOrNull(),
            activeStage = session.trackedQuestId?.let { session.questStageById[it] },
            party = session.partyMembers,
            currentRoom = session.roomId ?: "unknown"
        )

        // Text & continuity heuristics
        if (text.isBlank()) {
            recordIssue(
                IssueSeverity.WARNING,
                "Confusing Text",
                "Speaker: $speaker",
                "Empty dialogue line delivered by $speaker."
            )
        }
        val placeholderRegex = Regex("""\b(TODO|TEMP|TBD|FIXME|PLACEHOLDER)\b|\[(draft|placeholder|temp)\]""", RegexOption.IGNORE_CASE)
        if (placeholderRegex.containsMatchIn(text)) {
            recordIssue(
                IssueSeverity.WARNING,
                "Confusing Text",
                "Speaker: $speaker",
                "Dialogue contains draft placeholder tags: \"$text\""
            )
        }
        // Formatting issues like unescaped variables or dangling brackets
        if (text.contains("{") && text.contains("}") && (text.contains("{player") || text.contains("{npc"))) {
            recordIssue(
                IssueSeverity.WARNING,
                "Confusing Text",
                "Speaker: $speaker",
                "Uninterpolated template variable detected: \"$text\""
            )
        }
    }

    fun recordCombat(enemyIds: List<String>, roomId: String, session: GameSessionState) {
        stepCounter++
        traceLog += TraceEntry(
            stepIndex = stepCounter,
            stepType = "COMBAT",
            target = enemyIds.joinToString(", "),
            details = "Victory against [${enemyIds.joinToString(", ")}] in $roomId",
            activeQuest = session.trackedQuestId ?: session.activeQuests.firstOrNull(),
            activeStage = session.trackedQuestId?.let { session.questStageById[it] },
            party = session.partyMembers,
            currentRoom = roomId
        )
    }

    fun recordCraft(recipeId: String, resultItemId: String, session: GameSessionState) {
        stepCounter++
        traceLog += TraceEntry(
            stepIndex = stepCounter,
            stepType = "CRAFTING",
            target = recipeId,
            details = "Crafted $resultItemId using recipe $recipeId",
            activeQuest = session.trackedQuestId ?: session.activeQuests.firstOrNull(),
            activeStage = session.trackedQuestId?.let { session.questStageById[it] },
            party = session.partyMembers,
            currentRoom = session.roomId ?: "unknown"
        )
    }

    fun recordIssue(severity: IssueSeverity, category: String, locationOrContext: String, description: String, suggestion: String? = null) {
        issues += AuditIssue(severity, category, locationOrContext, description, suggestion)
    }

    fun generateReport(): String = buildString {
        appendLine("# Playtest & Narrative Audit Report: $worldTitle ($worldId)")
        appendLine()
        appendLine("Generated by `PlaythroughNarrativeAuditor` during automated headless campaign traversal.")
        appendLine()
        appendLine("## 1. Executive Summary")
        appendLine("- **Total Interactive Steps Taken:** ${traceLog.size}")
        appendLine("- **Total Issues Flagged:** ${issues.size}")
        val blockers = issues.count { it.severity == IssueSeverity.BLOCKER }
        val warnings = issues.count { it.severity == IssueSeverity.WARNING }
        val polish = issues.count { it.severity == IssueSeverity.POLISH_NOTE }
        appendLine("  - **Blockers:** $blockers")
        appendLine("  - **Warnings (Design / Text / Continuity):** $warnings")
        appendLine("  - **Polish Opportunities:** $polish")
        appendLine()

        appendLine("## 2. Issues & Findings")
        if (issues.isEmpty()) {
            appendLine("No blocking or warning-level issues detected during this run.")
        } else {
            appendLine("| Severity | Category | Location / Context | Description | Recommendation |")
            appendLine("|---|---|---|---|---|")
            issues.forEach { issue ->
                appendLine("| ${issue.severity} | ${issue.category} | ${issue.locationOrContext} | ${issue.description.replace("|", "\\|")} | ${issue.suggestion?.replace("|", "\\|") ?: "-"} |")
            }
        }
        appendLine()

        appendLine("## 3. Step-by-Step Playthrough Narrative Transcript")
        appendLine()
        appendLine("This transcript reflects the exact player experience, including dialogue, room travel, and quest updates in chronological order:")
        appendLine()
        traceLog.forEach { entry ->
            val questContext = if (entry.activeQuest != null) "[Quest: ${entry.activeQuest} (${entry.activeStage.orEmpty()})]" else ""
            when (entry.stepType) {
                "NAVIGATION" -> appendLine("**[Step ${entry.stepIndex} - Travel]** ${entry.details} $questContext")
                "ACTION" -> appendLine("- **[Action]** `${entry.target}`: ${entry.details} $questContext")
                "DIALOGUE" -> appendLine("> **[Dialogue]** ${entry.details}")
                "COMBAT" -> appendLine("**[Combat Encounter]** ${entry.details}")
                "CRAFTING" -> appendLine("**[Crafting]** ${entry.details}")
                else -> appendLine("- **[${entry.stepType}]** ${entry.details}")
            }
        }
    }

    fun exportReportToFile(destinationFile: File) {
        destinationFile.parentFile?.mkdirs()
        destinationFile.writeText(generateReport())
    }
}
