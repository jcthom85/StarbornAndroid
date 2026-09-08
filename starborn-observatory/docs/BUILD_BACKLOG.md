# Observatory Build Backlog

## Immediate next increment

1. Create a normalized asset index in a standalone JavaScript module.
2. Add a global search that searches IDs, names, descriptions, and source files.
3. Add a source inspector with JSON path and relationship links.
4. Upgrade maps into a three-pane layout: hierarchy, map/art, room details.
5. Add quest cards and a quest-stage timeline.
6. Add a reference graph view for the selected entity.
7. Build first-class enemy, ability, item, character, status, encounter, economy, combat-rule, and tutorial pages.
8. Add asset provenance to every detail view: source file, JSON path, and referenced art/audio.
9. Build the Scenario Lens around a selected room or quest stage.
10. Add combat and economy comparison views for balancing.

## Acceptance tests for the next increment

- A user can find any quest, room, enemy, cinematic, or audio cue by name or ID.
- A selected room shows its source file, art path, description, connections, gates, NPCs, items, and enemies.
- A selected quest shows every known start trigger, stage, task, milestone, and location reference.
- Every displayed relationship can be followed in both directions.
- Missing references are visibly marked rather than silently omitted.
- An enemy, ability, item, or quest can be traced into the rooms, encounters, rules, and assets that use it.
- A selected scenario explains both its prerequisites and its downstream consequences.
- The Android project has no changed files as a result of Observatory development.
