package com.example.starborn.domain.cinematic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CinematicCoordinatorTest {

    @Test
    fun skipCompletesActiveSceneOnceAndPromotesQueuedScene() {
        val first = scene("first")
        val second = scene("second")
        val service = mock<CinematicService> {
            on { scene("first") } doReturn first
            on { scene("second") } doReturn second
        }
        val coordinator = CinematicCoordinator(service)
        var firstCompletions = 0
        var secondCompletions = 0

        assertTrue(coordinator.play("first") { firstCompletions++ })
        assertTrue(coordinator.play("second") { secondCompletions++ })

        coordinator.skip()
        assertEquals(1, firstCompletions)
        assertEquals("second", coordinator.state.value?.scene?.id)

        coordinator.skip()
        coordinator.skip()
        assertEquals(1, secondCompletions)
        assertNull(coordinator.state.value)
    }

    private fun scene(id: String) = CinematicScene(
        id = id,
        steps = listOf(CinematicStep(CinematicStepType.NARRATION, text = id))
    )
}
