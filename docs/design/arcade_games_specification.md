# Starborn Arcade Cabinets: Complete Mini-Game Design Specification

## 1. Executive Summary & Narrative Context

In the retro-futuristic world of **Starborn**, analog entertainment is a cherished relic of the pre-Dominion era. Scattered across the six campaign worlds are forgotten, damaged coin-op arcade cabinets originally manufactured by *Hyperion Amusements*. 

Players can discover these machines in regional hubs, repair them using the **Tinkering Workbench**, and have Ollie transport them back to the **Astra Recreation Bay**. Alongside the *Great Frontier Analog Tape Deck*, the Astra becomes a lively vintage arcade hub where players can compete for high scores, earn exclusive crafting recipes, weapon mods, and collector titles.

---

## 2. System Mechanics & The Astra Arcade Bay Loop

```mermaid
graph TD
    A[Explore World Hub / Bar] --> B[Discover Broken Arcade Cabinet]
    B --> C[Tinker Repair at Workbench]
    C --> D[Cabinet Delivered to Astra Recreation Bay]
    D --> E[Play Mini-Game in Retro CRT Viewport]
    E --> F{High Score Achieved?}
    F -->|Bronze| G[Credits & Crafting Components]
    F -->|Silver| H[Rare Recipe or Equipment Mod]
    F -->|Gold / Champion| I[Unique Title, Cosmetic CRT Palette & Milestone Trophy]
```

### Core Architecture Principles:
1. **Lightweight & Isolated**: Pure Kotlin state machines with high-performance 60 FPS Compose `Canvas` rendering. No heavy third-party game engines.
2. **Authentic Retro Presentation**: Custom virtual arcade cabinet bezel, CRT curvature/scanline shader overlays, tactile arcade button controls, and chiptune sound cues routed via `AudioCuePlayer`.
3. **Persisted High Scores**: High scores, coin credits, and reward claims persisted safely in `UserSettingsStore` / `SessionStore`.

---

## 3. The 6 World Arcade Cabinets

---

### Cabinet 1: *Deep Mine Asteroid Drill*
* **World**: World 1 (Mining Colony — The Pit)
* **Exact Room Location**: `pit_mess` (Title: *"Mess Hall"*) — sitting in the corner of the miner recreation cafeteria where Tyson and the workers take their breaks.
* **Astra Destination**: `astra_common_room` (Title: *"Astra Common Room"*) — Recreation Bay Arcade Corner.
* **Genre**: 2D Lunar Lander / Asteroid Thrust Miner
* **Theme**: Pilot a retro mining probe through crumbling mine shafts, managing fuel and thruster momentum to drill glittering ore crystals while avoiding falling slag stalactites.

#### Gameplay Rules:
* **Controls**: Left Thruster, Right Thruster, Main Boost, Drill Button.
* **Objective**: Land softly on designated mineral pads and hold the Drill button to extract high-value minerals before fuel expires.
* **Hazards**: Drifting space debris, laser barriers, falling slag boulders, gravity anomalies.
* **Scoring**: Fuel conservation bonus + Clean landing streak multiplier + Ore purity value.

#### Reward Tiers:
* **Bronze (5,000 pts)**: 500 Credits + 3x `scrap_metal` + 2x `wiring_bundle`.
* **Silver (15,000 pts)**: Weapon Mod: `recoil_dampener` (Reduces spread & recoil on rapid-fire weapons).
* **Gold / Champion (30,000 pts)**: Special Mod: `drill_bit_mod` (+15% Stagger damage against armored foes) + Milestone Title: *"Chief Driller"*.

---

### Cabinet 2: *Canopy Hopper*
* **World**: World 2 (Sector 9 Jungle & Swamp)
* **Exact Room Location**: `sector9_pod_interior` (Title: *"Escape Pod Interior"*) — salvaged from the crashed emergency survival pod electronics bay.
* **Astra Destination**: `astra_common_room` (Title: *"Astra Common Room"*) — Recreation Bay Arcade Corner.
* **Genre**: Frogger / River Raid / Timing Lane Jumper
* **Theme**: Guide a tiny bioluminescent bog hopper across treacherous swamp rivers, navigating rotating spore pads, dodging electric eels, and leaping past predatory vine traps.

#### Gameplay Rules:
* **Controls**: 4-Way D-Pad / Swipe lanes.
* **Objective**: Cross 5 distinct swamp flow layers to reach the ancient canopy nest before the poison fog closes in.
* **Hazards**: Submerging lilypads, speeding hover-skimmers, snapping razor-vines, electric shockwaves.
* **Power-Ups**: Glow Fireflies (Speed Boost & temporary shield), Golden Spore (+1,000 Bonus).

#### Reward Tiers:
* **Bronze (4,000 pts)**: 500 Credits + 3x `herb` + 2x `beast_meat`.
* **Silver (12,000 pts)**: Cooking Recipe: *Tideglass Delight* (Party-wide +15% Health regeneration for 3 battles).
* **Gold / Champion (25,000 pts)**: Special Tackle: `bioluminescent_lure` (Guarantees rare nocturnal fish spawns) + Milestone Title: *"Swamp Stalker"*.

---

### Cabinet 3: *Spire Infiltrator*
* **World**: World 3 (Ancient Spires & Upper City)
* **Exact Room Location**: `spire_exec_lounge_bar` (Title: *"Exec Lounge"*) — glowing behind the velvet curtain in the VIP lounge corner.
* **Astra Destination**: `astra_common_room` (Title: *"Astra Common Room"*) — Recreation Bay Arcade Corner.
* **Genre**: Cyberpunk Maze Infiltrator / Pac-Man ICE
* **Theme**: Infiltrate the Spire's high-security central mainframe. Guide a rogue data packet through glowing neon circuitry mazes, collecting unencrypted data fragments while evading patrolling ICE Sentinel programs.
* **Special Mechanics**: Snagging an **OVERCLOCK Node** triggers temporary system vulnerability, turning the ICE Sentinels vulnerable to dereferencing for massive combo multiplier points.

#### Gameplay Rules:
* **Controls**: 4-Way Directional D-Pad / Buffer Swiping.
* **Objective**: Clear all data nodes in the security sector while dodging 4 unique ICE Sentinel AI behaviors (Aggressive, Flanker, Ambusher, Drifter).
* **Power-Ups**: Overclock Node (Sentinel vulnerable), Cloak Pulse (Temporary invisibility), Buffer Flush (Clears local maze hazard).
* **Hazards**: Glitching corrupted walls, security laser gates, accelerated Sentinel overdrive states.

#### Reward Tiers:
* **Bronze (6,000 pts)**: 750 Credits + 2x `circuit_board` + 2x `nano_filament`.
* **Silver (18,000 pts)**: Ammo Accessory: `phase_rounds` (Ignores 25% of robotic enemy armor).
* **Gold / Champion (35,000 pts)**: Headgear Accessory: `cyber_visor` (+10% Critical Hit Chance for Gh0st) + Milestone Title: *"Master Infiltrator"*.

---

### Cabinet 4: *Slag Catcher*
* **World**: World 4 (The Foundry)
* **Exact Room Location**: `foundry_conditioning_observation` (Title: *"Observation Booth"*) — abandoned in the supervisory observation booth above the cooling vats.
* **Astra Destination**: `astra_common_room` (Title: *"Astra Common Room"*) — Recreation Bay Arcade Corner.
* **Genre**: Kaboom! / High-Speed Paddle Bucket Catcher
* **Theme**: Operate an emergency molten slag containment dolly beneath the malfunctioning Foundry crucible. Catch cooling white-hot metal ingots while deflecting volatile plasma bombs into cooling chutes.

#### Gameplay Rules:
* **Controls**: 1:1 Analog Horizontal Slider / Finger Tracking.
* **Objective**: Catch consecutive descending metal ingots with matching container buckets (Iron, Titanium, Plasma).
* **Multiplier**: Consecutive catches build the "Overheat Multiplier" (up to 8x). Dropping an ingot resets the multiplier and heats the floor.
* **Hazards**: Volatile explosive canisters (must be deflected with side bumper shields, not caught in buckets).

#### Reward Tiers:
* **Bronze (7,500 pts)**: 750 Credits + 2x `hydraulic_fluid` + 2x `heavy_gear`.
* **Silver (20,000 pts)**: Weapon Mod: `thermal_clip` (Adds Burn effect to standard attacks).
* **Gold / Champion (40,000 pts)**: Armor Mod: `foundry_aegis_mod` (+20 Fire Resistance & immune to Overheat status) + Milestone Title: *"Master Smelter"*.

---

### Cabinet 5: *Orbital Defense 2000*
* **World**: World 5 (Orbital Void Ring)
* **Exact Room Location**: `orbital_customs_lounge` (Title: *"Customs Lounge"*) — positioned by the starview bay window in the passenger concourse.
* **Astra Destination**: `astra_common_room` (Title: *"Astra Common Room"*) — Recreation Bay Arcade Corner.
* **Genre**: Fixed Top-Down Wave Shmup (Space Invaders / Galaga)
* **Theme**: Pilot the legendary starfighter *Astra Mark I* to defend the orbital station against swarms of rogue drone fleets and massive dreadnought flagships.

#### Gameplay Rules:
* **Controls**: Left / Right Movement + Rapid Fire + Smart Bomb.
* **Objective**: Clear waves of incoming enemy formations with varying attack vectors and swooping diving patterns.
* **Power-Ups**: Dual Spread Laser, Plasma Shield Drone, Overclock Rapid Fire, EMP Shockwave.
* **Boss Fights**: Every 5th wave spawns a multi-segment Dreadnought Cruiser with breakable shield generators.

#### Reward Tiers:
* **Bronze (10,000 pts)**: 1,000 Credits + 2x `power_cell` + 2x `source_resin`.
* **Silver (30,000 pts)**: Weapon Mod: `void_clip` (Converts weapon damage to Void Element).
* **Gold / Champion (60,000 pts)**: Crafting Blueprint: `singularity_beam_blueprint` (Unlocks Tier-4 Ultimate Weapon) + Milestone Title: *"Ace of the Void"*.

---

### Cabinet 6: *Harmonic Pulse*
* **World**: World 6 (The Source)
* **Exact Room Location**: `source_memory_fragments` (Title: *"World-Fracture Landing"*) — a surreal, floating pre-war memory manifestation crystallized on the landing platform.
* **Astra Destination**: `astra_common_room` (Title: *"Astra Common Room"*) — Recreation Bay Arcade Corner.
* **Genre**: Arcade Rhythm / Polyphonic Beat Matcher
* **Theme**: Harmonize with the crystalline pulse of the Source. Streamlines of harmonic resonance flow into 4 sacred rune conduits in sync with the celestial soundtrack. Tap and sustain notes on beat to weave cosmic melodies.

#### Gameplay Rules:
* **Controls**: 4 Harmonic Conduits (Lens, Anvil, Anchor, Key).
* **Objective**: Hit incoming harmonic notes with precise timing (PERFECT, GREAT, GOOD) to build the Resonance Groove Multiplier up to 10x.
* **Special Notes**: Sustained Beam Holds, Polyphonic Chords (simultaneous double taps), and Starburst Fever Mode.
* **Audio Feedback**: Dynamic procedural synthesizer layers trigger on successful notes, completing the song orchestration in real-time.

#### Reward Tiers:
* **Bronze (10,000 pts)**: 1,000 Credits + 3x `source_resin`.
* **Silver (25,000 pts)**: Accessory: `focus_conduit` (+15 Max MP / Tech Points).
* **Gold / Champion (50,000 pts)**: Vanity Accessory: `starborn_ribbon` (+10% all stats across party) + Milestone Title: *"Maestro of the Source"*.

---

## 4. Technical Architecture & File Structure

```
app/src/main/java/com/example/starborn/feature/arcade/
├── domain/
│   ├── ArcadeIds.kt                   # Cabinet IDs, recipes, items, and milestones
│   └── ArcadeService.kt               # Discovery, repair completion, high scores, reward claims
├── games/
│   ├── deepmine/                      # World 1: Deep Mine Asteroid Drill
│   │   └── DeepMineEngine.kt
│   ├── canopyhopper/                  # World 2: Canopy Hopper
│   │   └── CanopyHopperEngine.kt
│   ├── spireinfiltrator/              # World 3: Spire Infiltrator
│   │   └── SpireInfiltratorEngine.kt
│   ├── slagcatcher/                   # World 4: Slag Catcher
│   │   └── SlagCatcherEngine.kt
│   ├── orbitaldefense/                # World 5: Orbital Defense 2000
│   │   └── OrbitalDefenseEngine.kt
│   └── harmonicpulse/                 # World 6: Harmonic Pulse
│       └── HarmonicPulseEngine.kt
└── ui/
    ├── ArcadeCabinetHubScreen.kt      # Selection hub in Astra Common Room
    ├── DeepMineArcadeScreen.kt        # World 1 Screen & Canvas
    ├── CanopyHopperArcadeScreen.kt    # World 2 Screen & Canvas
    ├── SpireInfiltratorArcadeScreen.kt# World 3 Screen & Canvas
    ├── SlagCatcherArcadeScreen.kt     # World 4 Screen & Canvas
    ├── OrbitalDefenseArcadeScreen.kt  # World 5 Screen & Canvas
    └── HarmonicPulseArcadeScreen.kt   # World 6 Screen & Canvas
```

---

## 5. Milestones & Achievements Integration

The following milestones are registered:
* `ms_arcade_cabinet_01_repaired`: *"Deep Mine Restored"* (Repaired Cabinet 1).
* `ms_arcade_cabinet_02_repaired`: *"Canopy Hopper Restored"* (Repaired Cabinet 2).
* `ms_arcade_cabinet_03_repaired`: *"Spire Infiltrator Restored"* (Repaired Cabinet 3).
* `ms_arcade_cabinet_04_repaired`: *"Slag Catcher Restored"* (Repaired Cabinet 4).
* `ms_arcade_cabinet_05_repaired`: *"Orbital Defense Restored"* (Repaired Cabinet 5).
* `ms_arcade_cabinet_06_repaired`: *"Harmonic Pulse Restored"* (Repaired Cabinet 6).
* `ms_arcade_all_repaired`: *"Arcade Restoration Complete"* (Repaired all 6 cabinets).
* `ms_arcade_champion_all`: *"Hyperion Grand Master"* (Attained Gold Champion score on all 6 arcade machines).
