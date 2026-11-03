# Lighting motion rework — November 3, 2026

The first three-room pass varied brightness too weakly over fixed pools. The user could not see a useful difference. This revision adds source-appropriate spatial motion, stronger receiving-surface illumination, and a brief fixture-settling sequence. It does not change gameplay, move the camera, or distort the background artwork.

## Current room behavior

- **Nova's Bunk:** the faulty amber fixtures dip and regain strength for roughly 1.8 seconds as the lighting layer starts, then settle to an almost constant warm pool on the sleeping shelf, wall and floor. The fixture itself stays fixed; the pool expands with available light rather than rocking around without a cause. `light_on` remains authoritative. Fresh lighting-layer creation (including a fresh saved-lit scene) can show this brief fixture behavior; it is not a separately persisted one-time story event.
- **Overgrown Glade:** cyan/violet mushroom clusters breathe independently with visibly changing halo reach. A separate canopy treatment projects soft leaf shadows and sunflecks onto a path mask. Their slow, coherent displacement represents wind moving the pictured overhead foliage; it does not move the painted mushrooms or sweep an arbitrary spotlight through the scene.
- **The Campfire:** reflected blue light changes position and reach over the stone ring, neighboring blocks and approach. Thin animated luminous contours remain rooted inside the existing painted blue flame. Its palette and origin match the background; there is no new orange fire or unrelated particle shower.

Localized motion remains clipped to authored receiving surfaces and follows the same image-relative Fit/Crop/transition geometry on both platforms. Source glows remain anchored. Reduced effects, disabled flashes/motion and accent suppression freeze the animation into steady illumination; high contrast attenuates it; Off removes it. Existing foreground/overlay pause and scene-memory bounds remain in use.

## Review the motion

Each sample is six seconds at 8fps from the production renderer. Gameplay uses the renderer's normal frame clock. The comparison labels show the rejected **previous pass** on the left and **reworked motion** on the right. These are isolated artwork/effect captures, not the full exploration HUD.

- [Bunk comparison](../../testing/evidence/lighting_motion_rework_2026-11-03/pit_nova_bunk-comparison.webp)
- [Glade comparison](../../testing/evidence/lighting_motion_rework_2026-11-03/sector9_landing_glade-comparison.webp)
- [Campfire comparison](../../testing/evidence/lighting_motion_rework_2026-11-03/source_campfire-comparison.webp)

The bunker comparison includes its new initial settling phase; the old samples began after two seconds, whereas reworked samples begin at 0.35 seconds. These samples illustrate behavior rather than identical historical activation timing. Preview repetition is not a claim of a seamless production loop.

A quantitative check counts the maximum percentage of frame pixels whose RGB channels differ from the sample's first frame by at least 8/255. Previous vs. reworked samples: Bunk 0.00% / 20.66%; Glade 0.06% / 5.87%; Campfire 0.26% / 8.20%. This verifies more than a handful of changing pixels; it is not a substitute for judging naturalness or fatigue in live play. Raw counts are saved alongside the previews.

## Verification

- 25 environmental runtime tests pass, covering all 477 room definitions, mask/state validation, legacy compatibility, independent bounded waves, motion roles and accessibility behavior.
- Two desktop render tests pass with real artwork in Fit and phone Crop. They now require perceptible temporal pixel change as well as localized rendering and unchanged side margins; steady flash-disabled output and actionable controls are retained.
- All 783 Android host tests pass, including earned campaign routes. Ten World 1 checkpoints were regenerated and verified after the lighting/Mira JSON changes.
- A native Android lighting test passes across all three rooms and five room-action clicks. New native captures are saved under the evidence directory's `android/` folder.
- Android compilation, debug/test APK assembly, lint and desktop asset parity pass. Later card/pill changes were validated separately in the linked report.

Evidence: `docs/testing/evidence/lighting_motion_rework_2026-11-03/`. Physical Pixel GPU/battery acceptance and live visual judgment are still pending. The latest APK has not been installed and existing desktop sessions have not been restarted by this rework.

See also [Android card, pill and Mira changes](../../testing/ANDROID_CARD_PILLS_AND_MIRA_2026-11-03.md).
