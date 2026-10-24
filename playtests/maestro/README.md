# Starborn Maestro playtests

Use [the maintained World 1 testing guide](../../docs/testing/WORLD1_PLAYTEST.md) for Mac/Windows setup and commands. Read [coverage and current results](../../docs/testing/WORLD1_MAESTRO_REFRESH_2026-10-23.md) before treating a flow as verified.

```sh
./scripts/setup-maestro.sh --install --device emulator-5554
./scripts/maestro.sh --suite smoke --device emulator-5554
```

Windows equivalents are `scripts/setup-maestro.ps1 -InstallPlaytest -Device emulator-5554` and `scripts/maestro.ps1 --suite smoke --device emulator-5554`.

`suites.json` defines smoke, continuous campaign, focused regression and recovery entrypoints. Chapters under `world1/` preserve campaign state; focused cases load earned, content-hashed checkpoints. Other routes remain historical. Run named suites/flows, not the full directory.

Only the isolated `.playtest` app is supported. Tests may reset its data. Tool downloads, APKs, emulator data and run artifacts are local and ignored. Static checks do not establish live gameplay acceptance.
