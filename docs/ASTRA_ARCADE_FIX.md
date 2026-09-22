# Astra arcade launch repair — 2026-09-22

The arcade navigation destination accepted only Deep Mine before entering a dispatcher that contained all six games. Consequently the other five cabinets immediately popped back to exploration. The destination now accepts all six known cabinet IDs; unknown IDs still return safely.

The pre-restored Astra debug/demo fixture now marks installation introductions as seen, so the first tap launches each game. Normal campaign restoration introductions remain unchanged. BurgQuest Demo and Debug Scenarios menu buttons were not edited.

## Verification

- Debug and instrumentation APK builds successful.
- 17 targeted Astra/arcade unit tests passed (travel, discoverability, hub travel, arcade service).
- `AstraArcadeNavigationTest.showcaseCabinetsLaunchOnFirstTap`: passed on emulator-5554, 19.339 seconds. Starts `burgfest_astra` with isolated persistence, enters the common room, scrolls to and taps every cabinet, checks the exact arcade route/ID, then returns to exploration between cabinets.
- Screenshots: `test-results/astra-arcade-rendered/`, six cabinet screens. Spire and Harmonic tutorial screens visually inspected. Earlier screenshot directories captured an unadvanced Compose navigation animation; they are not the final visual evidence.
- Test clock explicitly advances the navigation animation and pauses automatic advancement while arcade engines run continuously. An initial idle-wait failure was a test synchronization issue, not a gameplay stall.

This verifies launch/navigation, not full playthroughs or scoring of every minigame. Normal saves were not cleared. Updated debug APK installed on the emulator; no release, version bump, commit or upload performed.

Combat review resumed with the scoped diagnostic sequence in `COMBAT_REVIEW_NEXT_PASS.md`; no additional production balance changes made.
