# World 1 Vertical Slice Status

## Stateful Room Description Pillar

- World 1 now treats highlighted room words as stateful world interactions, not just embedded buttons. Key onboarding and quest rooms use `description_variants` so room prose changes after actions such as turning on Nova's bunk light, inspecting/rerouting/clearing the Med-Bay vents, thawing the Server Room console, and entering Red Alert escape states.
- Authoring standard: immediate feedback can appear in a popup, but the room description should preserve the changed truth once the popup is gone. Validators check base, dark, and variant descriptions for inline action discoverability.
- Coverage: 15 of 81 World 1 rooms carry variants (was 7). The 2026-07-28 pass added colony-wide reactions to the MQ01 cutter surge (`ms_w1_mq01_cutter_surge`) across the Pit and Trade Row, the post-handshake perceptual shift at `echo_heart` (`ms_w1_mq03_echo_marked`), and Red Alert states for the remaining Launch Bay rooms.

### Authoring trap: variants can hide actions

**A `description_variants` entry that omits an action's name makes that action untappable while the variant is active.** Inline actions are discovered by matching the action `name` against the *currently rendered* description.

`validate_world1_content.ps1 -StrictInlineActions` will **not** catch this: it checks whether the name appears in *any* description (base, dark, or variant), so a keyword present only in the base passes validation while being unreachable in play.

When writing a variant, carry every still-relevant action keyword into the new text. Omit a keyword only when the action is genuinely finished (a cleared blockage, a completed hack). Audit with:

```
for each World 1 room, for each variant:
    missing = [action.name for action in room.actions if action.name.lower() not in variant.description.lower()]
```

Two real defects found this way on 2026-07-28, both since fixed: turning on the bunk light (the first action in the game) permanently hid the `netting` holding Jed's carving, and the Red Alert variant on `launch_lift` hid both `override panel` and `hydraulics` for the entire Cargo Lift sacrifice sequence.

Last updated: 2026-07-28

## Verified Gates

Run from the repo root:

```powershell
.\scripts\verify_world1.ps1
```

The verification script runs `validateWorld1Assets`, `runAssetIntegrity`, `testDebugUnitTest`, and `lintDebug`. If an Android device is visible to `adb`, it also runs the Maestro smoke flows. Use `-SkipMaestro` for static verification only, or `-InstallDebug` to install the debug APK before Maestro. The script configures a repo-local `.gradle-codex` cache, Java temp directory, Android user home, and Kotlin in-process compilation settings so it can run in locked-down shells without writing Kotlin daemon or Android metrics files under the Windows user profile.

Release-candidate verification completed on the Pixel 8a (`46121JEKB11849`) on 2026-06-13. World 1 asset/integrity validators, all 133 JVM tests, and Android lint passed. The 31-flow gated Maestro set passed across the full run and focused reruns after stale selectors were updated for the redesigned abilities menu and expanded debug menu. This includes Hub 1, Hub 2, combat abilities/details, combat menu integration, all main quests, all gated side quests, save/load, and combat presentation checks. The optional `dynamic_enemy_movement.yaml` flow also passed separately.

The Deep Mine debug checkpoint now persists the Pressure Hauler moving patrol as defeated so deterministic MQ03 verification cannot be interrupted by the optional patrol system. `Debug: Dynamic Patrol` explicitly restores that patrol for its dedicated scenario. The exploration movement ticker now starts only when movement parties exist and uses JVM-safe monotonic time, preventing local unit-test coroutine leaks and infinite `advanceUntilIdle()` runs.

The `validateWorld1Assets` task covers World 1 room/content references, room topology, room action wiring, room NPC/shop interaction wiring, room NPC presence, audio catalog references, progression/milestone/dialogue references, balance guardrails, and dialogue emote references. Room topology validation checks every node entry room, verifies that every room in a World 1 node is reachable from that node's entry room, and rejects one-way north/south/east/west links inside World 1. Room action validation checks player-action event ids, milestone gates, container item ids, shop ids, and whether each World 1 room action name appears in its room description so it can be highlighted and tapped inline. Shop validation checks reachable World 1 shop stock, rotating stock, gate keys, gate milestones, and sell blacklist entries against the item catalog. Content polish validation rejects placeholder/default/demo copy in World 1 hub, node, room, room action, blocked-exit, NPC, dialogue, quest, stage, and task display text, and in all cinematic titles and step text. Combat validation checks World 1 enemy stats, drops, enemy-only abilities, and runtime affinity data, and rejects legacy `weaknesses` fields because combat reads `resistances`. Strict art/audio validation also checks PNG signatures and minimum dimensions for World 1 room, hub, node, playable character, NPC, enemy combat portrait, and used dialogue emote art, plus MP3/WAV headers and minimum file size for referenced audio cues.

The JVM critical-flow suite now covers the main World 1 path from Nova's bunk through launch and the Sector 9 crash-site handoff, plus all authored World 1 side quests: Scavenger's Stash, System Flush, Heavy Lifting, Protocol Override, and The Lost Shift. It also verifies a disk-backed save/load resume point at the launch lockdown handoff: the test completes the actual MQ03 event/dialogue path, writes the resulting session to a save slot, restores into a fresh runtime harness, then completes MQ04, MQ05, and the Sector 9 crash-site transition.

World 1 reward and economy scripting now has explicit guardrails in progression validation: event/dialogue item grants, item takes, reward payloads, shop references, quest reward items, and scripted reward quantities must resolve to valid data and use positive quantities where applicable. Unsupported event action types are rejected instead of being silently ignored. JVM tests also cover dialogue `give_credits` parsing into reward actions and EventManager reward dispatch.

The current World 1 combat placement covers all seven authored enemy types: Echo-Borer, Siren Skimmer, Dominion Dampener, Acoustic Bulwark, Resonance Buoy, Pressure Hauler, and The Iron Warden. The balance validator distinguishes standard, elite, and boss reward/HP ranges.

World 1 audio now has dedicated generated ambience loops for Homestead colony spaces, Logistics machinery, cold server rooms, emergency launch areas, and Stellarium dust weather. The bindings no longer use forest birds for mine or logistics ambience. Strict audio validation also guards runtime-required title and victory music in addition to bound World 1 exploration/combat/cinematic cues, and `validateWorld1Assets` now rejects World 1 hubs/rooms that cannot resolve both music and ambience through their room or hub binding. Weather-tagged World 1 rooms must also resolve a weather audio layer.

World 1 audio polish now includes short one-shot cues for quest detail presentation, bunk light toggles, terminal bootups, workshop repair success, lockdown escalation, Warden entry, Chime handoff, pod launch, and crash impact. Quest presentation cues are routed through semantic UI bindings, while story and room-state beats use authored `audio_layer` event actions.

## Current Non-Blocking Issues

- June 18, 2026 World 1 onboarding pass: focused device flows passed on Pixel 8a for `mainquest_wake_up_call.yaml`, `checkpoint_badge_gate.yaml`, `heavy_lifting_training.yaml`, `mainquest_the_echo.yaml`, `mainquest_red_alert.yaml`, and `mainquest_the_launch.yaml`. Static validation also passed with `.\gradlew.bat :app:runAssetIntegrity`, and `Hub1CriticalFlowTest` passed after wiring the MQ02 -> MQ03 handoff so `The Echo` starts/tracks immediately when Zeke grants the Mine Access Badge. The Tuning Fork sync now unlocks `nova_blast_wave` and shows the Source Art tutorial before `Red Alert`. Zeke now joins mechanically during the W2 crash-site check-in, while the party UI tutorial remains deferred until first usable Nova+Zeke combat. Menu saving is now taught at first Jed's Workshop arrival, and weakness reward feedback now shows a compact combat banner when weakness hits reduce cooldowns. Remaining content decisions are bed/rest recovery scope, first-party-combat tutorial timing, and optional snack/status micro-tutorials.
- Dynamic enemy movement/patrols remain excluded from the standard World 1 release gate, but the dedicated `dynamic_enemy_movement.yaml` Pixel flow passed on 2026-06-13.
- `verify_world1.ps1` was hardened on 2026-06-12 to isolate Gradle, Java temp, Android user-home, and Kotlin compiler state under `.gradle-codex`. Focused checks passed after the change: `.\scripts\verify_world1.ps1 -SkipGradle -SkipTests -SkipLint -SkipMaestro`, `.\scripts\verify_world1.ps1 -SkipTests -SkipLint -SkipMaestro`, and `.\scripts\verify_world1.ps1 -SkipGradle -SkipLint -SkipMaestro`.
- A fresh post-stabilization static gate passed on 2026-06-13: World 1 asset/integrity validators, all JVM tests, and Android lint.
- Static verification passed on 2026-06-10 with `.\scripts\verify_world1.ps1 -SkipMaestro`, covering World 1 validators, asset integrity, JVM tests, and lint. Follow-up focused static gates also passed after the compact room entity tray redesign and again on 2026-06-11 after adding the checkpoint, Scavenger's Stash, Heavy Lifting device coverage, and strict inline room-action validation.
- Static verification passed again on 2026-06-11 after the prioritized room-presence dock pass with `.\scripts\verify_world1.ps1 -SkipMaestro`, covering World 1 validators, asset integrity, JVM tests, and lint.
- The full scripted Maestro gate passed on the connected Pixel 8a on 2026-06-10 with `.\scripts\verify_world1.ps1 -SkipGradle -SkipTests -SkipLint -Device 46121JEKB11849`. That run covered `smoke_launch.yaml`, `start_new_game.yaml`, `early_tutorial_dismiss.yaml`, `early_exploration_navigation.yaml`, `room_keyword_inspection.yaml`, `early_jed_dialogue.yaml`, `room_entities_tray.yaml`, `room_item_pickup.yaml`, `first_combat_entry.yaml`, `enemy_party_combat.yaml`, `combat_target_prompt.yaml`, `combat_command_menu.yaml`, `combat_menu_dismiss.yaml`, `combat_enemy_status_rail.yaml`, `combat_flashbang_fx.yaml`, `save_load_roundtrip.yaml`, and `debug_full_inventory_menu.yaml`.
- `mainquest_wake_up_call.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts a fresh game and verifies the in-app `Wake Up Call` path: first tutorial dismissal, bunk inspection, quest update banners, travel to Jed's Bunk, Jed's intro dialogue, inventory and NPC tutorials, hub return, Jed's Workshop entry, tinkering tutorials, Functional Cryo-Inductor crafting, and quest completion popup.
- `sidequest_system_flush.yaml` passed standalone on the connected Pixel 8a on 2026-06-10 after the banner/backtracking polish pass. It starts from the Med-Bay debug checkpoint and verifies the in-app `System Flush` path: Doc intro dialogue, quest start popup, queued quest progress banners, visible travel selectors for return routing, vent-room entry, `Inspect Vent` action, return-to-Doc stage update, Doc turn-in dialogue, and quest completion popup.
- `sidequest_scavenger_stash.yaml` passed standalone on the connected Pixel 8a on 2026-06-11 after the inline keyword hit-target fix. It starts from the Trade Row Scrapper debug checkpoint and verifies the in-app `The Scavenger's Stash` path: Scrapper intro dialogue, quest start popup, queued quest progress banners, stash-room discovery, `Inspect Rebel cache`, `Rebel Cache` event-announcement dismissal, return-to-Scrapper dialogue, and quest completion popup.
- `shop_scrapper_contraband.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts from the Trade Row Scrapper debug checkpoint, opens the `Contraband` service action, verifies Scrapper's buy/sell shop screen, visible stock, empty sell state, and return to exploration.
- The Scrapper shop flow passed again on the Pixel 8a on 2026-06-12 after compacting oversized stock cards and replacing the large persistent shopkeeper dialogue card with a concise footer. Pulse Grenade, Battery Pack, and Scrap Metal are now visible together without an initial scroll, while Buy/Sell, affordability feedback, portrait, dialogue, and touch targets remain available.
- The side-quest pass drove follow-up exploration polish: quest progress banners were compacted and moved above the room copy/interactable area, and visible direction arrows now include visited backtracking exits instead of relying on swipe gestures.
- `heavy_lifting_training.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts from the Loading Dock debug checkpoint and verifies Bogs' `Heavy Lifting` intro, quest start popup, loader/cargo quest updates, Acoustic Bulwark combat entry, `Hydraulic Kick` appearing in Nova's combat skill menu, the training Bulwark victory, spoils, level-up, quest completion popup, and return to the compact room presence dock. The earlier run also fixed an NPC-presence dialogue bug where presence ids such as `foreman_bogs` were shown as names but sent raw ids to dialogue lookup.
- `checkpoint_badge_gate.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts from the Transit Checkpoint debug checkpoint and verifies Hank's denial, queued Paperwork quest updates, Zeke's override dialogue, Mine Access Badge grant, quest completion, Blast Door A opening into Transit Tunnel, `The Echo` starting in Concourse Lobby, and Bogs redirecting an untrained Nova into `Heavy Lifting` before the Deep Mine route. The earlier run also fixed blocked-direction `key` gates so they use the same locked-arrow and unlock evaluation path as standard `lock` gates.
- `mainquest_the_echo.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts from the new Deep Mine debug checkpoint and verifies Bogs' real MQ03 assignment dialogue, Deep Elevator progression, persisted cleared-encounter suppression for the Echo-Borer, Acoustic Bulwark, and Pressure Hauler route rooms, threshold and Echo Chamber quest updates, the Tuning Fork relic cinematic, `The Echo` to `Red Alert` quest handoff, and final banner dismissal back to clean exploration.
- `mainquest_the_echo.yaml` passed again on the Pixel 8a on 2026-06-12 after updating the stale title/`Next` assertions to the current titleless, tap-to-advance cinematic contract. The flow now verifies all five Tuning Fork narration steps before the `Red Alert` handoff.
- `mainquest_red_alert.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts from the new Red Alert debug checkpoint and verifies Zeke's comms route from the Emergency Exit, persisted cleared-encounter suppression for the Resonance Buoy and Acoustic Bulwark escape route, Cargo Lift quest progress, Jed's sacrifice dialogue, the Chime handoff, and the `Red Alert` to `The Launch` quest handoff. The pass also fixed a debug-start race where asynchronous quest runtime reset could wipe seeded mid-quest stages, and current-room NPC presence now refreshes immediately when milestone changes remove an NPC from the room.
- `mainquest_the_launch.yaml` passed standalone on the connected Pixel 8a on 2026-06-11. It starts from the Launch debug checkpoint and verifies the post-Warden Pod Bay state, Zeke's Chime-splice dialogue, Pod Core navigation-console interaction, the Planetary Impact cinematic, `The Launch` completion, the `A Strange Coast` World 2 handoff, quest banner dismissal, and Crash Site return to exploration. The run also exposed a missing Crash Site background, now fixed with `app/src/main/assets/images/rooms/world_2/crash_site.png`; adjacent Sector 9 hub/node/canopy/stream art remains a World 2 follow-up unless pulled into the handoff polish scope.
- The shared cinematic narrative overlay was redesigned on 2026-06-11 so narration-only scenes such as `Planetary Impact` now present as a centered story panel over a dimmed full-screen stage without visible title/progress/button chrome. The overlay reveals text progressively and advances by tapping the stage; voiced/speaker steps stay in the normal dialogue box treatment. `mainquest_the_launch.yaml` captures `planetary-impact-cinematic` during the flow.
- The exploration room-content surface was compacted on 2026-06-11: room prose now lives in a shorter scrollable panel, while NPCs, item/equipment pickups, and hostile parties live in a stacked presence tray with tight wrapping chips per category. Pixel 8a regression passes after the redesign: `room_entities_tray.yaml`, `room_item_pickup.yaml`, `presence_stress.yaml`, `room_keyword_inspection.yaml`, and `mainquest_wake_up_call.yaml`.
- The Tinkering screen was redesigned on 2026-06-11 as `Jed's Bench`: the extra tinkering vignette overlay was removed, station screens now respect the Android status-bar inset, workbench slots/output are compacted into one assembly panel, schematic cards use readable result/requirement copy, category filters fit on-phone without horizontal scrolling, and craftable recipes show `Ready` instead of `Locked`. Pixel 8a check after the pass: `mainquest_wake_up_call.yaml`, including the clean Wake Up Call route to Tinkering and schematic craft of `Functional Cryo-Inductor`.
- Item recovery event announcements were redesigned on 2026-06-11 so cache rewards render as compact loot cards with a recovery label and item chips instead of sentence-only modal copy. Pixel 8a check after the pass: `sidequest_scavenger_stash.yaml`, including `Rebel Cache` recovery and visible `Pulse Grenade` loot.
- Compact room-presence chips were tightened on 2026-06-11 so NPC labels are centered in their pills and item-chip contents are vertically centered. Pixel 8a checks after the change: `room_item_pickup.yaml` and `mainquest_the_launch.yaml`.
- The room-presence dock was reprioritized on 2026-06-11 so hostile parties read first, NPC chips stay calmer, and item chips take less visual weight in crowded rooms. A later polish pass hides redundant category markers in single-category rooms, caps very dense trays, and makes the collect-all pickup affordance compact. Pixel 8a checks after the change: `presence_stress.yaml`, `room_item_pickup.yaml`, and `room_entities_tray.yaml`.
- `presence_stress.yaml` was added on 2026-06-11 as a debug-only exploration UI fixture. It starts from an unreachable room containing multiple NPCs, multiple pickups, and multiple separate enemy-party entries so the presence tray can be reviewed under a crowded state before that density appears in story content.
- `presence_combat_return.yaml` is now part of the standard `verify_world1.ps1` Maestro gate. It starts from the crowded debug presence room, clears one hostile party, then verifies the defeated party disappears while remaining NPCs, items, and enemy parties stay visible after returning from combat.
- Pixel 8a regression coverage on 2026-06-12 also passed `heavy_lifting_training.yaml`, `checkpoint_badge_gate.yaml`, `mainquest_red_alert.yaml`, `mainquest_the_launch.yaml`, `first_combat_entry.yaml`, `enemy_party_combat.yaml`, `combat_target_prompt.yaml`, `combat_command_menu.yaml`, `combat_menu_dismiss.yaml`, `combat_enemy_status_rail.yaml`, `combat_flashbang_fx.yaml`, `save_load_roundtrip.yaml`, and `debug_full_inventory_menu.yaml`. The checkpoint flow completed every recorded command but Maestro 2.6.0 hung during process shutdown; its command report confirms the final screenshot completed.
- Maestro 2.6.0 does not expose the suggested `--wait-for-idle-timeout` option. A percentage-coordinate probe was slower and missed the control, so the suite retains Compose semantic selectors. The verifier now submits all YAML flows in one Maestro invocation to reduce repeated JVM/driver startup and Windows heartbeat-file contention. The wrapper isolates per-run temp files, cleans them afterward, and treats known disk, native startup, assertion, element, and device failures as fatal even when Maestro reports an unreliable process exit code.
- Cleared room encounters are now persisted as room state and reapplied when rooms reload, so defeated hostile parties no longer rely only on the current in-memory room model.
- `room_keyword_inspection.yaml` now taps the room keyword through the semantic selector `Inspect Sleeping pods` instead of a brittle coordinate. Inline room-action keywords expose stable `Inspect ...`, `Talk to ...`, or `Inspect ...` accessibility labels while preserving the highlighted inline-text presentation. Inline hit targets now hug the highlighted text with small padding so adjacent wrapped keywords, such as `rebel cache` and `security field`, do not steal each other's taps. `validateWorld1Content -StrictInlineActions` now rejects authored room actions whose names are missing from their room copy; the current all-room audit has no missing inline action text.
- Maestro device runs require the phone screen to be unlocked or ADB keyguard dismissal to succeed. If a run captures the Pixel lock screen, run `.\scripts\adb.ps1 -s <device-id> shell input keyevent KEYCODE_WAKEUP` and `.\scripts\adb.ps1 -s <device-id> shell wm dismiss-keyguard`, then rerun the flow.
- `scripts\adb.ps1` now fails fast after 45 seconds by default if `adb.exe` wedges, instead of hanging indefinitely. Override with `STARBORN_ADB_TIMEOUT_SECONDS` only for intentionally long ADB operations.
- Keep several GB free before full device runs. Maestro writes logs/screenshots under `.maestro-home`, and Gradle build outputs can be regenerated with `.\gradlew.bat clean` if the workspace gets too tight.
- Reviewable git checkpoints still need to be created from an environment with write access to `.git`.

## Maestro Selector Drift (2026-07-28/29 repair)

**Result: 28 of the 31 gated flows pass, up from 11.** Verified on `emulator-5554`.

### RESOLVED: the `Quest Updated` banner was never broken

Investigated at length on the assumption that progress banners were being dropped. **They are not.** Device instrumentation on both ends of the path showed the full chain working:

```
markTaskComplete quest=w1_sq02 task=enter_ventilation_hub newlyCompleted=[] alreadyDone=false
emit PROGRESS quest=w1_sq02 task=enter_ventilation_hub objectives=2
overlay received type=PROGRESS quest=w1_sq02
overlay QUEUED quest=w1_sq02 queueSize=1
```

Emitted, received, queued, rendered. The cause was **test-harness timing**: `QUEST_BANNER_AUTO_DISMISS_MS` is 5 s, and a single Maestro hierarchy dump on a loaded emulator can take longer than that, so the assertion polls after the banner has already self-dismissed. Confirmed by temporarily raising the constant to 30 s, at which point the `Quest Updated` assertions passed; the constant was then restored to 5 s, since player-facing timing should not be bent to suit a test harness.

**Do not assert transient banners in Maestro.** Anything with an auto-dismiss shorter than a few seconds is unassertable on this hardware. Verify progression through room/action steps and the quest-completion popup instead. The banner assertions were removed from `sidequest_system_flush` and `sidequest_scavenger_stash` for this reason.

Dead ends ruled out along the way, recorded so nobody re-walks them: it is not a domain-layer failure (`QuestRuntimeManagerTest` passes), not a duplicate `UiEventBus` instance (`AppServices` builds one and shares it), not conditional mounting of `QuestBannerOverlay`, not `replay = 0` on the event bus, and not a general breakage of `enter_room` triggers (`mainquest_red_alert` asserts the same banner on the same trigger type and passes).

### Test-fidelity gap: JVM harness bypasses the banner path

`Hub1Harness.onQuestTaskUpdated` writes directly to the session store:

```kotlin
onQuestTaskUpdated = { questId, taskId -> store.setQuestTaskCompleted(questId, taskId, true) }
```

Production instead routes through `questRuntimeManager.markTaskComplete(questId, taskId)` (`ExplorationViewModel`), which is where progress banners are emitted and where stage advancement is evaluated. The critical-flow tests therefore exercise a path the real game never takes, and structurally cannot catch banner or stage-progression regressions. Worth aligning the harness with production wiring.

Still failing, with diagnosis:

| Flow | Cause | Nature |
| --- | --- | --- |
| `early_jed_dialogue` | Asserts a `Wake Up Call` quest card + `CONTINUE` chain that the onboarding pass removed | Needs re-authoring against current onboarding |
| `early_exploration_navigation` | Same as above | Needs re-authoring |
| `sidequest_system_flush` | No `Quest Updated` banner ever appears — see the open bug below | **Blocked on a real defect**, deliberately left failing |
| `sidequest_scavenger_stash` | Same as above | **Blocked on the same defect** |
| `save_load_roundtrip` | Reaches the end of the save/load round trip but fails at a different step on consecutive runs (`Tap on "MENU"`, then the room title) | **Flaky** — longest flow in the suite, most state transitions; needs stabilising rather than another point fix |

`dark_room_visibility` and `dynamic_enemy_movement` (outside the 31-flow gate) **target a room that no longer exists**. Both wait for a room titled `Dark Room` and assert `"It's too dark to see what's here."`; no room carries that title and no asset or source file contains that string. The only dark room in the game is `pit_nova_bunk`, whose `description_dark` is authored and whose generic fallbacks are `"It's too dark to make out the room."` and `"It's too dark to feel your way in that direction."` These flows need re-pointing at a real dark room or deleting. (A roaming Pressure Hauler also intercepts the traversal, but that is a second problem, not the cause.)

## Maestro selector validator

`scripts/validate_maestro_selectors.ps1` (wired into `validateWorld1Assets`) cross-checks every flow selector that quotes authored prose against the assets and the Kotlin source. It exists because the canon migration rewrote dialogue and room copy while several flows kept asserting the old wording, and nothing connected the two — the breakage was invisible until a device run.

Design notes, since the naive version is useless:

- **Check assets *and* Kotlin.** UI chrome (`"Dialogue Popup. Tap to continue"`, `"Search world, quest, room, or system"`) lives in source, not JSON. Without the source in the haystack the validator reports 187 findings, essentially all noise.
- **Skip runtime-composed labels.** The UI builds `"Item acquired: $itemLabel"`, so the full literal exists nowhere. Prefixes like `Item acquired:`, `Engage `, `Craft `, `Save Slot` are skipped.
- **Skip `notVisible` / `assertNotVisible`.** Asserting the absence of a string that no longer exists is legitimate and often the point.
- **Only inspect prose-shaped selectors** (25+ chars, containing a space, not ALL-CAPS). Short labels are chrome and produce false positives.

Tuned this way it reports **6 genuine findings**: the two dead `Dark Room` assertions, a World 1 rest message (`hub_rest_w1`), and three World 2 facility lines. It is registered without `-Strict` until those are resolved; flip it on afterwards so an orphaning prose edit fails the gate.

The suite had silently rotted against several UI redesigns: 20 of 31 gated flows could not reach their first assertion. Nothing in the static gate catches this, because flow YAML is not validated by Gradle. The failures were **not** content regressions — they were stale selectors. The drift classes below are the ones to check first whenever a flow starts failing at its opening steps.

| # | Redesign | Stale selector | Current contract |
|---|---|---|---|
| 1 | Debug menu nested behind a button and entries renamed | `"Debug: Enemy Party"` tapped directly from the main menu | Tap `"Debug Scenarios"`, tap the search field `"Search world, quest, room, or system"`, `inputText` a distinctive term, then tap the exact card title (`"Enemy Party Combat"`). Titles dropped the `Debug: ` prefix — see `DebugScenario.kt` for the current list. |
| 2 | Prologue plays on a fresh session | Post-scenario room wait of 15 s | A scenario launched with `clearState: true` replays the ~45 s `intro_prologue`. The first room wait after launching a scenario needs ~90 s. |
| 3 | Presence dock: combined party chip became per-enemy standees | `"Engage Dominion Dampener + Resonance Buoy party"` | One standee per enemy: `"Engage Dominion Dampener"`. Built in `EnemyPartyStandee` as `"Engage ${enemyDisplayLabel(enemyId)}"`. |
| 4 | Presence dock category headers removed | `assertVisible: "People"` / `"Items"` | No category headers render. Assert the entity chips themselves. |
| 5 | Item acquisition overlay redesigned | `"ITEM OBTAINED"` + `"GOT IT"` | A tap-to-continue card: `"Item acquired: <item>. Tap to continue."` |
| 6 | Tutorial overlay button | `"GOT IT"` | `"Continue"` (`"CONTINUE"` matches). |
| 7 | NPC presence chips render `short_name` | `tapOn: "Foreman Boggs"` | `tapOn: "Boggs"` — chips use the NPC's `short_name`, not `name`. |

**Maestro text matching is full-match, not substring.** `assertVisible: "Heavy Lifting"` matches the debug scenario card (whose title is exactly that) but *not* the quest card, whose title is `Heavy Lifting: Required Training`. The same literal passes in one place and fails in another, which reads like an ordering flake. Wrap any assertion against a composed string — quest titles, item labels, dialogue — in `.*`.

**Narrative edits silently invalidate flow assertions.** Several flows asserted prose the canon migration had rewritten: `"Loader's sitting idle"` → `"Loader's idle"`, `"The bunk light snaps on"` → `"coughs on"`, `"filing cabinets for people"` → `"a name, a shift code, and a repair tally"`. Nothing links dialogue/room copy to the flows that assert it, so this class of breakage is invisible until a device run. Treat a prose edit as a reason to grep `playtests/maestro/` for the old string.

**Flows sharing a device are order-dependent.** A flow launched with `clearState: false` inherits whatever the previous flow left on screen — including text in the Debug Scenarios search box, which silently turns `inputText: "Heavy"` into a query that matches nothing. Every debug-menu flow now issues `eraseText` before typing.

**Onboarding beats were removed, not renamed.** The fun-factor pass deleted the up-front interruptions, so there is no `TUTORIAL` overlay or `Wake Up Call` quest card immediately after `New Game` — the prologue plays and the player lands in Nova's Bunk. The movement tutorial now fires only after the player acts (bunk light, then conduit). Flows still asserting the old sequence are encoding a superseded product decision and need re-authoring, not a selector fix.

Two further authoring hazards, unrelated to any redesign:

- **Turn order is speed-derived**, so which character's command menu is available at the start of combat varies. A flow that taps a specific character before acting is coupled to `CombatFormulas.speed`. Zeke has the lowest agility in the debug party and is never actionable first. Prefer asserting the thing under test directly over driving an ability through a named character.
- **Combat damage is unseeded** (`DefaultCombatRandom`: weapon roll plus ~6.6% crit), so kill-turn count varies between runs. Any repeated attack loop must guard each iteration with `when: notVisible: "Spoils Recovered"`, or it will fail whenever a crit ends the fight early. `fun_opening_mastery.yaml` failed roughly this often before being guarded.

**Dense rooms overflow the presence panel.** `debug_presence_stress` intentionally holds 3 NPCs, 4 items, and 3 hostile parties. The dock wraps into a fixed-height panel, so the tail of the tray sits below the fold and is not on-screen for `assertVisible`. Assert entities known to render above the fold and rely on the screenshot for reviewing density.

## Maestro Smoke Coverage

Current runnable flows:

```powershell
.\scripts\verify_world1.ps1 -InstallDebug
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\smoke_launch.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\start_new_game.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\early_tutorial_dismiss.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\early_exploration_navigation.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\room_keyword_inspection.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\early_jed_dialogue.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\mainquest_wake_up_call.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\room_entities_tray.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\room_item_pickup.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\presence_stress.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\presence_combat_return.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\shop_scrapper_contraband.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\sidequest_system_flush.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\sidequest_scavenger_stash.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\heavy_lifting_training.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\checkpoint_badge_gate.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\mainquest_the_echo.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\mainquest_red_alert.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\mainquest_the_launch.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\first_combat_entry.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\enemy_party_combat.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\combat_target_prompt.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\combat_command_menu.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\combat_menu_dismiss.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\combat_enemy_status_rail.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\combat_flashbang_fx.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\save_load_roundtrip.yaml
.\scripts\maestro.ps1 --device <device-id> test .\playtests\maestro\debug_full_inventory_menu.yaml
```

`save_load_roundtrip.yaml` is included in `verify_world1.ps1` and covers the in-app field menu save/load path with stable selectors (`Open Save Slots`, `Save Slot 1`, `Open Load Slots`, `Load Slot 1`) exposed by the Settings panel and `SaveLoadDialog`.
