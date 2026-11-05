# Dialogue exits and overflow cues — November 5, 2026

Follow-up to the Pixel report that Evelyn and Robert remained difficult/impossible to leave and that the new text-only overflow hints were weak and premature. The earlier controller tests established available goodbye options; they did not establish Android touch behavior or that an exit remained visible during reveal/scrolling.

## NPC exit behavior

Normal NPC conversations now expose a fixed **Leave** button in their header, usable while text is revealing or either content area is scrolled. Android Back invokes the same cancellation. Desktop NPC overlays also expose Leave; Escape closes an NPC conversation.

The runtime explicitly cancels an NPC session: clears its active dialogue/choices and queued ordinary NPC conversation requests, preserves already-requested scripted sequences, resumes suspended notifications, and restores ordinary room music when appropriate. It does not advance the current line, choose a response, execute an unfinished gift/quest trigger, or automatically open the merchant greeting. Returning to the NPC uses existing dialogue eligibility and rewards.

This is shared NPC behavior across the campaign, including Worlds 3–6. It does not depend on each conversation author remembering to add a goodbye. Existing authored goodbyes remain available. Scripted `line:` sequences and cinematic overlays do not receive the NPC cancellation control; their existing advance/choice/leave behavior remains intact.

The Android portrait/name/Leave header now participates in normal card layout. The text and response areas share bounded remaining height, keeping the header reachable on smaller layouts and larger fonts. Reveal-tap interception is confined to the body; it no longer surrounds the exit control. Body drags remain scroll gestures rather than forcing a full reveal.

## Overflow indication

Replaced the plain “More dialogue/responses/description” labels with one themed control: a pulsing outlined **MORE** pill, double downward chevrons, and a viewport-edge fade. It overlays the scroll viewport instead of adding another row that reduces available text space. Tapping advances that viewport; manual scrolling remains available.

Visibility follows actual measured overflow:

- No cue while scroll extent is still unknown or the viewport is unmeasured. Compose initially reports an unknown maximum as `Int.MAX_VALUE`; treating that as ordinary overflow caused a premature cue even for a short description.
- Dialogue text requires the last actually revealed character's line to fall below the visible viewport. Transparent unrevealed text still reserves stable wrapping, but cannot cause an early cue.
- Response cues wait until responses are revealed and measured; a hidden response area does not advertise overflow.
- Room descriptions require measured content extending below the viewport.
- Each cue disappears at the end of its own scroll area. It does not obscure content once all of it is reachable in the viewport.

## Verification

Six Android instrumentation tests use actual touch injection on an isolated Android 17 phone emulator:

1. A reveal tap in the reserved response area reveals text without accidentally choosing a response.
2. The fixed Leave button accepts touch while dialogue is revealing, at 1.4× font scale in a constrained card.
3. Android Back invokes cancellation without selecting or advancing dialogue.
4. Actual authored Evelyn and Robert goodbye responses receive touch events.
5. Dialogue cues stay absent while the revealed text fits, appear once revealed text extends below the viewport, hide response overflow until choices appear, and disappear at the text's end.
6. Short descriptions have no cue; long descriptions can be paged to their end and lose the cue there.

Focused shared-runtime tests cover cancellation before Evelyn/Robert gift triggers, reopening and earning each gift normally, fixed dismissal for Cheno/Chris/Andrew/Jason/Meeko/Rylan/Cameron/Charles/Oliver, and World 6 Zeke/Gh0st/Orion conversations without changing rescue progress. A separate regression checks that canceling chatter cannot discard a queued scripted interaction. Existing World 6 navigation tests and dialogue graph/trigger tests also passed. The graph review finds an ending path for all 488 authored dialogue lines; that structural check alone is not a test of every quest-state combination.

Captured component layouts were visually reviewed; fixture screenshots are retained alongside the logs. They are Android component tests, not screenshots of the full game running on the user's Pixel. The Pixel was disconnected during this pass. A debug APK is built locally; no version bump, Git publication, or Pixel install is part of this repair pass.

## World 2 compatibility review

Compared the October 31 World 2 second-pass record and its existing dialogue instrumentation regression. The reserved-choice-area reveal protection remains covered by actual Android touch input. Unfinished line triggers remain unexecuted on cancellation, matching World 2's existing rule that final dialogue dismissal credits progress. Charlie's object-based Back interaction is unchanged; the existing Charlie conversation regressions and Caretaker Echo's known-state topics remain valid. All **7 World2ExplorationPolishTest checks passed** alongside **8 host dialogue checks**, the focused later-world runtime suite, and **6 Android touch tests**. Debug APK assembly succeeded.
