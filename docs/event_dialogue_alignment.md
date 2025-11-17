# Event & Dialogue Alignment Plan

## Goals
- Keep quest and milestone state authoritative while combat and exploration expand.
- Guarantee dialogue triggers, event actions, and combat outcomes stay in sync with the new `CombatEngine`.
- Prepare for richer tutorials/cinematics without blocking gameplay tasks.

## Immediate Actions
1. **EventManager parity audit**
   - Mirror Python trigger filters (`item_acquired`, `player_action`, `encounter_victory`) by reading session inventory & combat results.
   - Persist event completion in `GameSessionState` using dedicated structures instead of `evt_completed_*` milestones.
   - Promote quest/task mutations to dedicated methods so UI can refresh without ad-hoc hooks.
2. **Dialogue condition fidelity**
   - Expand `DialogueConditionEvaluator` to check active inventory, milestone absence, and quest stages.
   - Pipe dialogue triggers through a dispatcher that can enqueue EventManager actions (start quest, give item, launch combat).
   - Surface portrait/audio metadata in `DialogueLine` so overlays can render avatars and trigger VO cues.
3. **Combat results → world state**
   - Surface `CombatOutcome` via `GameSessionStore` and fire `EventPayload.EncounterOutcome` for victory/defeat/retreat.
   - Attribute XP/AP/loot to the session/inventory, then re-run event hooks for drops and quest updates.
   - Add defeat/retreat tutorial events so overlays and cinematics mirror the Python guidance.

## Near-Term Tasks
- Create bridge service that converts `Player`/`Enemy` models into `Combatant` definitions and registers encounter rewards (XP, credits, drops).
- Attach `EventManager.onSpawnEncounter` to a shared encounter coordinator that launches `CombatScreen` with the proper enemy ids and callbacks.
- Add integration tests stubbing `CombatEngine` to verify quest advancement, milestone setting, and dialogue unlocks after combat victories.
- Share the cinematic coordinator across screens (exploration, combat, hub) so authored `play_cinematic` actions render through a single overlay. Add sanity tests that `EventManager` invokes `onPlayCinematic` and run-time assets (`cinematics.json`) include any new scenes (e.g., `scene_mine_restore`).
- Extend the data-integrity suite (`DataIntegrityTest`) with checks for tutorial scripts and exit unlock metadata so `system_tutorial` / `unlock_exits` references fail fast when assets are missing.

## Risks & Mitigations
- **State divergence** between combat results and exploration session → centralize updates in `GameSessionStore` to avoid duplicate bookkeeping.
- **UI notification gaps** after events fire → formalize UI event stream (snackbars, overlays) and ensure all EventManager actions use it.
- **Complexity creep** as dialogue/quest logic grows → codify data contracts (models, adapters) now to minimize later rewrites.

## Dependencies
- Inventory service must expose query APIs for dialogue/event checks.
- Upcoming persistence layer work should store event completion and quest stages alongside combat outcomes.
