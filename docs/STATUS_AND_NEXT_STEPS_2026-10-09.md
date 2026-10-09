# Starborn status and next steps — October 9, 2026

Snapshot analysis of the whole project (Android + desktop). Sources: git history through `a787912d` (Oct 8), [OVERALL_PLAN_CHECKPOINT.md](OVERALL_PLAN_CHECKPOINT.md), the `docs/desktop/` pass records, [ENVIRONMENTAL_EFFECTS_PLAN.md](design/ENVIRONMENTAL_EFFECTS_PLAN.md) and its implementation record, and [IMPROVEMENT_BACKLOG.md](IMPROVEMENT_BACKLOG.md).

**Bottom line:** content is essentially complete. The gap is validation by people, on real hardware, end to end, on both platforms.

## Where things stand

| Area | State |
| --- | --- |
| Campaign | 6 worlds, 12 hubs, 30 main + 30 side quests, 477 rooms, ending and credits |
| Assets | Room art, hub maps, node miniatures, portraits, enemy sprites and audio installed |
| Late content | Environmental puzzles in all worlds, Scrapper's Crucible, Skyline murder mystery, easter eggs (1.3.75–1.3.78) |
| Android | 1.3.87 on Google Play internal testing; Playtest sessions/scenarios and offline walkthrough reader |
| Shared runtime | `a787912d` moved gameplay into `gameRuntime` (ExplorationViewModel −6.3k lines, CombatViewModel −3.7k) |
| Desktop | Week of presentation passes: cinematics, dialogue, quests, combat FX, victory, hub, HUD, exploration |
| Uncommitted | Environmental-effects system across both platforms (~29 files incl. `rooms.json`, settings stores, combat/exploration screens) |

## Desktop — next work, in order

1. **Commit the environmental-effects work.** It is implemented and tested in isolation but uncommitted while touching both platforms' core screens.
2. **Restore a green baseline after the runtime refactor.**
   - 3 older desktop exploration-panel tests fail (direction labels/disabled semantics, old ScrollBy selector).
   - Full Android instrumentation suite does not compile (removed `HubMapLayouts`, older inline-action helpers).
   - `FishingRefinementTest.everyAuthoredRodCanLandEveryBehaviorByReadingTheFish` has failed since the port.
3. **Native release playthrough.** Almost every pass is offscreen-render validated; audio output was never heard (no JavaSound device in the build environment). Play the packaged `.exe` New Game → credits with real speakers, 125–200% DPI, fullscreen/F11, save/reload checkpoints.
4. **Close player-visible parity gaps.**
   - Multipart/composite bosses render as an ordinary formation in combat.
   - Weapon-specific attack trails/projectiles and per-status continuous visuals are simplified.
   - Lifecycle: fishing/arcade timers lack unified background pause; F11 window recreation may rebuild transient combat/minigame state.
   - Side activities: fishing scene/art, tinkering drag-and-drop, Astra navigation dialog, simulation deck/tapes.
5. **Controller support + rebindable keys.** The Controls dialog mentions "Keyboard & Gamepad" but there is no gamepad input code. Required for Steam / Steam Deck.
6. **Common presentation module.** Text/dialogue/cinematic Compose renderers are still per-platform copies; drift risk. `shared/src/androidJvmMain` looks like the start of this.
7. **Packaging.** MSI/EXE installers are stale; install/upgrade/uninstall untested. Only the portable folder is current.

## Whole game — next work

1. **Resume the paused combat program** (BurgQuest is done):
   - Finale every-other-Reality-Break stagger comparison.
   - Foundry full arrival → Titan route with real HP, supplies, purchases and rests.
2. **Human playtests.** 550+ unit tests and 70-seed boss matrices do not equal human experience. Use the 1.3.87 Playtest access with outside testers; capture where they die, get lost, or quit — especially Worlds 3–6.
3. **Physical-device acceptance.** Frame time/memory/battery with new effects, large text, touch, and process death mid-combat/puzzle (interrupted battle recovery could duplicate rewards or skip encounters).
4. **Scope discipline.** New features have been landing faster than validation. Consider a soft freeze: fixes and playtest-driven tuning only until a 1.0 release candidate.
5. **Hygiene.**
   - Confirm `openai_api_key.txt`, `elevenlabs_api_key.txt`, `play-service-account.json`, `keystore.properties`, `starborn-upload.jks` are gitignored.
   - Move root-level screenshots / `dump.xml` out of the repo root.
   - Update [antigravity_handoff.md](../antigravity_handoff.md) and [task.md](../task.md) (June-era) to point at the current checkpoint.

## Suggested sequence

1. Commit effects → 2. green tests → 3. native desktop playthrough with a bug list → 4. resume balance + start Android human playtests in parallel → 5. controller support if targeting Steam.

See also: [DESKTOP_PC_NATIVE_EVALUATION.md](desktop/DESKTOP_PC_NATIVE_EVALUATION.md) for the PC-native feel evaluation.
