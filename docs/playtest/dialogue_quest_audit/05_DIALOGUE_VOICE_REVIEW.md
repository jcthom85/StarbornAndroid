# Starborn dialogue and character voice review

This was an editorial read across all six worlds. Coverage included 534 dialogue entries, 210 authored responses, the spoken beats in the cinematic catalog, shop greetings and small talk, radio calls, and the rest dialogue in the exploration controller. I read quest and event conditions alongside scenes where availability or timing affected what a character could plausibly know. I checked all 408 authored continuation references for a valid target. Some entries appear to be prototypes or older greetings. The editorial read includes them, and their reachability remains unverified.

## Review by world

| World | Changes and scenes to check in play |
| --- | --- |
| 1 · The Mines | Jed’s repair dialogue talks Nova through the physical work, and Zeke stays focused on getting the hatch open under pressure. Boggs describes the required shield drill without suggesting that the optional cargo practice is a rescue. Check the workshop, the first talk with Boggs, and Jed’s lift sequence. |
| 2 · Sector 9 | Orion’s awakening has room for gratitude before the Bridge explanation. Gh0st’s decision to lower the rifle carries into the Anchor Drill, where Orion gives the crew immediate directions. Kaelen talks about his patrol through Leto’s cutter. Check the stasis reunion, the Beast aftermath, first departure, and the optional camp visits. |
| 3 · The Spire | Jax speaks as someone who knows how to work around patrols. The Curator and Sorrell answer the player in character through the wrong-answer branches. The heist retains the exact work-order clue. Wayfarer, the garden, and the hidden studio keep their personal references with shorter exchanges. Check the service cover, both Sorrell evidence questions, and the Wayfarer turn-in. |
| 4 · The Foundry | Nova gives Gh0st room to decide how to approach the Phantom records. His response to Rylos uses his name and follows the confrontation without repeating Rylos’s threat. Rylan and Cameron give concrete material requests. The Heat Liner dialogue describes the gear without promising environmental immunity. Check Project Phantom, Cooling Springs, and the Titan Dock approach. |
| 5 · The Ring | Charles explains why he’ll help through the departure records and who gets held back. His offer still names the public queue and both components. Orion and Zeke talk about Elara from what they’ve just seen, including before the Tear opens. Check Charles’s offer, status and turn-in, then the Anchor Chamber and Throne Room. |
| 6 · The Source | The crew’s optional lines describe what each member noticed in the memories. The campfire, the three refusals, Vale’s confrontation, and the diner give the crew different ways to show how they’ve changed. Check how the memory scenes sound in either rescue order and how the diner reads after the final battle. |

## Recurring voices

- **Nova** names the immediate job when things go wrong. Compare her asking for the cutter readout in the vault with her invitation for Gh0st to read the Phantom records together. Her words to Vale can then draw on what the crew actually did.
- **Jed** shows care by holding the housing, making Nova eat, and keeping the lift open. The bench taps and the lamp matter because they are ordinary things he does with her.
- **Zeke** notices how things work and gets practical when people need him. His dry humor comes out when there’s room for it. At the checkpoint, knowing Dominion’s procedures lets him buy Nova time. By the pod launch he’s working the panel beside her and telling her honestly what he can guarantee.
- **Orion** listens first. He hears pressure changes and remembers places through their sounds. His most personal lines admit what he misses or asks someone to stay.
- **Gh0st** starts with orders, then volunteers practical help and detail. His growing trust is audible when he takes watch, recognizes Elara’s cipher, and speaks up for the people behind the Foundry glass.
- **Thorne** uses the vocabulary of ownership, signatures, contracts and audit trails. She takes responsibility in the Ring through a recorded admission.
- **Vale** is calm and specific about the pain he thinks he can prevent. The forced note in the Throne Room and the repeated wrench tap in the Source let the player see what his offer costs.

## Verification

The copy edits preserve dialogue IDs, speaker assignments, branches, conditions and triggers. The fresh World 1, earned World 2, World 3 ledger, Worlds 4–6 completionist, and alternate World 6 finale route checks passed. Four older Hub 7–10 flow tests also fail when run against the previous APK’s dialogue, cinematic and shop copy. The focused verification run passed all 76 tests. On the Android emulator I loaded slot 23 and played the Archive Vault Lens scene through Nova’s revised line, Vale’s override, and the acquisition narration.


## Optional and repeat dialogue on Android

The emulator pass used isolated room and quest states with the game’s dialogue controller and touch interface. It sampled conversations in each world.

| World | Playback coverage |
| --- | --- |
| 1 | Pam’s family conversation, biscuit gift, goodbye, and return after the gift. |
| 2 | Kaelen’s meeting, patrol acknowledgment, repeat advice, and favor with partial and full supplies. |
| 3 | Evelyn and Robert’s topics, gifts, and goodbyes. Cheno’s parts handoff. Sorrell’s initial alibi, a wrong choice in each evidence round, exit and reentry, confession, and post-case response after saving and reloading. |
| 4 | Rylan’s Heat Liner exchange, followed by the Talia conversation after Lost Worker and a check that it cannot pay twice. |
| 5 | Charles’s motive, request, reminder, gate verification, materials handoff, reward, and completed response menu. |
| 6 | All three Memory Stair refusals, including leaving each choice open, reopening it, confirming it, and revisiting the completed interaction. |

Kaelen and Cheno now offer their turn-ins only once the full requested quantities are present. Rylan’s Talia topic now checks for completion of Lost Worker. The former milestone is no longer earned by the current route. This pass sampled Android playback and used automated touch input. The rest of the branches have source review coverage.
