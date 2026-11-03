# Android enemy cards, presence pills and Mira — November 3, 2026

## Enemy status cards

Enemy names use a readable sans-serif face at 13sp/16sp line height with two reserved lines. Exact HP values use 11sp/14sp, tabular digits and an explicit `HP current / maximum` label. The status card expands vertically as needed; exceptional values can wrap rather than shrinking into microscopic type. HP's 7dp bar precedes the thinner readiness bar; stability/break and weakness information remain intact. No combat values, targeting callbacks or combat rules changed.

A focused Android test checks long names and exact HP at font scales 1.0/1.3, including no text-layout overflow. The full combat formation test covers 24 combinations: 1/2/5 enemies, 1–4 party members, and short/tall layouts. Formation separation and sprite visibility pass. Existing whole-formation fitting still scales very crowded short-screen encounters; the source font sizes do not guarantee the same effective size after this fitting. This pass improves the card hierarchy/typeface without redesigning the battlefield layout.

Native examples: `evidence/android_card_and_mira_2026-11-03/cards-1.0.png`, `cards-1.3.png`, and full-scene short/tall captures in the same directory.

## Presence pills

NPC, item and service pills have a 100dp minimum width. Longer text still sizes naturally with icon and padding; no maximum widths or weighted text areas have been reintroduced. The earlier minimum-height/enlarged-font behavior remains. Small names such as Jed no longer produce cramped pills. Quantity badges remain compact badges, not 100dp controls.

The normal/enlarged-font sizing tests now check `max(content width, 100dp)`, so they reject both collapse and unnecessary excess width. A third test checks Jed/Mira, normal Talk callbacks, and the real-merchant caption.

## Mira decision and cause

Mira had a `shop` interaction pointing at `mechanic_shop`, but lacked `trade_after_dialogue`. The commerce flow only offers a shop after normal dialogue for explicitly opted-in NPCs. Separately, Android's pill guessed vendor status from words such as `mechanic` in an NPC ID. Together these produced a Shop caption without an accessible shop.

The dormant stock was medkit, pulse grenade and basic vest at 1.2 sale markup / 0.35 buy markdown. It overlapped existing World 1 shops; notably its ungated medkit would bypass the General Store's `ms_w1_mq03_complete` medkit gate and undercut Scrapper's markup. The user explicitly chose to keep Mira as a talk/quest NPC rather than invent another merchant role.

Removed only Mira's unused Shop interaction. Her Talk interaction, authored dialogue, role, portrait and quest definitions/triggers remain unchanged. The unreferenced `mechanic_shop` catalog definition was not enabled or repriced; this decision removes the NPC assignment rather than deleting unrelated catalog data.

Pill captions now use controller-provided `npcTradeKeys`, derived from the existing opted-in merchant map and valid shop definitions. The ID-word heuristic is removed. Mira uses normal Talk presentation; real merchants retain the usual dialogue-first trade offer. No custom Shop/Talk buttons or special Mira screen were introduced.

## Verification

- 783 Android host tests passed, zero failures/errors/skips, including earned campaign routes.
- Six native Android tests passed: card typography, three pill checks, lighting, and the 24-case combat formation matrix.
- Six targeted desktop merchant tests passed after adding the Mira regression. The new test checks Talk-only content, absence of Mira trade metadata, real Scrapper metadata, completing Mira dialogue, and no spurious shop greeting. Existing tests retain Scrapper's quest/reward-before-trade behavior. Room-entry tests also passed in the broader build.
- Debug/test APK assembly, Android lint, desktop compilation and asset parity pass. A final compile/native run includes test screenshot additions made after the broad lint run.
- All ten earned World 1 checkpoints refreshed and verified against current source JSON.

Logs and examples: `evidence/android_card_and_mira_2026-11-03/`. This pass has not been installed on the physical Pixel or committed/pushed. It includes the lighting motion rework described in [its report](../design/environmental-effects/LIGHTING_MOTION_REWORK_2026-11-03.md).
