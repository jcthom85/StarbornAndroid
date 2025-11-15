# Starborn Port – Remaining Conversion Scope

## High-Priority Gameplay Systems
- **Exploration overlays & interactions**: Direction ring, minimap highlights, service tray, quick radial menu, milestone bands, weather/vignette shaders, skill tree overlay, and quest journal exist; remaining polish: art-authored hotspot sprites, hub lore tooltips, and laten-game accessibility sizing.
- **Combat presentation**: FX, ally support cues, defeat/retreat cinematics, weather/theme overlays, and victory reward flows are implemented; polish burst timing, enemy telegraphs, and post-battle scripting to reach Kivy fidelity.
- **Dialogue & scripted events**: Branching choices display quest/tutorial/milestone badges, portraits resolve via metadata, and launch events mirror Python intros. Remaining: late-game encounter choreography, ally recruitment arcs, `audio_layer` actions, and combat epilogue cinematics.
- **Quests/Milestones/Tutorials**: Quest journal overlay, milestone gallery, tutorial queue, and quest runtime persistence are active; remaining work: late-game tutorial imports, per-node quest breadcrumbs, hub-summary journal cards, and expanded instrumentation coverage.
- **Inventory-adjacent systems**: Crafting/tinkering/cooking/first-aid/fishing are live with tuned timing flows, FX, and audio cues. Shops include rotating stock and smalltalk scripting; still pending: equipment comparison overlays, bulk-buy shortcuts, and contextual flavor lines.
- **Skill trees & leveling**: Compose overlays expose skill trees/resonance bars, but ability unlock cinematics + per-character progression polish mirror Python’s `SkillTreeManager` only partially; add ribbon FX, AP summaries, and authored tooltips.
- **Audio & theme management**: Hub/room music, ambience, weather cues, VO playback, and cinematic ducking route through `AudioRouter`. Outstanding: per-layer fade helpers, runtime slider-to-router bridge, authored theme swaps, combat stinger mixing, and haptic routing.
- **Save/Load**: Autosave cadence, manual slots, UI overlays, quicksave button, backup rotation, and legacy JSON import exist; remaining work covers cloud/export strategy and automated regression harnesses.

## Supporting Infrastructure
- **Shader/VFX layers**: AGSL vignette + weather overlays landed; still need combat damage-number shaders, blocked-exit cinematics, and fallback coverage for non-AGSL devices.
- **Minimap, radial menu, overlays**: Minimap legend overlay now exists; finish vendor-specific overlays, animated art hotspots, and map breadcrumbs for late hubs.
- **Tooling & editors**: Decide whether to port Python editors (items/milestones/starfield) or keep them as external scripts with clear import/export instructions.
- **Localization & typography**: Theme-aware typography exists, yet font fallback tables + pixel rounding guidelines are still open for non-Latin future work.

## Data Validation & Testing
- JSON schema validation grows through `DataIntegrityTest`; extend coverage for quest/task references, audio cue lookups, and tutorial scripts as new content lands.
- CI already runs `lintDebug`, `testDebugUnitTest`, and `connectedDebugAndroidTest` (see `.github/workflows/android-ci.yml`). Next up: dedicated asset-only Gradle task (`runAssetIntegrity`) plus detekt/static analysis wiring.

## Asset & Performance Considerations
- Verify memory footprint and scaling for large PNG backgrounds and pixel-art assets.
- Audio streaming/mixing strategy (SoundPool/ExoPlayer) to mirror Python’s layered router.
- Content migration: ensure existing saves can be imported or provide conversion tooling.
- Execute FrameTimeline, GPU, memory, and audio profiling passes per `docs/phase5_perf_release_checklist.md`, recording findings in `docs/profiling_reports.md`.

## Delivery Checklist
- Implement missing systems above.
- Replace placeholder UIs with production Compose components.
- Build QA plan: automated tests + manual checklists for main loops (new game, combat, quests, crafting, hub navigation).
- Optimize and polish for mobile (performance, orientation, screen sizes).
- Package for release (app icons, signing, Play Store metadata) with Phase 5 perf captures, release automation, and signing workflows completed.
