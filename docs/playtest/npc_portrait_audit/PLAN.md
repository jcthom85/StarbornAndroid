# NPC portrait production plan

Audited November 16, 2026. This document records the approved production plan. Execution and validation are recorded in [IMPLEMENTATION.md](IMPLEMENTATION.md).

## Recommendation

Treat the portraits as a cast with a shared rendering style and distinct identities. The current problem includes duplicated people, mismatched roles, missing speaker registrations, and inconsistent visual styles—not just missing images.

The production roster contains **47 non-party portrait identities: 40 proposed new portraits/redraws and seven provisional keeps**. It combines 43 non-party NPCs placed in playable rooms, Rylos's scripted dialogue, and three additional shop/device faces. This is a source-and-asset audit, not proof that every conditional appearance has been exercised on a device.

| Group | New or redrawn | Retain | Total |
| --- | ---: | ---: | ---: |
| World 1 | 10 | 2 | 12 |
| World 2 | 4 | 2 | 6 |
| World 3, including Concierge | 15 | 2 | 17 |
| World 4, including Rylos | 4 | 0 | 4 |
| World 5, including commissary | 5 | 1 | 6 |
| Astra / arcade services | 2 | 0 | 2 |
| **Total** | **40** | **7** | **47** |

World 6 reuses established identities and memories; it does not need another set of faces for the same people. Thorne and other recurring characters are counted once, regardless of world or transmission.

Provisional keeps: **Mira, Mika, Shopkeep, Sentinel-3, Maintenance Bot, Penny, Hacked Lab Terminal**. Compare these with the first finished batch before calling the overall set cohesive.

See [the complete production roster](production_roster.md) for every identity and proposed visual direction, and [the inventory](inventory.json) for current paths and room associations.

## Protected references and honest style assessment

Nova, Zeke, Orion and Gh0st remain unchanged, including existing expressions and Zeke's NPC portrait copy. They are the rendering references: strong silhouettes, dark outlines, controlled bright colors, clean cel shading and readable faces. Their appeal and small-screen clarity are stronger than much of the current NPC art.

Their large heads, eyes and broad expressions can skew young or cheerful when copied indiscriminately. Match their rendering language without giving every adult the same youthful face, smile, proportions or pose. Older characters need visible age; authority should read through posture and restraint; grief and guardedness need quieter expressions. Avoid painterly workers beside ornate fantasy figures beside cartoon heroes.

Base portraits should prioritize the face and upper-body silhouette. Limit occupational cues to one or two; avoid tiny tools, excessive costume detail and hands competing with the expression. Use deliberate variation in age, facial structure, hair mass, build, posture and palette. Human, animal and machine portraits should feel equally intentional.

The proposed directions are art briefs, not new canonical biography. Check authored descriptions before each final prompt. Real-life-reference characters do not imply a requirement for photographic likeness.

## Shared portraits that need separating

| Current shared image | Current identities affected |
| --- | --- |
| `elara.webp` | Elara, Evelyn, Selene |
| `vale.webp` | Vale, Scout Kaelen |
| `curator.webp` | Museum Curator, Charles, Robert |
| `doc.webp` | Doc, Megan, Dr. Lyra |
| `vance.webp` | Rourke, Chris, David, Ren, Jason, Director Sorrell |
| `orlo.webp` | Orlo, Andrew, Dax |
| `scrapper.webp` | Scrapper, Brutus |
| `armorer.webp` | Cheno, Cameron |
| `maintenance_bot.webp` | Maintenance Bot, Caretaker Echo, Zenith Automated Commissary |
| `orion_portrait.png` | Oliver, Meeko; also dormant Charlie references |

These ten groups affect 28 placed non-party NPCs, before additional shop consumers. Rylan also uses the generic mechanic image; the Arcade Prize Dispenser incorrectly uses that human face. These require distinct identities, not alternate crops of the same asset.

Several puzzle consoles intentionally share the lab-terminal image. Keep that coherent device family unless a specific console has a narrative reason to look different.

## Production order

1. **Establish the style with six portraits:** Evelyn, Elara, Jed, Thorne, Kaelen and Megan. This tests older warmth, emotionally important science-fiction characters, a worker, executive authority, field gear and clinical professionalism. Compare them with all four protected party members and with one another at actual dialogue size and 64/96/128-pixel previews. Refine this batch before scaling production.
2. **Complete story-critical continuity:** Vale, Warden, Rylos and remaining major speakers. Vale's current youthful blond portrait conflicts with his mature silver-haired combat appearance. Preserve a recognizable face across transformation. The Warden should match the active enclosed Iron Warden design. Rylos needs his described chrome face and a portrait registration. Elara and her cinematic depictions must share one approved identity. The user subsequently approved dark brown tied-back hair, warm brown skin and amber-brown eyes to distinguish her from Orion; update the glass scene and reaching-hand close-up together.
3. **Build local ensembles together:** Evelyn/Robert/Curator, the Salon's Dax/Sorrell/Lyra/Selene, Chris/Andrew, Jason/Meeko, and the Foundry workers. Viewing characters who share scenes side by side catches confusing faces and costume repetition early.
4. **Finish remaining workers, vendors, animals and service machines.** Reassess the seven provisional keeps against the finished cast. Review recurring World 6 appearances using the same identities.

This is a 40-base-portrait estimate, not a promise that every first output will pass. Do not generate full expression libraries for every NPC. After the base set works, identify only the expressions needed for specific high-value dialogue beats.

## Integration and quality checks

- Produce transparent 1024-square source portraits with consistent framing and apparent face size. Preserve source PNGs; ship optimized WebP using the existing art-production conventions. Check edge transparency against light and dark room backgrounds.
- Give each identity its own stable portrait path. Keep internal IDs such as `cook_vance` (Rourke) and `director_vance` (Sorrell); changing art does not require changing quest or save identifiers.
- Update every relevant consumer together: NPC portrait entries, shop portraits, explicit dialogue portraits, cinematic portraits and speaker mappings. Replacing a shared image in place would incorrectly change every identity still pointing to it.
- Register scripted speakers such as Rylos and Concierge appropriately. A generated file alone will not correct dialogue that currently falls back to the communicator portrait.
- Check explicit Vale, Thorne and Elara cinematic portrait references. Compare new portraits against existing cinematic and combat depictions; do not automatically repaint those larger assets or change the protected party art.
- Preserve original source assets outside the shipping pack before replacing them; remove obsolete packed images only after checking references.
- Inspect actual dialogue and shop presentation, including name alignment, crop, transparent padding, legibility, emote fallback and repeated visits. Confirm the party's four portrait families are untouched.

## Deferred candidates

These are not included in the 47-identity production scope. Confirm a player-facing use before spending on portraits:

- Two catalog entries without standard playable NPC placement: **Ollie and Charlie**. Charlie has special interaction handling; lack of an NPC room placement does not establish that all related content is unused.
- Four generic merchant endpoints without confirmed normal live placement: **Mechanic, Weapon, Armor and Accessory shops**. They may be debug-only; retain existing assets until usage is settled.
- Thirteen additional dialogue speakers without found room/NPC/event placement: **Innkeeper Bran, Caretaker Iva, Juno, Sergeant Leto, Pasha, Rana, Sedge, Doctor Sera, Sima, Tallo, Ellie, Schoolteacher Kasey and Maddie**. Do not confuse remaining dialogue records with proven live encounters.

## Reference evidence

- [Current portraits, sheet 1](current_portraits_1.jpg): party references and existing NPC art.
- [Current portraits, sheet 2](current_portraits_2.jpg): remaining NPC and shop art.
- [Cross-art continuity](story_continuity.jpg): Elara, Warden and Vale comparisons.

Source review covered the NPC and shop catalogs, room placement including conditional presence, dialogue/cinematic portrait overrides, and runtime speaker lookup. Visual standards follow `docs/story/Starborn_Art_Production_Guide.md` and `docs/story/Visual_Prompting_Guide.md`. Shipping NPC images currently live under `world_assets/src/main/assets/images/npcs`.
