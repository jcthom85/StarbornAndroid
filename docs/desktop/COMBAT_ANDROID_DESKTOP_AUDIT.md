# Android / desktop combat audit

Follow-up: party sprite sizes and normal enemy formation rules were corrected after this audit, using Android's 80/64/48 dp party sizing and enemy card width/portrait ratio. The other findings remain outstanding.

Scope: current Android CombatScreen, RosterViews, CommandPalette, EnemyFormationViewport and VictorySpoilsDialog; desktop CombatScreen, Battlefield and CombatFeedback; existing mobile screenshot and latest desktop offscreen renders. This is source/render evidence, not a fresh native Android or Windows playthrough.

## Confirmed gaps

1. Enemy formation: Android uses one row for 1-3 enemies, 2+2 for four, 2+3 for five. Desktop uses a fixed two-column grid and a 54/46 height split. Three enemies therefore have a different composition; extra rows can scroll or shrink. Android measures and fits the whole formation.
2. Composite bosses: Android reads composite groups, part positions/scales/offsets and renders CompositeEnemyRoster. Desktop renders each combatant as an independent grid sprite. A single Iron Warden screenshot does not validate multipart bosses.
3. Sprite life: Android changes party art for attacking, misses, defeat and victory, and includes idle breathing/bobbing and low-HP motion. Desktop keeps the normal combat image, dims defeated characters and has generic horizontal recoil/lunges. Horizontal lunge direction also no longer follows the above/below formation.
4. Sprite grounding: Android has richer shadows, selection glows and shield fields. Desktop selection is a simple border and lacks persistent shield visuals.
5. Combat information: compact desktop figure summary removed intent/charge/buffs/status/stability text. intent is still passed to DesktopBattleFigure but never read. Focused enemy info and selected actor info recover only part of this information. Party momentum gauge and visible nonselected status cues are missing; zero stability is not explicitly labelled broken.
6. Effects: both consume shared events, but desktop mostly uses generic lines/rings/text. Telegraph target cues are not shown around affected party members. Distinct shield-break, knockout, status, support and weapon effects do not match Android. Health recoil also runs on initial HP and healing, not just damage.
7. Atmosphere: Android applies 3dp background blur, dark tint, weather, vignette, shake and combat transitions. Desktop uses portrait/reflection backing and flat dimming, without equivalent atmosphere/entry/exit transitions.
8. Commands: desktop correctly moved command routing/skills/items into the right area. Android command-specific iconography, combat font, themed border/base, animated appearance, momentum gauge and guide indicators are not fully adapted. Applying only primary accent does not reproduce the palette.
9. Timed actions: desktop is a darkened centered generic box with a linear countdown. Android uses its dedicated combat prompt presentation. Needs visual and interaction parity review with actual timed actions.
10. Results: Android separates Spoils Recovered and Level Up stages with portraits/cards. Desktop places rewards and level-up summaries in one AlertDialog; no equivalent victory emotes or exit transition.
11. Compact layout: desktop commands can overlap the center battlefield. The buttons can close the overlay, but target visibility and timed prompt layering need small-window interaction checks.

## What is already shared

Combat simulation, authored enemies/items/skills, ATB flow, action dispatch, reward generation, tutorial state, audio event generation and availability helpers use the shared controller/engine. UI parity remains incomplete; sharing this engine does not validate every desktop UI path.

## Next implementation order

A. Restore information and formations: intent/target indicators, status/charge/broken state, momentum, Android normal formation rules and composite layouts. Derive layout/presentation data in shared code where practical.
B. Restore sprite behaviour and atmosphere: original emotes, idle/low-HP/shield states, grounded shadows, formation-aware attacks, distinct events, vignette/weather/transitions.
C. Adapt original command, tutorial, timed-action and victory components to the right area/central overlays with keyboard and accessibility support.
D. Exercise 1/3/5 enemies, four party members, multipart bosses, shield break/status/charge, ally/all-target actions, timing, tutorial and victory/defeat/retreat, then repeat at smaller window sizes.

No game code was changed during this audit.


## October 8 follow-up: effects and exploration enemies

Implemented desktop presentation for heal crosses, status particles, support rays, shield-break shards, targeted telegraph warnings, and knockout particles. Existing hit effects still honor the shared `showAttackFx` flag. Shared audio events remain routed to the audio driver. Added persistent shield fields, momentum pips, intent/status/buff/charge labels, Android character emote assets, idle motion, damage-only recoil, broken-enemy motion, and enemy knockout fades. Weather and vignette now occupy the portrait battlefield. Formation sizing reserves room for the additional labels.

Exploration now draws the shared `visualEnemyParties` and original enemy icon/composite data in the portrait stage, with Android tier sizing/overlap, shadows, entry/exit motion, and click-to-engage. Text enemy controls are suppressed when sprite parties are available. Dark rooms keep their existing enemy visibility rule.

Validation: Kotlin compilation and offscreen combat renders (one, two, three, five enemies, boss and narrow viewport), plus a three-enemy exploration stage fixture. These are presentation checks, not a full campaign playthrough.

Remaining Android fidelity gaps: weapon-specific attack trails/projectiles and per-status continuous visual treatments are still simplified. Multipart combat bosses still use the ordinary formation. Victory spoils and level-up screens still need the Android staged presentation. Thus event handling coverage does not imply complete Android visual parity.
