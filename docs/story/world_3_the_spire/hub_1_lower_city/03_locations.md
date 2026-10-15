# Lower City: node and room map

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The current node catalog contains 5 nodes and 37 distinct room memberships for `hub_5_lower_city`. Membership and entry policy are catalog facts, not a guaranteed linear route or universal hub access.

| Node | Entry room | Rooms | Entry / return policy |
|---|---|---:|---|
| `spire_vent_output` — Vent Output | `spire_vent_output` | 6 | `transition` / `entry` |
| `spire_the_static` — The Static | `spire_the_static` | 10 | `transition` / `entry` |
| `spire_night_market` — Night Market | `spire_night_market` | 7 | `transition` / `entry` |
| `spire_sewers` — The Sewers | `spire_sewers_landing` | 7 | `hub` / `entry` |
| `spire_transit_plaza` — Transit Plaza | `spire_transit_plaza` | 7 | `transition` / `entry` |

## Room membership

Use current room connections, action conditions, node transitions, and progression evaluation for navigation. This replaces old hand-drawn choice diagrams and room-purpose guesses. Titles are current display names, not new creative instructions.

### Vent Output

- `spire_vent_output` — Vent Output
- `spire_rain_gutter_alley` — Rain Gutter Alley
- `spire_underrail_platform` — Underrail Platform
- `spire_vent_output_scale_01` — Condensate Run
- `spire_vent_output_scale_02` — Maintenance Crawl
- `spire_vent_output_scale_03` — Rain Grate Overlook

### The Static

- `spire_the_static` — The Static
- `spire_old_subway_car` — Old Subway Car
- `spire_signal_stairwell` — Signal Stairwell
- `spire_zekes_apartment` — Zeke's Apartment
- `spire_safehouse_roof` — Safehouse Roof
- `spire_the_static_scale_01` — Back Bar Ledger
- `spire_the_static_scale_02` — Roof Stairwell
- `spire_the_static_scale_03` — Tenant Shrine
- `spire_rigging_deck` — Bridgman Rigging Deck
- `spire_hidden_studio` — Secret Signal Workshop

### Night Market

- `spire_night_market` — Night Market
- `spire_noodle_row` — Noodle Row
- `spire_lantern_bridge` — Lantern Bridge
- `spire_backstreet_clinic` — Backstreet Clinic
- `spire_night_market_scale_01` — Lantern Roof
- `spire_night_market_scale_02` — Spice Alley
- `spire_night_market_scale_03` — Rain Cache

### The Sewers

- `spire_sewers_landing` — Sewer Landing Pad
- `spire_runoff_lock` — Runoff Lock
- `spire_sewers_passage` — Sewer Passage
- `spire_maintenance_sluice` — Maintenance Sluice
- `spire_sewers_scale_01` — Pump Gallery
- `spire_sewers_scale_02` — Filter Sluice
- `spire_sewers_scale_03` — Ratline Nook

### Transit Plaza

- `spire_transit_plaza` — Transit Plaza
- `spire_checkpoint_queue` — Checkpoint Queue
- `spire_security_kiosk` — Security Kiosk
- `spire_elevator_service_gate` — Elevator Service Gate
- `spire_transit_plaza_scale_01` — Ad Bay
- `spire_transit_plaza_scale_02` — Ticket Shadow
- `spire_transit_plaza_scale_03` — Underrail Map Room

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
