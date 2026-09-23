# BurgQuest focused prep - September 22, 2026

September 23 follow-up: [combat layout and compact demo controls](COMBAT_LAYOUT_BURGQUEST.md), shipped in version 1.3.53 (137).

Later September 23 local work: [guided sampler and crew homecoming](BURGQUEST_GUIDED_SAMPLER.md). This supersedes the visitor-route recommendation below; it has not been released. Debug Scenarios and Load Game are now hidden locally alongside New Game.

## Implementation shipped in 1.3.52 (versionCode 136)

Committed as `aeec426`, pushed to `feature/multiplatform-port`, and accepted/committed on the Google Play internal track on September 22, 2026.

- New Game remains temporarily hidden. BurgQuest Demo and Debug Scenarios remain available.
- Tactical Combat is first and recommended. Its briefing launches an actual two-enemy encounter (Siren Skimmer + Spore-Spitter), with all four crew members. The initial vine pair was replaced after visual inspection showed both used borrowed bat art.
- Tactical and Titan have deterministic booth-only equipment, eight unlocked skills total, three of each equipped snack and four/eight medkits respectively. No full-inventory grant, 50,000 credits or all-skills menu remains in these fixtures. Campaign enemies and numerical balance are unchanged by this prep pass.
- Titan's Vent Exposure banner now explicitly calls out the recovery window. The briefing explains shock and healing during recovery.
- Astra starts in the common room with six installed arcades. Its guide suggests one arcade round, then west to the cargo bay's Astra workbench. The previously descriptive bench now opens tinkering. Cryo-Inductor ingredients/tools and its schematic are provided.
- Story is labeled an unabridged opening preview, not a guaranteed five-minute route to combat.
- A guide and Finish demo control remain above the game. Combat results lead to a finish screen; fresh replay, another showcase and title-screen return are available. Combat pauses behind the guide. Finishing unmounts the live game/arcade screen; ending or restarting a visit clears its owned navigation/combat ViewModels and releases services.
- Title-screen demo launches (including BurgQuest entries selected in Debug Scenarios) create separate AppServices with `burgquest-demo/datastore`. Campaign services remain alive and untouched. A new visit ignores old demo saves and rebuilds its fixture. Demo save/load UI cannot access campaign slots. Stable `burgfest_*` internal identifiers remain for compatibility.

## Verification evidence

- Debug APK and instrumentation APK build successfully.
- Full unit suite: **551 tests, zero failures/errors** (September 22 run).
- Exact booth fixture simulation: **40/40 victories, zero timeouts** over seeds 1-10 for both encounters and two policies. Policies use either offensive skills or basic attacks, with healing/support available in both; “attack-focused” does not mean no healing. Production combat code and assets are used; presentation callbacks are simulated.
- Final Tactical pair simulated at **11.75-21 seconds** attack-focused and **7.75-15 seconds** skill-based. These are intentionally gentle introductory enemies with their own existing artwork, not a standalone five-minute challenge. The superseded vine-pair results are not the final fixture's timing.
- Titan simulated combat durations: **95.25-117.75 seconds** attack-focused; **35.75-44 seconds** skill-based. Two attack-focused runs lost Orion while still winning the encounter.
- These times exclude human reading, selection and most presentation delays. They do **not** certify a 5-10 minute newcomer experience. Use Tactical + Astra as the suggested short visit; Titan is an optional challenge and Story is a narrative alternative.
- Raw simulation evidence: `test-results/burgquest-prep/combat-fixtures.xml`.

## Offline emulator verification

Final streamed installation succeeded after an emulator reboot reclaimed retained installation space. Earlier attempts encountered `INSTALL_FAILED_INSUFFICIENT_STORAGE`. No saves were cleared and no apps were uninstalled. An initial instrumentation attempt against the old installed app failed with a constructor mismatch; it is not counted as successful evidence.

Packaged checks now pass for all four fixtures and reset stock/health, physical campaign save isolation (session/autosave/quicksave/manual slot), opening the Astra workbench and crafting its stocked recipe, battle checkpoint disk restoration, background/resume pause, and keeping combat paused behind the demo guide. UI inspection caught and fixed status-bar overlap and overlapping finish buttons. The full unit suite then caught a stale inline bench link; the link was corrected and all 551 tests passed again.

The UI combat driver initially got stuck targeting an enemy that had already disappeared; its target selection was corrected. Continuous arcade animation then blocked test-idle synchronization; the driver was adjusted and Finish now unmounts the live screen. Test-driver failures are not reported as player defeats or production combat stalls.

Final run on September 22, 20:50-20:52 EDT: **8/8 Android instrumentation tests passed offline**, in 105.853 seconds. Wi-Fi and mobile data were disabled for the run and restored afterward. This covered both readiness checks, both navigation checks, all three recovery/pause checks, and first-tap launches of all six arcade cabinets.

The complete visitor flow passed: title button visibility, guide reopening, fresh retry, actual UI-commanded Tactical victory (31.715 seconds with automated input, not newcomer timing), switching to Astra, arcade launch and finish, title return, Titan launch, Story launch, final title return, and unchanged campaign session/manual/quicksaves. Physical save namespace isolation is separately checked, including autosaves.

A further offline navigation rerun passed in 73.128 seconds after strengthening Story assertions to require Nova's Bunk and its bunk-light prompt. Settled screenshots confirm the opening room renders after its transition. Both connectivity settings were verified restored to enabled afterward.

Screenshots and UI trees: `test-results/burgquest-prep/burgquest-prep/`. Combat simulation: `test-results/burgquest-prep/combat-fixtures.xml`. A defeat-specific UI walkthrough and a full timed newcomer visit have not been certified. Actual venue noise and booth hardware remain operator checks.

## Booth recommendation and operator check

Start with **Tactical Combat**, then offer **Astra** for one arcade round and optional crafting. Offer **Titan** to visitors who want the tougher combat example. Treat **Story** as an alternative for narrative-minded visitors, not a guaranteed short fight route.

Before the event, on the actual booth device:

1. Run that Tactical -> Astra visit with a newcomer and time the whole visit, including reading and choosing actions. Aim for 5-10 minutes across the visit, not each scenario.
2. Check text/tap targets at the intended display size, music/voice/SFX levels with the booth speakers/headphones, charging and screen brightness.
3. Finish to title and give the next visitor a fresh demo. Keep the normal Load Game option away from the visitor script.
4. Install/confirm version 1.3.52 from the internal track on the booth device.

World/finale work remains paused at [the overall checkpoint](OVERALL_PLAN_CHECKPOINT.md).
