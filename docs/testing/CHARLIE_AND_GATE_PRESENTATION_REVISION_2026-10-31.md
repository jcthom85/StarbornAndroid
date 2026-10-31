# Charlie and Temple Gate presentation revision

October 31, 2026. Responds to device feedback that Charlie's custom inline text/choices felt unlike the game, the gate reveal was too weak, and both animations stopped at content margins.

## Charlie

The ball remains an unseen-playmate scene prop. It no longer renders a caption, reaction paragraph or choice buttons in its own custom panel. Tapping it opens the existing **RoomObjectOfferDialog** / desktop room-object modal. Confirming starts the return animation. The response uses the standard **InspectionOverlay** narration; any item reward waits until that popup is dismissed. Back does not advance trust. No Charlie portrait or NPC conversation panel was added.

A pending-play token connects the standard confirmation to the animation. Only the completed pending animation can grant progression; disposal/cancellation clears it. Quest/item notices defer while the ball is playing. Existing trust milestones, old-save compatibility, XP and the single comfort stone remain intact.

The ball lane now spans the full scene width on Android and desktop. It sits outside the Android room-content container, with no horizontal padding, maximum-width card or parent content clipping. Its trajectory starts beyond the screen edge and uses that full width. The ball itself retains its round hit target.

## Temple Gate

The old plain rectangular-slab effect is replaced by a 2.4-second procedural resonance reveal: cymatic arcs and glyph marks illuminate, a cyan seam opens with the stone, and light rings and particles expand across the scene. It retains the existing Chime gate sound and physical eastward travel.

The reveal moved from the inset/clipped room-content box to the full-screen scene layer. Narration, quest details and level-up presentation defer appropriately while it plays. Reduced-motion settings skip movement; flash-disabled settings subdue the light and omit particles. Existing open-gate saves do not replay the closed-to-open reveal.

## Verification

- **45 hosted tests passed**, including Charlie's standard confirmation/inspection flow, narration-before-reward ordering, old trust states, repeat rewards/save reload, World 2 main/completionist routes, notice policy, discoverability and asset data integrity.
- **Three desktop Compose tests passed:** ball tap delegates inspection without custom text/buttons; the prop uses the full scene lane; confirmed animation completes before progression; gate renderer measures the full viewport. Screenshots were inspected.
- Android debug assembly and desktop compilation passed. Diff whitespace checks passed.
- Screenshots: [ball scene prop](evidence/scene_revision_2026-10-31/puffball.png), [resonance gate effect](evidence/scene_revision_2026-10-31/temple-reveal.png). These are isolated component renders, not full-device captures.

The new APK is built at `app/build/outputs/apk/debug/app-debug.apk`. No Pixel is connected at handoff; this revised build has not been installed. Device acceptance should check scene edges, the standard popup sequence, reward ordering, magical reveal/audio timing and accessibility settings.

## Separate read-only action-word review

[World 2 action-word report](WORLD2_ACTION_WORD_REVIEW_2026-10-31.md) inventories all 93 rooms and 164 authored actions. No room/action content was changed for that review. It identifies remaining command-style prose and placeholder feedback for discussion rather than silently implementing another wording pass.
