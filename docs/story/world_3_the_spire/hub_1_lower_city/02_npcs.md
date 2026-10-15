# Lower City: people and encounter references

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Jax and Mika appear through conditioned room presence. The district also has fixed local NPCs. Zeke is an existing companion. Earlier scripted barks and appearance notes are not implemented dialogue contracts.

These are authored fixed and conditional room references. Conditions are reported from data, not evaluated for a particular save. Party recruitment/dialogue is covered in the introductory notes.

| Person | Room | Placement |
|---|---|---|
| `jax` — Jax | `spire_the_static` | requires_milestones: ms_w2_mq05_complete |
| `musician_chris` — Chris | `spire_old_subway_car` | Fixed reference |
| `musician_andrew` — Andrew | `spire_old_subway_car` | Fixed reference |
| `zeke` — Zeke | `spire_zekes_apartment` | requires_milestones: ms_w3_mq11_complete |
| `contractor_cheno` — Cheno | `spire_rigging_deck` | Fixed reference |
| `creator_jason` — Jason | `spire_hidden_studio` | Fixed reference |
| `feline_meeko` — Meeko | `spire_hidden_studio` | Fixed reference |
| `mika` — Mika | `spire_night_market` | requires_milestones: ms_w2_mq05_complete |
| `noodle_cook_ren` — Ren | `spire_noodle_row` | Fixed reference |

## Encounter references

The room lists and authored spawn events reference the following enemies. Moving encounters, party formations, room overrides, and save state can change the presented fight. Consult current enemy/skill/affinity data and controllers for behavior; the old prose bestiary is retired.

| Enemy ID | Catalog name |
|---|---|
| `sewer_crawler` | Sewer Crawler |

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
