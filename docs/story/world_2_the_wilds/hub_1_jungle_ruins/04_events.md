# Sector 9 scenes and interaction boundaries

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

## Implemented scene references

| Boundary | Current owner | Meaning |
|---|---|---|
| Orion wakes and joins | Stasis Console choices, alignment success action, Orion awakening dialogue, then the Bridge claim | Waking, recruiting, and recovering the separate relic are distinct steps |
| Gh0st turns and Beast attacks | Hunter confrontation dialogue/actions and the authored Beast encounter | Protection is a voluntary choice; old repeated weapon-lock/protection-directive scripts are retired |
| Grounded drill | `scene_anchor_drill`, owned by `w2_mq04_anchor_drill` | Completion callback unlocks Link; not an automatic first-rest scene |
| Departure | `w2_departure_entry` and `scene_w2_astra_departure` | Player can stay; confirmed departure uses the low airlane and hands off beneath the Spire |

## Puzzle boundaries

Stasis requires mural, pod, overview, and coolant clue milestones. The console asks pressure, separate memory bands, then safe return. Correct completion dispatches `w2_mq03_apply_alignment`; wrong answers explain and retry.

Source Gate preparation covers horn, grounded cup, pressure gauge, and breakers. Its console asks for root relief into the receiver and isolation of the lock supply while relief stays open. Success dispatches `w2_mq05_resolve_gate`.

Ancient Echoes requires the collected crystals and their seated milestones. Mural Array dialogue assigns west to foundation, east to paired arcs, and north to star. `w2_sq04_restore_chorus` owns the Focus Conduit/XP completion. It is not a C#m7 frequency input or Chorus Echo unlock.

These source descriptions do not endorse every existing acoustic phrase. The lighter prose direction is a subsequent content-editing pass. Preserve stable action IDs and callback behavior when rewriting displayed language. See the world progression reference for exact event ownership.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
