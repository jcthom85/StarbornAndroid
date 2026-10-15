# Astra room map

Status: Current topology reference, checked against `rooms.json` October 14, 2026. These are five rooms, not five independent hub nodes. [Functionality](01_functionality.md) owns the facility descriptions.

| Room ID | Connections | Player purpose |
|---|---|---|
| `astra_bridge` | South to Common Room | Navigation and Chime archive |
| `astra_common_room` | North to Bridge; east to Quarters; west to Cargo; south to Simulation Deck | Crew check-in and galley |
| `astra_quarters` | West to Common Room | Rest and crew downtime |
| `astra_cargo_bay` | East to Common Room | Cargo records, tinkering, and disembarkation |
| `astra_simulation_deck` | North to Common Room | Combat archive and recreation console |

The cargo records panel contains supply, artifact, and catch sections. Recreation groups films, installed cabinets, and prizes rather than exposing every activity as another room hotspot. Availability and exact labels are resolved by runtime state; preserve legacy action handlers needed by old routes and saves.

Keep circulation short and make new crew material easy to notice. Existing room variants acknowledge ship repair and activity progress. New artwork, rooms, or lock schedules require their own scoped change; this map does not promise them.
