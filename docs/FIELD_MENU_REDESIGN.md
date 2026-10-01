# Field menu redesign

## Restore the classic menu

Open the exploration menu, tap Settings, and turn off **Field menu layout → Modern layout**.
The change is immediate and persists across app launches. Turn it on again to try the new layout.
This preference is separate from game saves; switching layouts does not change progression or inventory.

The pre-redesign menu source is available in commit `1ff842fc` (version 1.3.80, code 164).
The classic implementation remains in the app. It retains its original navigation and presentation,
with the new layout switch and a functional Skills button on party cards.
For a source rollback, revert only the menu implementation changes; do not reset unrelated work
such as the World Studio changes committed separately during this task.

## Implemented

- Modern layout is enabled by default for the Android exploration menu.
- Fixed header, quick save, Settings and Close controls.
- Fixed Party, Items, Tinker, Journal and Map navigation.
- Party strip with portraits, level and HP; tapping a member opens their Party page.
- Selected member page with equipment, Attributes and Skills access.
- Items and Journal share the page's scrolling area instead of a capped nested list.
- Larger wrapping item filters and fewer enclosing frames.
- Gear browsing normally lives in Party. The Inventory Gear page remains available during the gear tutorial.
- Equipment options compare listed item attributes with currently equipped gear.
  These comparisons do not predict final combat stats or the effects of attached mods.
- Existing crafting, map, detail pages, equip/use actions and tutorial callbacks are reused.
- First normal modern menu opening starts on Party; subsequent openings remember the selected destination.

## Validation and next review

### Refinement pass

Character headers remain visible when the tutorial displays several loadouts. The party strip
is limited to Party and Items, with separate level and HP lines. Modern Supplies excludes equipment,
and its duplicate credits footer is hidden. Gear dialogs share the modern palette and quieter borders;
their close controls have larger targets. Comparisons use readable effect names, percentages,
and explicit numeric differences where both items list the stat. Missing attributes read "Not listed"
rather than implying a numeric zero. Classic presentation remains available.

Android `:app:compileDebugKotlin` and desktop `:desktopApp:compileKotlin` succeeded.
No tests were added or run. The desktop menu itself was not redesigned.

On-device review is still needed for small screens, increased font size, gear/tutorial flows,
equipment selection, quick save feedback, and switching between layouts across launches.
Compilation does not establish visual quality or playtest results.
Released as version 1.3.81 (165); see the release commit and internal-track upload record in the repository history.
