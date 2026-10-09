package com.example.starborn.feature.combat.presentation

import com.example.starborn.feature.combat.viewmodel.*

/** Original Android tutorial writing, shared by platform renderers. */
object CombatTutorialCopy {
    fun title(tutorial: CombatTutorialState): String {
        val isLoader = tutorial.tutorialType == CombatTutorialType.LOADER_WEAKNESS
        return when (tutorial.step) {
            CombatTutorialStep.BRIEF -> "Combat Tutorial"
            CombatTutorialStep.BLOCKED_EXPLANATION -> "Direct Hit: Blocked"
            CombatTutorialStep.SUCCESS -> if (isLoader) "Stability Broken!" else "Guard Broken"
        else -> "Combat Tutorial"
        }
    }
    fun message(tutorial: CombatTutorialState): String {
        val isLoader = tutorial.tutorialType == CombatTutorialType.LOADER_WEAKNESS
        return when (tutorial.step) {
            CombatTutorialStep.BRIEF ->
            if (isLoader)
                "When Nova is ready, tap her, choose Abilities, then Arc Tether. Select the Faulted Loader to exploit its Shock weakness."
            else
                "That trainer eats direct hits. First, test the shield, then break its guard with Hydraulic Kick."
            CombatTutorialStep.SELECT_NOVA_ATTACK -> "Tap Nova when her action is ready."
            CombatTutorialStep.CHOOSE_ATTACK -> "Choose Attack. First, test the shield."
            CombatTutorialStep.TARGET_BASIC_ATTACK -> if (isLoader) "Choose the Faulted Loader." else "Choose the Shield Trainer."
            CombatTutorialStep.AWAIT_BASIC_RESULT -> if (isLoader) "Watch the standard Attack connect." else "Watch how the shield handles a direct hit."
            CombatTutorialStep.BLOCKED_EXPLANATION ->
            if (isLoader)
                "Standard Attacks build Momentum (the pips beneath your HP bar). Use Arc Tether to Overcharge it for boosted damage and exploit the loader's Shock weakness!"
            else
                "The shield reduced the attack to zero. Your Attack still built Momentum! Guard Break strips protection before you commit damage."
            CombatTutorialStep.SELECT_NOVA_SKILL ->
            if (isLoader) "Nova is ready. Tap Nova to choose an action." else "Nova is ready again. Tap Nova to break the guard."
            CombatTutorialStep.CHOOSE_SKILLS ->
            if (isLoader) "Select Abilities." else "Open Abilities to find a guard-breaking move."
            CombatTutorialStep.CHOOSE_HYDRAULIC_KICK ->
            if (isLoader) "Select Arc Tether." else "Use Hydraulic Kick."
            CombatTutorialStep.TARGET_HYDRAULIC_KICK ->
            if (isLoader) "Target the Faulted Loader." else "Choose an enemy for Hydraulic Kick."
            CombatTutorialStep.AWAIT_SHIELD_BREAK ->
            if (isLoader) "Watch the Shock pulse break its stability." else "Watch the guard break."
            CombatTutorialStep.SUCCESS ->
            if (isLoader)
                "Stability broken! Broken targets take 25% more direct damage. Exploiting weaknesses shortens cooldowns. Use Attacks to build Momentum for your next Overcharge!"
            else
                "Hydraulic Kick stripped the shield. Attack to finish the fight and build Momentum for your next Overcharge!"
        }
    }
    fun continueLabel(tutorial: CombatTutorialState): String {
        val isLoader = tutorial.tutorialType == CombatTutorialType.LOADER_WEAKNESS
        return when (tutorial.step) {
                CombatTutorialStep.BRIEF -> if (isLoader) "Engage" else "Start Training"
                CombatTutorialStep.BLOCKED_EXPLANATION -> if (isLoader) "Exploit Weakness" else "Break The Guard"
                CombatTutorialStep.SUCCESS -> if (isLoader) "Finish Combat" else "Finish The Fight"
            else -> "Continue"
        }
    }
}
