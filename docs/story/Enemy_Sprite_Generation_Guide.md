# Starborn Enemy Sprite Generation Guide

**Status:** Canon  
**Purpose:** Reproduce the established World 1 combat-enemy sprite style in future Codex sessions.

This specialized guide is part of the broader
`docs/story/Starborn_Art_Production_Guide.md` workflow.

## 1. Reference Set

Use these assets as the primary visual reference:

- `images/enemies/echo_borer_combat.png`
- `images/enemies/siren_skimmer_combat.png`
- `images/enemies/dominion_dampener_combat.png`
- `images/enemies/acoustic_bulwark_combat.png`
- `images/enemies/resonance_buoy_combat.png`
- `images/enemies/pressure_hauler_combat.png`
- `images/enemies/the_iron_warden_combat.png`

Do not use older World 1 enemy sprites as the main style target when producing replacements for this set.

## 2. Required Generation Settings

| Setting | Required value |
| --- | --- |
| Model | `gpt-image-2` |
| Quality | `low` |
| Source size | `1024x1024` |
| Source format | PNG |
| Source background | Flat solid `#00ff00` chroma key |
| Final format | Transparent RGBA PNG |
| Final folder | `app/src/main/assets/images/enemies/` |
| Filename | `<enemy_id>_combat.png` |

`quality=low` is intentional. It produced the approved combination of bold forms, clean cel shading, controlled detail, and reasonable API cost. Do not raise the quality automatically.

## 3. Style Lock

Every sprite should use:

- Bold chibi-anime/comic science-fiction styling.
- Thick, dark outer outlines.
- Chunky, immediately readable silhouettes.
- Compact or exaggerated proportions rather than realistic anatomy.
- Glossy cel shading with crisp highlights.
- Large, readable equipment and defining features.
- Cyan resonance lighting with selective amber, violet, red, brass, or industrial-yellow accents.
- Enough detail to establish identity without becoming noisy at mobile combat scale.
- A single isolated subject in a dynamic three-quarter combat pose.
- The entire subject visible with generous edge padding.

The target is colorful adventure-game enemy art, not realistic military concept art, grim horror, painterly realism, or miniature pixel art.

## 4. Prompt Template

Use this structure for every enemy:

```text
Use case: stylized-concept
Asset type: transparent mobile RPG combat enemy sprite
Primary request: Create a single full-body combat sprite for <NAME>, <SHORT ROLE AND CONCEPT>.
Subject: <BODY, SILHOUETTE, EQUIPMENT, CREATURE OR ARMOR DETAILS, AND SIGNATURE COMBAT FEATURE>.
Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details.
Composition/framing: Single isolated enemy centered in a dynamic three-quarter combat pose, full body visible, defining weapon or feature visible, generous padding, no ground plane.
Color palette: <PRIMARY MATERIALS AND COLORS>, cyan resonance accents, selective amber or violet highlights.
Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text.
Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border.
```

For flying enemies, use a hovering pose and keep wings or emitters fully inside the canvas. For bosses, increase mass, ornamentation, and silhouette authority while retaining the same chibi/comic rendering language.

## 5. Background Removal

Generate against chroma key first, then run:

```powershell
python "$env:USERPROFILE\.codex\skills\.system\imagegen\scripts\remove_chroma_key.py" `
  --input "<keyed-source.png>" `
  --out "app\src\main\assets\images\enemies\<enemy_id>_combat.png" `
  --auto-key border `
  --soft-matte `
  --transparent-threshold 12 `
  --opaque-threshold 220 `
  --despill
```

Do not use black as the generated background. Black is only how some image viewers display transparency.

## 6. Validation Checklist

Before accepting a sprite, verify:

1. The file is exactly `1024x1024`.
2. The image mode is RGBA.
3. All four corner alpha values are `0`.
4. No chroma-green fringe remains.
5. The whole subject and all weapons, wings, shields, antennae, and effects fit inside the canvas.
6. There is only one enemy and no environment or ground plane.
7. There is no writing, logo, watermark, or UI.
8. The silhouette remains readable when displayed at mobile combat size.
9. The sprite looks consistent beside the seven reference assets.

## 7. Codex Execution Notes

- Read this file before generating new Starborn enemies.
- Use the installed image-generation skill and `gpt-image-2`.
- Use `OPENAI_API_KEY` from the environment; never place the key in prompts, documentation, source control, or command output.
- Batch generation may use one distinct prompt per enemy.
- Put keyed intermediates under `tmp/imagegen/`.
- Keep only final transparent sprites in the production enemy folder.
- Do not wire sprites into `enemies.json` unless explicitly requested.
