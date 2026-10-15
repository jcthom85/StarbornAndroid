# Sector 9 Ruins: node and room map

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The current node catalog contains 5 nodes and 43 distinct room memberships for `hub_4_facility`. Membership and entry policy are catalog facts, not a guaranteed linear route or universal hub access.

| Node | Entry room | Rooms | Entry / return policy |
|---|---|---:|---|
| `hall_of_echoes` — Hall of Echoes | `sector9_hall_of_echoes` | 17 | `transition` / `entry` |
| `stasis_chamber` — Stasis Chamber | `sector9_stasis_chamber` | 7 | `transition` / `entry` |
| `source_gate` — Source Gate | `sector9_source_gate` | 7 | `transition` / `entry` |
| `hangar_bay` — Hangar Bay | `sector9_hangar_bay` | 6 | `transition` / `entry` |
| `the_sky` — The Sky | `sector9_the_sky` | 6 | `transition` / `entry` |

## Room membership

Use current room connections, action conditions, node transitions, and progression evaluation for navigation. This replaces old hand-drawn choice diagrams and room-purpose guesses. Titles are current display names, not new creative instructions.

### Hall of Echoes

- `sector9_foyer_grand_hall` — Sanctuary Grand Hall
- `sector9_foyer_left_gallery` — Left Reliquary
- `sector9_foyer_right_gallery` — Right Archives
- `sector9_foyer_security_hub` — Dominion Outpost
- `sector9_foyer_vault_door` — Hangar Portal
- `sector9_conduit_crawlspace` — Maintenance Conduit
- `sector9_conduit_junction` — T-Junction Wiring
- `sector9_conduit_breaker_room` — Breaker Deck
- `sector9_conduit_access_shaft` — Access Shaft
- `sector9_hall_of_echoes` — Hall of Echoes
- `sector9_hall_echo_alcoves` — Echo Alcoves
- `sector9_hall_acoustics_lab` — Acoustics Lab
- `sector9_hall_reflection_pool` — Cymatic Basin
- `sector9_archive_vault` — Archive Vault
- `sector9_archive_reading_room` — Interface Console
- `sector9_archive_tuning_bay` — Tuning Matrix
- `sector9_archive_secret_stash` — Hidden Reliquary

### Stasis Chamber

- `sector9_stasis_chamber` — Stasis Chamber
- `sector9_stasis_ring_array` — Ring Array
- `sector9_stasis_coolant_lines` — Coolant Lines
- `sector9_stasis_observation` — Observation Deck
- `sector9_vents_gantry` — High Gantry
- `sector9_vents_access_grate` — Infiltration Hatch
- `sector9_vents_tech_alcove` — Transmitter Site

### Source Gate

- `sector9_power_turbine` — Source Dynamo
- `sector9_power_manifold` — Distribution Manifold
- `sector9_power_breaker_deck` — Generator Controls
- `sector9_power_valve` — Purge Valve
- `sector9_source_gate` — Source Gate
- `sector9_gate_emitter_left` — Harmonic Horn
- `sector9_gate_emitter_right` — Resonance Cup

### Hangar Bay

- `sector9_hangar_bay` — Hangar Bay
- `sector9_hangar_scaffolding` — Maintenance Catwalks
- `sector9_hangar_staging` — Freight Lift
- `sector9_hangar_tower` — Control Console
- `sector9_hangar_fuel_lines` — Fuel Intake Lines
- `sector9_hangar_lock` — Launch Blast Shield

### The Sky

- `sector9_the_sky` — The Sky
- `sector9_sky_mesosphere` — Mesosphere Storm
- `sector9_sky_shield_threshold` — Shield Threshold
- `sector9_sky_sensor_buoy` — Security Buoy
- `sector9_sky_barrier_core` — Barrier Controller
- `sector9_sky_hard_deck` — Pushback Node

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
