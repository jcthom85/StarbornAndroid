# Exploration rewards and HUD pass

## Acquisition
Single-item rewards now use the Android ItemGrantedBanner composition, original category colors and preview icon mapping, item title/quantity/category/description, sequence counter, original font families and tap-to-dismiss behavior. The card uses the same bottom placement and a bounded desktop width. Batch rewards use Android's compact sparkle-icon banner and original acquisition summary.

## Tracked quest
The card shares the quest notification's dark panel, accent border/gradient, star, heading treatment and objective markers. It shows shared runtime objectives and completion counts, with a quiet highlight on progress changes. It opens the selected tracked quest in Journal and disappears when no active quest is tracked. No authored quest text or progress rules changed.

## Minimap
Kept the independent desktop top-right frame, subtle grid and connection lines. Restored Android's fixed current-room center, north-up pips and gold crosshair, preview/dark-room states, original service glyphs/colors and node-exit arrows/locks/stair chevrons. The whole minimap opens the full map; the extra Open map label is gone. Visibility remains based on shared discovery/preview data.

## Review
Kotlin compilation and offscreen renders succeeded. Reviewed the exploration composition and item sequence card at 1440x900 and the card at 900x650. The local render fixture used the original bunk-light action to produce a completed objective; preview map cells were exposed only in the fixture. Screenshots: desktopApp/build/reports/desktop/screenshots/hud-acquisition. These checks do not cover every late-game item, quest or map state.

## Background seams and dark-room map

The shared portrait backdrop now feathers the outer 20 dp of the fitted artwork into a lightly blurred reflection, which fades over 96 dp into the broad blur. Original aspect ratio and layout bounds remain intact. Both reflection layers follow cinematic camera transforms. Blur variants are cached by source image and blur strength.

The minimap has no visible heading. Dark rooms retain the same card, with a static interference and scanline treatment; topology, service glyphs, exits, and map activation are withheld. The obscured card also renders when the shared minimap state is unavailable. Static interference avoids continuous HUD flicker.

Reviewed offscreen exploration and obscured-map screenshots in build/reports/desktop/screenshots/seams-minimap.

## Stationary room-transition atmosphere

Room travel now composites the reflection and center artwork separately. Incoming and outgoing reflection layers remain stationary and crossfade with smoothstep easing. Only the center image receives directional translation, scale, and outgoing blur, inside a clipped offscreen frame with a stationary feather mask. HUD layout is composed once above these layers. Cinematic camera transforms still apply to both artwork and reflections.

Compiled and reviewed start, middle, and end presentation fixtures in build/reports/desktop/screenshots/stationary-sides. The fixture uses the same artwork for both rooms to isolate reflection movement; different-room gameplay was not exercised in this pass.
