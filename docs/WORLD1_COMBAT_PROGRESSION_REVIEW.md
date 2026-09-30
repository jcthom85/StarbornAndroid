# World 1 combat teaching and progression review

Date: September 30, 2026. Source review only; no tests or combat simulations run.
No production changes made during this review. Historical audit results are
context, not verification of the current working tree.

## Assessment

The teaching sequence is appropriate: the workshop loader demonstrates Shock
weakness, certification demonstrates Guard Break, mine groups introduce target
selection, and the escape/Warden combines earlier skills with Blast Wave.
Room-specific loader and trainer overrides keep the first lessons smaller than
their catalog counterparts. Do not increase complexity or change enemy stats
without current route evidence.

## Findings and recommended order

1. **Certification ordering/recovery — investigate first.** Customer Service
   connects to Security Post without a quest gate; the trainer is authored there
   unconditionally. Tutorial eligibility requires the training quest, and the
   training victory event requires it to be active. Cleared encounters are
   persisted and removed from rooms. Together these expose a potential early
   trainer victory followed by a blank certification log. Reproduce before
   calling it a confirmed softlock. Prefer recognizing the prior drill victory
   or controlling drill availability over forcing a second paid fight.
2. **Trainer/live enemy identity — confirmed content inconsistency.** The shared
   acoustic_bulwark definition is named Shield Trainer and describes a dummy;
   it also populates the Riot Control Post and Cargo Lift. Give the drill a
   context-specific identity while live encounters read as Dominion opponents.
   Preserve compatibility with existing enemy IDs and victory events.
3. **Mine objective/route agreement — confirmed authored mismatch.** The journal
   asks players to defeat the Riot Control Post guard. That post leads only to
   a gas-pocket dead end; the threshold route runs east through the conveyor,
   sifter and shoring. Threshold entry advances the stage without checking that
   guard task; Fork success does not check it either. Decide whether this is
   optional reinforcement or mandatory training, and align objectives/gates.
   Recommendation: retain it as a clearly optional combat/reward detour unless
   the main route requires its lesson.
4. **Certification evidence — narrower than earlier descriptions suggested.**
   With tutorials enabled, CombatTutorialTracker guides Hydraulic Kick against
   a seeded invulnerable trainer. The victory event itself checks neither room
   nor skill use. Do not claim new players receive no instruction. Scope drill
   completion to intended contexts while preserving legacy workshop completion;
   tutorial-disabled players should retain a valid progression path.
5. **Escape recovery/pacing — design review needed.** The Cargo Lift rest action
   fully restores party HP without inventory cost and has no authored quest
   condition. This is useful protection against supply exhaustion before the
   Warden, but its availability and text should fit the post-combat respite and
   Jed's sacrifice. Do not remove this safety valve without route evidence.

## Balance evidence still needed

Compare campaign-earned checkpoints for a direct route and optional-content
route, carrying real HP, equipment, skill choices, consumables and rests. Review
two-borer pressure, support-drone priority, and the Warden's shield/drone/gas
combination. Asset HP alone is not difficulty; formulas, overrides, cooldowns
and equipment matter. Existing scripted story audits assume victories and
cannot establish route survival or newcomer win rates.

## Next implementation scope

Resolve certification ordering and legacy completion, separate trainer/live
presentation, and clarify the mine detour objective. Keep the current puzzle
structure and combat stats until current runtime/route evidence supports changes.

## Implementation follow-up

- Scoped certification victory to Security Post and the legacy workshop drill.
- Added a durable drill-victory milestone, recorded even before the quest starts.
  Boggs registers a prior result once; entering Security Post can recover an
  active quest with prior victory evidence. Claim conditions protect completed
  tasks/quests from repeat certification payouts.
- Existing cleared-trainer room states backfill victory evidence on load.
  Completed training saves retain certification and Hydraulic Kick.
- Kept canonical enemy IDs, combat stats, rewards, and legacy victory events.
  Acoustic Bulwark is the live identity; drill rooms override only its display
  name, flavor and description. Exploration accessibility labels follow context.
- Added a backward-compatible optional task flag for the riot-post detour.
  Optional tasks do not block advancement or become the next required banner/hub
  objective, and skipped optional tasks are not automatically shown as completed.
- Updated quest schema, walkthrough and existing workshop Maestro selectors.
- Android debug build and desktop Kotlin compilation pass. Edited catalogs parse
  with no duplicate IDs. Tests and on-device acceptance have not been run.
- Acceptance still needed: early victory then first Boggs conversation, old-save
  active certification with a cleared trainer, normal/legacy drill completion,
  claim replay, tutorials disabled, and main mine route with the detour skipped.
