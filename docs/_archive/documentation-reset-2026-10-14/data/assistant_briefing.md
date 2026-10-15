# Starborn: Project Briefing & Lore

## Core Theme: Cosmic Resonance
The universe is not digital or simulated—it is **acoustic**. Reality is a "Song", and the "Source" is a vast, resonant Ocean.
- **Metaphors:** Sound, Vibration, Frequency, Ripples, Standing Waves, Chords, Harmony/Dissonance.
- **The Source:** An infinite Ocean of pure resonance.
- **Humanity:** "Ice" frozen on the surface of the Ocean. When humans die, they "melt" back into the Source.
- **Aethel:** "Currents" or "Chorus" within the Source.
- **Magic/Tech:** "Resonance" technology manipulates these frequencies. "Source Arts" are powerful, dissonant chords.

## Key Mechanics
### 1. Erosion (Replaces Hunger)
- **Concept:** Using Source power or enduring cosmic stress causes "Erosion" (neurological burnout/fading).
- **Effect:** High Erosion reduces max HP/Focus.
- **Recovery:** Restoring Resonance requires "tuning" (meditation, specific consumables like 'Neural Stabilizer', or 'comfort food' that resonates with memory).
- **Narrative:** It is the cost of touching the Source—you risk melting back into it.

### 2. Cooldown System (Replaces Source Points)
- **Combat:** Skills use a **Cooldown (Turn-based)** system, not a mana pool.
- **Pacing:** Combat is rhythmic. Powerful skills have long cooldowns (3-5 turns).
- **Strategy:** Managing cooldowns and rotating skills is key. "Snack Slot" items (5-turn CD) provide burst healing or buffs.

### 3. Tinkering & Crafting
- **Tinkering:** Modifying gear with components to alter its resonance properties.
- **Mods:** "Tuning forks" for weapons/armor.
- **Cooking:** Preparing food that lowers Erosion or provides temporary Resonance buffs.

## Visual Style
- **Characters:** Chibi sprites (in-game) + Anime Bust Portraits (dialogue).
- **Environment:** Modern Anime style. Clean, atmospheric, with visible "Resonance" effects (spectral ripples, glowing cymatic patterns).
- **UI:** Minimalist, mobile-first.
- **Acoustic Cues:** "Aura Colors" represent different chords/frequencies (e.g., C#m7 = Indigo, G Major = Gold).

### Art Production
- Master instructions: `docs/story/Starborn_Art_Production_Guide.md`.
- Read the master guide before generating room backgrounds, hub maps, node icons, title art, character sprites, portraits, or enemy sprites.
- Canonical default: `gpt-image-2`, `quality=low`; use the asset-specific dimensions and transparency workflow in the guide.
- Do not wire generated art into game data unless the user explicitly requests wiring.

### Enemy Sprite Production
- Canonical instructions: `docs/story/Enemy_Sprite_Generation_Guide.md`.
- Required baseline: `gpt-image-2`, `quality=low`, `1024x1024`, flat `#00ff00` chroma-key source, then local soft-matte/despill removal to transparent RGBA PNG.
- Match the approved World 1 reference set listed in that guide.
- Do not wire generated sprites into game data unless the user explicitly requests it.

## Characters
- **Nova:** Rogue/Scavenger. Spiky black hair, purple eyes. The "Tuner".
- **Zeke:** Support/Tank. Blond, blue jacket. The "Anchor".
- **Orion:** Scientist/Mage. White hair, glowing blue eyes. The "Resonator".
- **Gh0st:** Assassin/Tech. Hooded, red/black. The "Silencer".

## World
- **The Static:** (Formerly 'The Glitch Bar') A hub where drifters gather.
- **Elara's Song:** (Formerly 'Elara Protocol') A mysterious, omnipresent frequency that binds the world.
- **Vale:** The antagonist who seeks to "melt" humanity back into the Source.
