package com.example.starborn.domain.world

import com.example.starborn.core.MoshiProvider
import com.example.starborn.core.platform.DesktopAssetProvider
import com.example.starborn.data.assets.AssetJsonReader
import com.example.starborn.data.assets.CinematicSceneAsset
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.model.Room
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.feature.exploration.presentation.resolveRoomDescription
import com.example.starborn.feature.exploration.viewmodel.narrativeActionVisible
import com.squareup.moshi.Types
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LateGameLandmarksAndCinematicsTest {

    @Test
    fun `six late-game landmarks expose inline actions, milestone variants, and one-shot rewards`() {
        val reader = AssetJsonReader(DesktopAssetProvider(), MoshiProvider.instance)
        val rooms = reader.readList<Room>("rooms.json").associateBy { it.id }
        val events = reader.readList<GameEvent>("events.json")

        data class Spec(
            val roomId: String,
            val inspectAction: String,
            val solvedAction: String,
            val milestone: String,
            val rewardItem: String,
        )

        val specs = listOf(
            Spec("foundry_conditioning_scale_03", "w4_landmark_refusal_latch_inspect", "w4_landmark_refusal_solved", "ms_w4_landmark_refusal_trench", "cyano_coolant_injector"),
            Spec("foundry_forge_scale_04", "w4_landmark_slag_chapel_inspect", "w4_landmark_slag_chapel_solved", "ms_w4_landmark_slag_chapel", "magma_induction_core_mod"),
            Spec("orbital_grand_concourse_scale_04", "w5_landmark_berth_desk_inspect", "w5_landmark_berth_solved", "ms_w5_landmark_quarantined_berth", "zero_g_gyro_stabilizer_mod"),
            Spec("orbital_solarium_scale_03", "w5_landmark_birdsong_rig_inspect", "w5_landmark_birdsong_solved", "ms_w5_landmark_birdsong_rig", "orbital_deflector_mod"),
            Spec("source_memory_bridge_node_scale_02", "w6_landmark_counter_mugs_inspect", "w6_landmark_counter_solved", "ms_w6_landmark_memory_counter", "source_astral_feast"),
            Spec("source_spire_thought_node_scale_02", "w6_landmark_tuning_forks_inspect", "w6_landmark_amphitheater_solved", "ms_w6_landmark_unruly_amphitheater", "astral_weave_mod"),
        )

        specs.forEach { spec ->
            val room = rooms.getValue(spec.roomId)
            val bgFile = File("../world_assets/src/main/assets/${room.backgroundImage}")
            assertTrue("Missing landmark art for ${spec.roomId}: ${room.backgroundImage}", bgFile.isFile && bgFile.length() > 100_000L)

            val baseProse = requireNotNull(resolveRoomDescription(room, emptyMap(), emptySet(), false))
            assertTrue("${spec.roomId} base prose exceeds 45 words", baseProse.split(Regex("\\s+")).size <= 45)

            val triggerAction = room.actions.single { it["action_event"] == spec.inspectAction }
            val actionLabel = triggerAction["name"] as String
            assertTrue("${spec.roomId} prose must name '$actionLabel'", baseProse.contains(actionLabel, ignoreCase = true))
            assertTrue(narrativeActionVisible(triggerAction, emptyMap(), emptyMap(), emptySet()))

            // Execute one-shot solved event
            val store = GameSessionStore()
            val addedItems = mutableListOf<Pair<String, Int>>()
            var xpAwarded = 0
            val manager = EventManager(
                events = events,
                sessionStore = store,
                eventHooks = EventHooks(
                    onGiveItem = { id, qty -> addedItems += id to qty },
                    onGiveXp = { xp -> xpAwarded += xp },
                )
            )
            manager.handleTrigger("player_action", EventPayload.Action(spec.solvedAction))
            assertTrue("${spec.milestone} should be set", spec.milestone in store.state.value.completedMilestones)
            assertTrue("Expected reward ${spec.rewardItem} in $addedItems", addedItems.any { it.first == spec.rewardItem && it.second == 1 })
            assertTrue("Expected positive XP reward for ${spec.roomId}", xpAwarded >= 180)

            // Post-solve variant & hidden one-shot trigger
            val solvedProse = requireNotNull(resolveRoomDescription(room, emptyMap(), setOf(spec.milestone), false))
            assertTrue("${spec.roomId} solved prose exceeds 45 words", solvedProse.split(Regex("\\s+")).size <= 45)
            assertFalse("${spec.roomId} variant should differ from base prose", solvedProse == baseProse)
            assertFalse(narrativeActionVisible(triggerAction, emptyMap(), emptyMap(), setOf(spec.milestone)))

            // Non-repeatable solved event must not double-grant
            manager.handleTrigger("player_action", EventPayload.Action(spec.solvedAction))
            assertEquals(1, addedItems.count { it.first == spec.rewardItem })
        }
    }

    @Test
    fun `restyled late-game cinematics ship as WebP while intro prologue remains untouched`() {
        val type = Types.newParameterizedType(List::class.java, CinematicSceneAsset::class.java)
        val scenes = MoshiProvider.instance.adapter<List<CinematicSceneAsset>>(type)
            .fromJson(File("src/main/assets/cinematics.json").readText())
            .orEmpty()
            .associateBy { it.id }

        val restyledPanels = listOf(
            "images/cinematics/w4_coup_display_v2.webp",
            "images/cinematics/w5_stasis_lunge_v2.webp",
            "images/cinematics/w5_vale_soloist_tear_v1.webp",
            "images/cinematics/w5_tear_nova_wakes_v1.webp",
        )
        val allImages = scenes.values.flatMap { it.steps.orEmpty().filterNotNull().mapNotNull { s -> s.imagePath } }.toSet()
        restyledPanels.forEach { rel ->
            assertTrue("Expected $rel wired in cinematics.json", rel in allImages)
            val f = File("../world_assets/src/main/assets/$rel")
            assertTrue("Missing restyled cinematic $rel", f.isFile && f.length() > 100_000L)
        }

        val intro = scenes.getValue("intro_prologue")
        assertTrue(
            "intro_prologue must preserve contextual orion_portrait.png untouched",
            intro.steps.orEmpty().filterNotNull().any { it.portrait == "images/characters/orion_portrait.png" }
        )
    }
}
