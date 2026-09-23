# Avatar recovery implementation — 2026-09-22

Compliance Avatar now uses `avatar_recalibration` (Recalibrating Weapons) as its next eligible recovery action after Missile Barrage. The action targets itself, deals no damage, applies no status effects and displays “Recovery window — attack or heal.” Normal attack selection resumes afterward. There is no opening pause or recovery after its other specials; numeric stats are unchanged.

## Verification

- Full unit suite: **548 tests, zero failures/errors**.
- Actual production assets, prepared campaign-earned fixture, seeds 1–70 repeated deterministically: **55 victories, 15 defeats, zero timeouts**, matching the narrower experimental candidate. Evidence: `test-results/avatar-production/campaign.xml`.
- Emulator `FinalePresentationTest#avatarRecoveryCue`: passed in 20.238s on emulator-5554. Screenshot `test-results/avatar-production/presentation/recalibrating.png` visually confirms both title and secondary instruction fit clearly.
- UI test uses isolated persistence and seeds the post-barrage history to exercise presentation. It is not an earned-checkpoint balance playthrough. Unit tests verify the recovery trigger, lack of damage/status applications, and return to ordinary attacks; runtime simulations exercise full fights.
- Debug and instrumentation builds pass. Normal saves were not cleared.

This improves the tested policy from 34/70 to 55/70 victories, not a guarantee of human success. Early deaths and full-health critical hits remain possible. The test-only historical target-switch experiment now refuses to run on assets already containing recovery, avoiding accidental combination of candidate mechanisms.

Changes are local; no version increment, commit, push or Play upload in this pass. Next priority: finale pacing for slower strategies, followed by Foundry full-route resource validation.
