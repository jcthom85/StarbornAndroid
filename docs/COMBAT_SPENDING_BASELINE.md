# Reproducible combat spending baseline

The production CombatViewModel and enemy AI now accept CombatRandom, defaulting
to the unchanged DefaultCombatRandom for gameplay. Tests supply a seeded source
shared by action resolution, AI tie-breaking, target selection and loot rolls.
Presentation UUIDs are not part of measured-result comparisons.

## Method and limits

- Opening: solo level-1 Nova versus Faulted Loader, full HP, no optional gear.
- World 4: level-9 Nova/Zeke/Orion/Gh0st versus Titan Walker, full HP, no gear.
  This is an estimated progression fixture, not a campaign-earned save.
- Both receive three medkits explicitly supplied by the test, not campaign rewards.
- Every 250 ms: finish pending lunge/miss presentation callbacks, select ready
  actors in roster order (the last ready selection wins), heal the lowest HP-ratio
  living ally below 40% HP if a medkit remains; otherwise basic-attack the first
  living enemy. No skill, snack, guard or equipment strategy is used.
- Seeds 1–5 run twice each. Outcome, item consumption, credits, duration and final
  party HP must match. A 240-second timeout is a failure, not a zero-cost result.
- UI animation completion is supplied by the harness; reported virtual durations
  are not device timings. This is a deliberately simple policy, not player skill
  or statistical win-rate evidence.

## Observed results

| Fixture | Seeds | Outcomes | Medkits consumed | Replacement cost at mechanic |
| --- | --- | --- | --- | --- |
| Loader | 1–5 | Five victories | 0 each | 0 |
| Titan Walker, ungeared | 1 | Defeat | 0 | 0 |
| Titan Walker, ungeared | 2–5 | Four defeats | 3 each | 180 each |

Loader runs end with Nova at 83–108 HP (120 maximum), after 58–75.5 virtual
seconds. The loader pays zero credits. Titan runs end in defeat after
55.25–91.5 seconds and therefore pay no victory credits; its catalog victory
reward is 400 credits, which must not be counted as income on a failed attempt.

Against the earlier partial route baseline, 180 credits is about 12% of the
1,470 scripted battle credits through World 4, before purchases. Actual income
also depends on extra encounters and sales; actual spending depends on gear,
skills, free healing, supplied consumables and retry/save behavior.

## Decision

No price, enemy stat or credit-reward adjustment follows from these results.
Next: use attainable equipped loadouts and a skill-aware policy, then carry
health/inventory across a sequence rather than resetting every encounter. The
current results do not establish realistic campaign affordability.

Verification: all 327 JVM tests passed. Device, lint and asset validators were
not rerun in this runtime/test pass; no game assets were edited in this pass.

## Budget-equipped follow-up (2026-09-10)

Added a constructed sequence using the level-9 party, Nova's Laser Blaster and
Zeke's Shock Fists (432 + 456 credits), plus three medkits (180). Total 1,068
leaves two credits from the partial 1,070-credit through-World-3 baseline. No
earlier spending is assumed; this is not proof that a typical player can afford
this loadout. No armor, accessories or snacks are supplied.

The policy uses each character's starting offensive skill when available, then
basic attacks, retaining the existing medkit threshold. It is skill-aware but
does not exercise the full level-9 skill set or support strategy. The test checks
that the two weapons actually map into combat and that incoming saved HP is
honored. Victory HP, credits and medkit inventory are checked before the next
encounter. No replenishment occurs between encounters.

The calibration Loader fight wins in all five seeds using four skills and no
medkits. It leaves everyone at full HP, so it tests the handoff wiring but not
depleted-health recovery between victories. The following Slag Golem fight loses
in every seed, consuming 3/0/0/3/1 medkits respectively and executing 17/7/8/16/9
skills. The planned Titan Walker leg is never reached. Each sequence is repeated
with the same seed and must produce identical results.

This is not an authored campaign encounter sequence. Its result warrants checking
fixture progression/stat mapping, defensive equipment and support abilities before
making balance changes. Multi-victory attrition and attainable full loadouts remain
open. All 328 JVM tests passed; no device/lint run or gameplay asset tuning here.

## Defensive policy and animation fix (2026-09-10)

Confirmed that production combat uses base character stats plus equipment/buffs;
levels currently unlock skills but do not apply a per-level stat-growth curve.
No new growth rule was invented. The defensive fixture swaps Nova's 432-credit
Laser Blaster for her 432-credit Flux Liner, retaining Zeke's Shock Fists and
three medkits. Runtime assertions verify armor damage reduction and equipped
weapon mapping. Total fixture expenditure remains 1,068 credits.

The policy adds Zeke's Bulwark Stance when Guard is absent and Orion's Nano Repair
below 80% HP when Regen is absent. Emergency medkits take priority while available;
other turns use starting offensive skills/basic attacks. This is a bundled
equipment/policy comparison, not an isolated estimate of either intervention.

It exposed a real animation pause leak: replacing a lunge token added another
pause, but only the newest completion could release one. Attack and miss slots
now each own at most one pause. Replaced delayed introductions are cancelled;
stale and duplicate completions cannot release another animation's pause. A
focused regression covers overlapping attack/miss tokens and delayed replacement.

After the fix, every seeded scenario terminates and repeats identically. All
defensive seeds win the calibration fight. Seeds 2 and 5 also beat Slag Golem,
spending three medkits and earning 130 credits, then lose to Titan Walker without
medkits. Seeds 1/3/4 lose to Slag Golem after spending all three. The successful
Golem encounters exercise genuinely depleted resource handoff; fallen companions
enter the next encounter at 1 HP under the existing production persistence rule.

Replacing three medkits costs 180 at the mechanic, exceeding the Golem's 130-credit
reward by 50 before loot sales. This flags recovery pressure in this constructed
sequence, not a demonstrated campaign softlock. Next inspect actual recovery
opportunities/loot sales and full earned loadouts before tuning prices or enemies.
No gameplay asset prices/stats changed in this pass; device verification remains.
Verification: all 329 JVM tests passed. Lint and device checks were not rerun.

## Recovery audit and HP correction (2026-09-10)

Found a recovery correctness bug: exploration rest used raw character HP, while
combat added vitality, equipment, passives and meal HP. An unequipped Nova could
rest from 99 down to 60 despite having a 120 combat maximum. Rest and combat now
share PartyHealth for this ceiling. Regression coverage checks raw vitality,
armor HP and repeated rest; no new level-based growth rule was introduced.

World 4 has rest actions in the Heat Shadow Alcove and Forge Control Alcove.
The latter is connected directly west of Forge Anvil, where Titan Walker is
encountered. Their rest events also grant one painkiller and are nonrepeatable;
after their first use, retriggering the event does not heal. That existing rule
was not changed: repeatable healing would need separation from one-time supplies.
Cooking stations are separate actions, not automatic healing.

Slag Golem pays 130 credits and drops one scrap with probability 0.8 and one
power cell with probability 0.35. At the highest 50% catalog markdown, those sell
for 10 and 30 respectively: 18.5 expected credits, or 40 if both drop. Thus even
the optimistic sale total is 170, below three replacement medkits at 180; expected
total is 148.5. Dealer access and preserving crafting materials can reduce actual
sales. This does not include other room loot, camp supplies or quest rewards.

Conclusion: the earlier no-rest sequence omits a relevant authored recovery
opportunity and was also affected by incorrect rest HP. Do not rebalance the boss
from it. Next compare a first-use camp recovery between encounters and decide
whether repeatable healing (without repeatable supplies) is intended.

Recovery-fix verification: all 330 JVM tests passed. Device and lint checks
were not rerun; no camp rewards or repeatability settings changed.

## First-use camp comparison

The defensive sequence now also runs with `w4_camp_forge_alcove` after a Slag
Golem victory. EventManager executes the authored event with a PartyHealth-based
rest hook and inventory grant hook. The scenario verifies the painkiller grant
and unchanged session on immediate replay. It uses the same healing calculation
as exploration, but does not navigate the actual UI to the camp.

Seeds 2 and 5 reach the camp and then Titan Walker at full HP. Both still lose:
65.25 and 68 virtual seconds, compared with 47 and 53.75 without camp. The other
three seeds lose before reaching camp. The granted painkiller remains in inventory;
the unchanged measurement policy only consumes medkits, not all available items.
No shop visit or additional gear purchase is simulated. Same-seed sequences are
repeated and compared exactly; all 330 JVM tests pass.

This closes the first-use healing comparison, not balance acceptance. Next use
the full earned skill/item choices and verify a representative winning loadout
before deciding whether encounter or economy tuning is warranted. Camp rewards,
repeatability, prices and enemy stats were not changed in this comparison.

## Expanded earned offense and camp item policy

Added a separately reported expanded policy to the same defensive budget fixture:
prioritize unlocked Overload Fists and System Crash, retain Arc Tether, and use
Venom Edge/starting attacks as cooldown alternatives. Avoid Plasma Burst's burn
against the Foundry enemies' burn resistance. The policy can use the camp's
Neural Stabilizer (`painkillers`) on the acting character below 70% HP when an
emergency medkit does not take priority. Item target semantics remain self-only.
This is broader earned offense, not exhaustive use of every unlocked skill.

Seeds 2–5 defeat Slag Golem; seed 1 loses. Golem medkit use is respectively
2 (defeat), 3, 1, 1, 0. All four survivors rest and then lose to Titan Walker;
each uses the camp item, and Titan medkit use is 0/2/2/1. These are paired
policy scenarios, not measured player win rates or independent random samples.
Identical seeds repeat exactly. All 330 JVM tests pass.

The conclusion is narrower than a global supply shortage: Golem can be cleared
with low or no medkit use under this policy, while Titan remains uncompleted by
the two-piece gear fixture. Next audit actual earned equipment/passive unlocks
and the boss's counterplay, then select a representative full loadout. Do not
buff credits or nerf the entire Foundry based on this partial loadout. No gameplay
prices, stats, rewards or skill definitions changed in this test-only pass.

## Titan counterplay correction

Found two implementation gaps behind the advertised cooling-vent tactic:
Vent Heat applied opponent-targeted Exposed, and the processor did not consume
the authored defense_multiplier field. Vent Heat now applies a separate
Radiators Exposed status to its caster for two status turns. Titan's AI selects
it on the following turn after Missile Barrage or Titan Stomp when usable, respecting
skill blocking. Its cooldown is zero so cooling is available after either heavy
attack. Only Titan owns this skill in the current enemy catalog.

Physical base damage now applies status defense_multiplier to vitality-derived
defense. This also activates the existing generic Exposed definition; it does
not remove flat equipment damage reduction or elemental resistances. Regression
checks cover both heavy-attack transitions, self-only status application,
surviving status duration after processing, and higher physical damage with
identical random rolls. Missile Barrage's existing offensive Exposed effect is
unchanged; it is distinct from self-exposure while cooling.

Equipment audit correction: w1_mq01_patch_flux_liner awards Nova's Flux Liner
for free. The defensive fixture's purchase budget overcharges by 432 credits
if the player kept that quest reward. Its combat equipment remains valid, but
the claimed two-credit remainder is not an accurate earned-inventory budget.
Reconcile additional quest equipment and chosen passive unlocks before calling
this a representative full World 4 loadout. No global boss HP/damage or price
rebalance was made; cooling behavior is an explicit counterplay correction.

Post-correction verification: all 332 JVM tests, asset integrity, and World 1
asset validation pass. In the expanded defensive/camp sequence, seed 1 still
loses to Slag Golem. Seeds 2-5 now defeat Titan, using respectively 0/1/2/1
medkits during that fight and the camp item once each. Titan durations are
99.75/65.75/65.25/56.25 virtual seconds. Same-seed repeats match exactly.
These constructed scenarios demonstrate usable counterplay, not a player win
rate or full campaign affordability sign-off. Earlier Titan losses above are
historical pre-correction results.

## Earned-equipment budget reconciliation

The defensive fixture now retains the opening Flux Liner reward rather than
buying a replacement. A catalog assertion checks that the named patch event
still grants one. Purchases are Zeke's Shock Fists (456) and three medkits (180):
636 credits total, leaving 434 of the assumed 1,070-credit scripted baseline.
The offensive comparison still spends 1,068 and retains two credits. Equipment,
items used by the combat policy, and enemy stats are unchanged; spare credits
are not converted into extra supplies during the sequence. This corrects the
earlier two-credit defensive remainder, not the assumption of no prior spending.

Direct top-level item grants in World 1-3 events also identify optional loadout
candidates: Recoil Dampener (w1_sq05_read_datapad), Neon Band
(w3_sq13_restore_market), Corrosive Rounds (w2_sq02_recover_transceiver),
Focus Conduit (w2_sq04_complete or w1_loot_server_backup_cases), Digital Watch
(w1_loot_checkpoint_cell), Precision Sight (w1_loot_workshop_loft), Biolum Focus
(w2_relic_biolum_solved), Tachyon Core (w3_relic_chrono_solved), and Phase Rounds
from the World 3 ledger/telemetry branches. This is a candidate-source inventory,
not an exhaustive nested-action/dialogue audit or proof of route reachability.
Do not grant all these items to a mandatory-path fixture automatically.

Chosen passive nodes are not free level bonuses: production purchase logic
checks available shared player AP, prerequisites/milestones, and row investment
(five AP per row), then spends AP and unlocks the node. The current fixture adds
no discretionary purchased nodes. Remaining work is to derive an earned AP
ledger, choose legal purchases through those checks, verify optional item routes
and slot compatibility, and build a separate equipped comparison. Level-earned
skills already in the fixture must remain distinct from new AP purchases.

Verification for this test/documentation-only follow-up: all 10 tests in
OpeningCombatRuntimeTest pass, including repeated seeded encounter sequences;
git diff --check passes. The full suite, validators, lint and device checks were
not rerun in this pass. No production code or gameplay assets changed.

## Two-AP passive comparison

The explicit main-route bosses before World 4 award 0/0/2 AP (Iron Warden,
Beast, Administrator). AP is one shared balance, not two points per character.
Only Administrator, Titan, Compliance Avatar, Ascended Vale and Ascended God
have authored enemy AP rewards (2/3/4/5/8). No explicit AP fields were found in
the event/dialogue assets searched. This is a boss-catalog baseline, not a
complete earned save or a claim about repeatable encounters.

Added a separate five-seed, twice-repeated defensive/camp/expanded comparison
that spends the two points on Nova's Quiet Steps and Zeke's Training Session.
Each purchase passes production prerequisite/tier/AP evaluation and deducts
from the same session balance; runtime assertions check +5 speed and +5 strength
against otherwise identical characters. Optional equipment is still excluded.

This exposed missing directory listing in DesktopAssetProvider: JVM fixtures
were loading no skill-tree definitions. Development-directory enumeration now
loads them; packaged JAR enumeration remains unsupported. Android's asset
provider is unchanged. Earlier combat results are historical because loading
the trees also activates authored passive effects associated with already
unlocked IDs. The encounter ledger now includes awarded medkits as well as
consumption when checking inventory conservation.

The new comparison also caught a production persistence defect: victory only
copied inventory into the session when drops were nonempty. In seed 3's Slag
Golem victory, one medkit was consumed but a no-loot roll left all three in the
session. Victory now always snapshots inventory after applying drops, including
empty drops. This preserves consumption independently of loot luck; the seeded
chain's conservation assertion covers the failing case.

Final verification: all 332 JVM tests and both asset checks pass; diff whitespace
checks pass. All five two-AP chains clear Golem and Titan, repeated identically.
Total medkits consumed per chain: 3/2/3/3/2, plus one camp item each. Titan times:
55.75/60.25/65.75/57/55.5 virtual seconds. This demonstrates a viable constructed
loadout without adding optional gear or spending the remaining 434 credits; it
does not prove an entire campaign route, player win rate, or global balance.
Lint and device checks were not rerun.

## Optional workshop accessory acquisition

Added a focused Precision Sight acquisition-to-combat regression. It starts
inside workshop_floor after the loader gate, verifies the ungated west link to
workshop_loft and the prose-visible mod bench's explicit action reference, then
executes the actual loot event with its unmodified conditions. Item/milestone
hooks update the session. Recreating the event manager after an in-memory state
restore cannot grant the item again.

The earned inventory is carried into a seeded World 4 party; the authored item
is an accessory despite the bench's mod terminology. Equipping it in Nova's
scoped accessory slot adds 8 accuracy and 4 crit bonus in production combat
mapping, while Zeke's stats remain unchanged. This checks a local route edge,
event acquisition and owner-scoped stats, not touchscreen equipment selection,
disk persistence, a continuous campaign route, or changed encounter outcomes.

Verification: full JVM suite passes (333 tests) and diff whitespace checks pass.
This follow-up changes tests/documentation only; asset validators, lint and
device checks were not rerun.

## Earned accessory consecutive-encounter comparison

Reused the locally verified workshop loot-event helper in a separate optional
Precision Sight variant of the two-AP defensive/camp/expanded chain. Its actual
item grants are merged into inventory, the loot milestone is retained, and the
accessory is equipped only on Nova. Credits are unchanged. Victory checks retain
the item and slot while checking HP, credit and consumable carry-forward.

All five seeds clear Golem and Titan with identical repeat runs. Total medkits
remain 3/2/3/3/2 plus one camp item each, exactly as without the accessory. Seed
3's Golem fight is one virtual second shorter; no supply savings are demonstrated.
This skill-heavy policy does not establish the accessory's value for other
playstyles. No tuning is justified by this comparison alone. The local loot
route is exercised, but the intervening campaign and initial budget remain
constructed. Full suite: 333 passing tests; no gameplay assets/code changed.

## Player group-support behavior

Added production ATB/command scenarios for Guardian Covenant and Link, invoking
useSkill without explicit targets. Three living party members must receive
Shield or Regen respectively; Link must also increase each living member's HP.
The fallen fourth member is neither revived nor given the tested status, enemies
receive no healing/status benefit, and the caster enters cooldown. These checks
pass without production changes. Unlocks and injuries are seeded to isolate
behavior, not claimed as earned at the World 4 fixture. Enemy group-support and
campaign acquisition of these abilities remain separate coverage questions.

Verification: all 334 JVM tests pass. This pass changes tests/documentation only;
asset validators, lint and device checks were not rerun.

## Event-earned pre-World-4 checkpoint

## World 5 reward-derived boss baseline

## World 6 bounded checkpoint pass

Captured the reward-linked campaign before Ascended Vale, round-tripped through
protobuf disk storage, and excluded the final Tune World unlock. Checkpoint:
29,800 XP, level 12, 9 AP, 2,640 credits. Equip retained Flux Liner/Heat Liner and
Grav-Boots; no purchased weapons or AP allocation. Prior route attrition remains
unmodeled. A single defined policy carries supplies/HP to God only if Vale wins.

All five runs lose to Vale (2.5/1.5/1.5/1.5/1.5 virtual seconds), repeated exactly.
Only seed 1 executes a skill; no supplies are consumed. God is never exercised.
Vale's catalog includes AoE Silence Wave at 175 focus-scaled base power; this is
a candidate for targeted damage-trace inspection, not a proven sole cause.
Record this as an unresolved finale balance/coverage risk. This bounded pass is
performed, but the finale is not signed off. Full suite: 337 passing; no production
changes, validators/lint/device not rerun. Next: explicit economy/acceptance
decision and final verification rather than unbounded build variants.

Bounded closeout: model purchase of Laser Blaster, Shock Fists, Prism Focus and
Whisperblade from weapon_shop. Catalog stock/gates and prices are checked:
432 + 456 + 432 + 480 = 1,800, leaving 340 credits. Retain earned armor/accessory
and the five-AP allocation. This models the transaction, not shop UI/navigation
or prior campaign spending. The final defined policy prioritizes earned Cryo
Vent/Hydraulic Kick, avoids prioritizing System Crash into Source resistance,
and permits Link recovery for an injured party. Other healing rules persist.

Seeds 1-4 win, seed 5 loses; repeated seeds match. Victory times are
42.75/36.25/56.25/54 virtual seconds; seed 5 loses at 55.75. Some victories leave
fallen members. Ordinary medkits consumed: 1/1/1/0/1; rations: 4/2/2/7/1;
Medkit I: 0/1/1/0/0. This establishes a viable bounded build/policy, not optimal
balance or guaranteed success. No boss tuning made. Avatar investigation closes
with these limits; next is the World 6 pass, not further Avatar variants.
All 337 JVM tests pass; no production changes or validators/lint/device rerun.

Five-AP follow-up: with earned armor/accessory retained, spend the actual five
AP on Quiet Steps, Night Cloak, Training Session, Motivational Speech and
Budgeting. Each passes production tier/prerequisite/balance evaluation against
saved milestones, spending the shared balance to zero without changing credits
or inventory. Five seeds repeated identically still lose in
47.5/54.25/55.5/40/44.75 virtual seconds. This allocation alone does not resolve
the inherited policy/minimal-offense problem; it is not proof that all legal
allocations fail. Shop-equipped offense and policy suitability remain next.
All 337 JVM tests pass; tests/docs only, no validators/lint/device rerun.

Action-trace correction: the seed-one log showed Ration Pack healing only its
explicitly selected actor. resolveTargetsForItem allowed that selection to
override party support scope. Party-targeted support items now ignore the
single selection and resolve living allies. Regression verifies exactly one
consumption, 35 HP restoration to injured allies, max-HP clamping, no revival,
and no enemy benefit. Single-target item behavior is unchanged.

All 337 JVM tests and both asset checks pass after the fix; diff whitespace
checks pass. Earlier ration-use comparisons below are historical pre-fix
measurements. The updated earned-gear Avatar seeds still all lose, in
47/52.5/43/50/64.5 virtual seconds. No boss stats/prices changed. The trace also
shows Avatar's heavy physical hits and low party damage; AP/shop allocation
and policy suitability still need evaluation. Lint/device checks not rerun.

Earned-gear comparison: retained Heat Liner is equipped on Orion and Grav-Boots
on Gh0st, with Nova's Flux Liner unchanged. Test checks ownership, authored slot,
unchanged inventory/credits and identical seeded replays. All five lose in
47/42/35.5/39.5/52 virtual seconds. No AP or credits are spent, so this is not
a representative fully allocated build or an affordability verdict.

Counterplay inspection: Avatar has Missile Barrage, Seismic Stomp and Roar of
the Source; resistance favors neither the inherited Source-heavy policy nor
Shock (35 and 10 respectively). The description mentions Purge Mode, but searches
of combat code found no named Avatar/Purge implementation. This is a description/
mechanics question to review, not authorization to invent a phase. Next inspect
actual damage/action traces and legal resource allocation. All 336 JVM tests
pass; tests/docs only, no gameplay change or validators/lint/device rerun.

Extended the supplied-victory campaign through World 5, capturing and disk-
restoring the session before Compliance Avatar's reward. It has 17,930 XP,
level 10, 5 shared AP and 2,140 credits. The AP assertion excludes Avatar's own
four-point reward. The earlier Titan combat experiments do not mutate this
campaign branch: its earlier victories still supply rewards without attrition.

Five seeded, twice-replayed Avatar fights all lose with the inherited skill/
owned-supply policy and only Nova's retained Flux Liner equipped. Durations are
49.75/35.75/35.75/35.75/39.5 virtual seconds. Supplies remain unused at defeat;
the five AP and credits were not spent. This identifies a counterplay/loadout
investigation, not a global difficulty or income defect. Next inspect Avatar's
mechanics and use attainable equipment/skills before any tuning. Full suite:
336 tests pass; no production changes or validators/lint/device rerun.

Earned-AP follow-up: a separate comparison spends the saved two shared AP on
Nova Quiet Steps and Zeke Training Session. Each purchase passes production
prerequisite/tier/AP checks with actual saved milestones; session spending leaves
zero AP, unchanged credits and unchanged inventory before combat. No AP is
granted. Owned supplies participate as in the previous policy.

All five seeds clear Golem/Titan with or without camp, repeated identically.
With camp, total rations are 1/5/2/1/7 and Medkit I use 0/0/0/4/1, plus one
ordinary medkit and one camp item each. This is not uniformly more efficient
than leaving AP unspent: seed 4 uses more Medkit I and seed 5 more rations.
Without camp, seed 3 still spends all ten rations and takes 146 virtual seconds
on Titan. The comparison establishes a legal viable allocation, not an optimal
build or broad balance acceptance. No gameplay changes were needed.

Verification: all 336 JVM tests and diff whitespace checks pass. This pass
changes tests/documentation only; validators, lint and device checks not rerun.

Owned-supply follow-up: added a separately reported policy using the snapshot's
five Medkit I and ten Ration Packs. These are distinct catalog IDs, not aliases
of the ordinary medkit. Medkit I is an emergency fallback after ordinary medkits;
party rations take priority when at least two living members lack 35 HP. Actual
consumption is counted per item and checked against persisted victory inventory.
No supplies are granted and credits/AP remain unspent.

All five seeds now beat Golem; four clear Titan without camp and all five with
camp. With camp, total ration use is 3/4/3/0/4 and Medkit I use 0/1/5/2/0, plus
one ordinary medkit and one camp item in each complete sequence. Seed 3 uses all
five Medkit I and takes 167.25 virtual seconds on Golem: success is not evidence
of efficient pacing. Without camp, seeds 2 and 5 exhaust all ten rations; seed 3
loses to Titan. Every seed repeats exactly. These results supersede the limited
policy's outcome counts only for this new policy, not globally. All 336 tests
pass; test/documentation changes only, no validators/lint/device rerun.

Attrition follow-up: the same saved level-eight snapshot now also starts a
constructed Slag Golem then Titan sequence, paired with/without Forge camp.
Victory assertions check persisted HP, medkit consumption/loot and credits.
Camp executes the authored one-time event with production-equivalent rest/item
hooks and immediate replay protection. It does not navigate to camp in the UI.

Seeds 1/2/4/5 defeat Golem; seed 3 loses before camp. Without camp, seeds 1/4/5
also defeat Titan; with camp all four Golem survivors defeat Titan. Each complete
sequence consumes one medkit; camp variants additionally consume the granted
item. Seed 5's Titan fight falls from 174.5 to 62.25 virtual seconds with camp.
All paired seeds repeat exactly. No player win-rate inference is warranted.

This inserts Golem after the pre-Titan snapshot as a stress test, not proof of
chronological navigation or rewards for every actual route encounter. Previous
campaign attrition remains absent. Policy leaves credits/AP and some inventory
items unused, so failures do not establish supply shortage or justify tuning.
All 336 JVM tests pass; test/documentation only, no validators/lint/device rerun.

Saved-checkpoint combat follow-up: the disk-restored level-eight pre-Titan
snapshot now feeds the existing production ATB combat runner, retaining its
actual inventory and unlocks. Nova equips one owned Flux Liner; no weapons,
accessories, XP, supplies or purchased passives are added. Five seeds replay
identically and all defeat Titan, consuming 0/1/1/1/1 medkits in
62.25/60.25/62.25/65/62.25 virtual seconds. Seed 4 ends with Gh0st fallen.

This uses the existing skill-aware defensive/expanded policy, not optimal use
of every story skill or inventory alias. Prior supplied victories did not
simulate HP loss or consumption, so combat starts without campaign attrition.
It proves a saved reward-derived checkpoint can execute and win this boss under
the policy, not full-route affordability or a player win rate. All 336 tests
pass. No production changes; validators, lint and device checks were not rerun.

Pre-Titan follow-up: extended the reward-linked route through World 4 and captured
the session immediately before paying Titan's supplied victory. A protobuf disk
round trip preserves the state: 10,450 XP, level 8, 2 AP, 1,070 credits. All
level-earned unlocks are checked; Overload Fists is absent. The route is 550 XP
short of level 9 and includes story skills such as Nova Link and Hydraulic Kick
that the older constructed fixture omitted. Earlier level-nine victories remain
limited to that fixture, not proof of this route's readiness. Optional/unsimulated
battles are excluded. Next use this snapshot for combat and spending rather than
silently inserting extra XP. Full suite passed before adding exact checkpoint
assertions; focused verification is repeated for those assertions.

Follow-up: a separate continuous World 1-3 test now invokes production
CombatViewModel victory reward generation/payment for each supplied encounter
victory, using the encounter room and a fixed loot seed of 17 per battle. It
uses the production item repository and saves/restores the complete session
between worlds. The original event-only route and report are unchanged.

Result: 7,050 XP (5,105 event plus 1,945 battle), level 7, 2 shared AP and 1,070
credits. Assertions reconcile actual reward sums and restored party progression.
This confirms the constructed budget's credit/AP baseline, not its later level-9
boss timing. Next extend to the World 4 pre-boss point. Victories, navigation and
craft success remain supplied; no combat spending or shop purchases are simulated.
Loot is deterministic for this seed, not an expected income estimate. Production
reward methods are invoked reflectively to avoid adding a test-only public API.

CampaignEventIntegrationTest now reports restored inventory and shared AP for
every world. A separate continuous World 1-3 regression saves/restores at each
boundary, equips retained Flux Liner, and checks a real protobuf disk round trip
preserves the whole equipped session without reducing inventory or credits.
The route contains two liners: the starter-kit reward bundle and patch action
each grant one. This is authored duplication, not evidence of event replay; no
reward was removed. Opening intent and any resale implications remain to review.

The event-only checkpoint has 5,105 XP, level 7, zero credits and zero AP. It must
not be relabeled as the constructed level-9 combat checkpoint. Battle XP/AP,
credits, loot, real crafting costs, purchases and consumption are still excluded.
The harness now also forwards reward-bundle AP when present, matching exploration.
This advances saved reward provenance, not full earned-campaign combat testing.

Verification: all 335 JVM tests pass. Test/report changes only; no gameplay
rewards changed, and asset validators, lint and device checks were not rerun.
