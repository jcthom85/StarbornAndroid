# Narrative revision review

November 15–16, 2026. Implements the three editing batches from [the emotional continuity review](07_EMOTIONAL_CONTINUITY_AND_CHARACTER_REVIEW.md).

## What changed

- **Jed/Nova:** The active opening now shows Jed's two wrench taps and the lamp handoff. The spare exchange and farewell are preserved. Jed's optional Echo is a short remembered repair lesson with dialogue and an unanswered final response; it no longer repeats Vale's later offer to keep him alive.
- **Memory Stair:** The offered safe departure now concerns the cargo pod that brought Nova and Zeke to Sector 9. The screen says **cargo pod wreckage**. Internal Astra-named action/event/milestone identifiers remain intact for existing saves. The three refusals, their leave choices and their independent credit remain intact.
- **Shared memories and ending:** Bridge anchors recall the pod frame, Gh0st lowering his rifle, Orion's painted ceiling and Jed's workbench. The premature Colony morning and mandatory shared beach holiday are gone. The optional Tideglass span recalls Orion's coast rather than an unplayed crew photograph. The diner explicitly places its photograph after the adventure and recalls the witnessed Astra flight. The saucer, breakfast and lamp payoff remain.
- **Thorne/Vale:** Boggs names Thorne's pay freeze; Jax identifies her freight privilege. Her rooftop assistance keeps the Lens away from Vale. Gh0st demands the occupied-seat list and Nova refuses to restore Thorne's authority before accepting her route. The takeover, execution and final confrontation are retained.
- **Crew conflict:** Zeke's warning makes Nova wait for him to isolate the Lens return feed. Gh0st asks to read the Phantom record first, and Nova respects that boundary. These decisions change the scene without creating extra quest tasks.
- **Elara:** A handwritten correction in the Phantom record leads to Gh0st's memory of her asking whether he wanted the light left on—and waiting for an answer. He returns the question at their reunion. Her work on the cradle and the existing rescue are preserved.
- **Game text:** Revised adjacent quest summaries, completed-memory inspections, bridge descriptions, mainframe feedback and selected climax rooms. Removed designer commentary from the Avatar gate and containment seam. Made small voice corrections to Pam, Rylan and the Curator; retained their personal references and motives.
- **Interaction continuity:** Kept the existing action identifiers behind the new cargo-pod label. Preserved Jed's door and Thorne's cell links, restored the completed mainframe's terminal mention, and restored the missing blinding-geometry and return-to-fracture links in Silence Pulpit's initial description.

The existing illustrated cinematics retain their art and presentation settings. The optional Echo uses the established card-scene format. No new art, audio, battles, routes, stat changes or save migrations were introduced for this pass.

## Review saves

The automated main-story route exports an isolated save pack to `app/build/reports/narrative-review/slots/`. These are prepared files, not replacements for saves on either device. They use ordinary earned inventory, combat rewards and progression. The route supplies known solutions and a combat strategy; it does not establish blind-player discoverability or emotional pacing.

| Pack slot | Starting point | Review action |
| --- | --- | --- |
| 1 | Workshop Yard, loader defeated | Go north into Jed's Shutter. Watch the briefing, then repair the cutter and continue the governor discussion. |
| 2 | Cargo Lift, encounter cleared | Talk to Jed. Watch the farewell and lift separation. |
| 3 | Archive Vault | Use **lens relic**. Watch Nova respond to Zeke's warning, then Thorne and Vale's exchanges. |
| 4 | Waste Intake | Use **project phantom terminal**. Watch Gh0st choose how to read the file. |
| 5 | Cooling Springs | Use **steam pool**. Finish the Elara memory and override-key conversation. |
| 6 | Mainframe Nave | Use **Thorne trace**. Review the admission, the crew's terms and the firewall route. |
| 7 | Anchor Chamber | Use **elara**, then **anchor relic**. Compare the reunion with slot 5. |
| 8 | Shared Memory Span | Use **campfire promise**, go east to Singularity Threshold, and use **singularity threshold**. Review all three refusals at Memory Stair. |
| 9 | The Center | Use **Vale**. Review the confrontation and Nova's answer. |
| 10 | The Center, both final enemies defeated | Use **shared tuning focus**. Set Heartbeat **72 BPM**, Neural Pressure **36%**, Choir Phase **270°**; choose **Tune the Chorus**. Watch the Final Note and diner ending. |
| 11 | Jed's Echo Bench, marks already found | Use **jed echo**. Finish the remembered lesson, then go east and use **open shift gate** for Legacy. |

Slots 1–10 come from a fresh main-story run that skips Tideglass Day and Jed's Echo. Slot 11 branches from that earned World 6 save and follows the optional marks route. The main-story ending therefore does not rely on completing either side quest.

Reproduce the pack with:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest --tests '*LegalRouteCampaignRunnerTest.narrativeMainRouteExportsReviewSavesWithoutOptionalMemoryQuests'
```

This writes only the test output directory. Copying saves to a device is a separate step; back up the destination slots first.

The packaged review saves are at `app/build/reports/narrative-review/starborn-narrative-review-saves.zip`, with a standalone review guide and a manifest identifying the matching source assets/build. Only the eleven primary save files are included, not the persistence layer's backup copies. Pack numbers are local review labels; they must not be copied over occupied device slots without backing up or remapping those slots.

## Verification and remaining judgment

The fresh campaign and earned completionist Source routes reached credits. The focused runner checks that the extended Phantom, Cooling Springs, Thorne and Elara conversations credit their tasks only after the final line. The optional Echo survives saving while its cinematic is pending, resumes in a newly created session, leaves the shift-gate step outstanding, and pays exactly once after completion and revisit. The existing Source route tests also exercise leaving a refusal, reopening it, alternate refusal/rescue orders and finale save/reload.

Final validation also passed the dialogue service/event checks, asset and campaign-catalog integrity checks, World 6 side-quest independence check, narrative runtime/discoverability checks, and the World 4–6 public-interaction checks. Both World 5/6 guides' documented text controls pass the rendered-link check; a dedicated assertion verifies that every Memory Stair description displays **cargo pod wreckage** while retaining the existing action key. Quest structure and rewards, room routes/encounters, and all preexisting cinematic art/presentation settings were compared with the pre-edit copy and remain unchanged.

The regular Android debug build succeeded: **1.3.127 (211)**, package `com.junewiregames.starborn.prealpha`, at `app/build/outputs/apk/debug/app-debug.apk`. This pass did not increment the version, install to either device, or commit/push the working changes. The review pack's manifest records the exact APK and asset hashes because the version alone does not distinguish uncommitted builds.

Codex completed the follow-up editorial review in story order: [continuous scene review and final corrections](09_CONTINUOUS_SCENE_REVIEW.md). The review supports retaining the revised structure: the workshop establishes ordinary affection, Thorne's bargain retains responsibility and self-interest, the light question gives Elara's reunion a personal reference, and the crew's objections change Nova's actions. Six adjacent copy corrections address the diner, Elara's captivity, the names supplied by Thorne, and the Cooling Springs aftermath.

This is not a review assignment for the user. A future continuous playthrough can test elapsed spacing and on-screen pacing, but the editorial pass is complete. The report distinguishes script/runtime inspection from rendered-device observation; passing routes or calculating reveal times cannot prove an emotional response.

Rest-banter rotation was evaluated and left outside this editing batch. Its fixed priority still limits which optional lines a full party hears, but none of the added essential setup depends on rest. A persisted rotation would be a separate runtime improvement. The additional mid-campaign Jed reminder was also left out: establish and play the repaired opening/payoff first rather than adding another grief beat by default.

This pass does not certify every NPC line in the entire game. It resolves the scoped continuity and characterization findings and samples the affected supporting cast. The earlier [full voice review](05_DIALOGUE_VOICE_REVIEW.md) records the broader editorial coverage.
