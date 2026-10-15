# Upper City: quests

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Guest access, Laundry Service, executive spaces, Archive investigation, Lens theft, prototype work, Skyline Salon mystery, and rooftop escape. Location membership is not an either-Skypark-or-Lounge progression rule.

| Quest | Implemented objective | Payout / handoff |
|---|---|---|
| `w3_mq13` — Social Engineering | Obtain Skypark guest access, blind Laundry scanners, and use the service route. | 500 XP; MQ14 and Archive reveal. |
| `w3_mq14` — The Lens | Read the field, shutters, and tethers, resolve the Gallery, and take the Lens. | 550 XP, `the_lens`, and `source_art_scan`; starts MQ15. |
| `w3_mq15` — Burn Notice | Reach the roof, defeat the Administrator, Scan the window, and launch. | 700 XP; MQ16 and `foundry_slag_landing` handoff. |
| `w3_sq14` — Corporate Espionage | Copy the concierge credential, steal the ledger, and choose disclosure or private leverage. | 250 XP plus `encrypted_ledger` on disclosure or `phase_rounds` on private leverage. Branch events own actual payout. |
| `w3_sq15` — Prototype Testing | Inspect targeting bias, run the prototype trial, and scrub its telemetry. | 300 XP and `phase_rounds` after the telemetry step. |
| `w3_sq16` — Murder in the Skyline Salon | Inspect the glass, terminal timestamps, and comms rig; confront Vance. | 450 XP, `cyano_coolant_injector`, and `skyline_master_passkey` via Vance completion dialogue. |

Quest ownership by hub is organization, not strict execution order. Respect actual conditions, alternative outcomes, crafting and callback ownership. Old short IDs such as MQ_13/SQ_14 are retired as implementation references.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
