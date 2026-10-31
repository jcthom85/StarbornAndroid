# World 2 art corrections

October 31, 2026. Follows the functional second pass. Used the built-in image-generation tool, with existing project images as style references; no API key or CLI generation was used. Original generated files remain under the Codex generated-images directory. Accepted files were copied into project assets without post-processing their pixels. Discarded framing attempts are not packaged.

## Accepted assets and references

| Use | Saved asset | Result |
|---|---|---|
| Penny dialogue portrait | `world_assets/src/main/assets/images/npcs/penny_portrait.png` | New black-and-white Sheepadoodle portrait matching the game's clean anime/cel-shaded portrait style. Based on the existing NPC description, not a photograph of the real dog. Replaces the incorrect Orion portrait. |
| Razor-Vine combat sprite | `world_assets/src/main/assets/images/enemies/razor_vine_combat.png` | New red woody whips, emerald saw-toothed foliage, ivory thorns and acid sap; distinct from Stalker-Vine's luminous closed seedpod. Replaces the bat image. |
| Stalker-Vine combat sprite | `world_assets/src/main/assets/images/enemies/stalker_vine_combat.png` | Minimal framing revision preserving the established plant design. Bottom padding now 1.67%, within the existing 2% ±0.6% requirement. |
| Ruin-Guardian | `world_assets/src/main/assets/images/enemies/ruin_guardian_combat.png` | Existing stone/metal ancient sentinel artwork was already suitable. Corrected World 2's reference from Acoustic Bulwark; no new image needed. |
| Sentinel Orb | `world_assets/src/main/assets/images/enemies/sentinel_orb_combat.png` | Existing white/metallic security globe with cyan core already matched the hovering drone. Corrected World 2's reference from Resonance Buoy; no new image needed. |

Only the corresponding NPC/enemy portrait references changed. Stats, rewards, behavior, encounter formations and other enemies' image references are untouched. Existing later-world use of Guardian/Orb family artwork is retained.

All three generated assets are 1254×1254 RGBA PNGs with actual transparent backgrounds. Razor-Vine's accepted bottom padding is 1.75%. No baseline threshold was weakened. All 26 combat sprites pass the validator. [Dimensions, alpha bounds and hashes](evidence/world2_art_2026-10-31/asset-metadata.json).

## Final prompt set

### Penny

Reference: `images/characters/orion_portrait.png`, style only.

> Use case: stylized-concept. Asset type: Starborn RPG NPC dialogue portrait. Image 1 is STYLE REFERENCE ONLY: match its polished hand-painted anime/chibi JRPG illustration, crisp dark contours, simple dimensional cel shading, friendly expressive face and clean transparent cutout. Replace the human subject entirely with Penny, a big sweet shaggy BLACK-AND-WHITE SHEEPADOODLE dog, as described in existing game content. Chest-up portrait, head and fluffy shoulders, face in slight three-quarter view toward viewer, floppy ears, black patches and soft white muzzle, expressive warm brown eyes partly framed by shaggy curls, friendly open mouth and small pink tongue. Large adult dog, affectionate lively expression, no human body, no armor, no magic, no accessories. Keep head fully visible and centered, compact portrait silhouette readable at 64 pixels. Match the reference rendering style rather than photorealistic fur or 3D rendering. Genuine transparent background, no scenery, no lettering, no border, no floor or shadow. Square image with safe clear space around ears and hair.

### Razor-Vine

Initial references: Stalker-Vine and Siren Skimmer, style only.

> Use case: stylized-concept. Asset type: transparent Starborn JRPG full-body combat enemy sprite, square canvas. Both input images are STYLE REFERENCES ONLY, not edit targets. Match the clean hand-painted cel-shaded sci-fi fantasy style, bold crisp contours, faceted shading and readable colorful luminous accents of these game assets. Create one original Razor-Vine: an aggressive smaller ordinary predatory thorn plant. Distinguish from the large closed seedpod Stalker-Vine: a low compact tangle of reddish woody stems, dark emerald narrow saw-toothed leaves, three lean whip-like thorned tendrils rising in asymmetric hooked curves, sharp ivory thorns and subtle acid-green sap at thorn tips. Rooted base, no central glowing pod, no flower face, no eyes, no mouth, no animal, no wings, no mechanical parts. Front three-quarter presentation, entire creature fully visible, strong readable silhouette at small combat size. Genuine transparent background, no scenery, no floor, no shadow, no text or frame. IMPORTANT production framing: square canvas, root tips end exactly 2 percent of canvas height above the bottom edge (about 25 pixels for a 1254-pixel canvas); no alpha specks or glow below the root tips. Top and sides have clear 5 percent margins; subject reaches ground baseline near y=98 percent, no clipping.

Accepted framing follow-up: initial Razor-Vine as edit target; existing Ruin-Guardian as baseline reference.

> Use case: precise-object-edit. Image1 is the Razor-Vine edit target. Image2 is a BASELINE/FRAMING reference only: its feet sit just above the bottom edge, at the correct game sprite baseline. Preserve image1's exact three red thorn vines, emerald leaves, ivory spikes, green sap and hand-painted cel shading. Reframe the plant so its lowest root ends at the SAME relative ground baseline as the feet of image2: near the very bottom of the square, with only a narrow roughly2-percent transparent bottom strip. No deep empty area below it. Plant stays completely visible with no clipped roots. Transparent background, no ground or shadow or text, no new anatomy. Make only the vertical framing match to image2, not its subject or palette.

### Stalker-Vine

Edit target: original project Stalker-Vine.

> Use case: precise-object-edit. Edit target: image 1, the existing Stalker-Vine combat sprite. Change ONLY its framing on the transparent square canvas to align it to the game's baseline: the lowest visible root tip must be at y=98 percent of canvas height, leaving exactly 2 percent fully transparent rows below it (approximately 25 pixels if 1254 pixels high, 20 pixels if 1024 high). Currently the bottom margin is only 1 percent. Slightly reduce/reposition the whole sprite to increase this margin; preserve the original plant identity, same leafy closed glowing lime-green seedpod, cyan veins, all thorned curling tendrils and roots, same pose, same colors, same clean cel-shaded painterly line style and sharpness. Keep the complete silhouette and safe clear side/top margins, no cropping. No new anatomy, no scenery, no floor or shadow, no lettering, no background color. Preserve genuine transparent alpha throughout all background regions. Do not add stray particles or glow below the roots. This is a minimal production framing edit, not a redesign.

## Verification and handoff

- **781 host tests passed**, zero failures/errors/skips, including earned World 1 completion and World 2 progression/pairs. Lint and debug APK assembly pass. [Build output](evidence/world2_art_2026-10-31/host-lint-build.txt), [test counts](evidence/world2_art_2026-10-31/host-summary.json).
- All **26 enemy sprite baselines pass** at the existing threshold. All five relevant files decode from the packaged APK and match the approved sources byte-for-byte. [APK asset evidence](evidence/world2_art_2026-10-31/apk-assets.json).
- World 1 checkpoint hashes refreshed through a successful earned route and verified after the content-reference changes.
- Release **1.3.102 / 186**. APK SHA-256 `88562ed289e94c9f12e0bdb037eb2eac418cdbef2a51ee7d4e15ca87f264bbee`.
- Visual inspection compared accepted artwork against existing portraits and combat sprites. Physical-device acceptance remains separate from style and asset validation. At this build checkpoint no Pixel is detected; the chat handoff will report the installation outcome.
- The commit includes the previously completed non-image second-pass fixes, checkpoint refresh and evidence, alongside these image corrections. Unrelated user files are excluded.
