# Starborn Art Production Guide

**Status:** Canon  
**Purpose:** Single source of truth for generating Starborn visual assets in future Codex sessions.

## 1. Required First Steps

Before generating or editing art:

1. Read this guide.
2. Read `docs/story/Visual_Prompting_Guide.md`.
3. Read `data/assistant_briefing.md`.
4. Inspect the current JSON entry that will consume the asset.
5. Inspect two or more approved assets of the same type.
6. Do not wire, rename, delete, or replace assets unless the user requests it.

The game is mobile-first and played in portrait orientation. Art must remain readable behind compact mobile UI.

## 2. Visual North Star

Starborn is a colorful chibi-anime/comic science-fiction adventure with:

- Thick, dark outlines and clear silhouettes.
- Chunky, iconic forms rather than realistic proportions.
- Crisp cel shading and controlled painted texture.
- Cyan resonance light balanced by amber, violet, red, brass, rust, and industrial yellow.
- Blue-collar machinery, patched habitats, and cosmic acoustic technology.
- Serious stakes presented with adventure-game clarity, not grim photorealism.
- Low micro-detail so subjects and environments remain legible on a phone.

Avoid generic military realism, muddy dystopian palettes, horror-first rendering, glossy generic 3D, excessive visual noise, and one-note blue or purple scenes.

## 3. Default API Settings

These are the canonical defaults going forward:

| Asset type | Model | Quality | Size | Background |
| --- | --- | --- | --- | --- |
| Room background | `gpt-image-2` | `low` | `1088x1920` | Opaque |
| Hub map background | `gpt-image-2` | `low` | `1088x1920` | Opaque |
| Title-screen background | `gpt-image-2` | `low` | `1088x1920` | Opaque |
| Enemy combat sprite | `gpt-image-2` | `low` | `1024x1024` | Chroma key, then transparent |
| Character combat sprite | `gpt-image-2` | `low` | `1024x1024` | Chroma key, then transparent |
| Character/NPC portrait | `gpt-image-2` | `low` | `1024x1024` | Chroma key, then transparent |
| Hub node icon | `gpt-image-2` | `low` | `1024x1024` | Chroma key, then transparent |
| Standalone logo | `gpt-image-2` | `low` | `1536x1024` | Chroma key, then transparent |

`quality=low` is intentional. It is the approved balance of cost, clean shapes, mobile readability, and stylistic consistency. Do not increase quality automatically. Use `medium` only with explicit user approval, normally for an identity-sensitive edit or difficult logo typography.

Use `OPENAI_API_KEY` from the environment. Never print, document, commit, or embed the key.

## 4. Room Backgrounds

### Production Rules

- Opaque `1088x1920` portrait PNG.
- First-person environmental view.
- No people, characters, animals, creatures, or readable writing unless explicitly requested.
- No lootable item or temporary quest object unless explicitly requested.
- Show the room as a gameplay stage before actors and interaction overlays are added.
- Use a clear dominant feature and readable foreground, midground, and background.
- Reserve calmer regions where room UI, actions, dialogue, or popups commonly appear.
- Do not add black sidebars unless the current consuming UI or user explicitly requires them.

### Current Style References

Inspect the currently wired `*_v3.png` and `*_v5.png` Hub 1 room backgrounds under:

`app/src/main/assets/images/rooms/world_1/`

### Prompt Template

```text
Use case: stylized-concept
Asset type: portrait mobile game room background
Primary request: Create the background for <ROOM NAME> in <NODE/HUB/WORLD>.
Scene/backdrop: <ROOM PURPOSE, ARCHITECTURE, PERMANENT FEATURES, AND STORY CONTEXT>.
Style/medium: Simplified chibi-anime science-fiction environment, bold comic readability, chunky geometry, thick painted outlines, flatter cel shading, restrained texture, low micro-detail.
Composition/framing: 1088x1920 portrait, first-person view from the room entrance, clear dominant feature, readable depth layers, mobile-friendly negative space.
Lighting/mood: <LIGHT SOURCES AND EMOTIONAL TONE>, colorful adventure clarity rather than oppressive darkness.
Color palette: <LOCATION-SPECIFIC COLORS> balanced with cyan resonance and warm practical lighting.
Constraints: Environment only. No people, characters, animals, creatures, readable writing, letters, numbers, logos, UI, watermark, or temporary loot unless explicitly specified.
Avoid: photorealism, muddy darkness, excessive clutter, generic empty sci-fi corridors, tiny unreadable details, text, and visual elements that imply unrequested interactions.
```

## 5. Character and NPC Portrait Art

Character combat sprites and portraits are separate asset roles even if some current files share artwork.
NPC portraits and emotes use the same visual language as character portraits and emotes. Do not create a separate gritty, painterly, semi-realistic, or lower-detail NPC style.

### Shared Rules

- Transparent `1024x1024` RGBA PNG.
- Preserve established identity exactly: face, hair, eye color, skin tone, outfit, signature equipment, and palette.
- Inspect the existing character assets before prompting.
- Use thick outlines, bright cel shading, expressive anime features, and chibi proportions.
- Keep all hair, weapons, limbs, and effects inside the canvas.
- Judge portraits at dialogue-box size, not only at full resolution.

### Combat Sprite

- Show a full or gameplay-appropriate combat pose.
- Prioritize silhouette, role, weapon, and action readability.
- Avoid portrait cropping unless the combat UI intentionally uses bust art.

### Portrait

- Head-and-shoulders or upper-body composition.
- Expression must be readable at small dialogue size.
- Keep the face unobstructed.
- Emote variants must preserve identity, clothing, crop, lighting, and rendering style; change only the requested expression or pose detail.

### NPC Portraits

- Generate NPC base portraits as `1024x1024` transparent RGBA PNGs under `app/src/main/assets/images/npcs/`.
- NPCs must match the approved party portrait/emote style, especially the clean readability of Zeke, Nova, Orion, and Gh0st.
- NPC portraits should have bold dark outlines, bright cel shading, large readable facial features, simple confident shapes, and a clear mobile-scale silhouette.
- NPC role details are allowed, but keep them graphic and sparse: one or two readable props or costume cues, not dense concept-art clutter.
- Avoid muted painterly rendering, gritty realism, tiny costume details, low-contrast muddy palettes, complex props, environmental lighting, and semi-realistic anatomy.
- For NPCs who also exist as party/character art, reuse the approved character portrait unless a separate NPC identity is intentionally requested.
- Archive replaced NPC portraits before installing new ones, for example under `app/src/main/assets/images/npcs/_archive/<reason_or_date>/`.

### NPC Emotes

- NPCs can use emotes in dialogue, but they do not need the full party emote set by default.
- Generate only NPC emotes that are actually referenced by dialogue, cinematics, shops, or other content.
- NPC emotes must preserve the NPC base portrait's identity, crop, clothing, lighting, outline weight, and rendering style; only the expression or small pose detail should change.
- Dialogue must fall back gracefully when an NPC emote is not available: use the NPC base portrait first, then the global communicator/default portrait only if the speaker has no usable portrait.
- Before shipping dialogue changes that use emotes, run:

```powershell
python scripts\validate_dialogue_emotes.py
```

The validator reports referenced NPC emotes that have not been generated yet.

### Prompt Template

```text
Use case: stylized-concept
Asset type: transparent Starborn <combat sprite/dialogue portrait>
Primary request: Create <CHARACTER NAME> for <GAMEPLAY USE>.
Identity: Preserve <HAIR, EYES, FACE, SKIN, CLOTHING, EQUIPMENT, AND SIGNATURE COLORS> from the approved reference image.
Style/medium: Bold chibi-anime/comic science-fiction character art, thick black outline, glossy cel shading, expressive face, chunky mobile-readable silhouette.
Composition/framing: Single isolated character, <FULL-BODY COMBAT POSE/UPPER-BODY PORTRAIT>, centered with generous padding.
Color palette: Preserve the established character palette; use restrained cyan resonance accents only where appropriate.
Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject.
Avoid: identity drift, realistic anatomy, costume redesign, excessive detail, cropped hair or equipment, generic anime substitution.
```

For identity-sensitive work, use the current character image as a reference or edit target. Do not regenerate a known character from text alone when a reference is available.

### NPC Portrait Prompt Template

```text
Use case: stylized-concept
Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024.
Primary request: Create <NPC NAME>, <SHORT ROLE AND PERSONALITY>.
Character: <AGE/BUILD/EXPRESSION/COSTUME/ONE OR TWO ROLE CUES>. Keep the design readable and uncluttered at dialogue portrait size.
Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture.
Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression.
Color palette: Use clear, saturated character colors with restrained cyan resonance accents only where appropriate.
Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject.
Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution.
```

### NPC Emote Prompt Template

```text
Use case: stylized-concept
Asset type: Starborn NPC dialogue emote portrait; transparent PNG after chroma-key removal, 1024x1024.
Primary request: Create the <EMOTE NAME> emote for <NPC NAME>.
Identity: Preserve the approved <NPC NAME> base portrait exactly: face, hair, skin tone, outfit, palette, outline weight, crop, lighting, and rendering style.
Expression change: <SPECIFIC EXPRESSION OR SMALL POSE DETAIL>.
Style/medium: Same Starborn portrait/emote style as the approved party emotes: bright expressive 2D cel shading, bold dark outlines, simple mobile-readable shapes, large readable expression.
Composition/framing: Match the base portrait crop and scale. Centered upper-body bust with generous padding.
Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject.
Avoid: identity drift, costume redesign, different crop, different lighting, different line weight, painterly NPC style, and excessive prop detail.
```

## 6. Enemy Combat Sprites

Enemy-specific instructions, prompt templates, reference assets, and validation are canonical in:

`docs/story/Enemy_Sprite_Generation_Guide.md`

Summary: `gpt-image-2`, `quality=low`, `1024x1024`, flat `#00ff00`, then transparent RGBA extraction.

## 7. Hub Node Icons

### Production Rules

- Transparent `1024x1024` RGBA PNG.
- A compact isometric destination miniature, not a flat symbol or room background.
- The node must communicate its function through architecture and silhouette.
- Use bold geometry, thick outlines, cel shading, and restrained detail.
- No people, animals, writing, labels, UI, ground plane, or cast shadow.
- Match the hub background’s perspective and palette.

### Hub 1 References

- `pit_hub1_v2.png`
- `workshop_hub1_v2.png`
- `medbay_hub1_v2.png`
- `trade_row_hub1_v2.png`
- `checkpoint_hub1_v2.png`

### Prompt Template

```text
Use case: stylized-concept
Asset type: transparent isometric mobile-game hub node
Primary request: Create a compact destination miniature for <NODE NAME>.
Subject: <ARCHITECTURAL IDENTITY AND FUNCTION>.
Style/medium: Simplified chibi-anime science-fiction game asset, chunky isometric geometry, thick dark outlines, flatter cel shading, low micro-detail.
Composition/framing: Single centered isometric destination, complete silhouette, generous padding, readable at small mobile size.
Color palette: <NODE COLORS> coordinated with its hub, with restrained cyan and amber lighting.
Constraints: Flat solid #00ff00 chroma-key background, no cast shadow, no floor plane beyond the destination’s own compact base, no people, animals, writing, logos, UI, or watermark. Do not use #00ff00 in the subject.
Avoid: full scenes, realistic architecture, excessive clutter, detached props, text, and effects touching the border.
```

## 8. Hub Map Backgrounds

### Production Rules

- Opaque `1088x1920` portrait PNG.
- The hub map is an overworld environment behind separate node icons.
- Read actual node positions and rendered Y-coordinate behavior before prompting.
- Build connected paths and terrain that make node placement spatially believable.
- Leave the top title/description region calm.
- Use subtle terrain clearings, not giant pads, sockets, stages, or obvious placeholders.
- Do not paint the destination buildings into the background.
- No people, animals, writing, labels, UI, or logos.

Reference: `app/src/main/assets/images/hubs/homestead_v4.png`.

## 9. Title-Screen Art

### Background

- Opaque `1088x1920`.
- Reserve the upper area for the separate animated logo.
- Reserve the lower area for buttons.
- Show a specific Starborn place or story promise, not an abstract gradient.
- No baked-in logo, menu text, people, animals, or readable writing unless explicitly requested.

### Logo

- Transparent `1536x1024` RGBA PNG.
- The logo is a separate animation-ready asset.
- Text must read exactly `STARBORN`.
- Use bold anime science-fiction title treatment with resonance motifs and strong readability.
- Generate on chroma key and validate spelling before acceptance.

Current references:

- `app/src/main/res/drawable/title_background_starborn.png`
- `app/src/main/res/drawable/title_logo_starborn.png`

## 10. Transparency Workflow

For sprites, portraits, node icons, and logos, generate on a flat solid `#00ff00` background and run:

```powershell
python "$env:USERPROFILE\.codex\skills\.system\imagegen\scripts\remove_chroma_key.py" `
  --input "<keyed-source.png>" `
  --out "<final-transparent.png>" `
  --auto-key border `
  --soft-matte `
  --transparent-threshold 12 `
  --opaque-threshold 220 `
  --despill
```

Validate RGBA mode, transparent corners, subject coverage, and edge color. Retry with `--edge-contract 1` only if a visible green fringe remains.

## 11. Universal Validation

Before accepting any generated asset:

1. Confirm model, quality, dimensions, format, and destination.
2. Inspect the image visually at full size.
3. Compare it beside approved assets of the same type.
4. Check for accidental people, animals, writing, logos, or extra subjects.
5. Confirm important content is not cropped.
6. Confirm mobile-scale readability.
7. For transparent assets, verify RGBA and alpha-zero corners.
8. For room/hub backgrounds, verify the portrait composition supports the real UI.
9. Do not claim the asset is wired or tested in-game unless that work was actually performed.

## 12. Future Codex Startup Prompt

Use this at the start of a new art-production chat:

```text
I am working on Starborn at C:\Users\jctho\StudioProjects\StarbornAndroid. Before generating or editing any visual assets, read docs/story/Starborn_Art_Production_Guide.md, docs/story/Visual_Prompting_Guide.md, docs/story/Enemy_Sprite_Generation_Guide.md, and data/assistant_briefing.md. Treat them as canonical. Inspect the current JSON data and at least two approved assets of the same type before writing prompts. Follow the documented gpt-image-2 quality-low pipelines, dimensions, transparency workflow, visual style, content exclusions, naming, validation, and destination folders. Use OPENAI_API_KEY from the environment and never expose it. Do not alter unrelated files or wire generated assets into game data unless I explicitly request wiring.
```
