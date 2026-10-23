# Starborn Maestro playtests

Start with [the setup and coverage guide](../../docs/testing/maestro.md).

```powershell
.\scripts\setup-maestro.ps1 -InstallPlaytest -Device emulator-5554
.\scripts\maestro.ps1 --device emulator-5554 test playtests/maestro/suite_smoke.yaml
```

Flows and helpers are committed; downloaded Maestro, APKs, emulator data, and artifacts are ignored. The pinned runner targets the separate `.playtest` Android app. Tests may reset its data.

Run named flows. The historical full-directory suite includes routes that have not been revalidated against the current UI. Static CI checks YAML structure and references; it does not certify gameplay.
