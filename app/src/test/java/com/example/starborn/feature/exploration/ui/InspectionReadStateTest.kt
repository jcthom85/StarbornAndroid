package com.example.starborn.feature.exploration.ui

import com.example.starborn.domain.model.GenericAction
import com.example.starborn.domain.model.ToggleAction
import com.example.starborn.feature.exploration.presentation.*
import com.example.starborn.feature.fishing.viewmodel.FishingResultPayload
import com.example.starborn.feature.exploration.presentation.QuestJournalGrouping
import java.io.*
import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test

class InspectionReadStateTest {
    @Test fun passiveInspectionsPersistButChangedInformationBecomesUnread() {
        val action = GenericAction("moss", "generic", conditionUnmetMessage = "It vibrates.")
        val key = requireNotNull(inspectionReceiptKey(action, "The moss glows."))
        fun plan(a: GenericAction = action, text: String = "The moss glows.") =
            requireNotNull(buildInlineActionPlan(text, listOf(a), emptyMap(), null, mapOf(key to true)))
        assertTrue(plan().segments.single().inspected)
        assertFalse(plan(action.copy(conditionUnmetMessage = "Its signal has changed.")).segments.single().inspected)
        assertFalse(plan(text = "The moss dims.").segments.single().inspected)
        assertNull(inspectionReceiptKey(action.copy(actionEvent = "scan_moss"), "The moss glows."))
        assertNull(inspectionReceiptKey(ToggleAction("valve", "open", null, "Open", "Close", null, null), "valve"))
    }

    @Test fun journalUsesActualCampaignNames() {
        listOf("The Mines", "Sector 9", "The Spire", "The Foundry", "The Void", "The Source")
            .forEachIndexed { index, name -> assertEquals(name, QuestJournalGrouping.group("w${index + 1}_mq01")) }
        assertEquals("Other quests", QuestJournalGrouping.group("astra_side"))
    }

    @Test fun fishingCatchCanCrossAndroidSavedStateAndSerializationWithoutLosingDeliveryFlags() {
        val payload = FishingResultPayload("raw_glowfish", 2, "Catch secured", true, true)
        val handle = SavedStateHandle()
        handle["fishing_result"] = payload
        assertEquals(payload, handle.get<FishingResultPayload>("fishing_result"))
        val bytes = ByteArrayOutputStream().apply { ObjectOutputStream(this).use { it.writeObject(payload) } }.toByteArray()
        val restored = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
        assertEquals(payload, restored)
    }
}
