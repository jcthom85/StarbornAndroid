# World 1 “9/10” Prose Polish Plan

Date: 2026-07-15

Status: approved planning reference; implementation not yet started

## Summary

Polish the complete World 1 experience—from prologue through the launch/crash transition—using two implementation rounds separated by a review and three blind playtests. Preserve all canon, IDs, progression, inventory consequences, saves, and runtime behavior.

Before prose work begins, stabilize the dialogue and cinematic typewriter layouts so later copy changes can be judged without popup resizing jitter.

The four authority documents remain canonical. Archived, backup, and inactive content is outside the scored corpus.

## Phase 0: stable typewriter popup layout

### Current behavior

- `DialogueOverlay` already places an invisible full-text layout behind the revealed text. This reserves the final text height and should remain the shared behavior for ordinary dialogue and spoken cinematic steps.
- Dialogue responses appear only after the reveal completes. Lines with choices can therefore still change popup height at the end of the animation.
- Narrated cinematic steps render only the revealed substring. Their card is remeasured as words wrap and new lines appear, producing the visible stretching and jitter.
- `EventAnnouncementOverlay` presents its complete message immediately and does not currently use a typewriter reveal. It should remain stable and should not be converted to animated text as part of this work.

### Intended behavior

- Measure and reserve the final text footprint from the first frame while drawing only the revealed substring.
- Keep the popup/card at its final size throughout the reveal. Do not animate the container through every intermediate line height.
- Reserve the response/control region for dialogue choices when choices are present, then fade or enable the controls after the reveal instead of inserting them into layout late.
- Keep tap-to-complete behavior: the first tap finishes the current reveal and the next valid tap advances.
- Constrain exceptionally long text to the available safe screen height. The card remains fixed while the text region scrolls internally; it must not extend under system bars or off-screen controls.
- Reset measurement, reveal count, and scroll position when the dialogue line or cinematic step changes.
- Preserve accessibility semantics and expose the complete text to accessibility services rather than announcing each partial substring.
- Use the same stable text-reveal primitive for dialogue and narrated cinematics where practical, avoiding two implementations that can drift.

### Why not smooth the resizing

Animating the card height with `animateContentSize` would make the symptom less abrupt but would still move the popup, portrait, controls, and reading target throughout every line wrap. Reserving the final height is calmer, more readable, cheaper to lay out, and consistent with the technique already present in `DialogueOverlay`.

### Phase 0 acceptance criteria

- Short, multiline, and long dialogue/cinematic samples do not change outer card height during text reveal.
- A dialogue choice line does not jump when responses become available.
- Long text remains readable on the connected phone/emulator in portrait and landscape without clipping controls.
- Tapping during reveal completes it without advancing; tapping after completion advances exactly once.
- A new line/step never briefly displays the prior height, reveal substring, or scroll position.
- TalkBack/accessibility semantics expose stable complete copy and retain the existing advance affordance.
- Existing opening Maestro flows still locate and advance dialogue and cinematic overlays.

## Round 1: correctness, comprehension, pacing, and voice

### Baseline and inventory

- Record `git status`, the existing diff, JSON parsing, repository validators, Kotlin compilation, unit suites, save-migration tests, and relevant Maestro flows before editing production prose.
- Inventory every active World 1 string by text type: dialogue, cinematics, rooms, quests, events, milestones, enemies, items, skills, tutorials, shops, and hardcoded UI messages encountered during World 1.
- Preserve the pre-existing untracked migration handoff and prose-audit documents and all unrelated work.

### Canon and narrative causality

Preserve the complete opening chain:

1. Quota pressure motivates Nova.
2. Nova deliberately bypasses or explicitly confirms the cutter governor bypass.
3. Jed and Nova prepare the Cryo-Inductor and Flux Liner.
4. The live cutter test reaches the buried conduit, causes the surge, and wakes the Tuning Fork.
5. The system logs the cutter fault and Nova’s operator record.
6. The mine encounter uses 87 kHz, 68% cold loop, and 180-degree ground phase.
7. The cutter–suit–operator circuit completes the calibration handshake.
8. The Cryo-Inductor fuses; only the Flux Liner’s sacrificial strip is spent.
9. Nova receives a protocol mark without ancestry, prophecy, prior exposure, personal recognition, chosen status, or memory loss.

Do not change asynchronous callbacks, quest order, milestones, IDs, inventory consequences, unlocks, node transitions, save backfills, or combat balance.

### Prose pass

- Correct terminology and remove premature or generic Source-adjacent language where physical machinery is intended.
- Target 18 words or fewer and one idea per ordinary dialogue tap. Split longer lines when rhythm or comprehension improves; do not shorten solely to satisfy a metric.
- Target two concrete sentences and 45 words or fewer for ordinary rooms.
- Give each quest stage one actionable instruction.
- Make events and milestones report state and immediate pressure rather than restating the theme.
- Make enemy and skill descriptions mechanically useful: observable behavior, target, effect, status, duration, and cooldown where applicable.
- Preserve humor, warmth, worker detail, food, injury, and atmosphere.
- Preserve effective lines and rhythms, especially **“It is off. That hum is not mine.”**

### Voice targets

- Nova: tactile, sharp, mechanically fluent.
- Jed: care expressed through repairs, food, warnings, and preparation.
- Zeke: corporate language as humor and armor, without making every line the same joke.
- Gh0st: clipped, literal, action-led.
- Orion: permitted more lyricism, followed by intelligible instructions.
- System, quest, room, tutorial, and cinematic narration: distinct functional registers rather than a shared lyrical narrator.

## Round 1 checkpoint and blind playtest

After Round 1 implementation and validation:

- Produce a change ledger grouped by text type and speaker.
- Present representative before/after excerpts, including every canon- or progression-facing change.
- Perform a consecutive emulator read-through of World 1.
- Pause before the excellence pass.

Run three players unfamiliar with the canon through World 1 without explanation. Record:

- why Nova bypassed the governor;
- what caused the workshop surge and why she is sent into the mine;
- what the three tuning values control;
- what fused, what remained, and what the mark represents;
- perceived voice of Nova, Jed, Zeke, Gh0st, and Orion;
- slow, confusing, repetitive, unintentionally funny, or emotionally flat moments;
- remembered lines and desire to continue.

Treat an issue as confirmed when two players report it independently, or when one player encounters a progression or core-comprehension failure.

## Round 2: line-level excellence

- Correct confirmed comprehension and pacing problems first.
- Sharpen rhythm, humor, emotional transitions, and memorable phrasing without adding exposition.
- Remove remaining stacked metaphors, trailer lines, defensive canon explanations, and repeated poetic restatements.
- Preserve distinctive lines players remember positively unless they cause a concrete misunderstanding.
- Repeat the consecutive read-through and speaker/text-type voice audit.
- Update the prose audit with the completed World 1 assessment and remaining risks.

## Automated safeguards

Extend the World 1 prose validator without snapshotting the entire script:

- fail on encoding corruption and known forbidden canon contradictions;
- protect the benchmark line and required calibration/fault facts;
- warn above 18 dialogue words and fail above 32 unless explicitly allowlisted;
- flag ordinary room descriptions above 45 words;
- flag duplicate World 1 room/action prose, with a narrow allowlist for deliberate recurring system messages.

Tests should prefer milestones, state, inventory, callback ordering, and semantic facts over incidental exact wording. Update Maestro text selectors only when revised visible copy requires it; prefer stable semantic or accessibility selectors where available.

## Validation and test plan

Run before Round 1, after Phase 0, after Round 1, and after Round 2 as applicable. Record pre-existing failures separately.

- Parse every production JSON file.
- Run progression, World 1 content, room presence, dialogue-emote, audio-reference, and narrative-prose validators.
- Run `git diff --check`.
- Compile debug Kotlin.
- Run targeted `Hub1CriticalFlowTest` cases, the complete Hub 1 suite, opening save-migration tests, popup/reveal UI tests, and tests affected by selector changes.
- Run the complete JVM unit suite.
- Verify save/resume after Cryo repair, after the cutter surge, before and after the counter-tune, and during launch lockdown.
- Run on the connected emulator:
  - `start_new_game.yaml`;
  - `mainquest_wake_up_call.yaml`;
  - `mainquest_the_echo.yaml`;
  - `mainquest_red_alert.yaml`;
  - `mainquest_the_launch.yaml`;
  - `fun_opening_hook.yaml`;
  - `fun_opening_mastery.yaml`.
- Manually read every optional World 1 side quest, shop exchange, item/skill tooltip, enemy description, and launch/crash transition not exercised by those flows.

## Final acceptance criteria

- Dialogue and narrated cinematic boxes remain geometrically stable throughout typewriter reveals.
- All structural, save, progression, and UI tests pass with no new regression.
- Blind players accurately explain the opening’s motivation, physical causality, counter-tune, equipment consequences, and protocol mark.
- No blind player infers ancestry, chosen status, prior exposure, personal relic recognition, or memory loss.
- No recurring blind-playtest confusion or pacing complaint remains unresolved after Round 2.
- Each major speaker is identifiable without name labels in representative excerpts.
- No ordinary prose category scores below 8.5; World 1 averages at least 9.0 across canon accuracy, clarity, voice, restraint, atmosphere, mobile readability, mechanical usefulness, and consistency.
- The work is not described as externally validated above 9/10 until all three blind playtests are complete.
- Any unavailable test or unresolved contradiction is reported explicitly.
