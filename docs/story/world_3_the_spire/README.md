# Spire: implemented world reference

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

## Route and scope

World 3 has Lower City (`hub_5_lower_city`) and Upper City (`hub_6_upper_city`). Current node membership contains 37 and 33 rooms respectively. The old 32-room buildout budget is retired. The catalog has five main quests and six side quests, including the Skyline Salon murder investigation.

The crew arrives at `spire_sewers_landing` after Sector 9 departure. Clear an accepted arrival encounter, reach Vent Output, locate the apartment, and establish its shield. Gather the six preparation tasks and commit the planning table. MQ12's guarded completion handoff starts MQ13 and warps to Laundry Service. Guest access, scanner work, and the service route lead to the Archive.

The Lens sequence reads containment, shutters, and command tethers before resolving the Gallery. Claiming the Lens grants Scan and starts the lockdown quest. Escape requires the Administrator victory, a Scan of the launch window, and launch to the Foundry. Current text sometimes calls this the Shield window; retained story distinguishes the internal curtain crossing from the later outer-Shield breach. Do not add an orbital excursion to reconcile old prose.

## Quests and actual grants

| Quest | Implemented objective | Payout / handoff |
|---|---|---|
| `w3_mq11` — Homecoming | Clear the landing encounter, climb to Vent Output, and establish the apartment shield. | 400 XP; MQ12 and local route reveals. |
| `w3_mq12` — The Plan | Gather six preparation tasks, then commit the heist at the planning table. | 450 XP; the guarded completion handoff starts MQ13 and warps to Laundry Service. |
| `w3_mq13` — Social Engineering | Obtain Skypark guest access, blind Laundry scanners, and use the service route. | 500 XP; MQ14 and Archive reveal. |
| `w3_mq14` — The Lens | Read the field, shutters, and tethers, resolve the Gallery, and take the Lens. | 550 XP, `the_lens`, and `source_art_scan`; starts MQ15. |
| `w3_mq15` — Burn Notice | Reach the roof, defeat the Administrator, Scan the window, and launch. | 700 XP; MQ16 and `foundry_slag_landing` handoff. |
| `w3_sq11` — Neon Fix | Inspect the sign and install a neon sign core. | 200 XP; consumes the installed core. No Streetwise price passive is granted by the completion event. |
| `w3_sq12` — Cold Case | Recover the erased case number, search security records, and expose the transfer. | 200 XP and `gh0st_intercept`. |
| `w3_sq13` — Night Market Run | Trace stolen power, reroute it, and restore the market lights. | 200 XP and `neon_band`. |
| `w3_sq14` — Corporate Espionage | Copy the concierge credential, steal the ledger, and choose disclosure or private leverage. | 250 XP plus `encrypted_ledger` on disclosure or `phase_rounds` on private leverage. Branch events own actual payout. |
| `w3_sq15` — Prototype Testing | Inspect targeting bias, run the prototype trial, and scrub its telemetry. | 300 XP and `phase_rounds` after the telemetry step. |
| `w3_sq16` — Murder in the Skyline Salon | Inspect the glass, terminal timestamps, and comms rig; confront Vance. | 450 XP, `cyano_coolant_injector`, and `skyline_master_passkey` via Vance completion dialogue. |

## Preserve and retire

Keep the contrast between improvised Lower City life and executive access control; let Zeke's history and the crew's preparation have practical weight. Old scene scripts are not instructions to reintroduce Zeke, force playable split-party control, add an alert meter, or grant one Source Art per companion per relic.

Corporate Espionage has two reward branches; its catalog reward line is not the whole payout contract. Prototype Testing finishes after telemetry cleanup. The murder quest grants its rewards through Vance's completion dialogue. Price passives and exact bonuses from earlier briefs are not established by those old descriptions.

The main Archive puzzle is a milestone/action investigation, distinct from the optional chrono tuning definition in `tuning_puzzles.json`. This pass documents current puzzles without replacing their logic.

## Sources and maintenance

[rooms](../../../app/src/main/assets/rooms.json), [quests](../../../app/src/main/assets/quests.json), [events](../../../app/src/main/assets/events.json), [dialogue](../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
