# Foundry progression audit

Date: 2026-09-21. Read-only production audit plus test-only fixtures. This is not a campaign-earned save or a balance sign-off.

## Established facts

### Follow-up: XP discrepancy resolved

The earlier 3,700-XP scan below was incomplete: it omitted nested cinematic `on_complete` actions (750 XP) and Zeke's `zeke_w1_mq02_override_7` dialogue trigger (75 XP). Recursive event inspection plus production dialogue parsing reconciles **every one of the fifteen main quests**, totaling **4,525 XP**. No missing-reward defect is established by that comparison. Evidence: `test-results/foundry-progression-audit/xp-sources.txt`.

`FoundryProgressionAuditTest` checks that reconciliation against real assets, runs the actual cutter-surge event with its prerequisites to verify 50 XP is deferred until cinematic completion and not re-awarded on event retrigger, and verifies recruitment catch-up through GameSessionStore/ExplorationXpAwarder. The source reconciliation is not proof that every quest has been traversed in-game.

`GameSessionStore.addPartyMember` seeds missing companion XP and level from the leader. The exploration join hook only announces recruitment; subsequent XP awards unlock level-appropriate skills. Existing companion values are retained. Equal XP is therefore plausible on an uninterrupted route with no member removal, not inherently a fixture error.

A partial story-source tally adds **1,905 XP and 1,100 credits** from enemies explicitly named by World 1–3 main-quest victory triggers (including Echo Borer separately in Worlds 1 and 2). Together with quest grants this gives **6,430 XP, level 7**. This is a scenario construction, not a proved minimum or full route total: unnamed gauntlet/sewer parties, other room/moving encounters, optional content, repeat encounters, alternate gates, spending and drops are not reconciled. The 1,100 credits alone do not fund the 2,328-credit purchased set. That set remains a sensitivity control, not a certified affordable arrival build.

The driver now accepts `FOUNDRY_XP` (default 11000) and derives level/level-granted skills from real progression assets. At 6430 XP it grants level-6 skills but excludes level-9 skills, with a regression assertion. Nondefault XP is included in evidence filenames to distinguish runs. Other fixture assumptions, including full HP and three medkits per isolated fight, remain unchanged.

- `QuestAssetDataSource` loads `quests.json`, not `quests_base.json`.
- The fifteen World 1–3 main-quest reward listings total **4,525 XP**. These are listings, not a verified received-XP ledger. Explicit `give_xp` actions in events whose IDs begin with `w1_mq`, `w2_mq` or `w3_mq` total **3,700 XP**. Do not add these totals together: they may describe the same reward. Other dialogue, event and combat awards still require route verification.
- `EventManager` quest completion updates session state and invokes hooks. The inspected exploration completion hook records/completes the quest and emits a stage-complete trigger; it does not directly iterate the quest's displayed reward list. Therefore reward delivery must be traced through actual events/hooks, not inferred from the quest UI.
- Level 9 requires **11,000 XP**. Exploration awards go to current party members, with session fallbacks; combat awards go to the combat party. Recruitment timing and initial companion XP must be retained when constructing a route ledger. Do not assign everyone Nova's lifetime total without checking.
- Neither the inspected exploration leveling path nor combat leveling path grants AP per level. Combat awards explicit enemy AP once into the shared session pool.
- The Administrator grants **2 AP**. The World 3 launch event requires its defeat task before unlocking World 4. Other enemies with positive authored AP rewards are Titan Walker (3), Compliance Avatar (4), Ascended Vale (5) and Ascended God (8), all later progression candidates rather than pre-Foundry income.
- A legacy event, `qpack_evt_relay_stage4_complete`, grants 1 AP, 420 XP and 150 credits. Its quest `q_mine_relay_scramble` is absent from the active `quests.json` catalog. Exclude it from the normal-route budget until runtime reachability is proven. Thus 2 AP is the verified main-route enemy reward budget, not a claim that no other reward code can ever grant AP.
- The purchased equipment set costs **2,328 credits** and passes shop gate/compatibility validation. This proves availability, not affordability after other spending. Three medkits per fresh fight remain a controlled allowance, not verified remaining campaign inventory.

## Budgeted AP comparison

Added `weak_ap` and `purchased_ap` test fixtures. Each spends the two shared AP on Nova's `nova_quiet_steps` and GH0ST's `gh0st_scope`, one AP each. Production `evaluateNodeStatus` validates both purchases, including prerequisites and row gating. Remaining AP is zero. Exported runtime stats verify Nova gains 5 speed and GH0ST gains 10 accuracy.

This is one legal allocation, not an optimized or observed player build. Gear, level 9 assumption, offensive policy and three-medkit allowance remain unchanged to isolate this allocation's effect. Earlier fixtures already included level-unlocked skills; the new AP budget is additional spending, not a charge for those level grants.

Seeds 41–70; six parties; two builds; support-first and drone-first policies: **720 terminal fights, 479 wins, 241 defeats, zero timeouts**. Both fixture tests and the combat-driver test passed. Evidence: `test-results/foundry-ap-30-seed/`.

Wins out of 30, ordered **support-first / drone-first**:

| Party | Starter + 2 AP | Purchased + 2 AP |
| --- | --- | --- |
| Drone | 30 / 30 | 29 / 29 |
| Golem | 30 / 30 | 30 / 30 |
| Welder | 30 / 30 | 30 / 30 |
| Welder + Drone | 0 / 6 | 0 / 0 |
| Golem + Welder | 26 / 23 | 30 / 11 |
| Golem + Drone | 0 / 12 | 0 / 13 |

Compare with the same seeds/policies in `foundry-tactics-30-seed`. Purchased Golem + Welder support-first improves from 20/30 to 30/30; purchased Golem + Drone drone-first improves from 6/30 to 13/30. Welder + Drone remains severe. Solo purchased Drone drops from 30/30 to 29/30: changing speed changes action order and random-number consumption, so identical seeds do not imply identical hit rolls or monotonic improvement. These are scenario outcomes, not population win rates.

## Next acceptance gate

Completed level sensitivity: `FOUNDRY_XP=6430`, seeds 41–70, `weak_ap,purchased_ap`, `support,pressure` produced 530 wins / 190 losses / zero timeouts in 720 fights. Evidence is preserved at `test-results/foundry-level7-30-seed`; the report contains the party breakdown. The level-9 counterpart won 479/720. Since new unlocks change the fixed skill priority, this is not a controlled claim about leveling strength. Hold chosen skills constant in a follow-up before attributing changes to level or recommending skill tuning. No campaign route was traversed by either batch.

Before selecting ordinary campaign loadouts, build a runtime-supported ledger from a new-game route or verified checkpoints:

1. Record each reward source once, with event/encounter ID, party at receipt, before/after XP, AP, credits and inventory. Distinguish optional, repeatable and mandatory sources. The aggregate quest-listing/event-grant discrepancy is resolved above; verify actual delivery along the route rather than reopening that incomplete scan or counting listed and delivered rewards twice.
2. Record companion recruitment and catch-up behavior; calculate each arrival level from received XP. Keep the current level-9 fixtures as sensitivity controls until validated.
3. Reconcile equipment purchases with actual credit income and earlier expenses. Record medkit aliases, quantities, consumption, recovery/rest opportunities and remaining supplies.
4. Export the resulting session and derived combat stats. Add only test fixtures and assertions; do not repair production rewards or tune enemies as part of this audit.
5. Run suspect Foundry pairs and an authored multi-fight route without resetting HP/inventory between encounters. Retain isolated single-fight controls separately. Complete the existing recovery/status/save-boundary checks.

Escalate any confirmed reward-delivery defect with a minimal reproduction before changing production. A plausible route estimate must not be labeled an actual playthrough. Current evidence was collected headlessly; this batch adds no emulator route coverage.
