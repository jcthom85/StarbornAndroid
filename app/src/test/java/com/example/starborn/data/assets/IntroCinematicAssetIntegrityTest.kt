package com.example.starborn.data.assets

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroCinematicAssetIntegrityTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun illustratedIntroHasCompleteReadableSequenceAndResolvablePanels() {
        val type = Types.newParameterizedType(List::class.java, CinematicSceneAsset::class.java)
        val scenes = moshi.adapter<List<CinematicSceneAsset>>(type)
            .fromJson(File("src/main/assets/cinematics.json").readText())
            .orEmpty()
        val intro = scenes.single { it.id == "intro_prologue" }
        val steps = intro.steps.orEmpty().filterNotNull()

        assertEquals("illustrated", intro.presentation)
        assertEquals(true, intro.skippable)
        assertEquals("amb_intro_containment_pressure", intro.ambientCue)
        assertTrue(steps.sumOf { it.durationSeconds ?: 0.0 } in 30.0..40.0)

        val completeCopy = steps.joinToString("\n") { it.text.orEmpty() }
        listOf(
            "CONTAINMENT FAILURE",
            "Broadcast my identity. Draw it away.",
            "Mute this room. Begin stasis.",
            "FIRST DRILL CAGE: 00:40",
            "cannot make quota at factory settings"
        ).forEach { required ->
            assertTrue("Intro is missing required beat: $required", completeCopy.contains(required))
        }

        steps.mapNotNull { it.imagePath }.distinct().forEach { imagePath ->
            val file = File("../world_assets/src/main/assets/$imagePath")
            assertTrue("Missing cinematic image: $imagePath", file.isFile && file.length() > 100_000L)
        }
    }
}
