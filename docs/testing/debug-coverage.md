# Generated debug coverage inventory

Regenerate: `python scripts/debug_coverage.py`.
Check for drift: `python scripts/debug_coverage.py --check`.

These are planned assignments, not verified routes or test passes. Runtime reachability and exact fixture prerequisites require review.
Execution evidence belongs in `runs/`; regeneration never rewrites run records.

Rebuilt registry: 97 implemented entries. Built does not mean gameplay passed.

## Asset inventory

| Kind | Rows |
| --- | ---: |
| arcade | 6 |
| characters | 5 |
| cinematics | 51 |
| dialogue | 275 |
| enemies | 47 |
| events | 405 |
| exit | 928 |
| fishing_catch | 38 |
| fishing_lures | 6 |
| fishing_rods | 5 |
| fishing_zone | 6 |
| hub_nodes | 64 |
| hubs | 13 |
| items | 237 |
| milestones | 294 |
| npcs | 23 |
| patrol_zone | 2 |
| quest_stage | 94 |
| quest_task | 241 |
| quests | 60 |
| recipes_cooking | 15 |
| recipes_tinkering | 27 |
| room_action | 958 |
| rooms | 465 |
| shops | 10 |
| skills | 121 |
| statuses | 22 |
| system_behavior | 28 |
| tuning_puzzles | 6 |
| tutorial_scripts | 16 |
| worlds | 7 |

## Exact quest scenario backlog

Each quest scenario must start before acceptance/its first action and finish after payout and onward access.
Stage and task rows in the JSON ledger enumerate the required intermediate checks.

| Planned ID | Quest | Hub |
| --- | --- | --- |
| campaign_w1_mq01 | Wake Up Call | hub_1_homestead |
| campaign_w1_mq02 | Shift Clearance | hub_1_homestead |
| campaign_w1_mq03 | The Echo | hub_2_logistics |
| campaign_w1_mq04 | Red Alert | hub_2_logistics |
| campaign_w1_mq05 | The Launch | hub_2_logistics |
| campaign_w1_sq01 | The Scavenger's Stash | hub_1_homestead |
| campaign_w1_sq02 | System Flush | hub_1_homestead |
| campaign_w1_sq03 | Heavy Lifting: Required Training | hub_2_logistics |
| campaign_w1_sq04 | Protocol Override | hub_2_logistics |
| campaign_w1_sq05 | The Lost Shift | hub_2_logistics |
| campaign_w2_mq01 | A Strange Coast | hub_3_sector9 |
| campaign_w2_mq02 | The Signal | hub_3_sector9 |
| campaign_w2_mq03 | Sleeping Giant | hub_4_facility |
| campaign_w2_mq04 | The Hunter | hub_3_sector9 |
| campaign_w2_mq05 | Liftoff | hub_4_facility |
| campaign_w2_sq01 | Botanist | hub_3_sector9 |
| campaign_w2_sq02 | Lost Patrol | hub_3_sector9 |
| campaign_w2_sq03 | Tideglass Day | hub_3_sector9 |
| campaign_w2_sq04 | Ancient Echoes | hub_4_facility |
| campaign_w2_sq05 | Stolen Tech | hub_4_facility |
| campaign_w3_mq11 | Homecoming | hub_5_lower_city |
| campaign_w3_mq12 | The Plan | hub_5_lower_city |
| campaign_w3_sq11 | Neon Fix | hub_5_lower_city |
| campaign_w3_sq12 | Cold Case | hub_5_lower_city |
| campaign_w3_sq13 | Night Market Run | hub_5_lower_city |
| campaign_w3_mq13 | Social Engineering | hub_6_upper_city |
| campaign_w3_mq14 | The Lens | hub_6_upper_city |
| campaign_w3_mq15 | Burn Notice | hub_6_upper_city |
| campaign_w3_sq14 | Corporate Espionage | hub_6_upper_city |
| campaign_w3_sq15 | Prototype Testing | hub_6_upper_city |
| campaign_w4_mq16 | Into the Fire | hub_7_slag_pits |
| campaign_w4_mq17 | Ghost in the Machine | hub_7_slag_pits |
| campaign_w4_sq16 | Sabotage | hub_7_slag_pits |
| campaign_w4_sq17 | Lost Worker | hub_7_slag_pits |
| campaign_w4_sq18 | The Scrap Heap | hub_7_slag_pits |
| campaign_w4_mq18 | The Assembly | hub_8_assembly_line |
| campaign_w4_mq19 | The Anvil | hub_8_assembly_line |
| campaign_w4_mq20 | Meltdown | hub_8_assembly_line |
| campaign_w4_sq19 | Quality Control | hub_8_assembly_line |
| campaign_w4_sq20 | Overclocked | hub_8_assembly_line |
| campaign_w5_mq21 | Docking Procedure | hub_9_orbital_ring |
| campaign_w5_mq22 | Zero G | hub_9_orbital_ring |
| campaign_w5_sq21 | Employee of the Month | hub_9_orbital_ring |
| campaign_w5_sq22 | Solar Maintenance | hub_9_orbital_ring |
| campaign_w5_sq23 | Vacuum Seal | hub_9_orbital_ring |
| campaign_w5_mq23 | The Core Approach | hub_10_deep_ring |
| campaign_w5_mq24 | The Anchor | hub_10_deep_ring |
| campaign_w5_mq25 | Critical Mass | hub_10_deep_ring |
| campaign_w5_sq24 | Ghost in the Shell | hub_10_deep_ring |
| campaign_w5_sq25 | Admin Privileges | hub_10_deep_ring |
| campaign_w6_mq26 | Fractured Minds | hub_11_event_horizon |
| campaign_w6_mq27 | The Echo of the Mines | hub_11_event_horizon |
| campaign_w6_mq28 | The Crossing | hub_11_event_horizon |
| campaign_w6_sq26 | Jed's Echo | hub_11_event_horizon |
| campaign_w6_sq27 | The HR Record | hub_11_event_horizon |
| campaign_w6_sq28 | Elara's Song | hub_11_event_horizon |
| campaign_w6_mq29 | The Spire of Thought | hub_12_singularity |
| campaign_w6_mq30 | The Final Note | hub_12_singularity |
| campaign_w6_sq29 | The Aethel Grave | hub_12_singularity |
| campaign_w6_sq30 | The Final Scavenge | hub_12_singularity |

## Legacy review

91 existing menu entries; 99 menu/explicit dispatch IDs in the migration inventory.
See `debug-legacy-migration.json` for shared setup groups and hidden launchers.
No legacy entry is deleted merely because it shares a destination.
