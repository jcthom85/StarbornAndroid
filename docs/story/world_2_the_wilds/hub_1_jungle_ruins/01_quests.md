# Jungle Ruins: quests

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Crash recovery, wilderness routes, shoreline activities, Temple Gate, and the later Canopy Ridge confrontation. Facility events send the player back here; this is not a self-contained first half completed before Orion.

| Quest | Implemented objective | Payout / handoff |
|---|---|---|
| `w2_mq01` — A Strange Coast | Check Zeke, examine the pod, use the emergency medkit, and reach the stream. | 150 XP at stream completion; supplies are granted by pod examination, not a second completion payout. |
| `w2_mq02` — The Signal | Clear an accepted canopy encounter, reach Temple Gate, and use the Chime. | 200 XP; facility access and MQ03. No Link unlock here. |
| `w2_mq04` — The Hunter | Confront Gh0st, defeat the Beast, then complete the grounded drill. | Gh0st joins on Beast victory; drill callback grants `nova_link`, 300 XP, and MQ05. |
| `w2_sq01` — Botanist | Scan five plants and return through Zeke's completion dialogue. | 100 XP and two painkillers via Zeke dialogue; no Ecological Insight passive grant. |
| `w2_sq02` — Lost Patrol | Trace three patrol signatures, recover the damaged cutter, and repair it. | Repair produces `thermal_cutter`; completion grants 100 XP. Cache supplies ingredients/schematic and `mod_corrosive_rounds`. |
| `w2_sq03` — Tideglass Day | Visit Tide Pools, gather the supplied materials, and craft Source Resin. | Craft produces `source_resin`; completion grants 150 XP and two painkillers. |

Quest ownership by hub is organization, not strict execution order. Respect actual conditions, alternative outcomes, crafting and callback ownership. Old short IDs such as MQ_13/SQ_14 are retired as implementation references.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
