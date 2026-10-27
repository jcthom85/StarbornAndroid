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

    @Test
    fun `tutorials take priority over queued item toasts`() {
        val manager = UIPromptManager()
        // First item occupies current
        manager.enqueue(itemPrompt("Scrap Metal", "s1", 1, 1))
        // Next items are queued
        manager.enqueue(itemPrompt("Ration Pack", "s2", 1, 1))
        manager.enqueue(TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "combat_guide",
            message = "Combat rules",
            context = "Combat"
        )))

        // Dismiss current item -> Tutorial must be next due to higher priority
        manager.dismissCurrent()
        assertTrue(manager.state.value.current is TutorialPrompt)
    }

    @Test
    fun `suspending toasts pauses item toasts while allowing tutorials`() {
        val manager = UIPromptManager()
        manager.setToastsSuspended(true)

        manager.enqueue(itemPrompt("Scrap Metal", "s1", 1, 1))
        // Item is suspended, current remains null
        org.junit.Assert.assertNull(manager.state.value.current)
        assertEquals(1, manager.state.value.queue.size)

        // Tutorial can still promote while toasts are suspended
        manager.enqueue(TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "nav_guide",
            message = "Navigate",
            context = "Navigation"
        )))
        assertTrue(manager.state.value.current is TutorialPrompt)

        // Dismiss tutorial
        manager.dismissCurrent()
        // Item is still suspended
        org.junit.Assert.assertNull(manager.state.value.current)

        // Resume toasts -> item is promoted
        manager.setToastsSuspended(false)
        assertTrue(manager.state.value.current is ItemGrantedPrompt)
    }

    @Test fun `modal blockers wait for every owner and preserve queued tutorials and rewards`() {
        val manager = UIPromptManager()
        manager.setPresentationBlocked("dialogue", true)
        manager.setPresentationBlocked("inspection", true)
        manager.enqueue(itemPrompt("Biscuits", "gift", 1, 1))
        manager.enqueue(TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "help", message = "Help", context = "System")))
        org.junit.Assert.assertNull(manager.state.value.current)
        manager.setPresentationBlocked("dialogue", false)
        org.junit.Assert.assertNull(manager.state.value.current)
        manager.setPresentationBlocked("inspection", false)
        assertTrue(manager.state.value.current is TutorialPrompt)
        manager.dismissCurrent()
        assertEquals("Biscuits", (manager.state.value.current as ItemGrantedPrompt).itemName)
    }

    @Test fun `stale dismiss and unrelated collect all cannot skip a visible prompt`() {
        val manager = UIPromptManager()
        val first = itemPrompt("First", "one", 1, 1)
        val second = itemPrompt("Second", "two", 1, 1)
        manager.enqueue(first)
        manager.enqueue(second)
        manager.dismissCurrent(first.id)
        manager.dismissCurrent(first.id)
        manager.dismissItemSequence("one")
        assertEquals(second, manager.state.value.current)
    }

    @Test fun `dismiss callback can enqueue the next prompt without losing it`() {
        val manager = UIPromptManager()
        val next = itemPrompt("Next", "two", 1, 1)
        manager.enqueue(ItemGrantedPrompt("First", 1, onDismissCallback = { manager.enqueue(next) }))
        manager.dismissCurrent()
        assertEquals(next, manager.state.value.current)
    }

    @Test fun `Jed equipment offer follows every starter kit card`() {
        val manager = UIPromptManager()
        manager.setPresentationBlocked("dialogue", true)
        val pistol = itemPrompt("Mining Pistol", "kit", 1, 3)
        val armor = itemPrompt("Flux Liner", "kit", 2, 3)
        val core = itemPrompt("Cryo-Inductor", "kit", 3, 3)
        manager.enqueue(pistol); manager.enqueue(armor); manager.enqueue(core)
        val offer = TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "opening_gear_offer", message = "Equip the kit", context = "Equipment"), afterRewards = true)
        manager.enqueue(offer)
        manager.setPresentationBlocked("dialogue", false)
        for (reward in listOf(pistol, armor, core)) {
            assertEquals(reward, manager.state.value.current)
            manager.dismissCurrent(reward.id)
        }
        assertEquals(offer, manager.state.value.current)
    }

    @Test fun `reward group finishes before a tutorial arriving after its first card`() {
        val manager = UIPromptManager()
        val first = itemPrompt("Pistol", "kit", 1, 3)
        val second = itemPrompt("Liner", "kit", 2, 3)
        val third = itemPrompt("Parts", "kit", 3, 3)
        manager.enqueue(first); manager.enqueue(second); manager.enqueue(third)
        val guide = TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "later-guide", message = "A guide", context = "System"))
        manager.enqueue(guide)
        for (card in listOf(first, second, third)) {
            assertEquals(card, manager.state.value.current)
            manager.dismissCurrent(card.id)
        }
        assertEquals(guide, manager.state.value.current)
    }

    @Test fun `dialogue before receipt holds the entire group and mid-group pause preserves its remainder`() {
        val manager = UIPromptManager()
        manager.setPresentationBlocked("dialogue", true)
        val first = itemPrompt("Pistol", "kit", 1, 2)
        val second = itemPrompt("Liner", "kit", 2, 2)
        manager.enqueue(first); manager.enqueue(second)
        org.junit.Assert.assertNull(manager.state.value.current)
        manager.setPresentationBlocked("dialogue", false)
        assertEquals(first, manager.state.value.current)
        manager.setPresentationBlocked("inspection", true)
        manager.dismissCurrent(first.id)
        val guide = TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "guide", message = "A guide", context = "System"))
        manager.enqueue(guide)
        org.junit.Assert.assertNull(manager.state.value.current)
        manager.setPresentationBlocked("inspection", false)
        assertEquals(second, manager.state.value.current)
        manager.dismissCurrent(second.id)
        assertEquals(guide, manager.state.value.current)
    }

    @Test fun `individual item grants from the same busy period also stay together`() {
        val manager = UIPromptManager()
        val first = ItemGrantedPrompt("Rations", 2)
        val second = ItemGrantedPrompt("Medkit", 1)
        val tutorial = TutorialPrompt(com.example.starborn.domain.tutorial.TutorialEntry(
            key = "guide", message = "A guide", context = "System"))
        manager.enqueue(first); manager.enqueue(second); manager.enqueue(tutorial)
        manager.dismissCurrent(first.id)
        assertEquals(second, manager.state.value.current)
        manager.dismissCurrent(second.id)
        assertEquals(tutorial, manager.state.value.current)
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
