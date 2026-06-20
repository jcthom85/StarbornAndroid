# Starborn Antigravity CLI Handoff

Last updated: 2026-06-20

This is the authoritative takeover document. Read it before `task.md`, then use the story and production guides listed below for the current workstream. The older `codex_handoff.md`, `implementation_plan.md`, and `walkthrough.md` describe earlier World 3 work and are retained as historical implementation context.

## Current State

- The data-driven Android campaign is implemented through the World 6 ending and credits.
- World 3 Upper City, both World 4 hubs, both World 5 hubs, and both World 6 hubs have quests, events, dialogue, rooms, items, enemies, skills, milestones, and critical-flow tests.
- Persistent ending milestones include `ms_game_complete` and `ms_credits_seen`.
- Critical-flow coverage now runs from `Hub1CriticalFlowTest.kt` through `Hub10CriticalFlowTest.kt`.
- The last full verification run passed: `:app:testDebugUnitTest` and `:app:runAssetIntegrity`, with 157 tests, 0 failures, 140 rooms, 19 NPCs, and 0 duplicate availability warnings.
- World 3 through World 6 custom room backgrounds are generated, installed, and referenced by `app/src/main/assets/rooms.json`.
- The working tree intentionally contains substantial uncommitted campaign and asset work. Do not reset, clean, overwrite, or revert files merely because they are modified or untracked.

## Completed Campaign Blocks

- World 3 Hub 2, Upper City: `w3_mq13` through `w3_mq15`, `w3_sq14` through `w3_sq15`, and `Hub4CriticalFlowTest.kt`.
- World 4 Hub 1, Slag Pits: `w4_mq16` through `w4_mq17`, `w4_sq16` through `w4_sq18`, and `Hub5CriticalFlowTest.kt`.
- World 4 Hub 2, Assembly Line: `w4_mq18` through `w4_mq20`, `w4_sq19` through `w4_sq20`, and `Hub6CriticalFlowTest.kt`.
- World 5 Hub 1, Orbital Ring: `w5_mq21` through `w5_mq22`, `w5_sq21` through `w5_sq23`, and `Hub7CriticalFlowTest.kt`.
- World 5 Hub 2, Deep Ring: `w5_mq23` through `w5_mq25`, `w5_sq24` through `w5_sq25`, and `Hub8CriticalFlowTest.kt`.
- World 6 Hub 1, Event Horizon: `w6_mq26` through `w6_mq28`, `w6_sq26` through `w6_sq28`, and `Hub9CriticalFlowTest.kt`.
- World 6 Hub 2, Singularity: `w6_mq29` through `w6_mq30`, `w6_sq29` through `w6_sq30`, final bosses, ending cinematics, credits, and `Hub10CriticalFlowTest.kt`.

## Installed Late-Game Room Art

World 3 assets are under `world_assets/src/main/assets/images/rooms/world_3/`; all 12 Spire rooms now use bespoke 1088x1920 backgrounds.

World 4 assets are under `world_assets/src/main/assets/images/rooms/world_4/`; all 10 Foundry rooms now use bespoke 1088x1920 backgrounds.

Late-game navigation art is complete: hubs 5 through 12 use eight bespoke 1088x1920 map backgrounds under `world_assets/src/main/assets/images/hubs/`, and all 39 World 3 through World 6 destinations use bespoke 1024x1024 transparent node miniatures under `world_assets/src/main/assets/images/nodes/world_3/` through `world_6/`.

World 5 assets are under `world_assets/src/main/assets/images/rooms/world_5/`:

`orbital_executive_dock.png`, `orbital_grand_concourse.png`, `orbital_solarium.png`, `orbital_security_hub.png`, `orbital_service_shaft.png`, `orbital_server_farm.png`, `deep_anchor_chamber.png`, `deep_throne_room.png`, and `deep_tear.png`.

World 6 assets are under `world_assets/src/main/assets/images/rooms/world_6/`:

`source_campfire.png`, `source_zeke_nightmare.png`, `source_gh0st_nightmare.png`, `source_orion_nightmare.png`, `source_echo_mines.png`, `source_echo_elevator.png`, `source_memory_bridge.png`, `source_memory_stair.png`, `source_spire_thought.png`, `source_center.png`, and `source_new_world.png`.

These were generated as 1088x1920 PNG files with `gpt-image-2` at low quality, visually inspected, copied into `world_assets`, and wired in `rooms.json`.

## Recommended Next Work

1. Audit and create missing NPC portraits and enemy combat sprites, then run the dialogue-emote validator.
2. Audit music and sound-effect references, generate required audio with ElevenLabs, install it in the established asset locations, and validate every reference.
3. Run a full campaign playtest and balance pass from a clean save, followed by the complete automated regression suite.

The portrait/sprite audit is complete. Nine NPC identities still borrow generic or unrelated art: `the_warden`, `jax`, `mika`, `curator`, `lab_terminal`, `thorne`, `maintenance_bot`, `elara`, and `vale`. Seven enemy image references are missing on disk: `core_drill_behemoth_combat.png`, `mutated_crawler_combat.png`, `sentinel_droid_combat.png`, `aero_drone_combat.png`, `sentinel_orb_combat.png`, `ruin_guardian_combat.png`, and `phantom_assassin_combat.png`. Treat this list as the next bounded production manifest.

## Required Reading Order

1. `antigravity_handoff.md` - current status and next work.
2. `task.md` - completion checklist and remaining production work.
3. `data/assistant_briefing.md` - repository architecture and operating context.
4. `docs/story/Starborn_Art_Production_Guide.md` - mandatory room-art workflow.
5. `docs/story/Visual_Prompting_Guide.md` - prompt construction and visual consistency.
6. The relevant world's `00_overview.md`, `03_locations.md`, and quest/event files under `docs/story/world_3_the_spire/` or `docs/story/world_4_the_foundry/`.
7. `docs/story/Enemy_Sprite_Generation_Guide.md` when portrait and combat-sprite production starts.
8. `docs/story/Audio_Design_Guide.md` and `docs/story/Music_Production_Process.md` when audio production starts.

Use `implementation_plan.md`, `codex_handoff.md`, and `walkthrough.md` only as historical World 3 records. They are not the current plan.

## Credentials and Asset Generation

- `openai_api_key.txt` is available for image generation.
- `elevenlabs_api_key.txt` is available for music and sound-effect generation.
- Never print, paste, commit, or write either key into documentation, source files, generated metadata, prompts, or logs.
- Load a key into the current process environment only, for example:

```powershell
$env:OPENAI_API_KEY = (Get-Content -Raw 'openai_api_key.txt').Trim()
$env:ELEVENLABS_API_KEY = (Get-Content -Raw 'elevenlabs_api_key.txt').Trim()
```

- Follow `Starborn_Art_Production_Guide.md` for every generated room background. Do not generate directly from a room name without the story location brief and visual constraints.
- Inspect generated assets before wiring them into JSON. Avoid text, logos, UI, characters dominating the frame, and compositions that hide exploration affordances behind Android overlays.

## Source of Truth and Validation

Gameplay content lives primarily in `app/src/main/assets/*.json`; visual assets live under `world_assets/src/main/assets/`. Story documents provide intent, but the JSON and tests define currently implemented behavior.

After content or asset-reference changes, run:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:runAssetIntegrity
```

For focused manual validators, run as applicable:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\validate_room_presence.ps1
python scripts\validate_dialogue_emotes.py --fail-on-missing --min-uses 12
powershell -ExecutionPolicy Bypass -File scripts\validate_audio_references.ps1
```

Before finishing a work block, inspect `git status --short`, review only the files touched for that block, and report any validator or test that could not be run.
