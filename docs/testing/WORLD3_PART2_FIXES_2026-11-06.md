# World 3 second-part playtest repairs — November 6, 2026

## Dialogue exits corrected to the requested behavior

Removed the standalone Leave header button from Android and desktop NPC dialogue. Android Back and desktop Escape no longer cancel arbitrary conversations. Ordinary dialogue with no responses continues through its normal text; it receives no generated exit option.

Response menus preserve eligible authored leave responses and place them last. A menu without an eligible authored exit receives a final nonrewarding response: “Let's leave it there for now,” or “Step away from the controls” for consoles/terminals. The generated exit closes the session before running the current line's trigger. Accept/turn-in choices still require deliberate selection and do not count as goodbye responses merely because they end the conversation. Existing authored goodbye lines retain their existing semantics.

This is implemented in the shared dialogue session rather than by editing only Evelyn and Robert. It covers eligible response menus across Worlds 1–6, including conditional response availability. No ordinary linear story line gets an exit response. The previous fixed-header cancellation feature is superseded.

Android touch inspection exposed a second issue: the MORE overflow overlay could cover the final goodbye response after scrolling it into view. Added scroll clearance beneath content and made cue visibility exclude that clearance, so the cue disappears once the actual response/text fits. The final response can be touched without a separate header exit. This adjustment also protects bottom-of-description action words.

## Rest-screen instruction

Removed the visible “Tap anywhere to continue” text from the shared Android rest recovery presentation. Tap dismissal and the actual healing/status effects remain as before.

## Exploration music after temporary activities

The Android navigation routes for shop, tinkering, cooking, fishing and arcade replaced exploration music on entry and stopped their own track on disposal, without restoring the previous owner. Temporary activity music now captures and restores the previous music cue once, provided the activity still owns the layer. A newer room/story track cannot be overwritten by a late disposal callback.

Restoration uses `restartFromBeginning = false`, retaining the audio player's per-track playback bookmark. The streaming player now applies Play's requested fade-in rather than setting its full gain immediately. Playback bookmarks are recorded even while the player is temporarily seeking/buffering, rather than requiring `isPlaying` at the exact switch. Ambient layers and user volume controls retain their own behavior. No exploration restart or unrelated room warp is needed to resume the music.

## Rooftop departure controls

The authored scan and departure events already contain the required successful route: Administrator task complete → scan local curtain → launch toward Foundry. However, rescanning after the one-time scan event was complete had no event to run, and trying to launch before the Administrator task completed had no matching feedback event. Added explicit controller feedback when either roof control has no eligible event, explaining the missing Lens/story progress, Administrator/escort victory, already-recorded scan, or completed departure. Successful controls still execute their existing events and do not bypass combat or award tasks through fallback feedback.

Corrected the roof's post-Lens description and fallback text to distinguish the low-altitude Spire security curtain from the still-intact planetary Shield. No main-route or walkthrough step ordering changes are required.

The user's exact silent state cannot be identified without their save; no connected Pixel was available during diagnosis. Runtime tests cover pre-boss launch feedback, post-boss scan, repeat scan feedback, save/reload and successful launch. Android touch coverage independently checks that the two inline words invoke distinct actions.

## Difficulty finding — no balance change

The corrected reported encounter is **Administrator + Aero Drone**, not Aero Drone + Heavy Mech. Existing production completionist evidence at seed 17 records 62 damage events and 5 Medkits + 3 Medkit I + 2 ration packs used in the boss party, compared with 30 damage events and 1 Medkit in the preceding security party. The earned World 2 Beast trace used 1 ration + 1 Medkit + 1 Medkit I. These are automated policy observations from specific earned builds, not estimates of ordinary player consumption or multi-seed difficulty certification.

The report is a credible encounter-pressure concern. Required-path progression, party equipment/AP allocation, available recovery and boss counterplay should be compared using the actual phone save before tuning. No enemy, player, item, XP or reward values were changed in this repair. Optional combat cannot be assumed to excuse an unreasonable main-route difficulty spike.

## Verification

Final test/build results are recorded in the evidence directory. Regression coverage includes response-only exit eligibility, no current-line rewards on generated exit, fixed-order authored goodbye choices, rooftop event/state feedback, temporary music ownership and real Android playback resume/fade. Phone acceptance still matters for perceived music transitions and the user's actual rooftop save.

Final results: **815 full host tests passed**. After the final refinements, **17 focused host + 10 desktop runtime checks passed**. **8 Android instrumentation tests passed** on an isolated Android 17 phone emulator, including actual goodbye/rooftop touch input and real ExoPlayer position/fade checks. Debug APK build, runtime-room JSON parity, navigation audit and whitespace checks passed. The first Android run exposed the cue occluding goodbye and buffering bookmark loss; both are corrected and the complete eight-test run passes. The initial rooftop runtime assertion read the preceding scan message before dismissal; waiting for that message before checking the repeat interaction resolved the fixture ordering issue.

The separate integrated guide's exit wording is updated locally and its separate reader rebuilt. The original walkthrough/checklist is untouched. No version bump, commit, push, GitHub Pages publication or Pixel install was performed in this pass.
