# Hub 1 Production Lock

This document is the working lock sheet for building Hub 1: Homestead Quarter. Use it to decide which rooms are essential, which rooms can be cut or simplified, and when final room art should begin.

## Current Lock Status

Hub 1 is structurally ready to design in detail.

- Live data has 5 Hub 1 nodes and 40 Hub 1 rooms.
- The story spine is now clear: wake in the Pit, report to Jed, open the colony, complete optional Trade Row and Med-Bay loops, learn Guard Break through Heavy Lifting, get denied at the checkpoint, then get Zeke's badge override.
- `w1_mq01` has passing in-app Maestro coverage from new game through Jed's Workshop tinkering completion and the `Wake Up Call` quest-complete popup.
- `w1_sq01` and `w1_sq02` have playable first-pass data and passing in-app Maestro coverage. They are no longer content-design blockers.
- `w1_sq03` has passing in-app Maestro coverage through Bogs' intro, loader/cargo quest updates, Hydraulic Kick unlock, Acoustic Bulwark training victory, spoils, level-up, quest completion, and return to the room.
- If the player reaches `The Echo` without Guard Break training, Bogs now redirects them into `Heavy Lifting` before the Deep Mine route.
- Final artwork should begin only for rooms marked `Art Ready`.
- Rooms marked `Design First` need their gameplay action, quest step, or blocker clarified before final art.
- Rooms marked `Flavor Later` can use placeholder art until the first playable Hub 1 pass feels good.

## Hub 1 Node Order

| Order | Node | Room Count | Main Job | Required Before Hub 2? | Art Timing |
| --- | --- | ---: | --- | --- | --- |
| 1 | The Pit | 10 | Home, poverty, movement, interaction tutorial. | Yes | Route playtested; start now for core rooms. |
| 2 | Jed's Workshop | 8 | Jed relationship, tinkering, home base, Heavy Lifting setup. | Yes | Start now for core rooms. |
| 3 | Med-Bay | 7 | Optional side quest, Dominion neglect, corrosion/status teaching. | No | Eligible for art pass; `sidequest_system_flush.yaml` passes with banner/backtracking polish applied. |
| 4 | Trade Row | 9 | Shop, Scrapper, old resistance stash, optional economy loop. | No | Eligible for art pass; `sidequest_scavenger_stash.yaml` passes. |
| 5 | Transit Checkpoint | 6 | Paperwork gate, Zeke save, Hub 2 transition. | Yes | Start after `w1_mq02` flow is playtested. |

## Required Hub 1 Beats

| Beat | Required Rooms | Current Data State | Build Status |
| --- | --- | --- | --- |
| Wake in Nova's bunk | `pit_nova_bunk`, `pit_L2_corridor` | Quest starts on new game; bunk/netting actions exist. | In-app Maestro flow passes through bunk inspection and quest update. |
| Move through home | `pit_shaft`, `pit_L1_landing`, `pit_mess` | Rooms exist with flavor actions. | Critical route passes; compact description/presence dock polish applied. |
| Meet Jed | `workshop_floor` | Dialogue and task triggers exist. | In-app Maestro flow passes through Jed intro dialogue. |
| Tinkering tutorial | `workshop_floor` | Tinkering entry completes `w1_mq01` after Jed talk. | In-app Maestro flow passes through Functional Cryo-Inductor crafting and quest completion. |
| Scavenger's Stash | `trade_scrapper`, `trade_stash` | Scrapper starts `w1_sq01`; stash room/action advances quest; Scrapper turn-in completes it. | In-app Maestro flow passes through stash discovery, rebel-cache pickup, return dialogue, and completion. |
| System Flush | `medbay_exam1`, `medbay_vents` | Doc starts `w1_sq02`; vent room/action advances quest; Doc turn-in completes it. | In-app Maestro flow passes; banner/backtracking polish applied. |
| Heavy Lifting | `workshop_dock` | Bogs starts `w1_sq03`; loader/cargo actions exist; Acoustic Bulwark training victory completes quest. | In-app Maestro flow passes through Hydraulic Kick unlock, Acoustic Bulwark victory, spoils, level-up, quest completion, and return to exploration. |
| Checkpoint denial | `checkpoint_queue`, `checkpoint_bay`, `checkpoint_booth` | Enter/action events and Hank/Zeke dialogue exist. | In-app Maestro flow passes through Hank denial, Zeke override, and Paperwork completion. |
| Badge gate | `checkpoint_door`, `checkpoint_tunnel` | Door requires `mine_access_badge`; Concourse entry starts `The Echo`; Bogs redirects untrained Nova into Heavy Lifting before Deep Mine pressure. | In-app Maestro flow passes through badge gate, Concourse handoff, MQ03 start, and Bogs' Guard Break training signpost. |

## Room Purpose Lock

### The Pit

| Room | Purpose | Quest/Beat | Status | Art Direction |
| --- | --- | --- | --- | --- |
| `pit_nova_bunk` | New-game start room only; establishes Nova's poverty and personal stakes. It is not the node's hub entry/exit. | `w1_mq01` wake/interact. | Art Ready | Small pod, netting, patched gear, oppressive PA glow. |
| `pit_L2_corridor` | First navigation hallway; shows stacked sleeping pods. | `w1_mq01` route. | Art Ready | Claustrophobic vertical worker housing. |
| `pit_storage` | Optional small loot/flavor room. | Flavor. | Flavor Later | Tool closet, stolen odds and ends. |
| `pit_shaft` | Vertical connector between home levels. | `w1_mq01` route. | Art Ready | Industrial lift shaft, unsafe catwalks. |
| `pit_jed_bunk` | Jed personal lore and foreshadowing. | Flavor/Jed. | Flavor Later | Sparse bunk, repair notes, hidden warmth. |
| `pit_vents` | Secret crawlspace and possible future SQ02 link. | Exploration flavor. | Design First | Low vent tunnel, dripping condensation. |
| `pit_showers` | Poverty/body horror texture; optional loot. | Flavor. | Flavor Later | Communal industrial wash stalls. |
| `pit_L1_landing` | Main Pit entry/exit point and the room used when entering The Pit from the Hub 1 map. | `w1_mq01` route. | Art Ready | Worker traffic node, hazard signage. |
| `pit_mess` | Social space; shows community under pressure. | Flavor and possible Zeke volunteer location. | Design First | Communal tables, ration line, memorial wall nearby. |
| `pit_kitchen` | Small slop/ration flavor room. | Flavor. | Flavor Later | Steam, stained vats, ration packs. |

### Jed's Workshop

| Room | Purpose | Quest/Beat | Status | Art Direction |
| --- | --- | --- | --- | --- |
| `workshop_yard` | Transition from colony grit into Jed's space. | `w1_mq01` route. | Art Ready | Scrap piles, rusted shutter, salvage silhouettes. |
| `workshop_floor` | Core home-base room; Jed, tinkering, first quest completion. | `w1_mq01`. | Art Ready | Warmest Hub 1 room, cluttered workbench, patched lights. |
| `workshop_office` | Private Jed lore and future emotional setup. | Flavor/Jed. | Flavor Later | Cramped desk, old parts, hidden Chime foreshadowing if needed. |
| `workshop_loft` | Optional overlook/storage. | Flavor/loot. | Flavor Later | Rickety platform over shop floor. |
| `workshop_shed` | Materials room and route to dock/basement. | Exploration. | Design First | Tool wall, crates, side door to loader area. |
| `workshop_basement` | Secret mod/loot candidate. | Optional reward. | Design First | Flooded storage, leaking conduit, rare part stash. |
| `workshop_dock` | Heavy Lifting quest room and Guard Break training. | `w1_sq03`. | Art Ready | Loader, collapsed cargo, trapped-worker hints, riot shield trainer. |
| `workshop_back` | Alley/back-door transition; breathing room after dock. | Flavor/transition. | Flavor Later | Narrow service exit, dolly, grease-lit alley. |

### Med-Bay

| Room | Purpose | Quest/Beat | Status | Art Direction |
| --- | --- | --- | --- | --- |
| `medbay_hall` | Entry hub for Doc's space. | `w1_sq02` setup. | Art Ready After Feel Pass | Overcrowded triage benches, flickering clinical light. |
| `medbay_exam1` | Doc quest giver and neglect reveal. | `w1_sq02`. | Art Ready After Feel Pass | Stained white tiles, old scanner, injured miners. |
| `medbay_storage` | Medical loot and denied-resource texture. | `w1_sq02`/loot. | Flavor Later | Expired supplies, locked vaccine cabinet. |
| `medbay_vents` | Toxic blockage and combat/status lesson. | `w1_sq02`. | Art Ready After Feel Pass | Rust fans, green chemical haze, patchable leak. |
| `medbay_exhaust` | Exterior vent endpoint and visual scale. | `w1_sq02` endpoint. | Art Ready After Feel Pass | Canyon view through huge exhaust pipe. |
| `medbay_decon` | Transition airlock. | Flavor. | Flavor Later | Sterilant nozzles, harsh warning stripes. |
| `medbay_morgue` | Retired-asset horror. | Lore. | Art Ready Later | Cold drawers, numbered bodies, no names. |

### Trade Row

| Room | Purpose | Quest/Beat | Status | Art Direction |
| --- | --- | --- | --- | --- |
| `trade_entrance` | Market threshold and tonal contrast. | Exploration. | Art Ready Later | Flickering neon arch, crowd pressure. |
| `trade_strip` | Central market route. | `w1_sq01` path/shop access. | Art Ready After Feel Pass | Stalls, contraband, illegal power cells. |
| `trade_scrapper` | Scrapper quest giver and shop. | `w1_sq01`. | Art Ready After Feel Pass | Reinforced stall, datapad glow, paranoid clutter. |
| `trade_stash` | Rebel cache reveal. | `w1_sq01`. | Art Ready After Feel Pass | Hidden crate, scratched rebel names, damped scanner field. |
| `trade_bar` | Social rumor room. | Flavor. | Flavor Later | Cheap glasses, broken jukebox, exhausted miners. |
| `trade_den` | Optional gambling/lore room. | Flavor. | Flavor Later | Holo-card table, smoke, bad wagers. |
| `trade_locker` | High-tier locked loot candidate. | Optional reward. | Design First | Cold biometric locker, contraband protection. |
| `trade_gantry` | Overlook/scouting flavor. | Flavor. | Flavor Later | Condensed railings, view over market strip. |
| `trade_maint` | Maintenance/sewer transition and possible stash alternate. | `w1_sq01` optional branch. | Design First | Pumps, oily pipe, hidden crawlspace. |

### Transit Checkpoint

| Room | Purpose | Quest/Beat | Status | Art Direction |
| --- | --- | --- | --- | --- |
| `checkpoint_queue` | Bureaucratic pressure and MQ02 start. | `w1_mq02`. | Art Ready | Miners in line, biometric scanners, shift signage. |
| `checkpoint_bay` | Hank denial and search bay. | `w1_mq02`. | Art Ready | Scanning arches, reinforced counter, shock baton. |
| `checkpoint_booth` | Zeke override scene. | `w1_mq02`. | Art Ready | Glass booth, liability scores, camera watching Zeke. |
| `checkpoint_cell` | Shows consequences of non-compliance. | Lore. | Flavor Later | Cage, scratched names, asset graffiti. |
| `checkpoint_door` | Physical badge gate. | `w1_mq02` exit gate. | Art Ready | Huge blast door, red lock console. |
| `checkpoint_tunnel` | Transition to Hub 2. | Hub 2 handoff. | Art Ready | Sterile tube, moving walkway, colder light. |

## Art Start Rules

Current asset audit:

- Existing Hub 1 room backgrounds: `pit_nova_bunk`, `workshop_floor`, `checkpoint_queue`.
- Missing Hub 1 room backgrounds: 37 of 40.
- Missing Hub 1 hub/node images: `homestead`, `pit`, `workshop`, `medbay`, `trade_row`, `checkpoint`.

Start final art now for:

- `pit_nova_bunk`
- `pit_L2_corridor`
- `pit_shaft`
- `pit_L1_landing`
- `workshop_yard`
- `workshop_floor`
- `workshop_dock`
- `checkpoint_queue`
- `checkpoint_bay`
- `checkpoint_booth`
- `checkpoint_door`
- `checkpoint_tunnel`

First art batch:

| Priority | Asset | Why |
| --- | --- | --- |
| 1 | `images/hubs/homestead.png` | Hub identity and node map backdrop. |
| 2 | `images/nodes/pit.png` | First node icon. |
| 3 | `images/nodes/workshop.png` | Home-base node icon. |
| 4 | `images/nodes/checkpoint.png` | Hub 2 transition node icon. |
| 5 | `images/rooms/world_1/pit_corridor.png` | First navigation room after waking. |
| 6 | `images/rooms/world_1/pit_shaft.png` | Core Pit connector. |
| 7 | `images/rooms/world_1/pit_landing.png` | Pit exit route. |
| 8 | `images/rooms/world_1/workshop_yard.png` | Arrival at Jed's space. |
| 9 | `images/rooms/world_1/workshop_dock.png` | Heavy Lifting and Guard Break training. |
| 10 | `images/rooms/world_1/checkpoint_bay.png` | Hank denial and checkpoint pressure. |
| 11 | `images/rooms/world_1/checkpoint_booth.png` | Zeke override scene. |
| 12 | `images/rooms/world_1/checkpoint_door.png` | Badge gate. |
| 13 | `images/rooms/world_1/checkpoint_tunnel.png` | Transition to Hub 2. |

Do not start final art yet for:

- Optional flavor rooms until the critical path playtest confirms pacing.
- Med-Bay and Trade Row final room art until their in-app feel pass confirms route length and interaction placement.

Second art batch after feel pass:

| Priority | Asset | Why |
| --- | --- | --- |
| 1 | `images/nodes/trade_row.png` | Optional exploration node icon. |
| 2 | `images/nodes/medbay.png` | Optional exploration node icon. |
| 3 | `images/rooms/world_1/trade_strip.png` | Central Trade Row navigation. |
| 4 | `images/rooms/world_1/trade_scrapper.png` | SQ01 giver and shop. |
| 5 | `images/rooms/world_1/trade_stash.png` | SQ01 reveal room. |
| 6 | `images/rooms/world_1/medbay_hall.png` | Med-Bay entry route. |
| 7 | `images/rooms/world_1/medbay_exam.png` | SQ02 giver and Doc identity. |
| 8 | `images/rooms/world_1/medbay_vents.png` | SQ02 action room. |
| 9 | `images/rooms/world_1/medbay_exhaust.png` | SQ02 endpoint and scale shot. |

## Immediate Build Checklist

1. Keep `w1_sq03` in the regression set if later combat tuning touches tutorial HP, skill damage, spoils, level-up, or quest-completion timing.
2. Extend or refine `w1_sq01` and `w1_sq02` coverage only if later pacing/content changes touch those routes.
3. Keep the checkpoint/Bogs redirect flow in the regression set if later pacing changes touch the checkpoint, Concourse, Deep Mine, or Guard Break onboarding route.
4. Start first art batch once the critical-path feel pass is acceptable.
