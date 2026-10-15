# Starborn: authoring brief

Updated October 14, 2026. Start with [the documentation index](../docs/README.md) for current evidence, authorities, and pending work.

PythonDevKit loads this text directly into authoring context. Essential direction is included here because linked documents are not automatically supplied to the model. Include the relevant authority and current records for detailed tasks.

Current code, assets, and live routes are the default implementation truth. Most older documents predate the implemented game. Correct or retire a conflicting document first; do not change working logic to match an old spec. A code change needs evidence of an actual defect or an explicitly chosen redesign.

## The game

Starborn is a mobile-first, portrait science-fiction RPG about a colony mechanic escaping corporate ownership and building a crew. Exploration uses illustrated rooms and tappable prose. Combat uses ATB, character-turn cooldowns, weaknesses, stability breaks, equipment, and preparation. Android and Windows desktop share gameplay in `gameRuntime`; their interfaces need separate acceptance.

Write about people doing things: repairing a cutter, hiding a record, sharing food, opening a dangerous route, choosing who to protect. Keep the colorful anime/comic adventure identity alongside labor pressure, humor, and unsettling discoveries.

## Source language

The Source is a dangerous phenomenon connected to consciousness and changes in physical reality. Sound and vibration can accompany contact. Musical imagery is occasional and character-specific; it does not explain every machine, feeling, color, meal, modification, or battle.

Describe observable effects before interpretations. Let characters be uncertain. Nova speaks through practical observation; Zeke understands procedure and access; Gh0st assesses threats and increasingly volunteers preferences; Orion can use cultural or musical language without explaining every mystery.

Retain established names where useful: the Tuning Fork, Chime, Chorus, and Project Harmony. Ordinary machine noise, actual music, and personal musical objects remain appropriate. Avoid stacked frequencies, chords, harmonics, tuning, or songs when a direct description communicates the event.

## Essential story constraints

- Nova is a 22-year-old human miner and mechanic. Her risky cutter modification wakes the Fork; observation and counter-tuning let her survive. No ancestry, prophecy, prior Chime exposure, or relic selection explains her ability.
- The Fork writes a protocol signature into her nervous system. The Cryo-Inductor fuses; the Flux Liner loses its sacrificial ground strip but remains equipped.
- Orion's Chime is an identity key, paired beacon, and imprint carrier. Launch craft, Sanctuary cells, and the Astra provide their own power.
- The Bridge is a separate Architect relic connecting distinct willing minds without merging identities.
- Architects built the six Echoes and exploited Aethel consciousness. Aethel survived, adapted, and operated some systems.
- Gh0st is a distinct person built around a partial derivative of Elara's neural map, with his own identity and no inherited memories. A failsafe pauses one lethal action; protecting the crew is his choice.
- Source use causes bodily and neurological strain. Routine use does not consume memories. The ending costs Nova her Source access while preserving identity and memory.
- Thorne seeks a monopoly. Vale takes control and attempts to eliminate individuality. The ending creates a regional opening; the wider Dominion survives.

## Mechanics and authoring boundaries

Skills use cooldowns rather than mana. Weaknesses can accelerate cooldowns and help create Break openings. Equipped snacks have reusable cooldown abilities; meals and ordinary healing supplies have different consumption rules. Follow the actual catalog and [systems overview](../docs/story/Systems_Overview.md). Do not invent a persistent Erosion survival meter, universal twelve-skill kit, or musical color system.

Preserve IDs, event ownership, progression gates, rewards, and save compatibility during prose work. Keep required actions discoverable in every applicable room variant, using supported explicit action links when display wording differs. Old world/hub scripts are supporting drafts until reconciled. Plans and test records do not prove implementation or device acceptance.

## Task references

- Plot: [Master Story](../docs/story/Starborn_Master_Story.md). Source and relic constraints: [Lore Bible](../docs/story/Lore_Bible.md).
- People: [Characters](../docs/story/Characters.md), [Character Arcs](../docs/story/Character_Arcs.md).
- Prose: [Writer's Handbook](../docs/story/Starborn_Writer_Handbook.md). Structure: [Content Standards](../docs/story/Content_Standards.md).
- Art: read [Art Production Guide](../docs/story/Starborn_Art_Production_Guide.md) before generating assets and [Enemy Sprite Guide](../docs/story/Enemy_Sprite_Generation_Guide.md) for enemies. Preserve their model, dimensions, transparency, and reference workflows. Do not wire generated art unless the user requests it.

[Current State](../docs/CURRENT_STATE.md) records a dated implementation snapshot. Historical handoffs are evidence of earlier decisions, not active instructions.
