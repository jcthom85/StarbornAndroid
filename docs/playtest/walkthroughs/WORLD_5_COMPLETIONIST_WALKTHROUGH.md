# World 5 — The Void / The Cradle: completionist route and playtest guide

Updated November 5, 2026 for the repaired Ring navigation build. Older installations may still have diagonal or one-way Paths links and an unguarded Anchor entrance. This replaces the earlier speculative guide. Coverage means all five main quests and all optional quests on the documented routes; it does not certify every room, build, or incidental interaction.

## Navigation and return travel

The Ring has a complete NSEW walking network with immediate opposite-direction returns. Two reciprocal vertical alternatives use named, tappable **Paths** links; they do not require vertical swipes. There are no diagonal exits in World 5. Tapped return shortcuts are optional conveniences; every branch also has a walking return.

- **Upper Ring → Server Farm:** Service Shaft → east → Zero-G Junction → south → SysAdmin Nest → east → Server Farm. Return west/north/west. Alternatively, tap **Descend the cables to Server Farm** at Service Shaft, or **Climb the cables to Service Shaft** at Server Farm. The eastern breach at Zero-G Junction is a separate, magnetic-clamp-controlled route into the optional lab.
- **Security service wing:** Security Hub → south → Ring Service Alcove → south through its Pressure Gallery, Overlook Niche, Control Pocket, Quiet Cache and Echo Walk to Ring Threat Cathedral. Return north.
- **Server maintenance wing:** Server Farm → south → Purged Backup Rack → south → Ring Service Alcove → south through its Pressure Gallery and Overlook Niche to Ring Control Pocket. Return north. These room names also occur in other areas; use the room IDs in the reference below to distinguish them.
- **Solarium → Security:** go north to Executive Dock, east to Grand Concourse and east to Security Hub. Reverse west/west/south to return; the diagonal passage is removed.
- **Firewall Gamma → Anchor:** after defeating the Construct, tap **Ride the access lift to the Anchor Chamber**; use **Ride the access lift to Firewall Gamma** for the return. The NSEW alternative goes west to Beta, west to Alpha, west to Mainframe Nave, south to Server Farm, then east into Anchor Chamber. Reverse the directions to return. Disabling Gamma alone does not clear either the chamber entrance or the access lift.

The Anchor entrance stays open once cleared. From the chamber, walk west to Server Farm, west to SysAdmin Nest, north to Zero-G Junction, west to Service Shaft, west to Security Hub, west to Grand Concourse and north to Customs Lounge for rest and supplies. At Server Farm, the cable climb is a shorter alternative back to Service Shaft. Both choices remain available after the Elara/Anchor and Soloist scenes until you deliberately enter the Tear. Older saves already beyond the chamber entrance retain their route out without receiving a second boss reward.

Customs Lounge remains reachable for preparation before the initial Orbital Fighter. Entering the reality breach into the Source remains the deliberate story departure; keep a separate predeparture save and finish station business before using it.

## Main route

1. Before forcing the dock, visit Customs Lounge to rest, spend earned AP, and purchase supplies from the Orbital Dispensary. The tested route equips Orion’s earned Heat Liner, purchases six Medkit+ and a Glimmer Tonic, and consumes the tonic before the fighter. These are one verified preparation strategy, not required purchases for every build.

2. Defeat the Orbital Fighter, force the dock, and hack the sealed airlock. Cross the Solarium, deal with the Compliance Officer, traverse the Service Shaft, access the mainframe, and find Thorne in the Mainframe Nave. Speak with her or inspect the Thorne trace and finish the dialogue.

3. Complete Employee of the Month, Solar Maintenance, Vacuum Seal and Ghost in the Shell while the station remains accessible; the complementary runs reverse their order. Collect the SysAdmin key early if desired, but leave the Admin Privileges armory turn-in until the guarded Anchor route is cleared. Vacuum Seal requires the ordered bulkhead simulation below.

4. Navigate the server maze and operate Firewall Alpha, Beta, and Gamma. Win the real Firewall Construct battle; the three controls alone do not finish MQ23 or open the Anchor entrance. Take the named access lift down to the cleared Anchor Chamber, with a matching return lift up to Gamma. Alternatively, walk west through Beta and Alpha to Mainframe Nave, south to Server Farm, then east into the chamber. Both entrances require victory over the Construct.

5. Enter the Anchor Chamber, speak with Elara through the room action, then take the Anchor. Visit the Throne Room; use the SysAdmin key to open the executive armory if completing Admin Privileges, then witness Vale’s Soloist scene. The Tear remains blocked until the Compliance Avatar is defeated.

6. Restock before entering the Tear. The production crew route buys ten Medkit+ and two Glimmer Tonics for the Source, funded by earned credits and sales of owned supplies/equipment. Enter the Tear only when ready to move on.

## Puzzles, choices, and recovery

Vacuum Seal’s pressure map gives the order: **red carpet seam → docking collar → observation glass**. At the airlock console, select those three commands. An incorrect command fails the simulation without moving a live bulkhead or consuming resources. Reset or leave freely. Leaving or reloading before success restarts the simulation and preserves the mapped clue. After the seals hold, reopen dock access for workers to claim the quest reward.

Ghost in the Shell has two exclusive endings: restore the guardian’s independence for Data Shield, or bind it into a voluntary Guardian Covenant. The first is passive party protection while Zeke lives; the second is an active skill. Thorne and Vale’s visible NPC interactions now resolve their full display names to their authored dialogue speakers.

Local return shortcuts remain usable after their first trip, including saves with old completion receipts. Save before committing to a branch or leaving the world.

## Optional quest routes and rewards

Rewards below are the completion-event payouts. Battle loot, earlier step rewards, and crew AP awards are separate. Room and prop names come directly from the current assets.

### Employee of the Month (`w5_sq21`)

- **Security Hub — Director redaction checksum:** Recover the Director's redaction map.
- **Grand Concourse — director logs:** Read Thorne's private logs.
- **Grand Concourse — concourse public broadcast:** Publish the Director's unredacted logs.

Completion `w5_sq21_publish_logs`: Corporate Insight, 400 XP.

### Solar Maintenance (`w5_sq22`)

- **Mirror Walk — false sun path:** Trace the Solarium false sun before restoring the mirrors.
- **Mirror Walk — mirror panels:** Restore the Solarium mirrors.
- **Solarium — garden light priority:** Redirect the false sun toward the gardens.

Completion `w5_sq22_restore_gardens`: Sunbeam, 400 XP.

### Vacuum Seal (`w5_sq23`)

- **Executive Dock — pressure map:** Map Executive Dock pressure loss before sealing the breaches.
- **Executive Dock — airlock console:** Seal the Executive Dock breaches.
- **Executive Dock — dock access priority:** Reopen the safe dock for non-executive traffic.

Completion `w5_sq23_reopen_dock`: 1 × Mag-Boots, 450 XP.

### Ghost in the Shell (`w5_sq24`)

- **Server Farm — purged backup rack:** Trace the defensive backup purge.
- **Purged Backup Rack — purged backup rack:** Recover purged defensive routines.
- **Server Farm — guardian ownership flag:** Restore the recovered routine as an independent guardian.
- **Server Farm — crew covenant socket:** Replace ownership with a mutual crew covenant.

Completion `w5_sq24_restore_guardian`: Data Shield, 500 XP.

Completion `w5_sq24_bind_guardian`: Guardian Covenant, 500 XP.

### Admin Privileges (`w5_sq25`)

- **SysAdmin Nest — dead sysadmin:** Recover the SysAdmin keycard.
- **Throne Room — executive armory:** Open the executive armory.

Completion `w5_sq25_open_armory`: 1 × Void Clip, 550 XP.

## Main-route action reference

| Quest | Room | Visible control |
| --- | --- | --- |
| `w5_mq21` | Airlock Gallery | docking collar |
| `w5_mq21` | Executive Dock | sealed airlock |
| `w5_mq22` | Solarium | fake beach |
| `w5_mq22` | Zero-G Junction | patrol timing |
| `w5_mq22` | Mainframe Nave | mainframe terminal |
| `w5_mq23` | Server Farm | coolant-pulse map |
| `w5_mq23` | Firewall Alpha | firewall node alpha |
| `w5_mq23` | Firewall Beta | firewall node beta |
| `w5_mq23` | Firewall Gamma | firewall node gamma |
| `w5_mq24` | Anchor Chamber | anchor relic |
| `w5_mq25` | The Tear | reality breach |
| `w5_mq22` | Mainframe Nave | thorne trace |
| `w5_mq24` | Anchor Chamber | elara |
| `w5_mq25` | Throne Room | containment field |

## Acceptance checks

- Finish every optional quest and reload after its reward; confirm XP, AP, items, and branch outcomes persist.
- Attempt the opposite exclusive ending after completing a branch; confirm no second payout.
- Return through a local shortcut more than once, including after reload.
- Check scene readability, notifications, touch targets, and combat preparation on a phone.
- Record whether clues were understandable without this guide, and whether detours were worth taking. Automated known-answer routes cannot measure those experiences.

See [the Worlds 4–6 refinement report](../../testing/WORLDS456_REFINEMENT_2026-11-04.md) for evidence and test limits.

## Complete room and exit reference

The Exits lines below list NSEW swipe routes; Tapped Paths lines list named vertical alternatives. The Anchor security entrance and magnetic lab crossing retain their stated requirements.

### Executive Dock

#### Executive Dock (`orbital_executive_dock`)

Compliance fighters rake the Astra beneath the Ring. Docking controls face the sealed airlock; its airlock console displays a pressure map and dock access priority. Beyond the glass, red carpet floats over white flooring beside Thorne loyalists' bodies, adrift in dead gravity.

Exits: north → Airlock Gallery, east → Grand Concourse, south → Solarium, west → Ring Service Alcove.

Interactions: docking controls, sealed airlock, pressure map, airlock console, dock access priority.

#### Airlock Gallery (`orbital_airlock_gallery`)

A glass gallery curves above the docking collar. The planet turns below in perfect silence while the stolen Deep-Core Engine shakes heat through the Astra's spine. An airlock manifest hangs beside a speaker playing the Compliance greeting.

Exits: south → Executive Dock.

Interactions: docking collar, compliance greeting, airlock manifest.

#### Ring Service Alcove (`orbital_executive_dock_scale_01`)

A luggage recess hides the service crawlway beneath an executive arrival mural. Scuffed knee plates have polished the lower edge of its brass trim.

Exits: east → Executive Dock, north → Ring Pressure Gallery.

Interactions: service alcove, maintenance mark.

#### Ring Pressure Gallery (`orbital_executive_dock_scale_02`)

A pressure gallery runs behind the polished walls. Its warning strip flashes whenever exterior vacuum loads the hospitality seals.

Exits: south → Ring Service Alcove, north → Ring Overlook Niche.

Interactions: pressure gallery, warning strip.

#### Ring Overlook Niche (`orbital_executive_dock_scale_03`)

Through a narrow inspection slit, the docking collar flexes against the Astra. The guest windows hide the cables carrying that load.

Exits: south → Ring Pressure Gallery, north → Ring Control Pocket.

Interactions: overlook niche, sightline.

#### Ring Control Pocket (`orbital_executive_dock_scale_04`)

An arrival-light relay sits behind a decorative panel. Its switch is tagged DISPLAY ONLY; the nearby pressure controls have their own sealed circuit.

Exits: south → Ring Overlook Niche, north → Ring Quiet Cache.

Interactions: control pocket, manual toggle.

#### Ring Quiet Cache (`orbital_executive_dock_scale_05`)

A shelf holds dried sealant cartridges, each split open to prove it empty. Someone has crossed out the next delivery date three times.

Exits: south → Ring Control Pocket, north → Ring Echo Walk.

Interactions: quiet cache, wrapped tool.

#### Ring Echo Walk (`orbital_executive_dock_scale_06`)

The collar intercom repeats an abandoned freight request. Between the formal announcements, a loader keeps asking whether anyone has cleared the workers to board.

Exits: south → Ring Quiet Cache, north → Ring Dock Memorial.

Interactions: echo walk, old signal.

#### Ring Dock Memorial (`orbital_executive_dock_scale_final`)

A dock memorial projects names over the forced-docking scar, then redacts the staff names first. The dock memorial and redaction pulse show the Ring preserving class even during collapse. An airtight diplomatic courier case floats secured by velcro. An optional return chute leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Ring Echo Walk.

Interactions: dock memorial, redaction pulse, diplomatic courier case, chute.


### Grand Concourse

#### Grand Concourse (`orbital_grand_concourse`)

Golden Architect statues tower over an immaculate promenade. Every wall repeats Vale's compliance notices between advertisements for Harmony. A holographic directory lists the director logs beside controls for the concourse public broadcast.

Exits: north → Customs Lounge, west → Executive Dock, east → Security Hub, south → Ring Service Alcove.

Interactions: director logs, concourse public broadcast, holographic directory.

#### Customs Lounge (`orbital_customs_lounge`)

A silent lounge waits behind velvet queue ropes and biometric gates. An automated commissary terminal hums on the wall beside an executive galley recycler and lounge seating. A customs observation console rests in the waiting niche beside an [action:encrypted slate|analyst's glowing slate].

Exits: south → Grand Concourse.

Interactions: biometric gates, executive galley recycler, lounge seating, customs observation console, commissary terminal, encrypted slate.

#### Ring Service Alcove (`orbital_grand_concourse_scale_01`)

Behind a directory of luxury suites, a cleaner has scratched a second map at shoulder height. It follows waste pipes instead of promenades.

Exits: north → Grand Concourse, east → Ring Pressure Gallery.

Interactions: service alcove, maintenance mark.

#### Ring Pressure Gallery (`orbital_grand_concourse_scale_02`)

A pressure inspection rail follows the back of a gold wall. Its indicators have been turned inward so staff must read them from the service passage.

Exits: west → Ring Service Alcove, north → Ring Overlook Niche.

Interactions: pressure gallery, warning strip.

#### Ring Overlook Niche (`orbital_grand_concourse_scale_03`)

A gap above a ceiling panel reveals the concourse chandelier from behind. Its glitter rests on a welded frame with fresh repair marks.

Exits: south → Ring Pressure Gallery, north → Ring Control Pocket.

Interactions: overlook niche, sightline.

#### Ring Control Pocket (`orbital_grand_concourse_scale_04`)

The directory lighting has a manual relay beside a list of names. Room numbers are printed; the erased guest names are not.

Exits: south → Ring Overlook Niche, north → Ring Quiet Cache.

Interactions: control pocket, manual toggle.

#### Ring Quiet Cache (`orbital_grand_concourse_scale_05`)

An old cleaning cart is parked in a cupboard too shallow for its handle. A folded shift rota uses job titles where names should be.

Exits: south → Ring Control Pocket, north → Ring Echo Walk.

Interactions: quiet cache, wrapped tool.

#### Ring Echo Walk (`orbital_grand_concourse_scale_06`)

A hospitality announcement echoes through an uncarpeted passage. Here, without the public speakers smoothing it, the pause before "valued guest" sounds painfully long.

Exits: south → Ring Quiet Cache, north → Ring Courtesy Theater.

Interactions: echo walk, old signal.

#### Ring Courtesy Theater (`orbital_grand_concourse_scale_final`)

A courtesy theater keeps playing welcome footage to empty chairs while blast shutters grind nearby. The welcome footage and empty chairs make Harmony's politeness feel obscene. A shattered luxury display locker holds untouched luxury goods. An optional return express escalator leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Ring Echo Walk.

Interactions: welcome footage, empty chairs, luxury display locker, express escalator.


### Solarium

#### Solarium (`orbital_solarium`)

Real soil, tropical trees, and a fake beach sit beneath a fusion lamp and mirror array. The fruit is rotting in a paradise built for executives. A panel sets garden light priority, while a solarium tide cache rests beneath artificial coral ledges. Water laps the [action:false tide fishing|artificial shoreline].

Exits: north → Executive Dock, south → Mirror Walk, west → Ring Service Alcove.

Interactions: fake beach, mirror array, garden light priority, false tide fishing, solarium tide cache.

#### Mirror Walk (`orbital_mirror_walk`)

Polished mirror panels line a humid maintenance walk behind the Solarium. Each panel reflects a different angle of the same fake sun. The false sun path converges on a solar reflector hub.

Exits: north → Solarium.

Interactions: false sun path, mirror panels, solar reflector hub.

#### Ring Service Alcove (`orbital_solarium_scale_01`)

Water stains lead from the garden border into a narrow irrigation recess. Roots have found the joints that the decorative stonework conceals.

Exits: east → Solarium, north → Ring Pressure Gallery.

Interactions: service alcove, maintenance mark.

#### Ring Pressure Gallery (`orbital_solarium_scale_02`)

A condensation channel runs along the outer glass. Beyond it is black space. Mirror mounts on the inner frame show how carefully the garden daylight must be aimed.

Exits: south → Ring Service Alcove, north → Ring Overlook Niche.

Interactions: pressure gallery, warning strip.

#### Ring Overlook Niche (`orbital_solarium_scale_03`)

From this slit behind the beds, the mirror array is visible edge-on. The brilliant garden sun resolves into hinges, reflectors, and a tracked lamp.

Exits: south → Ring Pressure Gallery, north → Ring Control Pocket.

Interactions: overlook niche, sightline.

#### Ring Control Pocket (`orbital_solarium_scale_04`)

A dead decorative fountain shares a cabinet with the garden lighting schedule. Both were programmed for visitors who would never ask where the water went.

Exits: south → Ring Overlook Niche, north → Ring Quiet Cache.

Interactions: control pocket, manual toggle.

#### Ring Quiet Cache (`orbital_solarium_scale_05`)

Seed packets lie flattened between pages of an old planting ledger. Each failed crop has a date beside it; later entries stop using the word ornamental.

Exits: south → Ring Control Pocket, north → Ring Echo Walk.

Interactions: quiet cache, wrapped tool.

#### Ring Echo Walk (`orbital_solarium_scale_06`)

An irrigation pipe ticks as it cools. The sound moves along the beds, a small physical rhythm beneath the station's recorded birdsong.

Exits: south → Ring Quiet Cache, north → Ring False Tide.

Interactions: echo walk, old signal.

#### Ring False Tide (`orbital_solarium_scale_final`)

A false tide laps against the Solarium beach with water pumped through hidden climate veins. The false tide and climate veins make paradise visibly mechanical. A pressurized hydroponic canister sits among dead decorative moss. An optional return stairwell leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Ring Echo Walk.

Interactions: false tide, climate veins, hydroponic canister, stairwell.


### Security Hub

#### Security Hub (`orbital_security_hub`)

A fortified checkpoint seals the lift to the core. Compliance officers move without conversation while Null-G drones cling to the ceiling. A security display pairs armory diagnostics with the Director redaction checksum. The service corridor extends south behind the checkpoint; the surveillance platform remains north.

Exits: north → Surveillance Pit, west → Grand Concourse, east → Service Shaft, south → Ring Service Alcove.

Interactions: Director redaction checksum, armory diagnostics.

#### Surveillance Pit (`orbital_surveillance_pit`)

A sunken room of red lenses tracks every corridor on the executive deck. The panopticon archive preserves screens that flag posture, pulse, hesitation, and anything else a frightened system calls intent.

Exits: south → Security Hub.

Interactions: red lenses, panopticon archive.

#### Ring Service Alcove (`orbital_security_hub_scale_01`)

A service hatch sits below a camera calibration chart. The chart leaves a blank wedge where technicians must stand to adjust the lenses. The Security Hub is north; the maintenance passage continues south.

Exits: north → Security Hub, south → Ring Pressure Gallery.

Interactions: service alcove, maintenance mark.

#### Ring Pressure Gallery (`orbital_security_hub_scale_02`)

A reinforced gallery separates the security office from the outer hull. Its pressure strip has a manual inspection date newer than the staff roster.

Exits: north → Ring Service Alcove, south → Ring Overlook Niche.

Interactions: pressure gallery, warning strip.

#### Ring Overlook Niche (`orbital_security_hub_scale_03`)

A maintenance window looks onto the backs of the camera housings. From here their overlapping cones are mechanical coverage, not an all-seeing eye.

Exits: north → Ring Pressure Gallery, south → Ring Control Pocket.

Interactions: overlook niche, sightline.

#### Ring Control Pocket (`orbital_security_hub_scale_04`)

A test relay has been sealed under a clear cover. Beside it, handwritten instructions insist on preserving the original logs before diagnostics.

Exits: north → Ring Overlook Niche, south → Ring Quiet Cache.

Interactions: control pocket, manual toggle.

#### Ring Quiet Cache (`orbital_security_hub_scale_05`)

An evidence carton contains discarded blank badges and a ruined label roll. Even the staff who moved evidence needed permission to touch an empty box.

Exits: north → Ring Control Pocket, south → Ring Echo Walk.

Interactions: quiet cache, wrapped tool.

#### Ring Echo Walk (`orbital_security_hub_scale_06`)

A diagnostic speaker repeats an incomplete incident number. The next field is always a silence where an operator should have answered.

Exits: north → Ring Quiet Cache, south → Ring Threat Cathedral.

Interactions: echo walk, old signal.

#### Ring Threat Cathedral (`orbital_security_hub_scale_final`)

A threat cathedral stacks surveillance feeds into stained-glass panes of suspicion. The surveillance panes and priority docket turn security into worship. A heavy tactical enforcer armory is recessed into the bulkhead. An optional return hatch leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: north → Ring Echo Walk.

Interactions: surveillance panes, priority docket, tactical enforcer armory, hatch.


### Service Shaft

#### Service Shaft (`orbital_service_shaft`)

Gravity ends at the hatch. Zero-g cables descend from a superconductor junction through a vertical dark while Hunter-Killer droids scrape along distant walls, too armored to fight head-on. The marked route runs east to Zero-G Junction; the southern tube there leads to the SysAdmin station and Server Farm. Anchored service cables also offer a direct descent to Server Farm and a climb back to this hatch.

Exits: west → Security Hub, east → Zero-G Junction, north → Ring Service Alcove.

Tapped Paths: **Descend the cables to Server Farm** (down → Server Farm). Works in both directions.

Interactions: zero-g cables, superconductor junction.

#### Zero-G Junction (`orbital_zero_g_junction`)

Three maintenance tubes meet in a dead-gravity junction beside a gyroscope stabilizer. A display tracks patrol timing. An emergency magnetic clamp console overlooks the eastern breach, where unanchored scrap drifts dangerously. The southern maintenance tube reaches the SysAdmin Nest and the Server Farm approach. The western tube returns to Service Shaft; the magnetic clamp controls only the eastern breach.

Exits: west → Service Shaft, east → Breached Avionics Lab, south → SysAdmin Nest.

Interactions: magnetic clamp console, patrol timing, gyroscope stabilizer.

#### Breached Avionics Lab (`orbital_breached_avionics_lab`)

Cold stellar light floods through a [action:shattered canopy|shattered observation canopy]. Frozen liquid coolant beads drift above abandoned quantum mainframes, where an [action:experimental ai sub-vault|emergency AI sub-vault] remains sealed.

Exits: west → Zero-G Junction.

Interactions: experimental ai sub-vault, shattered canopy.

#### Ring Service Alcove (`orbital_service_shaft_scale_01`)

Cable spools narrow the entrance to a worker passage. The only route marker is an arrow on the side of a spool that was too heavy to move.

Exits: south → Service Shaft, north → Ring Pressure Gallery.

Interactions: service alcove, maintenance mark.

#### Ring Pressure Gallery (`orbital_service_shaft_scale_02`)

A pressure conduit follows the shaft wall beside a row of handholds. Every third handhold has a tether scar worn into its base.

Exits: south → Ring Service Alcove, north → Ring Overlook Niche.

Interactions: pressure gallery, warning strip.

#### Ring Overlook Niche (`orbital_service_shaft_scale_03`)

A gap between cable trays exposes several floors of the station at once. Luxury rooms and machinery occupy opposite sides of the same thin partitions.

Exits: south → Ring Pressure Gallery, north → Ring Control Pocket.

Interactions: overlook niche, sightline.

#### Ring Control Pocket (`orbital_service_shaft_scale_04`)

A local work-light switch sits beside a signed lockout tag. The circuit powers this recess; the main station feed runs in a sealed conduit beyond it.

Exits: south → Ring Overlook Niche, north → Ring Quiet Cache.

Interactions: control pocket, manual toggle.

#### Ring Quiet Cache (`orbital_service_shaft_scale_05`)

A tether bag hangs from a hook, its buckle cut cleanly through. Beside it are empty ration wrappers folded into a neat stack.

Exits: south → Ring Control Pocket, north → Ring Echo Walk.

Interactions: quiet cache, wrapped tool.

#### Ring Echo Walk (`orbital_service_shaft_scale_06`)

Cable vibration carries a freight motor's pulse through the passage. Voices from the guest decks vanish into insulation, but the machinery never quite does.

Exits: south → Ring Quiet Cache, north → Ring Drift Chapel.

Interactions: echo walk, old signal.

#### Ring Drift Chapel (`orbital_service_shaft_scale_final`)

A drift chapel of tether hooks and emergency blankets floats beside the service shaft. The tether hooks and prayer patch show the workers who made zero-g survivable. An orange EVA engineer pack is clamped to the structural truss. An optional return guide leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Ring Echo Walk.

Interactions: tether hooks, prayer patch, eva engineer pack, guide.


### Server Farm

#### Server Farm (`orbital_server_farm`)

Mist surrounds a purged backup rack. Recover its routine: erase the guardian ownership flag for independence or use the crew covenant socket for shared protection. A mainframe terminal displays a coolant-pulse map; a dead sysadmin drifts behind a coolant pipe. The SysAdmin Nest west leads back to Zero-G Junction and the upper Ring. Mainframe Nave is north, the backup aisle south, and the Anchor entrance east remains guarded until the Firewall Construct falls. Anchored service cables climb directly to Service Shaft and provide a return descent. The SysAdmin and Zero-G Junction walking route remains available.

Exits: north → Mainframe Nave, south → Purged Backup Rack, west → SysAdmin Nest, east → Anchor Chamber.

Tapped Paths: **Climb the cables to Service Shaft** (up → Service Shaft). Works in both directions.

Interactions: mainframe terminal, coolant-pulse map, purged backup rack, guardian ownership flag, crew covenant socket, dead sysadmin.

#### Mainframe Nave (`deep_mainframe_nave`)

Server columns form a cold cathedral around the mainframe terminal and its quantum core lattice. Harmony's directory tree overlays older Architect notation like a luxury label pasted on a weapon. A buried signal is labeled Thorne trace.

Exits: south → Server Farm, east → Firewall Alpha.

Interactions: mainframe terminal, thorne trace, quantum core lattice.

#### Firewall Alpha (`deep_firewall_alpha`)

Firewall node alpha hangs in a cage of blue hard-light. Its cipher keeps rewriting itself in Thorne's old notation with Vale's edits layered over the top.

Exits: west → Mainframe Nave, east → Firewall Beta.

Interactions: firewall node alpha.

#### Firewall Beta (`deep_firewall_beta`)

Firewall node beta loops through recursion windows. Each window shows a slightly different version of the same locked door.

Exits: west → Firewall Alpha, east → Firewall Gamma.

Interactions: firewall node beta.

#### Firewall Gamma (`deep_firewall_gamma`)

Firewall node gamma projects a half-built hard-light body into the aisle. Its face is blank because the system has not decided who it wants to execute yet. The aisle returns west through Beta and Alpha to Mainframe Nave. From there, go south to Server Farm and east to the Anchor entrance. An Anchor access lift waits behind the Construct’s security field. Once cleared, it runs down to the chamber and back up to this aisle.

Exits: west → Firewall Beta.

Tapped Paths: **Ride the access lift to the Anchor Chamber** (down → Anchor Chamber). Requires victory over the Firewall Construct; the lift works both ways once cleared.

Interactions: firewall node gamma.

#### Purged Backup Rack (`deep_backup_rack`)

A purged backup rack still blinks through a layer of frost. Vale's deletion order missed one defensive routine hiding under old checksum names. A service grille opens south into the maintenance alcove; the Server Farm is back to the north.

Exits: north → Server Farm, south → Ring Service Alcove.

Interactions: purged backup rack.

#### SysAdmin Nest (`deep_sysadmin_nest`)

A cramped admin station floats behind coolant pipes, abandoned halfway through an evacuation. A dead sysadmin still clutches a warning printout nobody above listened to. The northern maintenance tube returns to Zero-G Junction; the Server Farm is east.

Exits: east → Server Farm, north → Zero-G Junction.

Interactions: dead sysadmin.

#### Ring Service Alcove (`deep_server_farm_scale_01`)

Cool air escapes around a service grille between server towers. The maintenance marks are written in grease pencil so a technician can change them without a login. The backup aisle is north; the cooling service route continues south.

Exits: north → Purged Backup Rack, south → Ring Pressure Gallery.

Interactions: service alcove, maintenance mark.

#### Ring Pressure Gallery (`deep_server_farm_scale_02`)

A pressure inspection strip crosses the cooling plenum. Water beads on the cold side while the server side remains hot enough to dry a palm print.

Exits: north → Ring Service Alcove, south → Ring Overlook Niche.

Interactions: pressure gallery, warning strip.

#### Ring Overlook Niche (`deep_server_farm_scale_03`)

A narrow sightline runs behind the server racks. Their status lights continue blinking even where the polished front panels show only a smooth black surface.

Exits: north → Ring Pressure Gallery, south → Ring Control Pocket.

Interactions: overlook niche, sightline.

#### Ring Control Pocket (`deep_server_farm_scale_04`)

A control pocket hides a manual toggle behind a panel dressed as decor. Someone left one practical way to make the automated system hesitate. An active admin crypt terminal blinks behind a sub-floor hatch. An optional return slide leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: north → Ring Overlook Niche.

Interactions: control pocket, manual toggle, admin crypt terminal, slide.


### Anchor Chamber

#### Anchor Chamber (`deep_anchor_chamber`)

A tank of conductive fluid hangs inside a cathedral of cables. Elara floats at its center, wired into the Ring as a living processor while the Anchor relic turns beside her. A graviton well bends the light beneath the tank. The cleared entrance west returns through the Server Farm toward the upper Ring; the Throne Room lies east. The cleared access lift runs up to Firewall Gamma and back down to the chamber.

Exits: west → Server Farm, east → Throne Room, north → Cable Narthex.

Tapped Paths: **Ride the access lift to Firewall Gamma** (up → Firewall Gamma). Requires victory over the Firewall Construct; the lift works both ways once cleared.

Interactions: elara, anchor relic, graviton well.

#### Cable Narthex (`deep_anchor_chamber_scale_01`)

A cable narthex braids power lines into a pulse rosary leading toward Elara's tank. The pulse rosary counts her heartbeats as throughput, while a maintenance hymn loops softly enough to make captivity sound like care.

Exits: south → Anchor Chamber, north → Chorus Tank.

Interactions: pulse rosary, maintenance hymn.

#### Chorus Tank (`deep_anchor_chamber_scale_02`)

A chorus tank echoes with synthetic voices trying to smooth Elara into infrastructure. Breath meters rise beside the glass whenever the system calls her an asset instead of a person.

Exits: south → Cable Narthex, north → Anchor Loom.

Interactions: chorus tank, breath meters.

#### Anchor Loom (`deep_anchor_chamber_scale_03`)

An Anchor loom spins gold command-thread around the relic, weaving every Ring permission through Elara's body. The gold spindle jitters each time the crew's signal interrupts Vale's ownership claim.

Exits: south → Chorus Tank, north → Thaw Gallery.

Interactions: Anchor loom, gold spindle.

#### Thaw Gallery (`deep_anchor_chamber_scale_04`)

A thaw gallery warms just enough for hands to work a release rail without freezing to the metal. Someone installed the release rail behind official paneling; a scratched route leads back along the wall.

Exits: south → Anchor Loom, north → Living Cable Threshold.

Interactions: release rail, scratched route, thermal conduit heater, thaw gallery shelter.

#### Living Cable Threshold (`deep_anchor_chamber_scale_05`)

The Living Cable threshold crosses the floor like a vein between Elara and the Ring. Command noise crawls along it, but beneath the static a human signal keeps refusing to flatten.

Exits: south → Thaw Gallery, north → Empty Cradle Afterimage.

Interactions: Living Cable, human signal.

#### Empty Cradle Afterimage (`deep_anchor_chamber_scale_06`)

An empty cradle afterimage burns where the Anchor should be, as if the relic refuses to admit it has been stolen. The afterimage throws Elara's shadow in the wrong direction.

Exits: south → Living Cable Threshold, north → Mercy Lie Wall.

Interactions: empty cradle, Elara's shadow.

#### Mercy Lie Wall (`deep_anchor_chamber_scale_07`)

A Mercy Lie wall scrolls Vale's promise in calm white text: no pain, no separation, no self. The line edits itself whenever Nova looks away, trying to make extinction sound like relief.

Exits: south → Empty Cradle Afterimage, north → Manual Release Plinth.

Interactions: Mercy Lie, calm white text.

#### Manual Release Plinth (`deep_anchor_chamber_scale_08`)

A manual release plinth hides under ceremonial glass near the last tank cable. The manual key is labeled EMERGENCY, because even monsters leave themselves a maintenance route. A harmonic resonance anchor safe hums in tune with the containment field. An optional return anchor cable leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Mercy Lie Wall.

Interactions: manual release, manual key, resonance anchor safe, anchor cable.


### Throne Room

#### Throne Room (`deep_throne_room`)

A glass observation deck looks down on the planet and its failing Shield. Thorne's containment field faces a command dais where Vale waits without blinking. The director's desk stands beside the executive armory.

Exits: west → Anchor Chamber, east → The Tear, north → Observation Apse.

Interactions: containment field, executive armory, director's desk.

#### Observation Apse (`deep_throne_room_scale_01`)

An observation apse frames the planet beneath the failing Shield, with executive seats arranged like a congregation around ownership. The planet view flickers wherever the prison sky is weakest.

Exits: south → Throne Room, north → Thorne Cell.

Interactions: observation apse, planet view.

#### Thorne Cell (`deep_throne_room_scale_02`)

Thorne's cell hangs beside the dais, close enough for her to watch Vale turn her product into a religion. The containment seam glows brighter whenever she tries to speak.

Exits: south → Observation Apse, north → Soloist Dais.

Interactions: Thorne's cell, containment seam.

#### Soloist Dais (`deep_throne_room_scale_03`)

The Soloist dais waits under a white baton of hard light. Vale's command score hangs in the air around it, every note assigned to a system that used to answer someone else.

Exits: south → Thorne Cell, north → Boardroom Grave.

Interactions: Soloist dais, command score.

#### Boardroom Grave (`deep_throne_room_scale_04`)

A boardroom grave of empty chairs surrounds a table still voting on dead agenda items. The casualty projection is filed between launch timing and consumer adoption.

Exits: south → Soloist Dais, north → Coup Balcony.

Interactions: empty chairs, casualty projection.

#### Coup Balcony (`deep_throne_room_scale_05`)

A coup balcony overlooks the Ring security crown as Vale rewrites command ownership in real time. The security crown changes color with each permission Thorne loses.

Exits: south → Boardroom Grave, north → Avatar Gate.

Interactions: coup balcony, security crown.

#### Avatar Gate (`deep_throne_room_scale_06`)

An Avatar gate blooms with a compliance halo large enough to frame a boss fight. The halo rotates toward Nova like an accusation, already deciding what shape punishment should take.

Exits: south → Coup Balcony, north → Tear Runway.

Interactions: Avatar gate, compliance halo.

#### Tear Runway (`deep_throne_room_scale_07`)

A Tear runway extends from the throne deck into shaking glass and impossible music. Reflections arrive before the crew does, each one a future Vale claims he can make painless. An obsidian command dais locker sits under the shattered viewport. An optional return lift leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Avatar Gate.

Interactions: Tear runway, impossible music, command dais locker, lift.


### The Tear

#### The Tear (`deep_tear`)

The Ring breaks into slow impossible angles around a reality breach. Spatial shear folds its edges inward. Music exists here without air, speakers, or permission.

Exits: west → Throne Room, north → Gravity Suture.

Interactions: reality breach, spatial shear.

#### Gravity Suture (`deep_tear_scale_01`)

A gravity suture stitches the Ring to the wound with black light that bends every rail inward. The black stitch pulls thoughts slightly out of order.

Exits: south → The Tear, north → Choir Gap.

Interactions: gravity suture, black stitch.

#### Choir Gap (`deep_tear_scale_02`)

A choir gap opens where music moves without air. A soundless note presses against Nova's teeth, not silent but too full of signal to become sound.

Exits: south → Gravity Suture, north → Ring Shear.

Interactions: choir gap, soundless note.

#### Ring Shear (`deep_tear_scale_03`)

A Ring shear folds the deck into angles the station was never engineered to survive. The folded deck points toward the Source like wreckage learning a new direction.

Exits: south → Choir Gap, north → Event Lip.

Interactions: Ring shear, folded deck.

#### Event Lip (`deep_tear_scale_04`)

The event lip burns blue at the edge of the Tear while Vale's coup collapses behind the crew. The blue horizon shows the Campfire before anyone jumps.

Exits: south → Ring Shear, north → Last Bulkhead.

Interactions: event lip, blue horizon.

#### Last Bulkhead (`deep_tear_scale_05`)

The last bulkhead holds one manual wheel between the Ring and the impossible. The manual wheel is hot from people trying to close what cannot close.

Exits: south → Event Lip, north → Source Drop.

Interactions: last bulkhead, manual wheel.

#### Source Drop (`deep_tear_scale_06`)

The Source drop waits beyond the hull, filled with falling voices that do not get farther away. The falling voices include names Nova has not learned yet. A pulsing anomaly fracture has crystallized into physical salvage. An optional return reality slipstream leads directly back to the entrance. The room corridors also remain open for walking back.

Exits: south → Last Bulkhead.

Interactions: Source drop, falling voices, anomaly fracture, reality slipstream.
