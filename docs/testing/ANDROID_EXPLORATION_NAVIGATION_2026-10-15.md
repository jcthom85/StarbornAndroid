# Android exploration navigation - October 15, 2026

## Implemented layout

The floating bottom-left Overworld/Disembark button is removed. The wide contextual exit under the room description remains the single visible exit control.

Menu occupies a fixed bottom-center position above Android's navigation inset. Its existing 54-dp height and tutorial beacon remain. The surrounding area is transparent, with no full-width opaque footer. Menu is disabled during blocking interactions and room transitions.

The south and lower diagonal direction indicators occupy a separate lane above Menu; all direction targets have a minimum 48-dp touch area. Room entities render inside a bounded viewport above the bottom controls. The description/entity column scrolls when it exceeds that viewport. Transient prompt/toast placement respects system insets and sits above the control lanes.

## System Back behavior

Android's committed edge-back gesture and system Back button now invoke the contextual exit from eligible rooms. Ordinary in-room directional swipes retain their movement behavior.

The shared screen Back handler first dismisses the active popup, tool or field menu. At the room root, it ignores input during loading, transitions and blocking scenes. Demo sessions retain their existing root Back behavior. Eligible campaign rooms call the existing requestReturnToHub path, retaining the opening-story gate and the Astra cargo ramp's disembark behavior. System Back in other campaign rooms does not leave exploration. Keyboard/controller cancel uses the same popup priority and retains the existing title-exit confirmation fallback outside eligible rooms.

## Validation

- All 18 ExplorationViewModelTest checks pass, including hub eligibility, story restrictions and Astra return-location behavior.
- Production Android UI and both new instrumentation cases compile; debug APK assembles.
- The new device cases exercise centered Menu/south-arrow target separation at two portrait heights, exactly one visible Overworld exit, and popup/menu-before-hub Back priority in an isolated session.
- Device instrumentation was not executed during this pass; the existing emulator play session was left untouched. Live acceptance of thumb reach and enemy placement remains pending.

Build: `app/build/outputs/apk/debug/app-debug.apk`.
Logs: `tmp/android-navigation-validation.log` (passing unit checks; initial instrumentation compile error corrected) and `tmp/android-navigation-verified.log` (successful final compile/build).
