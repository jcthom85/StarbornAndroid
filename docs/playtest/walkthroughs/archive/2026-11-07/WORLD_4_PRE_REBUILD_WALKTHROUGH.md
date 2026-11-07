# World 4 — The Foundry: completionist route and playtest guide

Updated November 5, 2026 for the repaired Foundry navigation build. Older installations may still have diagonal Paths links and no walking route between the Slag Pits and Assembly. This replaces the earlier speculative guide. Coverage means all five main quests and all optional quests on the documented routes; it does not certify every room, build, or incidental interaction.

## Navigation and returning to earlier areas

All ordinary Foundry connections now use NSEW swipes, with immediate opposite-direction returns. The story transfer after Cooling Springs and the final ship departure remain explicit transitions.

- **Crusher branch:** Waste Intake → south → Crusher Throat → south → Records Shed → south → Magnet Ramp → south → Intake Catwalk → south → Foundry Sorter Spine. Return north. Intake Overlook and the Crucible still lie north of Waste Intake.
- **Timing Tower branch:** Conveyor Belt → north → Service Walk → north → Timing Tower → north → Reject Chute → north → Roller Underpass → north → Control Perch → north → Foundry Belt Nerve. Return south.
- **Turbine branch:** Power Core → south → Turbine Balcony → south → Engine Cradle → south → Array Lockers → south → Coolant Choir → south → Foundry Core Shadow. Return north. Engine Service Ring remains directly north of Power Core.
- **Cooling Springs to Waste Intake:** go north to Obsidian Shelf, east to Slag River, then east to Waste Intake. Reverse west/west/south to return; the diagonal shortcut is gone.
- **Forge to Power Core:** go north to Conveyor Belt, east to Conditioning Chamber, then east to Power Core. Reverse west/west/south to return; the diagonal shortcut is gone.

Before the Project Phantom/Springs sequence, the Assembly maintenance shutter is closed. Finish Gh0st’s conversation at Cooling Springs; it still moves the crew to Conveyor Belt and now clears that shutter permanently. From Conveyor Belt, go west to Reject Bay, then west to High-Pressure Pipe Gallery. Continue west through Decon Chamber and Service Airlock to Waste Intake. The same route reverses east back into Assembly. An older save with completed Springs/story progress recognizes this access when loaded.

This permits missed quests, the Crucible, Cameron’s gallery, and earlier rest stops to remain accessible after entering Assembly. The cryogenic valve still controls entry to the optional Subterranean Smelter Forge; the navigation repair does not bypass that gate. Ten tapped return shortcuts remain optional conveniences, with ordinary swipe routes available for walking back.

## Main route

1. Land at the Obsidian Shelf, cross the Slag River, hack the cooling vents at Waste Intake, then enter the Service Airlock. Return to Waste Intake for Project Phantom.

2. Inspect the Project Phantom terminal and finish Gh0st and Nova’s conversation. You can complete the early optional quests and Crucible before regrouping at Cooling Springs, or return for them afterward through the cleared maintenance corridor. The springs conversation grants Gh0st’s override key and Phase Counter, completes MQ17, and moves the party into the Assembly Line.

3. Navigate the conveyors, confront the prototype production at the Conditioning Chamber, then overload the matrix at the Forge. Quality Control and Overclock can be completed during the Assembly detour.

4. Read the pulse board in Timing Tower, inspect the grease marks in the Roller Underpass, throw the override paddle in Control Perch, then starve the breath pistons in Bellows Walk. Open the Anvil cradle at the Forge and speak with Nova to claim the relic. Follow the actual room labels in the action table below.

5. Rest at the Forge Control Alcove before Titan Dock. Inspect Commander Rylos and finish the confrontation before fighting the Titan Walker. The launch also requires both the Deep Core Engine and Phase-Cutter Arrays from the Power Core. Win the fight, collect both components, and use the escape launch to enter World 5.

## Puzzles, choices, and recovery

The Crucible is an optional three-round combat ladder, accepted from Pitmaster Brutus at The Scrapper’s Crucible. Start each round through his dialogue; a round-start receipt does not count as victory. The teams are two Magma Drones with a Welder Bot, a Slag Golem with a Welder Bot, then the Crucible Champion with a Magma Drone escort. Rest at Heat Shadow Alcove (the Obsidian Shelf branch) between rounds as needed. Return to Brutus after round three to claim 500 XP, 350 credits, and one Crucible Forge Plate. Retreat and defeat preserve an uncleared round for retry; reloading and speaking to Brutus again must remain possible. Do not expect a separate Scavenger skill from The Scrap Heap: that quest repairs a bypass and grants its listed XP.

Quality Control has two exclusive endings: release the units, or defend the workers. Both complete the quest; release grants Gh0st’s Harden, while defending grants a Rapid Capacitor. Choose deliberately and check that trying the other ending cannot grant another payout.

Local return shortcuts remain usable after their first trip, including saves with old completion receipts. Save before committing to a branch or leaving the world.

## Optional quest routes and rewards

Rewards below are the completion-event payouts. Battle loot, earlier step rewards, and crew AP awards are separate. Room and prop names come directly from the current assets.

### Sabotage (`w4_sq16`)

- **Slag River — ammo crate alpha:** Destroy Slag River ammo crate.
- **Waste Intake — ammo crate beta:** Destroy Waste Intake ammo crate.
- **Service Airlock — ammo crate gamma:** Destroy Service Airlock ammo crate and complete Sabotage.

Completion `w4_sq16_destroy_crate_gamma`: 1 × Explosive Tip, 350 XP.

### Lost Worker (`w4_sq17`)

- **Waste Intake — worker maintenance ping:** Trace the missing worker's final maintenance ping.
- **Cooling Springs — worker tablet:** Find missing worker tablet.
- **Service Airlock — engineer safe-route valve:** Open the missing engineer's heat-safe route.

Completion `w4_sq17_vent_safe_route`: 1 × Coolant System, 300 XP.

### The Scrap Heap (`w4_sq18`)

- **Waste Intake — waste intake brake:** Jam the waste intake before it consumes the wrecks.
- **Waste Intake — scrap mech pile:** Salvage destroyed mechs.
- **Cooling Springs — salvaged cooling bypass:** Build a cooling bypass from salvaged actuators.

Completion `w4_sq18_build_bypass`: 300 XP.

### Quality Control (`w4_sq19`)

- **Reject Bay — rejected droid:** Read the rejected units' fault codes.
- **Conveyor Belt — rejected droid:** Resolve defective units.
- **Conveyor Belt — conveyor release lock:** Release the reprogrammed units.
- **Conveyor Belt — worker defense relay:** Let the waking units remain as voluntary worker defenders.

Completion `w4_sq19_release_units`: Harden, 400 XP.

Completion `w4_sq19_defend_workers`: 1 × Rapid Capacitor, 400 XP.

### Overclocked (`w4_sq20`)

- **Control Alcove — hazard cycle map:** Map the overclocked chamber's heat sequence.
- **The Forge — test chamber terminal:** Calibrate the test terminal against the mapped heat cycle.
- **Control Alcove — overclock profile:** Steal the test chamber's overclock profile.

Completion `w4_sq20_steal_overclock`: 1 × Thermal Clip, 450 XP.

### The Scrapper's Crucible (`w4_sq_crucible`)

Talk to Brutus for rules, each of three fights, and the final reward. See the Crucible section above.

## Main-route action reference

| Quest | Room | Visible control |
| --- | --- | --- |
| `w4_mq16` | Obsidian Shelf | astra |
| `w4_mq16` | Stepping Stones | flare rhythm |
| `w4_mq16` | Waste Intake | cooling vent controls |
| `w4_mq16` | Service Airlock | airlock door |
| `w4_mq18` | Service Walk | warning lamps |
| `w4_mq18` | Conditioning Chamber | conditioning records |
| `w4_mq18` | The Forge | fabrication matrix |
| `w4_mq19` | Timing Tower | pulse board |
| `w4_mq19` | Roller Underpass | grease marks |
| `w4_mq19` | Control Perch | override paddle |
| `w4_mq19` | Bellows Walk | breath pistons |
| `w4_mq19` | Control Alcove | hammer cadence |
| `w4_mq19` | The Forge | anvil relic |
| `w4_mq20` | Engine Service Ring | thermal clamps |
| `w4_mq20` | Engine Service Ring | array rack |
| `w4_mq20` | Escape Catwalk | escape gantry |
| `w4_mq17` | Waste Intake | project phantom terminal |
| `w4_mq17` | Cooling Springs | steam pool |
| `w4_mq20` | Titan Dock | commander rylos |

## Acceptance checks

- Finish every optional quest and reload after its reward; confirm XP, AP, items, and branch outcomes persist.
- Attempt the opposite exclusive ending after completing a branch; confirm no second payout.
- Return through a local shortcut more than once, including after reload.
- Check scene readability, notifications, touch targets, and combat preparation on a phone.
- Record whether clues were understandable without this guide, and whether detours were worth taking. Automated known-answer routes cannot measure those experiences.

See [the Worlds 4–6 refinement report](../../testing/WORLDS456_REFINEMENT_2026-11-04.md) for evidence and test limits.

## Complete room and exit reference

These are physical NSEW connections. The maintenance shutter and cryogenic crossing still apply their story/control requirements.

### Obsidian Shelf

#### Obsidian Shelf (`foundry_slag_landing`)

The Astra cools on a shelf of black volcanic glass above a river of magma. Heat shields glow cherry-red while ash falls like dirty snow, and the Lens still shows the Shield window closing behind you. A compliance bulletin hisses from the factory PA.

Exits: north → Obsidian Overlook, east → Slag River, south → Cooling Springs, west → Ash Survey Point.

Interactions: astra, compliance bulletin.

#### Obsidian Overlook (`foundry_obsidian_overlook`)

A cracked overlook juts above the Foundry basin. The whole sector glows below: slag rivers, intake pistons, and the factory spine Vale built to feed something larger than war. Shield window telemetry flickers on the crew's flight display.

Exits: south → Obsidian Shelf.

Interactions: foundry basin, shield window telemetry.

#### Ash Survey Point (`foundry_obsidian_shelf_scale_01`)

An ash survey point overlooks the Astra's landing scorch and a field of black glass. The landing scorch shows how hard the Astra hit, while the survey marker rates the shelf as disposable. A basalt core sample rests on the surveyor's tripod.

Exits: east → Obsidian Shelf, north → Heat Shadow Alcove.

Interactions: ash survey, landing scorch, basalt core sample.

#### Heat Shadow Alcove (`foundry_obsidian_shelf_scale_02`)

A heat shadow alcove hides behind stacked coolant tags fluttering in furnace wind. The coolant tags mark batches diverted from worker safety.

Exits: south → Ash Survey Point, north → Slag Signal Post.

Interactions: heat shadow, coolant tags, geothermal induction plate, heat shadow refuge.

#### Slag Signal Post (`foundry_obsidian_shelf_scale_03`)

A slag signal post leans over the shelf with a range mast welded from spare rail. The range mast can ping the next safe rock when the river flares.

Exits: south → Heat Shadow Alcove, north → Survey Cache.

Interactions: slag signal, range mast.

#### Survey Cache (`foundry_obsidian_shelf_scale_04`)

A survey cache contains a cracked scope and old hazard notes from crews who stopped reporting. The cracked scope still reads heat pockets through the smoke.

Exits: south → Slag Signal Post, north → Foundry Emergency Marker.

Interactions: survey cache, cracked scope.

#### Foundry Emergency Marker (`foundry_obsidian_shelf_scale_final`)

An emergency marker blinks above the black glass, its cracked beacon still guiding crews through heat that management calls acceptable. The cracked beacon and shift tally make the landing shelf feel like part of a working disaster. A heat-sealed container rests half-buried in volcanic slag. An optional return slide leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → Survey Cache.

Interactions: cracked beacon, shift tally, heat-sealed container, slide.


### Slag River

#### Slag River (`foundry_slag_river`)

Broken stepping stones form a slag crossing over a flowing lava channel. Source flares erupt from pressure cracks without warning, bending the heat into bright, violent chords. An ammo crate alpha hangs from a chain beside a basalt pylon.

Exits: west → Obsidian Shelf, north → Stepping Stones, east → Waste Intake, south → Basalt Switchback.

Interactions: slag crossing, ammo crate alpha.

#### Stepping Stones (`foundry_slag_stepping_stones`)

Basalt stones rise from the lava channel in a readable flare rhythm. A roaring slag cataract blocks the northern crossing beside a cryogenic manifold valve.

Exits: south → Slag River, north → Subterranean Smelter Forge.

Interactions: cryogenic manifold valve, flare rhythm.

#### Subterranean Smelter Forge (`foundry_subterranean_smelter`)

Colossal obsidian crucibles loom in the crimson gloom. Ancient geothermal vents feed a pre-Dominion smelting anvil, where an unyielding precursor crucible rests sealed.

Exits: south → Stepping Stones.

Interactions: precursor crucible, smelting anvil.

#### Basalt Switchback (`foundry_slag_river_scale_01`)

A basalt switchback follows the slag-river wall beside a heat ladder glowing red at the edges. Its joints expand and contract with each furnace cycle. A heavy heat tether secures the hull against the glowing flow.

Exits: north → Slag River, east → Coolant Spillway.

Interactions: basalt switchback, heat ladder, heat tether.

#### Coolant Spillway (`foundry_slag_river_scale_02`)

A coolant spillway pours chemical blue into orange slag, raising a steam curtain thick enough to hide movement. The steam curtain turns patrol silhouettes into guesswork.

Exits: west → Basalt Switchback, north → Slag Ferry Winch.

Interactions: coolant spillway, steam curtain.

#### Slag Ferry Winch (`foundry_slag_river_scale_03`)

A slag ferry hangs from a winch with half its teeth stripped smooth by heat. The winch teeth catch every third crank.

Exits: south → Coolant Spillway, north → River Gauge Station.

Interactions: slag ferry, winch teeth.

#### River Gauge Station (`foundry_slag_river_scale_04`)

A river gauge station measures flow surges with a red needle bent past every printed warning. The red needle jumps before the river throws fire.

Exits: south → Slag Ferry Winch, north → Foundry River Control.

Interactions: river gauge, red needle.

#### Foundry River Control (`foundry_slag_river_scale_final`)

A river control booth watches slag levels with a burned console and a rescue hook nobody officially issued. The burned console and rescue hook explain how workers survive routes the map calls closed. A heavy smelter toolbox sits bolted beside the sluice valve. An optional return catwalk leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → River Gauge Station.

Interactions: burned console, rescue hook, smelter toolbox, catwalk.


### Cooling Springs

#### Cooling Springs (`foundry_cooling_springs`)

Factory noise fades in this hidden maintenance pocket. A steam pool and [action:cooling basin fishing|cooling basin] collect mineral runoff; a magma regulator pulses beside a salvaged cooling bypass. A worker tablet lies wrapped in a coolant-stained scarf beside the pool.

Exits: north → Obsidian Shelf, south → Lava Tube Bypass, west → Mist Basin.

Interactions: steam pool, worker tablet, salvaged cooling bypass, cooling basin fishing, magma regulator.

#### Lava Tube Bypass (`foundry_lava_tube_bypass`)

A service crawlspace cuts under the cooling springs. Mineral fans beat hot air into steam, and old worker tags hang from pipe valves like small prayers. Rylan tends a glowing welder bench beside the conduits.

Exits: north → Cooling Springs.

Interactions: pipe valves, welder bench.

#### Mist Basin (`foundry_cooling_springs_scale_01`)

A mist basin steams under mineral crust thick enough to write in. The mineral crust records every surge in brittle rings. A condensation trap drips steadily into a steel bucket.

Exits: east → Cooling Springs, north → Worker Marker.

Interactions: mist basin, mineral crust, condensation trap.

#### Worker Marker (`foundry_cooling_springs_scale_02`)

A worker marker stands beside a dead helmet light and a name scratched into pipe insulation. The helmet light died facing the exit.

Exits: south → Mist Basin, north → Spring Valve Nest.

Interactions: worker marker, helmet light.

#### Spring Valve Nest (`foundry_cooling_springs_scale_03`)

A spring valve nest knots pipes around a pressure wheel painted with old handprints. The pressure wheel can lower the heat if it still turns.

Exits: south → Worker Marker, north → Thermal Shelf.

Interactions: spring valve, pressure wheel.

#### Thermal Shelf (`foundry_cooling_springs_scale_04`)

A thermal shelf catches runoff in a cool seam beneath the slag wall. The cool seam is a natural flaw the Foundry never managed to monetize.

Exits: south → Spring Valve Nest, north → Foundry Blue Steam Pocket.

Interactions: thermal shelf, cool seam.

#### Foundry Blue Steam Pocket (`foundry_cooling_springs_scale_final`)

A blue steam pocket gathers under patched pipes, turning heat into a temporary hiding place. The patched pipes and cool residue show the Foundry being fought by its own workers. An insulated coolant reserve hums with frosty condensation. An optional return drain leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → Thermal Shelf.

Interactions: patched pipes, cool residue, coolant reserve, drain.


### Waste Intake

#### Waste Intake (`foundry_waste_intake`)

Pistons hammer scrap toward the factory interior. Cooling vent controls flank a waste intake brake beneath a worker maintenance ping. Thermal glass shields a Project Phantom terminal; ammo crate beta, a scrap mech pile and a wrecked loader stash crowd the lava's edge. The crusher passage descends south; Intake Overlook and the Crucible approach remain north.

Exits: west → Slag River, north → Intake Overlook, east → Service Airlock, south → Crusher Throat.

Interactions: cooling vent controls, worker maintenance ping, waste intake brake, project phantom terminal, ammo crate beta, scrap mech pile, wrecked loader stash.

#### Intake Overlook (`foundry_intake_overlook`)

A grated balcony watches the intake pistons chew ruined machines into feedstock. Every impact shakes loose sparks, serial plates, and pieces of stories Dominion filed as material. The safety railing is pitted by hot slag.

Exits: south → Waste Intake, north → The Scrapper's Crucible.

Interactions: intake pistons, safety railing.

#### Crusher Throat (`foundry_waste_intake_scale_01`)

The crusher throat opens below the intake platform. A jammed tooth holds back a mountain of rejected parts. A chipped crusher tooth groans against jammed titanium. The platform at Waste Intake is north; Records Shed continues south.

Exits: north → Waste Intake, south → Records Shed.

Interactions: crusher throat, jammed tooth, crusher tooth.

#### Records Shed (`foundry_waste_intake_scale_02`)

A records shed lists Phantom labels on crates that officially never existed. The Phantom labels use budget codes instead of names.

Exits: north → Crusher Throat, south → Magnet Ramp.

Interactions: records shed, Phantom labels.

#### Magnet Ramp (`foundry_waste_intake_scale_03`)

A magnet ramp drags loose filings uphill in glittering streams toward the sorting drums. The loose filings point toward live current under the floor.

Exits: north → Records Shed, south → Intake Catwalk.

Interactions: magnet ramp, loose filings.

#### Intake Catwalk (`foundry_waste_intake_scale_04`)

An intake catwalk sways above the waste pit on braces patched with hurry and prayer. The sway braces were signed by workers who expected them to fail.

Exits: north → Magnet Ramp, south → Foundry Sorter Spine.

Interactions: intake catwalk, sway braces.

#### Foundry Sorter Spine (`foundry_waste_intake_scale_final`)

A sorter spine rattles behind the intake with rejected parts climbing toward another chance at violence. The sorter spine and reject ledger make the waste system feel alive and complicit. A reinforced salvage bin caught against the intake teeth holds uncrushed gear. An optional return ladder leads directly back to the entrance. You can also walk back through the room corridors.

Exits: north → Intake Catwalk.

Interactions: sorter spine, reject ledger, salvage bin, ladder.

#### The Scrapper's Crucible (`foundry_crucible_arena`)

A sunken iron amphitheater enclosed by heavy [action:blast cage|blast cages] and [action:molten sluices|molten slag sluices]. Pitmaster Brutus monitors telemetry above scorched titanium, orchestrating brutal machine combat for rogue loaders and eager betters. A victor podium rises below an announcer horn.

Exits: south → Intake Overlook.

Interactions: blast cage, molten sluices, victor podium, announcer horn.


### Service Airlock

#### Service Airlock (`foundry_service_airlock`)

A pressurized airlock door separates volcanic ash from sterile factory corridors. Decon lights sweep the pumps in white bands. An ammo crate gamma sits beneath a soot-marked wall. An engineer safe-route valve is mounted on the coolant line.

Exits: east → Decon Chamber, west → Waste Intake, north → Seal Control Nook.

Interactions: airlock door, ammo crate gamma, engineer safe-route valve.

#### Decon Chamber (`foundry_decon_chamber`)

Chemical mist hangs in a narrow decon chamber. The decon cycle waits behind a sealed panel; the walls are too clean, the drains too large, and the emergency shower is bolted shut from the wrong side.

Exits: west → Service Airlock, east → High-Pressure Pipe Gallery.

Interactions: decon cycle.

#### Seal Control Nook (`foundry_service_airlock_scale_01`)

A seal control nook watches the airlock doors with a cycle lever polished by emergency use. The cycle lever sticks halfway between safe and fast. A cycle logbook hangs from a rusty chain.

Exits: south → Service Airlock, north → Decon Drain.

Interactions: seal control, cycle lever, cycle logbook.

#### Decon Drain (`foundry_service_airlock_scale_02`)

A decon drain foams with chemical runoff beneath cracked grating. The chemical foam carries metal flakes like stars.

Exits: south → Seal Control Nook, north → Pressure Lock Gallery.

Interactions: decon drain, chemical foam.

#### Pressure Lock Gallery (`foundry_service_airlock_scale_03`)

A pressure lock gallery blinks yellow gauges at anyone foolish enough to trust the seal. The yellow gauges disagree in small terrifying ways.

Exits: south → Decon Drain, north → Maintenance Shelf.

Interactions: pressure lock, yellow gauges.

#### Maintenance Shelf (`foundry_service_airlock_scale_04`)

A maintenance shelf holds spare gasket rings, burnt gloves, and a note reading DO NOT CYCLE TWICE. The spare gasket is warped but usable.

Exits: south → Pressure Lock Gallery, north → Foundry Seal Backroom.

Interactions: maintenance shelf, spare gasket.

#### Foundry Seal Backroom (`foundry_service_airlock_scale_final`)

A seal backroom stores pressure masks beside handwritten cycling rules. The pressure masks and cycling rules make the airlock feel maintained by people who expect the official procedure to fail. A stamped technician locker remains locked against the heat. An optional return hatch leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → Maintenance Shelf.

Interactions: pressure masks, cycling rules, technician locker, hatch.

#### High-Pressure Pipe Gallery (`foundry_pipe_gallery`)

Superheated steam hisses through massive overhead conduits in a pressurized gallery. Checkered racing pennants hang above a pipe bench stacked with flanging tools and leather gloves. A scuffed baseball rests in a tool rack. A family photo and leather dog collar hang beside the bypass manifold. An Assembly maintenance shutter stands at the east end, leading to Reject Bay. Once the crew clears it after Cooling Springs, the corridor stays open in both directions.

Exits: west → Decon Chamber, east → Reject Bay.

Interactions: racing pennants, pipe bench, family photo, bypass manifold, leather dog collar.


### Conveyor Belt

#### Conveyor Belt (`foundry_conveyor_belt`)

Conveyor belts carry half-assembled droids under hammering presses. A belt timing console marks the rhythm. A rejected droid waits between two choices: the conveyor release lock opens the worker tunnels, while the worker defense relay lets the waking units stay and protect the line. The service walk north leads onward to Timing Tower.

Exits: north → Service Walk, west → Reject Bay, east → Conditioning Chamber, south → The Forge.

Interactions: belt timing console, rejected droid, conveyor release lock, worker defense relay.

#### Service Walk (`foundry_belt_service_walk`)

A narrow service walk runs above the conveyor feed. Warning lamps blink in strict sequence, spelling out when the crushers fall and when a person can move. The tower steps continue north to Timing Tower; the conveyor floor is back to the south.

Exits: south → Conveyor Belt, north → Timing Tower.

Interactions: warning lamps.

#### Reject Bay (`foundry_reject_bay`)

Defective units are dumped into a low bay before the furnace eats them. A rejected droid still twitches when the conveyor shudders, hands opening and closing around nothing. A scorched smelter console rests against the bay wall. The western maintenance shutter leads back to the Pipe Gallery and the Slag Pits. The conveyor floor is east.

Exits: east → Conveyor Belt, west → High-Pressure Pipe Gallery.

Interactions: rejected droid, scorched smelter console.

#### Timing Tower (`foundry_conveyor_belt_scale_01`)

A timing tower ticks above the conveyor, its pulse board turning worker mistakes into production data. The pulse board predicts crush points before they happen. A belt speed dial is locked in place with a security seal. Steps south return to Service Walk; Reject Chute continues north.

Exits: south → Service Walk, north → Reject Chute.

Interactions: timing tower, pulse board, belt speed dial.

#### Reject Chute (`foundry_conveyor_belt_scale_02`)

A reject chute spits scrap rain into a pit where defective parts keep twitching. The scrap rain hides small parts worth stealing.

Exits: south → Timing Tower, north → Roller Underpass.

Interactions: reject chute, scrap rain.

#### Roller Underpass (`foundry_conveyor_belt_scale_03`)

A roller underpass runs beneath the belt through grease marks and warning paint. The grease marks show where previous crews crawled.

Exits: south → Reject Chute, north → Control Perch.

Interactions: roller underpass, grease marks.

#### Control Perch (`foundry_conveyor_belt_scale_04`)

A control perch hangs over the belt with an override paddle locked behind mesh. The override paddle can pause one section if the mesh comes loose.

Exits: south → Roller Underpass, north → Foundry Belt Nerve.

Interactions: control perch, override paddle.

#### Foundry Belt Nerve (`foundry_conveyor_belt_scale_final`)

A belt nerve junction pulses beneath the conveyor, matching every crush cycle to a production target. The nerve junction and target counter turn traversal into readable machinery. An assembly cache has been knocked onto an inspection shelf. An optional return slide leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → Control Perch.

Interactions: nerve junction, target counter, assembly cache, slide.


### Conditioning

#### Conditioning Chamber (`foundry_conditioning_chamber`)

Sterile white tiles are stained with oil. Rows of empty chairs with restraints face a wall of Spectral ID calibration screens displaying conditioning records.

Exits: north → Observation Booth, west → Conveyor Belt, east → Power Core, south → Heat-Soak Chamber.

Interactions: conditioning records.

#### Observation Booth (`foundry_conditioning_observation`)

Tinted glass looks down on the restraint chairs. The operator console grades fear response, compliance, and identity degradation in neat green columns.

Exits: south → Conditioning Chamber.

Interactions: operator console.

#### Heat-Soak Chamber (`foundry_conditioning_scale_01`)

A heat-soak chamber wraps Phantom frames in orange coils until their armor remembers pain. The orange coils pulse in sequence with the fabrication line. An empty thermal clamp still radiates blistering heat.

Exits: north → Conditioning Chamber, east → Quench Lane.

Interactions: heat-soak chamber, orange coils, thermal clamp.

#### Quench Lane (`foundry_conditioning_scale_02`)

A quench lane slams prototypes through cold vapor while steam alarms shriek above the track. The steam alarms are calibrated to production loss, not injury.

Exits: west → Heat-Soak Chamber, north → Defect Window.

Interactions: quench lane, steam alarms.

#### Defect Window (`foundry_conditioning_scale_03`)

A defect window looks into a bay where a shaking unit refuses to power down. The shaking unit twitches every time the line calls it defective.

Exits: south → Quench Lane, north → Calibration Pit.

Interactions: defect window, shaking unit.

#### Calibration Pit (`foundry_conditioning_scale_04`)

A calibration pit scores each prototype against load marks carved into ceramic floor. The load marks show failures that were recycled into the next batch.

Exits: south → Defect Window, north → Foundry Defect Choir.

Interactions: calibration pit, load marks.

#### Foundry Defect Choir (`foundry_conditioning_scale_final`)

A defect choir of rejected units clicks behind glass, half-awake and waiting for a command that may never come. The defect choir and glass seam deepen the question of what the Foundry throws away. A hardened stress-test locker survived the pressure cycles. An optional return valve leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → Calibration Pit.

Interactions: defect choir, glass seam, stress-test locker, valve.


### The Forge

#### The Forge (`foundry_forge_anvil`)

A massive hammer shapes ore around the anvil cradle, teaching metal to obey. The anvil relic glows inside its mount. A fabrication matrix panel overlooks the hammer lane, with a test chamber terminal beside it.

Exits: north → Conveyor Belt, west → Control Alcove, south → Bellows Walk.

Interactions: fabrication matrix, anvil cradle, anvil relic, test chamber terminal.

#### Control Alcove (`foundry_forge_control_alcove`)

A shielded alcove controls the Forge's moving belt plates. The panel is burned around the edges where previous operators tried to override the hammer cadence by hand. A hazard cycle map fills one display; another holds the overclock profile readout.

Exits: east → The Forge.

Interactions: hammer cadence, hazard cycle map, overclock profile, smelter break station, control alcove cot.

#### Bellows Walk (`foundry_forge_scale_01`)

The bellows walk rises beside breath pistons that feed the Forge like lungs. The breath pistons can starve the fire if timed correctly. An iron bellows valve trembles under forced draft.

Exits: north → The Forge, east → Anvil Shadow.

Interactions: bellows walk, breath pistons, bellows valve.

#### Anvil Shadow (`foundry_forge_scale_02`)

The anvil shadow remains on the floor even when the light changes. A relic imprint glows beneath the ash.

Exits: west → Bellows Walk, north → Mold Library.

Interactions: anvil shadow, relic imprint.

#### Mold Library (`foundry_forge_scale_03`)

A mold library stores broken casts for weapons Dominion decided were too expensive for ordinary troops. The broken casts are labeled by casualty forecast.

Exits: south → Anvil Shadow, north → Slag Chapel.

Interactions: mold library, broken casts.

#### Slag Chapel (`foundry_forge_scale_04`)

A slag chapel of welded braces and prayer bolts watches over the Forge core. The prayer bolts carry names etched too small for supervisors to notice.

Exits: south → Mold Library, north → Foundry Relic Draft.

Interactions: slag chapel, prayer bolts.

#### Foundry Relic Draft (`foundry_forge_scale_final`)

A relic draft moves through the Forge before the fire does, tugging ash toward the Anvil's old imprint. The relic draft and ash trail make the Forge feel older than Dominion's machinery. A heat-blackened master smith crate sits beside the flue.

Exits: south → Slag Chapel.

Interactions: relic draft, ash trail, master smith crate, flue.


### Power Core

#### Power Core (`foundry_power_core`)

The Deep-Core Engine pulses in the center of the factory, feeding heat and psionic current into a cradle mount built for something far larger than a ship. The engine mount connects to the core's thermal clamps. Phase-Cutter Arrays occupy the adjoining rack. The southern stair leads to Turbine Balcony; Engine Service Ring remains north.

Exits: north → Engine Service Ring, west → Conditioning Chamber, east → Titan Dock, south → Turbine Balcony.

Interactions: engine mount, phase-cutter arrays.

#### Engine Service Ring (`foundry_engine_service_ring`)

A maintenance ring circles the Deep-Core Engine at arm's length from impossible heat. Every clamp is labeled with launch-grade tolerances the Astra does not currently meet. Thermal clamps surround the core beside an array rack.

Exits: south → Power Core.

Interactions: thermal clamps, array rack.

#### Turbine Balcony (`foundry_power_core_scale_01`)

A turbine balcony hangs over spin blur so fast the air becomes a wall. The spin blur hides which blades are damaged. A turbine tachometer vibrates violently against its bracket. The stair north returns to Power Core; the maintenance route continues south toward Engine Cradle.

Exits: north → Power Core, south → Engine Cradle.

Interactions: turbine balcony, spin blur, turbine tachometer.

#### Engine Cradle (`foundry_power_core_scale_02`)

The engine cradle grips the Deep-Core Engine with mount clamps bigger than mining rigs. The mount clamps loosen when the core output spikes.

Exits: north → Turbine Balcony, south → Array Lockers.

Interactions: engine cradle, mount clamps.

#### Array Lockers (`foundry_power_core_scale_03`)

Array lockers line the core wall under phase seals and red theft warnings. The phase seals flicker whenever the Anvil wakes.

Exits: north → Engine Cradle, south → Coolant Choir.

Interactions: array lockers, phase seals.

#### Coolant Choir (`foundry_power_core_scale_04`)

Blue pipes carry coolant under core pressure, knocking against their clamps in a coolant choir. Flow drops sharply at a damaged valve. A pressure relief lever is clamped in the open position.

Exits: north → Array Lockers, south → Foundry Core Shadow.

Interactions: coolant choir, blue pipes, pressure relief lever.

#### Foundry Core Shadow (`foundry_power_core_scale_final`)

A core shadow falls across the engine clamps in a shape too sharp for the room's lighting. The core shadow and clamp gauge foreshadow the theft that will make the Astra climb. A lead-lined shielded core case is clipped to the reactor frame. An optional return lift leads directly back to the entrance. You can also walk back through the room corridors.

Exits: north → Coolant Choir.

Interactions: core shadow, clamp gauge, shielded core case, lift.


### Titan Dock

#### Titan Dock (`foundry_titan_dock`)

A circular platform hangs over the volcano primary vent. Catwalks tremble above orange depth while a Titan Walker waits in its launch cradle. Commander Rylos watches from the Titan Walker. The escape launch route runs past a gantry footlocker bolted to the rail.

Exits: north → Escape Catwalk, west → Power Core, east → Walker Scaffold.

Interactions: commander rylos, escape launch, gantry footlocker.

#### Escape Catwalk (`foundry_escape_catwalk`)

A shaking catwalk runs from the Titan Dock toward the escape gantry. Heat rolls up through the grate in hard orange sheets.

Exits: south → Titan Dock.

Interactions: escape gantry.

#### Walker Scaffold (`foundry_titan_dock_scale_01`)

A walker scaffold wraps the Titan dock in hydraulic scars and fresh weld smoke. The hydraulic scars show where earlier test pilots lost arguments. A weld inspection stencil marks dangerous stress fractures.

Exits: west → Titan Dock, north → Ammo Lift.

Interactions: walker scaffold, hydraulic scars, weld inspection stencil.

#### Ammo Lift (`foundry_titan_dock_scale_02`)

An ammo lift hauls shells past the dock with a chain brake glowing cherry red. The chain brake will fail before the schedule does.

Exits: south → Walker Scaffold, north → Escape Rail.

Interactions: ammo lift, chain brake.

#### Escape Rail (`foundry_titan_dock_scale_03`)

An escape rail points toward the Astra route, but a cut coupler hangs loose under the track. The cut coupler can be forced if the core surge covers the noise.

Exits: south → Ammo Lift, north → Meltdown View.

Interactions: escape rail, cut coupler.

#### Meltdown View (`foundry_titan_dock_scale_04`)

A meltdown view opens across the core where white fire climbs the walls like weather. The white fire bends toward the stolen engine cradle.

Exits: south → Escape Rail, north → Foundry Last Gantry.

Interactions: meltdown view, white fire.

#### Foundry Last Gantry (`foundry_titan_dock_scale_final`)

A last gantry shakes over the Titan dock, pointed directly at the escape route. The last gantry and release bolt turn the boss arena into a place built for a desperate exit. A magnetic titan maintenance pod is tethered beneath the crane. An optional return scaffolding leads directly back to the entrance. You can also walk back through the room corridors.

Exits: south → Meltdown View.

Interactions: last gantry, release bolt, titan maintenance pod, scaffolding.
