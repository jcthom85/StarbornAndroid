# Debug scenario QA

Current rebuild workflow and verification: [test suite rebuild](testing/README.md). The sections below document the older suite and its historical checks; rebuilt scenarios use separate test saves.

Use a disposable test save. Scenario launches replace the current session; save a personal playthrough in a separate slot first.

## Repaired shortcuts

All four boss entries now dispatch to pre-fight story setups, not a detached combat simulator. Hunter and Vale reuse their existing story checkpoints. Warden starts at Pod Bay; Titan starts before the Rylos confrontation. Verify victories advance the story, not only that combat opens.

Cooking Kitchen stocks five batches of every authored cooking recipe. Advanced Workshop stocks three batches of tinkering ingredients plus tools aboard the Astra; open Tinkering from the menu. Spire Runoff starts at Sewer Passage with rods and lures. These are stocked system sandboxes, not economy/progression proofs.

Fixed six obsolete tutorial room IDs and four obsolete quest-stage references, including Jed's Bench.

## Progression QA presets

- Exact Recipe: craft the Functional Cryo-Inductor from exact requirements; verify quantities and quest continuation.
- Missing Scrap: verify refusal consumes nothing, then investigate whether exploration can recover the missing resource. This also represents an already-spent-scrap inventory boundary; it does not simulate the spending event itself.
- Meal Save: injured crew at Sewer Landing with Glowfish Broth/Nova bonuses and three encounters remaining. Save to a test slot, reload that slot (do not relaunch the preset), compare effects, and finish three encounters. Use the cooking sandbox to test consumption/replacement separately.
- W2 Locked/Unlocked: same Canopy Walk location, differing Thermal Cutter possession. Check east/west reciprocity, locked feedback, and reload after crossing. Use the existing Temple Gate and Stasis Chamber checkpoints for Hall of Echoes traversal.
- W3 Before/After Intel: The Static before/after The Plan. Walk Transit Plaza, Underrail, Sewers, and the Upper City approach. Compare quest-stage gating; neither preset grants every inventory key. These are synthetic milestone checkpoints, not proof that earlier quest events ran correctly.

Gate presets retain sandbox combat progression but strip inventory to equipped items and rations, plus the explicit W2 cutter when applicable. They are not suitable for judging combat or resource balance.

## Automated coverage and limits

Catalog tests check unique entries, dispatch coverage, literal seeded rooms and quest stages, literal item grants, and recipe ingredient/tool references. Provision and navigation regression tests run alongside them. These checks are source/data integrity checks, not device execution of every launcher. Full launch, save/reload, and boss-victory flows still require device playtesting.

No additional Play release is made by this change; use the rebuilt debug APK until the next requested release.

## Emulator verification — 2026-09-14

Ran two Android instrumentation tests successfully on emulator-5554 (Medium Phone, Android 17). All 91 catalog launchers returned success and produced valid room/hub, quest-stage, and inventory references. This checks actual AppServices setup on Android, not every rendered destination screen or boss fight.

The rendered RoomDescription console control was tapped before and after the real dialogue-trigger handler spliced the Chime. The first tap remained retryable; the second started the launch cinematic. Tests also wrote/read disk saves for missing-coil recovery, prematurely consumed launch-event recovery, and meal/HP persistence, then verified meal expiration after three encounter decrements. They do not simulate three actual battles or full process death.

An initial combined run encountered shared-save interference between independent AppServices fixtures. Both fixtures now use separate context-backed directories, and the rerun passed. Test files remain in disposable emulator cache storage; no user save was cleared. Fifty selected unit tests also passed.
