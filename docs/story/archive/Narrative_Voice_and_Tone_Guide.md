# Starborn — Narrative Voice & Tone Guide

**Status:** Canon
**Purpose:** To define the specific "voice" of the game's text, ensuring consistency across rooms, items, quests, and UI.

---

# 1. The Core Pillar: "Blue Collar Cosmic"

The narrator is **not** a poetic bard; the narrator is a mechanic looking at a broken engine. The text should feel industrial, lived-in, and observant. It focuses on texture, temperature, and sound rather than abstract philosophy.

*   **The Rule:** If you can’t touch it, smell it, or fix it, don’t describe it—unless it’s **The Source**, in which case, describe how it breaks those rules.
*   **Keywords:** Grime, Neon, Ozone, Static, Hum, Cold, Heat, Rhythm.

---

# 2. Voice Guidelines by Text Type

## A. Room Descriptions (The "You" Camera)
Room text needs to convey immediate atmosphere and interactive potential without being a "wall of text."

*   **Perspective:** Second Person ("You").
*   **Focus:** Immediate sensory details. What does the air smell like? What is the light doing?

### Contrast Guide:
*   **Colony/Ship:** Describe **friction**. Things are rusted, taped together, humming, vibrating.
    *   *Bad:* "You are in a messy workshop."
    *   *Good:* "The workshop smells of ozone and stale coffee. A workbench is covered in scavenged droid parts that rattle every time the ship’s engine cycles."
*   **Dominion Areas:** Describe **sterility**. Things are silent, cold, white, blinding.
    *   *Example:* "The airlock hisses open to a red carpet that floats slightly off the floor. Vacuum silence."
*   **Source/Ruins:** Describe **synesthesia**. Sound looks like light; geometry feels like pressure.
    *   *Example:* "The murals shimmer like a waveform finding a speaker. The stone hums."

## B. Item Descriptions (Lore in the Margins)
Items should never just be stats. They are artifacts of the world. Use the "Show, Don't Tell" rule.

*   **The Rule:** Answer "Who held this last?" or "How does it feel to hold?"
*   **Consumables:** Focus on the low quality of life.
    *   *Example (Street Bento):* "Real protein, allegedly. Tastes like rain and hot sauce."
*   **Weapons:** Focus on the intent.
    *   *Example (Pulse Grenade):* "Standard issue mining explosive. Designed to crack rocks; equally effective on ribs."
*   **Key Items:** Focus on the "vibe."
    *   *Example (The Tuning Fork):* "It hums. Holding it feels like grabbing a live wire that sings."

## C. Quest Logs (The Cynical Note-Taker)
Since Nova is the protagonist, the Quest Log should reflect her internal monologue. It shouldn't be a dry "Go here, do that." It should be Nova reminding herself to survive.

*   **Voice:** Scrappy, direct, survival-focused.
*   **Example (Standard):** "Objective: Defeat the Warden."
*   **Example (Starborn Voice):** "The Warden has the launch codes. He also has a riot shield. Break the shield, take the codes, get off this rock."

## D. System/UI Messages (The Corporate Overlay)
Use the Dominion's own "clean, polite" voice against them for system alerts, tutorials, or errors. This creates a meta-layer of oppression.

*   **Voice:** Polite, clinical, threatening.
*   **Example:** Instead of "Access Denied," use: "Compliance Failure. Personnel unverified."

---

# 3. "The Source" Exception (The Tone Shift)

When the Source is involved (Relics, Echoes, Dreams), the voice breaks. The "Blue Collar" tone disappears, replaced by abstract, musical, and terrifying descriptions. This signals to the player that reality is wrong.

*   **The Shift:** From "Grime/Texture" to "Vibration/Aftertone."
*   **The Metaphor:** The Ocean, The Chord, The Current.

---

# 4. Implementation Checklist for Writers

To ensure consistency, apply this checklist to every piece of text:

1.  **The "Grunt" Test:** Is the description too flowery? If Nova wouldn't think it, cut it. (Unless it's Orion or The Source).
2.  **No Lore Dumps:** Don't explain the history of the mining colony in a room description. Put it in a "Dead Miner's Datapad".
3.  **Active Verbs:** Avoid "There is a table here." Use "A table dominates the room."
4.  **Lighting & Sound:** Every room description must mention the **light source** (neon, emergency red, sterile white, bioluminescent) or the **ambient sound** (humming, silence, dripping). This grounds the "Atmosphere" goals.

---

# 6. Refinements (Edge Cases)

## A. The "Tactile Verb" Rule (Interaction Text)
You covered Room Descriptions, but Interaction Results (what happens when I tap "Open") need the same love. Avoid passive results.

*   *Bad:* "The door opens."
*   *Good (Blue Collar):* "The hydraulics scream, but the door slides back."
*   *Good (Source):* "The door dissolves into light."
*   **The Rule:** Physical things should have **weight/resistance**. Source things should have **none**.

## B. The "Zeke" Overlay (Optional Hint System)
If you have a hint system or "Scan" mechanic, define its voice too. This keeps the "Found Family" aspect alive even in tooltips.

*   **Nova's Scan:** Practical. "Weak point: The joints."
*   **Zeke's Scan:** Nervous/Over-informative. "Technically that's a load-bearing drone. If you hit the left vent, it voids the warranty (and explodes)."

## C. Source Text: The "Synesthesia" Glossary
To help writers keep "The Source" consistent (so it doesn't just feel like random nonsense), map the senses incorrectly.

*   **Sound is described as Texture:** "The hum feels like grit in your teeth."
*   **Light is described as Sound:** "The light is screaming."
*   **Geometry is described as Emotion:** "The angle of the wall feels hateful."

---

# 7. Detailed Interaction Matrix

| Text Type | Persona | The "Verb" Rule | Example |
| :--- | :--- | :--- | :--- |
| **Interaction** | The Mechanic | Things have weight/resistance. | "You kick the panel. It rattles open." |
| **Source Interaction** | The Dream | Things happen before you touch them. | "You reach for the handle. The door is already open. It was always open." |
| **Fail State** | The HR Bot | Blame the user. Polite. | "Asset functionality ceased. Ticket closed." |
| **Hint/Scan** | The Insider (Zeke) | Too much info, high anxiety. | "Don't touch the glowing bit. That's pure radiation. Or magic. Both are bad." |
