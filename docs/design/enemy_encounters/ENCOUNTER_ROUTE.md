# Proposed encounter route

Implementation update (November 13): the World 1 Warden and World 2 mechanics are implemented locally with temporary art. See [current progress](PROGRESS.md); other redesigns and final art remain proposed. The original design below remains the reference.
Status: design candidate; these formations are not installed or implemented. This appendix assigns an explicit disposition to every current campaign encounter source. The source snapshot records location, room gates, instance overrides and event conditions.

A source is one static fight, one scripted spawn or one campaign patrol. Multiple source rows can merge into one proposed fight. The route is organized by node and source location rather than claiming a new strict play order; use each current world walkthrough for actual walking directions. Introduction constraints below must be reconciled with any optional branch entered early.

## Introduction and pressure order

- W1: preserve Loader → Guard Break access → mine combinations → launch pressure → Warden. Optional patrol never occupies protected teaching/recovery rooms.
- W2: introduce the local glider/spitter, vines and hounds before richer ecology combinations; show iris charge before laboratory pairing; retain earned companion/tool access before Beast. Optional branch entries get an opening tell rather than assuming a previous optional lesson.
- W3: crawler feeding → specialized city units → squad support → short rooftop prelude → recovery opportunity → Administrator. Optional relay squads must have an initial teaching cycle if encountered before the main route.
- W4: first Magma Drone → Golem/coolant introductions → finite Welder pairing → magnetic marks/Phantom commitment → Core combinations → Titan. Crucible is optional and allows recovery between rounds.
- W5: fighter screen → Screener and gravity lesson → Security squad → alignment and beam combinations → Gamma elite → earned Anchor/hunter response → reflector units → Avatar duet. Do not assume optional Service Shaft travel taught tether behavior; give the first tether squad a slow opening.
- W6: rescue order can vary; each rescue has Nova-accessible responses. Optional fragments are separate from rescue identities. Introduce copy/reticle behavior before the full memory defenses, then the linked finale.

## Reading the table

**Retain** keeps the current formation while allowing the declared behavior/presentation work. **Remix/replace/elite/boss/pursuit** specifies its new composition. **Retire** removes that fight for a stated pacing purpose. **Merge/relocate** points to the single destination encounter. No row authorizes a new exit gate.

The supply column lists guaranteed base/instance items, not all chance drops or quest rewards. Items on a replaced kind remain a one-time formation entitlement. Detailed baseline rewards, occurrence overrides and migration requirements are in PLAN.json and IMPLEMENTATION.md.

## World 1 — The Pit

11 existing encounter sources → 11 proposed opportunities/stages. This is not a required-combat count.

| Source / room | Current formation | Proposed formation / disposition | Purpose and contract | Guaranteed items |
| --- | --- | --- | --- | --- |
| **Scrap Yard** (`workshop_yard`)<br>`static:workshop_yard:1` | Faulted Loader | **retain**: Faulted Loader | Opening relay lesson; retain reduced instance and north gate. Retain existing combat gate. | scrap_metal ×1 |
| **Ventilation Hub** (`medbay_vents`)<br>`static:medbay_vents:1` | Siren Skimmer + Siren Skimmer | **retain**: Siren Skimmer + Siren Skimmer | Read inflation and choose single-target interruption versus area damage. | — |
| **Security Post** (`admin_security`)<br>`static:admin_security:1` | Acoustic Bulwark | **retain**: Acoustic Bulwark | Keep a single-variable Guard Break drill and guaranteed Wiring Bundle. | wiring_bundle ×1 |
| **Aisle 01** (`server_aisle1`)<br>`static:server_aisle1:1` | Resonance Buoy + Resonance Buoy | **retain**: Resonance Buoy + Resonance Buoy | Two staggered capacitor charges; teach interruption without simultaneous burst. | — |
| **Main Tunnel Alpha** (`mine_alpha`)<br>`static:mine_alpha:1` | Echo-Borer + Echo-Borer | **retain**: Echo-Borer + Echo-Borer | Pack exposure timing; preserve first-mine victory task. | — |
| **Riot Control Post** (`mine_checkpoint`)<br>`static:mine_checkpoint:1` | Acoustic Bulwark + Dominion Dampener | **retain**: Acoustic Bulwark + Dominion Dampener | Break protection or stop suppression; preserve main quest listener. | — |
| **Maintenance Access** (`launch_access`)<br>`static:launch_access:1` | Siren Skimmer + Echo-Borer | **retain**: Siren Skimmer + Echo-Borer | A final mixed organic target-priority test before security. | — |
| **Cargo Lift** (`launch_lift`)<br>`static:launch_lift:1` | Acoustic Bulwark + Resonance Buoy | **retain**: Acoustic Bulwark + Resonance Buoy | Interrupt charge or break the shield; preserve north gate and gauntlet task. Retain existing combat gate. | — |
| **Security Checkpoint B** (`launch_checkpoint`)<br>`static:launch_checkpoint:1` | Dominion Dampener + Resonance Buoy | **retain**: Dominion Dampener + Resonance Buoy | Deal with the mark or its amplifier; preserve north gate and Buoy victory contract. Retain existing combat gate. | — |
| **Pod Bay** (`launch_bay`)<br>`static:launch_bay:1` | The Iron Warden | **boss**: The Iron Warden | Warden solo shield/overclock boss; BOSS_DESIGNS W1. | warden_keycard ×1 |
| **Conveyor Belt** (`mine_conveyor`)<br>`patrol:w1_deep_mine_pressure_patrol` | Pressure Hauler | **retain**: Pressure Hauler | Retain one optional signaled patrol, its activation and safe-room exclusions. | — |

## World 2 — Sector 9

11 existing encounter sources → 11 proposed opportunities/stages. This is not a required-combat count.

| Source / room | Current formation | Proposed formation / disposition | Purpose and contract | Guaranteed items |
| --- | --- | --- | --- | --- |
| **Mist Pools** (`sector9_stream_pools`)<br>`static:sector9_stream_pools:1` | Siren Skimmer | **replace**: Mist Glider | First wetland creature: read the dive; new world identity. | — |
| **Resonant Sedge** (`sector9_stream_wetlands`)<br>`static:sector9_stream_wetlands:1` | Spore-Spitter | **retain**: Spore-Spitter | First poison tell; preserve guaranteed Herb. | herb ×1 |
| **Glow-Moss Cavern** (`sector9_stream_cave_depths`)<br>`static:sector9_stream_cave_depths:1` | Echo-Borer + Echo-Borer | **replace**: Moss Borer + Moss Borer | Cold cavern cousins, new silhouettes; preserve east combat gate and canopy task aliases. Retain existing combat gate. | — |
| **Resonant Canopy** (`sector9_canopy`)<br>`static:sector9_canopy:1` | Stalker-Vine + Spore-Spitter | **retain**: Stalker-Vine + Spore-Spitter | Root support and poison pressure; retain reduced teaching instances. | — |
| **Spore Thickets** (`sector9_wilds_thickets`)<br>`static:sector9_wilds_thickets:1` | Razor-Vine | **retain**: Razor-Vine | One short binding hazard protecting a guaranteed Herb. | herb ×1 |
| **Hound Nest** (`sector9_wilds_nest`)<br>`static:sector9_wilds_nest:1` | Shard-Hound + Shard-Hound | **retain**: Shard-Hound + Shard-Hound | Alternating pack designation; preserve guaranteed Beast Meat and early HP overrides. | beast_meat ×1 |
| **Left Reliquary** (`sector9_foyer_left_gallery`)<br>`static:sector9_foyer_left_gallery:1` | Ruin-Guardian | **elite**: Ruin-Guardian | Read stone wards; preserve west gate and guaranteed Scrap Metal. Retain existing combat gate. | scrap_metal ×1 |
| **T-Junction Wiring** (`sector9_conduit_junction`)<br>`static:sector9_conduit_junction:1` | Sentinel Orb | **retain**: Sentinel Orb | First iris-charge lesson and guaranteed Wiring Bundle. | wiring_bundle ×1 |
| **Acoustics Lab** (`sector9_hall_acoustics_lab`)<br>`static:sector9_hall_acoustics_lab:1` | Spore-Spitter | **remix**: Sentinel Orb + Spore-Spitter | Ancient sentry and encroaching ecology: choose the next charged attack. | — |
| **High Gantry** (`sector9_vents_gantry`)<br>`static:sector9_vents_gantry:1` | Sentinel Orb | **elite**: Sentinel Orb | Elevated sentry mastery; guarantee Battery and preserve the separate cutter gate. | battery ×1 |
| **Canopy Ridge** (`sector9_canopy_ridge`)<br>`event:w2_mq04_beast_ambush` | The Source Beast | **boss**: The Source Beast | Two-pattern solo Beast; preserve player-selected Face the Beast trigger and trial outcome. | source_resin ×1 |

## World 3 — The Spire

9 existing encounter sources → 9 proposed opportunities/stages. This is not a required-combat count.

| Source / room | Current formation | Proposed formation / disposition | Purpose and contract | Guaranteed items |
| --- | --- | --- | --- | --- |
| **Archive Vault** (`spire_archive_vault`)<br>`static:spire_archive_vault:1` | Corporate Assassin | **remix**: Corporate Assassin + Concierge Relay | Assassin protected by a finite command relay; retain the Lens story contract. | — |
| **Landing Pad Roof** (`spire_landing_pad_roof`)<br>`static:spire_landing_pad_roof:1` | Aero Drone + Heavy Mech | **retain**: Aero Drone + Heavy Mech | Short security prelude with nonoverlapping tells; recovery opportunity before boss. | — |
| **Landing Pad Roof** (`spire_landing_pad_roof`)<br>`static:spire_landing_pad_roof:2` | Administrator + Aero Drone | **boss**: Administrator + Aero Drone | Finite-reserve Administrator encounter; starting drone counts toward reserves. | power_cell ×2 |
| **Pump Gallery** (`spire_sewers_scale_01`)<br>`static:spire_sewers_scale_01:1` | Sewer Crawler | **retain**: Sewer Crawler | Teach the feeding pause and preserve landing-clear quest credit. | — |
| **Filter Sluice** (`spire_sewers_scale_02`)<br>`static:spire_sewers_scale_02:1` | Sewer Crawler + Sewer Crawler | **retain**: Sewer Crawler + Sewer Crawler | Pair develops the feeding lesson into target priority. | — |
| **Private Booth** (`spire_exec_lounge_scale_02`)<br>`static:spire_exec_lounge_scale_02:1` | Sentinel Mk. I | **elite**: Sentinel Mk. I | Optional quarantine elite; guaranteed Neon Sign Core remains here. | neon_sign_core ×1 |
| **Service Dumbwaiter** (`spire_exec_lounge_scale_05`)<br>`static:spire_exec_lounge_scale_05:1` | Aero Drone | **remix**: Riot Guard + Aero Drone | City shield/air squad; Circuit Board remains a one-time formation reward. | circuit_board ×1 |
| **Fuel Bridge** (`spire_landing_pad_scale_01`)<br>`static:spire_landing_pad_scale_01:1` | Aero Drone | **remix**: Aero Drone + Concierge Relay | Interrupt the flight command; preserve guaranteed Wiring Bundle. | wiring_bundle ×1 |
| **Drone Wreck Roof** (`spire_landing_pad_scale_03`)<br>`static:spire_landing_pad_scale_03:1` | Corporate Assassin | **elite**: Corporate Assassin | Optional marked-lunge duel; preserve two Ration Packs. | ration_pack ×2 |

## World 4 — The Foundry

41 existing encounter sources → 27 proposed opportunities/stages. This is not a required-combat count.

| Source / room | Current formation | Proposed formation / disposition | Purpose and contract | Guaranteed items |
| --- | --- | --- | --- | --- |
| **Slag River** (`foundry_slag_river`)<br>`static:foundry_slag_river:1` | Magma Drone | **retain**: Magma Drone | First heat/cooling lesson; preserve east crossing combat gate. Retain existing combat gate. | — |
| **Stepping Stones** (`foundry_slag_stepping_stones`)<br>`static:foundry_slag_stepping_stones:1` | Magma Drone | **retire**: — | Let the crossing puzzle breathe after the River drone. | — |
| **Waste Intake** (`foundry_waste_intake`)<br>`static:foundry_waste_intake:1` | Slag Golem | **elite**: Slag Golem | First readable Slam/cooling elite; no second immediately stacked fight. | — |
| **Waste Intake** (`foundry_waste_intake`)<br>`static:foundry_waste_intake:2` | Slag Golem + Magma Drone | **merge**: — | Move this mixed pressure test to the optional Magnet Ramp. Destination: `static:foundry_waste_intake_scale_03:1`. | — |
| **Service Airlock** (`foundry_service_airlock`)<br>`static:foundry_service_airlock:1` | Slag Golem | **replace**: Quench Mite | Short coolant-scavenger introduction before the machinery combinations. | — |
| **Decon Chamber** (`foundry_decon_chamber`)<br>`static:foundry_decon_chamber:1` | Slag Golem | **retire**: — | Quiet decontamination and story/control inspection after the introduction. | — |
| **Conveyor Belt** (`foundry_conveyor_belt`)<br>`static:foundry_conveyor_belt:1` | Welder Bot + Magma Drone | **retain**: Welder Bot + Magma Drone | Finite repair plus heat pressure; retain existing support lesson. | — |
| **Conveyor Belt** (`foundry_conveyor_belt`)<br>`static:foundry_conveyor_belt:2` | Phantom | **merge**: — | Concentrate the Phantom introduction at Reject Bay. Destination: `static:foundry_reject_bay:1`. | — |
| **Service Walk** (`foundry_belt_service_walk`)<br>`static:foundry_belt_service_walk:1` | Welder Bot | **retire**: — | Keep maintenance travel and recovery from becoming another Welder duel. | — |
| **Reject Bay** (`foundry_reject_bay`)<br>`static:foundry_reject_bay:1` | Phantom | **elite**: Phantom | First complete cloak/commitment lesson; one clear elite encounter. | — |
| **Conditioning Chamber** (`foundry_conditioning_chamber`)<br>`static:foundry_conditioning_chamber:1` | Flame Trooper | **remix**: Flame Trooper + Magnet Sifter | Teach the magnetic mark before its partner cone; never chain both controls. | — |
| **The Forge** (`foundry_forge_anvil`)<br>`static:foundry_forge_anvil:1` | Conveyor Crusher | **retain**: Conveyor Crusher | Short rhythm hazard, not an extra squad during relic acquisition. | — |
| **Power Core** (`foundry_power_core`)<br>`static:foundry_power_core:1` | Flame Trooper | **remix**: Flame Trooper + Phantom | One authored lock-and-lunge squad instead of two solo fights; fixed combined pressure budget. | — |
| **Power Core** (`foundry_power_core`)<br>`static:foundry_power_core:2` | Phantom | **merge**: — | Second existing solo becomes part of the first squad, not another reward roll. Destination: `static:foundry_power_core:1`. | — |
| **Engine Service Ring** (`foundry_engine_service_ring`)<br>`static:foundry_engine_service_ring:1` | Flame Trooper | **retain**: Flame Trooper | Keep the optional supply duel and two guaranteed Ration Packs. | ration_pack ×2 |
| **Titan Dock** (`foundry_titan_dock`)<br>`static:foundry_titan_dock:1` | Titan Walker | **boss**: Titan Walker + Missile Battery + Cutting Arm + Anchor Cannon + Shield Projector | Four destructible systems and one chassis, shared action budget; preserve north escape gate. Retain existing combat gate. | power_cell ×3 |
| **Heat Shadow Alcove** (`foundry_obsidian_shelf_scale_02`)<br>`static:foundry_obsidian_shelf_scale_02:1` | Magma Drone | **retire**: — | Make Heat Shadow Alcove a dependable refuge. | — |
| **Coolant Spillway** (`foundry_slag_river_scale_02`)<br>`static:foundry_slag_river_scale_02:1` | Welder Bot | **retain**: Welder Bot | Keep the supplies fight: two guaranteed Pure Iron for the arcade repair. | pure_iron ×2 |
| **Slag Ferry Winch** (`foundry_slag_river_scale_03`)<br>`static:foundry_slag_river_scale_03:1` | Slag Golem | **remix**: Slag Golem + Quench Mite | Cooling windows and a coolant scavenger; distinguish from the main Golem lesson. | — |
| **Worker Marker** (`foundry_cooling_springs_scale_02`)<br>`static:foundry_cooling_springs_scale_02:1` | Magma Drone | **retire**: — | Worker memorial and recovery branch: use atmosphere, not another drone. | — |
| **Records Shed** (`foundry_waste_intake_scale_02`)<br>`static:foundry_waste_intake_scale_02:1` | Welder Bot | **retire**: — | Records and clues become a deliberate quiet connector. | — |
| **Magnet Ramp** (`foundry_waste_intake_scale_03`)<br>`static:foundry_waste_intake_scale_03:1` | Slag Golem | **remix**: Slag Golem + Magnet Sifter | Optional mark-and-slam coordination test, taught before lethal combination. | — |
| **Decon Drain** (`foundry_service_airlock_scale_02`)<br>`static:foundry_service_airlock_scale_02:1` | Magma Drone | **replace**: Quench Mite + Quench Mite | Optional paired scavengers with alternating swell timing. | — |
| **Reject Chute** (`foundry_conveyor_belt_scale_02`)<br>`static:foundry_conveyor_belt_scale_02:1` | Welder Bot | **remix**: Welder Bot + Quench Mite | Finite repair plus a swelling coolant scavenger; choose the repair interrupt or stop the cold vent. | — |
| **Roller Underpass** (`foundry_conveyor_belt_scale_03`)<br>`static:foundry_conveyor_belt_scale_03:1` | Slag Golem | **remix**: Conveyor Crusher + Magma Drone | A declared press cycle and heat charger; pressure must fit one shared window. | — |
| **Quench Lane** (`foundry_conditioning_scale_02`)<br>`static:foundry_conditioning_scale_02:1` | Magma Drone | **remix**: Magma Drone + Quench Mite | Opposed heat/coolant timing and different target priorities. | — |
| **Anvil Shadow** (`foundry_forge_scale_02`)<br>`static:foundry_forge_scale_02:1` | Welder Bot | **retire**: — | Relic aftermath and anvil atmosphere remain quiet. | — |
| **Mold Library** (`foundry_forge_scale_03`)<br>`static:foundry_forge_scale_03:1` | Slag Golem | **elite**: Slag Golem | Optional cooling-window mastery in a mold enclosure; no repair support repeat. | — |
| **Engine Cradle** (`foundry_power_core_scale_02`)<br>`static:foundry_power_core_scale_02:1` | Magma Drone | **retire**: — | Give the engine hardware discovery space after Power Core combat. | — |
| **Ammo Lift** (`foundry_titan_dock_scale_02`)<br>`static:foundry_titan_dock_scale_02:1` | Welder Bot | **remix**: Flame Trooper + Welder Bot | Optional supply-guard squad: interrupt finite welds or the cone. | — |
| **Escape Rail** (`foundry_titan_dock_scale_03`)<br>`static:foundry_titan_dock_scale_03:1` | Slag Golem | **retire**: — | Reserve tension for Titan and the escape; no extra Golem on the rail. | — |
| **Foundry River Control** (`foundry_slag_river_scale_final`)<br>`static:foundry_slag_river_scale_final:1` | Magma Drone | **remix**: Magma Drone + Magma Drone | Optional two-burst timing mastery; offset charges, no burst stack. | — |
| **Foundry Sorter Spine** (`foundry_waste_intake_scale_final`)<br>`static:foundry_waste_intake_scale_final:1` | Welder Bot | **remix**: Welder Bot + Magnet Sifter | One meaningful sorting squad; consolidate the two independent solos. | — |
| **Foundry Sorter Spine** (`foundry_waste_intake_scale_final`)<br>`static:foundry_waste_intake_scale_final:2` | Magma Drone | **merge**: — | Remove the separate Drone duel; consolidated encounter owns the reward budget. Destination: `static:foundry_waste_intake_scale_final:1`. | — |
| **Foundry Belt Nerve** (`foundry_conveyor_belt_scale_final`)<br>`static:foundry_conveyor_belt_scale_final:1` | Conveyor Crusher | **retain**: Conveyor Crusher | Optional press mastery: richer declared pattern, same basic-response access. | — |
| **Foundry Defect Choir** (`foundry_conditioning_scale_final`)<br>`static:foundry_conditioning_scale_final:1` | Phantom | **remix**: Phantom + Welder Bot | Optional prototype repair team; preserve guaranteed Circuit Board on one formation receipt. | circuit_board ×1 |
| **Foundry Core Shadow** (`foundry_power_core_scale_final`)<br>`static:foundry_power_core_scale_final:1` | Slag Golem | **remix**: Slag Golem + Welder Bot | One cooling/repair composition; no duplicate solo reward sequence. | — |
| **Foundry Core Shadow** (`foundry_power_core_scale_final`)<br>`static:foundry_power_core_scale_final:2` | Welder Bot | **merge**: — | Merge the existing Welder into the first party. Destination: `static:foundry_power_core_scale_final:1`. | — |
| **The Scrapper's Crucible** (`foundry_crucible_arena`)<br>`event:w4_crucible_spawn_round_1` | Magma Drone + Magma Drone + Welder Bot | **retain**: Magma Drone + Magma Drone + Welder Bot | Optional arena support/charge test; stagger both drone windups. | — |
| **The Scrapper's Crucible** (`foundry_crucible_arena`)<br>`event:w4_crucible_spawn_round_2` | Slag Golem + Welder Bot | **retain**: Slag Golem + Welder Bot | Optional cooling/finite-repair test; rest allowed between rounds. | — |
| **The Scrapper's Crucible** (`foundry_crucible_arena`)<br>`event:w4_crucible_spawn_round_3` | Crucible Champion + Magma Drone | **boss**: Crucible Champion | Final arena solo duel; remove the accompaniment to distinguish the Champion from squad bosses. | pure_iron ×1 |

## World 5 — The Ring

42 existing encounter sources → 25 proposed opportunities/stages. This is not a required-combat count.

| Source / room | Current formation | Proposed formation / disposition | Purpose and contract | Guaranteed items |
| --- | --- | --- | --- | --- |
| **Executive Dock** (`orbital_executive_dock`)<br>`static:orbital_executive_dock:1` | Compliance Fighter + Compliance Fighter | **retain**: Compliance Fighter + Compliance Fighter | Paired designation/pass interception; preserve arrival tempo overrides and story trigger. | — |
| **Grand Concourse** (`orbital_grand_concourse`)<br>`static:orbital_grand_concourse:1` | Compliance Officer | **replace**: Customs Screener | Introduce public detention scanning with low solo damage. | — |
| **Customs Lounge** (`orbital_customs_lounge`)<br>`static:orbital_customs_lounge:1` | Compliance Officer | **retire**: — | Give Charles, the shop and the rest stop a coherent quiet room. | — |
| **Solarium** (`orbital_solarium`)<br>`static:orbital_solarium:1` | Null-G Drone | **retain**: Null-G Drone | First gravity-tether tell; basic attacks can break preparation. | — |
| **Mirror Walk** (`orbital_mirror_walk`)<br>`static:orbital_mirror_walk:1` | Null-G Drone | **replace**: Prism Surveyor | Teach beam alignment before the support unit appears in squads. | — |
| **Security Hub** (`orbital_security_hub`)<br>`static:orbital_security_hub:1` | Compliance Officer + Null-G Drone | **retain**: Compliance Officer + Null-G Drone | Detention plus gravity support; preserve the main security victory contract. | — |
| **Surveillance Pit** (`orbital_surveillance_pit`)<br>`static:orbital_surveillance_pit:1` | Compliance Officer | **remix**: Customs Screener + Void Turret | One scan/beam squad; replace two unconnected solos. | — |
| **Surveillance Pit** (`orbital_surveillance_pit`)<br>`static:orbital_surveillance_pit:2` | Null-G Drone | **merge**: — | Consolidate this presence into the first surveillance encounter. Destination: `static:orbital_surveillance_pit:1`. | — |
| **Server Farm** (`orbital_server_farm`)<br>`static:orbital_server_farm:1` | Void Turret + Void Turret | **retain**: Void Turret + Void Turret | Offset beam charge test; one compact machinery fight. | — |
| **Server Farm** (`orbital_server_farm`)<br>`static:orbital_server_farm:2` | Firewall Construct | **merge**: — | Reserve the full Firewall elite for Gamma; remove the early duplicate. Destination: `static:deep_firewall_gamma:1`. | — |
| **Mainframe Nave** (`deep_mainframe_nave`)<br>`static:deep_mainframe_nave:1` | Void Turret | **retire**: — | Thorne/Elara story space and preparation, not another turret. | — |
| **Firewall Alpha** (`deep_firewall_alpha`)<br>`static:deep_firewall_alpha:1` | Void Turret | **remix**: Void Turret + Customs Screener | Scan-and-beam test after both units have individual introductions. | — |
| **Firewall Beta** (`deep_firewall_beta`)<br>`static:deep_firewall_beta:1` | Void Turret | **remix**: Void Turret + Null-G Drone | Change target priorities from Alpha: tether versus the charging turret. | — |
| **Firewall Gamma** (`deep_firewall_gamma`)<br>`static:deep_firewall_gamma:1` | Firewall Construct | **elite**: Firewall Construct + Prism Surveyor | One definitive Firewall elite with readable frequency and fragile alignment support. | — |
| **Anchor Chamber** (`deep_anchor_chamber`)<br>`static:deep_anchor_chamber:1` | Hunter-Killer Droid | **pursuit**: Hunter-Killer Droid | One scripted hunter challenge; Anchor/Stasis disables pursuit, damage is not the required answer. | — |
| **Throne Room** (`deep_throne_room`)<br>`static:deep_throne_room:1` | Elite Guard | **elite**: Elite Guard | Reflector duel introduction; explicit safe physical/area alternatives. | — |
| **The Tear** (`deep_tear`)<br>`static:deep_tear:1` | Compliance Avatar | **boss**: Compliance Avatar + Directive Conductor | One unique boss duet with persistent broken-link consequences. | power_cell ×3 |
| **Collar Pressure Gallery** (`orbital_executive_dock_scale_02`)<br>`static:orbital_executive_dock_scale_02:1` | Compliance Officer | **retire**: — | Pressure gallery breathes between the arrival and branch test. | — |
| **Arrival Light Relay** (`orbital_executive_dock_scale_04`)<br>`static:orbital_executive_dock_scale_04:1` | Null-G Drone | **remix**: Tether Rigger + Null-G Drone | Optional capture-unit combination; offset tether preparation. | — |
| **Freight Intercom Walk** (`orbital_executive_dock_scale_06`)<br>`static:orbital_executive_dock_scale_06:1` | Compliance Officer | **retire**: — | Let the stranded-worker intercom land without an interchangeable officer. | — |
| **Inward Gauge Gallery** (`orbital_grand_concourse_scale_02`)<br>`static:orbital_grand_concourse_scale_02:1` | Compliance Officer | **remix**: Customs Screener + Null-G Drone | A scanner and gravity partner; one main test on this long branch. | — |
| **Directory Relay** (`orbital_grand_concourse_scale_04`)<br>`static:orbital_grand_concourse_scale_04:1` | Null-G Drone | **retire**: — | Directory relay is an inspection beat, not a second patrol. | — |
| **Hospitality Speaker Walk** (`orbital_grand_concourse_scale_06`)<br>`static:orbital_grand_concourse_scale_06:1` | Compliance Officer | **retire**: — | Hospitality announcements provide atmosphere between fights. | — |
| **Condensation Channel** (`orbital_solarium_scale_02`)<br>`static:orbital_solarium_scale_02:1` | Compliance Officer | **retire**: — | Condensation and artificial ecology provide a quiet connector. | — |
| **Fountain Cabinet** (`orbital_solarium_scale_04`)<br>`static:orbital_solarium_scale_04:1` | Null-G Drone | **remix**: Prism Surveyor + Null-G Drone | Alignment plus gravity, with clearly separate linked targets. | — |
| **Cooling Irrigation Walk** (`orbital_solarium_scale_06`)<br>`static:orbital_solarium_scale_06:1` | Compliance Officer | **retire**: — | Keep irrigation maintenance observational. | — |
| **Hull Inspection Gallery** (`orbital_security_hub_scale_02`)<br>`static:orbital_security_hub_scale_02:1` | Compliance Officer | **retire**: — | Hull inspection is a readable approach to the evidence squad. | — |
| **Evidence Test Relay** (`orbital_security_hub_scale_04`)<br>`static:orbital_security_hub_scale_04:1` | Null-G Drone | **remix**: Compliance Officer + Prism Surveyor | Officer command plus aim support; interrupt either coordination link. | — |
| **Incident Speaker Walk** (`orbital_security_hub_scale_06`)<br>`static:orbital_security_hub_scale_06:1` | Compliance Officer | **retire**: — | Incident recordings get a quiet aftermath. | — |
| **Tether Conduit** (`orbital_service_shaft_scale_02`)<br>`static:orbital_service_shaft_scale_02:1` | Compliance Officer | **replace**: Tether Rigger | Introduce the shaft technician before its combined optional test. | — |
| **Worklight Lockout** (`orbital_service_shaft_scale_04`)<br>`static:orbital_service_shaft_scale_04:1` | Null-G Drone | **retire**: — | Worklight lockout is an inspection/recovery interval. | — |
| **Freight Motor Walk** (`orbital_service_shaft_scale_06`)<br>`static:orbital_service_shaft_scale_06:1` | Compliance Officer | **remix**: Customs Screener + Tether Rigger | Scan plus capture: strict control budget and mandatory recovery gap. | — |
| **Cooling Plenum** (`deep_server_farm_scale_02`)<br>`static:deep_server_farm_scale_02:1` | Firewall Construct | **replace**: Prism Surveyor + Void Turret | Optional alignment/beam party instead of a second major Firewall elite. | — |
| **Chorus Tank** (`deep_anchor_chamber_scale_02`)<br>`static:deep_anchor_chamber_scale_02:1` | Hunter-Killer Droid | **retire**: — | Chorus Tank must not duplicate the supposedly singular hunter. | — |
| **Empty Cradle Afterimage** (`deep_anchor_chamber_scale_06`)<br>`static:deep_anchor_chamber_scale_06:1` | Hunter-Killer Droid | **retire**: — | Empty Cradle foreshadows absence; remove the third hunter. | — |
| **Thorne Cell** (`deep_throne_room_scale_02`)<br>`static:deep_throne_room_scale_02:1` | Elite Guard | **retire**: — | Thorne Cell is a story beat, not another shield guard. | — |
| **Boardroom Grave** (`deep_throne_room_scale_04`)<br>`static:deep_throne_room_scale_04:1` | Compliance Avatar | **replace**: Boardroom Bailiff | Unique optional judgment elite; never a second Compliance Avatar. | power_cell ×3 |
| **Avatar Gate** (`deep_throne_room_scale_06`)<br>`static:deep_throne_room_scale_06:1` | Elite Guard | **remix**: Elite Guard + Prism Surveyor | Optional reflector/aim-support mastery; safe alternate attack types stay useful. | — |
| **Choir Gap** (`deep_tear_scale_02`)<br>`static:deep_tear_scale_02:1` | Source Shadow | **replace**: Threshold Remnant | Optional incomplete fracture creature foreshadows Source without reusing its enemy ID/art. | — |
| **Last Bulkhead** (`deep_tear_scale_05`)<br>`static:deep_tear_scale_05:1` | Source Shadow | **retire**: — | Last Bulkhead preserves space around the climax. | — |
| **Ring Courtesy Theater** (`orbital_grand_concourse_scale_final`)<br>`static:orbital_grand_concourse_scale_final:1` | Compliance Officer | **remix**: Customs Screener + Prism Surveyor | Optional courtesy trap: remove scanning or aim support; low direct-damage control puzzle. | — |
| **Ring Threat Cathedral** (`orbital_security_hub_scale_final`)<br>`static:orbital_security_hub_scale_final:1` | Void Turret | **remix**: Compliance Officer + Void Turret + Null-G Drone | Optional three-role mastery formation; only one coordinated heavy attack per cycle. | — |

## World 6 — The Source

17 existing encounter sources → 15 proposed opportunities/stages. This is not a required-combat count.

| Source / room | Current formation | Proposed formation / disposition | Purpose and contract | Guaranteed items |
| --- | --- | --- | --- | --- |
| **Performance Review Loop** (`source_zeke_review_loop`)<br>`static:source_zeke_review_loop:1` | The Manager | **elite**: The Manager | Unique Zeke rescue elite; Nova-alone-safe counters and room-scoped rescue credit. | — |
| **Kill-Suite Loop** (`source_gh0st_kill_suite`)<br>`static:source_gh0st_kill_suite:1` | War Echo + War Echo | **elite**: War Echo + War Echo | Unique Gh0st rescue pair; designation/execution cycle rather than simultaneous burst. | — |
| **Tide-Well** (`source_orion_tide_well`)<br>`static:source_orion_tide_well:1` | Silent Shore | **elite**: Silent Shore | Unique Orion rescue elite; basic/Guard/Break counter before Orion rejoins. | — |
| **Manager Patrol Route** (`source_echo_patrol`)<br>`static:source_echo_patrol:1` | Source Shadow | **retain**: Source Shadow | Short first proper Source striker encounter. | — |
| **World-Fracture Landing** (`source_memory_fragments`)<br>`static:source_memory_fragments:1` | Source Shadow + Warped Sentinel + Glitch Hound | **retain**: Source Shadow + Warped Sentinel + Glitch Hound | Authored three-role fracture wave; preserve current arrival tempo and quest contract. | — |
| **Memory Defense Arena** (`source_spire_arena`)<br>`static:source_spire_arena:1` | Memory Leak + Nightmare Guard | **retain**: Memory Leak + Nightmare Guard | Fragile copy pressure behind a defender; preserve Center-path unlock. | — |
| **The Center** (`source_center`)<br>`static:source_center:1` | The Ascended: Soloist | **boss**: The Ascended: Soloist | Final encounter stage 1: controlled Soloist; stage 2 is a linked descriptor at the same arena. | — |
| **Badge Labyrinth** (`source_zeke_nightmare_node_scale_01`)<br>`static:source_zeke_nightmare_node_scale_01:1` | The Manager | **replace**: Review Clerk | Optional paperwork threat; cannot grant Zeke rescue credit. | — |
| **Order Well** (`source_gh0st_nightmare_node_scale_01`)<br>`static:source_gh0st_nightmare_node_scale_01:1` | War Echo | **replace**: War Order Fragment | Optional command fragment, distinct from the rescue pair. | — |
| **Black Tide Shelf** (`source_orion_nightmare_node_scale_01`)<br>`static:source_orion_nightmare_node_scale_01:1` | Silent Shore | **replace**: Tide Murmur | Optional small tide threat; cannot grant Orion rescue credit. | — |
| **Pay Stub Drift** (`source_echo_mines_node_scale_02`)<br>`static:source_echo_mines_node_scale_02:1` | Source Shadow | **remix**: Source Shadow + Review Clerk | Optional shadow exploiting a stamped target; one clear cooperation link. | — |
| **Canopy Root Step** (`source_memory_stair_node_scale_02`)<br>`static:source_memory_stair_node_scale_02:1` | Warped Sentinel | **retain**: Warped Sentinel | Short reticle lesson before the full fracture wave. | — |
| **Revision Gallery** (`source_spire_thought_node_scale_01`)<br>`static:source_spire_thought_node_scale_01:1` | Memory Leak | **retain**: Memory Leak | First fragile copy controller; keep brief, not a repeated HP elite. | — |
| **Memory Defense Line** (`source_spire_thought_node_scale_03`)<br>`static:source_spire_thought_node_scale_03:1` | Nightmare Guard | **elite**: Nightmare Guard | Optional shelter/commitment elite before its mixed defense encounter. | — |
| **No Throne** (`source_spire_thought_node_scale_04`)<br>`static:source_spire_thought_node_scale_04:1` | Memory Leak | **retire**: — | Let No Throne express the rejection of control without another Leak. | — |
| **Silence Pulpit** (`source_center_node_scale_03`)<br>`static:source_center_node_scale_03:1` | The Ascended: Silence | **relocate**: — | Relocate the transformed fight into the linked final arena; legacy partial saves have explicit recovery. Destination: `new:source_final_silence`. | — |
| **Source Scar Step** (`source_memory_stair_node_scale_final`)<br>`static:source_memory_stair_node_scale_final:1` | Source Shadow | **retire**: — | Source Scar Step becomes a quiet memory/reward destination. | — |
| **The Center: final linked stage** (`source_center`)<br>`new:source_final_silence` | Relocated from Silence Pulpit | **linked stage**: The Ascended: Silence + Voice Anchor + Memory Anchor | Linked final stage 2 and short closing cycle; no walking required between forms. Legacy partial-save recovery remains explicit. | Existing final-stage entitlement once |

## Encounters intentionally not inferred from old listeners

The legacy training listeners at Workshop Dock are included in the baseline progression contracts. A listener is not an authored spawn. The current source inventory counts the visible Security Post drill, not a second imaginary drill. Astra simulations, debug arenas and debug patrols are outside campaign counts. If another runtime injection is introduced later, give it its own source and acceptance row.

Counts describe source data, not a phone playthrough, mandatory path classification or balance acceptance. Route order, readiness gates and earned supply budgets must be verified before implementing any world.
