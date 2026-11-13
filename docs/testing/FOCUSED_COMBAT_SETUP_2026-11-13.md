# Focused combat playtest setup — November 13, 2026

Prepared four earned combat checkpoints on the standard emulator (`emulator-5554`) and expanded shared manual-save capacity from 10 to 20. Existing slots 1–10 were retained. The emulator now runs the current debug build, v1.3.120 (204), and is left at Load Game with slot 11 visible. No Pixel was connected.

## Available saves and added checkpoints

The original emulator had all ten numbered slots occupied, spanning Worlds 1–6. Slot 2 was at Pod Bay; slot 4 was at Canopy Ridge after Gh0st had joined, so it was unsuitable for reviewing the Beast before recruitment. The current campaign and newest numbered save were in World 6.

Added only these previously absent files:

| Slot | Location / encounter | Earned arrival |
| --- | --- | --- |
| 11 | Pod Bay / Iron Warden | Level 4, Nova alone |
| 12 | Canopy Ridge / Source Beast | Level 5, Nova, Zeke and Orion; confrontation complete; Face the Beast available |
| 13 | Hound Nest / Shard-Hound pair | Level 5, Nova and Zeke |
| 14 | Resonant Canopy / Stalker-Vine and Spore-Spitter | Level 5, Nova and Zeke |

Slots 15–20 remain empty. See the [player instructions](../playtest/FOCUSED_COMBAT_PLAYTEST_2026-11-13.md).

The Warden, hound and plant snapshots were captured before engagement on the verified public completionist route starting at the earned World 1 launch checkpoint. The Beast snapshot uses the previously verified shorter World 2 main-route arrival. These contain exact earned gear, HP, XP, inventory and quest state; the exporter grants nothing. All four have no pending battle, no battle checkpoint and no purchased build nodes. Their encounters remain undefeated.

JSON sources and provenance live in `app/src/test/resources/playtest/focused-combat/`. `FocusedCombatCheckpointExportTest` converts them to normal saves through GameSessionPersistence, checks exact round-trip parity and exports only slots 11–14 to `app/build/reports/focused-combat-checkpoints/slots/`.

## Preservation and installation

Backups are outside the repository at `/Users/jason/.codex/starborn-backups/focused-combat-2026-11-13/`:

- `emulator-5554-files-before.tar`: initial inventory backup.
- `emulator-5554-files-before-install.tar`: after stopping the app, before updating.
- `emulator-5554-files-after-preparation.tar`: after adding the four slots, before launching.
- `preservation-verification.json`: hashes and preservation assertions.

The first non-streamed install was rejected for storage space. A streamed in-place update succeeded. The app was not uninstalled and its data/cache were not cleared. Existing numbered saves, live campaign, autosave and quicksave were byte-for-byte unchanged immediately after import. After opening Load Game, slots 1–10 and the four new files were checked again and remained byte-for-byte unchanged. Normal app startup refreshed the autosave timestamp; no review checkpoint was loaded into the live campaign during UI verification.

New files were imported through temporary app-owned files, checked against the exported bytes, given private file permissions and renamed only into unused slot paths. No existing slot was overwritten. The separate laboratory emulator was not used for these user-facing saves.

## Verification

- **28 focused unit tests passed:** twenty-slot title-menu listing/loading, all numbered saves remaining independent of auto/quick saves, persistence tests, and exact four-checkpoint export/reload.
- The earned public completionist route was rerun successfully to capture the first-engagement snapshots, including actual combat and all ten World 2 quests through the World 3 arrival. Earlier World 2 main-route/Beast evidence supplies the fourth checkpoint.
- The updated APK was built and installed successfully. The displayed title version is v1.3.120 (204); no additional version increment was made for this setup.
- Actual emulator Load Game shows the new checkpoints and the final empty slot 20. The app was left with the Warden checkpoint visible; no gameplay was advanced.
- No combat stats, sprite sizes, encounter formations, quest logic or rewards changed in this setup. Save capacity is the only production behavior change.

Logs, test results, slot manifest, preservation assertions and Load Game evidence are in [the evidence folder](evidence/focused_combat_setup_2026-11-13/). No commit, push, Play Console upload or Pixel installation was performed.
