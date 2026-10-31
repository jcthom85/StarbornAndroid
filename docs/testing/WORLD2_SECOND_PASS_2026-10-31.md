# World 2 changes: second pass and acceptance status

October 31, 2026. Follow-up to the review of changes since World 2 began. Image generation, replacements and sprite padding edits were deferred during this pass. They are now handled in the separate [World 2 art pass](WORLD2_ART_PASS_2026-10-31.md); validation thresholds remain unchanged.

## Changes made

- Cooking results now use the existing narration card inside a modal, so cooking below the top of the recipe list cannot hide the result. Dismissing it preserves the list position.
- Generic objective actions no longer announce “Objective completed.” before the event runs. Ineligible placeholder actions show a neutral inspection and do not play success-like interaction audio. Authored eligible events supply their own feedback. Other authored fallback messages retain their existing behavior.
- Rapid Capacitor describes its actual Speed/Agility bonuses; no cooldown mechanic was added.
- Passive-inspection receipts depend on the object's name and response text, rather than unrelated surrounding room prose. Changed inspection information becomes unread; functional controls and disappearing collected objects retain their existing behavior. Existing older receipts may need one inspection to establish the narrower key.
- Temple reveal clears its modal blocker in a cancellation-safe `finally` block.
- Gh0st's confrontation credits its quest task, room flag and milestone only after the final dialogue line is dismissed. Entry is repeatable while unfinished, including old entry receipts, so saving/reloading an interrupted conversation does not skip the scene or strand the player.
- Bag/equipment tutorials can appear over their own field menu. Other tutorials, rewards and notices continue to wait behind it; other scene blockers still take precedence. This corrects a regression discovered during the full host run.
- Regenerated World 1's ten packaged checkpoints through the earned completionist route/import procedure. Regenerated the three existing crew checkpoints through their production generator; they now include the encounter reward and camp changes. No quest/reward snapshots were hand-edited.
- Added production-rule encounter coverage using earned World 1 resources and actual World 2 travel, covering all three pairs across seeds 1–5 and 17. The older canned loadout test is supplementary, not the balance certification.
- Added an adverse-situation check for recovering Zeke's required ordinary Medkit after spending that kit and all credits.
- Automated playthrough transcripts now write under build reports rather than overwriting curated playtest notes; preserved the existing World 1/2 documentation.
- Updated stale scripted tests to actually finish Gh0st's dialogue, expect the current object names, and stop expecting removed camp stabilizer gifts. Reset developer OP mode after its test so it cannot contaminate later combat assertions. Raised the host-test heap from 512 MB to 2 GB after the larger suite exhausted the old allocation.
- Updated Charlie walkthrough labels from “Leave it be” to the actual standard dialog's “Back”; rebuilt the companion site output. These local changes have not been published to GitHub Pages in this pass.
- Incremented the Android build to **1.3.101 / 185**, distinguishing it from earlier builds sharing 1.3.100 / 184.

## What is still deliberately unresolved

| Item | Status and next action |
|---|---|
| Stalker-Vine bottom padding; Penny and Razor-Vine artwork | Image work deferred by request. The sprite validator still fails; CI must not be described as green. Review Guardian/Bulwark and Orb/Buoy family reuse with the image pass. |
| An already completed, unpaid legacy Botanist quest | Cannot safely infer payment from existing save fields. No automatic compensation was invented that could duplicate XP/AP/items for paid saves. Preserve an affected save for a targeted restitution decision. Fresh/in-progress completion and payout remain covered. |
| Command-style action anchors | “Scavenge the remains,” “confront the stalker,” and “face the Beast” remain deliberate functional action wording. They are not passive inspections and should not acquire read tint. A future wording decision can replace them individually; no blanket substitution was made. |
| Broad balance/medical economy | Earned pair coverage and the replacement-kit route improve evidence, but do not certify every build, status-removal stockpile, random seed or later-world economy. Camp rest restores HP; it is not assumed to replace consumables. |
| Pixel acceptance | No physical Pixel was connected. Emulator and host evidence do not accept handheld fishing feel, audio composition, margins or touch ergonomics. |

## Focused Pixel checklist

- Charlie: Back changes no trust; confirm rolls/tosses the ball; standard narration precedes the stone reward; repeat/reload cannot duplicate rewards. Check both screen edges, leaving/backgrounding during play, and reduced-motion settings.
- Gate: magical reveal spans the scene; Chime and reveal feel synchronized; popups wait; east opens physically; interrupted/backgrounded playback leaves controls usable; old open saves do not replay it. Check reduced-motion and flash-disabled settings.
- Cooking: scroll to a low recipe, cook, read the standard result without scrolling, dismiss, and confirm inventory/batch/meal replacement. Verify large text and touch targets.
- Menus: after Jed, bag and equipment guidance appear in the menu and can be dismissed; unrelated items/quest notices wait. Exercise an equipment offer's Later and primary-action routes.
- Inspections: tap, leave/reload and return; unchanged response stays read after an unrelated quest changes room prose. A genuinely changed response becomes unread. Collected containers disappear normally. Inactive quest controls never claim objective completion.
- Gh0st: save/quit before finishing the confrontation, resume/restart, finish it, then fight the Beast. Confirm no early completion and no repeated rewards.
- Audio: exploration → dialogue → exploration; combat and boss entry/exit; film playback; camp chime; Sentinel voice; background/foreground. Automated playback checks establish routing, not whether transitions sound good.
- Fishing: touch-only hook/reel/release, failed catch/retry, successful catch inventory, background/foreground and long session. Separately assess optional motion controls and accessibility/TalkBack.
- Combat/exploration: early pairs with the actual saved build, status-removal supply demand, ability details at large font, long enemy names, victory timing, map exits, two/four-member party rail and service pill widths.
- Container queue: verify discovery text before every obtained-item group, no reward flash/double-tap-through and no duplicate grants after reload. Shared changes include 51 containers in Worlds 2–6; this pass does not claim full playthrough acceptance of those worlds.

## Evidence

- **781 host tests passed**, zero failures/errors/skips, followed by all six audit-output tests passing after relocating their generated files. [Suite counts](evidence/world2_second_pass_2026-10-31/host-suite.json), [host/build output](evidence/world2_second_pass_2026-10-31/host-and-build.txt), [audit output verification](evidence/world2_second_pass_2026-10-31/audit-output-tests.txt).
- **18 earned production-rule pair victories** across seeds 1–5 and 17. Each run actually earns its World 1 equipment/resources, travels World 2, defeats the vine/spitter, hounds and borers, and continues through departure. Observed healing demand: zero or one Medkit per pair, no knockout. This tests one combat policy/build, not all possible builds. Per-seed reports are in the same evidence directory.
- **10 Android instrumentation tests passed** on the API 37 ARM emulator: cooking from below the top of the list, reserved dialogue-choice tapping, swipe/reel fishing with one-time inventory delivery, standard inspection/rest offers, large-text party/menu controls, and music resume/restart/background policy. [Full result](evidence/world2_second_pass_2026-10-31/android-instrumentation.txt). Emulator music policy checks do not evaluate composition or listening quality.
- **Lint passed**, zero errors, 369 warnings and 10 hints. The first broad attempt encountered a lint FIR analysis exception; the stable final rerun passes. [Final output](evidence/world2_second_pass_2026-10-31/lint.txt).
- Both debug and Android test APK assembly passed. Emulator installation confirms **1.3.101 / 185**. APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `03080486adb0962d66378932b3c5ef55a39cdd0f861b2631140d9bb66b3425f5`.
- World 1 checkpoint verification passes for all ten earned snapshots. The companion site rebuild succeeds: two worlds, 38 sections, 377 steps. Diff whitespace checks pass.
- No image assets changed. Enemy sprite-bounds validation remains blocked by the explicitly deferred Stalker-Vine padding fix; this is not a fully green CI claim. No new commits, pushes, Pages publication or Pixel installation were performed in this pass.

Historical handoffs remain historical; “Addressed” in the considerations table means implemented, not Pixel-certified.
