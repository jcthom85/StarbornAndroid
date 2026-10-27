# Exploration popup presentation

Pam manages the Mess Hall and its serving counter; Rourke prepares everyday meals in the Ration Sink. Pam retains her own biscuit specialty and one-time gift. NPC identifiers and the legacy biscuit item identifier are unchanged for saves and quest triggers. Player-facing text uses Pam throughout. Pam has a new transparent portrait at `images/npcs/pam.png`; Rourke retains `images/npcs/vance.webp`.

## Presentation rules

- Active conversations, cinematics, modal choices, and activities hold focus. Supporting notices wait until those interactions finish.
- Inspections are queued rather than overwriting a message the player has not read. After inspections come event announcements, then tutorial/milestone/item prompts.
- Quest details and scene summaries share one ordered queue on Android and desktop. They cannot display over each other. Android keeps this queue in AppServices so leaving exploration for another screen does not lose pending notifications.
- Quest updates and passive toasts run after blocking notices. Only one passive toast displays at a time. Hidden notices do not consume their reading timer.
- The background tap/key handlers acknowledge only the visible notification. Prompt and quest-card dismissals carry the displayed identity so a delayed second click cannot dismiss the next card.
- Level-up notices wait for conversations and activities. Recovery and reward notices do not cover dialogue.
- Loading a different saved session or starting a new game clears pending quest presentations.

Quest state, inventory grants, XP, event triggers, and save identifiers remain owned by their existing systems. Showing or dismissing a card does not award a reward again. Existing prompt priority is retained; the changes coordinate visibility and lifetime.

Pam's looping default conversation is now a valid entry point and includes a Leave option. Her gift condition and milestone still prevent repeat rewards. Conditional quest dialogue continues to precede default conversation.

## Verification

Targeted regressions cover Pam's topics, exit, portrait, and one-time gift; Scrapper's quest acceptance and turn-in; hidden notices and paused timers; stale dismissals; queued inspections; rest/cooking behavior; and asset integrity. Desktop screenshots are generated in `desktopApp/build/reports/popup-flow/`. On-device acceptance remains separate from these automated checks.

Validation result: Android and desktop compilation and 95 targeted tests passed. Asset parity verified all 1,332 Android assets in desktop staging and packaging. Pam dialogue and deferred reward screenshots were rendered and inspected.

## Jed opening acceptance cases

The Jed acceptance flow runs with tutorials enabled: first conversation in `pit_jed_bunk`, starter-kit cards, workshop briefing, opening the workbench, the real repair-result callback, post-repair dialogue, grounding/governor preparation, the cutter cinematic, and the Shift Clearance handoff after Wake Up Call completes. Rewards and quest progress are asserted separately from presentation.

Equipment and first-AP offers wait behind reward cards. Dismissing the AP notice completes the notice without opening Skills; its explicit action opens Party > Nova > Abilities. Triggered follow-up dialogue is queued when another conversation is still active, and finishing an old dialogue cannot clear a replacement session.

Screenshots from this flow are generated under `desktopApp/build/reports/jed-popup-flow/`. The test acknowledges notices rather than deleting the queues it is exercising.

The Jed flow also checks obsolete progress: a queued objective banner for a quest that just completed is removed, including late-arriving progress events. Multiple pending updates for the same quest keep the newest objective state instead of replaying a backlog.

Jed follow-up validation: 77 targeted dialogue, tutorial, quest, reward, merchant, rest, and data-integrity tests passed on the final changes; Android and desktop compilation passed. The final rendered workshop frame returns to exploration without a forced Skills menu or stale Wake Up Call objective banner. Pixel/on-device acceptance has not been run for this batch.

Reward grouping: once the first card of an item sequence is presented, the remaining cards in that sequence precede unrelated queued tutorials and milestones. Individually granted items queued during the same notification period receive the same continuity. An active dialogue or modal scene can still pause presentation; the remaining rewards are retained and resumed. Targeted prompt-manager tests and the Jed and merchant flows passed after this change, along with both platform compilations.

Release 1.3.98 (Android version code 182): the final APK build, 112 targeted tests, and desktop asset parity checks passed. Android and desktop package versions are aligned at 1.3.98.
