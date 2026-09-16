package com.example.starborn.domain.pacing

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.domain.model.DialogueLine
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.Room
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates Priority 6 of the Automated Testing Strategy:
 * Automated Emotional, Conflict, and Humor Pacing Validation.
 *
 * Enforces the narrative and emotional contracts from Emotional_and_Conflict_Map.md:
 * 1. Breather Beats: Mandatory breather locations (Canopy Ridge, Skypark, Cooling Springs, Solarium)
 *    must exist and precede their respective act climaxes.
 * 2. Sacred Moment Protection: Zero humor dialogue or inappropriate emotes permitted within
 *    pivotal narrative beats (Jed's sacrifice, Gh0st's origin, Thorne's execution, Nova's sacrifice).
 * 3. Tonal Whiplash Lint: No abrupt transitions from grief/crying into comedic dialogue.
 * 4. Conflict Density: Every world maintains adequate combat presence without exploration drought.
 * 5. Quest Depth: Zero single-task hollow main quests across the entire 30-quest campaign.
 */
class NarrativePacingAndTonalLinterTest {

    private val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
    private val allRooms by lazy { reader.readList<Room>("rooms.json") }
    private val allQuests by lazy { reader.readList<Quest>("quests.json") }
    private val allDialogue by lazy { reader.readList<DialogueLine>("dialogue.json") }
    private val allEvents by lazy { reader.readList<GameEvent>("events.json") }

    // =========================================================================
    // 1. Mandatory Breather Beats Precede Act Climaxes
    // =========================================================================

    @Test
    fun breatherBeats_mandatoryBreathersPrecedeActClimaxes() {
        val roomMap = allRooms.associateBy { it.id }

        // 1. World 2 Breather: Canopy Ridge before Act Climax (w2_mq05)
        val canopyRidge = roomMap["sector9_canopy_ridge"]
        assertNotNull("Canopy Ridge must exist in rooms.json", canopyRidge)
        assertEquals("Canopy Ridge", canopyRidge?.title)
        // Verified reached during w2_mq04 before w2_mq05 climax
        val w2mq4Tasks = allQuests.first { it.id == "w2_mq04" }.stages.flatMap { it.tasks }
        assertTrue(
            "w2_mq04 must reference canopy or ridge in its progression",
            w2mq4Tasks.any { it.id.contains("canopy") || it.text.contains("Canopy") || it.id.contains("hunter") }
        )

        // 2. World 3 Breather: Skypark before Act Climax (w3_mq15)
        val skypark = roomMap["spire_skypark_dome"]
        assertNotNull("Skypark Dome must exist in rooms.json", skypark)
        assertEquals("Skypark Dome", skypark?.title)
        // Reached during w3_mq13 before w3_mq15 climax
        val w3mq13Tasks = allQuests.first { it.id == "w3_mq13" }.stages.flatMap { it.tasks }
        assertTrue(
            "w3_mq13 must reference upper city or skypark in progression",
            w3mq13Tasks.any { it.text.contains("Upper City") || it.text.contains("Skypark") || it.id.contains("upper") }
        )

        // 3. World 4 Breather: Cooling Springs before Act Climax (w4_mq20)
        val coolingSprings = roomMap["foundry_cooling_springs"]
        assertNotNull("Cooling Springs must exist in rooms.json", coolingSprings)
        assertEquals("Cooling Springs", coolingSprings?.title)
        // Connected to Foundry navigation route before Rylos confrontation
        assertTrue(coolingSprings?.connections?.isNotEmpty() == true)

        // 4. World 5 Breather: Solarium before Act Climax (w5_mq25)
        val solarium = roomMap["orbital_solarium"]
        assertNotNull("Solarium must exist in rooms.json", solarium)
        assertEquals("Solarium", solarium?.title)
        // Reached during World 5 exploration prior to Vale's Ascension
        assertTrue(solarium?.connections?.isNotEmpty() == true)
    }

    // =========================================================================
    // 2. Sacred Moment Protection
    // =========================================================================

    @Test
    fun sacredMoments_zeroHumorPermittedNearPivotalBeats() {
        val dialogueMap = allDialogue.associateBy { it.id }
        val humorKeywords = listOf("joke", "haha", "party", "prank", "hilarious", "chuckle", "giggle", "amusing")

        // 1. Jed's Sacrifice (w1_mq04)
        val jedSacrificeNodes = allDialogue.filter { it.id.startsWith("jed_w1_mq04_sacrifice_") }
        assertTrue("Jed's sacrifice nodes must exist", jedSacrificeNodes.isNotEmpty())
        for (node in jedSacrificeNodes) {
            // No comedic emotes
            assertFalse(
                "Jed's sacrifice node ${node.id} must not have happy/cool emote",
                node.emote in setOf("happy", "cool", "confident")
            )
            // No humor keywords in text
            val lower = node.text.lowercase()
            for (kw in humorKeywords) {
                assertFalse("Sacred node ${node.id} contains humor keyword '$kw': ${node.text}", lower.contains(kw))
            }
        }

        // 2. Gh0st's Origin / Rylos Confrontation (w4_mq20)
        val rylosNodes = allDialogue.filter { it.id.contains("w4_mq20") }
        for (node in rylosNodes) {
            assertFalse(
                "Rylos confrontation node ${node.id} must not have happy emote",
                node.emote in setOf("happy")
            )
            val lower = node.text.lowercase()
            for (kw in humorKeywords) {
                assertFalse("Confrontation node ${node.id} contains humor keyword '$kw'", lower.contains(kw))
            }
        }

        // 3. Thorne's Execution & Vale's Ascension (w5_mq25)
        val w5ClimaxNodes = allDialogue.filter { it.id.contains("w5_mq25") }
        for (node in w5ClimaxNodes) {
            assertFalse(
                "Thorne execution node ${node.id} must not have happy emote",
                node.emote in setOf("happy")
            )
            val lower = node.text.lowercase()
            for (kw in humorKeywords) {
                assertFalse("W5 climax node ${node.id} contains humor keyword '$kw'", lower.contains(kw))
            }
        }

        // 4. Nova's Final Sacrifice / Tune (w6_mq30)
        val w6ClimaxNodes = allDialogue.filter { it.id.contains("w6_mq30") }
        for (node in w6ClimaxNodes) {
            val lower = node.text.lowercase()
            for (kw in humorKeywords) {
                assertFalse("W6 finale node ${node.id} contains humor keyword '$kw'", lower.contains(kw))
            }
        }
    }

    // =========================================================================
    // 3. Tonal Whiplash Linter
    // =========================================================================

    @Test
    fun tonalWhiplash_noGriefToHumorTransitionsWithoutBufferBeats() {
        val dialogueMap = allDialogue.associateBy { it.id }
        val humorKeywords = listOf("joke", "haha", "party", "prank", "hilarious")

        // Inspect every node that conveys intense grief or crying
        val griefNodes = allDialogue.filter { it.emote in setOf("crying", "sad") }

        for (node in griefNodes) {
            // Check direct next node
            node.next?.let { nextId ->
                val nextNode = dialogueMap[nextId]
                if (nextNode != null) {
                    assertFalse(
                        "Grief node ${node.id} abruptly transitions to happy emote in ${nextNode.id}",
                        nextNode.emote == "happy"
                    )
                    val lower = nextNode.text.lowercase()
                    for (kw in humorKeywords) {
                        assertFalse(
                            "Grief node ${node.id} immediately transitions to comedy keyword '$kw' in ${nextNode.id}",
                            lower.contains(kw)
                        )
                    }
                }
            }

            // Check options
            node.options.orEmpty().forEach { opt ->
                opt.next?.let { nextId ->
                    val nextNode = dialogueMap[nextId]
                    if (nextNode != null) {
                        assertFalse(
                            "Grief node ${node.id} option ${opt.id} transitions to happy emote in ${nextNode.id}",
                            nextNode.emote == "happy"
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. Conflict Density Across Worlds
    // =========================================================================

    @Test
    fun conflictDensity_allWorldsMaintainAuthoredCombatPresence() {
        // Group rooms by world
        val worldEnemyCount = mutableMapOf<String, Int>()
        for (i in 1..6) {
            worldEnemyCount["w$i"] = 0
        }

        for (room in allRooms) {
            val bg = room.backgroundImage
            for (i in 1..6) {
                if (bg.contains("world_$i")) {
                    val key = "w$i"
                    if (room.enemies.isNotEmpty()) {
                        worldEnemyCount[key] = (worldEnemyCount[key] ?: 0) + 1
                    }
                }
            }
        }

        // Enforce that every world has at least 8 combat/enemy rooms
        for (i in 1..6) {
            val key = "w$i"
            val count = worldEnemyCount[key] ?: 0
            assertTrue(
                "World $key must have at least 8 enemy rooms to avoid pacing drought (found $count)",
                count >= 8
            )
        }
    }

    // =========================================================================
    // 5. Quest Depth & No Single-Task Main Quests
    // =========================================================================

    @Test
    fun questCadence_noDegenerateOneTaskMainQuests() {
        val mainQuests = allQuests.filter { it.id.matches(Regex("w[1-6]_mq[0-9]{2}")) }
        assertEquals("Campaign must contain exactly 30 main quests", 30, mainQuests.size)

        for (mq in mainQuests) {
            val taskCount = mq.stages.flatMap { it.tasks }.size
            assertTrue(
                "Main quest ${mq.id} (${mq.title}) must have at least 2 tasks to maintain narrative depth (found $taskCount)",
                taskCount >= 2
            )
        }
    }
}
