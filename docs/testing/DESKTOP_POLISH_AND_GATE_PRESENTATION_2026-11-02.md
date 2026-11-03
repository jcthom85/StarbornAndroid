# Desktop polish and Temple Gate presentation — November 2, 2026

## Implemented

- Cooking results use the existing exploration modal, with one explicit dismissal. Removed duplicated snackbar/inline result feedback. Meal replacement confirmation remains intact.
- Android and desktop read release identity from Gradle properties: 1.3.105, build 189. Desktop packaging uses the same version; its title screen includes the source revision and dirty status.
- Both platforms use one Temple Gate animation and lifecycle. Cyan/gold resonance originates at the painted lock, the sealed artwork dissolves into a matching open entrance, and the open background persists. Moving east into Sanctuary remains a separate normal transition.
- The effect fills the scene rather than a padded narrative panel. Artwork coordinates use Android's centered Crop or desktop's centered Fit. Saved already-open gates do not replay; reduced motion skips the animation; disabled flashes reduces its strength. Narration waits until the reveal finishes.
- The open room variant supports both the quest completion milestone and the existing temple_open state, including older saves.

## Verification

Focused desktop Compose tests cover compact/wide cooking results after scrolling, inventory reward, replacement confirmation, generated title identity, reveal completion/cancellation, notice blocking, saved-open and reduced-motion behavior, artwork resolution and coordinate geometry. Existing room-entry, exploration-panel and glade presentation tests ran alongside them.

The evidence directory `evidence/desktop_polish_gate_2026-11-02/` contains rendered closed/charging/opening/open frames for desktop Fit and phone Crop, subdued effects, and both cooking sizes. Phone images are desktop Compose renders using Android's scaling policy, not physical-device captures. Android compilation/unit tests/lint and desktop asset parity are recorded separately below. Physical Android and live desktop end-to-end acceptance remain manual checks.

Manual checks: unlock through the ordinary quest, verify the lock emits the effect, narration follows it, the entrance remains open, moving east enters the hall, returning keeps it open, and loading an open-gate save does not replay. Check these with reduced motion and disabled flashes as well.

## Image provenance

Mode: built-in image generation, precise edit of the existing closed gate image. No external API key. Reference: `world_assets/src/main/assets/images/rooms/world_2/temple_gate.webp`. Selected opaque output: `world_assets/src/main/assets/images/rooms/world_2/temple_gate_open.png` (944 × 1665). Existing artwork remains the closed state.

Prompt:

> Use case: precise-object-edit. Edit target: the supplied Starborn temple gate room background. Create the OPEN state of this EXACT same background, for a before/after game transition. Preserve the same portrait composition, camera, perspective, cel-shaded painted illustration, cliff geometry, arch frame, gold crest and green gem above the arch, cyan-lit stone pedestals, hanging vines, purple plants, waterfall on right, foreground stone walkway, steps and circular floor medallion. Do not shift the camera or change the surroundings. Change ONLY the two closed central engraved door leaves INSIDE the arch: they have withdrawn sideways into the cliff, revealing a clearly OPEN arched doorway with a dark teal stone passage extending inward, subtle cyan reflected light and a cool dry atmosphere. The open portal is framed by the original stone arch, occupying exactly the former door opening. Keep thin cyan cymatic traces around the arch; the big circular central lock plate no longer spans the opening because it withdrew with the door leaves. No new characters, no text, no UI, no blinding glow, no fog obscuring the opening, no new scene beyond the threshold. This is still the exterior entrance, not the foyer interior. Same frame alignment and proportions as reference; full opaque portrait room background.

### Final automated results

- 24 focused desktop tests passed (polish/gate, cooking/fishing flow, room entry, exploration panels, glade presentation).
- All 783 Android host unit tests passed, including earned campaign routes. Android compilation and `lintDebug` succeeded.
- Desktop asset parity verified 1,342 assets, including the new open gate background.
- Reimported and verified all 10 earned World 1 checkpoints against current content.
- No physical Android device was connected during this pass. Device rendering/audio and actual quest-driven presentation still need manual acceptance; the phone-scaled render is not device certification.
