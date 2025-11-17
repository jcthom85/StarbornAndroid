# Event / Quest / Tutorial Roadmap

_Updated: current session after reviewing Kotlin + Python repos._

## Snapshot
- `EventManager` now tracks completion explicitly, respects quest/task/milestone predicates, and sequences cinematics/tutorials via shared coordinators, but late-game encounter choreography, ally recruitment flows, and audio-layer commands have not been ported.
- `DialogueService` routes triggers through `EventManager` and renders portraits/VO, yet cinematic staging (portrait alignment, timed VO) for late-game chains is still missing.
- `QuestRuntimeManager` persists journal state in `GameSessionState` and emits banners/logs; hub breadcrumbs + ally-sidequest cards are not authored yet.
- `MilestoneRuntimeManager` loads migrated `milestones.json`, unlocks exits, and coordinates tutorial prompts, though the milestone gallery lacks hub summary cards and some unlock effects rely on Kotlin glue rather than JSON metadata.
- `TutorialRuntimeManager` listens to room entry/state toggles (light switch, swipe, crafting/fishing failures) but needs late-game script imports and auto-queued hub hints.

### 2025-02-15 Audit
- Kotlin assets currently include only **9** tutorial script entries, with `movement` duplicated and no coverage for the `light_switch_touch` / `swipe_move` flows handled by the original Kivy runtime (`app/src/main/assets/tutorial_scripts.json`, `Starborn_Python/tutorial_manager.py:1-137`). Late hubs and ally tutorials must be re-imported from the Python data directories.
- `quests.json` diverges as well: Kotlin ships four quest IDs (adds `tutorial_smoke`) while the Python build exposes three base quests. Ally recruitment/journal sequences are still fully authored in Python (`Starborn_Python/quests.json`), so we need an ingestion pass plus integrity checks for the backlog identifiers called out in design docs.
- `NarrativeSystemsInstrumentedTest` only validates the tinkering intro, Jed reward, and market breadcrumb flows (`app/src/androidTest/java/com/example/starborn/NarrativeSystemsInstrumentedTest.kt`). It does not yet cover combat-intro tutorials, ally recruitment, or hub journal cards, so instrumentation must expand once those assets land.

## Roadmap
1. **Late-game content ingestion**
   - Import ally recruitment arcs, end-game quest logs, and tutorial scripts; validate via `./gradlew runAssetIntegrity`.
   - Ensure encounter definitions include VO/audio metadata to keep dialogue → event → cinematic pacing tight.
2. **Event engine extensions**
   - Add `audio_layer` actions (layer, cue, gain, fade) routed through `AudioRouter`.
   - Expand trigger filters for chained boss encounters and late hub milestones while keeping `completedEvents` authoritative.
3. **Dialogue & cinematic polish**
   - Align portrait alignment + VO sequencing with late-game Python scenes, including timed fades and shared narration overlays.
4. **Quest & tutorial enhancements**
   - Add per-node breadcrumbs, hub summary cards, and late-game tutorial cues; extend instrumentation (`NarrativeSystemsInstrumentedTest`) with combat intro + ally recruitment loops.
5. **Validation**
   - Broaden `DataIntegrityTest` to cover quest task IDs, tutorial script references, and audio bindings; surface via `runAssetIntegrity` and CI.

## Next Steps (in-flight)
1. Keep migrating late-game content (quests/tutorials/milestones) while backfilling asset integrity checks.
2. Implement `audio_layer` event actions + router support as part of the audio mixing plan.
3. Expand instrumentation to cover combat intro tutorials and ally reward loops once combat/exploration share the same coordinators.
## Recent updates
- Shared cinematic coordinator is now provided by `AppServices`; combat/exploration screens observe the same queue so scripted events can fire from anywhere.
- `ms_mine_power_on` milestone now includes `unlock_exits` metadata driven by `evt_mine_generator_restore`, mirroring Python's tunnel unlock sequence.
- Imported light-switch and swipe tutorial scripts (`scene_light_switch_hint`, `scene_swipe_movement`) and updated `ExplorationViewModel` to drive them through `TutorialRuntimeManager`, with instrumentation coverage validating the scripted prompts.
