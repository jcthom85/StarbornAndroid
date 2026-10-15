# Sector 9 exploration reference

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The former expansion specification is retired as an implementation plan. Sector 9 is implemented, and the old 91-room budget, hard-deck ending, prose scripts, and hand-maintained connection lists can disagree with current records.

Use the [world reference](world_2_the_wilds/00_world_story.md), [quests](world_2_the_wilds/00_quest_list.md), and [progression ownership](world_2_the_wilds/00_storybeat_packet.md). Hub location files enumerate actual node membership: 50 jungle rooms and 43 facility rooms in this snapshot. Consult room connections, node transitions, and runtime gate evaluation for movement.

The current stasis, gate, and mural interactions already have clue/choice logic. Further optical/machinery puzzle changes belong to the explicit refinement plan, not restoration of this old expansion spec. The original is preserved in the world-document archive.

## Sources and maintenance

[rooms](../../app/src/main/assets/rooms.json), [quests](../../app/src/main/assets/quests.json), [events](../../app/src/main/assets/events.json), [dialogue](../../app/src/main/assets/dialogue.json), [hub_nodes](../../app/src/main/assets/hub_nodes.json). [Documentation index](../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
