# Starborn — Writer's Handbook (The Canon Style)

**Status:** Canon Authority — Prose, Dialogue, UI Copy, and Tonal Execution (Locked July 2026)
**Role:** The style authority, subordinate to the Master Story Bible, Lore Bible, and Character Arcs. Style may sharpen established canon but never revise it.
**Core Pillar:** "Blue Collar Cosmic." The universe is dirty, broken, and held together by duct tape—until the Source breaks it apart.

---

## I. The Golden Rules (Global)

1.  **The "Grunt" Test:** If a tired shift-worker wouldn't think it, don't write it. (Exception: Orion/Source).
2.  **Texture over Philosophy:** Don't describe the "concept" of poverty; describe the taste of recycled air.
3.  **No Lore Dumps:** History belongs in datapads. Room text belongs to the now.
4.  **The Actionable Word System:** Every Room Description must contain 2-4 tappable nouns (Interactables). 
    *   **Implementation:** These words are NO LONGER bolded with `**` in the JSON. Instead, they are defined in the room's `actions` array.
    *   **Matching:** The `name` field of the action must match the exact word (including casing) used in the `description` string.
    *   **Standard Casing:** Use normal sentence casing for these words. If a word is not at the start of a sentence or a proper noun, it should be lowercase.
    *   **Feedback:** If the action has no specific function (like a quest trigger), use `condition_unmet_message` to provide "examine" flavor text.
5.  **Mechanism Before Metaphor:** When causality matters, name the physical action first. Nova removes a governor, sweeps a counter-frequency, and grounds a discharge; the music metaphor can follow once the player understands what happened.
6.  **No Chosen-One Language:** Echoes do not choose, accept, judge, or recognize Nova as a person. Their damaged interfaces detect the Tuning Fork protocol signature written into her nerves. There is no Aethel ancestry or blood compatibility.
7.  **Dramatize the Arc:** At emotional climaxes, prefer a changed action or habit over a speech explaining the theme. A hand staying on a failing ground cable says more than "I have learned that I matter."

### Canon Guardrails for Text

*   Nova is a **22-year-old adult**. Use "woman," "miner," "mechanic," or her name in neutral narration. Diminutives such as "girl" must be deliberate, relationship-specific characterization rather than the narrative default.
*   The **Architects** made the Six Echoes and exploited Aethel consciousness while developing the Great Silence. The Aethel did not make the Echoes.
*   A **Chime** is an identity key and beacon. It can authenticate systems and carry resonance imprints; it never acts as a battery, fuel cell, thruster, or universal key.
*   Ordinary Source use causes pain, sensory distortion, lost seconds, fatigue, and neurological instability. Memories are endangered only by catastrophic failure and are never spent as routine fuel.
*   Gh0st is a distinct person built from a partial derivative of Elara's neural map. "Backup" is Dominion's dehumanizing label; "brother" is the relationship Elara and Gh0st formed.
*   The finale burns out Nova's Source connection while preserving her memories and identity. It opens this labor world and stops Vale; it does not end the wider Dominion.

---

## II. Room Descriptions (The "You" Camera)

*   **Format:** Second Person ("You").
*   **Length:** 2-3 sentences max.
*   **Formula:** [Atmosphere/Smell] + [Dominant Feature/Interactable] + [Exit/Threat].

### The Biome Tones

*   **The Mines/Ship (Industrial):** Friction. Rust. Noise. Things vibrate.
    *   *Bad:* "You are in a messy room."
    *   *Good:* "The air smells of ozone and stale coffee. A **Workbench** rattles against the wall every time the engine cycles."
*   **The Dominion (Corporate):** Sterile. Cold. Silent. Things are smooth and hostile.
    *   *Bad:* "The room is very clean and high-tech."
    *   *Good:* "The **Airlock** hisses open to a floor that looks like frozen milk. It is too clean to walk on. A **Camera** tracks your pulse."
*   **The Source/Ruins (Esoteric):** Synesthesia. Senses are crossed.
    *   *Bad:* "The magic energy is strong here."
    *   *Good:* "The **Mural** shimmers like a waveform finding a speaker. The light coming from the **Archway** tastes like copper."

---

## III. Item Descriptions (Hand-Feel)

*   **Format:** Third Person (Objective but opinionated).
*   **Formula:** [Physical Sensation] + [Who held this last?]

### Categories

*   **Consumables:** Focus on the low quality of life.
    *   *Street Bento:* "Real protein, allegedly. Tastes like rain and hot sauce."
*   **Weapons:** Focus on the intent/weight.
    *   *Pulse Grenade:* "Standard issue mining explosive. Heavy. Designed to crack rocks; equally effective on ribs."
*   **Key Items:** Focus on the "vibe" or "hum."
    *   *The Tuning Fork:* "It hums. Holding it feels like grabbing a live wire that sings."

---

## IV. Dialogue Voices (The Cast)

### Nova (The Protagonist)
*   **Role:** The Mechanic.
*   **Tone:** Scrappy, cynical, protective.
*   **Vocabulary:** Mining slang ("Slag," "Breach," "Core-deep").
*   **The Rule:** She creates friction and understands systems through touch. She does not ask "What is that?"; she asks "What is feeding back?" or "Can I break it?" Her early independence should sound practical, not meek: she takes dangerous work rather than admit she needs help.
*   **Line:** "Zeke, if that thing beeps one more time, I'm putting a laser bolt through it."

### Zeke (The Insider)
*   **Role:** The HR Manual (Panicked).
*   **Tone:** Verbose, anxious, uses corporate buzzwords to hide fear.
*   **Vocabulary:** "Optimization," "Compliance," "Asset," "Liability."
*   **The Rule:** He over-explains. He tries to apply logic to magic.
*   **Line:** "Technically, that beast is violating three separate zoning ordinances. Also, it's eating the hull."

### Orion (The Ancient)
*   **Role:** The Conductor.
*   **Tone:** Melancholic, formal, resonant.
*   **Vocabulary:** Musical/Signal metaphors ("Harmony," "Silence," "Tuning," "Echo").
*   **The Rule:** He never uses contractions. He speaks about the universe like it's a song.
*   **Line:** "The signal is faint here. Like a heartbeat slowing down."

### Gh0st (The Hardware)
*   **Role:** The Operating System.
*   **Tone:** Literal, concise, deadpan.
*   **Vocabulary:** "Target," "Neutralized," "Inefficient," "Directive."
*   **The Rule:** Statements of fact. No emotion until the mask cracks—and even then, show feeling through changed priorities, pauses, and chosen actions before giving him therapeutic self-analysis.
*   **Line:** "I have intimidated the beverage dispenser. It is now brewing."

---

## V. Interaction & Popups (The Feedback Loop)

When the player taps an object, the result text must have weight.

*   **Physical Actions:** Use violent/heavy verbs.
    *   *Bad:* "The door opens."
    *   *Good:* "The hydraulics scream, but the **blast door** grinds open."
*   **Source Actions:** Use dream logic.
    *   *Bad:* "The illusion fades."
    *   *Good:* "The **wall** dissolves into mist. It was never there."
*   **Fail States:** The "Polite Oppressor."
    *   *Context:* Hacking/Tech failures.
    *   *Voice:* Clean, polite, threatening.
    *   *Text:* "Compliance Failure. Personnel unverified. Please remain stationary for arrest."

---

## VI. The Source Glossary (Synesthesia Table)

Use this table when describing Starborn magic (The Source/Relics).

| Physical Sensation | Source Description (The Rewrite) |
| :--- | :--- |
| **Sound** | Described as Texture ("Grit in your teeth," "Sandpaper hum"). |
| **Light** | Described as Sound ("Screaming white," "A quiet blue glow"). |
| **Geometry** | Described as Emotion ("Hateful angles," "A lonely corridor"). |
| **Pain** | Described as Data ("Erosion," "Static," "Signal Lag," "Lost Seconds"). |

---

## VII. UI & System Text (The Corporate Overlay)

The game UI is a Dominion interface. It treats the player as an employee, not a hero.

This is primarily a **text and framing rule**, not a command to make the UI visually severe. Starborn's actual visual language is chibi/anime sci-fi with bold outlines, saturated accents, expressive icons, and readable comic-game panels. The Dominion layer should add paperwork flavor, status labels, and dry compliance language on top of that playful foundation.

*   **Quest Accepted:** "Assignment Logged."
*   **Quest Complete:** "Ticket Closed. Performance Rating: Adequate."
*   **Level Up:** "Asset Value Increased."
*   **Game Over:** "Asset Liability Exceeded. Recycling..."

### UI Visual Tone

*   **Good:** Colorful icon tabs, chunky outlined controls, bright cyan/orange/yellow accents, compact status strips, and clear game-first hierarchy.
*   **Good:** A quest panel that says "Assignment Logged" while still looking like a bold anime adventure interface.
*   **Bad:** Grey-on-grey corporate minimalism that looks like a real enterprise dashboard.
*   **Bad:** Oppressive terminal styling so heavy that it clashes with Nova's chibi portraits and the sticker-like enemy art.

---

## VIII. Example: Putting it Together (The "Golden Sample")

**Room:** The Server Farm (World 5)  
**Context:** Nova enters the freezing cold server room to find Thorne.

**Room Description:**
"The air here is absolute zero. Miles of blue **Server Racks** hang in the void, humming with the data of a billion stolen thoughts. A frosted **Vent** rattles in the corner."

**Interaction (Vent):**
"You kick the grate. It clatters into the darkness. Inside, huddled in the thermal exhaust, is Director Thorne."

**Dialogue (Thorne):**
"You opened every system my probes could not. I was supposed to collect you after the Anchor. Vale moved the schedule."

**Dialogue (Zeke):**
"For clarity, 'collect' means she tracked us through five sectors and planned to put Nova in a lab."

**Dialogue (Nova):**
"Less math, Zeke. Thorne, get up. We're breaking you out of your own jail."

**System Popup (Quest Update):**
OBJECTIVE UPDATED: Escort the Asset (Thorne) to the Anchor Chamber. Note: Asset value is depreciating rapidly.
