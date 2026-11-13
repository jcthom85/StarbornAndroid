# Proposed world rosters

Implementation update (November 13): the World 1 Warden and World 2 mechanics are implemented locally with temporary art. See [current progress](PROGRESS.md); other redesigns and final art remain proposed. The original design below remains the reference.
Status: design candidate. Every behavior and visual brief below is proposed unless separately verified in production. Existing IDs are retained where practical; a retained ID does not mean its new signature is already implemented. Current stats, skills and bindings are recorded in BASELINE.json.

## Scope and recurrence

The candidate assigns 69 distinct combat subjects to six worlds: all 48 current definitions and 21 proposed new definitions. A kind gets one unique approved visual identity; repeated instances of that kind can share it. Existing suitable artwork is reviewed and retained rather than regenerated automatically. Numbers are design scope, not a content quota.

World 2 receives Moss Borer and Mist Glider instead of borrowing the exact Pit definitions. World 5 receives Threshold Remnant instead of using World 6's Source Shadow. Preserve these family relationships in anatomy/materials while giving them different silhouettes, behaviors, IDs and reward budgets. Cross-world ID migration must be room-scoped; never globally rename Echo-Borer or Source Shadow.

Riot Guard already exists in the catalog but is not present in the current campaign encounter sources. This proposal gives it a specific Spire squad role. The duplicate Avatar and rescue elites in side rooms are replaced with distinct kinds rather than duplicated boss identities.

## World 1 — The Pit (8 kinds)

| Enemy / ID | Role and signature | Accessible response | Visual identity |
| --- | --- | --- | --- |
| **Faulted Loader** (`faulted_loader`; existing) | tutorial striker. Two safe reads then an exposed relay; preserve the reduced opening instance. | Use the taught electrical opening; basic attacks remain viable. | Asymmetric cargo fork, split relay, compact yellow loader. |
| **Echo-Borer** (`echo_borer`; existing) | burrowing pack. One borer braces while its partner emerges; bounded stagger rather than chained lost turns. | Choose exposed target or spend area damage when both emerge. | Segmented slate burrower with broad drill snout. |
| **Siren Skimmer** (`siren_skimmer`; existing) | volatile harrier. Visible throat inflation precedes a sonic burst. | Interrupt the inflation or guard the burst; cold is an earned shortcut. | Wide manta wings and an oversized luminous throat sac. |
| **Dominion Dampener** (`dominion_dampener`; existing) | suppression rifleman. Marks a target before a limited suppression volley. | Disrupt the mark, break the shooter, or defend the marked ally. | Low industrial armor, long baffled suppression rifle. |
| **Acoustic Bulwark** (`acoustic_bulwark`; existing) | shield defender / drill elite. Declared bracing protects an ally for one cycle; isolated drill remains readable. | Guard Break or physical pressure during the recovery; no optional reward required. | Broad acoustic shield disk, squat planted legs. |
| **Resonance Buoy** (`resonance_buoy`; existing) | capacitor support. Stores one visible charge and boosts a partner; no hidden summoning claim. | Break the charge or remove the supported attacker. | Small tethered bell-shaped capacitor with two exposed coils. |
| **Pressure Hauler** (`pressure_hauler`; existing) | optional pursuit elite. A slow pressure build precedes a heavy shove and venting recovery. | Hear/read the patrol notice, choose engagement, punish venting. | Tracked pressure tank with huge valve wheel; distinguish from Loader. |
| **The Iron Warden** (`the_iron_warden`; existing) | solo boss. Shield discipline becomes a finite emergency overclock. | Read commitment and exploit recovery; see BOSS_DESIGNS. | Human authority inside tall riot power armor, shield and restrained silhouette. |

Related encounter locations and introduction order: [Encounter route](ENCOUNTER_ROUTE.md). Boss details and complete formations: [Boss designs](BOSS_DESIGNS.md).

## World 2 — Sector 9 (9 kinds)

| Enemy / ID | Role and signature | Accessible response | Visual identity |
| --- | --- | --- | --- |
| **Shard-Hound** (`shard_hound`; existing) | pack striker. One hound marks prey and the other telegraphs a pounce. | Interrupt the caller or guard the marked target; no permanent focus-fire loop. | Faceted mineral antlers over a lean quadruped body. |
| **Spore-Spitter** (`spore_spitter`; existing) | biological controller. Swells a conspicuous spore bulb before a bounded poison burst. | Interrupt or use defense and accessible antidote/recovery. | Rooted pitcher-shaped organism, clustered amber spore sacs. |
| **Ruin-Guardian** (`ruin_guardian`; existing) | reliquary elite. A stone ward absorbs one attack cycle before its joints brighten. | Strike the visible unwarded joints or break its stance. | Heavy carved quadruped masonry with floating keystone ribs. |
| **Razor-Vine** (`razor_vine`; existing) | short hazard fight. A visible tendril coils before it binds one target; binding has a finite duration. | Cut the coil or interrupt it with a basic attack; burning is optional. | Low thorn fan with a single large coiling tendril. |
| **Stalker-Vine** (`stalker_vine`; existing) | symbiotic elite. Anchors itself to shelter a spitter, then spends a finite root reserve on recovery. | Attack the partner or interrupt rooting; finite healing prevents stalemate. | Tall walking mangrove roots around a translucent seed heart. |
| **Sentinel Orb** (`sentinel_orb`; existing) | Architect sentry. Its iris color and shape announce charge/discharge, not a guessed resistance table. | Read charge and break its exposed iris; preserve wiring/battery supply. | Ancient three-ring iris orb with weathered stone inlays. |
| **The Source Beast** (`the_beast`; existing) | solo creature boss. Measured hunting becomes an exhausted, dangerous frenzy. | Read lunges and recovery; see BOSS_DESIGNS. | One enormous living apex creature; scarred organic mass, no borrowed armor. |
| **Moss Borer** (`moss_borer`; new) | cavern pair. A moss shield opens when the creature exhales a visible mineral cloud. | Choose exposed partner; cold/physical timing remain useful. | Broad rooted beetle with moss sail plates; visual family connection to Pit burrowers, new art. |
| **Mist Glider** (`mist_glider`; new) | wetland harrier. Draws a visible mist membrane before a resonant dive. | Interrupt or guard the dive; mist drops after commitment. | Long reed-like flying fish with translucent fan fins; related ecology, unique silhouette. |

Related encounter locations and introduction order: [Encounter route](ENCOUNTER_ROUTE.md). Boss details and complete formations: [Boss designs](BOSS_DESIGNS.md).

## World 3 — The Spire (9 kinds)

| Enemy / ID | Role and signature | Accessible response | Visual identity |
| --- | --- | --- | --- |
| **Sewer Crawler** (`sewer_crawler`; existing) | urban scavenger. Acid feeding leaves the next lunge briefly exposed. | Exploit the feeding pause or use an area attack against a pair. | Low many-legged pipe scavenger with a translucent waste reservoir. |
| **Riot Guard** (`riot_guard`; existing) | formation defender. Braces to shelter a drone, then commits to a baton rush. | Break the brace or exploit the drone while the guard commits. | Human in narrow city armor with a rectangular transparent shield. |
| **Sentinel Mk. I** (`sentinel_mki`; existing) | optional quarantine elite. A quarantine field rotates between disruption and recovery. | Read its field cycle and interrupt; always preserve Neon Sign Core. | Suspended medical/quarantine pod with radial restraint fins. |
| **Aero Drone** (`aero_drone`; existing) | mobile disruption. Targeting light precedes stun pulse; evasive maneuver has a visible end. | Interrupt targeting or hit after the dodge; no unbounded stun chain. | Swept compact wings, white chassis, cyan sensors; existing dedicated art candidate. |
| **Corporate Assassin** (`corporate_assassin`; existing) | priority striker elite. Marks a victim then commits to a lunge; smoke cannot make it untargetable forever. | Defend the marked ally or interrupt the setup; Scan offers clarity, not sole access. | Tailored human silhouette, folded blade, distinct smoke emitter. |
| **Heavy Mech** (`heavy_mech`; existing) | armored platform elite. A lateral heat sweep is followed by exposed stabilizers. | Break the brace or punish stabilization; no overlap with an instant drone stun. | Low armored riot chassis, broad heat nozzle and stabilizer feet. |
| **Administrator** (`administrator_boss`; existing) | reinforcement boss. Finite drone reserves and command windows. | Choose command pressure versus drone control; see BOSS_DESIGNS. | Large suspended concierge intelligence with a smiling porcelain display. |
| **Concierge Relay** (`concierge_relay`; new) | city support terminal. Broadcasts a single visible shield or targeting order, then must recharge. | Interrupt the order or pressure its recipient; no infinite healing. | Floating brass desk bell with an unfolded holo-fan and three cable tassels. |
| **Interdictor Drone** (`administrator_interdictor`; new) | boss reserve drone. One issued interception command before its fragile emitter cools. | Remove the emitter or interrupt deployment; zero independent loot/XP. | Vertical clamp-shaped drone with bright central stop-light lens. |

Related encounter locations and introduction order: [Encounter route](ENCOUNTER_ROUTE.md). Boss details and complete formations: [Boss designs](BOSS_DESIGNS.md).

## World 4 — The Foundry (14 kinds)

| Enemy / ID | Role and signature | Accessible response | Visual identity |
| --- | --- | --- | --- |
| **Magma Drone** (`magma_drone`; existing) | heat harrier. One heat charge announces a burst and opens its coolant vanes. | Exploit cooling or interrupt the charge; retain first crossing gate. | Triangular heat-shielded skimmer with red-hot underside and blue coolant fins. |
| **Slag Golem** (`slag_golem`; existing) | cooling-window elite. Keep Molten Slam followed by exposed cooling; add a reliable tell before impact. | Guard or break the slam and punish cooling. | Bipedal slag crust around a glowing furnace belly, distinct from every loader. |
| **Welder Bot** (`welder_bot`; existing) | finite repair support. Field Weld has visible finite charges and a cooldown. | Interrupt the weld or kill its beneficiary; preserve limited repair supplies. | Narrow tripod, welding torch arm, dangling finite weld cartridges. |
| **Flame Trooper** (`flame_trooper`; existing) | armored furnace infantry. Pressure gauge and stance announce the cone; armor locks restrict its next action. | Break the setup or guard the cone, then punish the locked stance. | Insulated industrial human suit with a horizontal furnace shield. |
| **Phantom** (`phantom_prototype`; existing) | incomplete assassin elite. A half-cloak flickers before a committed Source slash. | Interrupt exposed hardware or strike after the slash; no permanent evasion. | Open skeletal stealth machine, incomplete face, asymmetric cable limbs. |
| **Conveyor Crusher** (`conveyor_crusher`; existing) | rhythm hazard. Three clearly shown press positions precede one committed slam. | Prepare on the warning beat and punish the raised press; no mandatory reflex input. | Overhead stamping press on a compact conveyor frame. |
| **Titan Walker** (`titan_walker_boss`; existing) | multipart core boss. Four distinct detachable weapon systems share a machine action budget. | Choose which threat to remove; see BOSS_DESIGNS. | Huge suspended siege chassis with clear sockets for four separate systems. |
| **Crucible Champion** (`crucible_champion`; existing) | optional arena duelist boss. Alternating heavy commitments and short feints; predictable recovery. | Read the commitment rather than repeat the Titan component solution. | Low crucible-alloy prizefighter frame with hammer fists and victory dents. |
| **Magnet Sifter** (`magnet_sifter`; new) | industrial controller. Magnetizes a named target before an allied physical strike. | Interrupt the field or defend; bounded mark cannot stack into lockout. | Horseshoe magnetic head over a short wheeled sorting bin. |
| **Quench Mite** (`quench_mite`; new) | coolant scavenger. Steals ambient heat to swell a coolant sac, then vents it. | Interrupt the swelling or exploit the empty sac; avoids another Burn-only enemy. | Pale six-legged boiler parasite with oversized blue fluid abdomen. |
| **Missile Battery** (`titan_missile_battery`; new) | Titan component. Queued area salvo; its tell persists until resolved. | Remove sustained group pressure. | Wide twin missile pod, top-left socket. |
| **Cutting Arm** (`titan_cutting_arm`; new) | Titan component. Declared heavy single-target cut and recovery. | Remove the most dangerous committed strike. | Long articulated saw arm, lower-left socket. |
| **Anchor Cannon** (`titan_anchor_cannon`; new) | Titan component. Marks a target for bounded disruption before a shot. | Remove the control threat. | Short heavy grapnel barrel with cable spool, lower-right socket. |
| **Shield Projector** (`titan_shield_projector`; new) | Titan component. Gives the chassis finite protection with a visible recharge. | Shorten the core fight; core never becomes absolutely immune. | Tall forked emitter with a flat shield lens, top-right socket. |

Related encounter locations and introduction order: [Encounter route](ENCOUNTER_ROUTE.md). Boss details and complete formations: [Boss designs](BOSS_DESIGNS.md).

## World 5 — The Ring (14 kinds)

| Enemy / ID | Role and signature | Accessible response | Visual identity |
| --- | --- | --- | --- |
| **Compliance Fighter** (`orbital_fighter`; existing) | paired interception striker. One craft designates while the other commits a missile pass. | Break designation, defend or interrupt; first paired screen must remain fair on arrival. | Thin orbital interceptor, separated thruster pods, no Aero Drone wing reuse. |
| **Compliance Officer** (`compliance_officer`; existing) | control squad specialist. Issues one explicit detention mark; a partner can exploit it next cycle. | Interrupt command or defend/cleanse the marked target. | Long ivory uniform coat and compact scanner baton, no Riot Guard body. |
| **Null-G Drone** (`null_g_drone`; existing) | gravity support. Projects one readable gravity tether that strengthens a partner attack. | Break the tether unit or guard the announced follow-up. | Compact gyro rings, clamp arms, maneuvering thrusters; new dedicated art candidate. |
| **Void Turret** (`void_turret`; existing) | fixed pressure gun. A charging aperture announces a narrow beam and venting downtime. | Interrupt/guard and hit its open housing. | Wall-mounted iris cannon with a broad horizontal anchoring bracket. |
| **Hunter-Killer Droid** (`hk_droid`; existing) | scripted pursuit elite. One recurring hunter; Stasis is a taught route to disabling pursuit. | Use the earned Anchor ability or withdraw safely; not a normal damage sponge. | Tall reverse-jointed hunting frame with a rotating capture cage. |
| **Elite Guard** (`elite_guard`; existing) | reflector elite. A shield explicitly reflects a limited ranged category, then drops for a strike. | Use physical/melee/area options available to the crew; announce reflection before punishment. | Towering ceremonial shield bearer, narrow gold crest and segmented mirror shield. |
| **Firewall Construct** (`firewall_construct`; existing) | frequency-switch elite. A shown frequency changes only on a declared cycle, never immediately after input. | Scan or observe its tell; neutral damage always remains useful. | Angular lattice crab of hard-light panels and a central triangular aperture. |
| **Compliance Avatar** (`compliance_avatar`; existing) | coordinated boss body. Command link and broken-link modes form a duet with the Conductor. | Disrupt coordination or pressure the body; see BOSS_DESIGNS. | Large ceremonial hard-light body, cathedral ribs, no Warden silhouette. |
| **Customs Screener** (`customs_screener`; new) | standard scanning sentry. Marks one crew member but has little direct damage. | Interrupt its scan or remove the paired attacker. | Narrow walking checkpoint arch with two articulated scanner paddles. |
| **Tether Rigger** (`tether_rigger`; new) | standard capture technician. Readies a visible tether before restricting a target for a bounded period. | Break its preparation or defend; no hardlock or mandatory rare cleanse. | Low three-legged maintenance chassis with a large cable spool and hooked boom. |
| **Prism Surveyor** (`prism_surveyor`; new) | standard targeting support. Aligns one beam guide that amplifies a partner volley for one cycle. | Break alignment or interrupt the volley; not the elite Firewall frequency puzzle. | Small floating diamond lantern inside a wide three-arm measuring caliper. |
| **Boardroom Bailiff** (`boardroom_bailiff`; new) | optional named elite. Displays a charge, then executes a visible sentence; a broken seal creates recovery. | Interrupt a seal or guard the sentence, then exploit recovery. | Empty ceremonial judicial armor with a huge square seal stamp, no Avatar body. |
| **Threshold Remnant** (`threshold_remnant`; new) | breach foreshadow striker. A half-formed fracture attack fails into a long exposed recovery. | Read the incomplete attack; introductory precursor to Source creatures. | Incomplete glass ribbon organism caught inside an orbital airlock-shaped outline. |
| **Directive Conductor** (`directive_conductor`; new) | boss command partner. Relays the Avatar link, with its own charge and shutdown response. | Cut coordination or exploit body windows; no infinite resummoning. | Floating angular halo of ordered command tiles, clearly separate from the Avatar body. |

Related encounter locations and introduction order: [Encounter route](ENCOUNTER_ROUTE.md). Boss details and complete formations: [Boss designs](BOSS_DESIGNS.md).

## World 6 — The Source (15 kinds)

| Enemy / ID | Role and signature | Accessible response | Visual identity |
| --- | --- | --- | --- |
| **The Manager** (`manager_projection`; existing) | Zeke rescue elite. One performance demand followed by an interruptible judgment. | Nova basic pressure/Guard/Break can answer before Zeke is rescued. | Floating office torso made of stacked forms, long pointing arms, hollow face. |
| **War Echo** (`endless_war_echo`; existing) | Gh0st rescue pair. The two echo soldiers alternate designation and execution instead of attacking at once. | Interrupt designation or defend; never require Gh0st to rescue Gh0st. | Split silhouette of ceremonial gunner and torn orders; one enemy kind, two instances. |
| **Silent Shore** (`silent_shore_wraith`; existing) | Orion rescue elite. A tide visibly gathers before a silence wave, followed by a listening opening. | Interrupt/Guard/basic pressure; Orion-only abilities cannot be required to rescue him. | Tall empty cloak with a crescent wave cavity instead of a face. |
| **Source Shadow** (`source_shadow`; existing) | fracture striker. Its silhouette compresses before a short committed slash. | Interrupt or exploit its recovery; useful basic-action target in mixed parties. | Offset black paper-like limbs surrounding a bright missing center. |
| **Warped Sentinel** (`distorted_sentinel`; existing) | fracture disruptor. A fractured reticle shows who will receive the next glitch bolt. | Break the reticle or guard its target. | Asymmetric levitating prism with broken orbit segments; no existing drone repaint. |
| **Glitch Hound** (`glitch_hound`; existing) | fast pack threat. Flickering footprint sequence precedes a pounce; brief recovery follows. | Guard marked prey or interrupt the approach. | Ribbon-legged quadruped with missing geometric body slices; no Borer shape. |
| **Memory Leak** (`memory_leak`; existing) | fragile memory controller. Copies one announced pressure effect with a finite echo buffer. | Remove quickly or interrupt the copy; not another long elite HP wall. | Floating spooling film and translucent memory panels around a small vulnerable knot. |
| **Nightmare Guard** (`nightmare_guard`; existing) | memory defender elite. Shelters the Leak until it commits to an accusatory strike. | Break shelter or use an available area response. | Human-shaped folded shields with shifting empty portrait frames. |
| **The Ascended: Soloist** (`ascended_vale`; existing) | final boss human form. Precise limited commands with explicit counterwindows. | Read commands rather than rely on one debuff; see BOSS_DESIGNS. | Calm human Vale, immaculate asymmetric coat, one controlled gesture. |
| **The Ascended: Silence** (`ascended_god`; existing) | final boss transformed form. Two distinct manifestations and a final committed silence cycle. | Remove a pressure source or attack the exposed form; see BOSS_DESIGNS. | Huge fractured porcelain figure unfolding into impossible rings, not Iron Warden art. |
| **Review Clerk** (`review_clerk`; new) | nightmare standard. Stamps a visible performance mark that another enemy can exploit. | Interrupt or defend; no rescue credit. | Small ambulatory filing cabinet with paper legs and a heavy stamp arm. |
| **War Order Fragment** (`war_order_fragment`; new) | nightmare standard. Repeats one military order before dissolving into a missed shot. | Interrupt the order or strike during the miss; no Gh0st rescue credit. | Floating broken rifle wrapped in strips of command paper, no soldier body. |
| **Tide Murmur** (`tide_murmur`; new) | nightmare standard. A low wave outline announces a bounded disruption pulse. | Interrupt the forming wave or guard; no Orion rescue credit. | Small shell-shaped absence filled with a moving water ribbon. |
| **Voice Anchor** (`silence_voice_anchor`; new) | final manifestation. Charges the silence command; removal reduces command pressure. | Can be destroyed or outlasted through a guarded opening; no independent reward. | Tall split tuning-arch with a clearly visible throat aperture. |
| **Memory Anchor** (`silence_memory_anchor`; new) | final manifestation. Charges the memory-erasure pulse on the shared boss schedule. | Destroy it to reduce memory pressure or attack the body; no independent reward. | Wide suspended broken memory lens, horizontal silhouette distinct from Voice Anchor. |

Related encounter locations and introduction order: [Encounter route](ENCOUNTER_ROUTE.md). Boss details and complete formations: [Boss designs](BOSS_DESIGNS.md).

## Art review and delivery

The structured roster records current art bindings and a decision for each subject. The seven approved World 1 reference designs and the dedicated Aero Drone, Riot Guard and Null-G Drone are review candidates, not an instruction to replace good art. Several canonical files currently have multiple users: retain a good image for its intended subject, then replace the other bindings with their own art. All active enemy kinds must ultimately have distinct approved image content; copying the same bitmap into differently named files does not satisfy this.

For multipart and transformed bosses, approve the assembled composition before final pieces. Voice and Memory anchors must have different silhouettes; the four Titan systems must be distinguishable without reading every label. Reserve Interdictors are a distinct kind, not invisible damage packets.

No final image prompts or new assets are commissioned in this design packet. Behavior, silhouette, pose, scale, component anchors and state treatments are the inputs to the later art batch.
