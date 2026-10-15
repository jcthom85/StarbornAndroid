# Upper City progression ownership

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

| Event | Trigger in current record | Ownership / effect |
|---|---|---|
| `w3_mq13_blend_in` | type=player_action, action=w3_mq13_blend_in | Records Skypark guest access. |
| `w3_mq13_disable_sensors` | type=player_action, action=w3_mq13_disable_sensors | Requires guest access; records scanner work. |
| `w3_mq13_enter_lobby` | type=player_action, action=w3_mq13_enter_lobby | Requires access and scanner tasks; finishes MQ13 and starts MQ14. |
| `w3_mq14_solve_light_puzzle` | type=player_action, action=w3_mq14_solve_light_puzzle | Requires Archive entry plus field/shutters/tethers milestones; no three-slider solver in this event. |
| `w3_mq14_take_lens` | type=player_action, action=w3_mq14_take_lens | Requires Gallery resolution; grants Lens and Scan, finishes MQ14, starts MQ15. |
| `w3_mq15_defeat_administrator` | type=encounter_victory, enemies=['administrator_boss'] | Records Administrator victory and advances launch stage. |
| `w3_mq15_launch_astra` | type=player_action, action=w3_mq15_launch_astra | Requires boss and Scan tasks; completes MQ15, starts MQ16, warps to Foundry. |

## Dialogue and presentation limits

Orion's puzzle instructions, Zeke's warning, Nova's Lens claim, Thorne's trap dialogue, and Vale's override are authored dialogue records. They are not proof that every camera move, timed six-second escape, possession effect, venting sequence, or sample speech in the old event document is implemented. Trace actual callbacks before changing behavior.

## Side decisions and investigation

`w3_sq14_leak_ledger` and `w3_sq14_weaponize_ledger` finish the disclosure/private-leverage branches with distinct items. `w3_sq15_scrub_telemetry` finishes Prototype Testing after its trial. Skyline Salon records three inspected clues through `w3_sq16_*` actions; `vance_confession_3` supplies completion rewards and milestone. These are separate contracts, not a universal five-side-quest template.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
