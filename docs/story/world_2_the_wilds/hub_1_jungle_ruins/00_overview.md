# Jungle Ruins: implemented hub overview

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Crash recovery, wilderness routes, shoreline activities, Temple Gate, and the later Canopy Ridge confrontation. Facility events send the player back here; this is not a self-contained first half completed before Orion.

Use `hub_3_sector9` and current node/room records for navigation. The local quest, cast, and location files describe source contracts; they do not authorize new lock schedules, mandatory scenes, or room expansion.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
