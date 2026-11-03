# Android exploration pill sizing — November 3, 2026

Fixed a current Android layout defect, not just an outdated installed build. Before this change service labels used weight inside a horizontally unbounded scrolling row, allowing their width to collapse. NPC/item pills also combined weighted labels with fixed width limits or minimums.

Service, NPC and item pills now measure their natural content width: icon, full text, count where present, spacing and padding. Removed the weighted label columns and item-name weight, plus their arbitrary minimum/maximum widths. Existing horizontal rails, scroll indicators, service actions, NPC interactions and pickup callbacks remain. Extremely long entries can exceed the viewport and be read by scrolling rather than having their text shortened to ellipsis. Heights retain a 46dp minimum and expand when enlarged text/subtitles need more room.

`QuickMenuAction` and `RoomEntitySection` are module-internal so Android instrumentation can exercise the real implementation; they are not a new public API.

## Verification

- Debug APK and Android test APK assembled successfully; Android lint passed.
- `PresencePillSizingTest`: two tests passed on API 37 Android emulator at font scales 1.0 and 1.4. The tests measure actual service/NPC/item pill widths against the themed text plus icon/padding, reject excess width, check NPC subtitle height, and scroll/click longer entries through the real rails.
- Rendered frames were inspected. The enlarged-font check exposed fixed-height subtitle clipping, which is addressed by the minimum-height change.
- Evidence: `evidence/android_pill_sizing_2026-11-03/` contains instrumentation output, build/lint output, and normal/enlarged screenshots. Screenshots show the isolated production section with test data, not a complete quest-driven exploration session.
- No gameplay, quest, save, desktop or content changes in this fix. Existing Pixel installation was 1.3.104/build188; the new local APK is 1.3.105/build189. No Pixel installation was performed as part of this check.
