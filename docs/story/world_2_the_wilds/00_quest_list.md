# Sector 9 quest and reward reference

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The production catalog contains five main and five side quests. The table distinguishes crafted outputs, dialogue grants, and completion events; do not add the quest metadata rewards again on top of those owners.

| Quest | Implemented objective | Payout / handoff |
|---|---|---|
| `w2_mq01` — A Strange Coast | Check Zeke, examine the pod, use the emergency medkit, and reach the stream. | 150 XP at stream completion; supplies are granted by pod examination, not a second completion payout. |
| `w2_mq02` — The Signal | Clear an accepted canopy encounter, reach Temple Gate, and use the Chime. | 200 XP; facility access and MQ03. No Link unlock here. |
| `w2_mq03` — Sleeping Giant | Read the facility clues, wake Orion, speak to him, and recover the separate Bridge. | 250 XP and `bridge_relic`; Orion joins via his conversation before the cradle claim. |
| `w2_mq04` — The Hunter | Confront Gh0st, defeat the Beast, then complete the grounded drill. | Gh0st joins on Beast victory; drill callback grants `nova_link`, 300 XP, and MQ05. |
| `w2_mq05` — Liftoff | Resolve the Source Gate, inspect the Astra, retrieve conduits, install the Bridge, and depart. | Departure callback grants 350 XP and starts MQ11 at the Spire. |
| `w2_sq01` — Botanist | Scan five plants and return through Zeke's completion dialogue. | 100 XP and two painkillers via Zeke dialogue; no Ecological Insight passive grant. |
| `w2_sq02` — Lost Patrol | Trace three patrol signatures, recover the damaged cutter, and repair it. | Repair produces `thermal_cutter`; completion grants 100 XP. Cache supplies ingredients/schematic and `mod_corrosive_rounds`. |
| `w2_sq03` — Tideglass Day | Visit Tide Pools, gather the supplied materials, and craft Source Resin. | Craft produces `source_resin`; completion grants 150 XP and two painkillers. |
| `w2_sq04` — Ancient Echoes | Recover and seat three crystals, then resolve the mural socket choices. | 150 XP and `focus_conduit`; no Chorus Echo skill or second Bridge relic. |
| `w2_sq05` — Stolen Tech | Blind the vent security, bypass its scan, recover the transmitter, and craft a capacitor. | Craft produces `rapid_capacitor`; completion grants 150 XP. |

## Interpretation boundaries

`quests.json` supplies journal tasks and reward metadata. Events, dialogue triggers, crafting outcomes, and the quest runtime determine actual delivery. Branches and ingredient grants must be inspected before calculating a route budget. An effect's name does not establish a passive bonus or exact numerical value.

## Sources and maintenance

[rooms](../../../app/src/main/assets/rooms.json), [quests](../../../app/src/main/assets/quests.json), [events](../../../app/src/main/assets/events.json), [dialogue](../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
