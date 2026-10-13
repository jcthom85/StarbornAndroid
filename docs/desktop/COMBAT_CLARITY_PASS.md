# Combat clarity on Android and desktop

This pass updates combat previews, effect labels and inspection without changing
combat calculations or the existing snack balance trials.

## Ability information

- Both platforms use shared snack wording for active effects, target scope,
  action cost, non-consumption and remaining cooldown. Equipped passive bonuses
  are distinguished from the activation where relevant.
- Desktop shows the preview directly beside the snack command and retains the
  full tooltip. Android provides an expandable, scrollable Snack details control
  before activation, including while the ability is cooling down.
- Comet Gummies explains that its crit bonus is for weapon attacks and that
  abilities follow their own crit rules. Pulse Citrus explains the setup cost
  and longer-fight use. Trail Mix distinguishes equipped HP from its active
  defense buff.
- Skill details state that using an ability spends an action and that cooldowns
  count the acting character's turns. Both platforms share the Overcharge-aware
  cooldown label, including the one-turn refund at two or three Momentum stacks.
- Corrected the Android Overcharge hint from an ambiguous “up to 3x” to three
  stacks. Corrected desktop Attack and Snack tooltips that described readiness
  filling or consumed instant-recovery food rather than the actual mechanics.

## Active effects

Android buff badges show signed values, percentage units and remaining turns
together. Tapping the effect rail or badges opens a scrollable list with full
labels and every effect, including overflow entries. Semantic descriptions carry
the full values and owner-turn meaning. Expired-effect dialogs clear their open
state so a later buff cannot unexpectedly reopen the panel.

Desktop buff badges use compact stat labels and reserve separate space for the
timer so long names cannot truncate it. The tooltip gives the full stat value
and character-turn meaning. The command actor's visible effects use compact
labels to preserve room for commands; the scrollable command pane keeps Retreat
reachable after a snack activation.

## Combat log repair

Regression testing exposed a concurrent modification while the live feed
iterated the mutable combat history. Both log views now receive the immutable
Compose snapshot list. The live feed scrolls using its actual last-12-entry
window and follows new entries even after the retained history reaches its cap.
A rendered regression test covers that full-history boundary.

## Verification

Tests cover real catalog previews, signed percentage buffs, duration/cooldown
semantics, activation and non-consumption, Android viewmodel behavior,
Overcharge labels, desktop preview/activation, reachable controls, log rollover,
and existing command-dialog/victory/defeat flows.

Final verification passed 26 Android tests, seven desktop checks and 28 shared
runtime tests. The Android debug APK was built successfully. The refreshed
Windows portable launcher passed from the temporary directory, including music
decoding, and all 1,328 packaged assets matched the source bytes.

Rendered desktop artifacts:

- `desktopApp/build/reports/combat-clarity/snack-preview.png`
- `desktopApp/build/reports/combat-clarity/snack-active-buffs.png`

Android has compilation and unit/integration coverage for this pass; touch
layout and feel still need device review. Desktop automated rendering and input
checks do not replace a full hands-on playtest or establish audible playback.
