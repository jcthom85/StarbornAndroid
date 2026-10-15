# Sector 9 progression ownership

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

This replaces Build Prep scripts. It records existing boundaries without inventing dialogue, scenes, or prerequisite changes.

| Event | Trigger in current record | Ownership / effect |
|---|---|---|
| `w2_mq01_examine_pod` | type=player_action, action=w2_mq01_examine_pod | Requires checking Zeke; grants recovered Chime and emergency/starter supplies. |
| `w2_mq01_stabilize_zeke` | type=player_action, action=w2_mq01_stabilize_zeke | Consumes one medkit and records stabilization. |
| `w2_mq01_explore_stream` | type=enter_room, room=sector9_landing_stream | Completes MQ01 and starts/tracks MQ02 after its prerequisites. |
| `w2_mq02_unlock_temple` | type=player_action, action=w2_mq02_use_chime | Completes MQ02 and warps to Hall of Echoes; no Link grant. |
| `w2_mq03_align_complete` | type=player_action, action=w2_mq03_align_complete | Opens Stasis Console dialogue after four clue milestones. |
| `w2_mq03_apply_alignment` | type=player_action, action=w2_mq03_apply_alignment | Successful dialogue choice marks the stasis rings resolved. |
| `w2_mq03_talk_to_orion` | type=player_action, action=w2_mq03_talk_to_orion | Records the conversation and adds Orion. |
| `w2_mq03_recover_bridge` | type=player_action, action=w2_mq03_recover_bridge | Grants Bridge, completes MQ03, and starts MQ04. |
| `w2_mq04_victory` | type=encounter_victory, enemies=['the_beast'], room_id=sector9_canopy_ridge | Beast victory at Canopy Ridge adds Gh0st; does not finish the drill. |
| `w2_mq04_anchor_drill` | type=player_action, action=w2_mq04_anchor_drill | scene_anchor_drill completion callback grants Link and starts MQ05. |
| `w2_mq05_resolve_gate` | type=player_action, action=w2_mq05_resolve_gate | Successful route choice records gate bypass and warps to Hangar. |
| `w2_mq05_reboot` | type=player_action, action=w2_mq05_reboot | Requires Bridge and Chime; consumes Bridge, records installation. |
| `w2_mq05_confirm_departure` | type=player_action, action=w2_mq05_confirm_departure | scene_w2_astra_departure callback finishes MQ05, warps to Spire, starts MQ11. |

The dialogue entries `w2_stasis_entry`, `w2_gate_entry`, and `w2_departure_entry` own choices and warnings. Callbacks matter: opening a scene or winning the Beast fight is not interchangeable with completing the drill/departure. Existing public campaign tests exercise these boundaries; no fresh route run was performed for this documentation pass.

## Sources and maintenance

[rooms](../../../app/src/main/assets/rooms.json), [quests](../../../app/src/main/assets/quests.json), [events](../../../app/src/main/assets/events.json), [dialogue](../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
