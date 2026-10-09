# Desktop menu and dialogue pass

## Implemented

- Cargo search, name/quantity/type sorting, separate key items and equipment,
  and selection fallback when a selected item is consumed or filtered out.
- Character selection and equipment search; item descriptions, equipped labels,
  weapon damage ranges and defense/HP/stat modifier comparisons against current
  equipment. Comparisons describe item values, not a prediction of final combat
  damage after every buff or multiplier.
- Journal views for active quests, completed quests, newest-first history and
  milestones. Search, tracked-first ordering and a separate objective/detail
  panel keep quest navigation distinct from historical messages.
- Spatial discovered-room map with connections, current-room marker, selected
  room details, threats, services and exit labels. Unknown destinations remain
  unexplored; selecting rooms inspects them without teleporting the party.
- Settings sections for music/effects/voice, display mode, tutorials and arcade
  accessibility. Saves use the asynchronous archive and overwrite confirmation.
  Loading another game remains a title-screen operation; the old direct state
  restore buttons were removed.
- Dialogue has a capped width, scroll support for long content, clean speaker
  headings and visible Enter/number-key guidance.

## Validation

Follow-up interaction checks and screenshot review are documented in
[Focused UI verification](FOCUSED_UI_VERIFICATION.md). The paragraph below records
the boundary at initial implementation time.

Compilation and portable Windows packaging completed. No tests were run for this
pass. Interactive checks across narrow windows, large inventories, long journals
and map layouts remain outstanding. These controls use existing authored data,
shared equipment rules and quest tracking.
