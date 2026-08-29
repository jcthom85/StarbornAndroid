# Deep Mine Asteroid Drill: Production Vertical Slice

## Product goal

The first Hyperion cabinet is a campaign-spanning restoration story, not a detached mini-game menu. Nova discovers a dead cabinet in the Pit mess hall, salvages its intact Hyperion logic board, restores it through the existing tinkering interaction, and later finds it installed in the Astra common room. The restored machine unlocks **Deep Mine Asteroid Drill**, a short skill game about controlling momentum, choosing risky ore deposits, and escaping before fuel runs dry.

The feature succeeds when the player feels that they rescued a small piece of pre-Dominion culture and made the Astra more like a home.

## Player flow

1. Inspect the broken cabinet in `pit_mess`.
2. A short authored discovery beat identifies its intact `hyperion_logic_board` and unlocks the repair schematic.
3. Repair **Deep Mine Cabinet Core** through tinkering using the protected logic board and inexpensive common components.
4. The repair completion marks the cabinet restored; Ollie handles transport once the Astra is available.
5. A cabinet action appears in `astra_common_room` and opens the game.
6. Runs are free after restoration. "Insert Coin" remains presentation only.
7. High scores and reward claims are stored in the campaign save.

The cabinet must never become permanently missable. Its logic board is unsellable, and the repair uses renewable common materials. Discovery before tinkering/Astra access is allowed and presented as a promise rather than an urgent objective.

## Restoration states

The canonical cabinet state is derived from campaign arcade progress:

- **Undiscovered**: broken cabinet can be inspected in the Pit.
- **Discovered**: repair schematic and protected logic board are available.
- **Repaired**: repair was completed and the cabinet is eligible for installation.
- **Installed**: playable in the Astra common room. Installation may be immediate when Astra access exists or resolved on the next Astra visit.

Transitions are idempotent. Repeated inspection, repair callbacks, save restoration, and reward submission cannot duplicate components or rewards.

## Game pillars

- **Momentum with readable consequences**: thrust changes velocity; it does not directly move the probe.
- **Greed versus safety**: richer deposits demand tighter approaches or hazardous detours.
- **Fast recovery**: restart is available immediately after a crash or fuel loss.
- **Short mastery arc**: a run targets 2–5 minutes, with bronze attainable after learning the controls and gold requiring consistent soft landings and efficient routing.
- **Fairness**: hazards are telegraphed, simulation is fixed-step, and seeded generation makes failures explainable and tests reproducible.

## First production ruleset

- Controls: left thruster, right thruster, main boost, and drill.
- The probe is affected by gravity and capped acceleration/velocity.
- Mineral pads require a low vertical speed, limited horizontal speed, and an upright approach.
- Holding drill while safely settled extracts ore and consumes fuel.
- Extraction fills a pad, increases score, and spawns the next route segment.
- Hard impact, terrain collision, or exhausted fuel ends the run.
- Soft consecutive landings build a multiplier; hard-but-survivable landings break the streak.
- Fuel pickups and stabilization assists are used sparingly to support recovery without removing mastery.

## Rewards

Reward tiers are cumulative and claimed once per save:

- Bronze: credits and common components.
- Silver: a validated, non-conflicting side-grade reward.
- Gold: a cabinet-themed mastery reward, cosmetic palette/trophy, and **Chief Driller** milestone/title presentation.

Final numeric thresholds and equipment effects must be tuned from playtest distributions. No arcade reward may be required for campaign combat progression or invalidate quest rewards.

## Technical shape

- Pure Kotlin fixed-step simulation with no Android or Compose dependencies.
- Compose Canvas renders immutable snapshots in a fixed logical viewport.
- Input is represented as held-button state plus edge-triggered actions.
- A shared arcade service owns discovery, repair completion, score submission, one-time grants, and milestone updates.
- `GameSessionState` and the protobuf save own cabinet progress. `UserSettingsStore` remains limited to player preferences.
- Navigation follows the existing fishing/tinkering activity pattern and returns safely to exploration.
- Audio uses `AudioCuePlayer`; settings for music, SFX, haptics, flashes, screen shake, contrast, and touch target size are honored.

## Quality gates

- Deterministic unit tests for physics, landing classification, drilling, score, and game-over.
- Save round-trip and backward-compatible default tests.
- Duplicate discovery, repair, and reward tests.
- Exploration integration tests for Pit and Astra action visibility.
- Lifecycle pause/resume does not advance simulation time.
- Stable frame pacing on representative hardware and no gameplay dependence on render FPS.
- Controls remain usable with large touch targets and common phone aspect ratios.
- High contrast and reduced-effects modes retain all gameplay information.
- A device playtest covers discovery -> repair -> installation -> play -> reward -> save/load.

## Selected visual direction and asset manifest

The production direction is a battered industrial Hyperion cabinet made for asteroid miners: oxidized green steel, amber lamps and phosphor, worn hazard stripes, miner-red controls, curved smoked glass, and cyan reserved for valuable ore. The CRT is dominant and dense with mine structure; modern flat app controls, glossy cyberpunk neon, and empty black space are explicitly out of scope.

Project assets:

- `docs/design/references/arcade/deep_mine_cabinet_concept_v1.png`: composition and material reference.
- `app/src/main/assets/images/arcade/deep_mine/mine_shaft_v1.webp`: optimized runtime CRT environment.
- `app/src/main/assets/images/arcade/deep_mine/mining_probe_sheet_v1.webp`: optimized transparent 4x2 runtime sprite sheet.
- `world_assets/src/main/assets/images/rooms/astra/common_room_arcade_v1.webp`: milestone-gated restored Astra room background.

The generated concept and assets use the following production prompt intents:

- **Cabinet concept**: portrait fictional pre-Dominion miner arcade cabinet; illustrated marquee; dominant curved CRT; compact mechanical control deck; battered steel, amber phosphor, oxidized green, hazard yellow, miner red, and restrained cyan ore; no modern app UI.
- **Mine shaft**: portrait hand-painted pixel-art asteroid shaft; layered rock, steel braces, cables, lamps, platforms, and ore seams; open readable central corridor; environment only.
- **Probe sheet**: consistent industrial mining probe across idle, lateral thrust, main boost, landing, drill, drilling, and damage states; transparent background; equal 4x2 cells.

These assets were generated with the built-in OpenAI image-generation workflow and copied into the workspace as project-bound artifacts.

## Scope boundary

This vertical slice establishes reusable arcade infrastructure but ships only Deep Mine Asteroid Drill. The other five cabinets should not be implemented until this cabinet passes the quality gates and playtest tuning confirms the restoration loop is compelling.
