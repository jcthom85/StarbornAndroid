# Navigation repair — 2026-09-14

Implemented the corrections identified in the [baseline audit](navigation-topology-audit.md).

## Result

- All local compass connections now agree with their map coordinates, including The Static and Hall of Echoes.
- No rooms overlap within a node; cardinal corridors do not pass through unrelated room markers.
- No cardinal inbound funnels remain.
- Five asymmetric pairs now use named, reciprocal diagonal passages.
- The four original one-way shortcuts retain their destinations and alternate return routes, with explicit “one way” descent controls. They do not masquerade as reversible cardinal movement.
- Every one of the original 928 directed room-to-room connections is preserved. All 126 transition records match the repaired connections.

The stream/beach loop in Sector 9 could not fit a consistent compass layout with its original direction labels. Cliffs↔Cove now uses north/south; Cove↔Pools and Pools↔Cargo have their east/west labels reversed at both ends. No connections or gates were removed. World 2 remains entirely cardinal.

Room coordinates were repacked to eliminate reversed directions and reduce oversized gaps. The Static, Signal Stairwell, Zeke's Apartment, and Safehouse Roof now increase Y going north. Some longer corridors remain; the minimap still uses its existing two-cell display radius.

## Progression preservation

A semantic comparison against the pre-repair assets verified identical room IDs, destination lists, actions, enemies, gates, state, and narrative fields. Only positions, connection direction keys, and named special-exit labels changed in rooms.json. The corresponding direction fields and IDs changed in node_transitions.json. Quests, events, milestones, and node ownership were unchanged.

The runtime now shares a directional gate with a destination only when its opposite exit actually returns to the source. Minimap previews derive return hints from real connections rather than inventing an opposite exit for a one-way arrival.

## Validation

63 targeted unit tests passed across NavigationIntegrityTest, SpecialExitIntegrityTest, DataIntegrityTest, MapStateBuilderTest, ExplorationViewModelTest, ProgressionIntegrityTest, QuestRuntimeManagerTest, NodeProgressionEvaluatorTest, and QuestPayoutIntegrityTest.

`:app:assembleDebug` succeeded. The packaged APK's rooms, node transitions, quests, and events were compared with the verified source assets and match.

The new regression checks cover opposite returns, the exact four named one-way exceptions and their escape paths, cardinal funnels, compass geometry, coordinate overlap, corridors passing through unrelated rooms, and unrelated gates accidentally unlocking.

Run `python navigation_audit.py --check` from the repository root for the current static audit. `--json` retains the raw non-reciprocal inventory, including the four documented one-way passages.

Validation establishes static graph and targeted runtime correctness; it is not a complete six-world manual playthrough.
