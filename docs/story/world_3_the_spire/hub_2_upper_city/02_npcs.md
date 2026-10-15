# Upper City: people and encounter references

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Curator and Lab Terminal have milestone-dependent presence; local NPCs include Director Vance and the Skyline suspects. There is no documented generic VIP sighting alert meter in the inspected route; the earlier alert-level proposal is retired.

These are authored fixed and conditional room references. Conditions are reported from data, not evaluated for a particular save. Party recruitment/dialogue is covered in the introductory notes.

| Person | Room | Placement |
|---|---|---|
| `botanist_evelyn` — Evelyn | `spire_skypark_dome` | Fixed reference |
| `entrepreneur_robert` — Robert | `spire_skypark_dome` | Fixed reference |
| `curator` — Museum Curator | `spire_skypark_dome` | requires_milestones: ms_w3_mq12_complete; forbidden_milestones: ms_w3_mq13_complete |
| `bartender_dax` — Dax | `spire_skyline_vip_suite` | Fixed reference |
| `director_vance` — Director Vance | `spire_skyline_vip_suite` | Fixed reference |
| `dr_lyra` — Dr. Lyra | `spire_skyline_vip_suite` | Fixed reference |
| `singer_selene` — Selene | `spire_skyline_vip_suite` | Fixed reference |
| `lab_terminal` — Hacked Lab Terminal | `spire_exec_lounge_bar` | requires_milestones: ms_w3_mq13_complete; forbidden_milestones: ms_w3_prototype_tested |

## Encounter references

The room lists and authored spawn events reference the following enemies. Moving encounters, party formations, room overrides, and save state can change the presented fight. Consult current enemy/skill/affinity data and controllers for behavior; the old prose bestiary is retired.

| Enemy ID | Catalog name |
|---|---|
| `administrator_boss` | The Administrator |
| `aero_drone` | Aero Drone |
| `corporate_assassin` | Corporate Assassin |
| `heavy_mech` | Heavy Mech |
| `sentinel_mki` | Sentinel Mk. I |

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
