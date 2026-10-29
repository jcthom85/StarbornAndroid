# World 1 focused functional regression

Companion to [World 1 functional standards](WORLD_1_FUNCTIONAL_STANDARDS.md). Baseline: `e44e40d9`, October 29, 2026. All boxes start **NOT RUN for the next device acceptance**; source verification and host results are recorded separately in the assessment. This checklist prioritizes the three playtest passes and reuse risks; it supplements, rather than replaces, [full phone acceptance](../playtest/WORLD1_ACCEPTANCE_CHECKLIST.md).

## Record the run

- [ ] Record date/tester, device/OS, platform, commit/dirty state, version, APK SHA-256 and install method. The version string alone does not distinguish successive local APKs.
- [ ] Record fresh/earned/legacy/synthetic save and slot, tutorials on/off, font scale, large-target/accessibility settings, audio settings, party/build/gear/mods/resources.
- [ ] Preserve an earned pre-Heart, pre-Warden and World 2 arrival save. Never run reset-based automation against the player's normal save package.
- [ ] Use PASS / FAIL / NOT RUN / BLOCKED per case, with expected/actual, reproduction steps, save reference and screenshot/video/audio timestamps. A caveated host pass is not a device PASS.

## Focused acceptance cases

| ID / related fixes | Steps and expected result | Evidence |
|---|---|---|
| R01 — NAV/TUT; P1 CB-01–04, IC-02 | Fresh start: reveal/light/door/Jed, inspect-loader-first. Complete weakness teaching while loader survives long enough to demonstrate Break. Craft, patch, confirm bypass, live test. No header/action occlusion or dialogue over a closing menu. | Video opening + workshop; quest/state snapshot. |
| R02 — NAV/QST; P1 PB-02 | On separate earned/fresh route, fight loader before reading strip. Leave calibration/workshop midway, save/load, return. No duplicate suit/reward, no invented inspection, and all remaining objectives stay reachable. | Inventory/quest before/after and saved slot. |
| R03 — NPC/QST; P1 IC-23 | Scrapper quest offer → cache → turn-in → Trade; return and repeat. Quest progression precedes commerce; reward once; both menu and conversation can end. Shopkeep can trade without a separate room shop button. | Quest log, grenade counts and exit interaction. |
| R04 — ECO; P1 PB-01; P2 CB-01 | Try selling quest/key items. Tap Staff Lounge dispenser five times, checking live count each time, then travel and reload. Essential items stay protected; repeatable dispenser grants exactly one each time; UI/session/save agree. | Five count pairs plus reload. |
| R05 — TUT/CBT; P2 IC-01–03 | Complete required certification, including blocked attack, Hydraulic Kick and follow-up attack. Explanation follows feedback; drill target survives teaching. Return to Boggs for authorization. Repeat with tutorials off using an earned path. | Action timeline + journal; distinguish guided HP from normal stats. |
| R06 — UI/ECO; P1 IC-16; P2 IC-04/08/09 | Spend AP, cancel respec, confirm respec; verify pool/refund and retained story skills. Group completed journal entries, track another quest. Consumables filter includes medicine/meals; coffee cannot be used in the field. | AP purchase ledger, screenshots and inventory counts. |
| R07 — NAV/UI; P1 IC-21; P3 IC-05/12 | Check crowded NPC/item rails with normal/enlarged text. Scroll to last entry in both directions. Talk to NPC at an exit and hub-return room: arrows/gateway stay visible, disabled, then reactivate without layout shifts. Minimap stays legible; full map retains services. | Narrow-phone video; screen-reader/keyboard equivalent where available. |
| R08 — QST/NAR; P3 IC-01 | Heart: wrong calibration value, leave/return, correct stages, normal cinematic completion. Inductor disappears once; story explains spent core; ITEM USED is not ITEM ACQUIRED; Fork/Blast Wave/rewards/journal follow without overlap. Retry action and reload. | Inventory before/after, popup sequence and room variant. |
| R09 — QST persistence | Separate earned save: interrupt Heart cinematic and later launch cinematic; test normal finish, skip, background/resume and force-stop/load. Each callback completes once with no missing grant or repeated consumption. | Pending scene/quest/item state before/after. |
| R10 — NPC/NAR; P3 IC-02/03/06/11 | Emergency Exit → Maintenance Access → Cargo Lift. Three Zeke radio calls use his name/radio portrait, do not replace later normal dialogue, and can finish. Narration has no redundant Continue text. Jed's position/bypass/wrench matches the handoff. Recruitment uses the important rounded announcement without a continuation hint. | Full escape video including speech/narration boundaries. |
| R11 — UI; P1 IC-06/07/09; P3 IC-07 | Queue item group, tutorial, quest completion, credits and a second dialogue close together. One foreground owner; no hidden expiry or skipped next reward. Swipe quest-update banner left, then right on another; test X; short drags snap back. | Video plus counts/quest results, not screenshots alone. |
| R12 — CBT/UI; P3 CB-03, IC-08/09 | Trigger Guard Break; read it after one second. Open Abilities/Details with normal/enlarged fonts, switch target and cancel. Percentage scaling is distinguishable from flat damage/base HP healing; details fit without accidental Use. | Video/frame capture; input/target state. |
| R13 — CBT; P3 IC-04 | Actual Skimmer/Borer fight with an earned Nova build. Observe Optics Static lasts one target turn, ordinary Blind elsewhere retains its behavior, and Freeze counterplay is useful. Record attacks/hits/statuses, not only a perceived miss streak. | Enemy IDs, build, attacks/misses, statuses and outcome over several attempts. |
| R14 — CBT/ECO; P3 IC-10 | Warden with/without Corrosive Rounds from comparable earned saves. Verify fixed corrosion ticks/expiry/proc and boss shield, drone, gas and overclock mechanics. Record turns, damage sources, consumables, knockouts and skill use. | At least two reasonable builds/multiple attempts; no debug victory/OP mode. Goal: hardest fight so far, accessible first boss. |
| R15 — NAR/ECO; P3 CB-02 | Cargo Lift recovery before checkpoint: service recess/pressure/dressings makes sense, restores actual HP and reports already-full correctly. Cook is separate wherever both services exist. | Recovery message and HP; no unintended reward replay. |
| R16 — QST alternate order | Earned alternate route: defeat Warden before returning to Jed; backtrack/handoff, then launch. No second boss, reward or dead end; Chime is required and consumed only at authorization. | Encounter count, Chime, quest and reward state. |
| R17 — NAR/audio; P2 CB-02; P3 CB-05 | Listen to back-to-back fights: combat starts at beginning, exploration resumes appropriately. Play launch/crash at slow reading speed and rapid advance/skip. Check music ownership and launch/impact SFX that have duplicate authored dispatch sources. | Recording with cue timestamps; note overlaps/restarts/volume jumps. |
| R18 — handoff; P3 CB-01 | Arrive in World 2 after normal launch: World 1 complete, `w2_mq01` active/tracked, Zeke remains in roster. Finish opening dialogue; talk repeatedly, save/load, talk again. No repeated recruitment, XP/AP or item grants. | Roster/quest/resource comparison and announcement count. |
| R19 — optional completion | Finish Stash, System Flush, certification, Protocol Override and Lost Shift; cart vault once. Revisit turn-ins/save/load. Reward equals actual inventory/XP/AP changes; no duplicate Lost Shift start notice; unopened branches do not block main route accidentally. | Completionist log and ledger; optional versus required classification. |
| R20 — later-world dialogue sentinel | Charlie before trust, each intermediate trust state, and after gift: find an exit from every choice menu. Repeat gift selection/revisit. October 29 follow-up adds an unconditional step-away choice; verify it closes the actual phone UI without changing trust or granting rewards. | Host exit/reward tests pass; record device result separately for W1-STD-02. Audit equivalent NPC loops in Worlds 2–6. |

Run R07–R12 on desktop as well as Android when claiming cross-platform acceptance. Windows remains a separate execution environment. Repeat critical boundaries with large text/large targets and fast taps; do not infer accessibility from default-size screenshots.

## Automated preflight and limitations

Use Android Studio's JBR/JDK configuration as appropriate for the host. Existing test commands are read-only with respect to gameplay source, but may generate ignored reports/captures.

```sh
./gradlew :app:testDebugUnitTest \
  --tests '*Hub1CriticalFlowTest' \
  --tests '*DataIntegrityTest' \
  --tests '*EventManagerTest' \
  --tests '*DialogueServiceTest' \
  --tests '*UIPromptManagerTest' \
  --tests '*PartThreeBalanceTest' \
  --tests '*Inventory*Test' \
  --tests '*AudioRouterTest' \
  --tests '*LegalRouteCampaignRunnerTest.productionWorld1ReachesTheWorld2HandoffWithEarnedResourcesAndSaveParity' \
  --tests '*LegalRouteCampaignRunnerTest.productionWorld1CanRecoverIfTheWardenIsDefeatedBeforeReturningToJed' \
  --tests '*LegalRouteCampaignRunnerTest.productionWorld1CompletionistRouteFinishesAllTenQuestsAndTheCartDetour' \
  :desktopApp:test \
  --tests '*DesktopMerchantInteractionTest' \
  --tests '*DesktopRestStandardsTest' \
  --tests '*DesktopQuestQueueTest' \
  --tests '*DesktopQuestPresentationTest' \
  --tests '*DesktopPartThreePresentationTest'
python3 scripts/world1_checkpoints.py
```

At assessment: Gradle selection passed **118 cases**; checkpoint validation reported **stale source hashes**. The earned-route tests use fixed policies/seeds, supplied puzzle answers and accelerated presentation. Static integrity tests check references/rules but do not execute every UI combination or prove every dialogue loop exits.

The October 29 follow-up regenerated all ten earned fixtures and passed freshness validation. For future content changes, regenerate using the workflow in [WORLD1_PLAYTEST.md](../testing/WORLD1_PLAYTEST.md). `--import-earned` writes shipped checkpoint assets; use only captures from a freshly passing production route. Then use the isolated `.playtest` package and all four named suites; two consecutive clean runs on the same build are the existing automation acceptance policy. Source changes invalidate old receipts even if display version is unchanged.

Add focused Android instrumentation for navigation disabled semantics, narrow/enlarged-font ability details, swipe threshold/animation and dialogue reveal. Run actual audio-player instrumentation when claiming track-position correctness. These were not rerun in this assessment.

## Closure record

- [ ] Main-route progression and earned alternate-order recovery PASS on the recorded build.
- [ ] R01–R19 each have an explicit result; known defects and omitted cases are listed, not silently passed.
- [ ] R20/Charlie is tracked separately and resolved before general dialogue-exit certification.
- [ ] Stale checkpoint hashes reconciled; fresh automation receipts attached.
- [ ] Audio, accessibility, phone pacing and combat balance receive separate human verdicts.
- [ ] AP recommendation drift and launch SFX ownership have explicit decisions or open follow-ups.
- [ ] For each standard exported to Worlds 2–6, record its evidence and any intentional creative exception.

A world can be the best available reference while these gates remain open. “All documented fixes implemented” is not a substitute for this closure record.

## Next Pixel session — focused order

The October 29 Charlie/checkpoint follow-up passed four dialogue tests and the earned completionist route; all ten checkpoint hashes match current content. These host results do not check the boxes above.

1. **R20:** At Charlie in Overgrown Glade, step away immediately, revisit, advance trust, and step away between stages. After receiving the stone, play/cuddle and leave again; revisit and save/load. Check that navigation returns and the gift does not repeat. Existing trust is preserved; use a separate save to cover earlier stages if needed.
2. **R08–R11, R15:** Use the Heart checkpoint for consumption/notice order, then escape for radio dialogue, Jed's handoff, rest and popup behavior. Try swiping both directions and fast taps.
3. **R12–R14:** Read Guard Break, inspect abilities, then fight Warden with an earned build. Record Corrosive Rounds, turn count and perceived challenge.
4. **R17–R18:** Listen through launch/crash, finish arrival dialogue, talk to Zeke repeatedly and save/load. Verify uninterrupted party membership and no repeat recruitment/rewards.
5. **R03 and R07:** Spot-check Scrapper quest/trade exits, crowded rails, and navigation visibility while talking. Mark anything not exercised NOT RUN.

Install the normal debug app as an update to preserve personal saves. Checkpoint scenarios create/reset a test session; save personal progress first and use separate slots. Reset-based Maestro automation belongs only in the isolated `.playtest` package.

### Installed build for this session

October 29, 2026: normal debug package `com.junewiregames.starborn.prealpha`, version 1.3.100 (184), built from `cb4c2cf3` plus the uncommitted Charlie/checkpoint follow-up. Installed with `adb install -r` on Pixel 8a / Android 17; existing app data was not cleared. Package update timestamp: `2026-10-29 14:13:37` (device-reported). Cold launch returned `Status: ok` and the app process was present. Local APK SHA-256: `7d98a6406bd057c55a9d9dca56c17ba542307d071ce02cd311b4380edf20285d`.

Fresh preflight: five selected host tests passed; ten earned checkpoints validated; Maestro structure passed (132 flows, 301 references). The escape flow now selects the current **Rest action** service instead of the removed cargo-lift description link. Maestro was structurally validated, not executed on the phone. All focused human acceptance cases remain NOT RUN pending this session.
