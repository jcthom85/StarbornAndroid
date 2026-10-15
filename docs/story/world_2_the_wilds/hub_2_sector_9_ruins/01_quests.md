# Sector 9 Ruins: quests

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Sanctuary investigation, Orion and the Bridge, Source Gate routing, maintenance vents, and the Astra launch. The folder name is legacy organization, not a claim that every ruin was built by Aethel.

| Quest | Implemented objective | Payout / handoff |
|---|---|---|
| `w2_mq03` — Sleeping Giant | Read the facility clues, wake Orion, speak to him, and recover the separate Bridge. | 250 XP and `bridge_relic`; Orion joins via his conversation before the cradle claim. |
| `w2_mq05` — Liftoff | Resolve the Source Gate, inspect the Astra, retrieve conduits, install the Bridge, and depart. | Departure callback grants 350 XP and starts MQ11 at the Spire. |
| `w2_sq04` — Ancient Echoes | Recover and seat three crystals, then resolve the mural socket choices. | 150 XP and `focus_conduit`; no Chorus Echo skill or second Bridge relic. |
| `w2_sq05` — Stolen Tech | Blind the vent security, bypass its scan, recover the transmitter, and craft a capacitor. | Craft produces `rapid_capacitor`; completion grants 150 XP. |

Quest ownership by hub is organization, not strict execution order. Respect actual conditions, alternative outcomes, crafting and callback ownership. Old short IDs such as MQ_13/SQ_14 are retired as implementation references.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
