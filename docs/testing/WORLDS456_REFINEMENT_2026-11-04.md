# Worlds 4–6 refinement and acceptance — November 4, 2026

Scope: apply the focused World 3 treatment to the Foundry, the orbital/deep Ring, and the Source. Evaluate the current executable content, repair demonstrated interaction problems, exercise all optional quests and complementary major branches, and document the playable routes. Existing work in this dirty checkout is preserved. This report describes this pass, not every change in the checkout.

## Results by world

| World | Assessment after this pass | Verified coverage | Remaining design limits |
| --- | --- | --- | --- |
| 4 — Foundry | Stronger delivery of Gh0st’s central reveal and choice; reliable optional combat ladder and rewards. The industrial setting has a coherent sabotage-to-escape progression. | Five main quests; Sabotage, Lost Worker, Scrap Heap, both Quality Control outcomes, Overclocked, and the three-round Crucible. | Several machinery sequences still advance through a chain of object clicks. Overclocked is a calibration interaction, not a combat gauntlet. The arena’s atmosphere does not implement an Overheat combat rule. |
| 5 — Ring | Largest improvement in moment-to-moment identity: meaningful pressure sequencing, distinct service detours, and accessible Thorne/Elara/Vale scenes. | Five main quests; all five optional quests; independent guardian and Guardian Covenant outcomes; side quests in forward and reverse order. | The bulkhead puzzle is a clear sequencing exercise with an explicit clue, rather than a difficult deduction. Mirror and firewall actions remain comparatively straightforward. Station combat still rewards preparation. |
| 6 — Source | More coherent finale guidance and reward communication. The ending survives a fresh runtime and cannot pay twice. Existing personal choices remain intact. | Five main quests; all five optional quests; both HR endings; nightmares in forward and reverse order; both real Ascended battles and credits. | Memory refusals and bridge anchors remain linear interactions. The final tuning panel is unchanged. Emotional pacing and whether the backtracking feels worthwhile require human play. |

These worlds have verified main-and-optional completion routes. That is stronger evidence than a content percentage, but does not establish universal completion, balance across builds, or subjective fun.

## Demonstrated findings and repairs

1. **NPC identity and scene access.** Thorne and Vale’s full NPC display names had no matching dialogue speaker catalog. The exploration controller now keeps a display-name catalog where one exists, otherwise resolves the speaker from the NPC’s declared talk line. Normal conditioned entry selection remains active; the declared line is not forced unconditionally. Shared-controller tests use the actual visible Thorne and Vale NPCs.
2. **Seven scenes bypassed by object actions.** Project Phantom, Cooling Springs, Rylos, Thorne’s trace, Elara’s reunion, the Soloist, and the final Vale confrontation now open their authored dialogue before applying their existing completion actions. Vale’s final line explicitly continues to Nova’s response. Repeatable scene openers check their room and existing quest prerequisites; original event IDs, rewards, and save receipts remain compatible. Earned routes use the visible controls rather than an invisible Rylos conversation.
3. **Twenty-eight one-use return shortcuts.** Foundry, Ring, and Source return events are now repeatable. Event regression checks exercise every shortcut with a legacy completion receipt, twice.
4. **Vacuum Seal promised an ordering puzzle but sealed everything in one tap.** The airlock console now runs a three-step bulkhead simulation using the existing pressure-map clue: carpet seam, docking collar, observation glass. Wrong choices explain the pressure failure and allow reset or departure without damage, item costs, or quest progress. Reloading an unfinished simulation restarts it and retains the mapped clue. The existing success event and 450-XP/Mag-Boots reward remain intact; dock access still requires a separate final action.
5. **Repeated service-room prose.** Thirty-two World 5 rooms now have location-specific descriptions and inspection responses tied to docking, hospitality, gardens, security, service shafts, and server cooling. These changes add contextual observations, not additional switches, rewards, or routes.
6. **Reward promises did not match effects.** Explosive Tip, Thermal Clip, Void Clip, and Starborn are accessories. Their unused `attack_element` metadata did not convert weapon attacks; descriptions now state their actual stats. Starborn’s critical bonus was 0.15 percentage points in a percentage-point engine; corrected to 15 points. An earned equipment comparison in actual combat setup verifies +5 Strength, +5 Focus, +3 Luck, +15 critical points, and unchanged weapon element. Late passive descriptions now reflect existing runtime effects and conditions.
7. **Misleading progression feedback.** Scrap Heap no longer announces a nonexistent Scavenger skill. The personal HR exit now preserves the evidence instead of claiming to delete backups; a duplicate milestone action is removed. Ascended victory messages and the journal identify the north path through White Shore and Key Orbit to the Silence Pulpit, then the return to The Center for tuning.
8. **Crucible rules and Overclocked feedback.** Brutus explains the actual teams, retreat/retry, inter-round rest, and the complete reward. Removed the quest’s unsupported Round 1 Overheat promise and clarified the cage’s emergency withdrawal. Overclocked’s event now checks the mapped clue required by its room control; descriptions correctly call it calibration rather than surviving waves. Legacy IDs remain unchanged.
9. **Outdated walkthroughs.** Replaced the three earlier guides, including speculative levels, playtimes, and gold-master claims, with current main routes, optional interactions, completion payouts, choices, recovery behavior, and explicit verification limits.

## Earned completionist evidence

The test starts a new campaign under production crew progression, plays Worlds 1–3, and saves the earned World 4 arrival. Two complementary Worlds 4–6 routes start from that save. They use actual controller actions, legal navigation, normal dialogue, earned AP, ordinary shops/equipment/rest, actual ATB combat, and disk save/reload. No inventory grants or synthetic victories fund the route. Combat seed 17, authored answers, virtual time, and a known supply/build policy are fixture constraints.

Each route completes all 15 late main quests and all 16 optional quests (six/five/five). Every optional completion checks exact XP/AP, inventory changes, and save/reload persistence. Reward controls are retried. Opposite Quality Control, guardian, and HR endings are attempted after completion to verify exclusive outcomes and no extra payout.

- **Crucible:** starts real encounters through Brutus, retreats from round one through the combat command, reloads, retries, wins all three real fights, reloads between rounds, claims exactly 500 XP/350 credits/one Forge-Plate, and verifies repeat dialogue cannot award again. A separate shared-controller fixture invokes the normal defeat callback and checks a reloaded retry; it does not simulate a full losing fight.
- **World 5:** earned credits, equipment, rest, meals, skill-tree purchases and healing supplies support the fighter, officer, Firewall Construct and Compliance Avatar. Optional quest order reverses on the complementary run.
- **World 6:** nightmare order reverses. Final tuning is checked while locked before both victories. Both Ascended fights resolve through the actual combat engine. Saving during the pending Final Note and reloading preserves completion; final XP is exactly 3,000 once, with credits marked seen.
- **Fresh runtime:** a separate seeded desktop fixture saves after tuning but before cinematic completion, closes all services, opens a new runtime, loads the slot, finishes/skips the ending and epilogue, reaches `source_new_world`, then reloads the completed save and verifies no duplicate XP. Seeded controller fixtures verify transitions; they are separate from the earned campaign proof.

## Verification

- **799 Android host tests** passed, with zero failures, errors, or skips.
- **19 focused desktop/controller tests** passed, with zero failures, errors, or skips.
- `:app:assembleDebug` passed; all 40 JSON catalogs packaged in the APK match the source catalogs.
- `git diff --check` passed. No installation, commit, push, or publication was performed.

Final test results, build hash, command logs, route trace, and source hashes are recorded in [the evidence directory](evidence/worlds456_refinement_2026-11-04/verification.json). The focused desktop set includes the World 3 regression coverage and shared content/progression checks.

The initial full run caught the missing Dock Interlock voice profile and old discoverability fixtures that assumed object actions immediately completed scenes. The console now has an explicit silent voice profile; those fixtures seed the physical room and distinguish repeatable scene opening from one-time quest rewards. New controller tests check the actual dialogue-to-reward path.

## Human acceptance still required

Play Worlds 4–6 on a phone without the walkthrough first. Check whether the pressure-map clue is found naturally, whether the simulation options and long dialogue fit comfortably, whether companion scenes get space between notifications, whether arena retry is obvious, and whether the finale directions feel natural. Observe supply needs on the player’s own build. This pass does not claim every optional order, every battle seed, a full losing arena battle, or device input/audio/visual acceptance.

Updated guides: [Foundry](../playtest/walkthroughs/WORLD_4_COMPLETIONIST_WALKTHROUGH.md), [Ring](../playtest/walkthroughs/WORLD_5_COMPLETIONIST_WALKTHROUGH.md), [Source](../playtest/walkthroughs/WORLD_6_COMPLETIONIST_WALKTHROUGH.md).
