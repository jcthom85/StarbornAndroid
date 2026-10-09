# Desktop data-source audit

October 7, 2026. Scope: source tracing across the live desktop routes, comparison with Android, and byte comparisons of staged/packaged assets. This is not a full gameplay playthrough. No gameplay or content changes were made during this audit.

## Cleanup implemented after the audit

The following findings have now been addressed:

- `AstraCatalog` is the single shared source for Android and desktop: all 14 original simulation programs and all 10 film titles, audio IDs and discovery locations. No original writing was edited.
- Character, enemy, skill and world labels resolve through authored definitions. Arcade titles come from the original Astra room actions. Cooking companion labels also use character definitions.
- `ItemDetails` presents supplied equipment attack styles, charge/splash modifiers, elements, on-hit statuses/chances, accuracy, critical chance, stat bonuses, and item restoration/damage/buff/cooldown/use/schematic effects. Status and schematic IDs resolve to authored names. Absent fields do not become fabricated zero stats or fallback prose.
- Party skill details include original names/descriptions, power, cooldown, targeting, statuses and battle limits. Map details include node exits, path hints, services, darkness, previews and threats.
- `GameBootstrap` shares normal/debug starter gear, initial session state, starting skill policy, quest identifiers and NG+ seed policy between Android and desktop. Platform save/audio/runtime setup remains in platform services.
- BurgQuest's existing common-room variation is in one shared presentation constant.
- Removed the unused alternate game, journal, rest, tape and cinematic screens, the unreachable cinematic route, and obsolete inventory/gear/journal/map/stats menu implementations.
- Added catalog/entity/audio checks, bootstrap/NG+ checks, item field checks, and rendered catalog checks. `verifyGameAssetParity` compares every staged and archived asset against Android source bytes, detects conflicting duplicate paths, and runs during portable packaging and Gradle `check`.

The original audit below records the pre-cleanup findings. This cleanup does not establish complete gameplay parity or replace a native playthrough. See `CONTENT_PARITY_CLEANUP.md` for validation and the portable build.

## Original audit result

Desktop's main gameplay content comes from the Android assets and shared runtime. This is not yet consistent across every presentation surface. There are live shortened catalogs and ID-derived labels, omitted authored information, and duplicated startup/presentation logic that can drift.

Authoritative sources are the Android asset tree, the world asset pack, shared runtime rules/controllers, and Android-authored catalogs that currently live in Kotlin. Not every catalog exists in JSON. Desktop layout, keyboard hints, and generic controls can be platform-specific; story writing, entity names, numerical rules, and catalog contents should share their source.

## Asset verification

- Packaging sources: `app/src/main/assets`, `world_assets/src/main/assets`, and `app/src/main/res`.
- 1,327 unique source files, including 45 JSON files.
- No conflicting duplicate paths among these sources.
- SHA-256 comparisons: zero missing or differing files in staged assets.
- SHA-256 comparisons: zero missing or differing files inside the portable application's `starborn-assets` JAR.
- This confirms the bytes in the packaged assets, not that every field is displayed or every asset path is resolved correctly by every screen.

## Live systems

| System | Source | Audit result |
| --- | --- | --- |
| Rooms, variants, actions, darkness, travel gates | `rooms.json`, shared exploration controller and room presentation | Shared source. Bunk light/door requirements are authored data, not a desktop invention. |
| Worlds, hubs, nodes, descriptions, locked previews | World/hub JSON and shared hub controller | Shared content. Some hub labels are generated from IDs; generic fallback prose remains. |
| Quests | `quests.json`, quest repository/runtime | Main quest HUD/Journal now display the authored main writing. Stages and tasks remain in the runtime for progression. |
| Dialogue and choices | Dialogue JSON and shared dialogue/exploration runtime | Live overlay uses supplied lines and choices. |
| Cinematics | Cinematic JSON and shared coordinator | Live renderer reads authored steps, images, timing, effects and audio commands. |
| Items and inventory | `items.json`, inventory service/session bridge | Real names, quantities and descriptions; presentation omits some effect/detail information. |
| Weapons, armor and mods | Item data, shared gear rules, exploration callbacks | Real equip rules and numerical data. UI provides incomplete equipment explanations and derives character labels from IDs. |
| Combat, enemies, skills, statuses | Character/enemy/skill/status JSON and shared combat controller/engine | Main rules and rewards are shared. Some FX events and detail are not presented. |
| Party stats, levels and skill trees | Character/leveling/progression/skill-tree data and shared runtime | Values are shared. Skill IDs are displayed as formatted IDs in one detail view. |
| Shops | Shop/item assets and shared shop controller | Inventory, price and availability come from the controller. |
| Cooking and tinkering | Recipe assets and shared services/crafting controller | Recipes, ingredients, results and eligibility are shared. |
| Fishing | Fishing recipe/zone data and shared fishing controller/service | Real zone text, fish and gear. A hardcoded fallback zone remains in routing. |
| Arcade | Shared arcade engines/service plus desktop cabinet directory | Rules/progress shared; directory IDs and titles duplicated in desktop Kotlin. |
| Astra destinations | Shared exploration runtime | Destination titles and travel state come from shared data. |
| Simulation deck | Desktop Kotlin list | Enemy IDs point to real enemies, but program titles are shortened and Android descriptions/categories are omitted. |
| Tape deck | Desktop Kotlin list | IDs/audio tracks are duplicated; display titles are generated from track IDs rather than using authored film titles/descriptions. |
| Audio | Shared bindings/catalog/router, packaged audio, desktop output adapter | Same bindings and assets. Native playback limitations remain separate from content sourcing. |
| Saves | Shared session state, protocol persistence and migration; desktop filesystem adapter | Core save representation is shared. Local paths and settings adapters are platform-specific. |

## Confirmed live exceptions

### 1. Simulation and tape catalogs — high priority

`DesktopRuntimeOverlays.kt` has its own lists. The simulation deck displays “Sentinel” instead of Android's “Sentinel Target Droid” and drops authored descriptions/categories. The tape deck renders lowercase text derived from IDs, whereas Android has titles such as “Film 01: Unpayable Debt” and location metadata. Item JSON also has film names and descriptions.

Move the existing Android catalog entries into a shared definition consumed by both platforms. Preserve their exact writing and track/enemy IDs. Use item definitions for film metadata where applicable. Do not create another desktop catalog.

### 2. Entity names generated from identifiers — high priority

- Equipment party chips and inventory target buttons capitalize character IDs.
- Party details replace underscores in skill IDs instead of resolving skill names.
- Unmatched enemy interaction labels replace underscores in enemy IDs, despite authored enemy names being available (for example, `sentinel_mki` is “Sentinel Mk. I”).
- The hub directory displays formatted world IDs instead of authored world titles.

Resolve names through the same repositories/runtime presentation data. Identifier text should be a diagnostic fallback, not normal presentation.

### 3. Authored information omitted — high priority

Gear comparisons show basic damage/defense/stat changes but omit richer weapon abilities, cooldowns/attack styles, armor features, and loadout effects. Party details show skill identifiers with no authored skill explanations. Item descriptions are real, but detailed effect information is simplified. Map services are displayed as enum names; node exits/path hints and some indicators are omitted. Quest rewards and full stage detail are not exposed by the simplified Journal.

These are presentation omissions, not replacement gameplay rules. Present existing data through shared formatting where Android already has it. The recent choice to emphasize main quest writing should remain intact; task completion can remain internal or be shown in an explicitly requested detail view.

### 4. Separate startup and New Game Plus logic — medium priority

`DesktopAppServices` duplicates Android's new-game bootstrap, starter-equipment maps, story identifiers and NG+ seed construction. The core four-character equipment mappings and opening quest/stage IDs match Android. Android has additional startup/migration helpers; desktop does not call the same bootstrap implementation.

Consolidate bootstrap and NG+ policy into shared code. Retain desktop save/audio/platform adapters. Do not infer changed gameplay rules simply from a different desktop service implementation; compare resulting sessions and migration behavior before replacing it.

### 5. Copied special-case prose and presentation catalogs — medium priority

The BurgQuest Astra common-room paragraph is copied in both platform UI files. Arcade cabinet titles and some program/film definitions are also Kotlin-authored. They are not automatically incorrect because they are outside JSON, but duplicated definitions create drift risk. Extract the existing authored content into one shared source.

### 6. Silent fallbacks — medium priority

Routing has a default fishing zone; the combat screen has default enemy IDs, though the live combat route supplies actual encounter enemies. Some missing-data paths use generic descriptions or default leveling/progression objects. Current packaged files match their source, but these fallbacks can conceal future data/lookup failures.

Use explicit unavailable/error states for missing required authored data. Keep harmless control labels and empty-state messages separate from story content.

## Unused code, not current gameplay

`DesktopGameScreen`, `DesktopJournalDialog`, `DesktopRestStopDialog`, and `DesktopTapeDeckDialog` have no live call sites in the desktop source. The old cinematic route has no scene assignment that activates it. Some legacy screens contain invented prose or independent formulas; the rest dialog directly changes HP with its own formula. These are cleanup liabilities, but they are not evidence that the current rest action uses that formula: the live rest action invokes the shared controller.

Remove/consolidate the legacy paths after preserving useful presentation code. Audit findings should always identify the live route before attributing behavior to it.

## Quest-source clarification

Both Android and desktop use `QuestAssetDataSource.loadQuests()`, which reads `quests.json`. The opening light/door/sleeping-level task text exists in that file. `quests_base.json` is an additional packaged file, not the active quest source; switching to it would change the quest set and break current progression references.

Desktop's recently updated HUD and Journal read the authored title, main summary, description and flavor through the quest repository. There should be no desktop rewrite of that writing.

## Recommended order

1. Share the simulation/film catalogs and restore exact authored labels/descriptions.
2. Replace live ID-derived entity labels with repository/runtime names.
3. Present missing equipment, skill, item and map details from existing definitions.
4. Share new-game/NG+ bootstrap policy and remove dead alternate screens.
5. Add data-origin/catalog consistency checks so new Android content cannot silently produce shortened or missing desktop content.

Native UI layout, audio output, progression across all worlds, and save upgrade behavior still require separate playtesting. This audit establishes source provenance and specific code-level exceptions.
