# Desktop versus Android audit

Later implementation updates: see `CONTENT_PARITY_CLEANUP.md` and `COMBAT_PRESENTATION_AUDIT.md`. The matrix below records earlier findings; several content, combat, prompt and cinematic-camera gaps have since been addressed.

Date: 2026-10-06. This replaces any earlier implication that sharing game controllers means desktop presentation is finished.

## Assessment

Desktop has the campaign data and shared gameplay rules, but several screens still expose only part of Android's presentation and feedback. The opening cinematic and actionable room prose were substantial omissions. They are fixed in this pass. Desktop is ready for focused playtesting, not a claim of complete Android parity or release readiness.

This is a source and automated UI audit across the live desktop navigation graph, Android screens, shared state, event consumers, saves, assets, settings, and minigames. It is not a manual six-world desktop playthrough. Tests of unused preview components do not establish live-route parity.

## Fixed in this pass

- The live exploration route now renders illustrated cinematics across the game viewport instead of in an AlertDialog. It follows Android's authored timing, including the four-second default and 500 ms minimum, without requiring dialogue clicks.
- Ported Android's camera transforms, outgoing-frame cross dissolves, cuts, end fades, breach strobe and scanlines, door impacts, beast flash, stasis mist/frost, and location/system/narration/dialogue caption treatments. The prologue has no Continue button. Hold to skip retains the shared coordinator's completion behavior.
- Cinematic ambience starts/stops with the scene; step sound and voice cues are dispatched. Music remains owned by the shared controller/router. This avoids a second music or story queue.
- Action planning, explicit authored markers, travel links, room description variants, and background variants now live in `gameRuntime/feature/exploration/presentation/RoomPresentation.kt`. Android and desktop call the same implementation. Desktop uses the Android text renderer, including bold underlined links and accessible hit areas for wrapped words.
- Links invoke the real room-action, NPC, and enemy callbacks. Locked links do not execute. Opening actions were checked by clicking the prose itself and inspecting persisted quest state.
- Desktop now resolves room darkness, obscures unrevealed titles, applies dark descriptions, and hides NPC/item/enemy lists in dark rooms. Generator lighting remains scoped to the wired rooms. Background variants follow authored state.
- The live dialogue route now uses Android's typewriter reveal, punctuation timing, speaker murmurs, voice replay, tagged choices, and reveal-before-choice behavior. Enter reveals the line before advancing; number keys select responses after reveal.
- Audio gains now apply centrally to every screen, including ambience. Previously the music/ambience label did not adjust ambience.
- Window focus/minimization now feeds combat background pause, exploration visibility, and cinematic automatic progression. Cinematic step timing restarts on resume, as Android's lifecycle block does. Native focus behavior still needs real-window verification.
- Fixed advertised field-menu number shortcuts and explicit fishing keyboard focus. The E shortcut skips locked room actions. Exploration now displays shared status and nearby/moving threat notices.

## Remaining parity matrix

"Functional" means the live route calls shared gameplay behavior; it does not mean all visual details or real-device behavior have been tested.

| Area | Current desktop behavior | Remaining work | Priority |
| --- | --- | --- | --- |
| Illustrated intro | Full viewport, authored timeline and sound dispatch, Android effects/captions | Listen on a real audio device; inspect wide screens, skip and resume in native window | P1 verification |
| Other illustrated scenes | Same new renderer and shared coordinator | Sample every illustrated scene and its final fade/room reveal | P1 verification |
| Card cinematics | Full viewport scrim with manual caption panel and cue dispatch | Narration reveal/glow and speaker presentation remain simpler than Android; duration behavior needs scene-by-scene review | P1 |
| Room prose/actions | Shared plan and dynamic prose/art; clickable action/NPC/enemy spans | Broader in-game samples across all six worlds, locked and disappearing actions | P1 verification |
| Exploration navigation | Keyboard cardinal movement, exit list, inline authored special exits | Android's direction indicator states, edge arrows, compact minimap, locked/enemy/nearby threat visuals are absent | P1 |
| Exploration enemies | Enemy list, engage callback, shared encounter/aggression rules; threat text | `visualEnemyParties`, composite icons, tier scaling, movement and aggression visual feedback are not rendered | P1 |
| Room atmosphere/transitions | Room image, darkness, authored fades | Live screen does not use weather, theme style, theme bands or vignette; `roomTransition` is not animated. Existing DesktopVfx helpers are unused | P2 |
| Dialogue | Android reveal/murmurs/replay/choices restored on live route | Verify long dialogue, many choices, portrait offsets and narrow layouts against Android; old preview test still covers a different component | P1 verification |
| Exploration prompts | All four concrete UIPrompt types and narration dismiss correctly | Tutorial/milestone/item presentation remains generic dialogs; item images, categories, sequence indicators and Android banners absent | P2 |
| Quest notifications | New/progress/completed/failed titles and toast text | Banner objectives/counts are discarded; ShowQuestSummary and JournalBadgeDelta ignored; ShowQuestDetail reduced to banner text | P1 |
| Combat commands/rules | Shared targeting, actions, abilities, items, ATB, tutorial callbacks and outcomes | Full encounter variety and keyboard-only coverage beyond focused tests | P1 verification |
| Combat feedback | Sprites, selected/valid targets, lunge/dodge, HP changes, telegraphs, heal/break/KO labels | StatusApplied, SupportCue, TurnQueued and CombatOutcomeFx events ignored; Impact element/showAttackFx/broken-bonus detail lost. Android effect canvas/particles and attack-specific presentation absent | P1 |
| Combat timed input | Tap button and Space call shared timed response | Android countdown/progress bar and full-screen timed prompt missing; native latency/pause testing needed | P1 |
| Combat victory | XP/AP/credits/drops and level-up information displayed | Android staged spoils, portraits and richer outcome presentation absent | P2 |
| Inventory | Search/sort/categories, real use/target callbacks, quantities and persistence | Android ingredient/fish/component/snack distinctions and effect detail rows are simplified; cargo and field-menu views differ | P2 |
| Equipment | Party selection, story locks, real equip/unequip, base stat comparison, mods | Weapon abilities, cooldowns/attack styles, armor features, richer slot layout and compatible-mod filtering absent. Raw base comparisons omit the full derived loadout explanation | P1 |
| Journal | Active/completed/history/milestones, search, tracking | Quest summary strings lose individual completed-objective state; no fishing journal; full stage/reward details are not opened from quest list | P1 |
| Map | Spatial known-room graph, selection, connections and services text; unknown destinations masked | Full Android legend/icons, node exits/path hints/dark indicators and minimap fallback missing; long-map navigation/zoom needs review | P2 |
| Stats/skills | Shared party stats and purchasable skill nodes | Skill tree is a list rather than branch graph; party details expose fewer fields than Android and need narrow-screen review | P2 |
| Tinkering | Shared controller, learned recipes, base/components, preview/build, scrap and tutorial notifications | Bench drag/drop/visual slots, ingredient selection clarity and tutorial highlighting are simplified; add UI acceptance tests | P2 |
| Cooking | Shared recipe discovery, portions, chefs, ingredients, batch cooking and prepared food rules | Live exploration call does not pass saved large-target/high-contrast preferences; keyboard and result feedback need coverage | P2 |
| Fishing | Shared hook/reel controller, rod/lure choices, progress/tension and result persistence | Android scene/art, effects/audio and detailed journal missing; no catch-loop desktop acceptance test; cancellation/reward acknowledgement needs verification | P1 |
| Arcade | Six desktop screens use shared engines, scoring/progress, keyboard input and accessibility flags | Verify every cabinet repair/start/pause/resume/reward loop through live exploration; visuals/audio/layout need side-by-side review | P2 |
| Astra navigation | Destinations/travel/disembark and shared transit state | Navigation is a basic dialog; transit/arrival presentation, long destination scrolling and native travel flow need polish | P2 |
| Simulation deck/tapes | Shared launch/play/stop callbacks and ownership gating | Lists are basic, partially hardcoded; tape dialog lacks scrolling; scene/art details and catalog drift coverage needed | P2 |
| Hub | Shared controller supplies node visibility/locks/entry and completion | Fixed 320 dp left panel plus other columns risks narrow/high-DPI clipping; full hub navigation and selected-node behavior need UI tests | P1 |
| Save/load/NG+ | Protocol saves, legacy migration/backup, manual/quick/auto slots, title loading, shared state restore | In-session load intentionally unavailable; real interruption/corrupt-save recovery, NG+ progression and upgrade continuity need coverage | P1 verification |
| Settings/accessibility | Persistent volume, display mode, tutorial and arcade preferences | Title and field settings differ. High contrast, large targets, vignette, screen shake/flash preferences are not consistently consumed outside arcade; introductory effects also need accessibility review | P1 |
| Desktop lifecycle | Foreground hooks added for exploration/combat/cinematics | Fishing/arcade timers and audio do not have unified background pause; F11 window recreation may rebuild transient combat/minigame state | P1 |
| Packaging/native usability | Portable app bundles assets/runtime; packaged smoke check exists | MSI/EXE freshness, install/upgrade/uninstall, 125–200% DPI, multi-monitor, fullscreen and real audio remain unverified | P1 verification |

## Implementation cleanup

The live route is `Main.kt -> DesktopExplorationScreen -> ui.cinematic -> DesktopCinematicOverlay`. The old `DesktopCinematicScreen` route has no assignment that populates its scene and must not be used as evidence for the opening. `DesktopGameScreen`, `DesktopJournalDialog`, `DesktopRestStopDialog` and `DesktopTapeDeckDialog` are also legacy/unused presentation paths. The unused rest dialog even contains its own invented HP formula; the live rest action correctly goes through the shared controller. Remove or consolidate these paths after preserving any useful presentation pieces.

Room action planning is now shared. The ported Compose text/dialogue/cinematic renderers still exist as platform files with asset/audio adapters. A common presentation module should replace these copies to prevent future Android changes silently failing to reach desktop. Do this after the regression cases are stable.

## Recommended sequence and acceptance criteria

1. **Protect the opening.** Keep tests on the live route: complete prologue without input, verify every sound dispatch, hold-to-skip, final room reveal, and prose-driven light/conduit progression. Manually listen and inspect widescreen native playback.
2. **Restore combat feedback.** Implement remaining FX event consumers and timed-input countdown. Verify ordinary/support/status/elemental attacks, misses, stability breaks, timing windows, victory/defeat/retreat, and focus changes with a normal-level party.
3. **Restore exploration readability.** Render enemy parties, directional state and minimap. Check locked exits, non-cardinal authored travel, approaching enemies, darkness/power and transition sequences in each world.
4. **Finish information parity.** Gear abilities/compatible mods, full quest details/objective completion, fishing records, richer inventory effects and map legend. Every displayed feature should have a reachable action and an acceptance test.
5. **Unify settings and lifecycle.** Consume accessibility preferences consistently; pause all active minigames; preserve transient state across display-mode changes. Check keyboard focus after every overlay closes.
6. **Run the release playthrough.** Fresh new game through all six worlds using the native desktop UI, with save/load checkpoints, side activities and NG+. Then exercise packaged Windows installs, DPI/display modes and real audio. Shared legal-route tests are useful but do not replace this playthrough.

## Evidence and limits

- Automated cinematic asset check: **51 scenes, 8 unique image/portrait references and 26 unique cue references; zero unresolved direct files**. Results: `tmp/cinematic-assets-audit.json`. This checks file presence, not audible playback or visual composition.
- Initial focused run passed the live automatic intro/audio-dispatch test, room reveal/voice test, opening prose interactions and Android explicit-action/description tests: `tmp/cinematic-action-verification.log`.
- Screenshots: `desktopApp/build/reports/desktop/screenshots/intro-breach-fullscreen.png` and `opening-pod-row.png`, visually inspected in this pass.
- The recording audio driver verifies dispatched commands without requiring speakers. This environment previously had no usable JavaSound Clip device; real output remains unverified.

### Final regression results

- `tmp/desktop-parity-audit-verification.log`: **BUILD SUCCESSFUL**; 13 desktop tests, 7 shared-runtime tests, and 66 selected Android tests passed, with no failures or skips. Android selection covers cinematic invariants and all-world discoverability/room-description behavior; this is not the entire Android suite.
- Desktop coverage includes full automatic prologue plus underlying-menu click protection, sound dispatch and ambience cleanup, room reveal/voice, action/NPC/enemy links and locked links, dialogue reveal/murmur/replay/choice callbacks, opening progression/save persistence, menu shortcuts/use/equip/tracking/map, crowded combat targeting/victory/defeat, save migration, and packaged assets.
- The first dialogue regression used an exact label without Android's numeric prefix; the selector was corrected. A later victory regression exposed an incomplete fixture (no weapon and an arbitrary eight-attack limit). The final fixture supplies an actual owned/unlocked Mining Pistol; the eight-command bound and victory assertions remain. No combat balance or runtime rule was changed.
- `tmp/desktop-parity-packaged-smoke.txt`: **PASS**, 1,327 bundled assets, 477 rooms, four character definitions, Java 21.0.12.1, launched from the Windows temporary directory, exit code 0.
- `tmp/desktop-parity-portable-build.log`: **BUILD SUCCESSFUL**. The portable app and ZIP are rebuilt for this pass. Existing MSI/EXE installers were not refreshed; use `desktopApp/build/compose/binaries/main/app/Starborn/Starborn.exe` or the latest portable ZIP for these changes. Close any previously running copy first. Start **New Game** to see the prologue; Continue does not replay it.
- The original broad Android fishing timing failure noted in `WINDOWS_PARITY_PROGRESS.md` remains outside this selected regression. It was not changed or waived.
