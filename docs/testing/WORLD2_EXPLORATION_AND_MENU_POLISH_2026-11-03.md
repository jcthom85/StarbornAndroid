# World 2 exploration and menu polish

This pass implements the five approved followups: interaction visibility and read styling, party overflow and enemy-name readability, tinkering layout, the Hangar shortcut, and Caretaker Echo chronology. Enemy names were audited, and three approved replacements were applied after a separate decision with Jason.

## Interaction rules

Passive inspections remain available, keep their saved read color and become unread when their response or actionable label changes. Unrelated room prose does not reset an unchanged response. Functional controls retain their normal appearance. Existing collection/removal behavior remains in effect.

Six additional World 2 quest interactions now require their optional quest to be active: distress signature beta, distress signature gamma, patrol transceiver, security grid, guard-system bypass and the transmitter case. The transceiver, Tideglass growth and transmitter case additionally wait until their own event prerequisites are satisfied. Text remains in the room description while unavailable; it becomes actionable when useful.

The canopy remains and distress signature alpha remain available before Lost Patrol starts because either can start that quest. Main-story locks, puzzle controls and useful clue inspections retain their visible hints.

Signal reflection at Beach Cliffs is a passive inspection. Its previous message-only event is now presented directly through the normal inspection path, so repeat taps give details and use persistent read styling. No quest task or reward was removed.

## Party selector

Android and desktop share a horizontally scrolling selector with consistent card widths, a partial next card when the viewport permits it and edge fades only on sides with actual overflow. Selecting a companion scrolls the full card into view. Two companions that fit do not show overflow cues. The selector preserves the surrounding platform-specific card content and styling.

## Enemy names

The Android labels can use their full row slot rather than 72 percent of the sprite width. Single-enemy labels have more room; three-enemy rows retain spacing between targets. Sprite sizes and enemy stats are unchanged.

A shared name renderer tries 13, 12 and 11 sp on Android; desktop retains its 11 sp baseline. It never reduces text below 11 sp. When a complete name still cannot fit on one line, it wraps in full without ellipsis. Desktop's fixed status height was changed to a minimum so wrapped names are not cut off.

All **48** current enemy names were measured with Android native graphics under Robolectric API 34 using sans-serif semibold, and separately with the desktop text measurer. The shared renderer was exercised for every name in an 82 dp label at font scales **1.0, 1.3 and 2.0**, checking full text and no visual overflow. Android measurements are reproducible font metrics, not screenshots from the Pixel.

### Approved names and measured limits

Jason approved these replacements after the initial audit. Enemy IDs, stats, encounters and rewards retain their existing values. Phantom's prototype identity remains in the quest description.

For a 320 dp combat grid, a three-enemy row has approximately **82.7 dp** of text per label after gaps and padding:

| Previous name | Approved name | Tier | Android width at 13 sp | Android width at 11 sp |
| --- | --- | --- | ---: | ---: |
| The Administrator | Administrator | Boss | 79 dp | 69 dp |
| Phantom Prototype | Phantom | Elite | 51 dp | 45 dp |
| Distorted Sentinel | Warped Sentinel | Common | 93 dp | 82 dp |

After applying the names, all **785 Android host tests** and the **desktop whole-roster rendering audit** passed again. The fresh debug APK builds successfully, and the ten World 1 playtest checkpoints were refreshed and verified. No installation was performed.

All three approved labels fit that Android budget at the 11 sp readability floor; Administrator and Phantom also fit at 13 sp. These names appear in the existing three-enemy encounters at `spire_landing_pad_roof`, `foundry_conveyor_belt` and `source_memory_fragments` respectively. At a 360 dp grid the per-label budget is 96 dp, and at 412 dp it is about 113 dp. The actual grid width can be smaller than the device viewport.

Desktop font metrics differ, so the deliberately narrow 82 dp stress layout can still wrap names that fit the Android equivalent. Ordinary desktop labels have a wider minimum. Larger accessibility text can require wrapping on either platform; the full-name fallback remains enabled. No names use ellipsis.

Future naming guidance should use measured width in the intended encounter, not a fixed character count. A short word with wide glyphs can consume more space than a longer narrow one. For a strict one-line rule in the smallest three-enemy grid, the measured name must fit its approximately 83 dp text budget at the agreed minimum size. Accessibility scaling still needs a wrapping fallback.

## Tinkering

Schematic information now follows result, effect, materials, availability and craft action. Material rows show owned/required counts; reusable tools are separate. Missing requirements are stated explicitly beside the action. Disabled craft buttons use readable colors.

Android schematic cards expand to show requirements and actions. Desktop keeps its browser and selected detail pane. Both move equipment comparisons and assembly notes into optional sections below the main action. Result summaries no longer put ownership bookkeeping above every materials list. Desktop result art is smaller so the main action stays close to the requirements.

Newly learned schematics show NEW until their details are viewed. The viewed IDs persist in additive protocol field 61; older saves default to an empty viewed set. Viewing a schematic does not craft, consume materials or change the learned recipe list. Equipment effects, recipe ingredients and crafting outcomes are unchanged.

## Hangar passage

Each direction has its own first-use confirmation using the standard room-object popup. Follow the passage confirms; dismissing it leaves the player in place and does not mark it learned. Subsequent uses of that direction travel directly. Confirmation receipts persist in room state and loading an earlier save restores that save's receipts.

Arrival uses the existing room transition and area-entry cue with the destination and passage origin. The quest and conduit requirements still determine whether each route can run. Both directions remain authored event routes, not ordinary cardinal exits.

## Caretaker Echo

Before waking the pod occupant, the Echo describes the archived safeguards without revealing Orion's name. After awakening, its question and reply acknowledge that the pod has opened. Once Orion introduces himself, Nova can ask about him by name; the Echo distinguishes archived calibration from his present memories.

The old claim that a local three-point procedure breaches the Planetary Shield has been removed. The Echo now explains local Source Gate and Astra departure procedures and explicitly limits that guidance to local travel.

## Verification

Final verification:

- **785 Android host tests**, zero failures or skips, including the native Android font audit and earned campaign routes.
- **29 targeted desktop tests**, zero failures or skips, including all 48 names at three font scales, menu/crafting behavior and the copied Pixel Tideglass save.
- Fresh Android debug APK builds successfully.
- World 1 checkpoint manifest refreshed from earned production test output; all **10 states** match current content.
- World 2 walkthrough passes its **245 directional connections** and generated reference checks for **93 rooms and ten quests**.
- `git diff --check` passes.

Checks cover the Lost Patrol starter exception, dormant quest tasks, saved passive inspection state, first-use confirmations in both directions and save/load, saved schematic view state without consumption, overflow hints and selected-card visibility, Echo before/after the reveal, readable missing-material details, and the complete enemy-name roster.

Evidence is stored in `evidence/world2_exploration_polish_2026-11-03/`. The Android font audit and desktop width audit are separate CSV files. The schematic screenshot was inspected and refined to bring materials and the main action into view.

The World 2 walkthrough and its generated references were updated locally. This pass does not publish the guide, commit, push or install the new build on the Pixel.

## Focused device checks

- At a quest starter, confirm it is still actionable before acceptance. At a dormant scan or collection target, confirm plain text becomes actionable when its quest and requirements permit it.
- Inspect flavor text, leave and reload: read styling persists. A changed response becomes unread. Collected objects still disappear.
- With two companions, confirm there are no false overflow hints. With four, select and scroll through everyone at normal and larger system text sizes.
- Open a new schematic, inspect materials and tools, verify its NEW state stays cleared after save/load, and check that a disabled craft action clearly explains what is missing.
- Test both passage directions: cancel, confirm, revisit, save/load and load an earlier save. Check destination cues and transition timing.
- Speak with the Echo before awakening, after awakening and after Orion's introduction.
- Check enemy labels on the Pixel, particularly three-enemy rows, bosses with escorts and larger text settings. Full names must remain visible; the three approved names above now fit the narrow Android budget at the stated sizes.

Roaming enemies and first-attempt puzzle rewards remain separate discussions.
