# NPC cast production — November 16, 2026

## Delivered scope

The planned 40 new/redrawn portraits have been generated through the OpenAI API using the user's local key file, `gpt-image-2`, quality `low`, and square 1024px source art. The seven provisional keeps remain unchanged. Each of the 47 production identities has its own portrait; repeat appearances of the same person intentionally use the same art.

The game data now connects 36 replacement NPC portraits, registers Rylos and Concierge as scripted speakers, and updates seven shop portraits. Existing General Store and Sentinel Scraps mappings still match their NPCs. NPC placement, dialogue, quest logic, rewards and cinematic timing remain unchanged. Rylos and Concierge registrations have no new room placement or interaction options.

The four party portrait families remain unchanged, including emotes and Zeke's NPC portrait copy. The new-game intro's art and sequence remain unchanged.

## Cinematic continuity

All 14 cinematic illustrations and the 17 room backdrops referenced by cinematic steps were inspected. Three illustrations contain redesigned NPCs or a visible part of them and were replaced:

- `w5_stasis_elara_v2.webp`: Elara's mature face, dark brown tied-back hair, warm brown skin, amber-brown eyes, charcoal-and-teal utility clothing and dark collar connection socket match her revised portrait. Gh0st remains recognizable in his established design; the hand-to-hand moment across the glass remains the scene's focus.
- `w5_tear_hands_v2.webp`: Elara's exposed reaching hand has the same warm brown skin, with a charcoal utility sleeve, muted teal seam and small cyan wrist interface. Gh0st's glove and the reaching composition remain consistent.
- `w4_coup_display_v2.webp`: Thorne's mature silver bob and executive blue, and Vale's mature silver hair, trimmed beard and charcoal/violet uniform, match their new portraits. Thorne's anger and Vale's control remain readable during the coup.

The four explicit NPC portrait overrides in these scenes were updated as well. Other cinematic NPC dialogue inherits the new portraits through the normal speaker lookup. Scenes using empty room backgrounds did not need new illustrations. The Warden's portrait now follows the active enclosed armored boss, and Vale's facial identity agrees with his ascended combat art; no enemy sprites were modified.

## Art review decisions

The first six portraits established the style, with further targeted revisions before export:

- Initially removed an Orion-like pendant from Elara; the user correctly identified a broader resemblance. Rebuilt her as a distinct dark-haired human adult and updated both affected cinematics. Subsequently replaced the unsupported research-coat costume with soft charcoal-and-teal utility clothing whose collar, seams and wrist interface echo Gh0st’s gear. See [the Elara revision](ELARA_REVISION.md).
- Corrected clipped bust framing and normalized the final transparent exports.
- Removed inappropriate human clothing from Oliver and Meeko; both remain natural feline creatures.
- Removed scarf/jewelry from Caretaker Echo; Zenith is a supply dispenser rather than a humanoid shopkeeper.
- Gave Robert a distinct clean-shaven face and practical tailoring, avoiding another Curator look.
- Gave Dax a clean-shaven copper-haired appearance distinct from the studio musicians.
- Refined Thorne's confrontation expression in the broadcast scene.

Transparent exports use the existing chroma-key matte/despill/edge-bleed process, additionally lifting enclosed regions containing pure key green (such as the opening inside Jax's claw). Subject olive and cyan colors are preserved. Shipped images use WebP quality 90, method 6, exact alpha colors, with at least 48px transparent safety margin around new portraits.

Superseded images with no remaining runtime, content, test or script references were archived locally outside the shipping pack before removal. Images still used by retained NPCs or deferred/debug shops remain available. Existing desktop portrait fixtures now refer to Jed and Pam's new assets.

## Review material and provenance

- [Production manifest](production_manifest.json): identities, old/new asset paths, selected source files and prompt references.
- [Final portrait sheets](cast_review_1.jpg), [continued](cast_review_2.jpg).
- [Small-size review](mobile_review.jpg): portraits at dialogue-scale sizes on light and dark backgrounds.
- [Cinematic comparison](cinematic_review.jpg).
- [Prompts](prompts/): base requests and targeted revisions, without credentials.
- Local sources and original archive: `output/imagegen/npc_cast_20261116/` (ignored by Git).

The 19 deferred/unconfirmed candidates from the original plan remain deferred. This pass did not generate unused dialogue-speaker portraits or full NPC emote libraries.

## Validation

Validation results are recorded in [validation.json](validation.json). The cast validator and dialogue/emote validator pass; all seven targeted desktop tests and the cinematic music integrity test pass. The opening-cinematic integrity test has one pre-existing failure: its two anonymous-speaker lines have no explicit portraits, while the test requires Orion portraits. Both lines and the entire intro are identical to the starting revision; the protected intro was not changed to satisfy this unrelated assertion. The reusable cast check is `scripts/validate_npc_portrait_cast.py`; the existing dialogue/emote validator checks the wider reference set. Targeted desktop dialogue/shop and Android cinematic asset tests check the affected consumers. Contact sheets are visual review artifacts, not a claim of phone playtesting.

No version increment, commit, push or device installation is part of this task.
