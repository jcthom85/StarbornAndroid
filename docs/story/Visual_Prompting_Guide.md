# Starborn — Visual Prompting Guide

**Status:** Canon  
**Purpose:** To define the exact format and content standards for generating room background descriptions for the image generator.

All image-generation workflows are indexed in
`docs/story/Starborn_Art_Production_Guide.md`. Enemy combat sprites also have
the specialized `docs/story/Enemy_Sprite_Generation_Guide.md`.

---

## 0. Starborn Visual North Star

Starborn is **bold anime/comic sci-fi adventure**, not grim realistic dystopia. The story can be harsh, but the visual language stays readable, colorful, and slightly playful so it matches the chibi/anime character sprites, expressive enemy art, and graphic storybook backgrounds.

Use these visual instincts when describing places:
*   **Readable silhouettes:** Rooms should have clear dominant shapes, strong foreground/midground/background separation, and easy-to-read landmarks.
*   **Bold color blocks:** Saturated cyan, teal, orange, yellow, red, and warm industrial light are welcome. Dark panels and shadows should frame color, not smother it.
*   **Thick-outline thinking:** Even when prompts cannot ask for a specific style, objects should be described with chunky forms, clear edges, and iconic shapes that support the existing sticker/comic asset language.
*   **Stylized stakes:** Dominion pressure, labor exploitation, rust, and machinery are real, but they are presented through heightened anime/comic adventure clarity rather than documentary grit.

The shorthand target is: **chibi anime adventure UI and backgrounds wearing a light Dominion work-terminal costume.**

---

## 1. The "Pure Description" Format
Prompts must be delivered as a single paragraph of raw descriptive text. 
*   **NO** visual style info (e.g., "digital painting", "high-res").
*   **NO** aspect ratio or technical metadata.
*   **NO** "exclusions" list (e.g., "No people").
*   **NO** conversational filler (e.g., "Here is the prompt").

---

## 2. Content Constraints (Environmental Integrity)

### A. The "Ghost Town" Rule
The background image represents the **stage**, not the **actors**.
*   **No NPCs:** Do not describe people or living creatures.
*   **No Animals or Creatures:** Do not describe pets, wildlife, pests, monsters, drones-as-creatures, or background silhouettes that read as living beings unless the room spec explicitly requires them.
*   **No Loose Items:** Do not describe lootable items (medkits, scrap, weapons).
*   **No Dynamic Objects:** Do not describe anything the player is meant to interact with unless it is a permanent architectural feature (e.g., a bolted-down workbench is fine; a glowing quest-item on top of it is not).
*   **No Readable Writing:** Do not describe legible signs, labels, posters, numbers, UI text, graffiti, or written notices unless the room spec explicitly requires readable text. Use non-readable shapes instead: blank warning placards, colored hazard bands, icon-like marks, scuffed decals, or unreadable corporate placards.

### B. Explicit-Spec Override
People, animals/creatures, and readable writing are allowed **only** when the room, story beat, or asset request explicitly calls for them. If allowed, describe exactly what must appear and keep it limited to that purpose. Otherwise, assume every room background is empty, silent, and staged for gameplay.

### C. The "Blue Collar Cosmic" Palette
Every description must pull from this texture and lighting library to ensure consistency. "Blue Collar Cosmic" means **stylized industrial adventure**, not muddy realism.
*   **Materials:** Scratched steel, rusted iron, reinforced glass, cold rock, grease-slicked cables, industrial nylon, stained concrete.
*   **Lighting:** Flickering amber LEDs, sickly fluorescent whites, harsh shadows, long light-shafts through dust, deep orange glows from machinery.
*   **State:** Worn, dented, leaking, patched-over, vibrating, claustrophobic.
*   **Graphic treatment:** Chunky machinery, bold hazard stripes, crisp signage, glowing readouts, and clean silhouettes that remain readable on mobile.

### D. The Tone Balance Rule
Do not let "oppressive" become visually joyless. Starborn backgrounds should support a colorful chibi/anime cast. A room can be dangerous, cramped, or exploitative while still using punchy color, strong shapes, and a clear adventure-game composition.

---

## 3. Composition & Perspective
*   **Perspective:** Always First-Person (as if the player is standing in the entry point).
*   **Focus:** Center the "Dominant Feature" of the room description.
*   **Scale:** Emphasize the cramped, industrial scale for miner areas and the vast, cold scale for Dominion/Architect areas.

---

## 4. Example: The Standard

### Correct (Pure Description)
> A first-person view into a compact industrial communal mess hall with bold yellow wall stripes, chunky bolted tables, and a bright cracked monitor glowing above the far bench. Exposed pipes curve across the low ceiling, amber LEDs flicker through orange dust, and patched steel plates break the rough rock walls into clear graphic shapes. The room feels worn and overworked, but the teal monitor light, rust-orange floor marks, and crisp hazard labels make the space easy to read at a glance.

### Incorrect (Contains metadata and style)
> Digital painting of a mess hall in 16:9. Use Blue Collar Cosmic style. Gritty and dark. No people, no food. Include tables and a monitor. Cinematic lighting.
