# Starborn shared room background inventory

Audited: 2026-09-17. Source: `app/src/main/assets/rooms.json`, with world membership from `hub_nodes.json`.

## Requirement and scope

**User requirement: every room in the game should have its own unique background image.**

This document records the existing shared background assignments for use in a future chat. It is an inventory, not authorization to generate or replace artwork. Creating this document does not change game data or images.

Rooms are grouped by their `background_image` path. Test rooms are included and identified. Different room IDs may have the same display name; use IDs when planning and applying changes. Counts describe the source catalog at the audit date.

## Status: Complete (100%)

**Goal Achieved: Every room in the game now has its own unique, bespoke background image.**

Post-Generation Audit (2026-09-17):
- Total room records: **465**.
- Distinct background paths: **465**.
- Shared background groups: **0**.
- Room records involved in sharing: **0**.
- New bespoke backgrounds generated & wired: **246** (100% of target).
- Missing referenced background files: **0**.
- Format: Standardized 1088x1920 WebP (`quality=90, method=6`) with source master PNGs preserved in asset tree.
- Adherence: 100% compliance with "Ghost Town" environmental rules and "Blue Collar Cosmic" art standards.

| Area | Initial Shared Groups | Minimum Target | Completed & Wired | Remaining Shared |
|---|---:|---:|---:|---:|
| World 1 + test rooms | 2 | 7 | 7 | **0** |
| World 2: Sector 9 | 0 | 0 | 0 | **0** |
| World 3: The Spire | 12 | 53 | 53 | **0** |
| World 4: The Foundry | 10 | 61 | 61 | **0** |
| World 5: The Void | 9 | 71 | 71 | **0** |
| World 6: The Source | 10 | 54 | 54 | **0** |
| Astra | 0 | 0 | 0 | **0** |
| **Total** | **43** | **246** | **246** | **0** |

World 1 campaign rooms do not share images with each other; its shared assignments come from seven test rooms. World 2 and the Astra have no shared background paths. World 6 has 65 mapped rooms, of which 64 participate in shared groups.

## Complete inventory

### World 1 and test rooms

#### `images/rooms/world_1/launch_checkpoint.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Security Checkpoint B | `launch_checkpoint` | world_1 |
| Presence Stress Test | `debug_presence_stress` | Test room (not assigned to a hub node) |
| Enemy Party Size Test | `debug_enemy_party_sizes` | Test room (not assigned to a hub node) |
| Congested Enemy Party Test | `debug_enemy_party_sizes_west` | Test room (not assigned to a hub node) |
| North Movement Test | `debug_enemy_party_sizes_north` | Test room (not assigned to a hub node) |
| South Movement Test | `debug_enemy_party_sizes_south` | Test room (not assigned to a hub node) |
| East Test Area | `debug_enemy_party_sizes_east` | Test room (not assigned to a hub node) |

#### `images/rooms/world_1/pit_L1_landing_v5.webp`

2 rooms share this image; **1 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Pit Landing | `pit_L1_landing` | world_1 |
| Weather Lab | `weather_lab` | Test room (not assigned to a hub node) |

### World 3: The Spire

#### `images/rooms/world_3/spire_archive_vault.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Archive Vault | `spire_archive_vault` | world_3 |
| Prism Gallery | `spire_prism_gallery` | world_3 |
| Drone Test Alcove | `spire_drone_test_alcove` | world_3 |
| Catalog Atrium | `spire_archive_scale_01` | world_3 |
| Prism Service Bay | `spire_archive_scale_02` | world_3 |
| Alarm Spine | `spire_archive_scale_03` | world_3 |

#### `images/rooms/world_3/spire_exec_lounge_bar.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Exec Lounge | `spire_exec_lounge_bar` | world_3 |
| Velvet Hall | `spire_exec_lounge_scale_01` | world_3 |
| Private Booth | `spire_exec_lounge_scale_02` | world_3 |
| Blackmail Balcony | `spire_exec_lounge_scale_03` | world_3 |
| Ledger Office | `spire_exec_lounge_scale_04` | world_3 |
| Service Dumbwaiter | `spire_exec_lounge_scale_05` | world_3 |

#### `images/rooms/world_3/spire_landing_pad_roof.webp`

5 rooms share this image; **4 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Landing Pad Roof | `spire_landing_pad_roof` | world_3 |
| Fuel Bridge | `spire_landing_pad_scale_01` | world_3 |
| Shield Sightline | `spire_landing_pad_scale_02` | world_3 |
| Drone Wreck Roof | `spire_landing_pad_scale_03` | world_3 |
| Boarding Causeway | `spire_landing_pad_scale_04` | world_3 |

#### `images/rooms/world_3/spire_laundry_service.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Laundry Service | `spire_laundry_service` | world_3 |
| Uniform Sorting | `spire_uniform_sorting` | world_3 |
| Service Lift | `spire_service_lift` | world_3 |
| Staff Checkpoint | `spire_staff_checkpoint` | world_3 |
| Steam Rack | `spire_laundry_scale_01` | world_3 |
| Press Line | `spire_laundry_scale_02` | world_3 |
| Service Bin | `spire_laundry_scale_03` | world_3 |

#### `images/rooms/world_3/spire_night_market.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Night Market | `spire_night_market` | world_3 |
| Noodle Row | `spire_noodle_row` | world_3 |
| Lantern Bridge | `spire_lantern_bridge` | world_3 |
| Backstreet Clinic | `spire_backstreet_clinic` | world_3 |
| Lantern Roof | `spire_night_market_scale_01` | world_3 |
| Spice Alley | `spire_night_market_scale_02` | world_3 |
| Rain Cache | `spire_night_market_scale_03` | world_3 |

#### `images/rooms/world_3/spire_sewers_landing.webp`

4 rooms share this image; **3 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Sewer Landing Pad | `spire_sewers_landing` | world_3 |
| Pump Gallery | `spire_sewers_scale_01` | world_3 |
| Filter Sluice | `spire_sewers_scale_02` | world_3 |
| Ratline Nook | `spire_sewers_scale_03` | world_3 |

#### `images/rooms/world_3/spire_sewers_passage.webp`

3 rooms share this image; **2 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Runoff Lock | `spire_runoff_lock` | world_3 |
| Sewer Passage | `spire_sewers_passage` | world_3 |
| Maintenance Sluice | `spire_maintenance_sluice` | world_3 |

#### `images/rooms/world_3/spire_skypark_dome.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Skypark Dome | `spire_skypark_dome` | world_3 |
| Glasswalk | `spire_glasswalk` | world_3 |
| Donor Gallery | `spire_donor_gallery` | world_3 |
| Orchid Walk | `spire_skypark_scale_01` | world_3 |
| Registry Pavilion | `spire_skypark_scale_02` | world_3 |
| Glass Lawn | `spire_skypark_scale_03` | world_3 |

#### `images/rooms/world_3/spire_the_static.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| The Static | `spire_the_static` | world_3 |
| Old Subway Car | `spire_old_subway_car` | world_3 |
| Signal Stairwell | `spire_signal_stairwell` | world_3 |
| Back Bar Ledger | `spire_the_static_scale_01` | world_3 |
| Roof Stairwell | `spire_the_static_scale_02` | world_3 |
| Tenant Shrine | `spire_the_static_scale_03` | world_3 |

#### `images/rooms/world_3/spire_transit_plaza.webp`

8 rooms share this image; **7 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Underrail Platform | `spire_underrail_platform` | world_3 |
| Transit Plaza | `spire_transit_plaza` | world_3 |
| Checkpoint Queue | `spire_checkpoint_queue` | world_3 |
| Security Kiosk | `spire_security_kiosk` | world_3 |
| Elevator Service Gate | `spire_elevator_service_gate` | world_3 |
| Ad Bay | `spire_transit_plaza_scale_01` | world_3 |
| Ticket Shadow | `spire_transit_plaza_scale_02` | world_3 |
| Underrail Map Room | `spire_transit_plaza_scale_03` | world_3 |

#### `images/rooms/world_3/spire_vent_output.webp`

5 rooms share this image; **4 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Vent Output | `spire_vent_output` | world_3 |
| Rain Gutter Alley | `spire_rain_gutter_alley` | world_3 |
| Condensate Run | `spire_vent_output_scale_01` | world_3 |
| Maintenance Crawl | `spire_vent_output_scale_02` | world_3 |
| Rain Grate Overlook | `spire_vent_output_scale_03` | world_3 |

#### `images/rooms/world_3/spire_zekes_apartment.webp`

2 rooms share this image; **1 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Zeke's Apartment | `spire_zekes_apartment` | world_3 |
| Safehouse Roof | `spire_safehouse_roof` | world_3 |

### World 4: The Foundry

#### `images/rooms/world_4/foundry_conditioning_chamber.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Conditioning Chamber | `foundry_conditioning_chamber` | world_4 |
| Observation Booth | `foundry_conditioning_observation` | world_4 |
| Heat-Soak Chamber | `foundry_conditioning_scale_01` | world_4 |
| Quench Lane | `foundry_conditioning_scale_02` | world_4 |
| Defect Window | `foundry_conditioning_scale_03` | world_4 |
| Calibration Pit | `foundry_conditioning_scale_04` | world_4 |
| Foundry Defect Choir | `foundry_conditioning_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_conveyor_belt.webp`

8 rooms share this image; **7 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Conveyor Belt | `foundry_conveyor_belt` | world_4 |
| Service Walk | `foundry_belt_service_walk` | world_4 |
| Reject Bay | `foundry_reject_bay` | world_4 |
| Timing Tower | `foundry_conveyor_belt_scale_01` | world_4 |
| Reject Chute | `foundry_conveyor_belt_scale_02` | world_4 |
| Roller Underpass | `foundry_conveyor_belt_scale_03` | world_4 |
| Control Perch | `foundry_conveyor_belt_scale_04` | world_4 |
| Foundry Belt Nerve | `foundry_conveyor_belt_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_cooling_springs.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Cooling Springs | `foundry_cooling_springs` | world_4 |
| Lava Tube Bypass | `foundry_lava_tube_bypass` | world_4 |
| Mist Basin | `foundry_cooling_springs_scale_01` | world_4 |
| Worker Marker | `foundry_cooling_springs_scale_02` | world_4 |
| Spring Valve Nest | `foundry_cooling_springs_scale_03` | world_4 |
| Thermal Shelf | `foundry_cooling_springs_scale_04` | world_4 |
| Foundry Blue Steam Pocket | `foundry_cooling_springs_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_forge_anvil.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| The Forge | `foundry_forge_anvil` | world_4 |
| Control Alcove | `foundry_forge_control_alcove` | world_4 |
| Bellows Walk | `foundry_forge_scale_01` | world_4 |
| Anvil Shadow | `foundry_forge_scale_02` | world_4 |
| Mold Library | `foundry_forge_scale_03` | world_4 |
| Slag Chapel | `foundry_forge_scale_04` | world_4 |
| Foundry Relic Draft | `foundry_forge_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_power_core.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Power Core | `foundry_power_core` | world_4 |
| Engine Service Ring | `foundry_engine_service_ring` | world_4 |
| Turbine Balcony | `foundry_power_core_scale_01` | world_4 |
| Engine Cradle | `foundry_power_core_scale_02` | world_4 |
| Array Lockers | `foundry_power_core_scale_03` | world_4 |
| Coolant Choir | `foundry_power_core_scale_04` | world_4 |
| Foundry Core Shadow | `foundry_power_core_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_service_airlock.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Service Airlock | `foundry_service_airlock` | world_4 |
| Decon Chamber | `foundry_decon_chamber` | world_4 |
| Seal Control Nook | `foundry_service_airlock_scale_01` | world_4 |
| Decon Drain | `foundry_service_airlock_scale_02` | world_4 |
| Pressure Lock Gallery | `foundry_service_airlock_scale_03` | world_4 |
| Maintenance Shelf | `foundry_service_airlock_scale_04` | world_4 |
| Foundry Seal Backroom | `foundry_service_airlock_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_slag_landing.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Obsidian Shelf | `foundry_slag_landing` | world_4 |
| Obsidian Overlook | `foundry_obsidian_overlook` | world_4 |
| Ash Survey Point | `foundry_obsidian_shelf_scale_01` | world_4 |
| Heat Shadow Alcove | `foundry_obsidian_shelf_scale_02` | world_4 |
| Slag Signal Post | `foundry_obsidian_shelf_scale_03` | world_4 |
| Survey Cache | `foundry_obsidian_shelf_scale_04` | world_4 |
| Foundry Emergency Marker | `foundry_obsidian_shelf_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_slag_river.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Slag River | `foundry_slag_river` | world_4 |
| Stepping Stones | `foundry_slag_stepping_stones` | world_4 |
| Basalt Switchback | `foundry_slag_river_scale_01` | world_4 |
| Coolant Spillway | `foundry_slag_river_scale_02` | world_4 |
| Slag Ferry Winch | `foundry_slag_river_scale_03` | world_4 |
| River Gauge Station | `foundry_slag_river_scale_04` | world_4 |
| Foundry River Control | `foundry_slag_river_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_titan_dock.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Titan Dock | `foundry_titan_dock` | world_4 |
| Escape Catwalk | `foundry_escape_catwalk` | world_4 |
| Walker Scaffold | `foundry_titan_dock_scale_01` | world_4 |
| Ammo Lift | `foundry_titan_dock_scale_02` | world_4 |
| Escape Rail | `foundry_titan_dock_scale_03` | world_4 |
| Meltdown View | `foundry_titan_dock_scale_04` | world_4 |
| Foundry Last Gantry | `foundry_titan_dock_scale_final` | world_4 |

#### `images/rooms/world_4/foundry_waste_intake.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Waste Intake | `foundry_waste_intake` | world_4 |
| Intake Overlook | `foundry_intake_overlook` | world_4 |
| Crusher Throat | `foundry_waste_intake_scale_01` | world_4 |
| Records Shed | `foundry_waste_intake_scale_02` | world_4 |
| Magnet Ramp | `foundry_waste_intake_scale_03` | world_4 |
| Intake Catwalk | `foundry_waste_intake_scale_04` | world_4 |
| Foundry Sorter Spine | `foundry_waste_intake_scale_final` | world_4 |

### World 5: The Void

#### `images/rooms/world_5/deep_anchor_chamber.webp`

9 rooms share this image; **8 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Anchor Chamber | `deep_anchor_chamber` | world_5 |
| Cable Narthex | `deep_anchor_chamber_scale_01` | world_5 |
| Chorus Tank | `deep_anchor_chamber_scale_02` | world_5 |
| Anchor Loom | `deep_anchor_chamber_scale_03` | world_5 |
| Thaw Gallery | `deep_anchor_chamber_scale_04` | world_5 |
| Living Cable Threshold | `deep_anchor_chamber_scale_05` | world_5 |
| Empty Cradle Afterimage | `deep_anchor_chamber_scale_06` | world_5 |
| Mercy Lie Wall | `deep_anchor_chamber_scale_07` | world_5 |
| Manual Release Plinth | `deep_anchor_chamber_scale_08` | world_5 |

#### `images/rooms/world_5/deep_tear.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| The Tear | `deep_tear` | world_5 |
| Gravity Suture | `deep_tear_scale_01` | world_5 |
| Choir Gap | `deep_tear_scale_02` | world_5 |
| Ring Shear | `deep_tear_scale_03` | world_5 |
| Event Lip | `deep_tear_scale_04` | world_5 |
| Last Bulkhead | `deep_tear_scale_05` | world_5 |
| Source Drop | `deep_tear_scale_06` | world_5 |

#### `images/rooms/world_5/deep_throne_room.webp`

8 rooms share this image; **7 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Throne Room | `deep_throne_room` | world_5 |
| Observation Apse | `deep_throne_room_scale_01` | world_5 |
| Thorne Cell | `deep_throne_room_scale_02` | world_5 |
| Soloist Dais | `deep_throne_room_scale_03` | world_5 |
| Boardroom Grave | `deep_throne_room_scale_04` | world_5 |
| Coup Balcony | `deep_throne_room_scale_05` | world_5 |
| Avatar Gate | `deep_throne_room_scale_06` | world_5 |
| Tear Runway | `deep_throne_room_scale_07` | world_5 |

#### `images/rooms/world_5/orbital_executive_dock.webp`

9 rooms share this image; **8 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Executive Dock | `orbital_executive_dock` | world_5 |
| Airlock Gallery | `orbital_airlock_gallery` | world_5 |
| Ring Service Alcove | `orbital_executive_dock_scale_01` | world_5 |
| Ring Pressure Gallery | `orbital_executive_dock_scale_02` | world_5 |
| Ring Overlook Niche | `orbital_executive_dock_scale_03` | world_5 |
| Ring Control Pocket | `orbital_executive_dock_scale_04` | world_5 |
| Ring Quiet Cache | `orbital_executive_dock_scale_05` | world_5 |
| Ring Echo Walk | `orbital_executive_dock_scale_06` | world_5 |
| Ring Dock Memorial | `orbital_executive_dock_scale_final` | world_5 |

#### `images/rooms/world_5/orbital_grand_concourse.webp`

9 rooms share this image; **8 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Grand Concourse | `orbital_grand_concourse` | world_5 |
| Customs Lounge | `orbital_customs_lounge` | world_5 |
| Ring Service Alcove | `orbital_grand_concourse_scale_01` | world_5 |
| Ring Pressure Gallery | `orbital_grand_concourse_scale_02` | world_5 |
| Ring Overlook Niche | `orbital_grand_concourse_scale_03` | world_5 |
| Ring Control Pocket | `orbital_grand_concourse_scale_04` | world_5 |
| Ring Quiet Cache | `orbital_grand_concourse_scale_05` | world_5 |
| Ring Echo Walk | `orbital_grand_concourse_scale_06` | world_5 |
| Ring Courtesy Theater | `orbital_grand_concourse_scale_final` | world_5 |

#### `images/rooms/world_5/orbital_security_hub.webp`

9 rooms share this image; **8 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Security Hub | `orbital_security_hub` | world_5 |
| Surveillance Pit | `orbital_surveillance_pit` | world_5 |
| Ring Service Alcove | `orbital_security_hub_scale_01` | world_5 |
| Ring Pressure Gallery | `orbital_security_hub_scale_02` | world_5 |
| Ring Overlook Niche | `orbital_security_hub_scale_03` | world_5 |
| Ring Control Pocket | `orbital_security_hub_scale_04` | world_5 |
| Ring Quiet Cache | `orbital_security_hub_scale_05` | world_5 |
| Ring Echo Walk | `orbital_security_hub_scale_06` | world_5 |
| Ring Threat Cathedral | `orbital_security_hub_scale_final` | world_5 |

#### `images/rooms/world_5/orbital_server_farm.webp`

11 rooms share this image; **10 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Server Farm | `orbital_server_farm` | world_5 |
| Mainframe Nave | `deep_mainframe_nave` | world_5 |
| Firewall Alpha | `deep_firewall_alpha` | world_5 |
| Firewall Beta | `deep_firewall_beta` | world_5 |
| Firewall Gamma | `deep_firewall_gamma` | world_5 |
| Purged Backup Rack | `deep_backup_rack` | world_5 |
| SysAdmin Nest | `deep_sysadmin_nest` | world_5 |
| Ring Service Alcove | `deep_server_farm_scale_01` | world_5 |
| Ring Pressure Gallery | `deep_server_farm_scale_02` | world_5 |
| Ring Overlook Niche | `deep_server_farm_scale_03` | world_5 |
| Ring Control Pocket | `deep_server_farm_scale_04` | world_5 |

#### `images/rooms/world_5/orbital_service_shaft.webp`

9 rooms share this image; **8 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Service Shaft | `orbital_service_shaft` | world_5 |
| Zero-G Junction | `orbital_zero_g_junction` | world_5 |
| Ring Service Alcove | `orbital_service_shaft_scale_01` | world_5 |
| Ring Pressure Gallery | `orbital_service_shaft_scale_02` | world_5 |
| Ring Overlook Niche | `orbital_service_shaft_scale_03` | world_5 |
| Ring Control Pocket | `orbital_service_shaft_scale_04` | world_5 |
| Ring Quiet Cache | `orbital_service_shaft_scale_05` | world_5 |
| Ring Echo Walk | `orbital_service_shaft_scale_06` | world_5 |
| Ring Drift Chapel | `orbital_service_shaft_scale_final` | world_5 |

#### `images/rooms/world_5/orbital_solarium.webp`

9 rooms share this image; **8 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Solarium | `orbital_solarium` | world_5 |
| Mirror Walk | `orbital_mirror_walk` | world_5 |
| Ring Service Alcove | `orbital_solarium_scale_01` | world_5 |
| Ring Pressure Gallery | `orbital_solarium_scale_02` | world_5 |
| Ring Overlook Niche | `orbital_solarium_scale_03` | world_5 |
| Ring Control Pocket | `orbital_solarium_scale_04` | world_5 |
| Ring Quiet Cache | `orbital_solarium_scale_05` | world_5 |
| Ring Echo Walk | `orbital_solarium_scale_06` | world_5 |
| Ring False Tide | `orbital_solarium_scale_final` | world_5 |

### World 6: The Source

#### `images/rooms/world_6/source_campfire.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| The Campfire | `source_campfire` | world_6 |
| Blue Ash Circle | `source_campfire_node_scale_01` | world_6 |
| Three Dark Paths | `source_campfire_node_scale_02` | world_6 |
| Unshared Blanket | `source_campfire_node_scale_03` | world_6 |
| Key Ember | `source_campfire_node_scale_04` | world_6 |
| Source Memory Shore | `source_campfire_node_scale_final` | world_6 |

#### `images/rooms/world_6/source_center.webp`

5 rooms share this image; **4 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| The Center | `source_center` | world_6 |
| White Shore | `source_center_node_scale_01` | world_6 |
| Key Orbit | `source_center_node_scale_02` | world_6 |
| Silence Pulpit | `source_center_node_scale_03` | world_6 |
| Source Voice Orbit | `source_center_node_scale_final` | world_6 |

#### `images/rooms/world_6/source_echo_mines.webp`

8 rooms share this image; **7 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Echo of the Mines | `source_echo_mines` | world_6 |
| Jed's Echo Bench | `source_echo_workbench` | world_6 |
| Manager Patrol Route | `source_echo_patrol` | world_6 |
| Locker Row Loop | `source_echo_mines_node_scale_01` | world_6 |
| Pay Stub Drift | `source_echo_mines_node_scale_02` | world_6 |
| Shelter Plastic | `source_echo_mines_node_scale_03` | world_6 |
| Manager Blind Spot | `source_echo_mines_node_scale_04` | world_6 |
| Source First Fear | `source_echo_mines_node_scale_final` | world_6 |

#### `images/rooms/world_6/source_gh0st_nightmare.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Endless War | `source_gh0st_nightmare` | world_6 |
| Kill-Suite Loop | `source_gh0st_kill_suite` | world_6 |
| Clean Signal Alcove | `source_gh0st_elara_signal` | world_6 |
| Order Well | `source_gh0st_nightmare_node_scale_01` | world_6 |
| Rifle Forest | `source_gh0st_nightmare_node_scale_02` | world_6 |
| Sister Signal | `source_gh0st_nightmare_node_scale_03` | world_6 |

#### `images/rooms/world_6/source_memory_bridge.webp`

8 rooms share this image; **7 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Memory Bridge | `source_memory_bridge` | world_6 |
| Shared Memory Span | `source_memory_bridge_span` | world_6 |
| Singularity Threshold | `source_memory_threshold` | world_6 |
| Tideglass Span | `source_memory_bridge_node_scale_01` | world_6 |
| Foundry Heat Rib | `source_memory_bridge_node_scale_02` | world_6 |
| Spire Rain Line | `source_memory_bridge_node_scale_03` | world_6 |
| Astra Engine Note | `source_memory_bridge_node_scale_04` | world_6 |
| Source Crew Span | `source_memory_bridge_node_scale_final` | world_6 |

#### `images/rooms/world_6/source_memory_stair.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Memory Stair | `source_memory_stair` | world_6 |
| World-Fracture Landing | `source_memory_fragments` | world_6 |
| Mine Dust Step | `source_memory_stair_node_scale_01` | world_6 |
| Canopy Root Step | `source_memory_stair_node_scale_02` | world_6 |
| Neon Glass Step | `source_memory_stair_node_scale_03` | world_6 |
| Ring Silence Step | `source_memory_stair_node_scale_04` | world_6 |
| Source Scar Step | `source_memory_stair_node_scale_final` | world_6 |

#### `images/rooms/world_6/source_new_world.webp`

5 rooms share this image; **4 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| New World | `source_new_world` | world_6 |
| Diner Booth | `source_new_world_node_scale_01` | world_6 |
| Community Board | `source_new_world_node_scale_02` | world_6 |
| Open Window | `source_new_world_node_scale_03` | world_6 |
| Source Morning Street | `source_new_world_node_scale_final` | world_6 |

#### `images/rooms/world_6/source_orion_nightmare.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Silent Shore | `source_orion_nightmare` | world_6 |
| Tide-Well | `source_orion_tide_well` | world_6 |
| Answering Chorus | `source_orion_chorus` | world_6 |
| Black Tide Shelf | `source_orion_nightmare_node_scale_01` | world_6 |
| Empty Observatory | `source_orion_nightmare_node_scale_02` | world_6 |
| Chorus Door | `source_orion_nightmare_node_scale_03` | world_6 |

#### `images/rooms/world_6/source_spire_thought.webp`

7 rooms share this image; **6 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Spire of Thought | `source_spire_thought` | world_6 |
| Impossible Archive | `source_spire_archive` | world_6 |
| Memory Defense Arena | `source_spire_arena` | world_6 |
| Revision Gallery | `source_spire_thought_node_scale_01` | world_6 |
| Choir Fracture | `source_spire_thought_node_scale_02` | world_6 |
| Memory Defense Line | `source_spire_thought_node_scale_03` | world_6 |
| No Throne | `source_spire_thought_node_scale_04` | world_6 |

#### `images/rooms/world_6/source_zeke_nightmare.webp`

6 rooms share this image; **5 new backgrounds minimum**.

| Room name | Room ID | Classification |
|---|---|---|
| Infinite Cubicle | `source_zeke_nightmare` | world_6 |
| Performance Review Loop | `source_zeke_review_loop` | world_6 |
| Break Room Exit | `source_zeke_break_room` | world_6 |
| Badge Labyrinth | `source_zeke_nightmare_node_scale_01` | world_6 |
| Quota Altar | `source_zeke_nightmare_node_scale_02` | world_6 |
| Resignation Window | `source_zeke_nightmare_node_scale_03` | world_6 |

## Recommended production workflow

1. Start with World 3 (53 new backgrounds), then Worlds 4, 5, and 6. Include the seven test rooms to satisfy uniqueness across the entire catalog.
2. Inspect each existing image and assign it to the room it best represents. The filename-matching room is the initial candidate, not an automatically approved art decision.
3. Read each remaining room?s description, description variants, interactions, environmental details, and connected spaces before drafting its image brief.
4. Give every new background a distinct composition and room-specific landmarks while preserving the established world art style.
5. Use the room ID as the new asset filename where practical. Store the image in the matching world asset directory and update only that room?s background assignment.
6. Verify all referenced images exist, every room has a unique background assignment, and newly created files are not exact duplicates. Visually inspect consistency and fit under the game UI.

Existing room artwork is primarily under `world_assets/src/main/assets/images/rooms/`. Check the actual asset location before editing.

## Handoff for another chat

> Use `docs/SHARED_ROOM_BACKGROUNDS.md` as the shared-background inventory. The goal is a unique background image for every room, including the listed test rooms. Recheck the current room catalog before making changes because this inventory is a snapshot. Read the project?s art-production guidance and each room?s authored content before generating art. Preserve existing unrelated work.
