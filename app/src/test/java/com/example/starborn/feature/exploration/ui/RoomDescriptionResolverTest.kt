package com.example.starborn.feature.exploration.ui

import com.example.starborn.domain.model.Room
import com.example.starborn.domain.model.RoomDescriptionVariant
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomDescriptionResolverTest {
    @Test
    fun fallsBackToBaseDescriptionWhenNoVariantMatches() {
        val room = testRoom(
            description = "Base text.",
            variants = listOf(
                RoomDescriptionVariant(
                    description = "State text.",
                    requiresState = mapOf("fixed" to true)
                )
            )
        )

        assertEquals(
            "Base text.",
            resolveRoomDescription(room, roomState = emptyMap(), completedMilestones = emptySet(), isRoomDark = false)
        )
    }

    @Test
    fun selectsFirstMatchingStateVariant() {
        val room = testRoom(
            variants = listOf(
                RoomDescriptionVariant(
                    description = "Fixed text.",
                    requiresState = mapOf("fixed" to true)
                )
            )
        )

        assertEquals(
            "Fixed text.",
            resolveRoomDescription(room, roomState = mapOf("fixed" to true), completedMilestones = emptySet(), isRoomDark = false)
        )
    }

    @Test
    fun selectsMilestoneVariantAndHonorsForbiddenMilestones() {
        val room = testRoom(
            variants = listOf(
                RoomDescriptionVariant(
                    description = "Active emergency.",
                    requiresMilestones = listOf("started"),
                    forbiddenMilestones = listOf("finished")
                )
            )
        )

        assertEquals(
            "Active emergency.",
            resolveRoomDescription(room, roomState = emptyMap(), completedMilestones = setOf("started"), isRoomDark = false)
        )
        assertEquals(
            "Base text.",
            resolveRoomDescription(room, roomState = emptyMap(), completedMilestones = setOf("started", "finished"), isRoomDark = false)
        )
    }

    @Test
    fun darkDescriptionTakesPriorityOverVariants() {
        val room = testRoom(
            descriptionDark = "Dark text.",
            variants = listOf(
                RoomDescriptionVariant(
                    description = "Fixed text.",
                    requiresState = mapOf("fixed" to true)
                )
            )
        )

        assertEquals(
            "Dark text.",
            resolveRoomDescription(room, roomState = mapOf("fixed" to true), completedMilestones = emptySet(), isRoomDark = true)
        )
    }

    private fun testRoom(
        description: String = "Base text.",
        descriptionDark: String? = null,
        variants: List<RoomDescriptionVariant> = emptyList()
    ): Room = Room(
        id = "test_room",
        env = "test",
        title = "Test Room",
        backgroundImage = "images/test.png",
        description = description,
        npcs = emptyList(),
        items = emptyList(),
        enemies = emptyList(),
        connections = emptyMap(),
        pos = listOf(0, 0),
        state = emptyMap(),
        actions = emptyList(),
        descriptionDark = descriptionDark,
        descriptionVariants = variants
    )
}
