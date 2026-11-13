package com.example.starborn.desktop

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.Room
import com.example.starborn.feature.exploration.presentation.InlineActionTarget
import com.example.starborn.feature.exploration.presentation.buildInlineActionPlan
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Verify documented text taps through the same matching/marker logic as the game UI. */
class World56WalkthroughReachabilityTest {
    private val root = if (File("app/src/main/assets").exists()) File(".") else File("..")
    private val rooms = AssetJsonReader(DesktopAssetProvider(listOf(File(root, "app/src/main/assets"))), MoshiProvider.instance)
        .readList<Room>("rooms.json")

    private fun targets(room: Room, description: String): Set<String> {
        // Visibility gates govern when an action is ready; this check governs whether its
        // authored label can be reached at all. Service pills deliberately aren't inline.
        val inline = room.actions.filter {
            it["type"] !in setOf("rest_stop", "shop", "fishing", "tinkering") &&
                it["hide_inline"] != true && it["travel_label"] == null
        }.map { GenericAction(name = it["name"] as String, type = "inspect") }
        return buildInlineActionPlan(description, inline, emptyMap(), room)?.segments.orEmpty()
            .mapNotNull { (it.target as? InlineActionTarget.Room)?.action?.name }.toSet()
    }

    @Test fun exportRenderedTargetsForGuideReview() {
        val result = rooms.filter { it.id.startsWith("orbital_") || it.id.startsWith("deep_") || it.id.startsWith("source_") }
            .associate { room -> room.id to (listOf(room.description) + room.descriptionVariants.map { it.description })
                .flatMap { targets(room, it) }.distinct() }
        File(System.getProperty("java.io.tmpdir"), "starborn-world56-rendered-targets.json")
            .writeText(MoshiProvider.instance.adapter(Map::class.java).toJson(result))
        assertEquals(146, result.size)
    }

    @Test fun everyDocumentedInspectionAndPrimaryTextControlHasAnActualLink() {
        val inspection = Regex("""(?:^\d+\. |(?<=\. ))At \*\*(.+?)\*\*, inspect (.+?)\.""")
        val control = Regex("""(?:^\d+\. |(?<=\. ))At \*\*(.+?)\*\*, (?:use|open) \*\*(.+?)\*\*""")
        val bold = Regex("""\*\*(.+?)\*\*""")
        var checked = 0
        for (world in listOf(5, 6)) {
            val worldRooms = rooms.filter { if (world == 5) it.id.startsWith("orbital_") || it.id.startsWith("deep_") else it.id.startsWith("source_") }
                .associateBy { it.title }
            val guide = File(root, "docs/playtest/walkthroughs/WORLD_${world}_COMPLETIONIST_WALKTHROUGH.md")
            guide.readLines().forEach { line ->
                val inspectMatch = inspection.find(line)
                val useMatch = control.find(line)
                val title = inspectMatch?.groupValues?.get(1) ?: useMatch?.groupValues?.get(1) ?: return@forEach
                val labels = inspectMatch?.let { bold.findAll(it.groupValues[2]).map { it.groupValues[1] }.toList() }
                    ?: listOf(useMatch!!.groupValues[2])
                val room = requireNotNull(worldRooms[title]) { "Unknown room: $line" }
                val descriptions = listOf(room.description) + room.descriptionVariants.map { it.description }
                for (label in labels) {
                    if (label in setOf("Rest", "Cook", "Fish")) continue
                    val raw = room.actions.singleOrNull { (it["name"] as? String)?.equals(label, true) == true }
                    assertNotNull("Unknown action: $line", raw)
                    val available = descriptions.map { targets(room, it).any { name -> name.equals(label, true) } }
                    if (inspectMatch != null) assertTrue("Inspection isn't linked in every room state: $line", available.all { it })
                    else assertTrue("Control isn't linked in any room state: $line", available.any { it })
                    checked++
                }
            }
        }
        assertTrue("Both guides' interactions must be checked", checked > 140)
    }
}
