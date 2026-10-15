# Upper City: node and room map

Status: Source-reconciled reference, October 14, 2026. Current code/assets are the implementation baseline. This is a source review, not a fresh playthrough or a claim that every route and device behavior is accepted.

The current node catalog contains 5 nodes and 33 distinct room memberships for `hub_6_upper_city`. Membership and entry policy are catalog facts, not a guaranteed linear route or universal hub access.

| Node | Entry room | Rooms | Entry / return policy |
|---|---|---:|---|
| `spire_laundry` — Laundry Service | `spire_laundry_service` | 7 | `hub` / `entry` |
| `spire_skypark` — Skypark | `spire_skypark_dome` | 9 | `transition` / `entry` |
| `spire_exec_lounge` — Exec Lounge | `spire_exec_lounge_bar` | 6 | `transition` / `entry` |
| `spire_archive` — The Archive | `spire_archive_vault` | 6 | `transition` / `entry` |
| `spire_landing_pad` — Landing Pad | `spire_landing_pad_roof` | 5 | `transition` / `entry` |

## Room membership

Use current room connections, action conditions, node transitions, and progression evaluation for navigation. This replaces old hand-drawn choice diagrams and room-purpose guesses. Titles are current display names, not new creative instructions.

### Laundry Service

- `spire_laundry_service` — Laundry Service
- `spire_uniform_sorting` — Uniform Sorting
- `spire_service_lift` — Service Lift
- `spire_staff_checkpoint` — Staff Checkpoint
- `spire_laundry_scale_01` — Steam Rack
- `spire_laundry_scale_02` — Press Line
- `spire_laundry_scale_03` — Service Bin

### Skypark

- `spire_skypark_dome` — Skypark Dome
- `spire_glasswalk` — Glasswalk
- `spire_donor_gallery` — Donor Gallery
- `spire_black_site_lab` — Executive Black-Site Lab
- `spire_skypark_scale_01` — Orchid Walk
- `spire_skypark_scale_02` — Registry Pavilion
- `spire_skypark_scale_03` — Glass Lawn
- `spire_skyline_vip_suite` — Skyline VIP Salon
- `spire_vip_private_booth` — Syndicate Booth

### Exec Lounge

- `spire_exec_lounge_bar` — Exec Lounge
- `spire_exec_lounge_scale_01` — Velvet Hall
- `spire_exec_lounge_scale_02` — Private Booth
- `spire_exec_lounge_scale_03` — Blackmail Balcony
- `spire_exec_lounge_scale_04` — Ledger Office
- `spire_exec_lounge_scale_05` — Service Dumbwaiter

### The Archive

- `spire_prism_gallery` — Prism Gallery
- `spire_archive_vault` — Archive Vault
- `spire_drone_test_alcove` — Drone Test Alcove
- `spire_archive_scale_01` — Catalog Atrium
- `spire_archive_scale_02` — Prism Service Bay
- `spire_archive_scale_03` — Alarm Spine

### Landing Pad

- `spire_landing_pad_roof` — Landing Pad Roof
- `spire_landing_pad_scale_01` — Fuel Bridge
- `spire_landing_pad_scale_02` — Shield Sightline
- `spire_landing_pad_scale_03` — Drone Wreck Roof
- `spire_landing_pad_scale_04` — Boarding Causeway

## Sources and maintenance

[rooms](../../../../app/src/main/assets/rooms.json), [quests](../../../../app/src/main/assets/quests.json), [events](../../../../app/src/main/assets/events.json), [dialogue](../../../../app/src/main/assets/dialogue.json), [hub_nodes](../../../../app/src/main/assets/hub_nodes.json). [Documentation index](../../../README.md). Update this reference when its corresponding records change; do not change working game logic to match an older script.
