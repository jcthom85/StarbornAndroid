# Lower City: quests

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

Arrival under the Spire, runoff/sewer traversal, The Static and the apartment, market activities, and preparation at Transit Plaza. The crew already travels with Zeke; there is no reunion with a separately waiting Zeke.

| Quest | Implemented objective | Payout / handoff |
|---|---|---|
| `w3_mq11` — Homecoming | Clear the landing encounter, climb to Vent Output, and establish the apartment shield. | 400 XP; MQ12 and local route reveals. |
| `w3_mq12` — The Plan | Gather six preparation tasks, then commit the heist at the planning table. | 450 XP; the guarded completion handoff starts MQ13 and warps to Laundry Service. |
| `w3_sq11` — Neon Fix | Inspect the sign and install a neon sign core. | 200 XP; consumes the installed core. No Streetwise price passive is granted by the completion event. |
| `w3_sq12` — Cold Case | Recover the erased case number, search security records, and expose the transfer. | 200 XP and `gh0st_intercept`. |
| `w3_sq13` — Night Market Run | Trace stolen power, reroute it, and restore the market lights. | 200 XP and `neon_band`. |

Quest ownership by hub is organization, not strict execution order. Respect actual conditions, alternative outcomes, crafting and callback ownership. Old short IDs such as MQ_13/SQ_14 are retired as implementation references.

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
