# Localized lighting demonstrations — November 3, 2026

This records the first pass. The user found it too subtle; see [the motion rework](LIGHTING_MOTION_REWORK_2026-11-03.md) for the current behavior and comparisons.

Three deliberately scoped examples extend the existing shared environmental renderer. They are active in normal exploration on Android and desktop; no separate demo mode is required. No new background art, quests, rewards, sound cues, gameplay state, or progression rules were introduced.

## Rooms

| Room | Source and receiving surfaces | Behavior |
| --- | --- | --- |
| Nova's Bunk (`pit_nova_bunk`) | Painted amber fixtures; wall, sleeping shelf and floor panels | Warm illumination follows `light_on`; softly varies once active. Existing 350ms binding fade handles activation. The existing room darkness system remains authoritative. |
| Overgrown Glade (`sector9_landing_glade`) | Cyan and violet mushrooms already in the illustration; nearby log and ground | Independent slow variations by cluster, rather than a synchronized whole-scene pulse. Charlie's ball and other interactions are unchanged. |
| The Campfire (`source_campfire`) | Existing blue song-flame; front ring, neighboring stone and approach | Bounded, irregular blue reflection. Keeps the illustrated flame's cyan palette rather than introducing an orange fire. Existing resonance remains. |

The World 2 cooking/rest rooms inspected during authoring do not visibly depict a fire; the Campfire example therefore uses the actual illustrated song-fire. This avoids placing a conspicuous light source on an unrelated feature in a room background.

## Presentation

Sources and masks use normalized artwork coordinates. The renderer reuses Android's Crop and desktop's Fit geometry, including scene transitions. Small source glows use Screen blending, with soft reflected pools clipped to authored polygons on the painted surfaces. This is art-directed illumination, not simulated geometry, shadow casting, normal maps, or real-time ray tracing.

New styles: `lamplight`, `bioluminescence`, `firelight`. Existing light styles retain their old rendering. Each demo is excluded from combat. Quality Off removes the effect. Reduced effects, disabled flashes, disabled motion, and accent suppression produce steady illumination. High contrast attenuates these lights to 45%. Existing foreground/modal pause, scene-memory bounds, fades, and transition opacity remain in use.

New lighting adds no particles or bitmap assets. Binding validation bounds localized sources to 1–8, reflected surfaces to 8, and each polygon to 3–12 normalized finite points. Brightness is continuous and bounded; it never switches fully off as a strobe.

## Authoring example

Inside a room's existing `environmental_effects` array:

```json
{
  "id": "warm_fixture",
  "preset": "lighting.bunk_warmth",
  "space": "scene",
  "requires_state": { "light_on": true },
  "in_combat": false,
  "emitters": [{ "x": 0.465, "y": 0.173, "radius": 0.085 }],
  "light_surfaces": [{
    "source": 0,
    "strength": 0.45,
    "points": [
      { "x": 0.23, "y": 0.20 },
      { "x": 0.75, "y": 0.20 },
      { "x": 0.72, "y": 0.27 },
      { "x": 0.24, "y": 0.27 }
    ]
  }]
}
```

`radius` is a fraction of artwork width. A surface's `source` selects the emitter whose brightness it follows. Its polygon clips a soft elliptical pool to a painted plane. These masks are not automatically inferred from an illustration; inspect every room before placing them. Existing four-binding limit still applies.

## Evidence and acceptance

- 24 environment/runtime tests pass, including all 477 room definitions, legacy compatibility, source geometry, state gating, quality/accessibility, mask rejection and bounded independent light curves.
- Two desktop render tests pass: real artwork in desktop Fit and phone Crop, localized pixel changes, untouched desktop side margin, steady flash-disabled lighting, and actionable controls beneath the layer. Existing desktop effects preference persistence also passed in the initial validation run.
- All 783 Android host tests pass, including earned campaign routes. Ten World 1 checkpoints reimported and verified against current content.
- One native API 37 Android test passes across all three rooms, checking bunk state activation and five successful action clicks beneath the decorative renderer. Native captures were inspected.
- Android debug APK/test APK build, Android lint and desktop parity verification (1,342 assets) passed. Android instrumentation was added after the broad lint run; it compiled and ran successfully in the subsequent native build.
- No physical Pixel rendering/performance acceptance or sustained GPU/battery profiling is claimed. Existing installed Pixel build has not been updated by this task. Existing desktop sessions were not interrupted; a restart is needed to load the new build.

Evidence: [captures and logs](../../testing/evidence/localized_lighting_2026-11-03/). Paired captures isolate the new lighting from existing dust/resonance to make its contribution reviewable; native captures likewise isolate the lighting layer and include a test action button rather than the full exploration HUD. Motion previews are six-second, 8fps samples from the production shared renderer. Actual gameplay follows the renderer's normal frame clock; preview looping is not the production timing.

### Motion samples

[Nova's Bunk](../../testing/evidence/localized_lighting_2026-11-03/pit_nova_bunk-motion.webp) · [Overgrown Glade](../../testing/evidence/localized_lighting_2026-11-03/sector9_landing_glade-motion.webp) · [The Campfire](../../testing/evidence/localized_lighting_2026-11-03/source_campfire-motion.webp)

Manual acceptance: switch on the bunk light through normal interaction; leave/return and load saves before/after activation; visit the glade while playing with the ball; rest/cook at the Campfire; test Reduced/Off/high contrast; check light placement at different window sizes and during travel. Judge subtlety and fatigue during actual play before extending this to more rooms.
