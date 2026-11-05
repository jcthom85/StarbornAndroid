# Navigation repair — 2026-09-14

World 6 is now reviewed: [November 5 Source navigation report](testing/WORLD6_NAVIGATION_2026-11-05.md). The last four diagonal direction entries are removed; the full-game direction policy retains reviewed named vertical routes in Worlds 3 and 5.


November 5 follow-up: [named travel review](testing/NAMED_TRAVEL_REVIEW_2026-11-05.md). Diagonals remain prohibited; reviewed stairs, lifts and cables may supplement the NSEW walking network. Historical findings below predate that correction.


World 5 update, November 5, 2026: Ring travel is now reciprocal NSEW, with a reversible Zero-G/SysAdmin approach and a story-cleared Anchor entrance. See [the World 5 navigation pass](testing/WORLD5_NAVIGATION_2026-11-05.md).

World 4 update, November 5, 2026: the Foundry now uses reciprocal NSEW local routes and a story-cleared maintenance corridor for return travel. See [the World 4 navigation pass](testing/WORLD4_NAVIGATION_2026-11-05.md).

World 3 update, November 5, 2026: local movement now uses reciprocal NSEW connections; its old diagonal passages and three one-way descents are superseded. See [the World 3 navigation pass](testing/WORLD3_NAVIGATION_2026-11-05.md). The earlier findings below remain historical.

Implemented the corrections identified in the [baseline audit](navigation-topology-audit.md).

## Result

- All local compass connections now agree with their map coordinates, including The Static and Hall of Echoes.
- No rooms overlap within a node; cardinal corridors do not pass through unrelated room markers.
- No cardinal inbound funnels remain.
- Five asymmetric pairs now use named, reciprocal diagonal passages.
- The four original one-way shortcuts retain their destinations and alternate return routes, with explicit “one way” descent controls. They do not masquerade as reversible cardinal movement.
- Every one of the original 928 directed room-to-room connections is preserved. All 126 transition records match the repaired connections.

The stream/beach loop in Sector 9 could not fit a consistent compass layout with its original direction labels. Cliffs↔Cove now uses north/south; Cove↔Pools and Pools↔Cargo have their east/west labels reversed at both ends. No connections or gates were removed. World 2 remains entirely cardinal.

Room coordinates were repacked to eliminate reversed directions and reduce oversized gaps. The Static, Signal Stairwell, Static Safehouse, and Safehouse Roof now increase Y going north. Some longer corridors remain; the minimap still uses its existing two-cell display radius.

## Progression preservation

A semantic comparison against the pre-repair assets verified identical room IDs, destination lists, actions, enemies, gates, state, and narrative fields. Only positions, connection direction keys, and named special-exit labels changed in rooms.json. The corresponding direction fields and IDs changed in node_transitions.json. Quests, events, milestones, and node ownership were unchanged.

The runtime now shares a directional gate with a destination only when its opposite exit actually returns to the source. Minimap previews derive return hints from real connections rather than inventing an opposite exit for a one-way arrival.

## Validation

63 targeted unit tests passed across NavigationIntegrityTest, SpecialExitIntegrityTest, DataIntegrityTest, MapStateBuilderTest, ExplorationViewModelTest, ProgressionIntegrityTest, QuestRuntimeManagerTest, NodeProgressionEvaluatorTest, and QuestPayoutIntegrityTest.

`:app:assembleDebug` succeeded. The packaged APK's rooms, node transitions, quests, and events were compared with the verified source assets and match.

The new regression checks cover opposite returns, the exact four named one-way exceptions and their escape paths, cardinal funnels, compass geometry, coordinate overlap, corridors passing through unrelated rooms, and unrelated gates accidentally unlocking.

Run `python navigation_audit.py --check` from the repository root for the current static audit. `--json` retains the raw non-reciprocal inventory, including the four documented one-way passages.

Validation establishes static graph and targeted runtime correctness; it is not a complete six-world manual playthrough.
