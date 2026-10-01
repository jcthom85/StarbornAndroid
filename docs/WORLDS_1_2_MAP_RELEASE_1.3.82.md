# Worlds 1–2 and Map release 1.3.82

- Version name: `1.3.82`
- Version code: `166`
- Game commit: `022741c5` (`feat: refine Worlds 1-2 and field map; release 1.3.82`)
- Branch: `feature/multiplatform-port`
- Google Play track: `internal` (bundle accepted and edit committed)
- Bundle SHA-1: `cd248c1ca7331b7cf2d7fe9b67e78c53071dfa03`
- Bundle: `app/build/outputs/bundle/release/app-release.aab` (1188.8 MB)

The signed release bundle completed with `:app:bundleRelease` on October 1,
2026. Google Play accepted version 166 and committed it to the internal track.
Gradle emitted existing deprecation and compiler warnings; the build succeeded.

The focused release regression set passed 61 tests: `World2RefinementTest` 11,
`World1RecentChangesTest` 13, `QuestRuntimeManagerTest` 7, and
`DataIntegrityTest` 30. The initial DataIntegrity run caught missing action-name
references in two World 1 descriptions; both were corrected and all 30 tests
passed on rerun.

Device playtesting remains necessary for World 1–2 pacing, puzzle feel, combat
balance, character writing, and Map tab gestures and layout.
