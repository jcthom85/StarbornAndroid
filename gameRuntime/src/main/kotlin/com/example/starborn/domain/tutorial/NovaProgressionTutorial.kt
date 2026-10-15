package com.example.starborn.domain.tutorial

import com.example.starborn.domain.combat.*
import com.example.starborn.domain.leveling.NovaBuildPrototype as Nova
import com.example.starborn.domain.session.GameSessionState

/** Guidance belongs to the executable prototype, never the legacy trees. */
object NovaProgressionTutorial {
    const val FIRST_AP = "tut_nova_first_ap_v1"
    const val GROUNDED = "tut_nova_grounded_combat_v1"
    const val STEPS = "tut_nova_steps_combat_v1"
    const val TREE_MESSAGE = "Grounded Wire (Saboteur): Arc Tether deals more stability damage. Quiet Steps (Improviser): +1 Speed helps Nova act sooner.\n\nSelect a node to read its effect, then Unlock to spend AP. Perks activate automatically; new attacks appear in Abilities. You can buy from both paths or save AP."

    fun firstAp(session: GameSessionState, enabled: Boolean): TutorialEntry? =
        if (enabled && Nova.isEnabled(session) && session.playerAp > 0 && session.purchasedSkillNodes.isEmpty() &&
            FIRST_AP !in session.tutorialCompleted) TutorialEntry(FIRST_AP, "Ability Points",
            "Ability Points (AP) are your party's shared budget for specializations. Story and level abilities are learned separately.\n\nEach starting perk costs 1 AP. Compare both paths, choose freely, or save your points. Return any time through Party > Nova > Skills.",
            mapOf("title" to "Shape Nova's build", "action_label" to "View Nova's Skills")) else null

    fun treeGuide(session: GameSessionState, enabled: Boolean): String? =
        if (enabled && Nova.isEnabled(session) && session.purchasedSkillNodes.isEmpty()) TREE_MESSAGE else null

    fun combatHint(session: GameSessionState, before: CombatState, after: CombatState): TutorialEntry? {
        if (!Nova.isEnabled(session) || after.outcome != null) return null
        val nova = before.combatants["nova"] ?: return null
        val entries = after.log.drop(before.log.size)
        val action = entries.filterIsInstance<CombatLogEntry.ActionQueued>().firstOrNull { it.actorId == "nova" }?.action
            ?: return null
        val groundedHit = action is CombatAction.SkillUse && action.skillId == "nova_arc_tether" &&
            entries.filterIsInstance<CombatLogEntry.Damage>().any { it.sourceId == "nova" && it.amount > 0 &&
                (after.combatants[it.targetId]?.stability ?: 0) < (before.combatants[it.targetId]?.stability ?: 0) }
        return when {
            Nova.GROUNDED in nova.combatant.talents && GROUNDED !in session.tutorialCompleted && groundedHit ->
                TutorialEntry(GROUNDED, "Grounded Wire at work",
                    "That Arc Tether hit dealt 20% more stability damage thanks to Grounded Wire. Stability is the enemy's guard: empty it to cause Break and make follow-up hits stronger. This perk adds no HP damage or extra Shock duration.")
            Nova.STEPS in nova.combatant.talents && STEPS !in session.tutorialCompleted ->
                TutorialEntry(STEPS, "Quiet Steps at work",
                    "Quiet Steps gives Nova +1 Speed. Speed fills her action gauge faster, helping her reach her next turn sooner. It's always active: there's no ability to equip or extra button to press.")
            else -> null
        }
    }
}
