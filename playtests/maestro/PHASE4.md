# Focused Phase 4 checks

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
