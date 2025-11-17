# Fishing Minigame Port Plan

## Goals
- Recreate the Python fishing loop (setup → wait → skill challenge → results) using Kotlin/Compose.
- Keep rods, lures, zones, and minigame tuning data-driven via `recipes_fishing.json`.
- Integrate with exploration so room actions can launch fishing and reward inventory items via existing services.

## Flow Overview
1. **Setup Screen**  
   - Present rod + lure inventory, using metadata from fishing JSON and player inventory quantities.  
   - Display stat hints (rod power, lure rarity bonus), surface failure reasons (no rods/lures owned).
2. **Engagement / Idle Overlay**  
   - Optional short delay or ambience before minigame begins (mirror “wait popup” in Kivy).
3. **Timing Minigame**  
   - Cursor sweeps across a meter; player taps within the highlighted zone.  
   - Difficulty + speed derive from JSON `minigame_rules`, zone can override defaults.  
   - Emit graded result (perfect / success / fail) to FishingService.
4. **Results Screen**  
   - Show item caught, rarity, quantity, flavor text, XP/credits hooks (future).  
   - Provide “Keep Fishing” (loop) and “Return to Explore”.

## Data Model Mapping
| Python | Kotlin target |
|--------|---------------|
| `recipes_fishing.json` | `assets/recipes_fishing.json` with Moshi adapters |
| Rod/Lure dict | `FishingRod`, `FishingLure` data classes |
| Zones list | `FishingZone` with `FishingCatchDefinition` |
| `FishingManager` | `FishingService` (random catch selection, rarity weighting) |
| `CatchResult` | `FishingResult` (name, rarity, quantity, flavor) |

## Architecture
- **Data**: `FishingAssetDataSource` reads rods, lures, zones, minigame rules.
- **Domain**:  
  - `FishingService` handles rod/lure availability, minigame configuration, weighted catch resolution.  
  - `FishingSessionState` tracks selected gear, challenge timing, and result.  
  - `FishingRuntimeManager` (future) can queue ambience FX and manage retries.
- **UI / Feature**:  
  - `FishingViewModel`: orchestrates setup → minigame → result, exposes `StateFlow<FishingUiState>`.  
  - `FishingScreen`: Compose implementation with three states (SetupCard, MinigameScaffold, ResultCard).  
  - Emits navigation callbacks (`onFinish`, `onRetry`, `onPlayFx`) for host screen.

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
