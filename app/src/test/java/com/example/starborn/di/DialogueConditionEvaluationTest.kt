package com.example.starborn.di

import com.example.starborn.domain.inventory.InventoryService
import com.example.starborn.domain.inventory.ItemCatalog
import com.example.starborn.domain.model.Item
import com.example.starborn.domain.session.GameSessionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DialogueConditionEvaluationTest {

    private lateinit var inventoryService: InventoryService

    @Before
    fun setup() {
        inventoryService = InventoryService(EmptyItemCatalog()).also { it.loadItems() }
    }

    @Test
    fun questTaskConditionsGateCrashSiteZekeRecruitment() {
        val initialCrashState = GameSessionState(
            activeQuests = setOf("w2_mq01")
        )
        val afterZekeCheckedState = initialCrashState.copy(
            questTasksCompleted = mapOf("w2_mq01" to setOf("check_on_zeke"))
        )

        val recruitCondition = "quest_active:w2_mq01,quest_task_not_done:w2_mq01:check_on_zeke"
        val repeatCondition = "quest_active:w2_mq01,quest_task_done:w2_mq01:check_on_zeke"

        assertTrue(isDialogueConditionMet(recruitCondition, initialCrashState, inventoryService))
        assertFalse(isDialogueConditionMet(repeatCondition, initialCrashState, inventoryService))
        assertFalse(isDialogueConditionMet(recruitCondition, afterZekeCheckedState, inventoryService))
        assertTrue(isDialogueConditionMet(repeatCondition, afterZekeCheckedState, inventoryService))
    }
}

private class EmptyItemCatalog : ItemCatalog {
    override fun load() = Unit

    override fun findItem(idOrAlias: String): Item? = null
}
