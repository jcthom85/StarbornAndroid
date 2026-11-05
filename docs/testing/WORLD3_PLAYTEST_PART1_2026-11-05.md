# World 3 playtest, first section — November 5, 2026

Implemented locally from Jason's first World 3 phone-playtest transcript. Earlier navigation and unique room artwork changes are preserved. The current phone installation predates this repair pass.

## Progress blockers

- Evelyn and Robert now have unconditional goodbye choices. The audit found the same missing exit in Cheno, Chris, Andrew, Jason, Oliver, and Meeko's looping conversations; each now has a goodbye too. Existing branches, gifts, conditions and rewards remain in place. A graph regression checks every authored dialogue has a structural path to termination; additional tests exercise goodbye choices with gift conditions before and after receipt. A structural graph check alone does not certify every possible quest-state combination.

## Critical issues

- **Performance:** the active exploration renderer is the shared pooled environmental simulation. Removed its 16ms frame filter, which skipped every other frame at 90Hz. Adjacent room backgrounds now decode in advance on the IO dispatcher into the existing byte-bounded cache. No rain preset, density, streak, color, or artwork was changed. The obsolete WeatherOverlay particle loop is not used by exploration and was left alone. These changes address concrete frame-pacing/loading costs; the reported periodic roof stutter still requires phone confirmation and, if it persists, device profiling.
- **Jax/Zeke continuity:** Jax introduces himself and asks where the crew came from before offering the empty apartment. Nova identifies the mining colony. Removed the unsupported claim that Jax remembers Zeke before the city deleted him, and the response that presupposes Zeke already knows Jax's schedules. The workstation uses copied local service credentials; a pipe-map inspection now explains Zeke recognizing Dominion symbols from mine diagrams.
- **Completed actions:** 25 exhausted one-time World 3 controls stop appearing as actionable text after their events complete. Nine information-bearing clues become gray passive inspections with their original clue text, allowing repeated reading without replaying quest tasks or rewards. Includes laundry lines, prism shutters, the containment/tether clues, patrol timing and case evidence. Visibility comes from actual completed event receipts, including loaded saves.
- **Dialogue scroll:** each newly displayed line resets both its text and response scroll positions.
- **Planning table travel:** the planning prompt explains the walk back to Transit Plaza, copied uniforms/badges, freight elevator, laundry intake destination and Astra autopilot relocation. Its confirmation explicitly commits the plan and departs; a preparation/leave choice permits postponing departure. The existing physical travel montage remains.
- **Guest registry:** Zeke maintains a service-worker cover story using the copied clearance instead of claiming a local paperwork responsibility. Replaced another unexplained use of “ghosts” as a verb in the sensor event.
- **Naming:** the `spire_laundry` region is now **Service Quarters**; its **Laundry Service** room retains its name. Stable IDs and navigation are unchanged.
- **Quest payoff:** World 3 story paragraphs beneath legacy “Quest Complete:” headings were silently suppressed by the event system. Removed those headers from 11 World 3 narrative messages; dedicated quest notifications remain, and the distinct outcome text can now display. This covers the safehouse, heist departure and service-chute outcome. Other worlds' legacy message policy is unchanged.
- **Prism portrait:** Prism Controls uses the existing portrait-free dialogue layout, avoiding the communicator fallback. No new image is needed for a control interface.

## Presentation and design considerations

- Android named travel is presented as dedicated buttons beneath the description's scroll area. It no longer adds a “Paths:” appendix to Android prose. Uses the existing validated TravelActions; no diagonal or new route was added. Desktop retains its existing travel presentation.
- Room text, dialogue text and response lists indicate when more content is below the visible area.
- Dialogue responses have tighter spacing and no numbering. Existing body font sizes remain readable; both text and choices remain scrollable.
- Bracketed dialogue narration is italicized and tinted without showing the authoring brackets. Action responses use italics instead of an “Action” badge; quest/shop/tutorial labels retain their separate meanings.
- **Puzzle stakes remain a design follow-up:** no automatic fights or first-attempt bonuses were added. The prism is a covert containment operation with retry explanations; adding a fight changes the heist's pacing and resource economy. Review puzzle-specific consequences and rewards after the current interaction fixes have phone acceptance, rather than adding a blanket penalty.

## Verification and phone acceptance

See the adjacent evidence directory for the task-start-to-final JSON delta and check results. Runtime tests cover actual Evelyn/Robert exits, repeated clue inspection, persistence, exhausted laundry-line visibility, prism portrait selection, and existing World 3 navigation. Domain tests cover the main quest and story payoff, all dialogue graphs, discovery prose and content integrity. Existing environmental tests cover the shared renderer's simulation contract.

Phone checks still required: rain continuity on Safehouse Roof, room-change responsiveness, scroll cues and reset behavior, large-font/small-screen layout, narration styling, named travel button accessibility, and the timing of restored story outcomes alongside completion notifications. Automated content/runtime checks do not establish those visual and performance results.

Final checks: **95 tests passed** (56 Android host, 14 desktop runtime/content, 25 environmental simulation); zero failures, errors or skips. Debug APK assembled successfully, with byte parity for all four changed JSON catalogs. Navigation audit and `git diff --check` passed. This build has not been installed on the phone.

## Narration reveal follow-up

Corrected a review finding where narration tint was applied after the typewriter transparency mask, exposing unrevealed action text. Dialogue now applies narration styling first and the shared typewriter mask last. Two focused shared JVM regressions pass, covering every reveal position through tinted italic narration, unchanged text/layout, completed reveal, plain speech and out-of-range reveal counts. Android debug APK rebuilt successfully. Phone visual/performance acceptance remains pending.
