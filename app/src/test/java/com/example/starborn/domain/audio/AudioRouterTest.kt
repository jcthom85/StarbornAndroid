package com.example.starborn.domain.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRouterTest {

    @Test fun temporaryActivitiesRestoreExplorationWithoutRestartingAndRespectNewOwners() {
        val router = AudioRouter(AudioBindings(music = mapOf("hub" to "exploration", "other" to "next_world")))
        router.commandsForRoom("hub", "room")
        for (cue in listOf("music_shop_cozy", "music_tinkering_focus", "music_cooking_kitchen", "music_fishing_ambient", "music_arcade_cabinet")) {
            val temporary = router.temporaryMusic(cue)
            assertTrue(temporary.enter.filterIsInstance<AudioCommand.Play>().any { it.cueId == cue })
            val resumed = temporary.restore().filterIsInstance<AudioCommand.Play>().single()
            assertEquals("exploration", resumed.cueId)
            assertEquals(false, resumed.restartFromBeginning)
            assertTrue(resumed.fadeMs > 0)
            assertTrue(temporary.restore().isEmpty())
        }
        val temporary = router.temporaryMusic("music_shop_cozy")
        router.commandsForRoom("other", "other_room")
        assertTrue("A disposed activity cannot replace a newer location's music", temporary.restore().isEmpty())
    }

    @Test
    fun newFightsExplicitlyRestartEvenWhenTheSameTrackIsAlreadySelected() {
        val router = AudioRouter(AudioBindings(music = mapOf("hub" to "exploration")))
        val explore = router.commandsForRoom("hub", "room").filterIsInstance<AudioCommand.Play>().single()
        assertEquals(false, explore.restartFromBeginning)
        assertEquals(emptyList<AudioCommand>(), router.commandsForRoom("hub", "next_room"))
        repeat(2) {
            val battle = router.commandsForLayerOverride(AudioCueType.MUSIC, "combat", loop = true,
                restartFromBeginning = true).filterIsInstance<AudioCommand.Play>().single()
            assertEquals(true, battle.restartFromBeginning)
        }
        val returned = router.commandsForRoom("hub", "room").filterIsInstance<AudioCommand.Play>().single()
        assertEquals("exploration", returned.cueId)
        assertEquals(false, returned.restartFromBeginning)
    }

    @Test
    fun commandsForRoomSwitchesMusicAndAmbientLayers() {
        val bindings = AudioBindings(
            music = mapOf(
                "hub_alpha" to "theme_alpha",
                "hub_bravo" to "theme_bravo"
            ),
            ambience = mapOf(
                "room_one" to "wind_gentle",
                "room_two" to "wind_fierce"
            )
        )
        val router = AudioRouter(bindings)

        val initialCommands = router.commandsForRoom(
            hubId = "hub_alpha",
            roomId = "room_one"
        )
        assertTrue(initialCommands.any { it is AudioCommand.Play && it.cueId == "theme_alpha" })
        assertTrue(initialCommands.any { it is AudioCommand.Play && it.cueId == "wind_gentle" })

        val redundantCommands = router.commandsForRoom(
            hubId = "hub_alpha",
            roomId = "room_one"
        )
        assertTrue("No new commands should be issued when nothing changes", redundantCommands.isEmpty())

        val switchedCommands = router.commandsForRoom(
            hubId = "hub_bravo",
            roomId = "room_two"
        )
        assertTrue(switchedCommands.any { it is AudioCommand.Stop && it.cueId == "theme_alpha" })
        assertTrue(switchedCommands.any { it is AudioCommand.Stop && it.cueId == "wind_gentle" })
        assertTrue(switchedCommands.any { it is AudioCommand.Play && it.cueId == "theme_bravo" })
        assertTrue(switchedCommands.any { it is AudioCommand.Play && it.cueId == "wind_fierce" })
    }

    @Test
    fun suppressedRoomStopsATrackACinematicStarted() {
        // The cold open's title theme is started on the music layer by the last cinematic step.
        // When the scene ends, the room has to be able to take the layer back and silence it --
        // Nova's bunk stays quiet until the light is on. Before this was wired up, room audio was
        // only applied once during load, before the cinematic ran, so the theme played straight
        // through the wake-up fade and into the dark bunk.
        val bindings = AudioBindings(
            music = mapOf("hub_1_homestead" to "music_w1_homestead_explore"),
            ambience = mapOf("pit_nova_bunk" to "amb_bunkroom_hum")
        )
        val router = AudioRouter(bindings)
        router.commandsForLayerOverride(AudioCueType.MUSIC, cueId = "music_title_theme")

        val darkBunk = router.commandsForRoom(
            hubId = "hub_1_homestead",
            roomId = "pit_nova_bunk",
            suppressMusic = true
        )
        assertTrue(
            "The cinematic's theme must stop when the room suppresses music",
            darkBunk.any { it is AudioCommand.Stop && it.cueId == "music_title_theme" }
        )
        assertTrue(
            "Nothing should start on the music layer while the bunk is dark",
            darkBunk.none { it is AudioCommand.Play && it.cueId == "music_w1_homestead_explore" }
        )
        assertTrue(
            "Room ambience still plays while music is suppressed",
            darkBunk.any { it is AudioCommand.Play && it.cueId == "amb_bunkroom_hum" }
        )

        val lightOn = router.commandsForRoom(
            hubId = "hub_1_homestead",
            roomId = "pit_nova_bunk",
            suppressMusic = false
        )
        assertTrue(
            "Turning the light on hands the music layer to the hub track",
            lightOn.any { it is AudioCommand.Play && it.cueId == "music_w1_homestead_explore" }
        )
    }

    @Test
    fun commandsForLayerOverrideStopsMusicImmediately() {
        val bindings = AudioBindings(
            music = mapOf("hub_alpha" to "theme_alpha")
        )
        val router = AudioRouter(bindings)
        router.commandsForRoom(hubId = "hub_alpha", roomId = null)

        val stopCommands = router.commandsForLayerOverride(
            layer = AudioCueType.MUSIC,
            stop = true
        )

        assertEquals(1, stopCommands.size)
        val stop = stopCommands.first() as AudioCommand.Stop
        assertEquals(AudioCueType.MUSIC, stop.type)
        assertEquals("theme_alpha", stop.cueId)
    }

    @Test
    fun voiceoverPlanDoesNotDuckLayersByDefault() {
        val router = AudioRouter(
            AudioBindings(
                music = mapOf("hub_alpha" to "theme_alpha"),
                ambience = mapOf("room_one" to "wind_gentle")
            ),
            AudioCatalog(cues = listOf(AudioCueMetadata(id = "intro_vo", category = "voice")))
        )
        router.commandsForRoom(hubId = "hub_alpha", roomId = "room_one")

        val plan = router.voiceoverPlan("intro_vo")

        requireNotNull(plan)
        assertTrue(plan.commands.none { it is AudioCommand.Duck })
        assertTrue(plan.commands.any { it is AudioCommand.Play && it.type == AudioCueType.VOICE })
    }

    @Test
    fun voiceoverPlanCanDuckLayersWhenRequested() {
        val router = AudioRouter(
            AudioBindings(
                music = mapOf("hub_alpha" to "theme_alpha"),
                ambience = mapOf("room_one" to "wind_gentle")
            ),
            AudioCatalog(cues = listOf(AudioCueMetadata(id = "intro_vo", category = "voice")))
        )
        router.commandsForRoom(hubId = "hub_alpha", roomId = "room_one")

        val plan = router.voiceoverPlan("intro_vo", duckLayers = true)

        requireNotNull(plan)
        assertEquals(2, plan.commands.count { it is AudioCommand.Duck })
        assertTrue(plan.commands.any { it is AudioCommand.Play && it.type == AudioCueType.VOICE })
    }

    @Test
    fun resonanceBattleEventsRouteToBattleCues() {
        val router = AudioRouter(
            AudioBindings(
                battle = mapOf(
                    "weakness_resolve" to "battle_weakness_resolve",
                    "stability_break" to "battle_stability_shatter",
                    "erosion_warning" to "battle_erosion_warning"
                )
            )
        )

        val weakness = router.commandsForBattle("weakness_resolve")
        val stability = router.commandsForBattle("stability_break")
        val erosion = router.commandsForBattle("erosion_warning")

        assertTrue(weakness.any { it is AudioCommand.Play && it.type == AudioCueType.BATTLE && it.cueId == "battle_weakness_resolve" })
        assertTrue(stability.any { it is AudioCommand.Play && it.type == AudioCueType.BATTLE && it.cueId == "battle_stability_shatter" })
        assertTrue(erosion.any { it is AudioCommand.Play && it.type == AudioCueType.BATTLE && it.cueId == "battle_erosion_warning" })
    }

    @Test
    fun commandsForRoomResolvesRoomNodeHubWorldHierarchyAndLoopPoints() {
        val bindings = AudioBindings(
            music = mapOf("hub_1" to "hub_theme", "world_1" to "world_theme"),
            nodeMusic = mapOf("deep_mine" to "node_theme"),
            roomMusic = mapOf("astra_bridge" to "bridge_theme")
        )
        val catalog = AudioCatalog(
            tracks = listOf(
                AudioTrackMetadata(id = "hub_theme", loopStartMs = 4000L, loopEndMs = 64000L),
                AudioTrackMetadata(id = "node_theme", loopStartMs = 1500L),
                AudioTrackMetadata(id = "bridge_theme")
            )
        )
        val router = AudioRouter(bindings, catalog)

        val hubCmd = router.commandsForRoom(
            hubId = "hub_1",
            roomId = "pit_mess",
            hubNodeId = "pit",
            worldId = "world_1"
        ).filterIsInstance<AudioCommand.Play>().single()
        assertEquals("hub_theme", hubCmd.cueId)
        assertEquals(4000L, hubCmd.loopStartMs)
        assertEquals(64000L, hubCmd.loopEndMs)

        val nodeCmd = router.commandsForRoom(
            hubId = "hub_1",
            roomId = "mine_alpha",
            hubNodeId = "deep_mine",
            worldId = "world_1"
        ).filterIsInstance<AudioCommand.Play>().single()
        assertEquals("node_theme", nodeCmd.cueId)
        assertEquals(1500L, nodeCmd.loopStartMs)

        val roomCmd = router.commandsForRoom(
            hubId = "hub_1",
            roomId = "astra_bridge",
            hubNodeId = "deep_mine",
            worldId = "world_1"
        ).filterIsInstance<AudioCommand.Play>().single()
        assertEquals("bridge_theme", roomCmd.cueId)
    }

    @Test
    fun suiteSelectionAvoidsImmediateRepeatsAndStaysSteadyWithinSuite() {
        val bindings = AudioBindings(
            music = mapOf("hub_1" to "suite:w1_explore")
        )
        val catalog = AudioCatalog(
            tracks = listOf(
                AudioTrackMetadata(id = "w1_a"),
                AudioTrackMetadata(id = "w1_b")
            ),
            suites = mapOf(
                "w1_explore" to AudioSuiteMetadata(tracks = listOf("w1_a", "w1_b"), shuffle = false)
            )
        )
        val router = AudioRouter(bindings, catalog)
        val first = router.commandsForRoom("hub_1", "room_1").filterIsInstance<AudioCommand.Play>().single()
        assertEquals("w1_a", first.cueId)
        assertTrue(router.commandsForRoom("hub_1", "room_2").isEmpty())

        router.commandsForLayerOverride(AudioCueType.MUSIC, cueId = "combat")
        val returned = router.commandsForRoom("hub_1", "room_2").filterIsInstance<AudioCommand.Play>().single()
        assertEquals("w1_b", returned.cueId)
    }

    @Test
    fun combatMusicResolvesEnemyOverrideBeforeWorldFallback() {
        val bindings = AudioBindings(
            combatMusic = mapOf(
                "compliance_avatar" to "music_w5_boss_compliance",
                "world_5_combat" to "music_w5_combat"
            )
        )
        val router = AudioRouter(bindings)
        assertEquals(
            "music_w5_boss_compliance",
            router.resolveCombatMusicTrack(listOf("compliance_avatar"), "world_5", isBoss = true)
        )
        assertEquals(
            "music_w5_combat",
            router.resolveCombatMusicTrack(listOf("orbital_fighter"), "world_5", isBoss = false)
        )
        val stinger = router.commandsForStinger("stinger_motif_jed").single() as AudioCommand.Play
        assertEquals(AudioCueType.BATTLE, stinger.type)
        assertEquals(false, stinger.loop)
        assertEquals(false, stinger.triggerHaptic)
    }

    @Test
    fun uiAndBattleBindingsResolveNewGameplayCuesAndPreserveUnknownEmpty() {
        val bindings = AudioBindings(
            ui = mapOf(
                "tinkering_wrench" to "sfx_tinkering_wrench",
                "turn_start" to "sfx_ui_confirm",
                "action_retreat" to "ui_room_move",
                "sfx_button_toggle" to "sfx_ui_button_click",
                "sfx_rest_chime" to "sfx_rest_chime",
                "sfx_bunk_light_on" to "sfx_bunk_light_on",
                "sfx_loot_cache" to "sfx_loot_cache",
                "sfx_forage_gather" to "sfx_forage_gather",
                "sfx_datapad_read" to "sfx_datapad_read",
                "ui_title_start" to "ui_title_start"
            ),
            battle = mapOf(
                "retreat" to "ui_room_move",
                "wpn_nova_resonance_shot" to "wpn_nova_resonance_shot",
                "wpn_zeke_body_impact" to "wpn_zeke_body_impact",
                "wpn_gh0st_wire_slash" to "wpn_gh0st_wire_slash",
                "wpn_orion_bowl_resonance" to "wpn_orion_bowl_resonance"
            )
        )
        val catalog = AudioCatalog(
            cues = listOf(
                AudioCueMetadata(id = "sfx_w3_shield_anchor", category = "stinger")
            )
        )
        val router = AudioRouter(bindings, catalog)

        fun playCueId(cmds: List<AudioCommand>) =
            cmds.filterIsInstance<AudioCommand.Play>().single().cueId

        assertEquals("sfx_tinkering_wrench", playCueId(router.commandsForUi("tinkering_wrench")))
        assertEquals("sfx_ui_confirm", playCueId(router.commandsForUi("turn_start")))
        assertEquals("ui_room_move", playCueId(router.commandsForUi("action_retreat")))
        assertEquals("sfx_ui_button_click", playCueId(router.commandsForUi("sfx_button_toggle")))
        assertEquals("sfx_rest_chime", playCueId(router.commandsForUi("sfx_rest_chime")))
        assertEquals("sfx_bunk_light_on", playCueId(router.commandsForUi("sfx_bunk_light_on")))
        assertEquals("sfx_loot_cache", playCueId(router.commandsForUi("sfx_loot_cache")))
        assertEquals("sfx_forage_gather", playCueId(router.commandsForUi("sfx_forage_gather")))
        assertEquals("sfx_datapad_read", playCueId(router.commandsForUi("sfx_datapad_read")))
        assertEquals("ui_title_start", playCueId(router.commandsForUi("ui_title_start")))
        assertEquals("sfx_w3_shield_anchor", playCueId(router.commandsForUi("sfx_w3_shield_anchor")))

        assertEquals("ui_room_move", playCueId(router.commandsForBattle("retreat")))
        assertEquals("wpn_nova_resonance_shot", playCueId(router.commandsForBattle("wpn_nova_resonance_shot")))
        assertEquals("wpn_zeke_body_impact", playCueId(router.commandsForBattle("wpn_zeke_body_impact")))
        assertEquals("wpn_gh0st_wire_slash", playCueId(router.commandsForBattle("wpn_gh0st_wire_slash")))
        assertEquals("wpn_orion_bowl_resonance", playCueId(router.commandsForBattle("wpn_orion_bowl_resonance")))

        assertTrue(router.commandsForUi("undefined-event").isEmpty())
        assertTrue(router.commandsForBattle("undefined-battle-event").isEmpty())
    }
}
