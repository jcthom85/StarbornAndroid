# Desktop combat presentation audit and first implementation pass

October 7, 2026. Source review against Android's live CombatScreen, shared CombatController and CombatFxEvent definitions. This is a code audit, not a native combat playthrough.

## Findings and changes

| Finding | Implemented |
| --- | --- |
| StatusApplied, SupportCue, TurnQueued and CombatOutcomeFx were ignored | Every shared FX event type now has a desktop consumer; status/support/readiness/outcome feedback and an event history are exposed. |
| Impact feedback discarded element and broken-bonus information; simultaneous cues overwrote each other | Separate expiring cues retain all impact modifiers and elemental color; local slash, ranged and cast/support effects render at the affected figure. |
| Status IDs, buff details, momentum and weapon charge were absent or unclear | Authored status names, stacks, durations, active buffs, momentum and charge turns are shown on their associated combatants. |
| Timed input was a small button with no clock | Full-screen click/Space response overlay and a countdown use the controller's monotonic start time/duration. |
| Desktop item list filtered only consumables and missed explicit target modes | Android eligibility and target picker policy moved to shared CombatItemPresentation; both platforms consume it. Equipped snacks remain separate. |
| Tutorial prose and Continue behavior differed | Original Android title/message/button writing moved to CombatTutorialCopy; only modal tutorial steps get Continue, and the expected ability is the selectable tutorial option. |
| Skill unavailability replaced its description | Description remains visible alongside availability, exact cooldown remaining and targeting. Item descriptions/effects are also displayed. |
| Readiness was buried in figure cards | Party readiness strip, acting/ready labels, Tab cycling and target highlighting. This is ATB readiness, not a fabricated fixed turn order. |
| Several keyboard commands could replace pending targeting | Action commands are gated during targeting/timed input; 4 invokes equipped snack, R retreats, L opens the paused battle log. |
| Combat room art was cropped | Uses the existing fitted portrait/reflected backdrop. |
| Accessibility settings partly ignored | Tutorial preference loaded before controller creation; hit flashes/recoil, high contrast and larger primary action buttons consume preferences. |

Audio continues to dispatch the original shared commands. Combat damage, enemy AI, rewards, targeting execution and progression rules were not replaced.

## Follow-up fixes requested during this pass

- Exploration narration no longer uses a generic AlertDialog. A bounded, readable inspection card follows Android's accent rail, border, gradient and spacing.
- Tutorial/milestone/item notifications use banners rather than extra generic dialogs. Only one narration/event inspection is rendered at a time. Authored messages are passed unchanged; no opening event JSON was edited.
- Intro camera transforms now apply to both the sharp center image and reflected blur, for incoming and outgoing dissolve frames. Static exploration/hub artwork keeps its original fitted behavior.

## Remaining work

- Native playtesting: crowded party/enemy formations, ordinary/support/status/elemental attacks, misses, stability breaks, timing input, victory/defeat/retreat, keyboard focus and saved accessibility settings.
- Android's complete particle canvas, delayed-death choreography, bespoke attack illustrations and staged victory spoils remain richer than this first desktop pass.
- Verify actual audio output and high DPI/window sizes. Current compilation does not establish audible playback, input latency or visual quality.
- Shared timed windows use controller deadlines; background-window timing behavior needs a dedicated cross-platform review.

## Validation

Both desktop and Android Kotlin compilation succeeded after the changes. No automated gameplay tests were added or run for this pass. Portable folder packaging succeeded. Its build-time content check verified all 1,327 Android assets in staging and the asset archive. Native gameplay and audio were not playtested in this pass.

Build log: `tmp/combat-prompts-cinematic-build.log`.
Portable app: `desktopApp/build/distributions/Starborn-Windows-portable/Starborn/Starborn.exe`.
