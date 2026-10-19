# Starborn: preparation for the World 1 review

October 19, 2026. Scope: the bounded crew-progression cleanup approved before the World 1 review. This is not World 1 completion sign-off.

## Baseline

- Android and desktop New Game use the 32-node crew catalog. Existing player saves receive one-time legacy compensation; learned combat abilities and existing progress are retained. See [production adoption](CREW_PRODUCTION_ADOPTION_2026-10-19.md).
- World 1's main quests award 2 AP after `w1_mq01`, 2 after `w1_mq03`, and 2 after `w1_mq05`. **The subsequent World 1 route review corrected certification's classification:** `w1_sq03` is required for mine access and adds 2, making the mandatory World 1 total **8 AP**. AP is shared, purchases are optional, resets are free outside battle, and fourth nodes wait for World 4 completion.
- First-AP guidance names the actual `Party > Nova > Skills` route, distinguishes story/level abilities from purchased perks, and introduces both starting choices. Source: `NovaProgressionTutorial.kt`, `ModernFieldMenu.kt`, `NovaBuildPrototype.kt`.
- The preceding production controller run cleared World 1 with earned resources and real combat. Its later World 6 failure does not establish Android World 1 presentation quality.

## Android smoke verification

**Complete: four checks passed at normal text size; both UI checks passed again at system font scale 2.0.** The debug app and instrumentation APKs build successfully. Target was an Android 16/API 36 emulator, 1080 x 2400 at 420 dpi. The connected phone's release build was not modified. No gameplay changes were required by this pass.

The checks are deliberately isolated from player saves:

1. Production New Game, followed by the real first-quest completion award at a controlled boundary; first-AP prompt, opening Nova's tree, purchasing Grounded Wire, reset accounting and save/reload.
2. All four rendered trees from an explicit middle-game checkpoint; one starting-node purchase per character and owner-specific refund; save/reload. This is not an Android UI click-through of all 32 nodes.
3. A constructed version-0 slot with known compensation and equipment, loaded through Android `AppServices`, then saved and reloaded without repeating compensation.
4. A byte-for-byte copy of the emulator's July 14 historical session, loaded into separate storage. Verify inventory, equipment, quests, schematics, learned combat skills and one-time credit against the preceding opening-migration policy.

The original emulator's datastore was backed up under `tmp/crew-emulator-legacy/`. Its current session copy is 1,804 bytes, dated July 14. It had version 0, room `pit_nova_bunk`, one completed historical quest, 0 AP and no compensable legacy tree purchases. It migrated to version 1 with 4 AP from proven Orion/Gh0st roster membership; resaving/reloading did not pay again. This historical case does not exercise a nonzero old-tree credit: the constructed fixture independently checked 5 unused AP + 4 legacy credit + 2 quest AP = 11.

The new compatibility assertions were corrected to respect two existing policies: retired Ollie party/equipment records are removed while owned items remain, and Android moves weapon/armor items from the ordinary inventory bag into equipment ownership. Assertions still verify consumable counts, retained equipment ownership, equipped items and credits. Those expectation corrections were not gameplay fixes.

Normal installation initially failed with insufficient storage; a separate workspace-contained emulator was prepared instead. Its first boot showed a System UI ANR, and the first instrumentation attempt terminated during app startup without test results. After startup settled, the final normal-text and doubled-text runs completed. Do not treat those initial attempts as passed tests or use this run as startup-performance acceptance. The 1.28 GB debug APK and emulator setup are recorded observations, not release-package measurements. The disposable emulator was stopped after collection, with font scale restored to 1.0; the original AVD and phone installation were preserved.

The first-AP UI test does not play the entire opening through touch controls: it completes `w1_mq01` through the session API to isolate the award/UI boundary. It renders the production prompt/tree components with the actual exploration view model on a test canvas, rather than navigating the whole production menu. Controller-route coverage and this UI test provide different evidence. Screenshot inspection found readable normal-size guidance, no horizontal truncation in the inspected text, and scrollable large descriptions; automated clicks verified the purchase/refund controls remained reachable at font scale 2.0.

### Retained evidence

- [Normal-text instrumented run: 4 passed](evidence/crew-android-smoke-2026-10-19.txt); [doubled-text run: 2 passed](evidence/crew-android-large-text-2026-10-19.txt); [historical migration summary](evidence/crew-legacy-migration-2026-10-19.txt).
- Normal: [first-AP prompt](evidence/crew-first-ap-normal-2026-10-19.png), [first choice](evidence/crew-first-choice-normal-2026-10-19.png). Doubled text: [prompt](evidence/crew-first-ap-large-2026-10-19.png), [scrollable first choice](evidence/crew-first-choice-large-2026-10-19.png).
- Debug APK SHA-256: `f297b7cdaec1856e61df7c60bbf077ebbec9a8f7809acb1fba2ca0b5173bdb76`.
- Instrumentation APK SHA-256: `5ad9b80d155ab84d0d8748c62119411bdd0fc2fb6eadd536cff3b61036c01358`.
- Historical snapshot SHA-256: `69ff27da3480a0c2d5fa7bf64e04ad076e439e5bd0f47a7a96f7fafc8d343753`. The actual save remains local under `tmp`; it was not added as a public fixture.

The UI/migration checks are `NovaTutorialInstrumentedTest` and `CrewLegacySnapshotInstrumentedTest`. To rerun the historical check, explicitly supply the copied file as `crew-legacy-original.pb` in the test installation's external-files directory. Never run it against the normal datastore or substitute an unknown player save in place.

## World 1 review entry criteria

- Crew purchase/reset/save behavior has usable Android evidence, with failures addressed or explicitly retained.
- A historical save has been tested on a copy; originals remain preserved.
- Current documentation entry points identify crew adoption as implemented; historical proposals are labeled as such.
- The World 6 continuous-route failure remains tracked separately and visible. Do not describe the whole game or full test suite as accepted.

## Start the World 1 pass

**Ready to begin the World 1 review.** The bounded progression smoke gate is satisfied for the tested emulator scope. No unresolved skill-tree issue from this pass requires delaying World 1.

Play from a normal fresh game with tutorials enabled, without debug supplies or route answers. Record time to the first meaningful action, first fight, first AP and first purchase. Check whether the prompt arrives at a sensible moment after the cutter sequence and whether both starting paths are understood. Continue through Zeke, the mine, the Warden, the relic, the escape and the World 2 handoff, saving/reloading at major boundaries.

Review the five main quests and five side quests against current implementation; the October 16 evaluation is a list of claims to revalidate, not an authorized bulk-fix list. Prioritize reproducible progression defects, unclear navigation and conflicting story states before prose, art or audio polish.

## Remaining acceptance boundaries

Physical-phone comfort, subjective readability, TalkBack, audio quality, interruption recovery, and a continuous human World 1 playthrough still require direct acceptance. A successful emulator boundary test does not close those items. Full-campaign balance remains open at World 6's `source_memory_fragments`, as recorded in the adoption report.
