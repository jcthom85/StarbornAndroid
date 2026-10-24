# Starborn playtest execution

Start with [the maintained World 1 testing guide](docs/testing/WORLD1_PLAYTEST.md). It provides Mac/Windows setup, named Maestro suites, checkpoint maintenance, evidence requirements and a phone checklist.

On macOS:

```sh
npm ci --ignore-scripts
./scripts/setup-maestro.sh --install --device emulator-5554
./scripts/maestro.sh --suite smoke --device emulator-5554
```

On Windows:

```powershell
npm ci --ignore-scripts
.\scripts\setup-maestro.ps1 -InstallPlaytest -Device emulator-5554
.\scripts\maestro.ps1 --suite smoke --device emulator-5554
```

Use `campaign`, `focused` and `recovery` for the other named suites. The continuous campaign starts at normal New Game and ends at the World 2 crash site; focused cases use earned checkpoints. All tests target the separate `.playtest` app and may erase its saves. Run through the wrapper so `${APP_ID}`, device selection, timeouts and reports are configured consistently.

The old full-directory / Worlds 1–6 suite is historical coverage. It is not certified passing. Host/controller tests, static validation, actual device interactions and human audiovisual acceptance establish different things; none implies the others passed.

Read [current coverage and results](docs/testing/WORLD1_MAESTRO_REFRESH_2026-10-23.md), then record a run using [the evidence template](docs/testing/run-template.md). Human World 1 acceptance uses [this checklist](docs/playtest/WORLD1_ACCEPTANCE_CHECKLIST.md).
