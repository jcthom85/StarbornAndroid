# Fishing, cooking, and tinkering integration

## Product rules

Fishing is optional gathering, cooking is combat preparation, and tinkering is permanent equipment progression. Share resources and information without making players complete all three activities before progressing.

## Implementation order

1. Secure each successful fishing reward immediately; returning or fishing again cannot lose or duplicate it. Show recipe uses with the catch. Fix perfect-catch evaluation and zone-specific lure advantages.
2. Add modest, biome-specific salvage weights using existing ordinary materials. Register the earned Bioluminescent Lure. Add a craftable salvage lure with explicit attraction. Preserve every existing quest/material source; no mandatory fishing gates.
3. Make meals activate on consumption, support every authored food stat, and persist their effects. Select a recruited companion to serve meals; this preference complements food effects when eating rather than granting a free buff for cooking. One active meal replaces the previous meal, without stacking. Repair Tideglass Delight.
4. Make bench slots select ingredient types; recipe quantities determine the actual amounts consumed. Preserve loaded recipes across refreshes and repair invalid recipe bases. Label unlearned, experimentally craftable recipes as discoveries rather than locked.
5. Show cooking results and ingredient uses in tinkering, including shared-resource notices. Verify save compatibility, repeated catches, food effects, all authored recipe requirements, and unchanged story gating. Build a debug APK.

## Balance boundaries

Mostly fish, occasional useful salvage; rare progression reserves remain exploration rewards. Rods improve handling; lures specialize catch selection. Food remains primarily a combat benefit. Do not add durability, bait upkeep, or nested crafting requirements.

## Implementation and verification — 2026-09-14

Implemented all five steps. Serving companion is a saved preference applied when eating, not metadata attached to individual inventory portions. Cooking does not grant an uneaten meal buff. Existing quest/event definitions and material sources remain unchanged.

Validation: 118 selected unit/regression tests passed across provision integration, fishing, crafting, persistence, asset integrity, exploration, combat, and navigation. Integration coverage includes all 27 tinkering recipes, all 15 meals, repeated catches, no duplicate reward on return, meal expiration/replacement, recruited-companion restrictions, salvage weights, and lure attraction. Navigation audit `--check` passed.

Debug APK built successfully at `app/build/outputs/apk/debug/app-debug.apk`; packaged fishing, tinkering, and item assets match their source files. Device playtesting of interface feel and long-session economy balance remains outstanding.
