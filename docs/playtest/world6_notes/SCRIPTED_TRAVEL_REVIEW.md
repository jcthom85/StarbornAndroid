# Scripted travel review inventory

Source-checked event transfers and authored room controls. See SCRIPTED_TRAVEL.json for prerequisites and nested action paths. Repeatability here describes the event flag. Quest conditions can still limit a repeatable event, and travel panels may have additional runtime conditions.

Ordinary directional movement, Astra navigation panels and hub entry also need presentation review in the client. This inventory covers authored event transfers and room controls, including their entry points.

## Destination world 2

| Entry point | Handler | Destination | Transition | Event repeatable |
| --- | --- | --- | --- | --- |
| launch_pod / navigation console | w1_mq05_use_nav_console | sector9_crash_site | runtime default | False |
| {"type": "player_action", "action": "w2_mq05_resolve_gate"} | w2_mq05_resolve_gate | sector9_hangar_bay | airlock | False |
| sector9_hangar_bay / salvage return route | w2_mq05_return_to_pod | sector9_landing_drop | passage | True |
| sector9_landing_drop / return to Astra | w2_mq05_return_to_hangar | sector9_hangar_bay | passage | True |

## Destination world Shared / other

| Entry point | Handler | Destination | Transition | Event repeatable |
| --- | --- | --- | --- | --- |
| sector9_hangar_bay / launch cradle | w2_mq05_launch | board_astra | airlock | True |
| deep_anchor_chamber_scale_08 / anchor cable | warp_deep_anchor_chamber_scale_08_to_entry | deep_anchor_chamber | passage | True |
| deep_throne_room_scale_07 / lift | warp_deep_throne_room_scale_07_to_entry | deep_throne_room | lift | True |
| deep_tear_scale_06 / reality slipstream | warp_deep_tear_scale_06_to_entry | deep_tear | source | True |

## Destination world 3

| Entry point | Handler | Destination | Transition | Event repeatable |
| --- | --- | --- | --- | --- |
| spire_transit_plaza / elevator controls | w3_mq12_use_elevator | spire_laundry_service | lift | True |
| {"type": "quest_stage_complete", "quest_id": "w3_mq12"} | w3_mq12_to_w3_mq13 | spire_laundry_service | lift | False |
| spire_vent_output_scale_03 / chute | warp_spire_vent_output_scale_03_to_entry | spire_vent_output | passage | True |
| spire_the_static_scale_03 / fire escape | warp_spire_the_static_scale_03_to_entry | spire_the_static | passage | True |
| spire_night_market_scale_03 / slide | warp_spire_night_market_scale_03_to_entry | spire_night_market | passage | True |
| spire_sewers_scale_03 / ladder | warp_spire_sewers_scale_03_to_entry | spire_sewers_landing | passage | True |
| spire_transit_plaza_scale_03 / turnstile | warp_spire_transit_plaza_scale_03_to_entry | spire_transit_plaza | airlock | True |
| spire_laundry_scale_03 / chute | warp_spire_laundry_scale_03_to_entry | spire_laundry_service | passage | True |
| spire_skypark_scale_03 / spiral | warp_spire_skypark_scale_03_to_entry | spire_skypark_dome | passage | True |
| spire_exec_lounge_scale_05 / elevator | warp_spire_exec_lounge_scale_05_to_entry | spire_exec_lounge_bar | lift | True |
| spire_archive_scale_03 / lift | warp_spire_archive_scale_03_to_entry | spire_archive_vault | lift | True |
| spire_landing_pad_scale_04 / ladder | warp_spire_landing_pad_scale_04_to_entry | spire_landing_pad_roof | passage | True |
| {"type": "player_action", "action": "w2_mq05_confirm_departure"} | w2_mq05_confirm_departure | spire_sewers_landing | runtime default | False |
| spire_service_lift / lift panel | w3_return_to_lower_city | spire_transit_plaza | lift | True |

## Destination world 4

| Entry point | Handler | Destination | Transition | Event repeatable |
| --- | --- | --- | --- | --- |
| spire_landing_pad_roof / launch window | w3_mq15_launch_astra | foundry_slag_landing | launch | False |
| {"type": "player_action", "action": "w4_mq17_regroup_springs"} | w4_mq17_regroup_springs | foundry_conveyor_belt | foundry | False |
| foundry_obsidian_shelf_scale_final / slide | warp_foundry_obsidian_shelf_scale_final_to_entry | foundry_slag_landing | passage | True |
| foundry_slag_river_scale_final / catwalk | warp_foundry_slag_river_scale_final_to_entry | foundry_slag_river | passage | True |
| foundry_cooling_springs_scale_final / drain | warp_foundry_cooling_springs_scale_final_to_entry | foundry_cooling_springs | passage | True |
| foundry_waste_intake_scale_final / ladder | warp_foundry_waste_intake_scale_final_to_entry | foundry_waste_intake | passage | True |
| foundry_service_airlock_scale_final / hatch | warp_foundry_service_airlock_scale_final_to_entry | foundry_service_airlock | airlock | True |
| foundry_conveyor_belt_scale_final / slide | warp_foundry_conveyor_belt_scale_final_to_entry | foundry_conveyor_belt | passage | True |
| foundry_conditioning_scale_final / valve | warp_foundry_conditioning_scale_final_to_entry | foundry_conditioning_chamber | passage | True |
| foundry_forge_scale_final / flue | warp_foundry_forge_scale_final_to_entry | foundry_forge_anvil | passage | True |
| foundry_power_core_scale_final / lift | warp_foundry_power_core_scale_final_to_entry | foundry_power_core | lift | True |
| foundry_titan_dock_scale_final / scaffolding | warp_foundry_titan_dock_scale_final_to_entry | foundry_titan_dock | passage | True |

## Destination world 5

| Entry point | Handler | Destination | Transition | Event repeatable |
| --- | --- | --- | --- | --- |
| foundry_titan_dock / escape launch, foundry_escape_catwalk / escape gantry | w4_mq20_escape_launch | orbital_executive_dock | launch | False |
| orbital_executive_dock_scale_final / chute | warp_orbital_executive_dock_scale_final_to_entry | orbital_executive_dock | passage | True |
| orbital_grand_concourse_scale_final / express escalator | warp_orbital_grand_concourse_scale_final_to_entry | orbital_grand_concourse | passage | True |
| orbital_solarium_scale_final / stairwell | warp_orbital_solarium_scale_final_to_entry | orbital_solarium | passage | True |
| orbital_security_hub_scale_final / hatch | warp_orbital_security_hub_scale_final_to_entry | orbital_security_hub | airlock | True |
| orbital_service_shaft_scale_final / guide | warp_orbital_service_shaft_scale_final_to_entry | orbital_service_shaft | passage | True |
| deep_server_farm_scale_04 / slide | warp_deep_server_farm_scale_04_to_entry | orbital_server_farm | passage | True |

## Destination world 6

| Entry point | Handler | Destination | Transition | Event repeatable |
| --- | --- | --- | --- | --- |
| deep_tear / reality breach | w5_mq25_enter_tear | source_campfire | source | False |
| source_campfire / song-fire | w6_mq26_reassemble | source_echo_mines | source | False |
| {"type": "enter_room", "room": "source_echo_elevator"} | w6_mq27_reach_elevator | source_memory_bridge | lift | False |
| source_memory_threshold / singularity threshold | w6_mq28_reach_singularity | source_memory_stair | source | False |
| {"type": "player_action", "action": "w6_mq30_tune_world"} | w6_mq30_tune_world | source_new_world | runtime default | False |
| source_campfire_node_scale_final / trail | warp_source_campfire_node_scale_final_to_entry | source_campfire | passage | True |
| source_zeke_nightmare_node_scale_03 / door | warp_source_zeke_nightmare_node_scale_03_to_entry | source_zeke_nightmare | airlock | True |
| source_gh0st_nightmare_node_scale_03 / clean signal | warp_source_gh0st_nightmare_node_scale_03_to_entry | source_gh0st_nightmare | source | True |
| source_orion_nightmare_node_scale_03 / harmonic wave | warp_source_orion_nightmare_node_scale_03_to_entry | source_orion_nightmare | source | True |
| source_echo_mines_node_scale_final / echo | warp_source_echo_mines_node_scale_final_to_entry | source_echo_mines | source | True |
| source_memory_bridge_node_scale_final / light beam | warp_source_memory_bridge_node_scale_final_to_entry | source_memory_bridge | source | True |
| source_memory_stair_node_scale_final / slide | warp_source_memory_stair_node_scale_final_to_entry | source_memory_stair | passage | True |
| source_spire_thought_node_scale_04 / return circuit | warp_source_spire_thought_node_scale_04_to_entry | source_spire_thought | passage | True |
| source_center_node_scale_final / echo | warp_source_center_node_scale_final_to_entry | source_center | source | True |
| source_center_node_scale_03 / return to the fracture | w6_resume_silence_at_center | source_center | source | True |

## Direct room controls

See the direct_controls array in SCRIPTED_TRAVEL.json for the complete control definitions.

- spire_sewers_landing — astra
- spire_transit_plaza — elevator controls
- spire_service_lift — lift panel
- spire_landing_pad_roof — launch window
- spire_vent_output_scale_03 — chute
- spire_the_static_scale_03 — fire escape
- spire_night_market_scale_03 — slide
- spire_sewers_scale_03 — ladder
- spire_transit_plaza_scale_03 — turnstile
- spire_laundry_scale_03 — chute
- spire_skypark_scale_03 — spiral
- spire_archive_scale_03 — lift
- spire_exec_lounge_scale_05 — elevator
- spire_landing_pad_scale_04 — ladder
