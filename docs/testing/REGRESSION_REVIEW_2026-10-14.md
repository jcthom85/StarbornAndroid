# Regression review — October 14, 2026

Scope: reproduce the eight failures recorded in the earlier evaluation against the current working tree, investigate their causes, and validate the resulting changes. The tree already contains substantial uncommitted work; this review is not a release or a device playthrough.

## Findings and changes

| Check | Finding | Change |
|---|---|---|
| Combat controls and affinity radar | The old global absence check for `ACTING` can match an enemy's live action cue while Nova chooses a command. It failed in the earlier report and passed in the initial reproduction. | Scope choosing/acting assertions to Nova's combat figure. Keep action controls, recent actions, and weakness discovery checks. |
| Equipped mod transfer | The slot is labelled `Weapon mod 1`, rather than `Weapon modification 1`. | Use the current label; retain cancel, transfer, source removal, and inventory-count checks. |
| Compact tinkering | The station now stacks below 500 dp; the test uses 560 dp. | Exercise the stacked flow at 480 dp; retain scrap cancellation, consumption, salvage, workbench and schematic checks. |
| Fishing modal | The modal heading is now `CATCH LOG`. | Use the current heading; retain casting and cancellation checks. |
| Quest journal | Background text is intentionally expandable; fishing records have moved to Astra cargo records. Separately, completed objectives in the current stage are counted but hidden from readers and accessibility semantics. | Test background expansion/collapse and History navigation. Render completed and incomplete current-stage objectives with accessible descriptions. Existing Astra catch-log tests cover the relocated records. |
| Settings saves | The button is now `Manage saves`. | Use the current label; retain save-and-quit callback and persisted-save checks. |
| Heading/ready prompt | The prompt is now `Choose a character`; heading typography is unchanged. | Update prompt text while retaining wrapping, no-overflow, font size and weight checks. |
| World 2 persistent interactions | More than one authored variant may require a milestone; some states use base prose. `single` rejects both valid cases. | Select descriptions through the runtime resolver, retain persistent-name and action assertions, and cover combined beach/stasis milestones without restoring retired tasks. |
| BurgQuest common room | The initial reproduction also exposed an old expectation of six individual cabinet actions. Cabinets now launch through the simulation deck recreation console. | Verify common-room absence, galley filtering and session identity. Existing Astra navigation/recreation tests cover cabinet access. |

No production combat rules, catalog text, rewards, gates or save schemas were changed by this review. The production change is limited to current-stage objective visibility in the desktop journal.

## Verification

Initial reproduction: 651 Android-hosted tests ran, with two failures (catalog uniqueness and common-room cabinet expectations); 20 selected desktop tests ran, with six failures. The prior combat failure did not reproduce in this run, consistent with its unscoped live-enemy selector.

Final verification: **652 Android-hosted tests and 147 desktop tests passed, with zero failures, errors or skips.** The complete desktop suite passed after the dialog selector correction; the complete Android suite passed after removing an overly strict completion-variant assertion. The new combined-milestone test is included in the final Android count. The journal screenshot was inspected: completed and pending objectives are readable, with distinct icons and no visible clipping.

Commands use Android Studio's JBR as `JAVA_HOME` and `tmp/game-evaluation.init.gradle` to isolate desktop report output. Full desktop verification used `gradlew.bat :app:testDebugUnitTest :desktopApp:test --continue --console=plain -I tmp/game-evaluation.init.gradle`; the final Android-only rerun used `gradlew.bat :app:testDebugUnitTest --console=plain -I tmp/game-evaluation.init.gradle`. The Android task currently runs its full suite even when desktop test filters are supplied; use report counts rather than infer scope from the command.

The documentation inventory, local links and scoped `git diff --check` also passed. Existing Gradle and image-conversion deprecation warnings remain outside this regression pass.

These checks establish automated source/runtime and Compose behavior for their fixtures. They do not establish physical-device usability, campaign balance, or release readiness.
