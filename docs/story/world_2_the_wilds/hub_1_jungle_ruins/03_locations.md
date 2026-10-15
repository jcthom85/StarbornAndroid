# Jungle Ruins: node and room map

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The current node catalog contains 5 nodes and 50 distinct room memberships for `hub_3_sector9`. Membership and entry policy are catalog facts, not a guaranteed linear route or universal hub access.

| Node | Entry room | Rooms | Entry / return policy |
|---|---|---:|---|
| `sector9_landing` — Crash Site | `sector9_crash_site` | 7 | `hub` / `entry` |
| `razor_vine_path` — Razor-Vine Path | `sector9_canopy` | 11 | `transition` / `entry` |
| `tideglass_beach` — Tideglass Beach | `sector9_stream_pools` | 18 | `transition` / `entry` |
| `canopy_ridge` — Canopy Ridge | `sector9_canopy_ridge` | 8 | `transition` / `entry` |
| `temple_gate` — Temple Gate | `sector9_temple_gate` | 6 | `transition` / `entry` |

## Room membership

Use current room connections, action conditions, node transitions, and progression evaluation for navigation. This replaces old hand-drawn choice diagrams and room-purpose guesses. Titles are current display names, not new creative instructions.

### Crash Site

- `sector9_crash_site` — Crash Site
- `sector9_pod_interior` — Escape Pod Interior
- `sector9_landing_brush` — Scorched Brush
- `sector9_landing_glade` — Overgrown Glade
- `sector9_landing_stream` — Stream Bank
- `sector9_landing_perimeter` — Security Perimeter
- `sector9_landing_drop` — Pod Fragment

### Razor-Vine Path

- `sector9_canopy` — Resonant Canopy
- `sector9_wilds_thickets` — Spore Thickets
- `sector9_wilds_nest` — Hound Nest
- `sector9_wilds_hollow` — Poison Hollow
- `sector9_wilds_grave` — Patrol Graveyard
- `sector9_wilds_overlook` — Canopy Edge
- `sector9_wilds_canopy_walk` — Hanging Root Bridge
- `sector9_wilds_archway` — Architect Gateway
- `sector9_wilds_dense_brush` — Beast Run
- `sector9_wilds_clearance` — Burned Glade
- `sector9_wilds_lookout` — Hunter's Blind

### Tideglass Beach

- `sector9_stream_pools` — Mist Pools
- `sector9_stream_wetlands` — Resonant Sedge
- `sector9_stream_flats` — Crystalline Flats
- `sector9_stream_falls` — Resonant Falls
- `sector9_stream_tidepools` — Beach Transition
- `sector9_stream_cave_entrance` — Cavern Entrance
- `sector9_stream_cave_depths` — Glow-Moss Cavern
- `sector9_stream_sunken_passage` — Waterlogged Tunnel
- `sector9_beach_cliffs` — Beach Cliffs
- `sector9_beach_cove` — Sandy Cove
- `sector9_beach_pools` — Tide Pools
- `sector9_beach_cargo` — Beached Cargo Wreck
- `sector9_beach_cave` — Sea Cavern
- `sector9_beach_pillar` — Sunken Obelisk
- `sector9_beach_grotto` — Hidden Grotto
- `sector9_beach_dunes` — Star-Sand Dunes
- `sector9_coastal_research_station` — Coastal Resilience Station
- `sector9_sunken_aethel_vault` — Submerged Aethel Vault

### Canopy Ridge

- `sector9_ridge_climb` — Vine Climb
- `sector9_ridge_ledges` — Windy Ledges
- `sector9_ridge_overhang` — Beast Lair Approach
- `sector9_canopy_ridge` — Canopy Ridge
- `sector9_ridge_viewpoint` — Shield Viewpoint
- `sector9_ridge_sniper_perch` — Sniper Lookout
- `sector9_ridge_crevice` — High Pass
- `sector9_ridge_plateau` — Sanctuary Outlook

### Temple Gate

- `sector9_temple_plaza` — Stone Courtyard
- `sector9_temple_vestibule` — Outer Guard Post
- `sector9_temple_lock_chamber` — Resonator Chamber
- `sector9_temple_left_wing` — Left Tuner Column
- `sector9_temple_right_wing` — Right Tuner Column
- `sector9_temple_gate` — Temple Gate

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
