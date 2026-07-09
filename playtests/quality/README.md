# Starborn Quality Reviews

Use these scorecards with `scripts/starborn-test.ps1` reports. Automation proves the game still runs; quality review decides whether a beat is clear, emotional, intuitive, and fun.

Canonical references:

- `docs/story/Emotional_and_Conflict_Map.md`
- `docs/story/Tutorial_Placement_Map.md`
- `docs/story/Content_Standards.md`
- `docs/story/Systems_Overview.md`

Recommended flow:

1. Run a focused suite, for example `.\scripts\starborn-test.ps1 -Suite world1_quality -InstallDebug`.
2. Open the generated `reports/playtests/<timestamp>/report.md`.
3. Fill a copy of `starborn_quality_scorecard.md` for the quest, hub, or system under review.
4. Link screenshots, telemetry notes, and exact flow IDs from the report.
5. Convert low scores into concrete content or UX tasks.

