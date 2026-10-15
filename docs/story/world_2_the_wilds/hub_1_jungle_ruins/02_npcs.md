# Jungle Ruins: people and encounter references

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Nova and Zeke enter together. Orion joins in the facility before the active Hunter quest returns to this hub; Gh0st joins after the Beast fight. Consult actual conditioned dialogue instead of the old useless-Zeke and forced-protection scripts.

These are authored fixed and conditional room references. Conditions are reported from data, not evaluated for a particular save. Party recruitment/dialogue is covered in the introductory notes.

| Person | Room | Placement |
|---|---|---|
| `zeke` — Zeke | `sector9_crash_site` | requires_milestones: ms_w1_mq05_complete; forbidden_milestones: ms_w2_mq01_complete |
| `creature_charlie` — Charlie | `sector9_landing_glade` | Fixed reference |
| `chef_david` — David | `sector9_stream_falls` | Fixed reference |
| `hound_penny` — Penny | `sector9_stream_falls` | Fixed reference |
| `scout_kaelen` — Scout Kaelen | `sector9_beach_cove` | Fixed reference |
| `clinician_megan` — Megan | `sector9_coastal_research_station` | Fixed reference |
| `sentinel_3` — Sentinel-3 | `sector9_temple_lock_chamber` | Fixed reference |

## Encounter references

The room lists and authored spawn events reference the following enemies. Moving encounters, party formations, room overrides, and save state can change the presented fight. Consult current enemy/skill/affinity data and controllers for behavior; the old prose bestiary is retired.

| Enemy ID | Catalog name |
|---|---|
| `echo_borer` | Echo-Borer |
| `razor_vine` | Razor-Vine |
| `shard_hound` | Shard-Hound |
| `siren_skimmer` | Siren Skimmer |
| `spore_spitter` | Spore-Spitter |
| `stalker_vine` | Stalker-Vine |
| `the_beast` | The Source Beast |

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
