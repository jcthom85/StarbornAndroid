# World 1 regression test handoff

## Task

Run the prepared World 1 tests in this working tree and report their results.
The user is switching models in the same chat. The first focused run compiled and executed 23 tests: 19 passed and 4 failed.
The optional riot objective lacked its asset flag. Three XP failures exposed two
legacy events whose encounter_id filters were ignored, adding 480 XP to unrelated
victories. Both production issues are now corrected; rerun both sets below.
The follow-up rerun passed all 24 focused and 31 related regression tests.
The six orphaned relay quest events have since been removed, along with their
missing-quest exemption. The focused test now checks retirement, unrelated
victories and stale saved quest state. Rerun the focused set plus DataIntegrityTest
for this cleanup if further changes occur. The cleanup was verified with all 13
World1RecentChangesTest and 30 DataIntegrityTest methods passing (43 total, no
failures/errors/skips; log: build/world1-luna/relay-cleanup.log). The broader
asset check also found the new Rebel Terminal speaker missing its voice profile;
it now uses "none", matching other terminal speakers.


Do not change production code, weaken assertions, commit, upload, install an APK,
clear application data, or revert any working changes. If compilation or tests
fail, preserve the evidence and report the problem. Do not infer a pass from an
up-to-date task or an empty test selection. No subagents are needed.

## Scope and expectations

20 new test methods:

- `World1RecentChangesTest`: 13 methods covering early/normal/legacy certification,
  wrong-room and unearned credit, actual disk save/load, idempotent recovery and
  payout protection, retired relay reward protection, trainer presentation without stat changes, the authored
  optional objective, cart ordering/permanence/old saves, and terminal wrong
  choices/disconnect/reopening/replay/partial saves.
- `CounterTuneSignalTest`: 4 methods covering stage prerequisites, full-trace
  cancellation, diagnostic directions and tolerance boundaries.
- `QuestRuntimeManagerTest`: 3 added methods covering optional tasks not blocking
  progression, skipped tasks not being marked complete, and earned optional
  completion being retained. Existing methods in this class should run too.

Event tests use real asset catalogs, EventManager, DialogueService/parser and
GameSessionPersistence. Presentation/reward hooks are headless. The focused
dialogue evaluator fails closed for unsupported conditions and is not the
Android ViewModel's condition evaluator. These tests do not establish UI feel,
rendering, full combat balance, process-death atomicity, or desktop progression.

## Run 1: new coverage and existing optional-task runtime coverage

Use PowerShell in `C:\Users\jcthomas\StudioProjects\StarbornAndroid`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
New-Item -ItemType Directory -Force -Path build/world1-luna | Out-Null
$w1TestArguments = @(
    ':app:testDebugUnitTest', '--console=plain', '--rerun-tasks',
    '--tests', 'com.example.starborn.domain.playtest.World1RecentChangesTest',
    '--tests', 'com.example.starborn.domain.playtest.CounterTuneSignalTest',
    '--tests', 'com.example.starborn.domain.quest.QuestRuntimeManagerTest',
    '--tests', 'com.example.starborn.data.DataIntegrityTest'
)
& .\gradlew.bat @w1TestArguments 2>&1 | Tee-Object -FilePath build/world1-luna/focused.log
$w1FocusedExitCode = $LASTEXITCODE
Write-Output "Focused Gradle exit code: $w1FocusedExitCode"
```

Read `app/build/test-results/testDebugUnitTest/TEST-*.xml` after the run. Confirm
both new classes have 13 and 4 executed tests respectively, and the three named
optional-task methods are present in the existing runtime class. Inspect tests,
failures, errors, skips and individual failure stack traces. Save a summary before
the next run because Gradle replaces the report directory.

If Run 1 fails or selects no tests, stop and report. A compile error is not a
failed gameplay assertion; distinguish the two.

## Run 2: related existing regressions, only after Run 1 passes

```powershell
$w1ExistingArguments = @(
    ':app:testDebugUnitTest', '--console=plain', '--rerun-tasks',
    '--tests', 'com.example.starborn.domain.dialogue.Hub1CriticalFlowTest',
    '--tests', 'com.example.starborn.domain.quest.SideQuestPermutationIndependenceTest',
    '--tests', 'com.example.starborn.domain.session.OpeningNarrativeMigrationTest',
    '--tests', 'com.example.starborn.domain.playtest.WorldOneProgressionAuditTest'
)
& .\gradlew.bat @w1ExistingArguments 2>&1 | Tee-Object -FilePath build/world1-luna/existing.log
$w1ExistingExitCode = $LASTEXITCODE
Write-Output "Existing Gradle exit code: $w1ExistingExitCode"
```

Inspect the new XML results too. Do not expand to emulator tests or the entire
campaign suite in this bounded handoff.

## Return in this chat

Report commands/exit codes, class and test counts, failures/errors/skips, and paths
to the logs and reports. For failures include the method, expected/actual result,
relevant stack frame and whether it appears to be harness, compilation, or
production behavior. Label that classification as tentative where uncertain.
State whether any files were changed. Passing this handoff is regression evidence,
not acceptance of puzzle fun or a complete World 1 playthrough.
