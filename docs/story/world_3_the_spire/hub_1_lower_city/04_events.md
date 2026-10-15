# Lower City progression ownership

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

| Event | Trigger in current record | Ownership / effect |
|---|---|---|
| `w3_mq11_clear_landing` | type=encounter_victory | Records an accepted victory while MQ11 is active; do not invent a separate mandatory enemy identity. |
| `w3_mq11_reach_vent` | type=enter_room, room=spire_vent_output | Requires landing task; advances to safehouse stage and reveals The Static. |
| `w3_mq11_find_apartment` | type=enter_room, room=spire_zekes_apartment | Records finding Zeke's apartment. Zeke is already in the travelling crew. |
| `w3_mq11_establish_shield` | type=player_action, action=w3_mq11_establish_shield | Finishes MQ11, starts MQ12, reveals market/plaza routes. |
| `w3_mq12_assemble_planning` | type=player_action, action=w3_mq12_assemble_planning | Requires blueprint task, finishes MQ12. |
| `w3_mq12_to_w3_mq13` | type=quest_stage_complete, quest_id=w3_mq12 | Guarded completion handoff starts/tracks MQ13, reveals Laundry/Skypark, warps to Laundry. |

The preparation tasks are Jax information, patrol mapping, security records, badge copying, disguise materials, and blueprint work. Source their actual locations from the quest and room records. The old reunion scene with a separately waiting Zeke and automatic elevator dialogue are historical scripts, not required runtime events.

Side-quest completion owners include `w3_sq11_install_core`, `w3_sq12_expose_transfer`, and `w3_sq13_restore_market`. The sign quest consumes the core; the latter two deliver Intercept and the Neon Band respectively. Mandatory Night Market banter and a Streetwise price passive are not established by these events.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
