# World 2 pre-playtest dialogue fix

October 29, 2026. Supersedes the production-condition blocker in the earlier World 2 readiness assessments; it does not certify all phone behavior or resolve the unrelated Worlds 3–6 findings.

Implemented authored `quest_task_done`, `quest_task_not_done`, `milestone_set` and `quest_not_completed` conditions in the shared runtime. Android delegates its condition entry point to that evaluator. Unknown condition types now reject the entry rather than granting it. A catalog regression checks the condition vocabulary used by every authored dialogue line and option.

The World 2 refinement and earned campaign test harnesses now use the real production evaluator, replacing their independent implementations. Added actual dialogue-selection regressions on both production entry points: completed crash introduction retires; Botanist cannot turn in with zero through four scans and can with five; Orion offers the side quest after Astra inspection. Added state/malformed-condition coverage.

Validation: **77 Android-hosted tests passed, zero failures/errors**, including World 1 main/completionist and early-Warden recovery routes, Charlie exit/reward tests, the bunk-light interruption, World 2 main/completionist routes, discovery/refinement, data integrity, dialogue/event integration, and the earned campaign through credits. Debug APK assembly passed. Installed successfully on Pixel 8a `46121JEKB11849` using `adb install -r` (1.3.100 / 184, updated 17:37:14); existing app data was preserved. The full route still uses direct speaker calls in places; this fix removes its custom predicate mismatch, not every harness limitation.

A new earned-route recovery test removes the emergency Medkits to represent previously spent supplies, walks to David's visible provisions shop before Zeke treatment, buys one replacement with earned credits, returns to the pod, consumes it in treatment and advances to MQ02. This establishes one affordable recovery route; it does not guarantee recovery after arbitrary spending of all money and saleable possessions. No Medkit content change was needed.

[Machine-readable evidence](evidence/WORLD2_PREPLAYTEST_DIALOGUE_FIX_2026-10-29.json) records test results, corrected production dialogue selections, APK checksum and device installation status.

Tonight's focused phone checks: reopen Zeke after the crash introduction; attempt Botanist turn-in before and after all five scans; check Orion's optional offers after inspecting Astra; confirm each conversation exits and queued rewards remain readable. Existing saves are not reset, and this change does not retroactively reconstruct tasks skipped by an older faulty dialogue turn-in.
