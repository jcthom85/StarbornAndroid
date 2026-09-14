# Navigation topology and spatial audit

This is the baseline audit recorded on 2026-09-14, before the navigation repair. Its counts and recommendations describe the original assets. See [navigation-repair.md](navigation-repair.md) for the implemented corrections and validation. Run `python navigation_audit.py --check` or `--json` against the current assets to inspect the repaired graph.

## Findings and scope

465 rooms, 928 directed room exits, and 126 node transition records were checked, including all six story worlds, Astra, and seven unassigned debug/lab rooms. Exactly **14 directed exits are non-reciprocal**: five asymmetric two-way pairs (10 exits) and four one-way shortcuts. All 14 also appear in node_transitions.json; these are the same defects represented twice, not 28 defects.

No dangling room destinations, duplicate room IDs, duplicate transition IDs/edge keys, transition-to-room disagreements, incorrect transition node owners, missing cross-node transition records, or within-node coordinate collisions were found. The seven unassigned rooms are weather_lab, debug_presence_stress, and the five debug_enemy_party_sizes rooms. They are outside the six-world counts.

| World | Rooms | Directed exits | Non-reciprocal exits | Same-direction funnel groups |
| --- | --- | --- | --- | --- |
| unassigned | 7 | 8 | 0 | 0 |
| world_1 | 81 | 152 | 0 | 0 |
| world_2 | 91 | 190 | 0 | 0 |
| world_3 | 65 | 139 | 7 | 4 |
| world_4 | 71 | 142 | 4 | 4 |
| world_5 | 80 | 161 | 3 | 3 |
| world_6 | 65 | 128 | 0 | 0 |
| world_astra | 5 | 8 | 0 | 0 |

Reciprocity includes all eight compass directions and up/down. Diagonal special exits are not errors: northeast/southwest and northwest/southeast are valid opposite pairs. A funnel means distinct sources use the same outgoing cardinal direction to reach a target, as in the question; it does not mean the sources share a node.

## Complete non-reciprocal exit inventory

“Actual direct return” is the direction at the target that leads back to the source. “Opposite lands at” shows what the player actually reaches by reversing their entry direction. IDs below are exact asset room IDs.

| World | Source | Exit | Target | Expected return | Actual direct return | Opposite lands at |
| --- | --- | --- | --- | --- | --- | --- |
| world_3 | spire_underrail_platform | south | spire_transit_plaza | north | NONE | spire_the_static |
| world_3 | spire_service_lift | east | spire_archive_vault | west | NONE | spire_prism_gallery |
| world_3 | spire_service_lift | south | spire_exec_lounge_bar | north | west | spire_uniform_sorting |
| world_3 | spire_glasswalk | south | spire_archive_vault | north | NONE | spire_donor_gallery |
| world_3 | spire_exec_lounge_bar | west | spire_service_lift | east | south | spire_archive_vault |
| world_3 | spire_exec_lounge_bar | east | spire_archive_vault | west | south | spire_prism_gallery |
| world_3 | spire_archive_vault | south | spire_exec_lounge_bar | north | east | spire_uniform_sorting |
| world_4 | foundry_cooling_springs | east | foundry_waste_intake | west | south | foundry_slag_river |
| world_4 | foundry_waste_intake | south | foundry_cooling_springs | north | east | foundry_slag_landing |
| world_4 | foundry_forge_anvil | east | foundry_power_core | west | south | foundry_conditioning_chamber |
| world_4 | foundry_power_core | south | foundry_forge_anvil | north | east | foundry_conveyor_belt |
| world_5 | orbital_solarium | east | orbital_security_hub | west | south | orbital_grand_concourse |
| world_5 | orbital_security_hub | south | orbital_solarium | north | east | orbital_executive_dock |
| world_5 | deep_firewall_gamma | east | deep_anchor_chamber | west | NONE | orbital_server_farm |

There are no additional non-reciprocal room exits in Worlds 1, 2, or 6, Astra, or the unassigned rooms.

## One-way shortcuts and trap assessment

| One-way edge | Shortest existing return route |
| --- | --- |
| spire_underrail_platform → spire_transit_plaza | spire_transit_plaza → spire_the_static → spire_underrail_platform |
| spire_service_lift → spire_archive_vault | spire_archive_vault → spire_exec_lounge_bar → spire_service_lift |
| spire_glasswalk → spire_archive_vault | spire_archive_vault → spire_donor_gallery → spire_glasswalk |
| deep_firewall_gamma → deep_anchor_chamber | deep_anchor_chamber → orbital_server_farm → deep_mainframe_nave → deep_firewall_alpha → deep_firewall_beta → deep_firewall_gamma |

None of these four is a graph-theoretic trap: the destination can reach the source. The listed return routes have no blocked_directions entries, and the abnormal endpoints have no initial darkness/state flags. This proves static return reachability, not immunity to combat, scripted state changes, or every saved-game state. The asset data does not establish that these shortcuts were deliberately designed as drops; treat their intention as unconfirmed.

## Complete inbound funnel inventory

| World | Target | Sources all exit… | Inbound sources | Opposite return reaches |
| --- | --- | --- | --- | --- |
| world_3 | spire_transit_plaza | south | spire_underrail_platform, spire_the_static | spire_the_static |
| world_3 | spire_exec_lounge_bar | south | spire_uniform_sorting, spire_service_lift, spire_archive_vault | spire_uniform_sorting |
| world_3 | spire_archive_vault | east | spire_service_lift, spire_exec_lounge_bar, spire_prism_gallery | spire_prism_gallery |
| world_3 | spire_archive_vault | south | spire_glasswalk, spire_donor_gallery | spire_donor_gallery |
| world_4 | foundry_cooling_springs | south | foundry_slag_landing, foundry_waste_intake | foundry_slag_landing |
| world_4 | foundry_waste_intake | east | foundry_slag_river, foundry_cooling_springs | foundry_slag_river |
| world_4 | foundry_forge_anvil | south | foundry_conveyor_belt, foundry_power_core | foundry_conveyor_belt |
| world_4 | foundry_power_core | east | foundry_conditioning_chamber, foundry_forge_anvil | foundry_conditioning_chamber |
| world_5 | orbital_solarium | south | orbital_executive_dock, orbital_security_hub | orbital_executive_dock |
| world_5 | orbital_security_hub | east | orbital_grand_concourse, orbital_solarium | orbital_grand_concourse |
| world_5 | deep_anchor_chamber | east | orbital_server_farm, deep_firewall_gamma | orbital_server_farm |

These are 11 direction groups affecting 10 target rooms; Archive Vault has two groups. They misroute intuitive backtracking but do not create permanent static traps. For the five asymmetric pairs, the direct return exists under the other direction shown above. For the four shortcuts, use the explicit multi-room paths above. Never overwrite the target's existing opposite exit: that would sever the legitimate route to Static, Uniform Sorting, Prism Gallery, Donor Gallery, Slag Landing/River, Conveyor/Conditioning, Executive Dock/Grand Concourse, or Server Farm.

## World 2: Sector 9 / Hall of Echoes

All 190 exits among the 91 World 2 rooms are reciprocal. There are no same-direction inbound funnels. In particular, Hall of Echoes has four valid reciprocal neighbors:

- west → sector9_foyer_grand_hall; Grand Hall east returns.
- north → sector9_hall_echo_alcoves; Alcoves south returns.
- east → sector9_archive_vault; Archive west returns.
- south → sector9_stasis_chamber; Stasis north returns.

The defect here is spatial presentation. Grand Hall is at (0,0), while its EAST destination Hall is at (0,-2). Hall's NORTH destination Alcoves is at (0,-3), and Alcoves' NORTH destination Acoustics Lab is at (0,-4). The renderer treats positive Y as north, so these rooms visibly contradict the navigation controls.

Safest correction: adjust pos only, retaining all IDs, exits, node membership, entry_room, actions, gates, and triggers. One collision-free example uses the following spaced layout for the complete Hall/archive branch while retaining the other rooms:

Hall=(4,0), Alcoves=(4,1), Acoustics Lab=(4,2), Reflection Pool=(5,1), Archive Vault=(5,0), Reading Room=(6,0), Tuning Bay=(6,-1), Secret Stash=(7,-1).

This preserves compass alignment for every within-node edge in that branch without colliding with the existing foyer/conduit positions. The Grand Hall→Hall edge spans four units, so it falls outside the minimap's two-cell preview radius; a production layout should compact/repack the whole node or intentionally depict the passage as a longer corridor. This example demonstrates a safe coordinate change, not a fully optimized final map.

Preserve the Thermal Cutter gates on sector9_wilds_canopy_walk.east ↔ sector9_wilds_lookout.west and sector9_conduit_access_shaft.north ↔ sector9_vents_gantry.south. Their distorted positions do not justify removing their key requirements.

Quest dependencies: w2_mq03_inspect_murals sets ms_w2_murals_read and ms_w2_murals_inspected. w2_mq03_align_complete additionally requires the pod, overview, and coolant milestones before awarding the Bridge and recruiting Orion. w2_sq04_complete requires all three crystal tasks and ms_w2_crystal_{west,east,north}_seated. Coordinate corrections leave these checks intact. Keep the named crystal actions and their narrative directions unchanged.

## World 3: Static / Transit / Underrail / Sewers / Upper City

Underrail SOUTH→Transit Plaza is the only non-reciprocal edge in the named Lower City cluster. Plaza NORTH→Static is valid and must remain. Static WEST→Underrail returns by a second step. Sewers Passage EAST↔Plaza WEST and Sewers Passage NORTH↔Vent Output SOUTH are reciprocal; Static and Underrail also have a reciprocal east/west connection. There is no broken sewer return.

Spatial defects remain: Vent Output EAST→Underrail moves (0,-1); the Static→Signal Stairwell→Zeke's Apartment→Safehouse Roof northbound chain decreases Y at every step. Transit Plaza's eastbound Checkpoint Queue is displaced (2,-1), and its southbound Security Kiosk is displaced (1,0). Fix local node coordinates without changing these routes.

There is no room ID or room title “Penthouse” in rooms.json. The penthouse is narrative language in w3_mq13 for the Laundry/Staff Checkpoint/Skypark/Archive infiltration. Its actual abnormalities are the six Upper City exits in the inventory.

The Archive already has four cardinal exits plus its northeast Catalog Atrium special exit. Its west exit to Prism Gallery is required for the Lens puzzle; replacing it to return to Service Lift or Lounge would damage that route. Its north exit to Donor Gallery is also legitimate. Laundry and Lounge are similarly saturated. A simple opposite-direction overwrite is unsafe.

Progression cross-check:
- w3_mq12 requires Jax, Underrail patrol timing, kiosk records, freight badges, disguises, blueprints, and the planning table. Preserve each action location, especially w3_mq12_map_patrols at Underrail. Plaza's w3_mq12_use_elevator only displays the planning requirement; it is not an automatic travel gate.
- w3_mq13_blend_in → w3_mq13_disable_sensors → w3_mq13_enter_lobby enforces guest access, blinded scanners, then starts w3_mq14 and reveals/unlocks Archive.
- w3_mq14_enter_archive is an enter_room trigger at spire_archive_vault, conditional on w3_mq14 being active. Keep the destination ID and an opportunity to enter after the quest activates.
- Lens acquisition still requires solve_light_puzzle; preserve the field/shutters/tether sequence, Prism Gallery access, and Drone Alcove→roof escape.
- w3_sq14 needs Lounge's concierge credential and ledger, then Service Lift's leak/weaponize actions. Keep both rooms and their connection.
- Existing room travel has no destination-node-unlocked check. Ungated routes already permit visiting Archive before its quest activates. This is a pre-existing difference between narrative access and physical access; do not claim that merely revealing/unlocking a node prevents entry. Adding stronger locks would be a progression change beyond a geometry correction.

## Worlds 4 and 5 progression cross-check

Cooling Springs↔Waste Intake: preserve the physical pair because w4_mq17 uses the Phantom terminal then regrouping at Springs, which grants Gh0st's override and warps to foundry_conveyor_belt. Side quests w4_sq17 and w4_sq18 also explicitly link intake investigation/salvage with Springs. w4_sq16's crate and the VHS pickup must remain accessible.

Forge↔Power Core: w4_mq19_reach_core_chamber fires on entering foundry_forge_anvil. The cradle needs ms_w4_pulse_board_read, ms_w4_grease_route_read, ms_w4_paddle_thrown, and ms_w4_pistons_starved; taking the Anvil requires the puzzle task. w4_mq20 engine/array theft requires defeat_titan_walker. Keep Forge's matrix, relic, and trial actions, the Power Core actions, and these conditions unchanged.

Solarium↔Security Hub: preserve access to w5_mq22_cross_solarium, w5_sq22 mirror/garden restoration, w5_sq21 redaction recovery, and the VHS pickup. A direction-label correction does not alter these action conditions.

Firewall Gamma→Anchor Chamber: w5_mq23 sequences Alpha, Beta, Gamma, then Firewall Construct victory; that victory starts w5_mq24. w5_mq24_enter_chamber requires entering deep_anchor_chamber while the quest is active, then Elara precedes Anchor acquisition. The existing direct Server Farm↔Anchor route already bypasses the physical firewall walk, although quest conditions still prevent completing the relic sequence early. Do not replace Anchor's west exit or delete the shortcut as an assumed quest fix. Keep the quest start and subsequent chamber-entry opportunity. A new reverse edge changes navigation but does not itself replace the firewall task requirements.

## Safest correction plan

1. Correct local map coordinates independently of gameplay adjacency. Use positive Y=north, avoid collisions, and account for the minimap preview radius. Coordinates are local to each node; positions in different nodes are not a shared global floor plan.
2. Correct the five asymmetric pairs by relabeling the existing edges into vacant opposite direction slots. The following proposal was simulated in memory: it preserves every ordered source→destination pair, introduces no direction-slot collision, and reduces 14 reciprocity violations to the four one-way shortcuts.
3. Keep those four shortcuts explicitly identified as one-way named passages until their intent is settled. Making every edge reciprocal necessarily adds or removes travel options; that cannot preserve 100% of the present directed topology. If universal reciprocity is mandatory, use dedicated junction rooms or unused named/vertical exits and assess the changed return paths against encounter and quest entry timing.
4. Update node_transitions.json direction fields/IDs alongside each changed connection, add matching special_exits labels for diagonal travel at both ends, and preserve IDs of rooms/events/quests/milestones. Preserve the existing special exits; do not commandeer northeast Catalog Atrium or the existing scale branches.
5. Validate the actual patch with asset integrity and special-exit tests plus focused before/after quest playthroughs. No proposed gameplay correction has been applied or certified by a playthrough in this audit.

| Existing directed exit | Proposed direction | Existing reverse exit | Proposed reverse |
| --- | --- | --- | --- |
| spire_service_lift south → spire_exec_lounge_bar | southeast | spire_exec_lounge_bar west → spire_service_lift | northwest |
| spire_exec_lounge_bar east → spire_archive_vault | northeast | spire_archive_vault south → spire_exec_lounge_bar | southwest |
| foundry_cooling_springs east → foundry_waste_intake | southeast | foundry_waste_intake south → foundry_cooling_springs | northwest |
| foundry_forge_anvil east → foundry_power_core | southeast | foundry_power_core south → foundry_forge_anvil | northwest |
| orbital_solarium east → orbital_security_hub | southeast | orbital_security_hub south → orbital_solarium | northwest |

These are connector directions, not a claim that current room coordinates form a consistent global geometry. Name the special passages meaningfully and lay out each local map separately. If the design requires cardinal-only navigation, introduce junctions instead, recognizing that extra rooms introduce entry events/steps and need separate validation.

## Runtime implications and preservation limits

[ExplorationViewModel.kt](../app/src/main/java/com/example/starborn/feature/exploration/viewmodel/ExplorationViewModel.kt) travel at line 2393 reads room.connections and checks source blocked_directions. No production call site of loadNodeTransitions was found: the transition catalog is loaded by a declaration and checked by DataIntegrityTest, but does not override movement.

At line 5629, unlockDirection unlocks the destination's opposite slot without checking that it returns to the original room. At line 1475, direction indicators similarly inspect the destination's opposite block. An asymmetric edge could therefore unlock or display a different route's gate. None of the 14 affected exits has such a source/opposite block today, and no affected room is referenced by milestones.json unlock_exits. This is a latent runtime hazard, not an observed gate bypass at these 14 exits. A future hardening change should require the opposite connection to return to the source before mirroring a gate.

Dark-room travel remembers only the opposite direction (line 2434); a non-reciprocal arrival into a dark room could expose the wrong escape or none. None of these abnormal endpoints is initially dark. Preserve that distinction when assessing “trap” severity.

The tested label-only proposal preserves directed adjacency and the affected action/quest predicates. It is the strongest low-risk correction supported by this static audit. It does not establish 100% runtime equivalence across all save files, encounter states, UI interactions, or narratives. Saved direction-based unlocks and any future direction-based content must be considered when applying relabels.

## Spatial appendix: complete local cardinal misalignments

The map renderer uses screenY = centerY - offsetY, so positive asset Y is north ([MinimapWidget.kt](../app/src/main/java/com/example/starborn/feature/exploration/ui/hud/MinimapWidget.kt), lines 135–157). Maps use rooms in the current node ([ExplorationViewModel.kt](../app/src/main/java/com/example/starborn/feature/exploration/viewmodel/ExplorationViewModel.kt), roomsForMaps). Cross-node coordinates therefore are not compared as if globally aligned.

There are 132 misaligned directed cardinal exits in the six worlds, representing 66 reciprocal pairs: W1=0, W2=24, W3=38, W4=34, W5=20, W6=16. Four additional debug exits form two debug pairs. These are distinct from the 14 cross-node reciprocity defects. Diagonal/vertical special exits are excluded from this local cardinal geometry test; multi-cell spacing alone is not treated as a compass error.

Each reciprocal spatial pair is listed once below. Delta is target pos minus source pos; expected N=(0,+), S=(0,-), E=(+,0), W=(-,0). Nonzero perpendicular displacement counts as a bent/misaligned corridor, not a connectivity failure.

| World | Source | Direction | Target | Delta (x,y) |
| --- | --- | --- | --- | --- |
| unassigned | debug_enemy_party_sizes | north | debug_enemy_party_sizes_north | 0, -1 |
| unassigned | debug_enemy_party_sizes | south | debug_enemy_party_sizes_south | 0, 1 |
| world_3 | spire_runoff_lock | north | spire_maintenance_sluice | 0, -1 |
| world_3 | spire_vent_output | west | spire_rain_gutter_alley | -2, -1 |
| world_3 | spire_vent_output | east | spire_underrail_platform | 0, -1 |
| world_3 | spire_the_static | north | spire_signal_stairwell | 0, -1 |
| world_3 | spire_signal_stairwell | north | spire_zekes_apartment | 0, -1 |
| world_3 | spire_zekes_apartment | north | spire_safehouse_roof | 0, -1 |
| world_3 | spire_night_market | east | spire_noodle_row | 3, -2 |
| world_3 | spire_night_market | north | spire_lantern_bridge | 2, -3 |
| world_3 | spire_night_market | south | spire_backstreet_clinic | 2, -1 |
| world_3 | spire_transit_plaza | east | spire_checkpoint_queue | 2, -1 |
| world_3 | spire_transit_plaza | south | spire_security_kiosk | 1, 0 |
| world_3 | spire_checkpoint_queue | north | spire_elevator_service_gate | 0, -1 |
| world_3 | spire_laundry_service | south | spire_uniform_sorting | 0, 5 |
| world_3 | spire_laundry_service | west | spire_laundry_scale_01 | 0, 20 |
| world_3 | spire_skypark_dome | east | spire_glasswalk | 2, 4 |
| world_3 | spire_exec_lounge_bar | south | spire_exec_lounge_scale_01 | 0, 20 |
| world_3 | spire_archive_vault | west | spire_prism_gallery | 1, 5 |
| world_3 | spire_archive_vault | east | spire_drone_test_alcove | 3, 5 |
| world_4 | foundry_slag_landing | north | foundry_obsidian_overlook | 0, -1 |
| world_4 | foundry_slag_landing | west | foundry_obsidian_shelf_scale_01 | 0, 20 |
| world_4 | foundry_slag_river | north | foundry_slag_stepping_stones | 0, -1 |
| world_4 | foundry_slag_river | south | foundry_slag_river_scale_01 | 0, 20 |
| world_4 | foundry_cooling_springs | south | foundry_lava_tube_bypass | 0, 1 |
| world_4 | foundry_cooling_springs | west | foundry_cooling_springs_scale_01 | 0, 20 |
| world_4 | foundry_waste_intake | north | foundry_intake_overlook | 0, -1 |
| world_4 | foundry_conveyor_belt | north | foundry_belt_service_walk | 0, -1 |
| world_4 | foundry_conditioning_chamber | north | foundry_conditioning_observation | 0, -1 |
| world_4 | foundry_conditioning_chamber | south | foundry_conditioning_scale_01 | 0, 20 |
| world_4 | foundry_forge_anvil | south | foundry_forge_scale_01 | 0, 20 |
| world_4 | foundry_power_core | north | foundry_engine_service_ring | 0, -1 |
| world_4 | foundry_titan_dock | north | foundry_escape_catwalk | 0, -1 |
| world_4 | foundry_titan_dock | east | foundry_titan_dock_scale_01 | 0, 20 |
| world_5 | orbital_executive_dock | north | orbital_airlock_gallery | 0, -1 |
| world_5 | orbital_executive_dock | west | orbital_executive_dock_scale_01 | 0, 20 |
| world_5 | orbital_grand_concourse | north | orbital_customs_lounge | 0, -1 |
| world_5 | orbital_grand_concourse | south | orbital_grand_concourse_scale_01 | 0, 20 |
| world_5 | orbital_solarium | south | orbital_mirror_walk | 0, 1 |
| world_5 | orbital_solarium | west | orbital_solarium_scale_01 | 0, 20 |
| world_5 | orbital_security_hub | north | orbital_surveillance_pit | 0, -1 |
| world_5 | orbital_server_farm | north | deep_mainframe_nave | 0, -1 |
| world_5 | orbital_server_farm | south | deep_backup_rack | 0, 1 |
| world_6 | source_campfire | east | source_campfire_node_scale_01 | 0, 20 |
| world_6 | source_zeke_nightmare | east | source_zeke_review_loop | 1, -1 |
| world_6 | source_orion_nightmare | east | source_orion_tide_well | 1, -1 |
| world_6 | source_echo_mines | east | source_echo_workbench | 1, 1 |
| world_6 | source_echo_mines | south | source_echo_mines_node_scale_01 | 0, 20 |
| world_6 | source_echo_patrol | east | source_echo_elevator | -1, -1 |
| world_6 | source_memory_bridge | east | source_memory_bridge_span | 1, 1 |
| world_2 | sector9_stream_tidepools | north | sector9_beach_pools | 1, -3 |
| world_2 | sector9_stream_sunken_passage | north | sector9_beach_grotto | 2, -5 |
| world_2 | sector9_beach_cove | east | sector9_beach_pools | 0, -1 |
| world_2 | sector9_beach_pools | north | sector9_beach_cave | 0, -1 |
| world_2 | sector9_beach_cave | north | sector9_beach_pillar | 0, -1 |
| world_2 | sector9_wilds_canopy_walk | south | sector9_wilds_archway | 0, 1 |
| world_2 | sector9_wilds_canopy_walk | east | sector9_wilds_lookout | 1, 3 |
| world_2 | sector9_foyer_grand_hall | east | sector9_hall_of_echoes | 0, -2 |
| world_2 | sector9_hall_of_echoes | north | sector9_hall_echo_alcoves | 0, -1 |
| world_2 | sector9_hall_echo_alcoves | north | sector9_hall_acoustics_lab | 0, -1 |
| world_2 | sector9_stasis_observation | north | sector9_vents_access_grate | 2, 0 |
| world_2 | sector9_vents_access_grate | north | sector9_vents_tech_alcove | 0, -1 |
| world_3 | spire_exec_lounge_scale_01 | east | spire_exec_lounge_scale_02 | 0, 1 |
| world_4 | foundry_slag_river_scale_01 | east | foundry_slag_river_scale_02 | 0, 1 |
| world_4 | foundry_conditioning_scale_01 | east | foundry_conditioning_scale_02 | 0, 1 |
| world_4 | foundry_forge_scale_01 | east | foundry_forge_scale_02 | 0, 1 |
| world_5 | orbital_grand_concourse_scale_01 | east | orbital_grand_concourse_scale_02 | 0, 1 |
| world_6 | source_echo_mines_node_scale_01 | east | source_echo_mines_node_scale_02 | 0, 1 |

## Transition record IDs requiring review

- `deep_firewall_gamma__east__deep_anchor_chamber`
- `foundry_cooling_springs__east__foundry_waste_intake`
- `foundry_forge__east__foundry_power_core`
- `foundry_power_core__south__foundry_forge`
- `foundry_waste_intake__south__foundry_cooling_springs`
- `orbital_security_hub__south__orbital_solarium`
- `orbital_solarium__east__orbital_security_hub`
- `spire_archive__spire_archive_vault__south__spire_exec_lounge`
- `spire_exec_lounge__spire_exec_lounge_bar__west__spire_laundry`
- `spire_exec_lounge__spire_exec_lounge_bar__east__spire_archive`
- `spire_skypark__spire_glasswalk__south__spire_archive`
- `spire_laundry__spire_service_lift__east__spire_archive`
- `spire_laundry__spire_service_lift__south__spire_exec_lounge`
- `spire_vent_output__spire_underrail_platform__south__spire_transit_plaza`

## Quest/event evidence index

The following events were cross-referenced through affected/focus room IDs, node IDs, or room action_event links. This includes indirect quest dependencies; a lack of literal room IDs in quests.json does not mean the rooms are quest-independent. Full conditions remain in events.json.

| Event | Trigger | Quest/task/milestone conditions |
| --- | --- | --- |
| w2_mq02_unlock_temple | {"type":"player_action","action":"w2_mq02_use_chime"} | type="quest_active" quest_id="w2_mq02"; type="quest_task_done" quest_id="w2_mq02" task_id="locate_temple_gate"; type="quest_task_done" quest_id="w2_mq02" task_id="clear_canopy_path" |
| w2_mq03_inspect_murals | {"type":"player_action","action":"w2_mq03_inspect_murals"} | type="quest_active" quest_id="w2_mq03"; type="quest_task_not_done" quest_id="w2_mq03" task_id="inspect_murals" |
| w2_mq05_launch | {"type":"player_action","action":"w2_mq05_launch"} | type="quest_active" quest_id="w2_mq05"; type="quest_task_done" quest_id="w2_mq05" task_id="reboot_bridge_relic"; type="quest_task_not_done" quest_id="w2_mq05" task_id="launch_ship" |
| w3_mq11_reach_vent | {"type":"enter_room","room":"spire_vent_output"} | type="quest_active" quest_id="w3_mq11"; type="quest_task_done" quest_id="w3_mq11" task_id="clear_landing"; type="quest_task_not_done" quest_id="w3_mq11" task_id="reach_vent" |
| w3_mq11_find_apartment | {"type":"enter_room","room":"spire_zekes_apartment"} | type="quest_active" quest_id="w3_mq11"; type="quest_task_not_done" quest_id="w3_mq11" task_id="find_apartment" |
| w3_mq11_establish_shield | {"type":"player_action","action":"w3_mq11_establish_shield"} | type="quest_active" quest_id="w3_mq11"; type="quest_task_done" quest_id="w3_mq11" task_id="find_apartment"; type="quest_task_not_done" quest_id="w3_mq11" task_id="establish_shield" |
| w3_mq12_talk_jax | {"type":"player_action","action":"w3_mq12_talk_jax"} | type="quest_active" quest_id="w3_mq12"; type="quest_task_not_done" quest_id="w3_mq12" task_id="talk_jax" |
| w3_mq12_map_patrols | {"type":"player_action","action":"w3_mq12_map_patrols"} | type="quest_active" quest_id="w3_mq12"; type="quest_task_not_done" quest_id="w3_mq12" task_id="map_patrols" |
| w3_mq12_source_disguises | {"type":"player_action","action":"w3_mq12_source_disguises"} | type="quest_active" quest_id="w3_mq12"; type="quest_task_not_done" quest_id="w3_mq12" task_id="source_disguises" |
| w3_mq12_hack_blueprints | {"type":"player_action","action":"w3_mq12_hack_blueprints"} | type="quest_active" quest_id="w3_mq12"; type="quest_task_done" quest_id="w3_mq12" task_id="talk_jax"; type="quest_task_done" quest_id="w3_mq12" task_id="map_patrols"; type="quest_task_done" quest_id="w3_mq12" task_id="interrogate_guard"; type="quest_task_done" quest_id="w3_mq12" task_id="copy_badges"; type="quest_task_done" quest_id="w3_mq12" task_id="source_disguises"; type="quest_task_not_done" quest_id="w3_mq12" task_id="hack_blueprints" |
| w3_mq12_assemble_planning | {"type":"player_action","action":"w3_mq12_assemble_planning"} | type="quest_active" quest_id="w3_mq12"; type="quest_task_done" quest_id="w3_mq12" task_id="talk_jax"; type="quest_task_done" quest_id="w3_mq12" task_id="map_patrols"; type="quest_task_done" quest_id="w3_mq12" task_id="interrogate_guard"; type="quest_task_done" quest_id="w3_mq12" task_id="copy_badges"; type="quest_task_done" quest_id="w3_mq12" task_id="source_disguises"; type="quest_task_done" quest_id="w3_mq12" task_id="hack_blueprints"; type="quest_task_not_done" quest_id="w3_mq12" task_id="assemble_table" |
| w3_mq12_use_elevator | {"type":"player_action","action":"w3_mq12_use_elevator"} | none |
| w3_sq11_inspect_sign | {"type":"player_action","action":"w3_sq11_inspect_sign"} | type="quest_not_started" quest_id="w3_sq11" |
| w3_mq12_to_w3_mq13 | {"type":"quest_stage_complete","quest_id":"w3_mq12"} | type="quest_completed" quest_id="w3_mq12"; type="quest_not_started" quest_id="w3_mq13" |
| w3_mq13_blend_in | {"type":"player_action","action":"w3_mq13_blend_in"} | type="quest_active" quest_id="w3_mq13"; type="quest_task_not_done" quest_id="w3_mq13" task_id="blend_in" |
| w3_mq13_disable_sensors | {"type":"player_action","action":"w3_mq13_disable_sensors"} | type="quest_active" quest_id="w3_mq13"; type="quest_task_done" quest_id="w3_mq13" task_id="blend_in"; type="quest_task_not_done" quest_id="w3_mq13" task_id="disable_sensors" |
| w3_mq13_enter_lobby | {"type":"player_action","action":"w3_mq13_enter_lobby"} | type="quest_active" quest_id="w3_mq13"; type="quest_task_done" quest_id="w3_mq13" task_id="blend_in"; type="quest_task_done" quest_id="w3_mq13" task_id="disable_sensors"; type="quest_task_not_done" quest_id="w3_mq13" task_id="enter_lobby" |
| w3_mq14_enter_archive | {"type":"enter_room","room":"spire_archive_vault"} | type="quest_active" quest_id="w3_mq14"; type="quest_task_not_done" quest_id="w3_mq14" task_id="enter_archive" |
| w3_mq14_read_containment_field | {"type":"player_action","action":"w3_mq14_read_containment_field"} | type="quest_active" quest_id="w3_mq14"; type="quest_task_done" quest_id="w3_mq14" task_id="enter_archive"; type="milestone_not_set" milestone="ms_w3_containment_field_read" |
| w3_mq14_trace_command_tethers | {"type":"player_action","action":"w3_mq14_trace_command_tethers"} | type="quest_active" quest_id="w3_mq14"; type="quest_task_done" quest_id="w3_mq14" task_id="enter_archive"; type="milestone_not_set" milestone="ms_w3_command_tethers_traced" |
| w3_mq14_take_lens | {"type":"player_action","action":"w3_mq14_take_lens"} | type="quest_active" quest_id="w3_mq14"; type="quest_task_done" quest_id="w3_mq14" task_id="solve_light_puzzle"; type="quest_task_not_done" quest_id="w3_mq14" task_id="take_lens" |
| w3_scan_archive_tethers | {"type":"player_action","action":"w3_scan_archive_tethers"} | type="milestone_set" milestone="ms_w3_mq14_complete" |
| w3_sq14_steal_ledger | {"type":"player_action","action":"w3_sq14_steal_ledger"} | type="quest_not_completed" quest_id="w3_sq14" |
| w4_mq16_hack_cooling_vents | {"type":"player_action","action":"w4_mq16_hack_cooling_vents"} | type="quest_active" quest_id="w4_mq16"; type="quest_task_done" quest_id="w4_mq16" task_id="cross_slag_river"; type="quest_task_not_done" quest_id="w4_mq16" task_id="hack_cooling_vents" |
| w4_mq17_access_phantom_terminal | {"type":"player_action","action":"w4_mq17_access_phantom_terminal"} | type="quest_active" quest_id="w4_mq17"; type="quest_task_not_done" quest_id="w4_mq17" task_id="access_phantom_terminal" |
| w4_mq17_regroup_springs | {"type":"player_action","action":"w4_mq17_regroup_springs"} | type="quest_active" quest_id="w4_mq17"; type="quest_task_done" quest_id="w4_mq17" task_id="access_phantom_terminal"; type="quest_task_not_done" quest_id="w4_mq17" task_id="regroup_springs" |
| w4_sq16_destroy_crate_beta | {"type":"player_action","action":"w4_sq16_destroy_crate_beta"} | type="quest_not_completed" quest_id="w4_sq16"; type="quest_task_not_done" quest_id="w4_sq16" task_id="crate_beta" |
| w4_sq17_find_worker | {"type":"player_action","action":"w4_sq17_find_worker"} | type="quest_not_completed" quest_id="w4_sq17" |
| w4_sq18_salvage_mechs | {"type":"player_action","action":"w4_sq18_salvage_mechs"} | type="quest_not_completed" quest_id="w4_sq18" |
| w4_mq18_overload_matrix | {"type":"player_action","action":"w4_mq18_overload_matrix"} | type="quest_active" quest_id="w4_mq18"; type="quest_task_done" quest_id="w4_mq18" task_id="defeat_prototypes"; type="quest_task_not_done" quest_id="w4_mq18" task_id="overload_matrix" |
| w4_mq19_reach_core_chamber | {"type":"enter_room","room":"foundry_forge_anvil"} | type="quest_active" quest_id="w4_mq19"; type="quest_task_not_done" quest_id="w4_mq19" task_id="reach_core_chamber" |
| w4_mq19_open_anvil_cradle | {"type":"player_action","action":"w4_mq19_open_anvil_cradle"} | type="quest_active" quest_id="w4_mq19"; type="quest_task_done" quest_id="w4_mq19" task_id="reach_core_chamber"; type="quest_task_not_done" quest_id="w4_mq19" task_id="solve_conveyor_puzzle"; type="milestone_set" milestone="ms_w4_pulse_board_read"; type="milestone_set" milestone="ms_w4_grease_route_read"; type="milestone_set" milestone="ms_w4_paddle_thrown"; type="milestone_set" milestone="ms_w4_pistons_starved" |
| w4_mq19_take_anvil | {"type":"player_action","action":"w4_mq19_take_anvil"} | type="quest_active" quest_id="w4_mq19"; type="quest_task_done" quest_id="w4_mq19" task_id="solve_conveyor_puzzle"; type="quest_task_not_done" quest_id="w4_mq19" task_id="take_anvil" |
| w4_mq20_steal_engine | {"type":"player_action","action":"w4_mq20_steal_engine"} | type="quest_active" quest_id="w4_mq20"; type="quest_task_done" quest_id="w4_mq20" task_id="defeat_titan_walker"; type="quest_task_not_done" quest_id="w4_mq20" task_id="steal_engine" |
| w4_mq20_steal_arrays | {"type":"player_action","action":"w4_mq20_steal_arrays"} | type="quest_active" quest_id="w4_mq20"; type="quest_task_done" quest_id="w4_mq20" task_id="defeat_titan_walker"; type="quest_task_not_done" quest_id="w4_mq20" task_id="steal_arrays" |
| w4_sq20_survive_waves | {"type":"player_action","action":"w4_sq20_survive_waves"} | type="quest_not_completed" quest_id="w4_sq20" |
| w5_mq22_cross_solarium | {"type":"player_action","action":"w5_mq22_cross_solarium"} | type="quest_active" quest_id="w5_mq22"; type="quest_task_not_done" quest_id="w5_mq22" task_id="cross_solarium" |
| w5_sq22_realign_mirrors | {"type":"player_action","action":"w5_sq22_realign_mirrors"} | type="quest_not_completed" quest_id="w5_sq22"; type="milestone_set" milestone="ms_w5_false_sun_traced" |
| w5_mq23_firewall_gamma | {"type":"player_action","action":"w5_mq23_firewall_gamma"} | type="quest_active" quest_id="w5_mq23"; type="quest_task_done" quest_id="w5_mq23" task_id="firewall_beta"; type="quest_task_not_done" quest_id="w5_mq23" task_id="firewall_gamma" |
| w5_mq24_enter_chamber | {"type":"enter_room","room":"deep_anchor_chamber"} | type="quest_active" quest_id="w5_mq24"; type="quest_task_not_done" quest_id="w5_mq24" task_id="enter_chamber" |
| w5_mq24_find_elara | {"type":"player_action","action":"w5_mq24_find_elara"} | type="quest_active" quest_id="w5_mq24"; type="quest_task_done" quest_id="w5_mq24" task_id="enter_chamber"; type="quest_task_not_done" quest_id="w5_mq24" task_id="find_elara" |
| w5_mq24_take_anchor | {"type":"player_action","action":"w5_mq24_take_anchor"} | type="quest_active" quest_id="w5_mq24"; type="quest_task_done" quest_id="w5_mq24" task_id="find_elara"; type="quest_task_not_done" quest_id="w5_mq24" task_id="take_anchor" |
| w3_sq12_find_case_number | {"type":"player_action","action":"w3_sq12_find_case_number"} | type="quest_not_completed" quest_id="w3_sq12"; type="milestone_not_set" milestone="ms_w3_case_number_found" |
| w3_sq14_copy_concierge_key | {"type":"player_action","action":"w3_sq14_copy_concierge_key"} | type="quest_not_completed" quest_id="w3_sq14"; type="milestone_not_set" milestone="ms_w3_concierge_key_copied" |
| w3_sq14_leak_ledger | {"type":"player_action","action":"w3_sq14_leak_ledger"} | type="milestone_set" milestone="ms_w3_ledger_stolen"; type="quest_not_completed" quest_id="w3_sq14" |
| w4_sq17_trace_ping | {"type":"player_action","action":"w4_sq17_trace_ping"} | type="quest_not_completed" quest_id="w4_sq17"; type="milestone_not_set" milestone="ms_w4_worker_ping_traced" |
| w4_sq18_stop_intake | {"type":"player_action","action":"w4_sq18_stop_intake"} | type="quest_not_completed" quest_id="w4_sq18"; type="milestone_not_set" milestone="ms_w4_intake_jammed" |
| w4_sq18_build_bypass | {"type":"player_action","action":"w4_sq18_build_bypass"} | type="milestone_set" milestone="ms_w4_mechs_salvaged"; type="quest_not_completed" quest_id="w4_sq18" |
| w5_sq21_find_redactions | {"type":"player_action","action":"w5_sq21_find_redactions"} | type="quest_not_completed" quest_id="w5_sq21"; type="milestone_not_set" milestone="ms_w5_redactions_found" |
| w5_sq22_restore_gardens | {"type":"player_action","action":"w5_sq22_restore_gardens"} | type="milestone_set" milestone="ms_w5_mirrors_realigned"; type="quest_not_completed" quest_id="w5_sq22" |
| w2_sq04_crystal_north | {"type":"player_action","action":"w2_sq04_crystal_north"} | type="quest_active" quest_id="w2_sq04"; type="quest_task_not_done" quest_id="w2_sq04" task_id="crystal_north" |
| w2_sq04_complete | {"type":"player_action","action":"w2_sq04_complete"} | type="quest_active" quest_id="w2_sq04"; type="quest_task_not_done" quest_id="w2_sq04" task_id="mural_tuning"; type="quest_task_done" quest_id="w2_sq04" task_id="crystal_west"; type="quest_task_done" quest_id="w2_sq04" task_id="crystal_east"; type="quest_task_done" quest_id="w2_sq04" task_id="crystal_north"; type="milestone_set" milestone="ms_w2_crystal_west_seated"; type="milestone_set" milestone="ms_w2_crystal_east_seated"; type="milestone_set" milestone="ms_w2_crystal_north_seated" |
| w2_sq04_decode_bridge_record | {"type":"player_action","action":"w2_sq04_decode_bridge_record"} | type="quest_completed" quest_id="w2_sq04"; type="milestone_not_set" milestone="ms_w2_bridge_record_decoded" |
| w3_sq14_weaponize_ledger | {"type":"player_action","action":"w3_sq14_weaponize_ledger"} | type="milestone_set" milestone="ms_w3_ledger_stolen"; type="quest_not_completed" quest_id="w3_sq14" |
| find_vhs_tape_06 | {"type":"player_action","action":"find_vhs_tape_06"} | none |
| find_vhs_tape_07 | {"type":"player_action","action":"find_vhs_tape_07"} | none |
| find_vhs_tape_09 | {"type":"player_action","action":"find_vhs_tape_09"} | none |

Linked quest definitions reviewed: `w2_mq02` (The Signal), `w2_mq03` (Sleeping Giant), `w2_mq05` (Liftoff), `w2_sq04` (Ancient Echoes), `w3_mq11` (Homecoming), `w3_mq12` (The Plan), `w3_sq11` (Neon Fix), `w3_sq12` (Cold Case), `w3_mq13` (Social Engineering), `w3_mq14` (The Lens), `w3_mq15` (Burn Notice), `w3_sq14` (Corporate Espionage), `w4_mq16` (Into the Fire), `w4_mq17` (Ghost in the Machine), `w4_sq16` (Sabotage), `w4_sq17` (Lost Worker), `w4_sq18` (The Scrap Heap), `w4_mq18` (The Assembly), `w4_mq19` (The Anvil), `w4_mq20` (Meltdown), `w4_sq20` (Overclocked), `w5_mq22` (Zero G), `w5_sq21` (Employee of the Month), `w5_sq22` (Solar Maintenance), `w5_mq23` (The Core Approach), `w5_mq24` (The Anchor), `w5_mq25` (Critical Mass).
