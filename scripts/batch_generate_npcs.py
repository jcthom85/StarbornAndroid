#!/usr/bin/env python3
import os
import subprocess
import shutil
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
TMP_DIR = PROJECT_ROOT / "tmp" / "imagegen"
NPC_OUT_DIR = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "npcs"
ARCHIVE_DIR = NPC_OUT_DIR / "_archive" / "2026-06-20"
REMOVE_CHROMA_SCRIPT = Path("C:/Users/jctho/.codex/skills/.system/imagegen/scripts/remove_chroma_key.py")

NPCS = {
    "the_warden": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create The Warden, Dominion Sector Commander. Character: Middle-aged stern man in heavy pressurized Dominion power armor, dark metallic gray and cyan resonance plating, ritual calm expression. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Dark metal gray, white, cyan resonance accents. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "jax": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Jax, Bartender and Info Broker. Character: Gruff middle-aged man with a cybernetic loader claw on his shoulder, wearing a simple blue-collar apron and vest. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Denim blue, gray, steel claw, warm amber highlights. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "mika": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Mika, Street Food Vendor. Character: Cheerful young woman with hair tied back in a bandana, holding a simple metal ladle or spatula, wearing utility overalls. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Warm orange bandana, yellow apron, tan utility clothing. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "curator": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Museum Curator. Character: Velvet-robed elegant older gatekeeper with a condescending or smug expression, wearing a tall soft collar. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Deep purple velvet robe, gold embroidery, white collar. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "lab_terminal": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Hacked Lab Terminal. Character: A compact retro-futuristic laboratory computer terminal screen displaying glowing neon cyan command line code and wave waveforms. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered screen, head-on terminal casing visible, head large in frame, generous padding. Color palette: Industrial gray casing, bright glowing cyan screen content. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "thorne": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Director Mara Thorne. Character: An articulate middle-aged woman with clean, sharp corporate attire, a pensive or tired expression, shoulder-length gray hair. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Navy blue corporate collar, crisp white shirt, gray hair. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "maintenance_bot": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Maintenance Bot. Character: A friendly-looking dented horticultural caretaker robot with a single glowing camera eye, wearing a small rusted gardening visor or holding a tiny leaf branch. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered robotic body bust, camera eye large in frame, generous padding. Color palette: Rusted bronze casing, amber lens light, forest green leaf branch. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "elara": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Elara. Character: Ethereal young woman with eyes closed in calm focus, floating hair, with glowing cyan resonance lines radiating around her, reading as a living CPU integration. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Light cyan hair, pale skin, bright glowing cyan resonance markings. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    },
    "vale": {
        "prompt": "Use case: stylized-concept. Asset type: Starborn NPC portrait for dialogue boxes, menus, and combat UI; transparent PNG after chroma-key removal, 1024x1024. Primary request: Create Lieutenant Arden Vale. Character: Young compliance commander with smooth features, a calm, hauntingly peaceful smile, wearing white and gold officer uniform. Keep the design readable and uncluttered at dialogue portrait size. Style/medium: Match the approved Starborn character portrait/emote style: bright expressive 2D cel-shaded game portrait, bold dark outlines, simple confident shapes, large readable facial features, clean anime-influenced indie RPG proportions, high-saturation accents, smooth flat color blocks, crisp silhouette, minimal painterly texture. Composition/framing: Centered upper-body bust, head large in frame, shoulders visible, generous padding, no crop through hair/head, simple pose, readable expression. Color palette: Crisp white uniform, gold trimmings, neat blonde hair. Constraints: Flat solid #00ff00 chroma-key background, no shadow, no floor plane, no reflection, no text, no logo, no watermark, no extra characters. Do not use #00ff00 in the subject. Avoid: separate NPC concept-art style, gritty realism, semi-realistic painterly rendering, muted muddy colors, tiny costume clutter, complex props, environmental lighting, cropped head, and generic anime substitution."
    }
}

def main():
    TMP_DIR.mkdir(parents=True, exist_ok=True)
    ARCHIVE_DIR.mkdir(parents=True, exist_ok=True)

    for npc_id, info in NPCS.items():
        keyed_file = TMP_DIR / f"{npc_id}_keyed.png"
        final_file = NPC_OUT_DIR / f"{npc_id}.png"

        # 1. Archive if it exists
        if final_file.exists():
            archive_path = ARCHIVE_DIR / f"{npc_id}.png"
            shutil.copy2(final_file, archive_path)
            print(f"Archived {npc_id}.png to {archive_path.relative_to(PROJECT_ROOT)}")

        # 2. Generate image via DALL-E
        print(f"Generating image for NPC: {npc_id}...")
        subprocess.run([
            "python", "scripts/generate_images.py",
            "--prompt", info["prompt"],
            "--output", str(keyed_file),
            "--model", "gpt-image-2"
        ], check=True)

        # 3. Apply chroma-key background removal
        print(f"Removing chroma-key for NPC: {npc_id}...")
        subprocess.run([
            "python", str(REMOVE_CHROMA_SCRIPT),
            "--input", str(keyed_file),
            "--out", str(final_file),
            "--auto-key", "border",
            "--soft-matte",
            "--transparent-threshold", "12",
            "--opaque-threshold", "220",
            "--despill"
        ], check=True)
        print(f"Finished processing NPC: {npc_id} -> {final_file.relative_to(PROJECT_ROOT)}")

if __name__ == "__main__":
    main()
