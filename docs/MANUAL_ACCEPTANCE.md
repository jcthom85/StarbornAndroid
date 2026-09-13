# Starborn manual acceptance

Use the new debug APK at `app/build/outputs/apk/debug/app-debug.apk` for these
checks. It still identifies as 1.3.32 (116), but contains uncommitted changes after
the Play upload. Play Internal Testing build 116 does not contain these fixes.
Keep a known-good numbered save before testing. Do not clear app storage.

## First phone pass (about 30–45 minutes)

1. Opening and UI: begin or load an early session, open the journal and inventory,
   inspect a long ability description, and try normal and doubled system text.
   Check readable wrapping, reachable buttons and scrolling. Restore text size.
2. Combat interruption: enter a battle, background for 30 seconds, then resume.
   Check health, readiness, sound and controls. Repeat during an enemy telegraph.
3. Battle recovery: after battle starts, allow the autosave to finish, terminate
   the app, relaunch and load Autosave. Expect the same encounter to restart from
   its initial party health and supplies. Win it and check loot is paid once.
   Repeat after the victory screen appears but before leaving it; recovery should
   restart from the unpaid checkpoint, not retain the payout and award it again.
4. Cinematic recovery: interrupt a progression cinematic (cutter surge, relic sync,
   launch, Anchor Drill or final note), relaunch and load Autosave. The scene should
   restart and its quest handoff should finish once. Existing saves stranded before
   pending-scene tracking was added are a separate compatibility case.
5. Puzzle interruption: close/reopen an unsolved tuning puzzle, then background it.
   No success reward should appear until the solution is submitted. Check that a
   restart leaves a usable puzzle action. Exact unsolved slider positions may reset.
6. Audio: repeat background/resume during music, ambience and voiced dialogue;
   try mute/unmute, headphones and a phone notification/call. Check for doubled,
   missing or stuck sound. Emulator counters do not establish audible quality.

## Gameplay acceptance (a separate longer session)

- Play the prepared finale and record loadout, supplies spent and any confusing
  status behavior. The five deterministic seeds are regression evidence only.
- Follow a continuous route and record purchases/sales. The 1,800-credit four-weapon
  build is bought once; carry those weapons forward. Pre-Avatar income of 2,140
  leaves 340, and subsequent income of 500 brings the budget to 840 before any
  additional spending. This is a conditional ledger, not proof of shop access or
  sufficient consumables through every earlier encounter.
- Check route hints and quest terminology without using debug warps. Report room,
  current journal objective and the action/direction that was unclear.
- Check startup, crowded combat, heat/battery and background/resume on the physical
  phone. Emulator debug measurements are diagnostic, not release performance limits.

For each failure, record device/Android version, build source, room/quest, exact
steps, expected/actual result and whether it repeats. A screenshot or short video
and the save slot used are useful. Do not upload private account/device logs.

## Remaining release gates

Old stranded-save repair, interruptions in the middle of multi-action cinematic
callbacks, complete route affordability, and physical-device performance remain
open until supported by targeted evidence. A new signed release and Play upload
are required before these fixes can be tested through Internal Testing.
