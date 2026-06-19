package com.example.starborn.feature.combat.viewmodel.helpers

import com.example.starborn.domain.session.GameSessionStore
import com.example.starborn.domain.session.GameSessionState
import com.example.starborn.domain.combat.CombatState
import com.example.starborn.domain.combat.CombatantState
import com.example.starborn.domain.combat.CombatSide
import com.example.starborn.domain.combat.StatusEffect
import com.example.starborn.feature.combat.viewmodel.CombatTutorialState
import com.example.starborn.feature.combat.viewmodel.CombatTutorialStep
import com.example.starborn.feature.combat.viewmodel.COMBAT_BASICS_TUTORIAL_ID
import com.example.starborn.feature.combat.viewmodel.COMBAT_TUTORIAL_SKILL_ID

const val COMBAT_TUTORIAL_PLAYER_ID = "nova"
const val COMBAT_TUTORIAL_ENEMY_ID = "acoustic_bulwark"
const val COMBAT_TUTORIAL_ROOM_ID = "workshop_dock"
const val COMBAT_TUTORIAL_QUEST_ID = "w1_sq03"
const val COMBAT_TUTORIAL_QUEST_STAGE = "guard_break_training"

class CombatTutorialTracker(
    private val tutorialsEnabled: Boolean,
    private val sessionStore: GameSessionStore,
    private val getCombatTutorial: () -> CombatTutorialState?,
    private val setCombatTutorial: (CombatTutorialState?) -> Unit,
    private val tryProcessEnemyTurns: () -> Unit,
    private val isAtbPaused: () -> Boolean,
    private val primeTutorialTurn: () -> Unit
) {
    fun onCombatTutorialContinue() {
        when (getCombatTutorial()?.step) {
            CombatTutorialStep.BRIEF -> {
                primeTutorialTurn()
                setCombatTutorialStep(CombatTutorialStep.SELECT_NOVA_ATTACK)
            }
            CombatTutorialStep.BLOCKED_EXPLANATION -> {
                primeTutorialTurn()
                setCombatTutorialStep(CombatTutorialStep.SELECT_NOVA_SKILL)
            }
            CombatTutorialStep.SUCCESS -> completeCombatTutorial()
            else -> Unit
        }
    }

    fun isCombatTutorialEligible(
        encounterEnemyIdList: List<String>,
        session: GameSessionState,
        currentRoomId: String?
    ): Boolean {
        if (!tutorialsEnabled) return false
        if (!currentRoomId.equals(COMBAT_TUTORIAL_ROOM_ID, ignoreCase = true)) return false
        if (encounterEnemyIdList.size != 1 ||
            !encounterEnemyIdList.first().equals(COMBAT_TUTORIAL_ENEMY_ID, ignoreCase = true)
        ) {
            return false
        }
        if (session.questStageById[COMBAT_TUTORIAL_QUEST_ID] != COMBAT_TUTORIAL_QUEST_STAGE) return false
        if (COMBAT_TUTORIAL_SKILL_ID !in session.unlockedSkills) return false
        return session.tutorialCompleted.none { it.equals(COMBAT_BASICS_TUTORIAL_ID, ignoreCase = true) }
    }

    fun seedCombatTutorial(state: CombatState, enemyIdList: List<String>): CombatState {
        val targetId = enemyIdList.singleOrNull() ?: return state
        val target = state.combatants[targetId] ?: return state
        val shieldedTarget = target.copy(
            statusEffects = target.statusEffects
                .filterNot { it.id.equals("invulnerable", ignoreCase = true) } +
                StatusEffect(id = "invulnerable", remainingTurns = 99)
        )
        setCombatTutorial(
            CombatTutorialState(
                step = CombatTutorialStep.BRIEF,
                targetId = targetId
            )
        )
        sessionStore.markTutorialSeen(COMBAT_BASICS_TUTORIAL_ID)
        return state.copy(combatants = state.combatants + (targetId to shieldedTarget))
    }

    fun setCombatTutorialStep(step: CombatTutorialStep) {
        val current = getCombatTutorial() ?: return
        setCombatTutorial(current.copy(step = step))
    }

    fun completeCombatTutorial() {
        sessionStore.markTutorialCompleted(COMBAT_BASICS_TUTORIAL_ID)
        setCombatTutorial(null)
        if (!isAtbPaused()) {
            tryProcessEnemyTurns()
        }
    }

    fun skipCombatTutorial() {
        if (getCombatTutorial() == null) return
        completeCombatTutorial()
    }

    fun onCombatTutorialCommand(command: String): Boolean {
        val tutorial = getCombatTutorial() ?: return true
        return when {
            command.equals("Attack", ignoreCase = true) &&
                tutorial.step == CombatTutorialStep.CHOOSE_ATTACK -> {
                setCombatTutorialStep(CombatTutorialStep.TARGET_BASIC_ATTACK)
                true
            }
            command.equals("Skills", ignoreCase = true) &&
                tutorial.step == CombatTutorialStep.CHOOSE_SKILLS -> {
                setCombatTutorialStep(CombatTutorialStep.CHOOSE_HYDRAULIC_KICK)
                true
            }
            else -> false
        }
    }

    fun onCombatTutorialSkillSelected(skillId: String): Boolean {
        val tutorial = getCombatTutorial() ?: return true
        if (tutorial.step != CombatTutorialStep.CHOOSE_HYDRAULIC_KICK ||
            skillId != COMBAT_TUTORIAL_SKILL_ID
        ) {
            return false
        }
        setCombatTutorialStep(CombatTutorialStep.TARGET_HYDRAULIC_KICK)
        return true
    }

    fun onCombatTutorialSkillDialogDismissed() {
        if (getCombatTutorial()?.step == CombatTutorialStep.CHOOSE_HYDRAULIC_KICK) {
            setCombatTutorialStep(CombatTutorialStep.CHOOSE_SKILLS)
        }
    }

    fun onCombatTutorialTargetCancelled() {
        when (getCombatTutorial()?.step) {
            CombatTutorialStep.TARGET_BASIC_ATTACK ->
                setCombatTutorialStep(CombatTutorialStep.CHOOSE_ATTACK)
            CombatTutorialStep.TARGET_HYDRAULIC_KICK ->
                setCombatTutorialStep(CombatTutorialStep.CHOOSE_SKILLS)
            else -> Unit
        }
    }

    fun isCombatTutorialCommandEnabled(command: String): Boolean {
        val step = getCombatTutorial()?.step ?: return true
        return when (step) {
            CombatTutorialStep.CHOOSE_ATTACK -> command.equals("Attack", ignoreCase = true)
            CombatTutorialStep.CHOOSE_SKILLS -> command.equals("Skills", ignoreCase = true)
            else -> false
        }
    }

    fun isCombatTutorialTargetEnabled(targetId: String): Boolean {
        val tutorial = getCombatTutorial() ?: return true
        return targetId == tutorial.targetId && (
            tutorial.step == CombatTutorialStep.TARGET_BASIC_ATTACK ||
                tutorial.step == CombatTutorialStep.TARGET_HYDRAULIC_KICK
            )
    }
}
