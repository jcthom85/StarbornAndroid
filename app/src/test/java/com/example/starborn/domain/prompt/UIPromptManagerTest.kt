package com.example.starborn.domain.prompt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UIPromptManagerTest {

    @Test
    fun `collect all removes only the matching item sequence`() {
        val manager = UIPromptManager()
        manager.enqueue(itemPrompt("Mining Pistol", "starter", 1, 3))
        manager.enqueue(itemPrompt("Flux Liner", "starter", 2, 3))
        manager.enqueue(itemPrompt("Cryo-Inductor", "starter", 3, 3))
        manager.enqueue(TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "inventory_help",
            message = "Open Inventory.",
            context = "Inventory"
        )))
        manager.enqueue(itemPrompt("Ration Pack", "later", 1, 1))

        manager.dismissItemSequence("starter")

        assertTrue(manager.state.value.current is TutorialPrompt)
        assertEquals(1, manager.state.value.queue.size)
        assertEquals("Ration Pack", (manager.state.value.queue.single() as ItemGrantedPrompt).itemName)
    }

    @Test
    fun `dismissing one acquisition advances within its sequence`() {
        val manager = UIPromptManager()
        manager.enqueue(itemPrompt("Scrap Metal", "starter", 1, 2, quantity = 2))
        manager.enqueue(itemPrompt("Ration Pack", "starter", 2, 2))

        manager.dismissCurrent()

        val current = manager.state.value.current
        assertTrue(current is ItemGrantedPrompt)
        current as ItemGrantedPrompt
        assertEquals("Ration Pack", current.itemName)
        assertEquals(2, current.sequenceIndex)
        assertEquals(2, current.sequenceTotal)
    }

    private fun itemPrompt(
        name: String,
        sequenceId: String,
        index: Int,
        total: Int,
        quantity: Int = 1
    ) = ItemGrantedPrompt(
        itemName = name,
        quantity = quantity,
        sequenceId = sequenceId,
        sequenceIndex = index,
        sequenceTotal = total
    )
}
