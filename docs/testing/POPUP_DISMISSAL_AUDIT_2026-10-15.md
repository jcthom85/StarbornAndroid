# Popup and dismissal audit ? October 15, 2026

> Historical snapshot: the recommendations were subsequently implemented in [popup alignment and backdrop repair](POPUP_ALIGNMENT_AND_BACKDROP_FIX_2026-10-15.md). The tables below describe the pre-alignment source audit.

## Scope and evidence

This inventories player-facing foreground cards, dialogs, drawers, menus, transient notices, tooltips, and debug/demo surfaces in Android, desktop, and shared UI source. Decorative weather, flashes, damage numbers, and ordinary screen navigation are excluded. A screen may use several variants of the same popup; those variants are grouped below. Legacy wrappers remain in the source appendix and are identified where relevant.

This is a source audit, with focused desktop tutorial interaction tests. It is not a claim that every popup has been opened on both devices. Default Dialog cancellation means the source wires the framework dismissal callback and does not disable outside-click or Back/Escape dismissal. It does **not** mean tapping the body acknowledges the popup. Custom overlays have no such default.

The appendix enumerates every direct Dialog, AlertDialog, Popup, DropdownMenu, ModalBottomSheet and TooltipArea construction found in the three UI source trees. The review tables also include custom overlays that use ordinary Boxes/Surfaces rather than those primitives. [JSON call-site details](POPUP_DISMISSAL_CALLSITES_2026-10-15.json) preserve exact handlers, title expressions, confirmation buttons and cancellation buttons.

## Changes completed in this pass

- Desktop exploration tutorial cards now acknowledge from a body click as well as their authored button. Android already did this.
- Both combat tutorial explanation cards now advance from a body tap/click as well as Continue. This applies to the briefing, blocked-attack explanation and success message.
- Both combat perk/build guidance dialogs now acknowledge from a body tap/click as well as Got it.
- Required-action combat hints and tinkering guidance still advance through the required game action. Tapping those hints must not bypass training.
- The same existing callback handles the body and button. A tutorial with an action such as View Nova?s Skills retains that action when its body is clicked.

No other popup dismissal rules were changed by this audit. The alignment column below is a recommendation for the next pass.

## Information, story, and notifications

Desktop exploration Enter acknowledges narration, announcements and queued prompts when the exploration surface has focus. Right-click also dismisses those three. Android exploration Back handles the top applicable overlay; its blocking-screen tap handler can also acknowledge narration/queued prompts. Those host shortcuts are separate from the component?s body behavior. Dialog windows can intercept keys before the host does.

| Surface | Desktop now | Android now | Alignment recommendation |
| --- | --- | --- | --- |
| Exploration tutorial / first-AP guidance | Body or authored button; host Enter/right-click | Body or authored button; exploration Back; root tap path | Fixed. Standard information-card behavior. |
| Combat tutorial explanation (brief, blocked result, success) | Body or Continue; outside/Escape disabled by tutorial Dialog | Body or Continue; campaign Back does not exit combat | Fixed. Preserve training gates. |
| Combat perk/build guidance | Body or Got it; default dialog cancellation | Body or Got it; default dialog cancellation | Fixed. |
| Required-action combat hint | Perform the highlighted action; no tap dismissal | Same | Keep action-driven. |
| Tinkering step banner | Slot base/component, then Build; not freely dismissible | Slot base/component, then Synthesize; not freely dismissible | Keep action-driven; distinguish visually from acknowledgements. |
| Skill-tree purchase guidance | Embedded in the tree; purchase completes it; closing tree leaves guidance pending | Same | Keep action-driven. |
| Inspection / authored narration | Continue; body/background when tapToDismiss is true; host Enter/right-click | Continue; body/background when tapToDismiss is true; Back/root paths also exist | Align the root paths with tapToDismiss so that flag has one meaning. |
| Milestone notification | Dismiss button; host Enter/right-click; body currently disabled | Body or Dismiss; Back/root tap | Add desktop body acknowledgement. |
| Single acquired item / sequenced items | Body advances one item | Body advances one item | Keep; prevent one click from advancing twice. |
| Batch acquisition | Body or Dismiss | Body or Dismiss | Keep. |
| Event announcement | Continue/body/background; Enter/right-click | Card tap; exploration Back | Keep consistent card acknowledgement. |
| Level-up result | Continue; default dialog cancellation | Continue; exploration Back; body not clickable | Add body acknowledgement on both. |
| Rest recovery result | Continue; default dialog cancellation; body not clickable | Full recovery overlay tap | Align to body acknowledgement on both. |
| Blocked room exit | OK; default Dialog cancellation; body not clickable | OK; default AlertDialog cancellation; body not clickable | Add body acknowledgement on both. |
| Locked overworld location | OK in DesktopExplorationModal; default Dialog cancellation | Tap locked-location card; hub Back | Align to body acknowledgement. |
| Important quest detail notification (new/completed/failed) | Continue/X; Enter/Escape; Details opens journal; background absorbs clicks | Continue/X; Details opens journal; background does not dismiss | Decide whether body should acknowledge; preserve interactive Details and scrolling. |
| Quest summary notification | Continue/X; Enter/Escape; per-quest Details | Continue; Details opens journal | Use the same information-card policy as important quest details. |
| Small quest update banner | X or automatic expiry (~3.2 s); body not wired to dismiss | Tap, X, upward swipe, or expiry (~3.2 s) | Add desktop tap dismissal; swipe remains optional mobile convenience. |
| Small progress/toast notice | Automatically expires (~4 s in desktop quest presentation) | Automatically expires (~1.8 s in ProgressToastOverlay) | Unify intent/timing; no modal acknowledgement needed. |
| Shop greeting / entry conversation | Choice buttons, or Enter shop; default Dialog cancellation | Explicit choices or Close; exploration overlay | Treat choices as choices; tapping prose must not buy or choose. |
| NPC dialogue | Tap text to reveal then advance when no choice; explicit choice buttons; desktop Enter/number keys | Tap text to reveal/advance when no choice; explicit choices | Keep two-step reveal/advance and explicit choices. |
| Standard cinematic narration | Continue/Skip buttons; authored duration can advance; body not clickable | Tap screen reveals text, next tap advances | High-priority parity fix: desktop should use reveal-then-advance too. |
| Illustrated cinematic | Click to reveal/advance; authored timing; hold Skip when permitted | Tap to reveal/advance; authored timing; hold Skip when permitted | Keep intentional reveal and hold-to-skip behavior. |
| Victory spoils / growth pages | Continue/Next; Enter after settle guard; body absorbed; Escape consumed | Continue/Next; controller A/Enter/Space; controller B/Escape also advances in host | Review Escape-as-advance on Android; keep reward-page progression explicit. |
| Defeat / retreat result | Continue button | Automatically transitions back after short result delay | Decide one pacing policy for both platforms. |
| Combat ready cue / timed attack prompt | Ready cue resolves by choosing actor; timed prompt by tap/Space or timeout | Ready cue resolves by actor choice; timed prompt by tap/controller or timeout | Gameplay cues, not dismissible notices. Keep separate. |
| Area-entry / hub / combat transition overlay | Animation or runtime transition completes; input blocked | Animation or runtime transition completes; area-entry ~0.55?0.9 s | Keep automatic; do not turn incidental taps into scene skips. |

Primary implementations: [desktop prompt cards](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationPrompts.kt), [desktop exploration host](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationScreen.kt), [Android exploration host and cards](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt), [desktop combat tutorial](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCombatChrome.kt), [Android combat tutorial](../../app/src/main/java/com/example/starborn/feature/combat/ui/components/CombatStageBackdrop.kt), [desktop quest presentation](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopQuestPresentation.kt), [Android quest overlays](../../app/src/main/java/com/example/starborn/ui/overlay), [desktop cinematics](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCinematicOverlay.kt), [Android combat host](../../app/src/main/java/com/example/starborn/feature/combat/ui/CombatScreen.kt).

## Choices, menus, and tools

For these surfaces, a body click is interaction space, not acknowledgement. Default dialog cancellation below cancels/closes; it never presses the affirmative action.

| Surface / variants | Desktop dismissal | Android dismissal | Alignment recommendation |
| --- | --- | --- | --- |
| Room Actions chooser | Close, Escape/outside; number or selected action leaves chooser | Close, Back/outside; selected action leaves chooser | Keep body interactive. |
| Room activity offer (Rest, shop, workbench, fishing, arcade, tuning) | Back or default cancellation; explicit activity button proceeds | Same | Keep explicit action. |
| Toggle / on-off choice | Cancel/default cancellation; explicit enable/disable | Cancel/default cancellation; explicit choices | Keep. |
| Tuning puzzle (standard, counter-tune, other authored presentations) | Cancel/default cancellation; submit solves | Cancel/default cancellation; Engage Handshake/submit solves | Keep. |
| Field menu and nested details (inventory, status, journal, map, settings, Field Kit) | Close/Escape/Alt+Left backs out of detail before menu; background absorbs clicks | Close/Back backs out of detail before menu; background not a confirmation action | Keep hierarchical Back behavior. |
| Party details / gear details | Field menu detail Back/Close; standalone gear details default cancellation | Party dialog Back/outside; embedded modern detail uses menu Back | Keep; ensure every standalone variant has visible close. |
| Skill tree | Close/default cancellation for standalone; field-menu Back for embedded | Close/default cancellation or menu Back for embedded | Do not dismiss by clicking purchase cards. |
| Quest journal / quest details | Close/default cancellation or menu Back; tracking is explicit | Close/Back; journal and detail overlays have own close controls | Keep; distinguish journal from transient quest notification. |
| Map legend | Menu/map controls, detail Back | Close/Back | Keep tool behavior. |
| Gear picker (weapon, armor, mod, accessory; modern/legacy) | Menu detail Back; selection equips | Default cancellation/Close; selection equips | Keep; no body dismissal. |
| Consumable target chooser | Cancel/default cancellation; choose party member to use | Cancel/default cancellation; choose target | Keep. |
| Combat skills/items/target picker | Backspace or Escape cancels applicable selection/menu; explicit skill/item/target | Close/Back/Escape cancels applicable selection/menu; explicit choice | Align keyboard cancel keys; preserve tutorial restrictions. |
| Ability details / active status details | Rich hover tooltip for skills/status; combat log expands separately | Ability/status AlertDialog; Close/default cancellation | Different input affordance is acceptable; keep content equivalent. |
| Expanded combat log | Close/X/default Dialog cancellation | No matching expanded modal found; in-battle log presentation | No forced modal parity required. |
| Mod comparison target menu | Select target or dismiss DropdownMenu | Corresponding comparison is embedded in gear UI | Keep menu cancellation. |
| Move equipped mod confirmation | Cancel/default cancellation; select source to move | Same | Keep explicit move. |
| Scrap item confirmation (modern and legacy) | Cancel/default cancellation; Scrap confirms consumption | Same | Keep body inert. |
| Replace active meal (cooking or inventory eating) | Cancel/default cancellation; Cook & eat/eat confirms | Same, with separate inventory/cooking variants | Keep body inert. |
| Save/load slot browser | Close/default cancellation when not busy; explicit slot action | Close or exploration Back; custom full-screen overlay has no default outside dismissal | Unify busy/close rules; never map body tap to load/delete. |
| Overwrite / delete / load / quickload confirmation | Cancel/default cancellation; explicit confirmation | Cancel/default cancellation where enabled; confirmLoad option controls load confirmation | Keep explicit; document when load confirmation is enabled. |
| Controls guide | Close/default cancellation; H opens it | Controls provided through input/menu UI; no matching desktop-style modal | Keep explicit close. |
| Key-binding capture | Cancel/Escape/default cancellation; key capture applies binding | Desktop-only | Keep cancel separate from captured key. |
| Settings window (title/hub) | Close/default cancellation | Close/default cancellation | Keep. |
| Astra navigation | Close/default cancellation even while transit title exists | Close controls; default cancellation guarded while transitTitle is non-null | Resolve transit cancellation policy; currently differs. |
| Simulation deck | Close/default cancellation; selecting program enters combat | Same | Keep explicit selection. |
| Tape/film deck | Close/default cancellation; playback controls do not close | Same | Keep. |
| Recreation / restored cabinets / prizes | Close/default cancellation; select activity | Same | Keep. |
| Cargo manifest / relic array / catch log / milestone gallery | Close/default cancellation; archive tabs stay open | Close/Back; facility/catch are Dialogs, gallery is custom overlay | Keep browsing stable under taps. |
| Fishing result / secured catch / failed catch | Continue or Space returns to setup; explicit exit leaves fishing | Fish Again or Return to Explore; explicit result choices | Review button naming and next destination together; keep restart versus exit explicit. |
| Fishing catch journal | Close/default cancellation/Escape | Fishing journal is embedded in fishing screen rather than matching desktop modal | Keep explicit close for modal variant. |
| Arcade start / help / pause / game-over / result panels (all six games) | Named action button; Back/Escape path pauses active play, then exits from paused/finished state | Named action button; Back pauses active play, then exits | Keep game-specific state transitions; body tap should not restart/spend rewards. |
| Hover tooltips: exits, minimap, hub nodes, items, skills, status, command buttons, menu controls | Disappear when hover leaves; framework TooltipArea wrappers | No hover equivalent required for touch; details dialogs/labels provide access | Keep transient hover behavior; do not add Continue. |

The direct-container appendix below supplies per-variant source links, callbacks and confirmation controls, including legacy implementations. Custom full-screen menus and arcade overlays are included in this review table even though they do not construct Dialogs.

## Title, debug, and demo surfaces

| Surface / variants | Current dismissal | Alignment recommendation |
| --- | --- | --- |
| New-game / campaign-protocol chooser | Both: Cancel/default dialog cancellation; explicit new-game/protocol choice | Keep explicit because starting can overwrite autosave. |
| Debug scenario chooser / procedure review | Both: Close/default cancellation; Back inside review returns to list; Launch test proceeds | Keep nested review Back separate from leaving chooser. |
| Scenario unavailable / playtest setup failed | Close/Return to Main Game/default cancellation | Information-card candidate, but leaving isolated session should stay explicit. |
| Sampler showcase chooser | Android AlertDialog: Close/default cancellation; explicit demo selection | Keep selection explicit. |
| Demo pause menu | Android: close/default cancellation; guide/finish actions explicit | Keep. |
| Demo opening guide / reopened guide | Android: dismissal exits before begun; after begun it closes guide; explicit Begin/Resume | Flag: one dismissal gesture has different effects depending on begun state. |
| Demo finish / choose next demo | Finish AlertDialog ignores onDismissRequest; explicit navigation buttons required; chooser has normal cancellation | Keep deliberate ending choice, explain it visually. |
| Homecoming conversation | Android Dialog onDismissRequest completes; dialogue body reveals/advances | Review whether Back/outside should complete the whole scene. |
| Playtest walkthrough viewer | Android full-screen Dialog: Return to game/default cancellation | Keep. |
| Walkthrough room/quest/search picker | Android AlertDialog: select or default cancellation | Keep. |
| Walkthrough reset checklist | Cancel/default cancellation; explicit reset | Keep body inert. |
| Playtest note editor / notes report | Close/default cancellation; explicit save/copy/export controls | Keep editor taps safe. |
| Environmental preview and preset menu | Preview close/Back path; preset DropdownMenu selects or cancels | Keep tool behavior. |

## Proposed common rules

1. **Acknowledge information:** tap/click the card body or Continue; desktop Enter/Space and Escape, mobile Back should acknowledge the top card. Apply to tutorials, milestones, level-up, rest, blocked-route and ordinary inspection cards. Preserve authored restrictions deliberately rather than bypassing them through a root handler.
2. **Read story:** first tap reveals text, next advances. Choices remain explicit; skip remains deliberate. Standard desktop cinematics need parity here.
3. **Choose or confirm:** body never performs the action. Explicit action button proceeds; Cancel, outside, Escape/Back cancel when the workflow permits it.
4. **Browse:** scroll/tap content freely; Close, Escape/Back or outside dismisses the top tool, with nested views backing out one level.
5. **Guide an action:** required-action hints clear when that action completes. They should not look like tappable acknowledgements.
6. **Transient feedback:** timed notices expire; optional tap/X dismisses early. Hover tooltips disappear when the pointer leaves. Scene transitions finish automatically.

Suggested next alignment pass: information-card body/keyboard parity first; standard cinematic reveal/advance second; quest notifications and combat result pacing third. The wider alignment remains a design recommendation; this pass implements tutorial acknowledgements.

## Validation

- Six desktop checks pass: three tutorial-dismissal tests plus the existing three Nova tutorial flow tests. They cover body/button exactly-once callbacks across all three combat explanation stages, required-action hints remaining active, and real first-AP/skill-purchase progression.
- Android production code and three new instrumentation cases compile. Debug APK assembles. Instrumentation was not run on the emulator during this pass, preserving the current play session.
- Remaining rows describe source wiring; cross-platform device acceptance remains open.

## Source call-site inventory

The scan found 93 direct dialog/menu/tooltip containers. Shared wrappers and legacy versions are retained rather than counted as distinct player experiences. The review tables above cover custom foreground cards which do not use a Dialog. This appendix records callback wiring, not a claim that every surface was device-tested. Full expressions are in [the JSON inventory](POPUP_DISMISSAL_CALLSITES_2026-10-15.json).

| Platform | Source | Container / title expression | Dismissal callback | Explicit properties |
| --- | --- | --- | --- | --- |
| Desktop | [DesktopCombatChrome.kt:44](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCombatChrome.kt#L44) | Dialog | {} | DialogProperties( dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false ) |
| Desktop | [DesktopCombatLog.kt:194](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCombatLog.kt#L194) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Desktop | [DesktopCombatScreen.kt:945](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCombatScreen.kt#L945) | AlertDialog / { Text(hint.context ?: "Nova's build") } | runtime::dismissBuildHint | Framework defaults |
| Desktop | [DesktopControlsDialog.kt:18](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopControlsDialog.kt#L18) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopCookingScreen.kt:53](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopCookingScreen.kt#L53) | AlertDialog / { Text("Replace your active meal?") } | { pendingMeal = null } | Framework defaults |
| Desktop | [DesktopExplorationMinimap.kt:33](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationMinimap.kt#L33) | TooltipArea | Pointer leaves hover region (framework tooltip) | Framework defaults |
| Desktop | [DesktopExplorationModal.kt:23](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationModal.kt#L23) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopExplorationScreen.kt:311](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationScreen.kt#L311) | AlertDialog / { Text(offer.title) } | runtime::dismissRoomObject | Framework defaults |
| Desktop | [DesktopExplorationScreen.kt:317](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationScreen.kt#L317) | AlertDialog / { Text("A Quiet Rest") } | { restNotice = null } | Framework defaults |
| Desktop | [DesktopExplorationStyle.kt:38](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationStyle.kt#L38) | TooltipArea | Pointer leaves hover region (framework tooltip) | Framework defaults |
| Desktop | [DesktopExplorationStyle.kt:67](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopExplorationStyle.kt#L67) | TooltipArea | Pointer leaves hover region (framework tooltip) | Framework defaults |
| Desktop | [DesktopFieldMenuDialog.kt:418](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopFieldMenuDialog.kt#L418) | AlertDialog / { Text(allItems[id]?.name ?: id) } | { useItemId = null } | Framework defaults |
| Desktop | [DesktopFishingRuntimeScreen.kt:1209](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopFishingRuntimeScreen.kt#L1209) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopHubMapScene.kt:158](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopHubMapScene.kt#L158) | TooltipArea | Pointer leaves hover region (framework tooltip) | Framework defaults |
| Desktop | [DesktopHubScreen.kt:203](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopHubScreen.kt#L203) | Dialog | { settingsOpen = false } | DialogProperties(usePlatformDefaultWidth = false) |
| Desktop | [DesktopKeyboardSettings.kt:69](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopKeyboardSettings.kt#L69) | AlertDialog / { Text(action.label) } | { editing = null } | Framework defaults |
| Desktop | [DesktopMainMenuScreen.kt:350](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopMainMenuScreen.kt#L350) | AlertDialog / { Text("Start New Game?", color = Color.White, fontWeight = FontWeight.Bold) } | { showNewGameConfirm = false } | Framework defaults |
| Desktop | [DesktopMainMenuScreen.kt:396](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopMainMenuScreen.kt#L396) | AlertDialog / { Text("Scenario unavailable") } | { debugScenarioError = null } | Framework defaults |
| Desktop | [DesktopMainMenuScreen.kt:580](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopMainMenuScreen.kt#L580) | AlertDialog / { Text("DEBUG SCENARIOS", color = TitleCyan, fontWeight = FontWeight.Black) } | onDismiss | Framework defaults |
| Desktop | [DesktopMainMenuScreen.kt:658](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopMainMenuScreen.kt#L658) | AlertDialog / { Text("SETTINGS & CONFIGURATION", color = TitleCyan, fontWeight = FontWeight.Black) } | onDismiss | Framework defaults |
| Desktop | [DesktopModComparison.kt:26](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopModComparison.kt#L26) | DropdownMenu | Positional dismissal callback: expanded, { expanded = false } | Framework defaults |
| Desktop | [DesktopModTransferDialog.kt:14](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopModTransferDialog.kt#L14) | AlertDialog / { Text("Move $name?") } | onDismiss | Framework defaults |
| Desktop | [DesktopRoomInteractionDialog.kt:35](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRoomInteractionDialog.kt#L35) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopRoomInteractionDialog.kt:73](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRoomInteractionDialog.kt#L73) | AlertDialog / { Text(if (info == null) "No quicksave available" else "Load quicksave?") } | onDismiss | Framework defaults |
| Desktop | [DesktopRuntimeGearContent.kt:61](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeGearContent.kt#L61) | AlertDialog / { Text(selected.name) } | { detailsOpen = false } | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:15](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L15) | AlertDialog / { Text("Explore the Astra") } | runtime::dismissBurgQuestAstraExitDialog | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:20](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L20) | AlertDialog / { Text("Recreation") } | runtime::closeAstraRecreation | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:32](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L32) | AlertDialog / { Text(facility.title) } | runtime::closeAstraFacility | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:46](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L46) | AlertDialog / { Text("Astra Catch Log") } | runtime::closeCatchLog | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:64](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L64) | AlertDialog / { Text("Milestone gallery") } | runtime::closeMilestoneGallery | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:72](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L72) | AlertDialog / { Text("Astra navigation") } | runtime::dismissAstraNavConsole | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:83](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L83) | AlertDialog / { Text("Simulation deck") } | runtime::dismissSimulationDeck | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:99](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L99) | AlertDialog / { Text("Great Frontier tapes") } | runtime::dismissTapeDeck | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:114](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L114) | AlertDialog / { Text("${level.characterName} · Level ${level.newLevel}") } | runtime::dismissLevelUpPrompt | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:119](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L119) | AlertDialog / { Text("${tree.characterName} · ${tree.availableAp} AP available") } | runtime::closeSkillTreeOverlay | Framework defaults |
| Desktop | [DesktopRuntimeOverlays.kt:135](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopRuntimeOverlays.kt#L135) | AlertDialog / { Text(quest.title) } | runtime::closeQuestDetails | Framework defaults |
| Desktop | [DesktopSaveLoadDialog.kt:47](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopSaveLoadDialog.kt#L47) | Dialog | { if (!busy) onDismiss() } | Framework defaults |
| Desktop | [DesktopSaveLoadDialog.kt:92](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopSaveLoadDialog.kt#L92) | AlertDialog | { confirmation = null } | Framework defaults |
| Desktop | [DesktopShopDialog.kt:156](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopShopDialog.kt#L156) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopTinkeringContent.kt:44](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopTinkeringContent.kt#L44) | AlertDialog / { Text("Scrap ${item.name}?") } | { pendingScrap = null } | Framework defaults |
| Desktop | [DesktopTuningPuzzleDialog.kt:74](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopTuningPuzzleDialog.kt#L74) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopTuningPuzzleDialog.kt:106](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopTuningPuzzleDialog.kt#L106) | Dialog | onDismiss | Framework defaults |
| Desktop | [DesktopTuningPuzzleDialog.kt:150](../../desktopApp/src/main/kotlin/com/example/starborn/desktop/ui/DesktopTuningPuzzleDialog.kt#L150) | Dialog | onDismiss | Framework defaults |
| Android | [CombatScreen.kt:1259](../../app/src/main/java/com/example/starborn/feature/combat/ui/CombatScreen.kt#L1259) | AlertDialog / { androidx.compose.material3.Text(hint.context ?: "Nova's build") } | viewModel::dismissBuildHint | Framework defaults |
| Android | [CommandPalette.kt:430](../../app/src/main/java/com/example/starborn/feature/combat/ui/components/CommandPalette.kt#L430) | AlertDialog / { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text( text = "Items", style = Material… | onDismiss | Framework defaults |
| Android | [StatusEffectsRow.kt:379](../../app/src/main/java/com/example/starborn/feature/combat/ui/components/StatusEffectsRow.kt#L379) | AlertDialog / { Text("Active effects") } | onDismiss | Framework defaults |
| Android | [CookingScreen.kt:49](../../app/src/main/java/com/example/starborn/feature/crafting/ui/CookingScreen.kt#L49) | AlertDialog / { Text("Replace your active meal?") } | { pendingMeal = null } | Framework defaults |
| Android | [ModComparisonPanel.kt:26](../../app/src/main/java/com/example/starborn/feature/crafting/ui/ModComparisonPanel.kt#L26) | DropdownMenu | Positional dismissal callback: expanded, { expanded = false } | Framework defaults |
| Android | [ModTransferDialog.kt:14](../../app/src/main/java/com/example/starborn/feature/crafting/ui/ModTransferDialog.kt#L14) | AlertDialog / { Text("Move $name?") } | onDismiss | Framework defaults |
| Android | [TinkeringScreen.kt:254](../../app/src/main/java/com/example/starborn/feature/crafting/ui/TinkeringScreen.kt#L254) | AlertDialog / { Text("Scrap ${item.name}?") } | { pendingScrap = null } | Framework defaults |
| Android | [TinkeringScreen.kt:1356](../../app/src/main/java/com/example/starborn/feature/crafting/ui/TinkeringScreen.kt#L1356) | AlertDialog / { Text(title) } | onDismiss | Framework defaults |
| Android | [AstraRoomPanels.kt:29](../../app/src/main/java/com/example/starborn/feature/exploration/ui/components/AstraRoomPanels.kt#L29) | Dialog | onClose | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [GearSelectionDialogs.kt:52](../../app/src/main/java/com/example/starborn/feature/exploration/ui/components/GearSelectionDialogs.kt#L52) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [ModernGearPicker.kt:39](../../app/src/main/java/com/example/starborn/feature/exploration/ui/components/ModernGearPicker.kt#L39) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [PartyMemberDetailsDialog.kt:48](../../app/src/main/java/com/example/starborn/feature/exploration/ui/components/PartyMemberDetailsDialog.kt#L48) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [SkillTreeOverlay.kt:45](../../app/src/main/java/com/example/starborn/feature/exploration/ui/components/SkillTreeOverlay.kt#L45) | Dialog | onClose | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [TogglePromptDialog.kt:49](../../app/src/main/java/com/example/starborn/feature/exploration/ui/components/TogglePromptDialog.kt#L49) | AlertDialog / { Text(title) } | onDismiss | Framework defaults |
| Android | [ExplorationScreen.kt:1122](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1122) | AlertDialog / { Text("Route blocked") } | viewModel::dismissBlockedPrompt | Framework defaults |
| Android | [ExplorationScreen.kt:1127](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1127) | AlertDialog / { Text("Room actions") } | viewModel::dismissRoomActions | Framework defaults |
| Android | [ExplorationScreen.kt:1150](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1150) | AlertDialog / { Text(offer.title) } | viewModel::dismissRoomObject | Framework defaults |
| Android | [ExplorationScreen.kt:1382](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1382) | AlertDialog / { Text("Replace your meal?") } | { pendingMealReplacement = null } | Framework defaults |
| Android | [ExplorationScreen.kt:1481](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1481) | AlertDialog / { Text( text = "Return to Title?", style = MaterialTheme.typography.titleMedium, color = FieldMenuD… | { showExitConfirmDialog = false } | Framework defaults |
| Android | [ExplorationScreen.kt:1520](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1520) | AlertDialog / { Text( text = "Astra Docked", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.… | { viewModel.dismissBurgQuestAstraExitDialog() } | Framework defaults |
| Android | [ExplorationScreen.kt:1691](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1691) | Dialog | viewModel::closeAstraFacility | androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false) |
| Android | [ExplorationScreen.kt:1718](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L1718) | Dialog | viewModel::closeCatchLog | androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false) |
| Android | [ExplorationScreen.kt:3158](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L3158) | AlertDialog / { Text(puzzle.title) } | onDismiss | Framework defaults |
| Android | [ExplorationScreen.kt:3514](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L3514) | Dialog | onDismiss | Framework defaults |
| Android | [ExplorationScreen.kt:3665](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L3665) | Dialog | { if (transitTitle == null) onDismiss() } | Framework defaults |
| Android | [ExplorationScreen.kt:3784](../../app/src/main/java/com/example/starborn/feature/exploration/ui/ExplorationScreen.kt#L3784) | Dialog | onDismiss | Framework defaults |
| Android | [InventoryTabContent.kt:1308](../../app/src/main/java/com/example/starborn/feature/exploration/ui/tabs/InventoryTabContent.kt#L1308) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [InventoryTabContent.kt:1418](../../app/src/main/java/com/example/starborn/feature/exploration/ui/tabs/InventoryTabContent.kt#L1418) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [ModernTinkerContent.kt:258](../../app/src/main/java/com/example/starborn/feature/exploration/ui/tabs/ModernTinkerContent.kt#L258) | AlertDialog / { Text("Scrap ${item.name}?") } | { pendingId = null } | Framework defaults |
| Android | [TinkerTabContent.kt:1259](../../app/src/main/java/com/example/starborn/feature/exploration/ui/tabs/TinkerTabContent.kt#L1259) | AlertDialog / { Text("Scrap ${item.name}?") } | { pendingScrap = null } | Framework defaults |
| Android | [BurgQuestHomecoming.kt:41](../../app/src/main/java/com/example/starborn/feature/mainmenu/ui/BurgQuestHomecoming.kt#L41) | Dialog | onComplete | Framework defaults |
| Android | [MainMenuScreen.kt:558](../../app/src/main/java/com/example/starborn/feature/mainmenu/ui/MainMenuScreen.kt#L558) | AlertDialog / { Text("Select Campaign Protocol", color = Color.White, fontWeight = FontWeight.Bold) } | { showNewGameConfirm = false } | Framework defaults |
| Android | [MainMenuScreen.kt:745](../../app/src/main/java/com/example/starborn/feature/mainmenu/ui/MainMenuScreen.kt#L745) | AlertDialog / { Text("Settings", fontWeight = FontWeight.Black) } | onDismiss | Framework defaults |
| Android | [MainMenuScreen.kt:802](../../app/src/main/java/com/example/starborn/feature/mainmenu/ui/MainMenuScreen.kt#L802) | AlertDialog / { Text("Debug Scenarios", fontWeight = FontWeight.Black) } | onDismiss | Framework defaults |
| Android | [MainMenuScreen.kt:938](../../app/src/main/java/com/example/starborn/feature/mainmenu/ui/MainMenuScreen.kt#L938) | AlertDialog / { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("Starborn Sampler Showcase", font… | onDismiss | Framework defaults |
| Android | [WalkthroughViewer.kt:70](../../app/src/main/java/com/example/starborn/feature/playtest/WalkthroughViewer.kt#L70) | Dialog | onReturnToGame | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [WalkthroughViewer.kt:175](../../app/src/main/java/com/example/starborn/feature/playtest/WalkthroughViewer.kt#L175) | AlertDialog / { Text(mode) } | { picker = null } | Framework defaults |
| Android | [WalkthroughViewer.kt:196](../../app/src/main/java/com/example/starborn/feature/playtest/WalkthroughViewer.kt#L196) | AlertDialog / { Text("Start a fresh checklist?") } | { resetOpen = false } | Framework defaults |
| Android | [WalkthroughViewer.kt:201](../../app/src/main/java/com/example/starborn/feature/playtest/WalkthroughViewer.kt#L201) | AlertDialog / { Text("Playtest note") } | { noteOpen = false } | Framework defaults |
| Android | [WalkthroughViewer.kt:221](../../app/src/main/java/com/example/starborn/feature/playtest/WalkthroughViewer.kt#L221) | AlertDialog / { Text("World ${guide.world} notes (${array.length()})") } | { notesOpen = false } | Framework defaults |
| Android | [ShopScreen.kt:948](../../app/src/main/java/com/example/starborn/feature/shop/ui/ShopScreen.kt#L948) | AlertDialog / { Text(title) } | onDismiss | Framework defaults |
| Android | [BurgQuestNavigationHost.kt:236](../../app/src/main/java/com/example/starborn/navigation/BurgQuestNavigationHost.kt#L236) | AlertDialog / { Text("Demo menu") } | { showDemoMenu = false } | Framework defaults |
| Android | [BurgQuestNavigationHost.kt:245](../../app/src/main/java/com/example/starborn/navigation/BurgQuestNavigationHost.kt#L245) | AlertDialog / { Text(if (launch.isHomecoming) "Make yourself at home" else scenario.title) } | { if (begun) { showGuide = false } else onExit() } | Framework defaults |
| Android | [BurgQuestNavigationHost.kt:292](../../app/src/main/java/com/example/starborn/navigation/BurgQuestNavigationHost.kt#L292) | AlertDialog / { Text(if (ending == BurgQuestEnding.FINISHED) "Until the next adventure" else "Starborn Sampler") } | {} | Framework defaults |
| Android | [BurgQuestNavigationHost.kt:348](../../app/src/main/java/com/example/starborn/navigation/BurgQuestNavigationHost.kt#L348) | AlertDialog / { Text("Playtest setup failed") } | onExit | Framework defaults |
| Android | [ItemTargetSelectionDialog.kt:51](../../app/src/main/java/com/example/starborn/ui/components/ItemTargetSelectionDialog.kt#L51) | Dialog | onDismiss | DialogProperties(usePlatformDefaultWidth = false) |
| Android | [SaveLoadDialog.kt:233](../../app/src/main/java/com/example/starborn/ui/components/SaveLoadDialog.kt#L233) | AlertDialog / { Text( text = "Confirm Action", style = MaterialTheme.typography.titleMedium.copy(fontWeight = Fon… | { confirmAction = null } | Framework defaults |
| Android | [SkillsDialog.kt:69](../../app/src/main/java/com/example/starborn/ui/dialogs/SkillsDialog.kt#L69) | AlertDialog / { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row( verticalAlignment = Alignment.Cen… | onDismiss | Framework defaults |
| Android | [SkillsDialog.kt:322](../../app/src/main/java/com/example/starborn/ui/dialogs/SkillsDialog.kt#L322) | AlertDialog / { Text( text = skill.name, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWei… | onDismiss | Framework defaults |
| Shared | [EnvironmentalEffectsPreview.kt:65](../../shared/src/androidJvmMain/kotlin/com/example/starborn/ui/environment/EnvironmentalEffectsPreview.kt#L65) | DropdownMenu | Positional dismissal callback: dropdown,{dropdown=false},Modifier.heightIn(max=320.dp) | Framework defaults |
