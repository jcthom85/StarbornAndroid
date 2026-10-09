# Desktop content parity cleanup

October 7, 2026.

## Changes

The original Android data remains authoritative. Shared `AstraCatalog`, `BurgQuestPresentation`, `GameBootstrap`, and `ItemDetails` remove presentation/startup duplication. Desktop labels resolve authored names and item, skill and map displays expose supplied fields. Obsolete alternate screens and menu bodies were removed.

New games retain the Android opening: Nova begins without seeded equipment; companion defaults come from the original item definitions. NG+ keeps Android's inventory/party/gear/level/XP/credit policy and resets the story with Master Protocol active. These changes do not rewrite quests or saved content.

## Consistency checks

- `DesktopContentConsistencyTest`: catalogs, entity/audio references, normal/debug bootstrap, NG+ preservation/reset, supplied item fields.
- `DesktopAstraCatalogPresentationTest`: original titles/descriptions/locations, ownership gating, final entries accessible by scrolling; rendered screenshots under `desktopApp/build/reports/desktop/screenshots`.
- `:desktopApp:verifyGameAssetParity`: every Android source file must match the staged tree and assets JAR; conflicting duplicate paths fail the build. Portable packaging and `check` depend on it.

## Validation

- Final selected regression batch: **25 tests passed, zero failures/errors**. Includes content/catalog checks, both catalog renders, exploration HUD, opening acceptance, actionable room text/dialogue, cinematic automation/audio commands, portrait backdrops, early game and packaged asset loading.
- Android `:app:compileDebugKotlin` and desktop Kotlin compilation succeeded.
- Packaging verified **1,327 original assets** in staging and the asset archive.
- Portable executable smoke test passed from an unrelated working directory (`%TEMP%`): 1,327 assets, 477 rooms, 4 characters, bundled Java 21.0.12.1, exit code 0.
- Visually inspected the simulation and film catalog screenshots. All catalog entries remain accessible by scrolling; unowned films are disabled.
- The cinematic test now advances its virtual clock in larger increments and checks forward playback, avoiding a timing-sensitive expectation that it must catch exactly frame 1. It still verifies automatic scene completion and the authored audio commands.

Build/check log: `tmp/content-cleanup-final.log`. Portable smoke report: `tmp/content-cleanup-portable-smoke.txt`.

## Remaining scope

Native audio playback, a full campaign playthrough, combat FX polish, and broader desktop interaction/layout parity still need playtesting. This pass completes the agreed data-source cleanup; it is not a claim that all Android features have been reproduced visually.

## Portable app

`desktopApp/build/distributions/Starborn-Windows-portable/Starborn/Starborn.exe`

This is a folder distribution; no ZIP is required.
