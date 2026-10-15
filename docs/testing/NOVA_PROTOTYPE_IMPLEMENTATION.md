# Nova progression prototype

October 15 scope update: [all four crew trees are now implemented](CREW_PROTOTYPE_IMPLEMENTATION.md), with earned checkpoints and free build resets. Nova-only limitations below describe the earlier slice.

October 14, 2026. Working-tree implementation of the first playable slice of [candidate 1](../design/PROGRESSION_DESIGN_PACKET_CANDIDATE_1.md). This is an opt-in test-session feature, not a production-tree rollout or a claim of proven balance.

## Implemented behavior

Nova has two four-node paths costing 1/2/3/4 AP. Fourth nodes require World 4 completion. Grounded Wire changes Arc Tether stability damage; Controlled Vent raises Cryo Vent to 65 power; Breaker Bolt is a purchased single-target attack with extra stability damage; Exposed Seam rewards Weak/Brittle targets. Quiet Steps adds Speed; Scatter Charge is a purchased all-enemy attack; Hot Start rewards high-HP targets; Keep Moving grants a limited kill-triggered Shield. Direct HP and stability bonuses are resolved independently with their specified caps. Prototype Cryo Vent applies Brittle without implicitly applying Frozen.

Quest/recruitment AP awards are claimed once per run. Purchases atomically spend AP and store paid receipts separately from freely learned skills. Receipts, award claims and prototype version persist through protobuf saves. NG+ preserves AP, purchases, learned skills and schematics while resetting run-specific award claims and story progress. Legacy node migration/refunds and development respec are not implemented in this slice. Other crew retain core skills; their AP trees are unavailable in prototype sessions.

Party/menu HP now uses the same gear, passive and meal maximum as combat, and open member details refresh when that maximum changes. Base attribute headings distinguish the displayed base values. These HP and NG+ corrections also apply to normal campaigns; normal trees and AP reward schedules remain unchanged.

Primary implementation: `NovaBuildPrototype.kt`, `TalentEffects.kt`, `CombatActionProcessor.kt`, `CombatController.kt`, `GameSessionStore.kt`, `GameSessionPersistence.kt`, `GameBootstrap.kt`, `ExplorationProgressionFormatter.kt` and platform launchers.

## Play the prototype

Android debug build: open Debug Scenarios and select **Nova / Specializations Prototype**. It starts an isolated opening session with no free AP or resource grants. Earn quest AP, then open Nova's Skills from the party menu. Normal debug saves remain separate through the existing isolated-test-session mechanism.

Desktop, from the repository with the Android Studio JBR configured as `JAVA_HOME`:

```powershell
.\gradlew.bat :desktopApp:run -Pstarborn.novaPrototype=true
```

Start a new game in that process. Prototype saves use the `nova-build-prototype` subdirectory of the desktop save directory. Do not manually copy prototype saves into a normal campaign while the feature is experimental.

## Verification

The full suites pass 665 Android-hosted tests and 150 desktop tests, with zero failures, errors or skips. Android debug assembly also succeeds. New focused checks cover talent HP/stability separation and caps, threshold behavior, targeting, cooldown, Cryo status, kill limits, AP idempotence, prerequisite/story gates, purchase ownership, protobuf round trips and actual bootstrap NG+ behavior.

Two public-controller routes complete Worlds 1 and 2 at seed 17, one for each path, using real combat and earned purchases with no assisted actions. Each earns 16 AP: 14 main-route points plus 2 from the optional Heavy Lifting quest; buys three nodes for 6 AP; and retains 10. Both purchased attacks are actually used. Route logs record damage events, resources, HP and an intentional retreat that leaves its enemy undefeated. Reports are generated at `app/build/reports/progression-prototype/saboteur.txt` and `improviser.txt`.

Desktop checks exercise the visible Skills purchase button, effective menu/combat HP, owned skill power/Speed, live meal-HP refresh, actual service NG+, and disk save/load. The menu capture is generated at `desktopApp/build/reports/progression-prototype/nova-tree.png` and was visually reviewed for readable details and purchase state.

Validation commands:

```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain -I tmp/game-evaluation.init.gradle
.\gradlew.bat :desktopApp:test :app:assembleDebug --console=plain -I tmp/game-evaluation.init.gradle
```

The init script redirects desktop test reports into `tmp/game-evaluation-results/desktopApp/test/`; Android XML results remain under `app/build/test-results/testDebugUnitTest/`. It does not change gameplay. The Android artifact is `app/build/outputs/apk/debug/app-debug.apk`.

## Remaining evidence

These routes use one seed and authored decisions. They establish purchase-to-combat execution, not equal power, interesting human choices, or final difficulty tuning. Other crew AP purchases are unavailable, so this slice cannot establish party-wide AP allocation balance. Capstones have focused synthetic combat/gate checks, not earned late-game route coverage. Battle checkpoint recovery restarts from the pre-battle session; live mid-battle talent trigger serialization is not added. No physical-phone playthrough or process-death acceptance is claimed.

Next: human playtests of both Nova paths, varied seeds and resource pressure, then matched late-game/capstone checks before implementing the other crew. Keep production migration separate until the complete design and preservation behavior are verified.

## October 15: first-purchase guidance and desktop Break parity

The first available AP now offers a one-time explanation of the party's shared AP budget, separate story/level skills, and the option to save points. Its action opens Nova's Skills in the regular Party menu. The tree compares Grounded Wire and Quiet Steps until the first purchase; it explains selecting a node, spending AP, automatic perks and combat Abilities. Nothing is purchased automatically. Turning tutorials off discards the pending explanation without completing it; reenabling them offers it again when eligible.

Grounded Wire gets a brief combat explanation after an Arc Tether hit actually damages stability. Quiet Steps gets one after Nova acts with the perk owned. Both wait until the existing combat tutorial is inactive, pause ATB while visible, and record acknowledgment in the existing persisted tutorial state. Completed guidance does not repeat after save/load. This guidance applies to the opt-in prototype; other crew paths remain pending.

Desktop now uses the mobile-authored Break burst and persistent purple cracked field with orbiting dazed stars. The effect bodies live in `shared/.../shared/combat/StabilityBreakEffects.kt`; Android's existing components delegate to them. Desktop adds the field while `breakTurns > 0`, removes it on recovery/death, uses mobile's slumped/dazed motion, and shows the red broken stability icon/bar. Reduced flashes attenuate the burst and hold the field's pulse steady; disabled screenshake stops sprite sway. These presentation changes do not alter Break rules or damage.

New checks cover the first earned award, no automatic purchase, direct opening of Skills, dismissal/settings behavior, real combat pause/resume after animation completion, persisted acknowledgment, mobile guidance/purchase UI, and desktop field visibility through recovery/death. Captures are generated under `desktopApp/build/reports/progression-prototype/tutorial-*.png`, `desktopApp/build/reports/combat-clarity/stability-*.png`, and the emulator's app external-files `nova-tutorial-test/` directory. Phone tests use `IsolatedProgressionContext`, separate from the owner's saves.

Device-test compilation also required updating four stale existing test references (moved presentation helpers and the sampler dialog's named callbacks). A Windows mapped-file error during narrative-report export was corrected by writing a temporary complete report and atomically replacing the destination. That change is limited to the test auditor.

Final October 15 verification: 667 Android-hosted tests pass with no failures/errors/skips, and debug/app-test assembly succeeds. The desktop full run passed all 150 preexisting tests and three new tutorial tests; its remaining new field test had a merged-semantics selector failure. After correcting that selector, all eight focused prototype/tutorial/Break checks pass, including the final stability icon/bar changes. A further three-test tutorial rerun verifies the regular-menu handoff without a duplicate Party prompt. The isolated phone instrumentation test passes the explanation, menu/choice presentation and actual purchase. Captures were visually reviewed; this remains emulator/UI evidence, not a physical-device or human balance claim.
