#!/usr/bin/env python3
import subprocess
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
TMP_DIR = PROJECT_ROOT / "tmp" / "imagegen"
ENEMY_OUT_DIR = PROJECT_ROOT / "world_assets" / "src" / "main" / "assets" / "images" / "enemies"
REMOVE_CHROMA_SCRIPT = Path("C:/Users/jctho/.codex/skills/.system/imagegen/scripts/remove_chroma_key.py")

ENEMIES = {
    "core_drill_behemoth": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Core Drill Behemoth, mutated swamp boss. Subject: A massive, hyper-evolved swamp beast warped by raw resonance, giant jaws, stone-like scales, sharp crystalline claws, glowing cyan resonance cracks along its back. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic three-quarter combat pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Muddy green scales, dark grey stone plating, cyan resonance glowing cracks. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    },
    "mutated_crawler": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Mutated Crawler, corrosive sewer creature. Subject: A multi-legged insectoid crawler with glowing yellow-green acid glands along its side, large mandibles, thick chitinous plates, prying antennae. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic three-quarter combat pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Dark brown chitin, glowing yellow-green acid sacs, amber highlights. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    },
    "sentinel_droid": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Sentinel Droid, corporate riot guard. Subject: An armored humanoid guard robot with bulky composite plates, carrying a high-voltage stun baton and a compact neon cyan tower shield. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic three-quarter combat pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Clean white and dark grey plating, orange visor, cyan shield energy. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    },
    "aero_drone": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Aero Drone, flying stun drone. Subject: A hovering security drone with a sleek metallic frame, central blue glowing stun emitter, small stabilizing wings, and venting thruster nozzles. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic hovering pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Polished chrome, dark grey mechanics, bright cyan thruster/stun glow. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    },
    "sentinel_orb": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Sentinel Orb, zero-g defense sphere. Subject: A spherical security drone with mechanical panels shifting open, revealing a central glowing cyan laser lens, and tiny thruster ports keeping it afloat. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic hovering pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Metallic silver panels, black internal chassis, bright cyan laser eye. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    },
    "ruin_guardian": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Ruin Guardian, ancient stone sentinel. Subject: A relic-stone robot constructed from ancient heavy slabs bound together by glowing cyan resonance current, single glowing center eye, cracks glowing with psionic power. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic three-quarter combat pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Weathered grey stone, brass mechanical joints, bright cyan energy flows. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    },
    "phantom_assassin": {
        "prompt": "Use case: stylized-concept. Asset type: transparent mobile RPG combat enemy sprite. Primary request: Create a single full-body combat sprite for Phantom Assassin, corporate stealth strike agent. Subject: A sleek stealth humanoid striker in skin-tight pitch-black combat armor, wearing a full visor glowing with a single cyan bar, holding dual glowing energy daggers. Style/medium: Starborn enemy sprite style: bold anime/comic sci-fi adventure, thick black outline, chunky readable silhouette, glossy cel shading, compact or exaggerated proportions, mobile-readable details. Composition/framing: Single isolated enemy centered in a dynamic three-quarter combat pose, full body visible, defining weapon or feature visible, generous padding, no ground plane. Color palette: Matte black suit, dark grey trim, neon cyan visor and blade energy. Constraints: Create the requested subject on a perfectly flat solid #00ff00 chroma-key background for background removal. The background must be one uniform color with no shadows, gradients, texture, reflections, floor plane, or lighting variation. Keep the subject fully separated from the background with crisp edges and generous padding. Do not use #00ff00 anywhere in the subject. No cast shadow, no contact shadow, no reflection, no watermark, and no text. Avoid: readable writing, UI, logos, extra characters or creatures, photorealism, realistic proportions, tiny noisy details, and effects that touch the image border."
    }
}

def main():
    TMP_DIR.mkdir(parents=True, exist_ok=True)
    ENEMY_OUT_DIR.mkdir(parents=True, exist_ok=True)

    for enemy_id, info in ENEMIES.items():
        keyed_file = TMP_DIR / f"{enemy_id}_keyed.png"
        final_file = ENEMY_OUT_DIR / f"{enemy_id}_combat.png"

        # Generate image via DALL-E
        print(f"Generating image for Enemy: {enemy_id}...")
        subprocess.run([
            "python", "scripts/generate_images.py",
            "--prompt", info["prompt"],
            "--output", str(keyed_file),
            "--model", "gpt-image-2"
        ], check=True)

        # Apply chroma-key background removal
        print(f"Removing chroma-key for Enemy: {enemy_id}...")
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
        print(f"Finished processing Enemy: {enemy_id} -> {final_file.relative_to(PROJECT_ROOT)}")

if __name__ == "__main__":
    main()
