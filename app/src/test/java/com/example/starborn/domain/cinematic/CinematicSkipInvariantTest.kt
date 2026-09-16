package com.example.starborn.domain.cinematic

import com.example.starborn.data.assets.CinematicSceneAsset
import com.example.starborn.data.assets.CinematicStepAsset
import com.example.starborn.domain.event.EventHooks
import com.example.starborn.domain.event.EventManager
import com.example.starborn.domain.event.EventPayload
import com.example.starborn.domain.model.GameEvent
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.session.GameSessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

/**
 * Validates Priority 5 of the Automated Testing Strategy:
 * Cinematic Skip & Interruption Invariant Testing across all scenes in cinematics.json.
 *
 * Verifies:
 * 1. Structural validity of all authored scenes (non-empty steps, non-blank text).
 * 2. Referential integrity between events.json and cinematics.json.
 * 3. Immediate skip invocation guarantees completion callbacks for every single scene.
 * 4. Queued scene promotion after skip.
 * 5. State parity: Immediate skip produces identical session state to full step-by-step playback.
 */
class CinematicSkipInvariantTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // =========================================================================
    // 1. Structural Integrity of the Complete Cinematic Catalog
    // =========================================================================

    @Test
    fun allCinematicScenesAreValidAndNonEmpty() {
        val scenes = loadScenes()
        assertTrue("Cinematics catalog should have at least 50 scenes, found ${scenes.size}", scenes.size >= 50)

        val seenIds = mutableSetOf<String>()
        for (scene in scenes) {
            val sceneId = scene.id
            assertNotNull("Scene ID must not be null", sceneId)
            assertTrue("Scene ID must not be blank", sceneId!!.isNotBlank())
            assertFalse("Duplicate scene ID found: $sceneId", seenIds.contains(sceneId))
            seenIds.add(sceneId)

            val steps = scene.steps
            assertNotNull("Scene $sceneId must have steps", steps)
            assertTrue("Scene $sceneId must have at least 1 step", steps!!.isNotEmpty())

            for ((index, step) in steps.withIndex()) {
                assertNotNull("Step $index in $sceneId must not be null", step)
                val text = step?.text ?: step?.line
                val isSilentVisual = step?.captionStyle?.lowercase() == "none" ||
                    ((step?.durationSeconds ?: 0.0) > 0.0 && text.isNullOrEmpty())
                assertTrue("Step $index in $sceneId must have non-blank text unless it is a visual timing step",
                    !text.isNullOrBlank() || isSilentVisual)
            }
        }
    }

    // =========================================================================
    // 2. Referential Integrity: events.json -> cinematics.json
    // =========================================================================

    @Test
    fun allCinematicReferencesInEventsResolveToAuthoredScenes() {
        val scenes = loadScenes().associateBy { it.id }
        val events = loadEvents()

        val missingReferences = mutableListOf<String>()

        for (event in events) {
            for (action in event.actions) {
                if (action.type.lowercase() in setOf("play_cinematic", "trigger_cutscene")) {
                    val sceneId = action.sceneId
                    if (!sceneId.isNullOrBlank() && !scenes.containsKey(sceneId)) {
                        missingReferences.add("Event '${event.id}' references non-existent scene '$sceneId'")
                    }
                }
            }
        }

        assertTrue("All cinematic references in events.json must exist in cinematics.json:\n" +
            missingReferences.joinToString("\n"), missingReferences.isEmpty())
    }

    // =========================================================================
    // 3. Skip Callback Invariant for Every Single Scene in Catalog
    // =========================================================================

    @Test
    fun skipImmediatelyFiresCompletionCallbackForEveryAuthoredScene() {
        val domainScenes = loadDomainScenes()
        assertTrue("Should have loaded domain scenes", domainScenes.isNotEmpty())
        val sceneMap = domainScenes.associateBy { it.id }

        val service = mock<CinematicService> {
            on { scene(anyOrNull()) } doAnswer { invocation ->
                val id = invocation.getArgument<String?>(0)
                sceneMap[id]
            }
        }
        val coordinator = CinematicCoordinator(service)

        for (scene in domainScenes) {
            var completed = false
            var sceneStartFired = false
            var sceneEndFired = false

            coordinator.setCallbacks(
                onSceneStart = { sceneStartFired = true },
                onSceneEnd = { sceneEndFired = true }
            )

            val started = coordinator.play(scene.id) {
                completed = true
            }

            assertTrue("Scene '${scene.id}' should start playback", started)
            assertTrue("onSceneStart should fire for '${scene.id}'", sceneStartFired)
            assertNotNull("State should be active for '${scene.id}'", coordinator.state.value)
            assertEquals(scene.id, coordinator.state.value?.scene?.id)
            assertEquals(0, coordinator.state.value?.stepIndex)

            // Force immediate skip
            coordinator.skip()

            assertTrue("Completion callback MUST fire upon skip for '${scene.id}'", completed)
            assertTrue("onSceneEnd should fire upon skip for '${scene.id}'", sceneEndFired)
            assertNull("State must be cleared after skip for '${scene.id}'", coordinator.state.value)
        }
    }

    // =========================================================================
    // 4. Queued Scene Promotion on Skip
    // =========================================================================

    @Test
    fun skipPromotesQueuedScenesAndCompletesBothCleanly() {
        val domainScenes = loadDomainScenes()
        assertTrue(domainScenes.size >= 2)
        val first = domainScenes[0]
        val second = domainScenes[1]
        val sceneMap = mapOf(first.id to first, second.id to second)

        val service = mock<CinematicService> {
            on { scene(anyOrNull()) } doAnswer { invocation ->
                val id = invocation.getArgument<String?>(0)
                sceneMap[id]
            }
        }
        val coordinator = CinematicCoordinator(service)

        var firstDone = false
        var secondDone = false

        coordinator.play(first.id) { firstDone = true }
        coordinator.play(second.id) { secondDone = true }

        assertEquals(first.id, coordinator.state.value?.scene?.id)

        // Skip first scene -> should promote second scene
        coordinator.skip()
        assertTrue("First scene should complete", firstDone)
        assertFalse("Second scene should not be completed yet", secondDone)
        assertEquals(second.id, coordinator.state.value?.scene?.id)
        assertEquals(0, coordinator.state.value?.stepIndex)

        // Skip second scene -> should clear
        coordinator.skip()
        assertTrue("Second scene should complete", secondDone)
        assertNull("Coordinator state should be cleared", coordinator.state.value)
    }

    // =========================================================================
    // 5. State Parity Invariant: Full Playback vs Immediate Skip
    // =========================================================================

    @Test
    fun immediateSkipProducesIdenticalSessionStateToFullPlayback() {
        val domainScenes = loadDomainScenes()
        val sceneMap = domainScenes.associateBy { it.id }
        val service = mock<CinematicService> {
            on { scene(anyOrNull()) } doAnswer { invocation ->
                val id = invocation.getArgument<String?>(0)
                sceneMap[id]
            }
        }
        val events = loadEvents()

        // Find events that contain play_cinematic with on_complete handlers
        val cinematicEventsWithOnComplete = events.filter { event ->
            event.actions.any { it.type.lowercase() in setOf("play_cinematic", "trigger_cutscene") && !it.onComplete.isNullOrEmpty() }
        }

        assertTrue("Should find events with cinematic on_complete handlers", cinematicEventsWithOnComplete.isNotEmpty())

        for (event in cinematicEventsWithOnComplete) {
            val cinematicAction = event.actions.first {
                it.type.lowercase() in setOf("play_cinematic", "trigger_cutscene") && !it.onComplete.isNullOrEmpty()
            }
            val sceneId = cinematicAction.sceneId ?: continue

            // Run A: Full playback (advance step by step)
            val storeA = GameSessionStore()
            val coordinatorA = CinematicCoordinator(service)
            val eventManagerA = EventManager(
                events = listOf(event),
                sessionStore = storeA,
                eventHooks = EventHooks(
                    onPlayCinematic = { id, onComplete ->
                        coordinatorA.play(id, onComplete)
                    },
                    onQuestTaskUpdated = { qId, tId ->
                        if (!qId.isNullOrBlank() && !tId.isNullOrBlank()) {
                            storeA.setQuestTaskCompleted(qId, tId, true)
                        }
                    },
                    onSetRoomState = { rId, k, v ->
                        if (!rId.isNullOrBlank() && !k.isNullOrBlank()) {
                            storeA.setRoomState(rId, k, v)
                        }
                    }
                )
            )
            eventManagerA.handleTrigger(event.trigger.type, EventPayload.Action(event.trigger.action.orEmpty()))
            // Advance until finished
            while (coordinatorA.state.value != null) {
                coordinatorA.advance()
            }

            // Run B: Immediate skip
            val storeB = GameSessionStore()
            val coordinatorB = CinematicCoordinator(service)
            val eventManagerB = EventManager(
                events = listOf(event),
                sessionStore = storeB,
                eventHooks = EventHooks(
                    onPlayCinematic = { id, onComplete ->
                        coordinatorB.play(id, onComplete)
                    },
                    onQuestTaskUpdated = { qId, tId ->
                        if (!qId.isNullOrBlank() && !tId.isNullOrBlank()) {
                            storeB.setQuestTaskCompleted(qId, tId, true)
                        }
                    },
                    onSetRoomState = { rId, k, v ->
                        if (!rId.isNullOrBlank() && !k.isNullOrBlank()) {
                            storeB.setRoomState(rId, k, v)
                        }
                    }
                )
            )
            eventManagerB.handleTrigger(event.trigger.type, EventPayload.Action(event.trigger.action.orEmpty()))
            // Immediately skip
            coordinatorB.skip()

            // State parity assertions
            assertEquals("Milestones must be identical for event ${event.id}",
                storeA.state.value.completedMilestones, storeB.state.value.completedMilestones)
            assertEquals("Active quests must be identical for event ${event.id}",
                storeA.state.value.activeQuests, storeB.state.value.activeQuests)
            assertEquals("Completed quests must be identical for event ${event.id}",
                storeA.state.value.completedQuests, storeB.state.value.completedQuests)
            assertEquals("Inventory must be identical for event ${event.id}",
                storeA.state.value.inventory, storeB.state.value.inventory)
            assertEquals("Room states must be identical for event ${event.id}",
                storeA.state.value.roomStates, storeB.state.value.roomStates)
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private fun loadScenes(): List<CinematicSceneAsset> {
        val type = Types.newParameterizedType(List::class.java, CinematicSceneAsset::class.java)
        val adapter = moshi.adapter<List<CinematicSceneAsset>>(type)
        return requireNotNull(adapter.fromJson(File("src/main/assets/cinematics.json").readText()))
    }

    private fun loadDomainScenes(): List<CinematicScene> {
        return loadScenes().map { asset ->
            CinematicScene(
                id = asset.id.orEmpty(),
                title = asset.title,
                backdrop = CinematicBackdrop.fromRaw(asset.backdrop),
                presentation = CinematicPresentation.fromRaw(asset.presentation),
                ambientCue = asset.ambientCue,
                skippable = asset.skippable ?: false,
                steps = asset.steps.orEmpty().mapNotNull { step ->
                    val text = step.text ?: step.line ?: return@mapNotNull null
                    val type = CinematicStepType.fromRaw(step.type)
                    CinematicStep(
                        type = type,
                        speaker = step.speaker,
                        text = text,
                        portrait = step.portrait,
                        durationSeconds = step.durationSeconds,
                        emote = step.emote,
                        imagePath = step.imagePath,
                        cameraMotion = CinematicCameraMotion.fromRaw(step.cameraMotion),
                        cameraStartScale = step.cameraStartScale,
                        cameraEndScale = step.cameraEndScale,
                        cameraStartX = step.cameraStartX,
                        cameraEndX = step.cameraEndX,
                        cameraStartY = step.cameraStartY,
                        cameraEndY = step.cameraEndY,
                        transition = CinematicTransition.fromRaw(step.transition),
                        audioCue = step.audioCue,
                        voiceCue = step.voiceCue,
                        musicCue = step.musicCue,
                        fadeOutSeconds = step.fadeOutSeconds,
                        captionStyle = CinematicCaptionStyle.fromRaw(step.captionStyle, type)
                    )
                }
            )
        }
    }

    private fun loadEvents(): List<GameEvent> {
        val type = Types.newParameterizedType(List::class.java, GameEvent::class.java)
        val adapter = moshi.adapter<List<GameEvent>>(type)
        return requireNotNull(adapter.fromJson(File("src/main/assets/events.json").readText()))
    }
}
