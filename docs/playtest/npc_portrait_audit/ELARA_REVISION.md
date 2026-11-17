# Elara identity correction — November 17, 2026

The user rejected the pale-haired portrait because it resembled Orion and approved a distinct human design: dark brown wavy hair loosely tied back, warm brown skin, amber-brown eyes, strong brows and a mature attentive face. The final costume is worn charcoal utility clothing with muted teal inset panels, a notched stand collar and restrained geometric seams. A dark collar connection socket and small cyan wrist interface suggest her connection to machinery. The collar construction and wrist interface echo Gh0st’s gear while her soft clothing remains distinct from his protective field outfit. The earlier lab coat was an art interpretation, not an established scientist role, and has been removed.

## Updated consumers

- The NPC catalog and the explicit stasis-scene portrait already point to `images/npcs/elara_portrait_v2.webp`; that asset now contains the revised identity.
- `scene_w5_anchor_stasis` uses the revised `images/cinematics/w5_stasis_elara_v2.webp`, matching her hair, face, eyes, skin and visible hand while retaining the quiet reunion through glass.
- `scene_w5_cross_the_tear` now uses `images/cinematics/w5_tear_hands_v2.webp` for both illustrated steps. The exposed hand and sleeve match the new portrait; the charcoal sleeve and small cyan rectangular wrist interface match the glass scene.
- Inspected the stasis lunge and ending/crossing illustrations. They contain the crew/enemy or environments, without a further visible Elara face to replace. The ending mentions her return in text but does not depict an additional Elara portrait in that background.

No dialogue, quest logic, rewards, scene timing, party portraits or intro assets were changed in this correction. Prior versions are archived under `output/imagegen/elara_revision_20261116/archive/` and `output/imagegen/elara_outfit_20261117/archive/` outside the shipping pack.

## Generation and review

Generated with the user's OpenAI API key through the bundled image CLI, `gpt-image-2`, quality `low`. Portrait source: 1024 square; cinematic sources: 1088×1920. Shipped as WebP quality 90 with the existing alpha/export conventions. The approved portrait was supplied as the identity reference for both cinematic edits.

Prompts: [portrait](prompts/elara_utility_outfit.txt), [glass scene](prompts/cinematic_elara_utility_outfit.txt), [reaching hands](prompts/cinematic_elara_wrist_interface.txt), [glass cleanup](prompts/cinematic_elara_utility_cleanup.txt).

[Portrait and scene review](elara_revision_review.jpg). Full production paths and checks are recorded in the production manifest and validation record. No installation, version increment, commit or push was requested for this correction.

Final validation: cast/portrait/cinematic references pass; 51 used emote references pass; enclosed curl background cleaned with no residual chroma key; unrelated character and cinematic files remain byte-for-byte unchanged.
