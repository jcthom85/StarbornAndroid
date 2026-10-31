# World 2 fishing and important considerations implementation

October 30, 2026. Implements the approved fishing direction, followed by unambiguous improvements from both halves of World 2 Part 1 notes. **23 of 34 considerations addressed; 11 remain for discussion/review.** Resident backstory/placement (first-half critical #7) remains separately deferred. These changes are not full-world regression certification.

## Changes made

- Android fishing now uses a portrait water scene, rod/line tension cues, short contextual instructions, an upward hook swipe and an equivalent button. Hold to reel and release for slack remain the core controls. Motion remains optional. The protected first bite waits for readiness. Setup is quieter, catch history collapses, and results clearly confirm inventory delivery once before another cast. Existing fish behavior, equipment, rewards and balance remain intact; desktop retains its own fishing layout.
- Removed NPC inline background highlighting; aligned action colors with the room theme. Party selection now signals horizontal overflow. NPC/service pills fit labels more closely while retaining touch heights.
- Combat abilities use clearer headers and full-width descriptions, with large-text controls arranged separately. Removed attack-power percentages from the affected quick/detail presentations. Enemy names wrap above HP. Victory presentation lasts two seconds; acquisition copy says Ability.
- Tutorial titles omit redundant bracket categories and combat copy no longer calls the setting World 2. New-quest details have a slower entrance and dimmed backdrop. Autosave appears above Quicksave.
- Discovery narration now blocks item notifications until dismissed, preventing the reward flash. Film 3 discovery copy describes its recovery. Signal Reflection gives context outside its optional quest instead of silently doing nothing.
- Cooking uses one styled feedback card rather than redundant notifications. Shop purchase feedback follows the theme, and shop speech reuses the normal dialogue overlay, including portrait animation. During typewriting, the reserved response area reveals text without selecting a response; visible choices retain their normal behavior. Action responses use an Action tag instead of raw bracket directions.
- Longer music fades soften track changes. An original short rest chime replaces the bunk-light cue. Sentinel-3 uses soft machine murmurs with readable dialogue instead of bracketed voice directions. Audible quality still needs listening on a device.
- The Prism panel has consistent dark styling and a Capture control outside the scrolling body. Stalker-Vine has original transparent plant-creature art in place of the bat assignment; see [asset and full generation prompt](STALKER_VINE_ASSET_2026-10-30.md).

The [complete consideration matrix](../playtest/reports/starborn_world2_part1_remaining_important_considerations.md) preserves all 34 notes and their individual disposition. Shared UI fixes apply where those components are reused; no resident backstory, enemy balance, reward distribution or later-world progression was rewritten.

## Evidence and limits

- 141 hosted unit/integration tests passed, including fishing cues/swipes, tutorial formatting, dialogue/audio behavior, prompt sequencing, World 2 interactions and production campaign routes.
- Two desktop Compose presentation tests passed for the fishing scene and compact Prism panel.
- Four Android emulator instrumentation tests passed: a full fishing catch loop with swipe/button readiness, reel/slack, one inventory award and another cast; ability controls at normal and double text size; and safe dialogue reveal/response selection. The first fishing test attempt exposed a held test pointer after landing; the test now releases it before the next tap and passed on rerun.
- Debug APK assembled successfully; diff whitespace validation passed. [Evidence manifest](evidence/WORLD2_FISHING_AND_IMPORTANT_CONSIDERATIONS_2026-10-30.json) records source hashes, test counts and APK SHA-256.
- Android tests used a temporary API 37 arm64 emulator. The emulator was shut down afterward. The connected Pixel was unavailable; this build has **not** been installed or accepted on the Pixel. No commit or push was performed in this pass.

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Screenshots: [actual Android fishing](evidence/world2_polish_2026-10-30/fishing-android.png), [shared fishing scene](evidence/world2_polish_2026-10-30/fishing-scene.png), [Prism panel](evidence/world2_polish_2026-10-30/prism-panel.png).

## Discussion backlog

1. **Residents and Charlie:** residents' common reason for being here, Megan's role/station, Charlie's name discovery, play and payoff. No independent narrative rewrite.
2. **Combat direction:** Fight pill appearance (IC-04), encounter composition (IC-24), crafting reward distribution/incentives (IC-27), aggression and approach presentation (IC-28). The [encounter-size audit](WORLDS_2_6_ENCOUNTER_SIZE_AUDIT_2026-10-30.md) documents actual static groups; event-spawned fights need separate review.
3. **Quest information:** spoiler boundaries (IC-09) and whether active-quest organization actually needs changing (IC-10).
4. **Discovery writing:** agree when narration adds value (IC-13), and natural nouns versus scan instructions (IC-34). The [96-interaction audit](WORLDS_2_6_DISCOVERY_PRESENTATION_AUDIT_2026-10-30.md) is read-only; no blanket narration was invented.
5. **Camp and set-piece decisions:** whether first-visit supplies belong at camps (IC-29), and how the Temple-to-Hall opening should feel physically (IC-31).

These groups contain the 11 held matrix entries (04, 09, 10, 13, 20, 24, 27, 28, 29, 31, 34), plus separately deferred resident #7. Deeper music composition and fishing difficulty tuning remain runtime review, not speculative changes.

## Focused Pixel checks

- Fish from setup through bite, hook, protected readiness, surge/slack and result. Try both hook methods, cancel, background/resume, and fish again. Confirm inventory increments exactly once and survives saving/reloading. Assess catch length, optional motion and accessibility on physical hardware.
- Open a narrated container: details first, then each item notification after dismissal; no reward flash. Check a quest-active and inactive Signal Reflection.
- Reveal long NPC dialogue by tapping the lower panel, then choose and leave normally. Try shop speech/purchases and cooking feedback.
- Check party scrolling, long enemy names and abilities at increased font size. Confirm victory timing, save ordering and Prism Capture visibility.
- Listen to track transitions, rest chime and Sentinel-3 at normal volume. The emulator used no audio, so listening remains required.
