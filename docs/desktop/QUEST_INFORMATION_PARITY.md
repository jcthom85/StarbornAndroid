# Quest presentation and information parity

Implemented October 7, 2026. This supersedes the quest-notification and Journal findings in the older parity matrix.

## Quest presentation

- Quest events no longer become strings in the left narrative pane.
- Important quest events render centered cards with the original event title, summary and objectives, Continue/Details/close actions, keyboard dismissal, theme accent, and Android's new-quest shimmer.
- The original quest cue is dispatched once per visible card; deferral and temporary hiding do not replay it. This verifies dispatch, not audible output.
- Progress events render a top-center banner with objective completion/next roles and remaining count. Android timing is retained: 3.2 seconds, 220 ms entry, 180 ms exit, two-second duplicate suppression.
- Important events and summaries queue; identical quest/type cards already pending are suppressed. Summary events have their own centered presentation. Ordinary toast events are separate transient notifications.
- A service-owned consumer collects events while exploration is offscreen, so combat/side-activity quest notifications can be shown on return. New/load/reset clears pending presentation state.
- Cinematics, dialogue, menus, inspections, tutorial prompts and other blocking interactions defer display. Important cards block exploration input and restore its focus after dismissal; progress banners do not block movement.
- Details opens the corresponding shared quest details in Journal. Journal badge deltas are consumed and cleared when Journal is opened.
- Event payloads and quest runtime rules remain unchanged. In particular, Android currently suppresses legacy non-progress banner events; the desktop follows that routing rather than creating additional new/completed notifications.

## Information pages

- Journal uses shared QuestDetailUi for visible stages, individual objective completion, rewards and tracking. Authored description/flavor remain intact; unreached stages are filtered by the shared builder.
- Fishing records use the existing runtime journal, distinguishing caught/clean catches and masking undiscovered species.
- Equipment compares supplied damage range, defense, HP bonuses, accuracy, critical chance and stat modifiers. Missing absolute stats are labeled Not specified; additive absent bonuses are zero. Comparisons are item stats before character bonuses, not fabricated derived character totals.
- Candidate/current feature descriptions are separate from numeric deltas. Ollie equipment management is hidden according to the existing shared gear restrictions.
- Menu feedback uses the runtime's expiring menuFeedback instead of falling back to stale persistent status messages.

## Validation

Seven focused tests pass: typed queue ordering/deduplication, update objective roles/counts, summaries/toasts/badges/reset, once-only presentation starts, deferred cards and original quest audio dispatch, timed/manual update dismissal, and shared Journal completion/reward/fishing presentation.

Build/test log: `tmp/quest-ui-verification.log`.

Rendered screenshots inspected: `desktopApp/build/reports/desktop/screenshots/quest-new.png`, `quest-completed-narrow.png`, `quest-updated.png`, and `journal-quest-details.png`.

Portable packaging log: `tmp/quest-ui-package.log`. Packaging runs the original Android asset parity check.

Native opening-to-first-combat playtesting, actual sound output, high DPI, populated fishing records, broad equipment combinations, and full save/load failure scenarios remain to be reviewed. These focused checks do not establish full-game parity.

## Next priorities

Combat particle/attack effects and staged victory presentation; side-activity presentation; display/background lifecycle; then a native release playthrough with save/reload checkpoints.
