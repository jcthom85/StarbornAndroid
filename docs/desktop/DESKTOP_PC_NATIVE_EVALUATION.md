# Desktop port evaluation: PC-native feel vs. Android spirit

October 9, 2026. Evaluation of the Windows build against the goal: *take the Android game to PC so it feels like a native PC game while keeping the spirit of the original.*

Evidence: source review of `desktopApp` (≈100 Kotlin files) and the shared runtime, plus the latest offscreen renders under `desktopApp/build/reports/desktop/screenshots/` and `desktopApp/build/reports/environmental-effects/`. **Not** a native hands-on playthrough; items marked *(verify)* need one.

## Verdict

The **spirit is well preserved**. The **PC-native layer is about half done**. Visual fidelity to Android has had most of the effort (rightly), but several things a PC player notices in the first five minutes are still mobile-shaped: touch wording, click-heavy combat, no hand cursor, missing PC settings, no window icon, and no controller.

| Pillar | Grade | One-line summary |
| --- | --- | --- |
| Spirit / art fidelity | **A−** | Original art, prose links, fonts, cinematics, portraits, all intact |
| Layout for landscape | **B** | Three-panel exploration and hub work well; combat wastes its side panels |
| Mouse | **C+** | Everything is clickable, but no hover cursor, no right-click, limited tooltips |
| Keyboard | **B−** | Good coverage; some odd bindings, no rebinding, no quick-load |
| Controller | **F** | None, despite the Controls dialog claiming "Gamepad" |
| Wording / prompts | **D** | Tutorials and prompts tell desktop players to tap and swipe |
| Settings / options | **C** | Display modes and accessibility exist; standard PC options missing |
| Window / OS integration | **C** | No icon, no min size, size not remembered, saves in `~/.starborn` |
| Audio pipeline | **C** *(verify)* | Works in principle; single-thread full-decode design risks hitches |
| Ship-readiness hygiene | **C−** | Debug Scenarios on title, F8 dev tool live, version 1.0.0 vs Android 1.3.87 |

## What's already right (keep it)

- **Portrait art in a landscape frame.** Centered original 9:16 art with blurred reflected sides is a strong, honest solution. It keeps every authored composition intact and reads as deliberate, not letterboxed. See Night Market, Campfire, Homestead hub renders.
- **Prose panel with inline action links.** The left narrative card is very "PC adventure game" (Disco Elysium-adjacent) while being exactly the Android writing and interaction model.
- **Right-side HUD stack.** Minimap → tracked quest → party is clean and conventional for PC.
- **Dialogue box.** Bottom-anchored portrait box with typewriter reveal and `ENTER TO CONTINUE` reads like a native PC RPG.
- **Field menu as a sidebar modal.** Sidebar tabs + detail pane is the right PC pattern. Quick save `[F5]` in the header is good.
- **Display modes.** Windowed / Borderless / Fullscreen, Alt+Enter and F11 are all present.
- **Shared runtime.** Gameplay, content, and rules come from one source, so polish work is pure presentation.

## Findings

### 1. Mobile wording reaches desktop players — **high**

The single most "this is a phone port" signal. Desktop players are told to tap and swipe:

- [tutorial_scripts.json](../../app/src/main/assets/tutorial_scripts.json): 8 messages ("Swipe toward any open exit…", "Tap a highlighted character name…", "Tap the dialogue card…").
- [CombatTutorialCopy.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/combat/presentation/CombatTutorialCopy.kt#L21-L34): "When Nova is ready, tap her…".
- [ExplorationController.kt](../../gameRuntime/src/main/kotlin/com/example/starborn/feature/exploration/viewmodel/ExplorationController.kt#L2195-L2202) hint strings and the Items/Party tutorial messages near L3703/L3741.
- [rooms.json](../../app/src/main/assets/rooms.json#L610): "Tap the Faulted Loader to engage in combat!"
- Desktop-authored UI: "Tap to continue" in [DesktopCinematicOverlay.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCinematicOverlay.kt#L939) and [DesktopItemAcquisition.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopItemAcquisition.kt#L163); timed-prompt button labelled "Tap" in [DesktopCombatFeedback.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCombatFeedback.kt#L123); touch-hand icon on "Select a ready character" in [DesktopSceneHeadings.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopSceneHeadings.kt#L30); "touch clicks" footer in [DesktopControlsDialog.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopControlsDialog.kt#L173).
- Arcade instructions: "Tap D-Pad or HOP button", "Tap the 4 frequency pads", "Tap VENT STEAM".

**Fix:** don't fork the writing. Add an input-vocabulary layer in the shared runtime: tokens like `{select}`, `{move}`, `{advance}`, `{key:interact}` resolved per platform/input device ("Tap" / "Click" / "Press E" / "Press Ⓐ"). Convert the ~20 strings above once. Add a test that fails if `tap|swipe` appears in player-facing text without a token.

### 2. Combat is click-heavy and the side panels are underused — **high**

From the combat renders: the left panel shows a title card, a minimal enemy card, and a "Battle log" link. The right panel shows only "Select a ready character" until a sprite is clicked. With one ready character, the player still has to click Nova, then a command, then a target, every turn.

- **Auto-select** the ready character (oldest-ready first) and show its command list immediately. Keep Tab to cycle. Android can keep tap-to-select; this is a presentation choice.
- **Persistent party roster** on the right (portraits, HP, ATB fill, status icons), always visible, clickable to switch actor. This is the PC-JRPG convention (FF/Octopath/Sea of Stars style) and uses the empty space.
- **Inline battle log** (last 4–6 lines) in the left panel instead of a link to a paused modal.
- **Enemy card**: show weaknesses discovered, intent, statuses. The data exists (`intent` is computed but the compact summary drops it).
- **Hover a target** to preview damage/effect and highlight it. Right-click or Esc backs out of targeting.
- Number keys for commands are good; show the key glyphs on the command buttons.

### 3. Mouse affordances — **medium-high**

- **No hand cursor anywhere.** Zero uses of `pointerHoverIcon` in `desktopApp`. Prose links, hub nodes, buttons, sprites, and minimap all keep the arrow cursor. This is a cheap, high-impact fix: one `Modifier.clickableWithCursor()` helper.
- **No right-click.** Zero secondary-button handling. Common PC expectations: right-click to cancel targeting / close a panel, right-click an item to use or equip, right-click a hub node to inspect.
- **Tooltips** exist only on the minimap and hub map (2 `TooltipArea` uses). Add them to item tiles, gear slots, skill nodes, status icons, combat commands, and stat names.
- **Mouse wheel** works on the map, but the Controls dialog still says "+ / - buttons to zoom" ([L145](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopControlsDialog.kt#L145)). Wheel should also scroll the prose/description panel and lists *(verify)*.
- Hover states exist (9 uses) but are not consistent across menus.

### 4. Keyboard — **medium**

Good: WASD/arrows travel, E interact, Esc/I menu, M hub, F5 quick save, H help, number keys in dialogue and combat, Tab focus.

Gaps:
- **No rebinding.** Expected in PC settings, and also an accessibility issue (AZERTY: WASD becomes ZQSD).
- **No quick load (F9).**
- **Menu tabs on Alt+1–6** are unusual. PC convention is direct hotkeys from the field (I items, C/P party, K skills, J journal, M map) and Q/E or Tab to cycle tabs inside the menu. Today **M opens the hub**, which conflicts with the "M = map" expectation. Consider M → field map and a separate key for hub travel.
- **E always uses the first room action.** There is no keyboard way to choose among several actions except Tab focus. Number-key or arrow selection of context actions would fix this.
- Controls dialog oddity: "A / Left Arrow — Travel West / Arcade".
- Esc opens the field menu, but there's no in-game "Quit to Desktop" path except the window X. Add Quit to Title and Quit to Desktop to the menu (Settings has "Save and return to title" only).

### 5. Controller — **high if Steam is a target**

No gamepad input code exists; the Controls dialog subtitle claims "Keyboard & Gamepad". Compose Desktop has no built-in gamepad API. Options are SDL2 via JNI (e.g. a small `sdl2-gamecontroller` binding) or JInput. Plan it as a focus-navigation layer: D-pad/stick moves focus, A confirm, B back, bumpers for tabs/actors, Start for menu. The existing keyboard focus work is the right foundation. The input-vocabulary layer in finding 1 then shows Xbox/PS glyphs.

### 6. Settings — **medium**

Present: three volume sliders, display mode, tutorials, reduce flashes, larger controls, disable screen shake, high contrast, plus environmental effects (uncommitted).

Missing for a PC-native feel:
- **Master volume**, and **ambience separate from music** (ambience is currently bound to the music slider in [DesktopAppServices.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/DesktopAppServices.kt#L380-L381)).
- **UI scale / text size.** Critical on 4K and for high-DPI laptops; also replaces the touch-flavoured "Larger controls" toggle.
- **Monitor selection** for fullscreen and **frame cap / vsync**.
- **Text speed** for the typewriter and **auto-advance** option for dialogue.
- **Keybindings** page and (later) controller page.
- **Mute when unfocused.**
- Title-screen settings and field-menu settings should be the same page.

### 7. Window and OS integration — **medium**

From [Main.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/Main.kt#L62-L80):
- **No window icon** (`icon =` not set) and **no `iconFile`** in [build.gradle.kts](../../desktopApp/build.gradle.kts) packaging. The taskbar, Alt+Tab, and installer show the default Java/Compose icon. Very visible, very cheap.
- **No minimum window size.** A user can shrink the window below the compact layout's assumptions.
- **Window size/position/maximized state isn't remembered.** It always opens at 1280×800, centered.
- **Display-mode change recreates the window** via `key(displayMode)`. `movableContentOf` keeps composition, but the audit already flags possible loss of transient combat/minigame state *(verify)*.
- **Save/config location** is `%USERPROFILE%\.starborn` ([L59](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/DesktopAppServices.kt#L59)). Windows convention is `%APPDATA%\Starborn` or `Saved Games\Starborn`. Moving it now (with migration) avoids pain before Steam Cloud.
- **Closing the window** autosaves to slot 0 silently. That's good, but if the player is mid-combat there's no "unsaved progress" warning *(verify what slot 0 captures mid-combat)*.

### 8. Audio pipeline — **medium (verify)**

[DesktopAudioDriver.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/DesktopAudioDriver.kt):
- **One worker thread** does MP3 decoding *and* all playback commands ([L16](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/DesktopAudioDriver.kt#L16)). Each music change fully decodes the track into a `Clip` ([L70-L77](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/DesktopAudioDriver.kt#L70-L77)). While a multi-minute MP3 decodes, UI/battle SFX queue behind it, which shows up as late hit sounds right when combat music starts.
- Full-PCM clips for long music use tens of MB each (202 MP3s, 253 MB compressed).
- **Fix:** stream music/ambience through a `SourceDataLine` on its own thread. Keep `Clip` for short SFX, pre-decoded and cached for the common UI/battle cues. Run SFX on a separate executor from decoding.
- Real-device listening has never happened, per the parity docs.

### 9. Visual details that read as "mobile UI" — **low-medium**

From the menu renders:
- Large fully-rounded pill buttons (Skills, Equip, Next) are Material-mobile. PC JRPG menus usually use squarer framed buttons, like the 8dp hub controls you just standardized. Apply that style everywhere.
- **Mixed fonts:** Supplies/Key Items tabs and Controls dialog use system Monospace; the title footer is monospace too. Use the Starborn font set consistently.
- Inventory: 3 tiles in a large grid with lots of dead space. On PC, a denser list/grid with columns (name, type, qty) and a sticky detail pane reads better.
- Gear comparison: "Damage minimum / Not specified / 4" is hard to parse. Use a two-column current → new layout with colored deltas (▲ +4).
- Raw labels: "Weapon mod1", "west -> Unexplored (locked)", lowercase context actions ("memory oasis", "campfire cookware").
- Inconsistent Menu button: icon pill in some renders, full-width plain bar in others.

### 10. Ship hygiene — **medium before any public build**

- **Debug Scenarios** is on the release title screen.
- **F8 environmental preview** is reachable in release exploration ([DesktopExplorationScreen.kt](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationScreen.kt#L201)). Gate both behind a debug flag or launch argument.
- Title shows **New Game** first even when a save exists. PC convention puts **Continue** first, showing the save's location and playtime.
- Footer reads "STARBORN (Desktop Edition)". Fine for testing; drop it for release.
- `packageVersion = "1.0.0"` doesn't track Android's 1.3.x. Share one version source.
- Silent fallbacks remain: default combat enemies `scrapper_guard/scrapper_drone` ([Main.kt L135](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/Main.kt#L135)) and default fishing zone ([L205](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/Main.kt#L205)).

## Guiding principles for the rest of the polish

1. **Same game, different hands.** Writing, art, rules, pacing are shared. Input vocabulary, layout density, and OS conventions are per-platform.
2. **Use the landscape space for information, not decoration.** The central art column is sacred; the side panels should carry the data Android hides behind taps (party status, log, intent, tooltips).
3. **Every clickable thing announces itself**: hand cursor, hover state, tooltip, and a key glyph if it has a shortcut.
4. **Never tell a PC player to tap.** Enforce it with a test.
5. **One style system.** Squared 8dp framed controls, Starborn fonts, no system monospace.

## Prioritized polish plan

### Tier 1: quick wins (a day or two, big perceived change)
1. Window icon + `iconFile` for MSI/EXE; minimum window size; remember size/position.
2. Hand cursor helper applied to all clickables.
3. Replace desktop-authored "Tap" strings and the touch icon; fix the stale Controls dialog lines.
4. Gate Debug Scenarios and F8 behind a debug flag; Continue-first title.
5. Right-click / Esc cancels targeting and closes panels.

### Tier 2: core PC feel (about a week)
6. Input-vocabulary tokens in the shared runtime; convert all tutorial and prompt text; add the "no tap/swipe" test.
7. Combat: auto-select ready actor, persistent party roster panel, inline log, hover target preview, key glyphs on commands.
8. Settings: master volume, separate ambience, UI scale, text speed, mute-when-unfocused, monitor selection; unify title and field settings.
9. Tooltips on items, gear, skills, statuses, stats; consistent hover states; replace pill buttons; remove monospace.
10. Audio: streaming music thread, separate SFX executor, cached short cues. Then a real listening pass.

### Tier 3: platform completeness (multi-week)
11. Key rebinding with a conflicts check.
12. Controller support via SDL2: focus navigation and glyph switching.
13. Saves/config to `%APPDATA%\Starborn` with migration; Steam Cloud-friendly layout.
14. Shared version number; refreshed MSI/EXE; install/upgrade/uninstall test.
15. Native playthrough at 1080p, 1440p, 4K@150%, and ultrawide, with real audio, before calling it done.

## Open questions for you

- **Distribution target:** Steam, itch, or private builds? Steam makes controller support, Steam Deck layout (1280×800 is your default window size, which conveniently matches Deck), and cloud saves near-mandatory.
- **Combat interaction:** is auto-selecting the ready character acceptable on desktop only, or do you want Android to adopt it too?
- **"M" key:** field map or hub travel?
