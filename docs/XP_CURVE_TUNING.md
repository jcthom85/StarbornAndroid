# XP progression pass

The previous 4,900 XP level cap was below the restored main-quest payouts.
The revised level-12 threshold is 26,000 XP. Levels 6 and 9 require 3,000 and
11,000 XP, placing skill tiers approximately in Worlds 2, 4 and 6 for Nova.

## Measured inputs

Quest amounts come from quests.json; battle amounts sum enemies.json XP rewards
for each winEncounter call in CampaignEventIntegrationTest. These are scripted
encounters, not a proof of the minimum reachable combat route. Incidental combat,
repeat battles and optional-quest battles are not estimated. The baseline below
excludes the script's extra 480 event XP and its 100 XP World 1 side quest.

| World end | Main quest XP this world | Scripted battle XP | Cumulative baseline | All side-quest XP added | Baseline level |
|---|---:|---:|---:|---:|---:|
| 1 | 675 | 655 | 1,330 | 1,830 | 4 |
| 2 | 1,250 | 540 | 3,120 | 4,270 | 6 |
| 3 | 2,600 | 750 | 6,470 | 8,770 | 7 |
| 4 | 4,600 | 600 | 11,670 | 15,770 | 9 |
| 5 | 6,200 | 1,830 | 19,700 | 26,100 | 10 |
| 6 | 9,600 | 5,320 | 34,620 | 44,370 | 12 |

Optional quest payouts alone can bring level 12 forward to World 5. This is an
intentional reward for completion, subject to playtesting. The curve is cumulative:
0, 100, 350, 900, 1,800, 3,000, 5,000, 7,500, 11,000, 15,000, 20,000, 26,000.

Existing saves keep earned levels and skills; exploration already preserves
levels, and combat now does too. Such saves may need more XP before their next
level. Following the companion progression pass, quest XP goes in full to each
current party member, matching combat's per-participant awards. New recruits
inherit the lead's XP/level through the existing recruitment behavior. Older
saves keep existing XP differences; this does not retroactively compensate past
quests. Item rewards remain separate work; enemy stats have not changed.
