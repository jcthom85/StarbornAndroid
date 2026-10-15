# Story documentation guide

Status: Current navigation/authoring guide, revised October 14, 2026. Start with [the project index](../README.md). Current code/assets and live routes are the default implementation truth; old documents are corrected first when they disagree.

## Responsibilities

- [Master Story](Starborn_Master_Story.md): retained plot, causality, transitions, ending.
- [Lore](Lore_Bible.md): artifact functions, Source constraints, history, and knowledge boundaries.
- [Characters](Characters.md) and [Character Arcs](Character_Arcs.md): people, relationships, intended behavioral development.
- [Writer's Handbook](Starborn_Writer_Handbook.md): prose and voice for current editing.
- [Content Standards](Content_Standards.md): pacing, purpose, interaction and consequence contracts.
- [Systems Overview](Systems_Overview.md): source-backed system map; exact rules remain in code/catalogs.
- [Art Production Guide](Starborn_Art_Production_Guide.md): visual production workflow. Audio/music guides own actual audio production, not all narrative vocabulary.

These roles do not make an older design claim more correct than its implemented replacement. Record an unresolved difference with evidence before changing the game.

## World and hub material

World folders contain story outlines, storybeat packets, and hub-specific overviews, quests, NPCs, locations and events. Their files are supporting designs and historical scripts until reconciled against current assets and routes. Numbered filenames organize topics; they do not establish implementation status. Legacy folder names may differ from displayed world names.

For a world pass:

1. Locate current room, quest, dialogue, event, milestone and ability records.
2. Trace the live public actions and relevant callbacks/gates.
3. Mark document claims implemented, partial, proposal, superseded, or unresolved.
4. Correct the document to reflect the game; separate deliberate future edits from the implementation summary.
5. Change production content only within an approved design/editing scope or a demonstrated defect.
6. Record affected state variants, validation, and remaining human/device acceptance.

## Identifiers and links

Reuse established IDs and schemas. The old scoped-prefix examples are retired; documentation cleanup is not an ID migration. Do not rename actions, milestones, success events, save fields, or content records to make a folder scheme tidy.

Use repository-relative Markdown links for maintained documents. Give each mutable fact one maintained owner and link to it. Keep status/date/evidence boundaries visible. Mark historical instructions before the original body and keep their snapshots out of default authoring context.

The [inventory](../DOCUMENTATION_INVENTORY.md) records review status; [the cleanup record](../DOCUMENTATION_CLEANUP.md) identifies completed work. A pending row is a review task, not a proven defect.
