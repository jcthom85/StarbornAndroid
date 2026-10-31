# World 2 Part 1 — second-half critical fixes

October 30, 2026. Implements the critical items in the second-half notes, excluding all deferred resident/backstory decisions and fishing design changes.

- **Long minimap paths:** map data now retains the geometry of an existing connection when its destination dot lies beyond the small viewport. Android and desktop draw the continuation clipped to the map window; they do not expose the remote dot's name, enemy or service details. Darkness/preview protections remain. Regression covers both ends of the actual three-grid-space configuration.
- **Patrol Graveyard transceiver:** changed from an ineffective duplicate of Signal Hollow's conditional `trace_beta` trigger to a passive contextual inspection using its existing authored description. It works before/after accepting Lost Patrol and does not grant cutter materials or skip Signal Hollow's evidence step.
- **Ridge Overlook Cache:** the one-time Film 04 event receipt now retires the action. Award-once and action disappearance persist across reload, including existing event receipts.
- **Orion introduction:** the pod entry narration refers to its occupant/the sleeper, avoiding assumed recognition before the introduction. Quest requirements and state IDs are unchanged.
- **Scans and REST:** reran the previous active-quest visibility, repeated healing/full-health feedback and one-time-supply checks. No new fishing setup changes were made.

Validation: **119 tests passed, zero failures/errors**; Android debug APK assembly and desktop Kotlin compilation passed. Includes World 1/2 earned routes, completionist/recovery checks, real fishing-cast/reward regressions, full campaign, live-controller inspection/cache persistence and ten camp repeat-rest tests. Existing host-route limitations apply; phone presentation still requires acceptance.

No ADB device was connected. APK built at `app/build/outputs/apk/debug/app-debug.apk`; not installed in this pass. No commit or push performed.

[Evidence](evidence/WORLD2_PART1_SECOND_HALF_CRITICAL_FIXES_2026-10-30.json) · [complete remaining important considerations and fishing discussion](../playtest/reports/starborn_world2_part1_remaining_important_considerations.md).
