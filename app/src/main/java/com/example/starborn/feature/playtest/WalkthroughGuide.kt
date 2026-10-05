package com.example.starborn.feature.playtest

import android.content.Context
import java.security.MessageDigest

internal data class GuideBlock(val id: String, val text: String, val heading: Int = 0, val step: Int? = null)
internal data class GuideSection(val title: String, val blocks: List<GuideBlock>)
internal data class WalkthroughGuide(val world: Int, val revision: String, val sections: List<GuideSection>) {
    val steps get() = sections.sumOf { section -> section.blocks.count { it.step != null } }
}

/** Small reader for our authored guide dialect; no HTML or remote content. */
internal fun parseWalkthrough(world: Int, markdown: String): WalkthroughGuide {
    val sections = mutableListOf<GuideSection>()
    var title = "Before you start"
    val blocks = mutableListOf<GuideBlock>()
    val paragraph = StringBuilder()
    var ordinal = 0
    fun flush() {
        if (paragraph.isNotBlank()) {
            val text = paragraph.toString().trim()
            blocks += GuideBlock("block-${ordinal++}", text, step = Regex("^(\\d+)\\. ").find(text)?.groupValues?.get(1)?.toIntOrNull())
            paragraph.clear()
        }
    }
    markdown.lineSequence().forEach { raw ->
        val line = raw.trimEnd()
        when {
            line.startsWith("# ") -> Unit
            line.startsWith("## ") -> {
                flush()
                if (blocks.isNotEmpty()) sections += GuideSection(title, blocks.toList())
                blocks.clear()
                title = line.removePrefix("## ")
            }
            line.isBlank() -> flush()
            line.startsWith("### ") || line.startsWith("#### ") -> {
                flush()
                blocks += GuideBlock("block-${ordinal++}", line.trimStart('#', ' '), if (line.startsWith("####")) 4 else 3)
            }
            line.startsWith("- ") || Regex("^\\d+\\. ").containsMatchIn(line) -> {
                flush(); paragraph.append(line); flush()
            }
            else -> { if (paragraph.isNotEmpty()) paragraph.append(' '); paragraph.append(line) }
        }
    }
    flush()
    if (blocks.isNotEmpty()) sections += GuideSection(title, blocks.toList())
    val revision = MessageDigest.getInstance("SHA-256").digest(markdown.toByteArray()).take(8).joinToString("") { "%02x".format(it) }
    return WalkthroughGuide(world, revision, sections)
}

internal fun loadWalkthrough(context: Context, world: Int): WalkthroughGuide =
    context.assets.open("playtest_guides/WORLD_${world}_COMPLETIONIST_WALKTHROUGH.md")
        .bufferedReader(Charsets.UTF_8).use { parseWalkthrough(world, it.readText()) }

// Quest routes include preparation and return visits, not just the journal appendix.
internal fun WalkthroughGuide.questSections(questId: String?): Set<Int> {
    val chapters = when (questId) {
        "w1_mq01" -> listOf(1, 2)
        "w1_mq02" -> listOf(5, 6)
        "w1_mq03" -> listOf(8, 9, 10)
        "w1_mq04" -> listOf(10)
        "w1_mq05" -> listOf(11)
        "w1_sq01" -> listOf(3)
        "w1_sq02" -> listOf(4)
        "w1_sq03" -> listOf(6)
        "w1_sq04" -> listOf(7)
        "w1_sq05" -> listOf(8)
        "w2_mq01" -> listOf(2)
        "w2_mq02" -> listOf(3, 4, 5, 6)
        "w2_mq03" -> listOf(7)
        "w2_mq04" -> listOf(10)
        "w2_mq05" -> listOf(12, 13, 14)
        "w2_sq01" -> listOf(2, 4, 5, 7)
        "w2_sq02" -> listOf(5)
        "w2_sq03" -> listOf(9)
        "w2_sq04" -> listOf(8)
        "w2_sq05" -> listOf(11)
        else -> emptyList()
    }
    if (questId?.startsWith("w${world}_") != true) return emptySet()
    return sections.indices.filter { i -> chapters.any { sections[i].title.startsWith("$it. ") } }.toSet()
}
