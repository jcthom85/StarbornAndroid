package com.example.starborn.desktop.ui

/**
 * Adapts player-facing mobile instructions (e.g. tap, swipe) into native PC terminology
 * (click, WASD / arrow keys, press hotkey).
 */
internal fun String.adaptInputVocabularyForDesktop(): String {
    if (isEmpty()) return this
    return this
        .replace("Swipe toward any open exit to move. The minimap pips and connecting lines show nearby routes.",
            "Use WASD or arrow keys to travel, or click highlighted routes on the map.")
        .replace("Swipe in the highlighted direction to move between rooms.",
            "Use WASD or arrow keys to move, or click available exits.")
        .replace("Tap a highlighted character name to start a conversation.",
            "Click a highlighted character name or press E to start a conversation.")
        .replace("Tap a character's name and choose Talk to start a conversation.",
            "Click a character's name and choose Talk (or press E) to start a conversation.")
        .replace("Tap the dialogue card to continue the conversation and advance the story.",
            "Click or press Enter on the dialogue card to advance the story.")
        .replace("Highlighted room details are interactive. Tap one to inspect it or use its available action.",
            "Highlighted room details are interactive. Click one or press E to inspect or interact.")
        .replace("Head out of the Pit toward Jed. Tap highlighted exits or swipe toward open connections to keep moving.",
            "Head out of the Pit toward Jed. Use WASD / Arrow keys or click highlighted exits to keep moving.")
        .replace("Tap a portrait to switch the active character. Each party member has unique skills.",
            "Click a portrait or press Tab to switch the active character. Each party member has unique skills.")
        .replace("Tap the Broken Cryo-Inductor in the tray to place it in the Base socket.",
            "Click the Broken Cryo-Inductor in the tray to place it in the Base socket.")
        .replace("Tap Scrap Metal in the tray to add the replacement part, then press Synthesize when the repair is ready.",
            "Click Scrap Metal in the tray to add the replacement part, then click Synthesize when the repair is ready.")
        .replace("Head into Tyson's stall and tap the Talk prompt to hear his request.",
            "Head into Tyson's stall and click the Talk prompt (or press E) to hear his request.")
        .replace("Tap the tinkering table when Jed calls you over to begin the tutorial.",
            "Click the tinkering table (or press E) when Jed calls you over to begin the tutorial.")
        .replace("Tap an info icon to inspect an item. Equipment is in Party: choose a character, then tap an equipment slot.",
            "Click an item to inspect it. Equipment is in Party: choose a character, then click an equipment slot.")
        .replace("Tap Weapon to equip her cutter, then check Armor and available mod slots before heading into the mines.",
            "Click Weapon to equip her cutter, then check Armor and available mod slots before heading into the mines.")
        .replace("When Nova is ready, tap her, choose Abilities, then Arc Tether. Select the Faulted Loader to exploit its Shock weakness.",
            "When Nova is ready, select Abilities [2], then Arc Tether. Select the Faulted Loader to exploit its Shock weakness.")
        .replace("Tap Nova when her action is ready.", "Nova is ready. Select Attack [1] or Abilities [2].")
        .replace("Nova is ready. Tap Nova to choose an action.", "Nova is ready. Choose an action [1-4].")
        .replace("Nova is ready again. Tap Nova to break the guard.", "Nova is ready again. Select Abilities [2] to break the guard.")
        .replace("Tap the Faulted Loader to engage in combat!", "Engage the Faulted Loader to enter combat!")
        .replace(Regex("\\bTap\\b"), "Click")
        .replace(Regex("\\btap\\b"), "click")
        .replace(Regex("\\bSwipe\\b"), "Move")
        .replace(Regex("\\bswipe\\b"), "move")
}
