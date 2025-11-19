# Fishing Minigame Port Plan

## Goals
- Recreate the Python fishing loop (setup → wait → skill challenge → results) using Kotlin/Compose.
- Keep rods, lures, zones, and minigame tuning data-driven via `recipes_fishing.json`.
- Integrate with exploration so room actions can launch fishing and reward inventory items via existing services.

## Flow Overview (updated)
1. **Gear Setup**  
   - Player chooses rod + lure before any action. Rods advertise pull resistance; lures list what species/rarities they attract.  
   - Disable progression until at least one rod and lure are owned.
2. **Lure Bob / Bite Wait**  
   - Once the player hits “Cast,” show the lure bobbing while a random bite timer (min 3 s) runs.  
   - Light haptics fire occasionally to simulate nibbling; if the player backs out they return to setup.
3. **Hookset (Gyro or fallback)**  
   - When the bite window triggers, send a strong haptic cue and await a single upward device jerk detected via gyroscope.  
   - If gyro is unavailable/disabled, surface a “Set Hook” button overlay as fallback.
4. **Reel Phase**  
   - After a successful hookset, swap to a tap-to-reel meter. Each tap pulls the fish closer; the fish pattern applies pullback forces between taps.  
   - Better rods reduce pullback strength; lure bonuses can slightly weaken specific species. Failure (meter empties) loses the catch.
5. **Results Screen**  
   - When the fish reaches the player, show the catch panel (item, rarity text, flavor). Include “Fish Again” and “Return” CTAs.

## Data Model Mapping (additions)
| Python | Kotlin target | Notes |
|--------|---------------|-------|
| `recipes_fishing.json` | `assets/recipes_fishing.json` | Extend schema with lure targeting + fish behavior definitions. |
| Rod/Lure dict | `FishingRod`, `FishingLure` | Rods gain `stability` (resists pull); lures gain `attracts` list and optional per-zone rarity bonuses. |
| Zones list | `FishingZone` | Each catch entry links to a `behavior_id` describing pull pattern + strength. |
| `fishing_manager.py` | `FishingService` | Continue weighting by rarity, but also factor lure attraction and rod stability into pullback math. |
| `fish behavior patterns` | New `FishBehaviorDefinition` | Defines pull curve (wobble, burst, erratic), base strength, and stamina decay. |

### Schema extensions (draft)
```jsonc
{
  "lures": [
    {
      "id": "mystery_lure",
      "name": "Mystery Lure",
      "rarity_bonus": 10,
      "attracts": ["glowing_minnow", "star_squid"],
      "zone_bonus": { "coastal": 5 }
    }
  ],
  "fish_behaviors": {
    "wobble_easy": {
      "pattern": "sine",
      "base_pull": 0.4,
      "burst_pull": 0.0,
      "stamina": 12
    },
    "burst_hard": {
      "pattern": "burst",
      "base_pull": 0.8,
      "burst_pull": 1.2,
      "stamina": 20
    }
  },
  "zones": {
    "forest": [
      {
        "item": "Glowing Minnow",
        "weight": 5,
        "rarity": "rare",
        "behavior_id": "burst_hard"
      }
    ]
  }
}
```
Kotlin data additions:
- `FishingLure.attracts: List<String> = emptyList()` and `zoneBonuses: Map<String, Int> = emptyMap()`.
- `FishingCatchDefinition.behaviorId: String?`.
- `FishBehaviorDefinition(pattern: PatternType, basePull: Double, burstPull: Double, stamina: Double)`.
- `FishingRod.stability: Double = 1.0` (multiplier applied against pullback).

## Architecture (phase-aware)
- **Data**: `FishingAssetDataSource` now reads `fish_behaviors` and `lure_targets` sections alongside rods/lures/zones/minigame rules.
- **Domain**:  
  - `FishingService` still handles weighted catches and loot rolls, and now also returns a `FishBehaviorProfile` describing pull pattern + stamina once a catch is chosen.  
  - `FishingSessionState` (or view model state) tracks: setup gear, randomized bite timer, hookset timeout, reel progress, and fail reasons.  
  - `HapticsBridge` abstracts soft vs. strong vibration calls so wait/bite cues remain consistent across hardware.
- **UI / Feature**:  
  - `FishingViewModel` expands to four states: `Setup`, `WaitingForBite`, `Hookset`, `Reeling`, plus `Result`. It exposes flow events for haptics (nibble vs. bite) and gyroscope fallback actions.  
  - `FishingScreen` includes: gear picker, wait overlay with bobbing animation, hookset prompt (gyro + fallback button), tap-to-reel canvas with fish progress, and result card.  
  - Device-motion handler listens for upward jerk; when disabled it reveals a clearly labeled “Set Hook” CTA.

### ViewModel state machine
```
Setup
  | cast()
  v
WaitingForBite (timer >= 3s, random + lure bonus)
  | bite detected -> notifyHaptics(HARD), transition to Hookset
  v
Hookset (gyro window 1-1.5s)
  | success -> create FishBehaviorProfile + enter Reeling
  | timeout/fallback fail -> emit miss result
  v
Reeling
  | onTap -> advance progress; apply fish pull pattern tick
  | if progress <= 0 -> fail
  | if progress >= 100 -> success -> Result
Result -> Setup/exit
```

## Integration Hooks (service outputs)
- `FishingService.getCatchResult` now returns `FishingResult.behavior`, exposing the full `FishBehaviorDefinition` for the selected catch so the reel phase can read pull pattern, burst strength, and stamina in real time.
- Each `FishingRod` carries `stability`, allowing the reel UI to reduce incoming pullback based on equipped gear.
- `FishingLure.attracts` and `zoneBonuses` are available in the view model for UI copy (e.g., show “Highly effective in Coastal waters” tooltips).
- Upcoming minigame work should keep the state machine above and consume the new behavior/stability data rather than hard-coding fish physics.

## Open Questions / TODO
- Confirm loot balance against Python tables (rod power vs. rarity weights) and clamp edge cases where quantity spikes.
- Wire XP/tutorial hooks through `EventManager` so success/failure triggers authored guidance.
- Layer ambience, splash FX, and haptics once AudioRouter supports multi-track fades.
- Localise on-screen copy and surface gear stats via string resources.

## Remaining Enhancements
1. Mirror Python catch weighting & rarity tables, including zone-specific failure rewards.  
2. Author failure tutorials for cooking/first-aid stations that reference the fishing cadence.  
3. Integrate ambience/music cues and VO playback via `AudioRouter` once layering lands.  
4. Extend JVM tests to cover lure rarity weighting plus UI instrumentation smoke for the fishing route.  
5. Add analytics hooks (timing, success ratio) to aid future tuning.  
