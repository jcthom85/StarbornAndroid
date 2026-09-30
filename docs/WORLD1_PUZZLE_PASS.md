# World 1 puzzle pass

## Scope and design

1. **Counter-Tune the Fork:** replace the numeric-entry presentation for this
   puzzle with three guided signal experiments. Match pulse spacing, balance
   cutter output through cooling, then oppose the Fork's phase to flatten the
   combined signal. Each step shows a live visual trace and a text diagnostic.
   Keep the canonical 87 kHz / 68% / 180-degree result and the existing success
   event, rewards, cinematic and tolerances. Other worlds retain their existing
   tuning interfaces. No timer or hearing requirement. Remove the answer from
   the active objective; the resulting calibration remains in the cinematic.
2. **Ore cart:** inspect the rails for an optional clue, divert the track, and
   separately release the brake. Correct routing is sufficient to release it. A failed attempt explains what is missing. Destroying the bulkhead is
   permanent, and cannot replay or close the passage when the switch resets.
   Existing saves with the original destruction milestone retain access.
3. **Protocol Override:** keep terminal discovery and thawing, then use a short
   terminal dialogue to choose where the key should be written. A clue identifies
   the local removable board as the output that avoids the audited network.
   Wrong selections explain the consequence without consuming the key or
   rewarding the quest. Returning/cancelling can reopen the choice. Preserve
   completed and partially completed saves and the existing reward amounts.
4. Preserve bunk-door onboarding, loader weakness teaching, Zeke's paperwork,
   System Flush, and mine-power exploration.

## Implementation and verification

- Share the Fork's visual panel and signal calculations between Android and
  desktop, retaining their existing event dispatch and other puzzle screens.
- Use existing room actions, event conditions and dialogue for cart/override.
- Update existing regression scenarios to reflect the new guided controls and
  terminal choice; document ordering, persistence and retry acceptance checks.
- Check signal calculations and readable UI state, including small layouts.
- Build Android and compile desktop; review content references and dispatch.
- Document observed verification and any remaining device acceptance.

## Acceptance

- The Fork is solvable from traces and instructions without numeric answers.
- The final signal is visibly cancelled; individual controls explain their role.
- Changing a control cannot grant a reward before the final handshake.
- Cart passage stays open after switching, revisiting and save/load.
- The override requires an informed choice, and grants its rewards once.
- No release, version bump, commit or upload is part of this pass.

## Implementation notes and current verification

- `:app:assembleDebug` and `:desktopApp:compileKotlin` pass (September 30, 2026).
- All six edited JSON catalogs parse and contain no duplicate IDs;
  `git diff --check` reports no whitespace errors.
- Pulse and cooling holds stabilize the matched values at their target centers,
  preventing a small accepted frequency drift from making phase cancellation
  point toward a different phase than the success condition.
- Android bootstrap, slot loading, legacy import, and debug seeds use the
  existing migration hook, now restoring the breached cart state for old saves.
- Existing terminal regression scenarios and the Echo Maestro scenario were
  updated to use the new choice/control flow. Tests have not been run.
- Device acceptance remains: small-screen/large-font layout, puzzle feel,
  cancelling/reopening, cart save/load, and terminal retries/reward protection.
- Desktop shares the new Fork panel, but its existing exploration implementation
  does not dispatch room actions through Android's EventManager. This pass does
  not implement full desktop campaign progression; the cart and terminal changes
  are wired to the Android event flow.

## Evaluation follow-up

- Rail inspection is now an optional clue. A cart routed west can be released
  without a prior inspection; attempts on the main line still hold the brake.
- Diverting the points after the breach describes the crashed cart and open
  passage, rather than claiming a cart is still waiting behind its brake.
- Protocol Override's cinematic now shows the isolated board extraction and
  confirms that no network transmission occurred.
- The Fork retains its current three-stage structure for player evaluation.
