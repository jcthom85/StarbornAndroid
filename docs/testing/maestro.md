# Android Maestro testing

Start with [World 1 setup, named suites and evidence rules](WORLD1_PLAYTEST.md). It covers macOS and Windows, the pinned Maestro 2.11.0 runner, isolated `.playtest` app, earned checkpoints, verification commands and maintenance.

[Current World 1 findings, coverage and results](WORLD1_MAESTRO_REFRESH_2026-10-23.md) distinguishes authored automation from actual device evidence. [Phone acceptance](../playtest/WORLD1_ACCEPTANCE_CHECKLIST.md) remains required for pacing, sound, clarity, accessibility and encounter variation.

Run the four named suites (`smoke`, `campaign`, `focused`, `recovery`) or isolate a named YAML. Other flows are historical unless included by these entrypoints; the full-directory and Worlds 1–6 aggregators are not accepted regression suites. Legacy launchers remain available until replacement gameplay evidence and caller migration justify removal.

Static CI validates structure/references, isolated app parameters, current scenario/NPC/action identifiers, source-selector drift and earned-checkpoint freshness. It does not start an emulator or certify gameplay. Local artifacts and downloaded tooling remain ignored.
