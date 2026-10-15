# Sector 9 Ruins: people and encounter references

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The facility contains fixed local characters and milestone-dependent Zeke/Orion presence. Architects built the underlying systems and Aethel later adapted them. Avoid deriving enemy weaknesses from the old bestiary prose.

These are authored fixed and conditional room references. Conditions are reported from data, not evaluated for a particular save. Party recruitment/dialogue is covered in the introductory notes.

| Person | Room | Placement |
|---|---|---|
| `caretaker_drone` — Caretaker Echo | `sector9_archive_reading_room` | Fixed reference |
| `zeke` — Zeke | `sector9_stasis_chamber` | requires_milestones: ms_w2_mq01_complete; forbidden_milestones: ms_w2_mq03_complete |
| `orion` — Orion | `sector9_stasis_chamber` | requires_milestones: ms_w2_stasis_awake; forbidden_milestones: ms_w2_mq03_complete |
| `orion` — Orion | `sector9_source_gate` | requires_milestones: ms_w2_mq03_complete; forbidden_milestones: ms_w2_mq05_complete |
| `zeke` — Zeke | `sector9_hangar_bay` | requires_milestones: ms_w2_mq03_complete; forbidden_milestones: ms_w2_mq05_complete |

## Encounter references

The room lists and authored spawn events reference the following enemies. Moving encounters, party formations, room overrides, and save state can change the presented fight. Consult current enemy/skill/affinity data and controllers for behavior; the old prose bestiary is retired.

| Enemy ID | Catalog name |
|---|---|
| `ruin_guardian` | Ruin-Guardian |
| `sentinel_orb` | Sentinel Orb |
| `spore_spitter` | Spore-Spitter |

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
