# Save & Backup Parity Plan

_Scope: Phase 5 persistence polish._

**Status:** Quicksave slots, backup rotation, and autosave fingerprint throttling are now implemented in the Kotlin build (Nov 2025). Export/cloud steps remain backlog.

## Goals
- Mirror Python’s `SaveSystem` capabilities (manual slots, autosave throttling, quicksave) while embracing Android storage best practices.
- Provide automatic backup rotation so corrupted writes can be rolled back.
- Define the path toward optional cloud/export support without blocking the on-device experience.

## Current Kotlin State
- `GameSessionPersistence` stores Proto DataStore saves plus three manual slot protobuf files.
- Autosave writes every 90 seconds via `AppServices.scheduleAutosave`.
- Manual save/load UI mirrors slots and autosave metadata.
- Legacy JSON imports populate quests/milestones/tutorial state.
- Missing pieces: quicksave entry point, backup rotation, autosave fingerprinting, cloud/export UX.

## Proposed Enhancements
### 1. Autosave Fingerprint + Throttling
- Track a lightweight fingerprint (world/hub/room ids + quest status + inventory digest) per autosave write.
- Extend `GameSessionState` serialization to expose a `fingerprint()` helper.
- Before writing autosave, compare fingerprint + elapsed time. Skip writes when nothing meaningful changed.
- Persist last fingerprint in `GameSessionStore` or alongside autosave proto metadata so process restarts remain accurate.

### 2. Quicksave Flow *(implemented)*
- Add `quicksave` slot (proto file `game_session_quicksave.pb`) triggered via pause menu.
- UI: add “Quicksave” button with confirmation; load dialog surfaces quicksave entry above autosave.
- Hook into `MainMenuViewModel`/`GameSaveRepository` with new APIs: `writeQuickSave()`, `loadQuickSave()`, `clearQuickSave()`.

### 3. Backup Rotation *(implemented)*
- Before overwriting any slot/quicksave/autosave file, copy the previous file to `<name>.timestamp.bak` (mirrors Python).
- Keep last N backups (default 3). Rotation handled by helper inside `GameSessionPersistence`.
- Surface backup list in `scripts/` so QA can restore quickly when debugging saves.

### 4. Export / Cloud Prep
- Document exported payload format (Proto → JSON) so we can later send to Play Games / Drive.
- Short term: add “Export Save” developer action that writes current slot to `Documents/Starborn/slotX.json`. This validates serialization + decoding path.
- Long term: integrate with Drive/Play Games once auth pipeline is ready; spec placeholder env vars/secrets in docs.

### 5. QA & Telemetry
- `GameSessionPersistenceTest` now covers quicksave round-trips and backup rotation. Future work: add explicit autosave throttle test + export script coverage.
- Update `docs/testing_matrix.md` once export tooling lands.

## Implementation Steps
1. **Persistence helpers**
   - Add `GameSessionPersistence.BackupManager` for copy + prune logic.
   - Store fingerprint + timestamp in proto metadata.
2. **UI wiring**
   - Expose quicksave buttons (pause menu + main menu overlay).
   - Show backup availability in developer debug panel (optional).
3. **Export script**
   - CLI task `./gradlew :app:dumpSave --slot 1 --out build/saves/slot1.json`.
4. **Docs**
   - Keep this plan updated; reference from `docs/phase5_perf_release_checklist.md` once features land.

Deliverable: by end of Phase 5 we have resilient local saves (auto/manual/quick) with backup rotation, a stub export story, and automated regression tests guarding the persistence stack. This unlocks final QA + beta packaging without risking player progress.*** End Patch
