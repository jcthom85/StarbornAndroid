# Desktop portrait backdrop pass

Implemented October 7, 2026.

## Presentation

- Exploration, hubs, and the illustrated intro share `DesktopPortraitBackdrop`.
- The original image is centered, fitted within the viewport, and keeps its intrinsic aspect ratio. Every image edge remains visible.
- Mirrored, blurred copies fill the surrounding space. A subtle gradient darkens the side extensions while preserving the sharp center.
- Cinematic camera movement and hub parallax affect the extensions. Cinematic fades, timing, effects, sound dispatch, and input blocking remain in the existing renderer.
- Center and side bounds are available through `PortraitBackdropLayout` for the next HUD pass. Existing HUD placement is unchanged.

## Rendering

Actual asset images use a cached, downsampled Skia blur rather than a full-screen blur on every animation frame. The blur cache holds at most 12 images; the sharp center continues to use the full-resolution asset. Decoding and blur generation run off the UI thread.

## Verification

All 9 selected checks passed with no failures or skips: portrait geometry and center pixels, room/hub/intro screenshots, live hub, cinematic progression and sound dispatch, opening interactions, actionable prose, and dialogue presentation.

Screenshots cover 16:9, 16:10, 21:9, and narrow windows. Art is fully visible and side extensions fill the viewport. Reports are in `desktopApp/build/reports/desktop/screenshots/portrait-backdrops/`.

Physical audio playback and native monitor DPI behavior still require a local playthrough. The automated checks exercise audio commands and Compose window geometry.

The Windows portable application was rebuilt successfully. Its packaged smoke check passed from outside the repository: 1,327 assets, 477 rooms, 4 characters, bundled Java 21.0.12.1, exit code 0. The updated archive is `desktopApp/build/distributions/Starborn-Windows-portable.zip`.

## Next pass

Use the exposed side bounds to arrange desktop HUD features after reviewing this backdrop treatment in play.
