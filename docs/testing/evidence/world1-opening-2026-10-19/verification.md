# World 1 opening verification

October 19, 2026; working tree includes the crew-adoption changes and the opening content corrections.

- Focused JVM run: 43 passed. Suites: Hub1 critical flow 15, narrative visibility 4, opening combat 17, opening migration 3, intro integrity 1, production opening/handoff routes 3.
- Separate corrected pacing run: 2 passed.
- Desktop opening interaction: 1 passed, final test duration 14.152 seconds. The test explicitly drains the prologue/fade and records rather than plays audio; it does not establish real-time presentation quality.
- Android debug assembly succeeded. APK SHA-256: `3ba65449f8d312ef47fe147b301729c815d8cc7243b82e20098834db7ebff0d1`.
- World 1 explicit action references: 82 room definitions checked, zero errors. Global validation still has seven Sector 9/Astra issues.

Controller evidence:

- [Read the diagnostic before combat](read-strip.txt).
- [Engage before reading the diagnostic](fight-first.txt).
- [Full main-route World 2 handoff](world1-handoff.txt).

These are fixed-policy headless controller runs at seed 17 with real combat and no seeded supplies/AP. They skip/drain presentations and use authored puzzle answers. Main-route success is not optional-content, physical-device, audiovisual or fun acceptance.

Local logs: `tmp/world1-opening-tests.log`, `tmp/world1-pacing-correction.log`, `tmp/world1-desktop-opening.log`, `tmp/world1-opening-android-build.log`. The initial red regressions are in `tmp/world1-opening-before.log`; the original slow desktop test's stack is in `tmp/world1-desktop-threads.log`. The final passing run supersedes intermediate test-fixture compile/assertion errors; it does not represent a full 699-test suite rerun.
