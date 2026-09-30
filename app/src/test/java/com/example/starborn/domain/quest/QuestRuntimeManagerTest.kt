package com.example.starborn.domain.quest

import com.example.starborn.data.assets.QuestAssetDataSource
import com.example.starborn.data.repository.QuestRepository
import com.example.starborn.domain.model.Quest
import com.example.starborn.domain.model.QuestStage
import com.example.starborn.domain.model.QuestTask
import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.ui.events.UiEventBus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.example.starborn.ui.events.QuestBannerType
import com.example.starborn.ui.events.QuestObjectiveRole
import com.example.starborn.ui.events.UiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuestRuntimeManagerTest {

    @Test
    fun stageAdvancementUpdatesState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val quest = Quest(
            id = "quest_main",
            title = "Repair the Generator",
            summary = "Bring the generator back online.",
            description = "The mining colony needs power restored.",
            stages = listOf(
                QuestStage(
                    id = "intro",
                    title = "Talk to Jed",
                    description = "Head to Jed's shop and hear him out.",
                    tasks = listOf(QuestTask(id = "talk_to_jed", text = "Speak with Jed."))
                ),
                QuestStage(
                    id = "repair",
                    title = "Repair the Generator",
                    description = "Collect parts and repair the generator.",
                    tasks = listOf(QuestTask(id = "repair_generator", text = "Fix the generator."))
                )
            )
        )
        val questRepository = createRepository(quest)
        val sessionStore = GameSessionStore()
        val manager = QuestRuntimeManager(questRepository, sessionStore, scope, UiEventBus())

        sessionStore.startQuest("quest_main", track = true)
        advanceUntilIdle()

        manager.setStage("quest_main", "repair")
        advanceUntilIdle()
        manager.markTaskComplete("quest_main", "repair_generator")
        advanceUntilIdle()

        assertEquals("repair", manager.currentStageId("quest_main"))
        val state = sessionStore.state.value
        assertEquals("repair", state.questStageById["quest_main"])
        val completed = state.questTasksCompleted["quest_main"].orEmpty()
        assertTrue(completed.contains("repair_generator"))
        scope.cancel()
    }

    @Test
    fun markQuestCompletedMovesQuestToCompletedSessionState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val quest = Quest(
            id = "quest_main",
            title = "Repair the Generator",
            stages = listOf(
                QuestStage(
                    id = "repair",
                    title = "Repair",
                    tasks = listOf(QuestTask(id = "repair_generator", text = "Fix the generator."))
                )
            )
        )
        val questRepository = createRepository(quest)
        val sessionStore = GameSessionStore()
        val manager = QuestRuntimeManager(questRepository, sessionStore, scope, UiEventBus())

        sessionStore.startQuest("quest_main", track = true)
        advanceUntilIdle()
        manager.markQuestCompleted("quest_main")
        advanceUntilIdle()

        val state = sessionStore.state.value
        assertFalse("quest_main" in state.activeQuests)
        assertTrue("quest_main" in state.completedQuests)
        assertEquals("repair", state.questStageById["quest_main"])
        assertTrue(state.questTasksCompleted["quest_main"].orEmpty().contains("repair_generator"))
        scope.cancel()
    }

    @Test
    fun progressBannerListsCompletedObjectiveNextObjectiveAndRemainingCount() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val quest = Quest(
            id = "quest_main",
            title = "Repair the Generator",
            stages = listOf(
                QuestStage(
                    id = "repair",
                    title = "Repair",
                    tasks = listOf(
                        QuestTask(id = "find_parts", text = "Find parts."),
                        QuestTask(id = "prime_generator", text = "Prime the generator."),
                        QuestTask(id = "reset_breaker", text = "Reset the breaker."),
                        QuestTask(id = "report_back", text = "Report back.")
                    )
                )
            )
        )
        val questRepository = createRepository(quest)
        val sessionStore = GameSessionStore()
        val uiEventBus = UiEventBus()
        val events = mutableListOf<UiEvent>()
        val collector = launch(dispatcher) {
            uiEventBus.events.collect { events.add(it) }
        }
        val manager = QuestRuntimeManager(questRepository, sessionStore, scope, uiEventBus)

        sessionStore.startQuest("quest_main", track = true)
        advanceUntilIdle()
        manager.markTaskComplete("quest_main", "find_parts")
        advanceUntilIdle()

        val banner = events.filterIsInstance<UiEvent.ShowQuestBanner>().single {
            it.type == QuestBannerType.PROGRESS
        }
        assertEquals(listOf("find_parts", "prime_generator"), banner.objectives.map { it.id })
        assertEquals(listOf(true, false), banner.objectives.map { it.completed })
        assertEquals(
            listOf(QuestObjectiveRole.JUST_COMPLETED, QuestObjectiveRole.NEXT),
            banner.objectives.map { it.role }
        )
        assertEquals(2, banner.remainingObjectiveCount)
        collector.cancel()
        scope.cancel()
    }

    @Test
    fun finalTaskCompletionDoesNotEmitProgressBanner() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val quest = Quest(
            id = "quest_main",
            title = "Repair the Generator",
            stages = listOf(
                QuestStage(
                    id = "repair",
                    title = "Repair",
                    tasks = listOf(QuestTask(id = "repair_generator", text = "Fix the generator."))
                )
            )
        )
        val questRepository = createRepository(quest)
        val sessionStore = GameSessionStore()
        val uiEventBus = UiEventBus()
        val events = mutableListOf<UiEvent>()
        val collector = launch(dispatcher) {
            uiEventBus.events.collect { events.add(it) }
        }
        val manager = QuestRuntimeManager(questRepository, sessionStore, scope, uiEventBus)

        sessionStore.startQuest("quest_main", track = true)
        advanceUntilIdle()
        manager.markTaskComplete("quest_main", "repair_generator")
        advanceUntilIdle()

        assertTrue(sessionStore.state.value.completedQuests.contains("quest_main"))
        assertFalse(
            events.filterIsInstance<UiEvent.ShowQuestBanner>().any { it.type == QuestBannerType.PROGRESS }
        )
        collector.cancel()
        scope.cancel()
    }

    @Test
    fun optionalObjectiveDoesNotBlockStageProgression() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val quest = Quest(id = "optional_route", title = "Route", stages = listOf(
                QuestStage(id = "mine", title = "Mine", tasks = listOf(
                    QuestTask("detour", "Optional detour", optional = true),
                    QuestTask("threshold", "Reach the threshold")
                )),
                QuestStage(id = "fork", title = "Fork", tasks = listOf(QuestTask("sync", "Sync")))
            ))
            val store = GameSessionStore()
            val manager = QuestRuntimeManager(createRepository(quest), store, scope, UiEventBus())
            store.startQuest(quest.id, track = true)
            advanceUntilIdle()
            manager.markTaskComplete(quest.id, "threshold")
            advanceUntilIdle()
            assertEquals("fork", manager.currentStageId(quest.id))
            assertFalse("detour" in manager.completedTaskIds(quest.id))
            assertFalse(quest.id in store.state.value.completedQuests)
        } finally { scope.cancel() }
    }

    @Test
    fun skippedOptionalTaskIsNotMarkedCompletedWhenQuestEnds() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val quest = Quest(id = "optional_finish", title = "Route", stages = listOf(
                QuestStage(id = "final", title = "Final", tasks = listOf(
                    QuestTask("detour", "Optional detour", optional = true),
                    QuestTask("finish", "Finish")
                ))
            ))
            val store = GameSessionStore()
            val manager = QuestRuntimeManager(createRepository(quest), store, scope, UiEventBus())
            store.startQuest(quest.id, track = true)
            advanceUntilIdle()
            manager.markTaskComplete(quest.id, "finish")
            advanceUntilIdle()
            assertTrue(quest.id in store.state.value.completedQuests)
            assertFalse("detour" in manager.completedTaskIds(quest.id))
            val entry = manager.state.value.completedJournal.single { it.id == quest.id }
            assertFalse(entry.objectives.single { it.id == "detour" }.completed)
            assertTrue(entry.objectives.single { it.id == "finish" }.completed)
        } finally { scope.cancel() }
    }

    @Test
    fun completedOptionalTaskRemainsCompletedAndDoesNotFinishRequiredTask() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        try {
            val quest = Quest(id = "optional_taken", title = "Route", stages = listOf(
                QuestStage(id = "final", title = "Final", tasks = listOf(
                    QuestTask("detour", "Optional detour", optional = true),
                    QuestTask("finish", "Finish")
                ))
            ))
            val store = GameSessionStore()
            val manager = QuestRuntimeManager(createRepository(quest), store, scope, UiEventBus())
            store.startQuest(quest.id, track = true)
            advanceUntilIdle()
            manager.markTaskComplete(quest.id, "detour")
            advanceUntilIdle()
            assertFalse(quest.id in store.state.value.completedQuests)
            manager.markTaskComplete(quest.id, "finish")
            advanceUntilIdle()
            assertTrue(quest.id in store.state.value.completedQuests)
            assertTrue(manager.state.value.completedJournal.single().objectives.single { it.id == "detour" }.completed)
        } finally { scope.cancel() }
    }

    private fun createRepository(vararg quests: Quest): QuestRepository {
        val dataSource = mockk<QuestAssetDataSource>()
        every { dataSource.loadQuests() } returns quests.toList()
        return QuestRepository(dataSource).apply { load() }
    }
}
