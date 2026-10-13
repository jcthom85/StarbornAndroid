> Historical pass: the approved follow-up changes mod comparisons, fishing
> rewards and Trail Mix balance. See [Systems refinement](SYSTEMS_REFINEMENT_PASS.md)
> for current behavior and validation.

# Fishing and crafting usability pass

## Changes

- Desktop fishing now receives the session and crafting services, enabling
  persisted tutorials, tackle preferences, journal records, collection rewards
  and recipe-use information, matching mobile's service wiring.
- Desktop fishing pauses waiting, hook and fight timers when the window loses
  focus or the journal is open. Pausing releases the reel. Space auto-repeat
  cannot advance several phases while the key remains held.
- Fishing uses the authored cast, splash, bite and catch audio events. The first
  practice bite explains that there is no deadline and hides the countdown.
- Desktop catch results scroll and show authored names, flavor, uses, clean
  records and collection rewards. Continue acknowledges an already secured catch.
  Missed hooks no longer claim the line snapped. The journal identifies clean
  catches. Reduced flashes disables sonar pulses and the flashing strike border.
- Fishing header controls wrap into their own row; the cast label lists the
  implemented Space shortcut.
- Desktop and mobile cooking offer Cook and eat one portion for meals. The
  shared item-use controller checks active-meal replacement before cooking
  spends ingredients, applies the usual party/chef rules, and consumes one
  portion. Extra portions remain available. If eating fails after successful
  cooking, the prepared food stays in inventory with an explanatory message.
- Cooking's sound event is valid on both platforms. Desktop passes through
  high-contrast and large-target settings.
- Desktop, mobile station and legacy mobile field-kit scrap entry points require
  confirmation showing one item and its salvage yield. The modern mobile field
  kit already had confirmation. Execution still rechecks protected equipment,
  ownership and capacity in the shared controller.
- Tinkering completion uses the authored audio binding on both platforms.
  Desktop scrap also plays completion feedback. Compact desktop panes now stack
  selection above details; larger windows retain side-by-side panes. Salvage
  yields stay visible above the Scrap button while descriptions scroll.

## Verification

- New shared cooking tests cover replacement refusal before ingredient spending,
  confirmed batch cooking with one portion consumed, and snack refusal.
- New desktop interaction tests cover scrap cancel/confirm, cooking replacement
  cancel/confirm, and fishing's practice hook, focus pauses during waiting and
  fighting, successful retrieval, secured result, and duplicate-free Continue.
- Fresh rendered previews are written to
  `desktopApp/build/reports/system-previews/` by the desktop interaction tests.
- The full desktop regression run passed 123 tests. After the final session and
  crafting-service wiring fix, all 7 affected desktop tests and 49 targeted
  Android unit tests passed with zero failures or skips; Android and desktop
  compilation succeeded.
- Rebuilt the Windows portable app with packaged JDK 21.0.12.1. Asset parity
  verified all 1,328 assets. The final launcher passed its smoke check from the
  Windows temp directory, loading 477 rooms and 4 characters and decoding title
  music. Audible playback and touch/motion feel still require real-device QA.
- Reviewed the final cooking, compact tinkering and fishing-result renders.

## Remaining hands-on checks

Play fishing with touch/motion and physical keyboard/mouse, listen to sound on
real devices, and evaluate station reward pacing through campaign progression.
This pass does not change authored fishing difficulty or recipe balance.

## Follow-up presentation and comparison pass

- Wide desktop cooking separates the recipe browser from selected-recipe
  details. Compact windows retain the scrolling recipe cards. Selected recipes
  stay stable when ingredient quantities change, and batch quantities survive
  switching between wide and compact layouts.
- Meals show gains and losses against the active meal, including bonuses lost
  on replacement and the three-battle duration. Comparisons use the serving
  companion's actual perk. Completion snackbars no longer delay the next cook.
- Mod previews compare each party member's current mod, including negative
  stat changes, empty slots and changes to the mod's attack element. Shared
  preview data feeds desktop, modern mobile, mobile station and legacy field-kit
  presentations. Accuracy and critical bonuses use percentage labels.
- Fishing rod cards explain faster reeling and tension resistance rather than
  raw power/stability values. Desktop lure cards identify favored catches,
  local matches and rare-catch weighting instead of implying an absolute catch
  probability.
- Full desktop regression coverage passed 125 tests. After final preview-data
  and resizing refinements, all 8 affected desktop checks and 69 Android tests
  passed with zero failures or skips. The Android selection includes provision
  integration, campaign catalog/progression, economy and six-world runtime
  progression with save/reload checkpoints.
- Final rendered checks include compact Workbench, Schematics and Scrap pages,
  cooking at wide and compact sizes, and preservation of batch selection across
  resizing. The refreshed portable launcher passed from the Windows temporary
  directory; all 1,328 assets matched and title music decoded successfully.

Campaign checks are automated structural coverage. The six-world runner can
seed room positions, trigger production actions reflectively and synthesize
battle victories; it mocks fishing. Recipe integration separately exercises
the real fishing and crafting rules. These checks establish working progression
and material rules, but do not establish enjoyable pacing, physical touch/motion
responsiveness, or audible playback. No reward or difficulty rebalance was made.
