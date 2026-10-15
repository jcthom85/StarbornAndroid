# Popup alignment and desktop room-transition repair - October 15, 2026

This implements the common interaction rules from the [popup audit](POPUP_DISMISSAL_AUDIT_2026-10-15.md). The audit and its JSON appendix remain a snapshot of the earlier behavior.

## Room-transition repair

Two rendering paths allowed sharp artwork into the side atmosphere:

- Before asynchronous blur preparation finished, reflected extensions fell back to the original sharp bitmap. Cached blur now initializes immediately when available; otherwise the extensions retain their existing blurred state or use an opaque dark cover, never sharp bitmap reflections. The seam layer uses the same covered fallback.
- During parallax travel, the sharp center image could move beyond its fitted bounds and draw over the atmosphere. A fixed clipping viewport now contains all center-image motion. The blur preparation is also skipped for center-only layers.

The blur cache remains asynchronous and its map accesses are synchronized. This avoids adding a full-resolution blur to the transition frame.

Regression tests suspend blur completion across two different synthetic room images and inspect both side regions before and after completion. A separate pixel check translates the sharp center in both horizontal directions and verifies that neither side is overwritten. Existing backdrop tests cover four window ratios and real room, hub and cinematic assets.

## Implemented interaction rules

| Family | Current behavior |
| --- | --- |
| Tutorial explanations and perk guidance | Body tap/click or explicit acknowledgement; desktop information-card Enter/Space/Escape; Android Back acknowledges combat explanation stages. Required-action hints remain action-driven. |
| Milestones | Body tap/click or Dismiss on both platforms. |
| Level-up, rest result, blocked-room exit, locked overworld location | Body tap/click acknowledges. Existing buttons remain. Desktop information dialogs support Enter/Space/Escape; Android Back/controller paths acknowledge the applicable result. Rest recovery waits for acknowledgement on both platforms. |
| Important quest detail and summary notifications | Body tap acknowledges; Details and other child controls retain their own actions. Desktop Enter/Space/Escape and Android Back dismiss the active notice. |
| Small quest updates | Tap/X or automatic expiry. Android also retains swipe dismissal. |
| Toast feedback | Tap or automatic expiry; ordinary progress toasts use 1.8 seconds on both platforms. |
| Standard story cinematics | First body tap/click reveals the text, next advances. Desktop Continue uses the same reveal/advance action, with Enter/Space support. Authored timed advancement and explicit Skip remain. Illustrated cinematics retain their existing behavior. |
| Defeat and retreat results | Automatic return on both platforms, with a short result delay. Victory reward pages remain explicit; Android Escape/controller B no longer advances victory pages. |
| Choices, purchases, scrap, meal replacement, mod moves and other confirmations | Body remains inert. Explicit action proceeds; cancellation remains cancellation. |
| Browsers and tools | Existing Close/Back and nested navigation remain. Navigation console cancellation is permitted during transit without cancelling the journey. Save/load Back respects the busy guard. |
| Authored inspection restrictions | Root keyboard, Back, right-click and tap paths respect tapToDismiss instead of bypassing it. The explicit Continue button remains available. |
| Homecoming story conversation | Outside/Back cannot complete the entire scene. The authored reveal/advance sequence completes it. |
| Hover tooltips, guided action cues and scene transitions | Retain their distinct hover, action-driven and automatic completion behavior. |

Desktop acknowledgement consumes both key edges and acts on release, so one press cannot spill into the next queued information card. The foreground priority follows the visible narration/announcement/prompt order and does not acknowledge hidden cards behind dialogue or cinematics.

## Validation

25 focused desktop tests pass with 0 failures and 0 skips, including blur-loading and viewport pixel checks, information/choice/cinematic interactions, queued keyboard acknowledgements, tutorial stages, quest presentation, and combat-return persistence. Android production code and instrumentation sources compile, and the debug APK assembles. Android instrumentation was not executed on the emulator during this pass; the existing play session was left untouched.

Evidence:

- Desktop test XML: `tmp/game-evaluation-results/desktopApp/test/xml/`.
- Validation logs: `tmp/popup-alignment-final.log` and `tmp/popup-alignment-verified.log`.
- Controlled pending/ready blur images: `desktopApp/build/reports/desktop/screenshots/backdrop-loading/`.
- Real-asset ratio screenshots: `desktopApp/build/reports/desktop/screenshots/portrait-backdrops/`.
- APK: `app/build/outputs/apk/debug/app-debug.apk`.

The pixel checks and screenshots verify the composited renderer; live desktop and Android manual acceptance remain useful for subjective timing and feel.
