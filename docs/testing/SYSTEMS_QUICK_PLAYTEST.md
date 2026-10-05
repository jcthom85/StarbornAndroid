# Quick systems playtest

## Google Play internal build

Version 1.3.87 (171) is the internal tester release with **Playtest Scenarios** on the normal title screen and the in-game walkthrough reader. The uploaded 1.3.86 (170) release uses normal release settings and hides both tester entry points. Launching a scenario opens a separate Test Session; normal campaign saves remain in the main session. To resume a test checkpoint after restarting the app, select **Resume Playtest Saves**, then **Load Game**. To leave testing, quit to the test title screen and select **Return to Main Game**.

Internal release command: `./gradlew.bat :app:bundleRelease -PenablePlaytestScenarios=true`. Regular release builds default to hiding the scenario menu. Keep the tester-enabled artifact on the internal track; do not promote it to a public track.

## Separate APK: install and launch

Install `app/build/outputs/apk/playtest/app-playtest.apk` on your Android phone. Its launcher name is **Starborn Playtest** and its package is `com.junewiregames.starborn.prealpha.playtest`. It installs beside the Google Play game and has separate app data, including test-session saves. You may need to allow installation from the app opening the APK.

From this computer, with USB debugging enabled and one phone connected:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app/build/outputs/apk/playtest/app-playtest.apk
```

To rebuild:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assemblePlaytest --console=plain
```

## The shortest useful session (about 30 minutes)

Open **Playtest Scenarios** on the title screen. Search for the names below, read the starting state and checklist, then select **Launch / Reset**.

1. **Fishing / sector9 stream**: compare weakest and strongest rods; catch, fail, cancel; inspect reward/journal feedback. Existing regional fishing scenarios include all gear.
2. **Cooking / All Recipes and Chefs**: compare chefs; cook a meal and a snack; cancel then confirm meal replacement. Check ingredient costs, preview, and narrow-screen readability.
3. **Cooking / Meals and Snacks in Combat**: Nova starts with Mineral Trail Mix equipped. Eat a meal, then head to nearby Stream Pools, Wetlands, and Cave Depths for real encounters. Test snack cooldown, combat food, and Well-Fed charges after victory/retreat. Save between battles.
4. **Tinkering / All Schematics**: inspect recipe text, component sockets, compatibility, and result feedback.
5. **Tinkering / Salvage and Missing Materials**: protect the equipped lens and Cryo-Inductor; scrap the spare grip; attempt a craft without materials.

If time permits, run **Fishing / First Kit Discovery**, **Cooking / Discover Regional Recipes**, and **Tinkering / Learn a Schematic**. These check access and explanations rather than just fully unlocked menus. **Tinkering / Exact Cryo Requirements** and **Tinkering / Missing Scrap** already cover the opening workshop recipe.

## Saves and reset

- Use manual **slot 1** as the untouched starting checkpoint; **slot 2** for an interesting intermediate state.
- To resume, use **Load Game**. Relaunching a scenario rebuilds its starting state; it does not resume a checkpoint.
- To reset, quit to title, select the same scenario, and use **Launch / Reset**. Manual slots remain available; overwrite them deliberately when switching tests.
- The Playtest app uses one shared test-save collection, not a separate collection per scenario. Label your notes with the scenario and slot.
- Continue your real World 1?2 campaign in the Google Play app to judge pacing, scarcity, and usefulness. Stocked scenarios cannot establish that balance.

## What to report

For each issue, record scenario name, last action, expected result, actual result, screenshot, and saved slot if reproducible. Also record things that work but feel dull, unclear, too easy, or too demanding. One sentence per observation is enough.

## Verification

The Playtest APK is built with debug signing and embedded world assets for direct installation. The normal release build keeps its scenario menu disabled. Compilation/build checks do not establish that every scenario has been played successfully on a device.
