# Starborn content standards

Status: Current authoring standard, revised October 14, 2026. Owns content purpose, pacing, interaction contracts, and consequence. [Writer's Handbook](Starborn_Writer_Handbook.md) owns prose; [Master Story](Starborn_Master_Story.md) owns plot.

## Scope and pacing

The October 14 catalog contains six campaign worlds, 30 main quests, and 32 side quests. Counts are snapshots, not quotas. The old five-side-quests-per-world rule and minimum room counts are retired.

A room earns its place through navigation meaning, discovery, atmosphere, risk, choice, mastery, reward, or character. Combine or remove weak connectors. Later worlds need meaningful development rather than automatic growth in size.

Side quests can reward XP, equipment, abilities, relationships, routes, information, or visible consequences. Check actual payouts and acquisition paths. Optional content should offer possibilities without silently becoming the only viable build or recovery route.

## Encounters and teaching

Use [Encounter Design Standard](Encounter_Design_Standard.md) as supporting guidance, checking current mechanics and world-specific evidence. Compose recognizable tactical questions using available tools and earned abilities. Enemy-count and element-percentage targets are authoring aids, not universal guarantees of fun.

World 1's loader, certification, mine groups, and escape provide its teaching sequence. Check the actual loadout before naming usable elements. The old Physical/Shock/Burn assumption omits the current Cryo Vent introduction. Preserve valid progression with tutorials disabled and legacy saves.

## Interaction contracts

- Required actions must remain discoverable in the description and each applicable variant. Supported explicit links such as `[action:existing action name|natural display text]` can separate display wording from identity. Check `RoomPresentation.kt` and validators for actual behavior.
- Use normal casing and useful descriptions. Avoid decorative clauses inserted solely to satisfy keywords.
- Match a generic action's presented promise to its actual consequence.
- Keep immediate feedback concise; use persistent variants for lasting changes.
- Update copy and visibility together when actions retire or routes change. Check the public route.
- Preserve stable IDs, success events, quest gates, rewards, and save compatibility during editing.

## Choices and world response

Supply enough evidence to understand a decision. Distinct branches should produce observable differences worth remembering; a milestone may need stronger presentation or later acknowledgment.

| World | Emphasis |
|---|---|
| Mines | Work, maintenance, quota pressure, compromised access, escape |
| Sector 9 | Survival, strange ruins, new companions, repairing a way out |
| Spire | Reconnaissance, class barriers, credentials, heist consequences |
| Foundry | Connected machinery, worker danger, Gh0st's choices, ship engineering |
| Orbital Ring | Confinement, luxury, surveillance, Elara, failing control |
| Source | Personal history, unfamiliar physical behavior, chosen cooperation |

## Puzzles and completion

Give players evidence, an understandable manipulation, feedback, and a consequence. Different labels on target-value sliders do not establish different reasoning tasks. Essential clues need alternatives to sound, color, fine gestures, and reaction speed.

[The refinement plan](../design/SOURCE_LANGUAGE_AND_PUZZLE_REFINEMENT_PLAN.md) describes direction and proposed replacements. Replacements remain proposals until implemented and verified.

Record affected sequences, observable changes, checks, and remaining acceptance. Verify initial and changed states, relevant branches, and save/load boundaries. Automated public routes demonstrate specific runtime coverage. Blind-player understanding and physical-device feel require separate evidence.
