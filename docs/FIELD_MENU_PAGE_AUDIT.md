# Field menu page audit

Code review of the Android exploration menu, October 1, 2026. Decisions below are recommendations,
not completed changes. Only Item Details was updated during this audit. No tests or device review
were performed; visual judgments are inferred from layout code and the earlier Party Details screenshot.
The classic-layout switch remains the fallback. Keep its presentation separate from modern refinements.

## Page inventory and decisions

| Page or subpage | Decision | Keep | Next change |
|---|---|---|---|
| Shared shell/navigation | Refine | Fixed navigation, room tint, gold selection, Back | Make tutorial guidance layout-aware; give tutorial banner reserved space; review font scaling and per-page scroll restoration |
| Party overview | Refine | Selected member, portraits, equipment, Attributes/Skills | Show useful progression and skill availability; reduce repeated character information; make selection discoverable |
| Party / Attributes | Substantial refinement | Primary/combat stats and wrapping skill chips | Replace hardcoded HP 100% and XP 25% fills with real values; remove fallback 500/500; use one page scroll instead of inner 580dp scroll; rework crowded stat label/value rows |
| Party / Skills | Substantial refinement | Branch selection, AP, selected node detail, unlock action | Inner scroll and 700dp cap compete with shell; fixed 6×3 grid and large padding squeeze nodes; keep AP visible and clarify prerequisites/locked/unlocked state without nested frames |
| Party / Weapon picker | Refine | Compatible options, equipped badge, comparisons | Separate inspection from immediate equip; compact descriptions and comparison hierarchy; review long names and one-handed close access |
| Party / Armor picker | Refine | Compatibility, equipped marker, stat comparisons | Same interaction and density treatment as weapon picker |
| Party / Accessory picker | Refine | Slot filtering, current selection, Unequip | Make current equipment name obvious; improve long effect text currently ellipsized; unify with other gear selectors |
| Party / Mod picker | Refine | Milestone locks and base-slot dependency | Show destination slot/base gear clearly; explain locked requirements and compare with equipped mod; same selector interaction as accessories |
| Items / Supplies | Keep structure, refine | Icon filters, quantities, direct use, details | Reduce small badges and tiny info targets; distinguish inspection from item use; review large inventories and category rules |
| Items / Key Items | Keep | Dedicated category and details access | Clearer empty state; emphasize purpose/quest relevance when actual item data supplies it |
| Items / Tutorial Gear | Overhaul integration | Equipment actions and named character sections | Modern instructions should teach Party equipment; avoid view disappearing immediately after cutter equip; do not tie instructional eligibility only to Items entry |
| Items / Item Details | Updated, review on device | Actual description, quantity, effects | Generic flavor box and duplicate Type removed; wrapping badges, bounded row widths, quieter modern card, no disabled Use button on nonusable items. Next review: live quantity after use and actual usability vs merely having an effect |
| Tinker / Workbench | Keep mechanic, substantial UI refinement | Base/component sockets, preview, synthesis, highlighted tutorial targets | Preserve experimentation; reduce decoration and small labels; reserve tutorial space; coordinate tray/page scrolling; use consistent action terminology |
| Tinker / Schematics | Refine | Discovered/locked recipes, load/craft actions | Replace two capped lists with clear page sections; distinguish loading a recipe from crafting; improve requirements readability |
| Tinker / Locked requirements preview | Refine | Optional disclosure | Larger control and readable requirements; decide how much undiscovered recipe identity to reveal |
| Tinker / Scrap | Substantial refinement | Salvage action and eligible inventory | Replace generic salvage sentence with actual output if data supports it; increase 28dp action height; add review/confirmation appropriate to the selected item before consuming it |
| Journal / Active | Keep | Tracked quest first, selection, progress | Stronger tracked/current objective hierarchy; consistent selected styling; remove excess frames |
| Journal / Completed | Keep | Separate completed archive | Compact entries and meaningful empty state; verify late-game list size |
| Journal / Quest Details | Refine | Description, objectives, tracking toggle | Embedded layout inherits a 700dp cap; unify title/action placement and optional objective presentation; make next objective easiest to scan |
| Map | Refine | Full map, gestures, legend and dark-room behavior | Keep the pan/zoom map directly on this tab; avoid a second full-map detail page |
| Map / Legend | Keep, refine | Symbol explanations | Unify embedded spacing, headings and card styling; ensure legend remains legible at larger fonts |
| Settings / Layout | Keep | Persisted classic/modern switch | Keep easily discoverable; explain only player-relevant behavior |
| Settings / Save Data | Keep, refine | Quick save, manual save/load, return to title | Reduce duplication with header quick save; ensure feedback is visible; clarify quick vs manual slots |
| Settings / Save slots | Keep behavior, refine | Slot metadata and existing overwrite/delete confirmations | Match modern palette; review long save metadata and selected/empty slot hierarchy |
| Settings / Load slots | Keep behavior, refine | Slot selection and metadata | Same slot styling; review how current unsaved progress is handled before loading |
| Settings / Overwrite or delete confirmation | Keep | Explicit confirmation and cancel | Clear slot identity, responsive text, consistent action hierarchy |
| Settings / Audio & Display | Keep | Audio controls and comfort options | Improve grouping and spacing without adding another navigation layer |
| Settings / Accessibility & Comfort | Keep, audit coverage | Existing settings | Confirm modern controls actually honor high contrast/large touch preferences; current shell does not consume those flags |
| Settings / Testing & Diagnostics | Move out of normal player flow | Developer utility | Exploration passes the tutorial-reset callback unconditionally; isolate debug actions from production Settings |

## Order of work

### Completed first pass

Party Attributes now receives calculated HP and XP progress, omits unknown HP bars, and no longer
fabricates a 500/500 fallback. Modern embedded details use the shell scroll, with quieter borders
and weighted stat labels. The skill chips retain the wrapping fix.

Modern bag/gear prompts trigger from Party as well as Items and explicitly teach Party equipment.
Classic prompts retain their existing route. The modern gear beacon points to Party. Temporary
Inventory Gear access is retained while that Items page is open, so equipping the cutter does not
replace the visible equipment page. Modern tinkering guidance occupies reserved space below the
header; classic positioning remains unchanged. Tinker and Save script wording matches the controls.

Android and desktop Kotlin compilation passed. No tests or device review were run.

### Completed second pass

Modern Skills uses readable rows instead of the cramped fixed grid. Selecting a row opens its
details and requirements inline. The shell header keeps available AP visible, and modern embedded
skills use the page scroll without the previous 700dp cap. Classic skills retain the grid.

Modern weapon, armor, accessory and mod selection now share one picker. Tapping an option inspects
it; a separate fixed Equip button applies it. Current equipment remains identified, descriptions
and attributes are readable, and comparisons appear for inspected replacements. Accessories/mods
retain Unequip. Existing compatibility filtering, milestone locks and equipment actions are reused.
Classic pickers retain immediate selection. Device review remains required.

### Completed third pass

Modern Items rows open inspection instead of immediately using the item; info targets are 48dp.
Open Item Details resolves the current inventory entry, displays zero if the item is gone, and
does not offer Use when quantity is zero. Existing target selection and item-use rules are retained.

Modern Journal highlights the tracked quest, previews its first unfinished objective when runtime
objective state is available, and keeps completed archive entries compact. Quest Details uses the
shell scroll without the embedded 700dp cap and has quieter card styling. Classic list presentation
and standalone quest dialogs retain their existing layout. Compilation passed; device review pending.

1. Tutorial integration and Party Attributes correctness.
2. Party Skills, then all four equipment selectors as one consistent family.
3. Items list and Item Details device review.
4. Journal Active, Completed and Quest Details.
5. Map entry, Full Map and Legend.
6. Tinker Workbench/tutorial, Schematics/requirements and Scrap.
7. Settings, save/load/confirmation dialogs, accessibility coverage and debug separation.

Keep each pass narrow: agree on the page's purpose, implement, compile, and review screenshots
at normal and larger font sizes before progressing. Gameplay state transitions and destructive
actions need explicit verification when that testing work is requested. This document does not
claim those flows are tested.

## Evidence anchors

### Completed Settings/save pass

Save descriptions distinguish manual slots from quick checkpoints. Modern Save & Return waits
for quick save success before leaving; failure keeps the player in game. The modern shell displays
status feedback. Exploration load dialogs confirm replacement of unsaved progress; existing
overwrite/delete confirmations remain. Slot dialogs have quieter modern styling and larger actions.
Tutorial reset is available only in debug builds.

Modern high contrast removes room tint, strengthens shell separators and selected control outlines.
Large targets expands shell buttons/navigation and selected item, equipment, skill, settings and
crafting controls. This is explicit coverage of those controls, not a claim that every legacy
widget or system-font configuration has been validated. Normal/larger-font screenshots, TalkBack,
gesture behavior and save failure paths still need device review. Compilation passed; no tests run.

### Completed Tinker pass

Modern Workbench trays and Schematics/Scrap lists use the page's scroll instead of nested capped
lists. Workbench tutorial highlighting and synthesis mechanics are reused. Small explanatory text
and actions are larger in the modern layout. Discovered recipes show available/required quantities
and separate Load Recipe from Craft. Unknown requirements remain an optional disclosure.

Scrap displays recipe-derived returned materials and confirms consumption of one item before
calling the existing salvage action. No crafting recipes, material quantities or unlock rules changed.
Classic list/grid layout is retained. Android and desktop Kotlin compilation passed; device review
is pending and no tests were run.

### Completed Map pass

The Map tab now hosts the interactive full map directly, with pan, pinch/zoom controls, and Recenter; the redundant Open Full Map detail page was removed. The tab shows visited/discovered counts in the modern layout, while the fallback minimap opens the Legend. The Map tab is the single map entry in the field menu. Embedded Legend has reduced padding and quieter borders. Kotlin compilation passed; touch gesture behavior and narrow-screen layout still require device review. No tests were run.

- `feature/exploration/ui/menu/ModernFieldMenu.kt`: shared shell and navigation.
- `feature/exploration/ui/tabs/InventoryTabContent.kt`: Items, Item Details, equipment selectors.
- `feature/exploration/ui/components/PartyMemberDetailsDialog.kt`: constant HP/XP progress and inner scroll.
- `feature/exploration/ui/components/SkillTreeOverlay.kt`: fixed grid, branch/node details and inner scroll.
- `feature/exploration/ui/tabs/TinkerTabContent.kt`: tutorial banner, tray, catalog and scrap actions.
- `feature/exploration/ui/tabs/MapTabContent.kt`: available-map preview lacks an entry callback.
- `feature/exploration/ui/ExplorationScreen.kt`: detail routing, embedded maps and unconditional debug callback.
- `ui/components/SaveLoadDialog.kt`: existing overwrite/delete confirmation.
- `assets/tutorial_scripts.json` and `ExplorationViewModel.kt`: old instruction labels and Items-only tutorial trigger.


### Final focused cleanup

Manual saving now awaits the repository result, disables repeated actions while saving, and keeps
its dialog open with an error on failure. Modern menu feedback expires after six seconds and is
announced as a polite accessibility live region. Canceling item target selection preserves Item
Details. Effect details show cooldown, use limits and individual buff durations from item data;
list badges no longer assert a fixed five-turn cooldown.

The header moves its action buttons onto a separate row on narrow screens or larger font settings.
Selection semantics were added to party, navigation, gear and skill controls. Gear selection restores
item icons and shares numeric/stat formatting with comparisons. Skills are grouped by their actual
progression rows. Health labels wrap, and Party page buttons respect larger touch targets.
Scrap preview recipe matching now uses the same item-ID rule as the action.

Android and desktop Kotlin compilation passed. No tests were added or run. Device review remains
necessary for font scaling, touch navigation, saves and item target cancellation. The classic layout
remains available through Settings; this work has not been committed or released.
