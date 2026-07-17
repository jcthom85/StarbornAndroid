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
            "CAGE ONE DESCENT: 00:40",
            "QUOTA STATUS: SHORT"
        ).forEach { required ->
            assertTrue("Intro is missing required beat: $required", completeCopy.contains(required))
        }

        assertTrue("The prologue must not reveal the unknown speaker", steps.none { it.speaker == "Orion" })
        assertTrue("The prologue must not identify Orion in narration", !completeCopy.contains("Orion"))
        assertTrue("The prologue should cut directly into the bunk", !completeCopy.contains("Nova got"))
        val unknownDialogue = steps.filter { it.speaker == "???" }
        assertEquals(2, unknownDialogue.size)
        assertTrue(
            "Spoken intro lines must use dialogue presentation",
            unknownDialogue.all { it.type == "dialogue" && it.captionStyle == "dialogue" }
        )
        assertTrue(
            "Unknown speaker lines must use contextual portraits without revealing the name",
            unknownDialogue.all { !it.portrait.isNullOrBlank() && it.portrait!!.contains("orion_") }
        )
        val illustratedSteps = steps.filter { it.imagePath != null }
        illustratedSteps.zipWithNext().forEach { (current, next) ->
            if (current.imagePath == next.imagePath) {
                assertEquals(current.cameraEndScale, next.cameraStartScale)
                assertEquals(current.cameraEndX ?: 0.0, next.cameraStartX ?: 0.0, 0.001)
                assertEquals(current.cameraEndY ?: 0.0, next.cameraStartY ?: 0.0, 0.001)
            }
        }

        steps.mapNotNull { it.imagePath }.distinct().forEach { imagePath ->
            val file = File("../world_assets/src/main/assets/$imagePath")
            assertTrue("Missing cinematic image: $imagePath", file.isFile && file.length() > 100_000L)
        }
        steps.mapNotNull { it.portrait }.distinct().forEach { portraitPath ->
            val file = File("../world_assets/src/main/assets/$portraitPath")
            assertTrue("Missing cinematic portrait: $portraitPath", file.isFile && file.length() > 100_000L)
        }
    }
}
