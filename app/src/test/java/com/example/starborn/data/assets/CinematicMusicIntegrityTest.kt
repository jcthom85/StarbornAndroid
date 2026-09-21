package com.example.starborn.data.assets

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class CinematicMusicIntegrityTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun allCinematicMusicCuesResolveToCatalogTracks() {
        val type = Types.newParameterizedType(List::class.java, CinematicSceneAsset::class.java)
        val scenes = moshi.adapter<List<CinematicSceneAsset>>(type)
            .fromJson(File("src/main/assets/cinematics.json").readText())
            .orEmpty()

        val catalogText = File("src/main/assets/audio_catalog.json").readText()

        val scenesWithMusic = scenes.mapNotNull { scene ->
            val musicCues = scene.steps.orEmpty().filterNotNull().mapNotNull { it.musicCue }
            if (musicCues.isNotEmpty()) scene.id to musicCues else null
        }.toMap()

        // Key narrative cutscenes must have music cues
        val expectedScoredScenes = listOf(
            "intro_prologue",
            "scene_launch_crash",
            "scene_mine_restore",
            "scene_anchor_drill",
            "scene_w6_final_note",
            "scene_w6_epilogue_credits"
        )

        expectedScoredScenes.forEach { sceneId ->
            assertTrue("Expected scene $sceneId to have music cues", scenesWithMusic.containsKey(sceneId))
        }

        // Every music cue in cinematics must exist in audio_catalog.json
        scenesWithMusic.values.flatten().distinct().forEach { cueId ->
            assertTrue("Music cue '$cueId' not found in audio_catalog.json", catalogText.contains("\"id\": \"$cueId\""))
        }
    }
}
