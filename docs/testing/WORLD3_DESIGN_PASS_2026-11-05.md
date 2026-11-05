# World 3 focused design pass — November 5, 2026

Implements the agreed follow-up to the World 1–3 fun assessment. This work follows release 1.3.108 and is a local candidate for phone playtesting.

## Playable maintenance cover

The existing Skypark guest-registry encounter now asks the player to maintain the crew's cover. The preparation workstation and planning table establish a **scheduled Skypark air-filter inspection**. The Staff Checkpoint badge scanner and Uniform Sorting laundry tags repeat that information, supporting saves already beyond the planning table.

The Curator asks for the work order. An emergency-repair claim lacks an incident number; a private-delivery claim contradicts the uniforms and route. Each receives a distinct response and a recoverable follow-up. The player may correct the request or leave and return. Guest access is credited only when the successful conversation ends, using the existing task/event. No automatic battle or resource penalty is added.

The challenge is deliberately integrated into the existing registry scene rather than becoming another required checkpoint errand. Correct choices explain why preparation worked; Zeke recognizes the same institutional loophole he used at the mine checkpoint.

## Murder deduction

Vance's initial confrontation option no longer provides the solved accusation. The player must identify:

1. **The 21:14 Syndicate Booth badge entry** contradicts Vance's claim that he stayed at his corner table after 21:00. The 21:12 equipment vent and 21:15 drink preparation are not movement records.
2. **Coolant in the glass matching the missing comms-rig core** supplies independent physical evidence when Vance disputes the log. Repeating the same timestamp or citing the clean dispenser does not establish that connection.

Every incorrect argument has a specific rebuttal. Both questions offer an exit. Leaving/loading permits reconsideration; no reward or completed task follows a wrong answer. Existing required inspections and once-only rewards are retained. The terminal now reports raw entries rather than announcing that Vance lied. Nova no longer cites the optional unread datapad as established evidence; Vance reveals his motive in the confession.

## Persistent consequences

- **Night Market Run:** the broadcast describes vendors reopening, their names entering the occupied-stall registry, and the siphoning billboard going dark. The completed room description, Mika's later greeting and the passive **open stalls** inspection preserve the result.
- **Corporate Espionage:** publication produces worker-network responses and a shared claim; private leverage produces a private equipment acknowledgment without publishing the accounts. The **ledger receipts** screen can be read before choosing and repeatedly afterward, including after reload. Both transmission controls retire after either outcome. Stage-sensitive room descriptions reflect the selected branch. The publication milestone now says **Ledger Published**, removing its obsolete promise of price changes. Existing items, XP and branch exclusivity are unchanged; this pass does not rebalance publication versus private equipment value.
- **Prototype Testing:** calibration demonstrates the corrected policy in a recorded trial: a worker silhouette passes safely and an armed drone target is engaged. The completed-stage room description and passive **trial replay** preserve the result. Scrubbed telemetry and the issued reward are acknowledged by the terminal after completion. This is explicitly a recording, not a new live fight.

## Scope and save behavior

The pass changes authored catalogs and regression fixtures. It adds no rooms, connections, diagonal directions, mandatory battles, new progression flags, migration requirements or rewards. Current quest/task IDs and outcome milestones remain stable. Existing completed quests remain completed; returning to a pending registry or case starts its new conversation without inventing completion. Optional outcomes can be inspected after their existing milestones.

The existing rooftop recovery route is retained pending phone pacing feedback. Rain, combat tuning, the Prism puzzle and optional chrono controls are unchanged by this design pass.

The local completionist walkthrough and generated companion reader describe the new choices, outcomes and current Android travel-button presentation. The hosted reader is not published by this pass.

## Validation

Final checks and task-start content deltas are recorded in the adjacent evidence folder. Acceptance focuses on correct/recoverable cover choices, both deduction questions and wrong rebuttals, leaving/loading pending conversations, branch-specific receipts, one-time rewards, action visibility and the earned completionist route.

Human acceptance remains necessary for blind clue comprehension, whether the dialogue adds satisfying tension, response readability, and the perceived payoff of revisiting the city. Automated completion is evidence of supported routes and state behavior, not proof of subjective fun.

Results: **813 full Android host tests passed**, including both earned ledger routes. After the final description adjustment and desktop fixture synchronization fix, **63 focused checks passed** (42 Android host, 21 desktop), with zero failures/errors/skips. The debug APK built successfully and all five changed runtime catalogs match its packaged bytes. The generated reader matches the maintained guide, retains all 70 room references, and includes the new solutions. Navigation audit and whitespace checks passed. The APK has not been installed, and this pass has not been committed, pushed or published.

The first validation run caught a premature inspection visibility error, which was fixed. Initial desktop consequence checks raced restored state; the fixture now waits for UI milestones/completed quests to match the restored session before selecting an action. The corrected runtime suite passes. The full-run log retains those initial desktop findings for traceability.
