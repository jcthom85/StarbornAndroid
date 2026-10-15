# Spire implementation reference

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The old First Buildout Pass and 32-room district budget are retired. The implemented Spire has 70 node-member rooms, five main quests, and six side quests in this snapshot.

Use [the world reference](world_3_the_spire/README.md) for the actual sequence and grants, then local hub files for current node membership, cast placement, and event ownership. Keep the heist's legibility, class contrast, preparation, and visible consequences as editing goals without treating old proposed mechanics as missing code.

Future prose work should ground the Gallery in visible light, shutters, containment, and routing. Changing its interaction is a scoped redesign; no alert meter, playable party split, new room budget, or invented reward passive is authorized by this reference. The original blueprint is preserved in the world-document archive.

## Sources and maintenance

[rooms](../../app/src/main/assets/rooms.json), [quests](../../app/src/main/assets/quests.json), [events](../../app/src/main/assets/events.json), [dialogue](../../app/src/main/assets/dialogue.json), [hub_nodes](../../app/src/main/assets/hub_nodes.json). [Documentation index](../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
