# World 3 completionist playtest guide

Updated November 4, 2026 from the runtime catalogs. The main route is covered by earned campaign tests; optional branches and device presentation still require human playtesting. This replaces the earlier guide whose reward tables, orbital-flight claims and permanent lockouts were not reliable.

## Main route

1. Clear the sewer landing, follow the sewer/vent route to Zeke’s apartment, then talk to the visible Zeke to establish the safehouse.
2. Get Jax’s codes at The Static. Map Underrail patrols, obtain security records at the Security Kiosk, copy badges at the Service Gate, gather disguises on the safehouse roof, then hack the apartment workstation and assemble the planning table.
3. Blend in at Skypark Dome, disable the Laundry sensors, then enter the lobby.
4. At Archive Vault read the containment field and trace command tethers; read the Prism Gallery shutters and resolve its light puzzle. Return to the Lens relic to finish the acquisition and warning sequence.
5. Prepare healing and rest before the roof encounters. Defeat Administrator, scan the Shield gap and use the launch action. The Foundry is the next destination; this does not yet cross the outer Planetary Shield into high orbit.

Keep a predeparture save. Astra return travel exists for unlocked destinations; do not treat every incomplete quest as permanently lost. Early legitimate boss victories are recovered when their objective prerequisites become eligible, without repeating combat drops.

## Optional-quest checks

- Neon Fix: Sentinel Mk. I in Private Booth drops the Neon Sign Core reliably. An older cleared encounter with a missing core can recover it while the quest is active. Install the core once. There is no implemented permanent price discount; that old promise has been removed.
- Prototype Testing is a calibration task using recorded drone targets, not a spawned combat trial. Keep any independent room enemies separate from the calibration objective. Phase Rounds is the existing reward; inspect its actual equipment slot.
- Use the current journal tasks below for the other investigations, favors and branch decisions. Test exclusive outcomes on separate saves.

## Current journal objectives

### Homecoming (`w3_mq11`)

Avoid Dominion surveillance by landing in the sewers, clear the landing zone, climb into the Lower City, and let Zeke turn his old apartment into a safehouse before the city notices he came home.

**Securing the Sewers**

- [ ] Clear local hostile forces at the landing pad. (`clear_landing`)
- [ ] Follow the run-off tunnels up to the Vent Output. (`reach_vent`)

**Zeke's Safehouse**

- [ ] Locate Zeke's apartment inside The Static block. (`find_apartment`)
- [ ] Let Orion shield Zeke's apartment from Dominion scans. (`establish_shield`)

### The Plan (`w3_mq12`)

Use Jax's curfew schedules, Gh0st's security pull, Zeke's elevator blueprints, patrol timing, service gate badges, and safehouse disguise materials to build a plan that depends on the crew trusting each other under pressure.

**Intelligence Gathering**

- [ ] Get curfew schedules from Jax at The Static. (`talk_jax`)
- [ ] Study patrol timing from the underrail map. (`map_patrols`)
- [ ] Pull security records from the Security Kiosk in Transit Plaza. (`interrogate_guard`)
- [ ] At the Elevator Service Gate, copy the freight badge patterns. (`copy_badges`)
- [ ] Source disguise materials from the safehouse roof laundry lines. (`source_disguises`)
- [ ] At Zeke's Apartment, build the service-elevator route at his workstation. (`hack_blueprints`)

**The Assembly**

- [ ] Use the planning table to assign roles, commit the crew, and launch the heist. (`assemble_table`)

### Neon Fix (`w3_sq11`)

Inspect the flickering sign at The Static and install a replacement neon sign core.

**Sign Repair**

- [ ] Inspect the flickering sign outside The Static. (`inspect_sign`)
- [ ] Install the replacement neon sign core. (`install_core`)

### Cold Case (`w3_sq12`)

Examine terminal records in the Transit Plaza to find clues about Elara.

**Database Breach**

- [ ] Recover the erased case number from the memorial in Uniform Sorting. (`find_case_number`)
- [ ] Use the case number at the Security Kiosk to search Dominion records. (`access_terminal`)
- [ ] Expose where security transferred the missing witness. (`expose_transfer`)

### Night Market Run (`w3_sq13`)

Search the alleyways and fix the flickering lanterns to light up the market.

**Lighting the Market**

- [ ] Trace who is siphoning power from the Night Market. (`trace_power_theft`)
- [ ] Reroute executive sign power into the market grid. (`reroute_power`)
- [ ] Relight the market and reveal the hidden stalls. (`fix_market_lights`)

### Social Engineering (`w3_mq13`)

Move through Laundry Service, borrow the shape of workers the city refuses to see, pass the Staff Checkpoint, endure the Skypark's perfect contempt, blind scanner coverage, and send Nova and Gh0st through the service chute toward the Archive.

**Guest Access**

- [ ] Survive the Skypark guest registry and acquire guest access. (`blend_in`)
- [ ] Have Zeke and Orion blind the scanner grid from Laundry Service. (`disable_sensors`)

**Pass the Scanners**

- [ ] Use the service chute to pass into the Archive approach in disguise. (`enter_lobby`)

### The Lens (`w3_mq14`)

Enter the Archive, read the vault's light machinery with Orion and Zeke, and take the Lens knowing the relic will expose more than the crew planned to steal.

**Archive Infiltration**

- [ ] Enter the Archive vault from the service route. (`enter_archive`)
- [ ] Make the containment field, shutters, and tether-lines agree. (`solve_light_puzzle`)

**Claim the Relic**

- [ ] Take the Lens relic after Zeke's warning and survive the alarm. (`take_lens`)

### Burn Notice (`w3_mq15`)

Fight through the lockdown, cross the drone test alcove, defeat the Administrator on the landing pad, and use the Lens to time the Astra through the Shield toward the Foundry before Vale's compliance net closes.

**Escape Lockdown**

- [ ] Cut through the drone test alcove and reach the landing pad under alarm. (`reach_landing_pad`)
- [ ] Break the Administrator's rooftop lockdown. (`defeat_administrator`)
- [ ] Use Scan to read the Shield's launch window. (`scan_shield_gap`)

**Slip the Shield**

- [ ] Send the Astra through the Shield window. (`launch_astra`)

### Corporate Espionage (`w3_sq14`)

Lift a rival executive ledger from a private safe and turn their secrets into leverage.

**Private Safe**

- [ ] At Exec Lounge, copy the concierge credential from the mirror. (`copy_concierge_key`)
- [ ] Use the credential on the private safe in Exec Lounge to steal the encrypted ledger. (`steal_ledger`)
- [ ] Choose whether to publish the ledger or weaponize it privately. (`leak_ledger`)

### Prototype Testing (`w3_sq15`)

Inspect its targeting bias, calibrate the prototype rack, and scrub the telemetry before claiming Phase Rounds.

**Prototype Calibration**

- [ ] In Drone Test Alcove, scan the prototype's hidden targeting bias. (`scan_targeting`)
- [ ] Calibrate the prototype rack in Drone Test Alcove against the recorded drone targets. (`test_weapon`)
- [ ] Scrub Nova's calibration signature from the test telemetry. (`scrub_telemetry`)

### Murder in the Skyline Salon (`w3_sq16`)

Examine the poisoned glass, inspect the automated lounge access logs, interrogate the four trapped suspects, and expose the killer to lift the lockdown.

**Crime Scene Investigation**

- [ ] Inspect Auditor Malick's poisoned glass in the Syndicate Booth. (`inspect_glass`)
- [ ] Inspect the lounge security terminal timestamp log. (`inspect_terminal`)
- [ ] Inspect the sabotaged executive comms rig. (`inspect_rig`)

**Expose the Killer**

- [ ] Confront Director Vance with the contradictory timestamp log. (`expose_vance`)

## Room reference

Base descriptions and actions are listed below; milestone variants and visibility conditions change during play. Rest and Cook use their standard service controls. Use the in-game map for positioning.

### Vent Output

#### Vent Output (`spire_vent_output`)

The vent output coughs the crew into acid rain and neon exhaust. [action:trash pile|Trash piles] clog the curb, and Rain Gutter Alley vanishes west between buildings that lean like they are tired.

Exits: south → Sewer Passage, west → Rain Gutter Alley, east → Underrail Platform, north → Condensate Run.

Interactions: trash pile.

#### Rain Gutter Alley (`spire_rain_gutter_alley`)

Rainwater sheets through Rain Gutter Alley in oily ribbons. A patrol mirror hangs above a cracked stairwell, giving the alley eyes even when no guard is present.

Exits: east → Vent Output.

Interactions: patrol mirror, cracked stairwell.

#### Underrail Platform (`spire_underrail_platform`)

An abandoned underrail platform trembles whenever the city trains pass overhead. The underrail map is scratched with routes, curfews, patrol timing, and warnings written in marker.

Exits: west → Vent Output, east → The Static, down → Transit Plaza.

Interactions: underrail map, patrol timing.

#### Condensate Run (`spire_vent_output_scale_01`)

A condensate run channels warm rainwater down the back of the Vent Output. The scan drip under the pipe maps footsteps from the street above, while the rust initials prove lower-city kids have used this route for years.

Exits: south → Vent Output, north → Maintenance Crawl.

Interactions: condensate run, scan drip.

#### Maintenance Crawl (`spire_vent_output_scale_02`)

A maintenance crawl squeezes between coolant pipes and old warning paint. The old stencil says unauthorized workers become infrastructure, and the pipe gap is just wide enough for a desperate crew.

Exits: south → Condensate Run, north → Rain Grate Overlook.

Interactions: maintenance crawl, old stencil.

#### Rain Grate Overlook (`spire_vent_output_scale_03`)

A rain grate opens above the lower street where upper runoff falls clean before hitting rust. The upper runoff and patrol reflection make the route feel watched from both directions. Tucked behind the grating sits a hidden smuggler stash. A one-way chute leads directly back to the entrance.

Exits: south → Maintenance Crawl.

Interactions: rain grate, upper runoff, smuggler stash, chute.

### The Static

#### The Static (`spire_the_static`)

The Static is a dark dive bar built around an old subway car. Neon signs pulse erratically, the sign outside keeps failing, and the signal stairwell behind the bar hums with pirate broadcasts.

Exits: west → Underrail Platform, east → Old Subway Car, north → Signal Stairwell, south → Transit Plaza, northeast → Back Bar Ledger.

Interactions: sign.

#### Old Subway Car (`spire_old_subway_car`)

The Static spills into an old subway car bolted beside the bar. Torn booth seats, stickered windows, and a silent jukebox make the car half studio. Chris tunes an audio board while Andrew rests his guitar. A reinforced door opens to the rigging deck. A Wayfarer tour poster curls above a No Charge tape beside the audio board.

Exits: west → The Static, east → Bridgman Rigging Deck.

Interactions: silent jukebox, stickered windows, Wayfarer tour poster, No Charge tape.

#### Signal Stairwell (`spire_signal_stairwell`)

A signal stairwell climbs behind The Static, each landing crowded with antenna wire and old mail slots. The whole shaft hums with pirate broadcasts. A chrono coil console hums on the landing, monitoring temporal flux lines.

Exits: south → The Static, north → Zeke's Apartment.

Interactions: antenna wire, mail slots, chrono coil console.

#### Zeke's Apartment (`spire_zekes_apartment`)

Zeke's old apartment is sparse enough to feel erased. A workstation hums on a table with his deleted employee ghost still buried inside it, a planning table waits under bad light, and roof access opens toward the impossible skyline.

Exits: south → Signal Stairwell, north → Safehouse Roof.

Interactions: workstation, planning table.

#### Safehouse Roof (`spire_safehouse_roof`)

The safehouse roof is barely larger than the apartment below it. A skyline of corporate towers rises beyond laundry lines full of patched coats and disguise materials, and the shield shimmer turns the clouds into a visible cage.

Exits: south → Zeke's Apartment.

Interactions: skyline, laundry lines.

#### Back Bar Ledger (`spire_the_static_scale_01`)

The Static's back bar hides cracked glasses, ration labels, and a favor slate no customer admits to reading. A chipped tumbler sits beside the slate, marked with names Jax only says after closing.

Exits: southwest → The Static, north → Roof Stairwell.

Interactions: back bar, favor slate.

#### Roof Stairwell (`spire_the_static_scale_02`)

Concrete roof stairs climb past curfew chalk and old burn marks from drone sweeps. The curfew chalk changes every night, but the scorch line never moves.

Exits: south → Back Bar Ledger, north → Tenant Shrine.

Interactions: roof stairs, curfew chalk.

#### Tenant Shrine (`spire_the_static_scale_03`)

A tenant shrine of folded notices, dead neon, and cheap candles turns eviction paperwork into a memorial wall. The dead neon still flickers when transit drones pass overhead. A discarded courier satchel lies forgotten in the corner. A one-way fire escape leads directly back to the entrance.

Exits: south → Roof Stairwell.

Interactions: tenant shrine, folded notices, courier satchel, fire escape.

#### Bridgman Rigging Deck (`spire_rigging_deck`)

High-tension cables and hydraulic winch drums span the structural gap beneath the entertainment level. Work lights illuminate steel trusses, speaker stacks, and [action:heavy winch rig|heavy tow rigs]. Contractor Cheno studies a [action:rigging schematic|holographic load schematic], his broad shoulders dusted with truss grease. A cider mug and framed family holopic sit beneath the work lights.

Exits: west → Old Subway Car, east → Secret Signal Workshop.

Interactions: rigging schematic, cider mug, heavy winch rig, framed family holopic.

#### Secret Signal Workshop (`spire_hidden_studio`)

Tucked behind acoustic baffles, a cluttered studio hums with glowing CRT monitors and [action:amplifier roost|tube amplifiers]. A miniature [action:galaxy terminal|galaxy simulation] rotates silently. Meeko the charcoal cat lounges like an executive boss while Jason solders [action:workbench clutter|circuit boards]. A music workstation is wedged between the monitors.

Exits: west → Bridgman Rigging Deck.

Interactions: galaxy terminal, music workstation, workbench clutter, amplifier roost.

### Night Market

#### Night Market (`spire_night_market`)

The Night Market insists on being heard. Lanterns swing over food steam, Noodle Row shouts through rain, and a lantern bridge crosses the drain like stubborn stars. A market registry broadcast crackles from a speaker above the stalls.

Exits: west → Checkpoint Queue, east → Noodle Row, north → Lantern Bridge, south → Backstreet Clinic, northeast → Lantern Roof.

Interactions: lanterns, market registry broadcast.

#### Noodle Row (`spire_noodle_row`)

Noodle Row steams under plastic awnings. The soup counter never stops moving, with Chef Ren dishing out hot bowls and news to anyone with coin. The rain drums steadily against the vinyl above.

Exits: west → Night Market.

Interactions: soup counter, plastic awnings.

#### Lantern Bridge (`spire_lantern_bridge`)

A narrow lantern bridge crosses above the market drain. Paper charms flutter from the rail, each one carrying a name, a debt, or a prayer. An executive power siphon runs along a bright cable toward a billboard.

Exits: south → Night Market.

Interactions: paper charms, market drain, executive power siphon.

#### Backstreet Clinic (`spire_backstreet_clinic`)

The backstreet clinic is one room behind a noodle stall, lit by battery lamps and stubborn mercy. A triage curtain gives patients more dignity than privacy.

Exits: north → Night Market.

Interactions: triage curtain, battery lamps.

#### Lantern Roof (`spire_night_market_scale_01`)

String lights cross the lantern roof in careful knots above the Night Market. The anchor knots keep them up when patrol drones skim too low, and the lantern shadows hide handoffs under color. A vendor lockbox is chained beneath the string lights.

Exits: southwest → Night Market, north → Spice Alley.

Interactions: lantern roof, anchor knots, lantern roof kitchen, market refuge, vendor lockbox.

#### Spice Alley (`spire_night_market_scale_02`)

Spice alley smells like heat, fried protein, and ozone. Vendor codes are scratched under every awning, telling locals which stalls sell food, data, or silence. A vendor ledger sits weighted down by an iron hex nut.

Exits: south → Lantern Roof, north → Rain Cache.

Interactions: spice alley, vendor codes, vendor ledger.

#### Rain Cache (`spire_night_market_scale_03`)

A rain cache beneath a torn canopy keeps wrapped tools dry beside counterfeit guest pins. The guest pins are cheap, but the route printed on the underside is current. A heavy black market safe rests under a tarp. A one-way slide leads directly back to the entrance.

Exits: south → Spice Alley.

Interactions: rain cache, wrapped tools, black market safe, slide.

### The Sewers

#### Sewer Landing Pad (`spire_sewers_landing`)

The Astra sits powered down beside a sewer landing pad, its hull reflecting algae-glow and warning strobes from the runoff lock ahead. The air is damp, smelling of ozone, old pipes, and a city trying not to notice you arrived.

Exits: east → Runoff Lock, north → Pump Gallery.

Interactions: astra.

#### Runoff Lock (`spire_runoff_lock`)

A rusted runoff lock divides the sewer landing from the city drains. Green water slaps against a sealed floodgate while a ladder cage climbs into steam.

Exits: west → Sewer Landing Pad, east → Sewer Passage, north → Maintenance Sluice.

Interactions: floodgate, ladder cage.

#### Sewer Passage (`spire_sewers_passage`)

A long concrete sewer passage carries [action:runoff fishing|warm runoff] under the Spire. Water drips from rusted vent pipes overhead while the tunnel splits toward maintenance heat, rainlight, and the guarded city above.

Exits: west → Runoff Lock, north → Vent Output, east → Transit Plaza.

Interactions: vent pipes, runoff fishing.

#### Maintenance Sluice (`spire_maintenance_sluice`)

The maintenance sluice runs above the main tunnel, narrow and hot with valve heat. A maintenance panel blinks beside a pipe map nobody has updated in years.

Exits: south → Runoff Lock.

Interactions: maintenance panel, pipe map.

#### Pump Gallery (`spire_sewers_scale_01`)

The pump gallery beats under the city like a bad heart. A pressure map flashes routes the official grid denies exist, and the pump rhythm covers small mistakes.

Exits: south → Sewer Landing Pad, north → Filter Sluice.

Interactions: pump gallery, pressure map.

#### Filter Sluice (`spire_sewers_scale_02`)

A filter sluice strains glittering trash from black water. A caught badge spins behind the grate, too damaged to pass but clean enough to copy. A drainage marker glows faintly above the water line.

Exits: south → Pump Gallery, north → Ratline Nook.

Interactions: filter sluice, caught badge, drainage marker.

#### Ratline Nook (`spire_sewers_scale_03`)

A dry ratline nook branches away from the main sewer. Chalk arrows mark which exits are watched this week, and the dry ledge smells faintly of smoke and noodle broth. A rusted maintenance locker clings to the wet brickwork. A one-way ladder leads directly back to the entrance.

Exits: south → Filter Sluice.

Interactions: ratline nook, chalk arrows, maintenance locker, ladder.

### Transit Plaza

#### Transit Plaza (`spire_transit_plaza`)

Transit Plaza spreads beneath the Great Elevator, all surveillance glass, guard lanes, and crowded hesitation. The elevator controls tower above a checkpoint queue where fear moves faster than the line, while a security kiosk glows under the stairs.

Exits: north → The Static, west → Sewer Passage, east → Checkpoint Queue, south → Security Kiosk, northeast → Ad Bay.

Interactions: elevator controls.

#### Checkpoint Queue (`spire_checkpoint_queue`)

The checkpoint queue snakes between metal rails and glass cameras. A queue display assigns wait times, threat scores, and fines before anyone reaches a guard.

Exits: west → Transit Plaza, east → Night Market, north → Elevator Service Gate.

Interactions: queue display, glass cameras.

#### Security Kiosk (`spire_security_kiosk`)

A security kiosk sits under the plaza stairs, unattended because the guards trust the software more than each other. The security terminal and case records glow behind cracked glass. A public kiosk relay waits beside the record display.

Exits: north → Transit Plaza.

Interactions: security terminal, case records, public kiosk relay.

#### Elevator Service Gate (`spire_elevator_service_gate`)

A side service gate hides in the shadow of the Great Elevator. Freight crews pass through with eyes down, freight badges out, and no interest in heroics.

Exits: south → Checkpoint Queue.

Interactions: service gate, freight badges.

#### Ad Bay (`spire_transit_plaza_scale_01`)

The ad bay projects a smiling loop of citizens thanking Dominion for clean air. The smiling loop glitches whenever a real worker walks past, and the dead speaker clicks through missing applause.

Exits: southwest → Transit Plaza, north → Ticket Shadow.

Interactions: ad bay, smiling loop.

#### Ticket Shadow (`spire_transit_plaza_scale_02`)

A ticket shadow behind the fare kiosk keeps commuters out of scanner sight for three blessed seconds. The fare kiosk accepts payment faster than guilt, but its blind spot is real.

Exits: south → Ad Bay, north → Underrail Map Room.

Interactions: ticket shadow, fare kiosk.

#### Underrail Map Room (`spire_transit_plaza_scale_03`)

The underrail map room stores obsolete route boards and a service legend that still matches the staff tunnels. A taped transfer notice explains why Dominion forgot to update the old map. An abandoned transit lockbox sits beneath a broken arrivals board. A one-way turnstile leads directly back to the entrance.

Exits: south → Ticket Shadow.

Interactions: underrail map, service legend, transit lockbox, turnstile.

### Laundry Service

#### Laundry Service (`spire_laundry_service`)

Laundry Service runs beneath the Cloud District, packed with steam pipes, cleaning bots, a sensor console, and a service chute for invisible workers. Uniform sorting and the staff checkpoint turn labor into security theater.

Exits: north → Staff Checkpoint, east → Prism Gallery, south → Uniform Sorting, west → Steam Rack.

Interactions: sensor console, service chute.

#### Uniform Sorting (`spire_uniform_sorting`)

Uniform sorting smells like detergent and fear. Uniform racks roll in strict rows, and laundry tags mark each sleeve for a life the Upper City will never notice. An erased memorial name leaves a polished scar on a transit plaque.

Exits: north → Laundry Service, east → Service Lift, south → Exec Lounge.

Interactions: uniform racks, laundry tags, erased memorial name.

#### Service Lift (`spire_service_lift`)

A service lift rises through the bones of the Upper City. A worker network uplink can publish the ledger, while a private dead drop can turn it into a precise weapon against one executive. The lift panel flickers beside a rattling inspection hatch.

Exits: west → Uniform Sorting, north → Skypark Dome, down → Archive Vault, southeast → Exec Lounge.

Interactions: lift panel, inspection hatch, worker network uplink, private dead drop.

#### Staff Checkpoint (`spire_staff_checkpoint`)

The staff checkpoint is clean, bright, and cruelly polite. A badge scanner chirps at every worker while a wall display grades smiles for guest readiness.

Exits: south → Laundry Service, east → Skypark Dome.

Interactions: badge scanner, wall display.

#### Steam Rack (`spire_laundry_scale_01`)

The steam rack turns executive uniforms ghost-white. Badge thread glints inside the cuffs, and the hiss of pressurized water makes the room safe for whispered planning.

Exits: east → Laundry Service, north → Press Line.

Interactions: steam rack, badge thread.

#### Press Line (`spire_laundry_scale_02`)

The press line stamps every guest crease to the same perfect angle. A guest crease becomes a social password here, and the heat plate can make cheap cloth look expensive from scanner distance.

Exits: south → Steam Rack, north → Service Bin.

Interactions: press line, guest crease.

#### Service Bin (`spire_laundry_scale_03`)

A service bin overflows with ruined napkins, broken jewelry, and a discarded pass snapped in half. The discarded pass is dead, but its route stamp is still readable. A tailored uniform case has been hidden inside an air intake. A one-way chute leads directly back to the entrance.

Exits: south → Press Line.

Interactions: service bin, discarded pass, tailored uniform case, chute.

### Skypark

#### Skypark Dome (`spire_skypark_dome`)

The Skypark Dome offers fake sun and blooming orchids under polished glass. Evelyn sings jazz while pruning glowing blossoms, and Robert demonstrates his industrial cleaning rigs with silent theatrical flair. A guest registry tracks arrivals beside a jazz conservatory planter and an industrial vortex buffer.

Exits: south → Service Lift, west → Staff Checkpoint, east → Glasswalk, north → Orchid Walk.

Interactions: guest registry, jazz conservatory planter, industrial vortex buffer.

#### Glasswalk (`spire_glasswalk`)

The glasswalk curves along the dome edge with the whole Lower City visible far below. A camera seam runs under the handrail like a vein.

Exits: west → Skypark Dome, east → Donor Gallery, down → Archive Vault.

Interactions: camera seam, handrail.

#### Donor Gallery (`spire_donor_gallery`)

Executive portraits line the marble hall. A donor wall and portrait plaques praise private contracts, while a crimson laser grid console seals the northern private corridor.

Exits: west → Glasswalk, south → Archive Vault, east → Skyline VIP Salon, north → Executive Black-Site Lab.

Interactions: laser grid console, donor wall, portrait plaques.

#### Executive Black-Site Lab (`spire_black_site_lab`)

Cold neon backlights row upon row of cryogenic stasis cylinders and prototype server banks. An executive black-box safe rests sealed beneath a humming quantum holographic projector.

Exits: south → Donor Gallery.

Interactions: black-box safe, holographic projector.

#### Orchid Walk (`spire_skypark_scale_01`)

The orchid walk blooms under artificial rain. A donor plaque thanks families who bought their way above the smog, while root sensors drink more clean water than Trade Row sees in a week. A hydroponic mist nozzle hisses fine vapor into the humid breeze.

Exits: south → Skypark Dome, north → Registry Pavilion.

Interactions: orchid walk, donor plaque, hydroponic mist nozzle.

#### Registry Pavilion (`spire_skypark_scale_02`)

A registry pavilion floats over the park with a guest ledger projected in polite gold. The guest ledger shows which false identity can survive the next checkpoint. A pavilion bench cache is tucked beneath the seating.

Exits: south → Orchid Walk, north → Glass Lawn.

Interactions: registry pavilion, guest ledger, skypark cookfire, pavilion shelter, pavilion bench cache.

#### Glass Lawn (`spire_skypark_scale_03`)

The glass lawn is transparent all the way down to the city it refuses to see. Scanner mist drifts between ornamental trees, checking posture, fabric, and fear. A locked botanical vault is embedded in the synthetic root bed. A one-way spiral leads directly back to the entrance.

Exits: south → Registry Pavilion.

Interactions: glass lawn, scanner mist, botanical vault, spiral.

#### Skyline VIP Salon (`spire_skyline_vip_suite`)

Plush velvet stanchions divide the glowing skyline salon from the rain-swept spires outside. Holographic chandeliers illuminate four stranded patrons around an automated crime-scene barrier. Emergency sirens pulse against the soundproof glass. A dispenser log and security terminal wait beside the barrier.

Exits: west → Donor Gallery, north → Syndicate Booth.

Interactions: crime-scene barrier, dispenser log, security terminal.

#### Syndicate Booth (`spire_vip_private_booth`)

A private booth of curved mahogany overlooks the neon clouds. Auditor Malick slumps lifeless across the table beside a crystal glass. An executive comms rig lies open near the victim's splayed fingers. The auditor's datapad lies beside the comms rig.

Exits: south → Skyline VIP Salon.

Interactions: crystal glass, executive comms rig, auditor's datapad.

### Exec Lounge

#### Exec Lounge (`spire_exec_lounge_bar`)

The Exec Lounge floats on a grav-platform above a silent atrium. A private safe hides behind sculpture, a concierge credential mirror flashes beside the terminal, and every booth looks designed for blackmail. A VIP vector arcade cabinet stands behind the velvet curtain.

Exits: north → Uniform Sorting, northwest → Service Lift, northeast → Archive Vault, south → Velvet Hall.

Interactions: private safe, concierge credential mirror, concierge terminal, VIP vector arcade cabinet.

#### Velvet Hall (`spire_exec_lounge_scale_01`)

A velvet hall swallows footsteps with expensive sound baffles. The sound baffles make every conversation private until someone pays more. Sticky crystallized liquor glazes the tabletop.

Exits: north → Exec Lounge, east → Private Booth.

Interactions: velvet hall, sound baffles, crystallized liquor.

#### Private Booth (`spire_exec_lounge_scale_02`)

A private booth overlooks the atrium. A half-empty glass sweats beside a terminal still logged into a director account. The safe keypad is scorched and dead.

Exits: west → Velvet Hall, north → Blackmail Balcony.

Interactions: private booth, half-empty glass, safe keypad.

#### Blackmail Balcony (`spire_exec_lounge_scale_03`)

The blackmail balcony angles every table toward a camera seam disguised as decorative brass. The camera seam records everything except ownership.

Exits: south → Private Booth, north → Ledger Office.

Interactions: blackmail balcony, camera seam.

#### Ledger Office (`spire_exec_lounge_scale_04`)

A ledger office behind the lounge safe stores an encrypted ledger under layers of concierge niceties. The encrypted ledger hums with executive panic.

Exits: south → Blackmail Balcony, north → Service Dumbwaiter.

Interactions: ledger office, encrypted ledger.

#### Service Dumbwaiter (`spire_exec_lounge_scale_05`)

A Service Dumbwaiter drops toward the kitchen levels with a silver tray still balanced inside. The silver tray reflects the patrol outside better than the room's polished cameras. A discreet executive safe gleams behind the mirrored mahogany. A one-way elevator leads directly back to the entrance.

Exits: south → Ledger Office.

Interactions: Service Dumbwaiter, silver tray, executive safe, elevator.

### The Archive

#### Prism Gallery (`spire_prism_gallery`)

The Prism Gallery bends Archive light into audible color. Prism shutters split the containment field into three bands, and a braided chord sleeps inside the glass until the room's evidence agrees.

Exits: west → Laundry Service, east → Archive Vault.

Interactions: prism shutters, braided chord, scan alarm chords.

#### Archive Vault (`spire_archive_vault`)

The Archive Vault rises in indexed crystal stacks over a glass floor two hundred stories high. The lens relic hangs in a containment field that hums toward Nova before she touches it. Command tether-lines vanish into the glass, routing power back toward the Prism Gallery.

Exits: west → Prism Gallery, north → Donor Gallery, southwest → Exec Lounge, east → Drone Test Alcove, northeast → Catalog Atrium.

Interactions: containment field, lens relic, command tether-lines, scan tether-lines.

#### Drone Test Alcove (`spire_drone_test_alcove`)

Scratched glass and warning tape seal the alcove. A prototype rack waits beside sleeping security drones, their dormant sensor paths revealed by the Lens. A flight telemetry log flashes on the rack screen. A prototype targeting audit waits on the calibration screen.

Exits: west → Archive Vault, east → Landing Pad Roof.

Interactions: prototype rack, prototype targeting audit, security drones, scan targeting paths, flight telemetry log.

#### Catalog Atrium (`spire_archive_scale_01`)

The catalog atrium sings file numbers in a soft index choir. Each note points to a life Dominion turned into property, and the index choir never wonders who is listening. An index terminal flashes amber in the dark.

Exits: southwest → Archive Vault, north → Prism Service Bay.

Interactions: catalog atrium, index choir, index terminal.

#### Prism Service Bay (`spire_archive_scale_02`)

The prism service bay stores a light wrench and replacement lenses for the gallery array. The light wrench proves the holy-looking Archive still needs maintenance. A lens polishing cloth lies on the technician's tray.

Exits: south → Catalog Atrium, north → Alarm Spine.

Interactions: prism service bay, light wrench, lens polishing cloth.

#### Alarm Spine (`spire_archive_scale_03`)

The alarm spine runs behind the vault in red conduits. Every conduit is labeled with a consequence instead of a destination, and the red conduits pulse faster when Nova gets close. An encrypted datacache hums softly inside a book press. A one-way lift leads directly back to the entrance.

Exits: south → Prism Service Bay.

Interactions: alarm spine, red conduits, encrypted datacache, lift.

### Landing Pad

#### Landing Pad Roof (`spire_landing_pad_roof`)

The roof shudders under violent wind and red lockdown light. The Astra waits across the pad while the Planetary Shield rotates overhead in heavy resonance bands, guarding the sky. The flight display marks a shield window and a launch window beside a test telemetry relay.

Exits: west → Drone Test Alcove, north → Fuel Bridge.

Interactions: shield window, launch window, test telemetry relay.

#### Fuel Bridge (`spire_landing_pad_scale_01`)

A fuel bridge rattles between the roof and the Astra. Wind shear shoves rain sideways through warning lights and reveals the Shield rhythm in the clouds.

Exits: south → Landing Pad Roof, north → Shield Sightline.

Interactions: fuel bridge, wind shear.

#### Shield Sightline (`spire_landing_pad_scale_02`)

The shield sightline lines up with a tiny gap shimmer rotating through the storm. The gap shimmer lasts less than a breath.

Exits: south → Fuel Bridge, north → Drone Wreck Roof.

Interactions: shield sightline, gap shimmer.

#### Drone Wreck Roof (`spire_landing_pad_scale_03`)

A drone wreck skids across the roof, its broken rotor still trying to obey a command that no longer matters. The broken rotor twitches whenever the alarm network speaks.

Exits: south → Shield Sightline, north → Boarding Causeway.

Interactions: drone wreck, broken rotor.

#### Boarding Causeway (`spire_landing_pad_scale_04`)

The boarding causeway stretches toward the Astra hatch under lockdown strobes. The Astra hatch light blinks like a heartbeat through rain and warning static. A forgotten avionics crate sits strapped against the wind break. A one-way ladder leads directly back to the entrance.

Exits: south → Drone Wreck Roof.

Interactions: boarding causeway, Astra hatch, avionics crate, ladder.

