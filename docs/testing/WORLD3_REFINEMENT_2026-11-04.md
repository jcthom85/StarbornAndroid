# World 3 refinement — November 4, 2026

Implements the approved pre-playtest repairs and one focused puzzle improvement. The subsequent [optional-content pass](WORLD3_OPTIONAL_CONTENT_2026-11-04.md) adds verified city return travel and reward/display corrections; use its latest build evidence for those followups. These changes are local. They do not install an APK, commit, push or publish the web guide.

## Player-facing changes

- Museum Curator and Hacked Lab Terminal now use the same speaker names as their NPC definitions. Guest-registry interaction delivers the Curator/Zeke scene. Repeat interactions provide current guidance.
- Mika’s dialogue and the lantern object both require tracing the power siphon. Existing saves that already have a ready market grid can finish the broadcast without losing their progress.
- Prototype calibration requires its targeting audit through both terminal dialogue and object events. The approved records/calibration scope is retained.
- All ten Spire return shortcuts are repeatable, including old saves containing their formerly consumed event receipts.
- Laundry descriptions prioritize disabled scanners over the earlier guest-profile state. The later optional-content pass connects the Transit Plaza elevator controls to Laundry Service and the Service Lift panel back to Transit Plaza, replacing the incorrect regional-map directions.
- Vance has an initial alibi before the evidence-gated confrontation and a nonrewarding completed-case response. The crime-scene barrier no longer falsely claims to block the public exit.
- Lens interaction delivers Zeke’s warning before acquisition; Thorne’s warning and Vale’s lockdown order follow. The planning handoff explains Astra’s autopilot relocation to the rooftop. Departure distinguishes the local security curtain from the still-overhead planetary Shield.
- The sewer objective names its actual crawler encounter. Neon Fix supplies a source/location hint. Planning copy describes committing the prepared route rather than promising a role-assignment choice.
- Intercept describes its existing self-Guard effect. The coolant injector describes its implemented elemental/status/stat effects. The passkey is described as evidence rather than an unimplemented transit unlock. No combat values changed.

## Lens control sequence

After inspecting the field, shutters and command tether, the braided-chord action opens Prism Controls. The player must:

1. Open relief into blue RETURN.
2. Isolate red FEED while leaving RETURN open.
3. Copy the local authorization hold before disconnecting the gold audit tether.

Incorrect routes reset the handles with a physical explanation and no resource penalty. Leaving or restoring an unsolved save starts a fresh control attempt. There is no timing requirement. The existing solve event, solved milestone, quest task IDs and reward event remain intact; previously solved saves can proceed straight to the Lens warning. Existing old-control receipts do not consume the new repeatable interaction.

## Verification

Final verification passed:

- Full Android host suite: **793 tests**, zero failures, errors or skips; includes the earned production campaign through credits.
- Focused shared-controller suite: **4 tests**, zero failures, errors or skips.
- `:app:assembleDebug` passed. The APK has not been installed.
- Local guide browser checks passed for World 3 direct entry/checklists, existing World 1/2 progress, search, theme, mobile layouts and offline reload. The World 3 mobile screenshot was visually inspected.
- JSON catalogs parse, dialogue targets resolve, and `git diff --check` passes.
- [Verification and asset/APK hashes](evidence/world3_refinement_2026-11-04/verification.json); final command logs are preserved beside it.

New regressions exercise canonical dialogue names, prerequisite bypasses, legacy shortcuts, description precedence, initial versus completed suspect conversations, incorrect routing, leaving/restoring, successful routing, warning/escalation delivery and once-only rewards. Shared-controller tests use visible room actions and normal save/load in addition to event-level tests. The earned production campaign also exercises the new registry, routing and Lens interactions.

## Remaining acceptance

Phone acceptance should cover clue comprehension, scrolling/choice readability, object and dialogue popup timing, the transition from acquisition to radio warnings, rest and rooftop resource pressure. The murder case still supplies the accusation after evidence collection; heist planning remains linear. These were not expanded into additional redesigns. The optional chrono slider puzzle is unchanged.
