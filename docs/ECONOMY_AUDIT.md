# Shop and crafting audit

## Transaction fixes

- Purchases verify current stocked/rotating items, canonical item aliases and
  current milestone state. Alias-keyed gates now apply to canonical item IDs.
- Purchase totals, inventory capacity and sale credit totals are checked using
  wide arithmetic before mutation. Invalid transactions preserve stock/credits.
- Sales consume current inventory with a checked result, rather than trusting
  the UI's cached inventory. Successful trades immediately synchronize session
  inventory for saves and downstream screens.
- Ingredient consumption combines aliases referring to the same item before
  checking availability. Nonpositive requirements are rejected without mutation.
- Cooking rejects nonpositive batches, overflowing ingredient requirements and
  output quantities that cannot fit, including the potential masterwork portion.
- Removed the mechanic's unreachable `MET_MECHANIC` basic-vest gate. That token
  had no setter anywhere in production assets/code. Other gates remain intact.

## Catalog findings

Ten shops were checked with production item-alias resolution. No profitable
direct buy/resell pair was found, including cross-shop sales and potential
rotation stock. This does not prove crafting-resale loops are absent or that
players can reach all shops at the same time.

Listed purchase-price ranges (credits; gates ignored for this comparison):

| Shop | Minimum | Maximum |
| --- | ---: | ---: |
| Mechanic | 60 | 288 |
| Weapons | 120 | 876 |
| Armor | 288 | 648 |
| Accessories | 336 | 528 |
| General store | 24 | 336 |
| Scrapper contraband | 28 | 168 |
| Tyson's provisions | 24 | 336 |
| Upper City lounge | 27 | 1,890 |
| Sentinel scraps | 14 | 41 |
| Arcade prizes | 50 | 1,000 |

No prices were changed. Affordability needs campaign credit income, mandatory
purchases and combat consumable usage measured together, not price ranges alone.

## Resolved recipe materials

Added catalog definitions and one-time room-entry salvage caches for the three
previously missing materials. Each cache supplies four units, covering two units
for the cabinet repair and two for the matching mod. Existing saves can collect
an unclaimed cache when revisiting its room; no quest replay is required.

| Material | Cache room | Affected recipes |
| --- | --- | --- |
| `pure_iron` | `foundry_service_airlock` (World 4) | `repair_slag_catcher_cabinet`, `mod_thermite_core` |
| `composite_plate` | `orbital_executive_dock` (World 5) | `repair_orbital_defense_cabinet`, `mod_orbital_deflector` |
| `astral_thread` | `source_echo_mines` (World 6) | `repair_harmonic_pulse_cabinet`, `mod_astral_weave` |

Removed the missing-ingredient allowlist: every recipe ingredient must now
resolve through the production item catalog. The salvage regression executes
the real room-entry events, restores session state, verifies one-time collection,
then crafts both recipes using CraftingService. Other ingredients are seeded;
this does not establish their acquisition routes or device interaction behavior.
The supplies are finite and sellable. Pickup text recommends retaining them;
selling them or making duplicate mods can leave too few for the cabinet repair.
No renewable farming source or additional shop stock was added in this pass.

## Remaining economy work

### Bounded closeout decision (2026-09-10)

Keep general shop prices, battle credit rewards and XP thresholds unchanged in
this pass. The evidence supports the specific transaction/recipe corrections
already made, not a global increase in income or reduction in prices.

- The reward-linked pre-Avatar route has 2,140 credits before any simulated
  spending. Four entry-tier weapons cost 1,800, leaving 340; stock/gates are
  checked, but shop navigation and prior spending are not. The combined legal
  five-AP/earned-gear/weapon build with a defined policy wins four of five seeds.
  This demonstrates one viable conditional budget, not full-route affordability.
- At a 60-credit medkit replacement price, 340 covers five medkits with 40 left.
  That does not also fund every ration, other upgrade or earlier purchase. Do
  not count the same cumulative credits separately for each checkpoint.
- The final checkpoint's 2,640 credits and nine AP are unspent in its diagnostic
  scenario. Rapid defeats therefore cannot establish an economic shortage.
  They remain a serious combat-readiness risk, with the second phase untested.

Acceptance: targeted economy correctness work is verified; global affordability
and finale balance are NOT approved. Remaining evidence gaps are continuous
spending/access, renewable gathering/fishing loops, optional-content budgets,
and device/player validation. These are explicit outstanding work, not silently
completed by the bounded closeout. Do not claim Phases 2-3 fully complete or
release readiness on the basis of test counts. The next verification pass checks
regressions; it cannot turn these unknowns into balance evidence.

### Crafting/resale follow-up

Found two deterministic shop-funded loops: one scrap (23 credits at Sentinel
Scraps) became Armor Plating Mk. I selling for up to 32; one herb plus one beast
meat (14 + 32) became Source Resin selling for up to 300. Recipes/tools/gates
may delay access but do not remove the repeatable margin once available.

Added an optional `resale_value` base before dealer markdown, preserving purchase
prices, equipment stats and ingredient costs. Armor Plating uses 40 (maximum
sale 20); Source Resin uses 80 (maximum sale 40). Items without an override retain
their previous pricing. Existing inventory uses the current catalog on load.

The crafting-chain regression starts with the cheapest shop inputs, repeatedly
relaxes costs through cooking and tinkering, and compares every reachable output
with every accepting dealer. Cooking uses 15% expected masterwork yield and
one-item batches; favorable individual rolls may profit even when long-run
expectation does not. It grants all shop/gate/tool/recipe access as a conservative
check. Non-purchasable fishing, gathering and finite-drop inputs remain outside
this shop-funded-loop test; profitable crafting using earned loot is not itself
an infinite-credit exploit.

### Affordability baseline (not balance acceptance)

The explicit `winEncounter` calls in CampaignEventIntegrationTest reference
enemies whose catalog credit rewards total the following, counted once per call:

| World | Scripted battle credits | Cumulative |
| --- | ---: | ---: |
| 1 | 330 | 330 |
| 2 | 260 | 590 |
| 3 | 480 | 1,070 |
| 4 | 400 | 1,470 |
| 5 | 1,170 | 2,640 |
| 6 | 0 | 2,640 |

The event-only ledger currently reports zero credit payouts on that scripted
route. World 1's 330 battle credits alone cannot cover both the mechanic's
288-credit basic vest and a 60-credit medkit (348 total). This flags a spending
constraint to measure, not a demonstrated progression blocker: the harness does
not simulate every encounter, gathering, loot sales or consumable usage, and the
vest is not established as mandatory. No general shop price or credit-reward
rebalance was made from this partial baseline.

Prior transaction-pass verification: all 323 JVM tests passed; asset integrity
and World 1 validators passed.
Material-source verification: all 324 JVM tests, asset integrity and World 1
validators passed. Device and lint checks were not rerun.
Craft/resale follow-up: all 326 JVM tests, asset integrity and World 1 validators
passed. Device and lint checks were not rerun.

- Extend beyond shop-funded crafting to renewable gathering/fishing inputs if
  their reward/time cadence suggests an exploit.
- Measure route earnings against gear upgrades and expected consumable spending.
- Verify shop and crafting interactions on-device. No device or lint rerun was
  included in this pass.
