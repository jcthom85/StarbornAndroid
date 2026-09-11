# Focused Phase 4 checks

## Large-text regression checks

`phase4_large_text_combat.yaml` passed end to end on 2026-09-11 at system font
scale 2.0 after installing the stacked detail-value fix. Screenshots confirm
level-2/max Overcharge, full cooldown suffix/traits, Plasma Burst name and
controls, detail access, and scrolling back to Smoke Bomb. This closes the
scoped crowded/Overcharge emulator check. Physical-phone acceptance remains.
The crowded-list helper uses 70% visibility for names because the 100% scroll
threshold failed even with visible label bounds; review the separate name and
control screenshots. `phase4_resume_crowded.yaml` is only a checkpoint helper
for Nova's command menu at max charge, not a standalone scenario initializer.

`phase4_large_text.yaml` groups the Loader ability list/details, opening
journal, and starter loadout flows. It requires system `font_scale` 2.0.
Passed end to end on the Android 17 Medium Phone emulator on 2026-09-11;
screenshots reviewed and original font scale restored. The absent optional
third Continue produced a warning, not a failed required action. This closes
these representative opening-screen checks, not crowded/Overcharge large-text
combat or physical-device acceptance.
Record the original value before changing it and restore it in a `finally`
block after Maestro finishes (including on failure). This emulator's original
value is 1.0. These flows do not clear app data or save slots, but each launches
a new unsaved debug session. Do not run the whole Maestro directory.

`phase4_loadout.yaml` checks empty equipment presentation, selects Mining
Pistol and Flux Liner, and captures selectors and equipped tiles. This proves
debug UI interaction, not campaign acquisition or affordability. At large text
sizes the inventory categories scroll horizontally, and gear details scroll
vertically. The other large-text fixes put scenario headers in the scrollable
list, place ability controls below their text at font scales >= 1.5, and wrap
quest badges and stage titles.

Semantic visibility can include text at a clipped viewport edge. Review the
screenshots, especially final objectives, equipped armor and detail cooldowns;
do not treat a passing selector alone as visual acceptance. The opening helper
now adds a short content swipe, and Loader details scroll through Combat traits.

`AbilityRowInstrumentedTest` additionally checks blocked/recovered actions at
270 dp with 1.0 and 2.0 font scales, including stacked large-text controls.
Both tests passed after the responsive layout change. Running the assembled
test APK directly with `adb shell am instrument` avoids Gradle connected-test
cleanup removing the game from the shared emulator. Never uninstall or clear
the game to prepare these checks.

## Physical-device Phase 4 acceptance (pending)

Use a build containing the current UI fixes; the previously uploaded internal
1.3.25 (109) bundle predates these uncommitted changes. Record the build/commit,
phone model, Android version, display size, font size, and date with results.
Use a test device/profile or first preserve any in-progress game. Do not clear
app data, uninstall the game, or overwrite a player's save to prepare a test.
Debug scenarios replace unsaved progress; only use them in a disposable session.

- At normal text size and the phone's largest supported text size, open the
  field menu and dismiss opening inventory/gear tutorials. Controls must remain
  reachable and explanatory text must wrap or scroll without hidden endings.
- In Gear, select and equip the starter weapon/armor; inspect names, mod-lock
  explanation, selector scrolling, and category navigation (including Key Items).
- Open Wake Up Call: read its overview, stage guidance and last objective;
  verify the status badges and Back/Close navigation.
- In combat, read ability names, power/cooldown summaries, disabled explanations,
  and target details. Scroll a crowded list in both directions. At level-2 and
  max Overcharge, verify banner text, detail scrolling and reachable controls.
- Confirm disabled Use does nothing, Details remains usable, and an available
  action is usable again after its blocker is removed. Record any scenario
  not reached as untested, rather than inferring it from emulator results.

Restore the original font/display settings afterward. Attach screenshots for
any clipping or unreachable control. This checklist does not close campaign
balance/economy, all-device accessibility, lifecycle/audio or performance work.

## Earlier normal-size checks

`phase4_overcharge_detail_scroll.yaml` starts from maximum-charge Arc Tether
details and scrolls down to Combat traits. Passed on 2026-09-11. The traits
screenshot verifies the full cooldown suffix, applied status and traits;
the initial cooldown selector alone can match while text is partly clipped.
This closes current-setting scroll access, not large-text verification.

`phase4_overcharge.yaml` starts the full-inventory First Combat checkpoint,
uses two basic attacks, and checks the level-2 Overcharge banner and Arc Tether
list/detail cooldown preview. Passed end to end on 2026-09-11; screenshots
show +50% power, one-turn refund and matching "1 turn after use (Overcharge)"
labels. This does not execute an overcharged skill or verify maximum charge.
The static prose-selector validator expands the actual cooldown formatter
template for singular/plural labels; the earlier false positives are resolved.
It retains app data but replaces the unsaved debug session.

`phase4_max_overcharge.yaml` reuses the level-2 flow and adds a third attack.
Passed end to end on 2026-09-11. List and detail Level 3 banners are readable;
the detail cooldown suffix is below the initial viewport and needs scrolling
verification. This checks presentation, not charge consumption or damage.

`phase4_opening_guidance.yaml` opens the Gear & Inventory checkpoint's journal,
dismisses menu hints, opens Wake Up Call, and verifies its grouped overview,
current-stage guidance and scroll access to the final cutter objective. Four
screenshots cover journal, overview, current stage and final objectives. Its
helper `phase4_assert_opening_guidance.yaml` starts at the top of quest details.
The debug scenario leaves earlier objectives unchecked despite placing Nova
at the workshop: use this for layout, not earned progression or route guidance
correctness. No tracking toggle, save, or app-data reset is performed.

`phase4_loader_labels.yaml` launches the Gear & Inventory debug scenario,
enters the Faulted Loader tutorial, and checks its briefing plus Arc Tether's
list/detail labels. It captures three screenshots for visual review; semantic
assertions alone do not prove that text is unclipped.

`phase4_loader_cooldown_refund.yaml` includes that flow, then uses Arc Tether
and checks that it is available again at the next command menu. The weakness
refund and turn-end reduction clear its two-turn cooldown. It checks an enabled
Use button, no "On cooldown" explanation, and matching list/detail labels.
It does not finish the fight or test disabled cooldowns, Overcharge, and other
unavailability reasons. A bounded retry handles the portrait being visible
before Nova is ready to act.

`phase4_nonweak_cooldown.yaml` uses the full-inventory First Combat checkpoint
and Arc Tether against Shock-resistant Echo-Borers. It checks a nonzero
cooldown, its explanation, disabled numeric button, and matching detail label.
It waits for the checkpoint's retained cinematic before traveling. The Use
selector is bounded by Arc Tether and Cryo Vent labels: Smoke Bomb is first in
this scenario, so selecting the first Use button would test the wrong skill.
This is debug UI coverage, not evidence of campaign-earned abilities or gear.
The assertion helper `phase4_assert_arc_cooldown.yaml` requires the abilities
dialog already open. It accepts one or two remaining turns without claiming
turn-timing correctness; the observed two-turn branch passed on 2026-09-11.
The number's Text node is enabled even when its parent button is disabled, so
the assertion checks a disabled parent containing that number. The complete
combined flow passed on 2026-09-11 after the ability-name wrapping fix. The
two-turn branch was exercised; the one-turn branch remains unexecuted.
Screenshots confirm Smoke Bomb and Plasma Burst names wrap in full. This
does not verify every ability, large fonts, or physical-device layouts.

The flow uses `clearState: false`. It replaces the current **unsaved** session;
do not run it while preserving an in-progress unsaved playthrough. It does not
explicitly save, load, or delete save slots. Other flows in this directory may
use `clearState: true`; do not run the entire directory against retained data.

Use the installed local Maestro CLI with `--udid emulator-5554 test
playtests/maestro/phase4_loader_labels.yaml` (and Android Studio's JBR as
`JAVA_HOME` when needed). Maestro writes logs and screenshots beneath its
reported test-output directory, which may require sandbox approval.

Scenario selection deliberately uses scrolling rather than search input:
Android's first-use handwriting keyboard intercepted input on the API 37.1
emulator. No keyboard or device settings need to be changed for this flow.

This is not a full campaign, combat balance, large-font, or physical-device
acceptance test. Remaining Phase 4 scope is tracked in
`docs/IMPROVEMENT_BACKLOG.md`.
